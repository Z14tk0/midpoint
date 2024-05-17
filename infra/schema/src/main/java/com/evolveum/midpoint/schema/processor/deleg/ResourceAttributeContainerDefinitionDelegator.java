package com.evolveum.midpoint.schema.processor.deleg;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.deleg.ContainerDefinitionDelegator;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

public interface ResourceAttributeContainerDefinitionDelegator
        extends ContainerDefinitionDelegator<ShadowAttributesType>, ResourceAttributeContainerDefinition {

    @Override
    ResourceAttributeContainerDefinition delegate();

    @Override
    default Collection<? extends ShadowSimpleAttributeDefinition<?>> getPrimaryIdentifiers() {
        return delegate().getPrimaryIdentifiers();
    }

    @Override
    default Collection<? extends ShadowSimpleAttributeDefinition<?>> getSecondaryIdentifiers() {
        return delegate().getSecondaryIdentifiers();
    }

    @Override
    default Collection<? extends ShadowSimpleAttributeDefinition<?>> getAllIdentifiers() {
        return delegate().getAllIdentifiers();
    }

    @Override
    default ShadowSimpleAttributeDefinition<?> findAttributeDefinition(ItemPath elementPath) {
        return delegate().findAttributeDefinition(elementPath);
    }

    @Override
    default @NotNull ShadowAttributesContainer instantiate() {
        return delegate().instantiate();
    }

    @Override
    default @NotNull ShadowAttributesContainer instantiate(QName name) {
        return delegate().instantiate(name);
    }

    @Override
    default @NotNull List<? extends ShadowSimpleAttributeDefinition<?>> getDefinitions() {
        return delegate().getDefinitions();
    }

    @Override
    default ShadowAttributesComplexTypeDefinition getComplexTypeDefinition() {
        return delegate().getComplexTypeDefinition();
    }

}
