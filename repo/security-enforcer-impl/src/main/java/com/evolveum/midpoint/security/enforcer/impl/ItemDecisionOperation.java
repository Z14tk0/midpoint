/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.security.api.AuthorizationConstants.EXECUTION_ITEMS_ALLOWED_BY_DEFAULT;
import static com.evolveum.midpoint.security.api.AuthorizationConstants.OPERATIONAL_ITEMS_ALLOWED_FOR_CONTAINER_DELETE;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType.EXECUTION;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

/** TODO polish */
class ItemDecisionOperation {

    /** Using {@link SecurityEnforcerImpl} to ensure log compatibility. */
    private static final Trace LOGGER = TraceManager.getTrace(SecurityEnforcerImpl.class);

    AccessDecision onAllowedItems(
            @NotNull AutzItemPaths allowedItems,
            @NotNull AuthorizationPhaseType phase,
            @NotNull AuthorizationParameters<?, ?> params) {

        ItemDecisionFunction itemDecisionFunction =
                (itemPath, removingContainer) ->
                        decideUsingAllowedItems(itemPath, allowedItems, phase, removingContainer);
        if (params.hasDelta()) {
            // Behave as if this is execution phase for delete delta authorizations.
            // We do not want to avoid deleting objects just because there are automatic/operational items that were
            // generated by midPoint. Otherwise, we won't be really able to delete any object.
            ItemDecisionFunction itemDecisionFunctionDelete =
                    (itemPath, removingContainer) ->
                            decideUsingAllowedItems(itemPath, allowedItems, EXECUTION, removingContainer);
            return decideOnDeltaByItems(
                    params.getDelta(), params.getOldObject(), itemDecisionFunction, itemDecisionFunctionDelete);
        } else if (params.hasObject()) {
            return decideOnObjectValueByItems(params.getAnyObjectValue(), itemDecisionFunction);
        } else {
            return null;
        }
    }

    private @NotNull AccessDecision decideUsingAllowedItems(
            @NotNull ItemPath itemPath,
            @NotNull AutzItemPaths allowedItems,
            @NotNull AuthorizationPhaseType phase,
            boolean removingContainer) {
        if (isAllowedByDefault(itemPath, phase, removingContainer)
                || allowedItems.includes(itemPath)) {
            return AccessDecision.ALLOW;
        } else {
            return AccessDecision.DEFAULT;
        }
    }

    private boolean isAllowedByDefault(ItemPath nameOnlyItemPath, AuthorizationPhaseType phase, boolean removingContainer) {
        return removingContainer && OPERATIONAL_ITEMS_ALLOWED_FOR_CONTAINER_DELETE.containsSubpathOrEquivalent(nameOnlyItemPath)
                || phase == EXECUTION && EXECUTION_ITEMS_ALLOWED_BY_DEFAULT.containsSubpathOrEquivalent(nameOnlyItemPath);
    }

    /**
     * Is the execution of the delta allowed, regarding given "item decision function(s)"?
     *
     * The currentObject parameter is the state of the object as we have seen it (the more recent the better).
     * This is used to check authorization for id-only delete deltas and replace deltas for containers.
     *
     * - `currentObject` should not be null except for `ADD` deltas
     *
     * @see #decideOnContainerValueByItems(PrismContainerValue, ItemDecisionFunction, boolean, String)
     */
    private @Nullable AccessDecision decideOnDeltaByItems(
            @NotNull ObjectDelta<?> delta, PrismObject<?> currentObject,
            ItemDecisionFunction itemDecisionFunction, ItemDecisionFunction itemDecisionFunctionDelete) {
        if (delta.isAdd()) {
            return decideOnObjectValueByItems(delta.getObjectToAdd().getValue(), itemDecisionFunction);
        } else if (delta.isDelete()) {
            return decideOnObjectValueByItems(currentObject.getValue(), itemDecisionFunctionDelete);
        } else {
            AccessDecision decision = null;
            for (ItemDelta<?, ?> modification : delta.getModifications()) {
                ItemPath itemPath = modification.getPath();
                AccessDecision modDecision = itemDecisionFunction.decide(itemPath.namedSegmentsOnly(), false);
                if (modDecision == null) {
                    // null decision means: skip this modification
                    continue;
                }
                if (modDecision == AccessDecision.DEFAULT && modification instanceof ContainerDelta<?>) {
                    // No decision for entire container. Sub-items will dictate the decision.
                    AccessDecision subDecision =
                            decideOnContainerDeltaByItems((ContainerDelta<?>) modification, currentObject, itemDecisionFunction);
                    decision = AccessDecision.combine(decision, subDecision);
                } else {
                    if (modDecision == AccessDecision.DENY) {
                        LOGGER.trace("  DENY operation because item {} in the delta is not allowed", itemPath);
                        // We do not want to break the loop immediately here. We want all the denied items to get logged
                    }
                    decision = AccessDecision.combine(decision, modDecision);
                }
            }
            return decision;
        }
    }

    private AccessDecision decideOnContainerDeltaByItems(
            ContainerDelta<?> delta, PrismObject<?> currentObject, ItemDecisionFunction itemDecisionFunction) {
        AccessDecision decision = null;
        ItemPath path = delta.getPath();

        // Everything is plain and simple for add. No need for any additional checks.
        for (PrismContainerValue<?> valueToAdd : emptyIfNull(delta.getValuesToAdd())) {
            AccessDecision valueDecision = decideOnContainerValueByItems(
                    valueToAdd, itemDecisionFunction, false, "delta add");
            decision = AccessDecision.combine(decision, valueDecision);
        }

        // For deleted container values watch out for id-only deltas. Those deltas do not have
        // any sub-items in them. So we need to use data from currentObject for autz evaluation.
        for (PrismContainerValue<?> valueToDelete : emptyIfNull(delta.getValuesToDelete())) {
            AccessDecision valueDecision = null;
            if (valueToDelete.isIdOnly()) {
                PrismContainerValue<?> fullValueToDelete =
                        determineContainerValueFromCurrentObject(path, valueToDelete.getId(), currentObject);
                if (fullValueToDelete != null) {
                    valueDecision = decideOnContainerValueByItems(
                            fullValueToDelete,
                            itemDecisionFunction,
                            true,
                            "delta delete (current value)");
                }
            } else {
                valueDecision = decideOnContainerValueByItems(
                        valueToDelete, itemDecisionFunction, true, "delta delete");
            }
            decision = AccessDecision.combine(decision, valueDecision);
        }

        // Values to replace should pass the ordinary check. But we also need to check old values
        // in currentObject, because those values are effectively deleted.
        var valuesToReplace = delta.getValuesToReplace();
        if (valuesToReplace != null) {
            for (PrismContainerValue<?> valueToReplace : valuesToReplace) {
                AccessDecision valueDecision = decideOnContainerValueByItems(
                        valueToReplace, itemDecisionFunction, false, "delta replace");
                decision = AccessDecision.combine(decision, valueDecision);
            }
            var oldValues = determineContainerValuesFromCurrentObject(path, currentObject);
            for (PrismContainerValue<?> oldValue : emptyIfNull(oldValues)) {
                AccessDecision oldValueDecision = decideOnContainerValueByItems(
                        oldValue, itemDecisionFunction, true, "delta replace (removed current value)");
                decision = AccessDecision.combine(decision, oldValueDecision);
            }
        }

        return decision;
    }

    private PrismContainerValue<?> determineContainerValueFromCurrentObject(
            ItemPath path, long id, PrismObject<?> currentObject) {
        var oldValues = determineContainerValuesFromCurrentObject(path, currentObject);
        for (PrismContainerValue<?> oldValue : emptyIfNull(oldValues)) {
            if (id == oldValue.getId()) {
                return oldValue;
            }
        }
        return null;
    }

    private <C extends Containerable> Collection<PrismContainerValue<C>> determineContainerValuesFromCurrentObject(
            ItemPath path, PrismObject<?> currentObject) {
        PrismContainer<C> container = currentObject.findContainer(path);
        return container != null ? container.getValues() : null;
    }

    /**
     * Is the operation on given object allowed, regarding given "item decision function"?
     *
     * Contains special treatment regarding empty PCVs.
     */
    private AccessDecision decideOnObjectValueByItems(
            PrismContainerValue<?> value, ItemDecisionFunction itemDecisionFunction) {
        AccessDecision decision =
                decideOnContainerValueByItems(
                        value, itemDecisionFunction, false, "object");
        if (decision == null && value.hasNoItems()) {
            // There are no items in the object. Therefore there is no item that is allowed. Therefore decision is DEFAULT.
            // But also there is no item that is denied or not allowed.
            // This is a corner case. But this approach is often used by GUI to determine if
            // a specific class of object is allowed, e.g. if it is allowed to create (some) roles. This is used to
            // determine whether to display a particular menu item.
            // Therefore we should allow such cases.
            return AccessDecision.ALLOW;
        } else {
            return decision;
        }
    }

    /**
     * Can we apply the operation on container value, concerning given item decision function?
     *
     * Any {@link AccessDecision#DENY} item means denying the whole value.
     *
     * @see #decideOnDeltaByItems(ObjectDelta, PrismObject, ItemDecisionFunction, ItemDecisionFunction)
     */
    private AccessDecision decideOnContainerValueByItems(
            @NotNull PrismContainerValue<?> containerValue,
            ItemDecisionFunction itemDecisionFunction, boolean removingContainerValue, String contextDesc) {
        AccessDecision decision = null;
        // TODO: problem with empty containers such as orderConstraint in assignment. Skip all empty items ... for now.
        for (Item<?, ?> item : containerValue.getItems()) {
            ItemPath itemPath = item.getPath();
            AccessDecision itemDecision = itemDecisionFunction.decide(itemPath.namedSegmentsOnly(), removingContainerValue);
            logContainerValueItemDecision(itemDecision, contextDesc, itemPath);
            if (itemDecision == null) {
                // null decision means: skip this item
                continue;
            }
            if (itemDecision == AccessDecision.DEFAULT && item instanceof PrismContainer<?>) {
                // No decision for entire container. Sub-items will dictate the decision.
                //noinspection unchecked,rawtypes
                List<PrismContainerValue<?>> itemValues = (List) ((PrismContainer<?>) item).getValues();
                for (PrismContainerValue<?> itemValue : itemValues) {
                    AccessDecision itemValueDecision = decideOnContainerValueByItems(
                            itemValue, itemDecisionFunction, removingContainerValue, contextDesc);
                    // No need to compute decision per value, as the "combine" operation is associative.
                    decision = AccessDecision.combine(decision, itemValueDecision);
                    // We do not want to break the loop immediately here even if the decision would be DENY.
                    // We want all the denied items to get logged.
                }
            } else {
                if (itemDecision == AccessDecision.DENY) {
                    LOGGER.trace("  DENY operation because item {} in the object is not allowed", itemPath);
                    // We do not want to break the loop immediately here. We want all the denied items to get logged.
                }
                decision = AccessDecision.combine(decision, itemDecision);
            }
        }
        logContainerValueDecision(decision, contextDesc, containerValue);
        return decision;
    }

    private void logContainerValueItemDecision(AccessDecision subDecision, String contextDesc, ItemPath path) {
        if (LOGGER.isTraceEnabled()) { // TODO get ctx here
            if (subDecision != AccessDecision.ALLOW || InternalsConfig.isDetailedAuthorizationLog()) {
                LOGGER.trace("    item {} for {}: decision={}", path, contextDesc, subDecision);
            }
        }
    }

    private void logContainerValueDecision(
            AccessDecision decision, String contextDesc, PrismContainerValue<?> cval) {
        if (LOGGER.isTraceEnabled()) { // TODO get ctx here
            if (decision != AccessDecision.ALLOW || InternalsConfig.isDetailedAuthorizationLog()) {
                LOGGER.trace("    container {} for {} (processed sub-items): decision={}", cval.getPath(), contextDesc, decision);
            }
        }
    }

    <O extends ObjectType> AccessDecision onSecurityConstraints(
            ObjectSecurityConstraints securityConstraints,
            ObjectDelta<O> delta,
            PrismObject<O> currentObject,
            String operationUrl,
            AuthorizationPhaseType phase,
            ItemPath itemPath) {
        ItemDecisionFunction itemDecisionFunction = (nameOnlyItemPath, removingContainer) ->
                decideUsingSecurityConstraints(
                        nameOnlyItemPath, removingContainer, securityConstraints, operationUrl, phase, itemPath);
        ItemDecisionFunction itemDecisionFunctionDelete = (nameOnlyItemPath, removingContainer) ->
                decideUsingSecurityConstraints(
                        nameOnlyItemPath, removingContainer, securityConstraints, operationUrl, EXECUTION, itemPath);
        return decideOnDeltaByItems(delta, currentObject, itemDecisionFunction, itemDecisionFunctionDelete);
    }

    private @Nullable AccessDecision decideUsingSecurityConstraints(
            @NotNull ItemPath nameOnlySubItemPath,
            boolean removingContainer,
            @NotNull ObjectSecurityConstraints securityConstraints,
            @NotNull String operationUrl,
            AuthorizationPhaseType phase,
            @Nullable ItemPath itemRootPath) {
        if (isAllowedByDefault(nameOnlySubItemPath, phase, removingContainer)) {
            return null;
        }
        if (itemRootPath != null && !itemRootPath.isSubPathOrEquivalent(nameOnlySubItemPath)) {
            return null;
        }
        return AccessDecision.translate(
                securityConstraints.findItemDecision(nameOnlySubItemPath, operationUrl, phase));
    }

    /** TODO rename */
    AccessDecision onSecurityConstraints2(
            ObjectSecurityConstraints securityConstraints,
            PrismContainerValue<?> containerValue,
            boolean removingContainer,
            String operationUrl,
            AuthorizationPhaseType phase,
            @Nullable ItemPath itemPath,
            String decisionContextDesc) {
        return decideOnContainerValueByItems(
                containerValue,
                (nameOnlyItemPath, lRemovingContainer) -> decideUsingSecurityConstraints(
                        nameOnlyItemPath, lRemovingContainer, securityConstraints, operationUrl, phase, itemPath),
                removingContainer, decisionContextDesc);
    }
}
