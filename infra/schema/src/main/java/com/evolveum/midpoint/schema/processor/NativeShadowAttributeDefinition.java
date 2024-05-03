/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.ShortDumpable;

public interface NativeShadowAttributeDefinition extends
        Cloneable, Freezable, Serializable, ShortDumpable,
        PrismItemBasicDefinition,
        PrismItemAccessDefinition,
        PrismItemMiscDefinition,
        PrismPresentationDefinition,
        ShadowItemUcfDefinition {

    @Nullable
    ShadowReferenceParticipantRole getReferenceParticipantRoleIfPresent();

    @NotNull
    ShadowReferenceParticipantRole getReferenceParticipantRole();

    NativeShadowAttributeDefinition clone();

    interface NativeShadowAttributeDefinitionBuilder extends ItemDefinition.ItemDefinitionLikeBuilder {

        void setNativeAttributeName(String value);
        void setFrameworkAttributeName(String value);
        void setReturnedByDefault(Boolean value);
        void setReferenceParticipantRole(ShadowReferenceParticipantRole value);
    }
}
