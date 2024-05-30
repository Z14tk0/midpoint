/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.input.SourceMappingProvider;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class SourceOrTargetOfAssociationMappingPanelFactory extends SourceOrTargetOfMappingPanelFactory implements Serializable {

    protected ItemPath createTargetPath(ItemPath containerPath) {
        return ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_ASSOCIATION_TYPE,
                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                containerPath,
                ResourceAttributeDefinitionType.F_INBOUND,
                InboundMappingType.F_TARGET);
    }

    protected ItemPath createSourcePath(ItemPath containerPath) {
        return ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_ASSOCIATION_TYPE,
                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                containerPath,
                ResourceAttributeDefinitionType.F_OUTBOUND,
                InboundMappingType.F_SOURCE);
    }

    protected SourceMappingProvider createProvider(IModel<PrismPropertyWrapper<VariableBindingDefinitionType>> itemWrapperModel) {
        return new SourceMappingProvider(itemWrapperModel) {
            @Override
            protected PrismContainerDefinition<? extends Containerable> getFocusTypeDefinition(ResourceObjectTypeDefinitionType resourceObjectType) {
                return PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AssignmentType.class);
            }
        };
    }
}