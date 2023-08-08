/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_C;

/**
 * Work definition that can provide object set specification.
 *
 * It has to be aware of an activity type name because of the default implementation of {@link #getAffectedObjects()}.
 */
public interface ObjectSetSpecificationProvider
        extends ActivityTypeNameAware, AffectedObjectsProvider, FailedObjectsSelectorProvider {

    @NotNull ObjectSetType getObjectSetSpecification();

    /** Provided here to avoid code duplication in individual work definition implementations. */
    @Override
    default @NotNull TaskAffectedObjectsType getAffectedObjects() {
        ObjectSetType set = getObjectSetSpecification();

        // Currently, all objects are in this namespace (otherwise we should call schema registry,
        // but there's no fully suitable method now)
        var rawType = set.getType();
        QName qualifiedType = rawType != null ? QNameUtil.qualifyIfNeeded(rawType, NS_C) : null;

        return new TaskAffectedObjectsType()
                .activity(new ActivityAffectedObjectsType()
                        .activityType(getActivityTypeName())
                        .objects(new BasicObjectSetType()
                                .type(qualifiedType)
                                .archetypeRef( // Consider keeping only the OID
                                        CloneUtil.cloneCloneable(set.getArchetypeRef()))
                        )
                );
    }

    @Override
    default FailedObjectsSelectorType getFailedObjectsSelector() {
        return getObjectSetSpecification().getFailedObjectsSelector();
    }
}
