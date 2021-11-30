/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.ResourceObjectClass;
import com.evolveum.midpoint.model.impl.sync.tasks.SynchronizationObjectsFilterImpl;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReconciliationWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Code common to all three reconciliation sub-activities: operation completion, resource reconciliation,
 * and remaining shadows reconciliation.
 */
public abstract class PartialReconciliationActivityRun
        extends SearchBasedActivityRun
        <ShadowType,
                ReconciliationWorkDefinition,
                ReconciliationActivityHandler,
                ReconciliationWorkStateType> {

    /** Object class to reconcile (resource + OC + kind + intent). */
    ResourceObjectClass resourceObjectClass;

    /** Post-search filter (currently OC, kind, intent). */
    SynchronizationObjectsFilterImpl objectsFilter;

    PartialReconciliationActivityRun(
            @NotNull ActivityRunInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> activityRun,
            String shortNameCapitalized) {
        super(activityRun, shortNameCapitalized);
    }

    @Override
    public void beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        ResourceObjectSetType resourceObjectSet = getResourceObjectSet();

        resourceObjectClass = getModelBeans().syncTaskHelper
                .getResourceObjectClassCheckingMaintenance(resourceObjectSet, getRunningTask(), result);
        objectsFilter = resourceObjectClass.getObjectFilter();

        setContextDescription(getShortName() + " on " + resourceObjectClass.getContextDescription()); // TODO?
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return resourceObjectClass.getResourceRef();
    }

    protected @NotNull ResourceObjectSetType getResourceObjectSet() {
        return getWorkDefinition().getResourceObjectSetSpecification();
    }

    @Override
    public ActivityState useOtherActivityStateForCounters(@NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return getActivityState().getParentActivityState(ReconciliationWorkStateType.COMPLEX_TYPE, result);
    }

    protected @NotNull ModelBeans getModelBeans() {
        return getActivityHandler().getModelBeans();
    }
}
