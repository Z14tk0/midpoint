/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class ShadowAuditHelper {

    @Autowired private AuditHelper auditHelper;
    @Autowired private PrismContext prismContext;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    public void auditEvent(AuditEventType event, ShadowType shadow, ProvisioningContext ctx, OperationResult result) {
        auditEvent(event, shadow, null, ctx, result);
    }

    public void auditEvent(AuditEventType event, ShadowType shadow, Collection<Operation> operationsWave, ProvisioningContext ctx, OperationResult result) {
        Task task = ctx.getTask();

        SystemConfigurationType config;
        try {
            config = systemObjectCache.getSystemConfigurationBean(result);
        } catch (Exception ex) {
            throw new SystemException("Couldn't load system configuration", ex);
        }

        if (!isEnhancedShadowAuditingEnabled(config)) {
            return;
        }

        ProvisioningOperationContext operationContext = ctx.getOperationContext();
        if (operationContext == null) {
            operationContext = new ProvisioningOperationContext();
        }

        AuditEventRecord auditRecord = new AuditEventRecord(event, AuditEventStage.RESOURCE);
        auditRecord.setRequestIdentifier(operationContext.requestIdentifier());
        if (shadow != null) {   // todo if shadow is null check shadow manager to get it by identifier (or something like that), or check EntitlementConverter - it should know shadow oid
            auditRecord.setTargetRef(new ObjectReferenceType()
                    .oid(shadow.getOid())
                    .targetName(shadow.getName())
                    .type(ShadowType.COMPLEX_TYPE).asReferenceValue());
        }
        auditRecord.setTimestamp(Clock.get().currentTimeMillis());
        auditRecord.setChannel(task.getChannel());

        ObjectDeltaOperation<ShadowType> delta = createDelta(event, shadow, operationsWave);
        if (delta != null) {
            auditRecord.addDelta(delta);
        }

        boolean recordResourceOids;
        List<SystemConfigurationAuditEventRecordingPropertyType> propertiesToRecord;
        ExpressionType eventRecordingExpression = null;

        if (config != null && config.getAudit() != null && config.getAudit().getEventRecording() != null) {
            SystemConfigurationAuditEventRecordingType eventRecording = config.getAudit().getEventRecording();
            recordResourceOids = Boolean.TRUE.equals(eventRecording.isRecordResourceOids());
            propertiesToRecord = eventRecording.getProperty();
            eventRecordingExpression = eventRecording.getExpression();
        } else {
            recordResourceOids = false;
            propertiesToRecord = emptyList();
        }

//        addRecordMessage(auditRecord, clone.getMessage());    // todo add message and other fields to event
        if (recordResourceOids) {
            auditRecord.addResourceOid(ctx.getResourceOid());
        }

        PrismObject<ShadowType> object = shadow != null ? shadow.asPrismObject() : null;

        for (SystemConfigurationAuditEventRecordingPropertyType property : propertiesToRecord) {
            auditHelper.evaluateAuditRecordProperty(property, auditRecord, object,
                    operationContext.expressionProfile(), task, result);
        }

        if (eventRecordingExpression != null) {
            // MID-6839
            auditRecord = auditHelper.evaluateRecordingExpression(eventRecordingExpression, auditRecord, object,
                    operationContext.expressionProfile(), operationContext.expressionEnvironmentSupplier(), ctx.getTask(), result);
        }

        if (auditRecord == null) {
            return;
        }

        ObjectDeltaSchemaLevelUtil.NameResolver nameResolver = (objectClass, oid) -> repositoryService.getObject(objectClass, oid, null, result).getName();

        auditHelper.audit(auditRecord, nameResolver, ctx.getTask(), result);
    }

    private ObjectDeltaOperation<ShadowType> createDelta(AuditEventType event, ShadowType shadow, Collection<Operation> operations) {
        if (event == AuditEventType.ADD_OBJECT) {
            ObjectDelta<ShadowType> delta = DeltaFactory.Object.createAddDelta(shadow.asPrismObject());
            return new ObjectDeltaOperation<>(delta);
        } else if (event == AuditEventType.DELETE_OBJECT) {
            ObjectDelta<ShadowType> delta = prismContext
                    .deltaFactory()
                    .object()
                    .createDeleteDelta(ShadowType.class, shadow.getOid());

            return new ObjectDeltaOperation<>(delta);
        } else if (event == AuditEventType.MODIFY_OBJECT) {
            // todo what if shadow oid is null?
            if (shadow != null) {
                ObjectDelta<ShadowType> delta = prismContext
                        .deltaFactory()
                        .object()
                        .createEmptyDelta(ShadowType.class, shadow.getOid(), ChangeType.MODIFY);
                for (Operation operation : operations) {
//                    operation.asBean(prismContext).
                }
            }
        } else if (event == AuditEventType.DISCOVER_OBJECT) {
            // todo this currently can happen, why? where to figure out that's it's discover object?

        }

        return null;
    }

    private boolean isEnhancedShadowAuditingEnabled(SystemConfigurationType config) {
        if (config == null || config.getAudit() == null) {
            return false;
        }

        SystemConfigurationAuditEventRecordingType eventRecording = config.getAudit().getEventRecording();
        return BooleanUtils.isTrue(eventRecording.isRecordEnhancedShadowChanges());
    }
}
