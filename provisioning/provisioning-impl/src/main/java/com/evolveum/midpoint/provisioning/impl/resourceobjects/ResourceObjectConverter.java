/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.LiveSyncToken;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.TokenUtil;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.async.UcfAsyncUpdateChangeListener;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.*;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AsyncUpdateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;

/**
 * Serves as a facade for accessing resource objects.
 *
 * Responsibilities (mostly delegated):
 *
 * . protected objects
 * . simulated activation (delegated to {@link ActivationConverter})
 * . entitlements (delegated to {@link EntitlementReader} and {@link EntitlementConverter})
 * . script execution
 * . avoid duplicate values
 * . attributes returned by default/not returned by default
 *
 * Limitations (applies to the whole package):
 *
 * . must NOT access repository (only indirectly via {@link ResourceObjectReferenceResolver}) - TODO re-check
 * . does not know about OIDs
 *
 * @author Katarina Valalikova
 * @author Radovan Semancik
 */
@Component
public class ResourceObjectConverter {

    private static final String DOT_CLASS = ResourceObjectConverter.class.getName() + ".";
    static final String OPERATION_MODIFY_ENTITLEMENT = DOT_CLASS + "modifyEntitlement";
    private static final String OPERATION_ADD_RESOURCE_OBJECT = DOT_CLASS + "addResourceObject";
    private static final String OPERATION_MODIFY_RESOURCE_OBJECT = DOT_CLASS + "modifyResourceObject";
    private static final String OPERATION_DELETE_RESOURCE_OBJECT = DOT_CLASS + "deleteResourceObject";
    private static final String OPERATION_REFRESH_OPERATION_STATUS = DOT_CLASS + "refreshOperationStatus";
    private static final String OPERATION_HANDLE_CHANGE = DOT_CLASS + "handleChange";
    static final String OP_SEARCH_RESOURCE_OBJECTS = DOT_CLASS + "searchResourceObjects";
    static final String OP_COUNT_RESOURCE_OBJECTS = DOT_CLASS + "countResourceObjects";

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectConverter.class);

    /** TODO document this */
    public static final String FULL_SHADOW_KEY = ResourceObjectConverter.class.getName() + ".fullShadow";

    /**
     * Retrieves resource object, given its primary identifiers.
     *
     * Note that this method can return `null` only if the resource is caching-only and the `repoShadow` is `null`.
     *
     * @param repoShadow Used when read capability is "caching only"
     */
    @Contract("_, _, !null, _, _ -> !null")
    public CompleteResourceObject getResourceObject(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification.Primary primaryIdentification,
            @Nullable ShadowType repoShadow,
            boolean fetchAssociations,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, GenericConnectorException, ExpressionEvaluationException {

        return fetchResourceObject(
                ctx,
                primaryIdentification,
                ctx.createAttributesToReturn(),
                repoShadow,
                fetchAssociations,
                result);
    }

    /**
     * Fetches the resource object either by primary identifier(s) or by secondary identifier(s).
     * In the latter case, the primary identifier is resolved (from the secondary ones) by the repository.
     *
     * Returns `null` if the resource is caching-only, and the `repoShadow` is `null`.
     * In all other "unavailability" cases, appropriate exception is thrown.
     *
     * @param repoShadow Used when read capability is "caching only"
     */
    @Contract("_, _, _, !null, _, _ -> !null")
    @Nullable CompleteResourceObject fetchResourceObject(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification.Primary primaryIdentification,
            @Nullable AttributesToReturn attributesToReturn,
            @Nullable ShadowType repoShadow,
            boolean fetchAssociations,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return ResourceObjectLocateOrFetchOperation.executeFetch(
                ctx, primaryIdentification, fetchAssociations, attributesToReturn, repoShadow, result);
    }

    /**
     * Obtains the resource object:
     *
     * - Tries to get the object directly if primary identifiers are present.
     * - Tries to search for the object if they are not.
     *
     * Both cases are handled by the resource, i.e. not by the repository.
     * The returned object is in the "initialized" state.
     *
     * Currently seems to be used for entitlements search.
     * (It is questionable whether we should do the full processing of activation etc here.)
     */
    public CompleteResourceObject locateResourceObject(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification identification,
            boolean fetchAssociations,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, GenericConnectorException, ExpressionEvaluationException {
        return ResourceObjectLocateOrFetchOperation.executeLocate(ctx, identification, fetchAssociations, result);
    }

    public SearchResultMetadata searchResourceObjects(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectHandler resultHandler,
            @Nullable ObjectQuery query,
            boolean fetchAssociations,
            @Nullable FetchErrorReportingMethodType errorReportingMethod,
            @NotNull OperationResult parentResult)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return new ResourceObjectSearchOperation(ctx, resultHandler, query, fetchAssociations, errorReportingMethod)
                .execute(parentResult);
    }

    public Integer countResourceObjects(
            @NotNull ProvisioningContext ctx,
            @Nullable ObjectQuery query,
            @NotNull OperationResult parentResult)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return new ResourceObjectCountOperation(ctx, query)
                .execute(parentResult);
    }

    public AsynchronousOperationReturnValue<ShadowType> addResourceObject(
            ProvisioningContext ctx,
            ShadowType shadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            boolean skipExplicitUniquenessCheck,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_ADD_RESOURCE_OBJECT);
        try {
            return ResourceObjectAddOperation.execute(ctx, shadow, scripts, connOptions, skipExplicitUniquenessCheck, result);
        } catch (Throwable t) {
            result.recordException("Could not create object on the resource: " + t.getMessage(), t);
            throw t;
        } finally {
            result.close();
        }
    }

    public AsynchronousOperationResult deleteResourceObject(
            ProvisioningContext ctx,
            ShadowType shadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_DELETE_RESOURCE_OBJECT);
        try {
            return ResourceObjectDeleteOperation.execute(ctx, shadow, scripts, connOptions, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    static void updateQuantum(
            ProvisioningContext ctx,
            ConnectorInstance connectorUsedForOperation,
            AsynchronousOperationResult aResult,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ConnectorInstance readConnector = ctx.getConnector(ReadCapabilityType.class, parentResult);
        if (readConnector != connectorUsedForOperation) {
            // Writing by different connector that we are going to use for reading: danger of quantum effects
            aResult.setQuantumOperation(true);
        }
    }

    /**
     * Returns known executed deltas as reported by {@link ConnectorInstance#modifyObject(ResourceObjectIdentification,
     * PrismObject, Collection, ConnectorOperationOptions, UcfExecutionContext, OperationResult)}.
     */
    public AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue<?>>>> modifyResourceObject(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repoShadow,
            OperationProvisioningScriptsType scripts,
            ConnectorOperationOptions connOptions,
            Collection<? extends ItemDelta<?, ?>> itemDeltas,
            XMLGregorianCalendar now,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        OperationResult result = parentResult.subresult(OPERATION_MODIFY_RESOURCE_OBJECT)
                .addParam("repoShadow", repoShadow)
                .addArbitraryObjectAsParam("connOptions", connOptions)
                .addArbitraryObjectCollectionAsParam("itemDeltas", itemDeltas)
                .addArbitraryObjectAsContext("ctx", ctx)
                .build();

        try {
            return ResourceObjectModifyOperation.execute(ctx, repoShadow, scripts, connOptions, itemDeltas, now, result);
        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        } finally {
            result.recordEnd();
        }
    }

    public LiveSyncToken fetchCurrentToken(
            ProvisioningContext ctx, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException {
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.trace("Fetching current sync token for {}", ctx);

        UcfSyncToken lastToken;
        ConnectorInstance connector = ctx.getConnector(LiveSyncCapabilityType.class, parentResult);
        try {
            lastToken = connector.fetchCurrentToken(ctx.getObjectDefinition(), ctx.getUcfExecutionContext(), parentResult);
        } catch (CommunicationException ex) {
            throw communicationException(ctx, connector, ex);
        } catch (GenericFrameworkException ex) {
            throw genericConnectorException(ctx, connector, ex);
        }

        LOGGER.trace("Got last token: {}", SchemaDebugUtil.prettyPrint(lastToken));

        computeResultStatus(parentResult);

        return TokenUtil.fromUcf(lastToken);
    }

    public UcfFetchChangesResult fetchChanges(
            ProvisioningContext ctx,
            @NotNull LiveSyncToken initialToken,
            @Nullable Integer maxChangesConfigured,
            ResourceObjectLiveSyncChangeListener outerListener,
            OperationResult gResult)
            throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, GenericFrameworkException, ObjectNotFoundException, ExpressionEvaluationException {

        LOGGER.trace("START fetch changes from {}, objectClass: {}", initialToken, ctx.getObjectClassDefinition());
        AttributesToReturn attrsToReturn;
        if (ctx.isWildcard()) {
            attrsToReturn = null;
        } else {
            attrsToReturn = ctx.createAttributesToReturn();
        }

        ConnectorInstance connector = ctx.getConnector(LiveSyncCapabilityType.class, gResult);
        Integer maxChanges = getMaxChanges(maxChangesConfigured, ctx);

        AtomicInteger processed = new AtomicInteger(0);
        UcfLiveSyncChangeListener localListener = (ucfChange, lParentResult) -> {
            int changeNumber = processed.getAndIncrement();

            OperationResult lResult = lParentResult.subresult(OPERATION_HANDLE_CHANGE)
                    .setMinor()
                    .addParam("number", changeNumber)
                    .addParam("localSequenceNumber", ucfChange.getLocalSequenceNumber())
                    .addArbitraryObjectAsParam("primaryIdentifier", ucfChange.getPrimaryIdentifierValue())
                    .addArbitraryObjectAsParam("token", ucfChange.getToken()).build();

            try {
                ResourceObjectLiveSyncChange change =
                        new ResourceObjectLiveSyncChange(ucfChange, ctx, attrsToReturn);
                // Intentionally not initializing the change here. Let us be flexible and let the ultimate caller decide.
                return outerListener.onChange(change, lResult);
            } catch (Throwable t) {
                lResult.recordFatalError(t);
                throw t;
            } finally {
                lResult.computeStatusIfUnknown();
            }
        };

        // get changes from the connector
        UcfFetchChangesResult fetchChangesResult = connector.fetchChanges(
                ctx.getObjectDefinition(),
                TokenUtil.toUcf(initialToken),
                attrsToReturn,
                maxChanges,
                ctx.getUcfExecutionContext(),
                localListener,
                gResult);

        computeResultStatus(gResult);

        LOGGER.trace("END fetch changes ({} changes); interrupted = {}; all fetched = {}, final token = {}",
                processed.get(), !ctx.canRun(), fetchChangesResult.isAllChangesFetched(), fetchChangesResult.getFinalToken());

        return fetchChangesResult;
    }

    @Nullable
    private Integer getMaxChanges(@Nullable Integer maxChangesConfigured, ProvisioningContext ctx) {
        LiveSyncCapabilityType capability = ctx.getCapability(LiveSyncCapabilityType.class); // TODO what if it's disabled?
        if (capability != null) {
            if (Boolean.TRUE.equals(capability.isPreciseTokenValue())) {
                return maxChangesConfigured;
            } else {
                checkMaxChanges(maxChangesConfigured, "LiveSync capability has preciseTokenValue not set to 'true'");
                return null;
            }
        } else {
            // Is this possible?
            checkMaxChanges(maxChangesConfigured, "LiveSync capability is not found or disabled");
            return null;
        }
    }

    private void checkMaxChanges(Integer maxChangesFromTask, String reason) {
        if (maxChangesFromTask != null && maxChangesFromTask > 0) {
            throw new IllegalArgumentException(
                    String.format("Cannot apply %s because %s", LiveSyncWorkDefinitionType.F_BATCH_SIZE.getLocalPart(), reason));
        }
    }

    public void listenForAsynchronousUpdates(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectAsyncChangeListener outerListener,
            @NotNull OperationResult parentResult) throws SchemaException,
            CommunicationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException {

        LOGGER.trace("Listening for async updates in {}", ctx);
        ConnectorInstance connector = ctx.getConnector(AsyncUpdateCapabilityType.class, parentResult);

        UcfAsyncUpdateChangeListener innerListener = (ucfChange, listenerTask, listenerResult) -> {
            ResourceObjectAsyncChange change = new ResourceObjectAsyncChange(ucfChange, ctx);
            // Intentionally not initializing the change here. Let us be flexible and let the ultimate caller decide.
            outerListener.onChange(change, listenerTask, listenerResult);
        };
        connector.listenForChanges(innerListener, ctx::canRun, parentResult);

        LOGGER.trace("Finished listening for async updates");
    }

    public AsynchronousOperationResult refreshOperationStatus(
            ProvisioningContext ctx, ShadowType shadow, String asyncRef, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        OperationResult result = parentResult.createSubresult(OPERATION_REFRESH_OPERATION_STATUS);

        ResourceType resource;
        ConnectorInstance connector;
        try {
            resource = ctx.getResource();
            // TODO: not really correct. But good enough for now.
            connector = ctx.getConnector(UpdateCapabilityType.class, result);
        } catch (ObjectNotFoundException | SchemaException | CommunicationException
                | ConfigurationException | ExpressionEvaluationException | RuntimeException | Error e) {
            result.recordFatalError(e);
            throw e;
        }

        OperationResultStatus status = null;
        if (connector instanceof AsynchronousOperationQueryable) {

            LOGGER.trace("PROVISIONING REFRESH operation ref={} on {}, object: {}",
                    asyncRef, resource, shadow);

            try {

                status = ((AsynchronousOperationQueryable) connector).queryOperationStatus(asyncRef, result);

            } catch (ObjectNotFoundException | SchemaException | ConfigurationException | CommunicationException e) {
                result.recordFatalError(e);
                throw e;
            }

            result.recordSuccess();

            LOGGER.debug("PROVISIONING REFRESH ref={} successful on {} {}, returned status: {}", asyncRef, resource, shadow, status);

        } else {
            LOGGER.trace("Ignoring refresh of shadow {}, because the connector is not async operation queryable", shadow.getOid());
            result.recordNotApplicableIfUnknown();
        }

        OperationResult refreshResult = new OperationResult(OPERATION_REFRESH_OPERATION_STATUS);
        refreshResult.setStatus(status);
        AsynchronousOperationResult asyncResult = AsynchronousOperationResult.wrap(refreshResult);
        updateQuantum(ctx, connector, asyncResult, parentResult); // We have to use parent result here because the result is closed.
        return asyncResult;
    }

    /**
     * Does _not_ close the operation result, just sets its status (and async operation reference).
     */
    static void computeResultStatus(OperationResult result) {
        if (result.isInProgress()) {
            return;
        }
        OperationResultStatus status = OperationResultStatus.SUCCESS;
        String asyncRef = null;
        for (OperationResult subresult : result.getSubresults()) {
            if (OPERATION_MODIFY_ENTITLEMENT.equals(subresult.getOperation()) && subresult.isError()) {
                status = OperationResultStatus.PARTIAL_ERROR;
            } else if (subresult.isError()) {
                status = OperationResultStatus.FATAL_ERROR;
            } else if (subresult.isInProgress()) {
                status = OperationResultStatus.IN_PROGRESS;
                asyncRef = subresult.getAsynchronousOperationReference();
            }
        }
        result.setStatus(status);
        result.setAsynchronousOperationReference(asyncRef);
    }

    static ObjectAlreadyExistsException objectAlreadyExistsException(
            String message, ProvisioningContext ctx, ConnectorInstance connector, ObjectAlreadyExistsException ex) {
        return new ObjectAlreadyExistsException(
                String.format("%sObject already exists on the resource (%s): %s",
                        message, ctx.getExceptionDescription(connector), ex.getMessage()),
                ex);
    }

    static GenericConnectorException genericConnectorException(
            ProvisioningContext ctx, ConnectorInstance connector, GenericFrameworkException ex) {
        return new GenericConnectorException(
                String.format("Generic error in connector (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage()),
                ex);
    }

    static CommunicationException communicationException(
            ProvisioningContext ctx, ConnectorInstance connector, CommunicationException ex) {
        return new CommunicationException(
                String.format("Error communicating with the resource (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage()),
                ex);
    }

    static ConfigurationException configurationException(
            ProvisioningContext ctx, ConnectorInstance connector, ConfigurationException ex) {
        return new ConfigurationException(
                String.format("Configuration error (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage()),
                ex);
    }
}
