/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.prism.util.CloneUtil.toImmutable;

import java.util.Collection;
import java.util.Map;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.impl.delta.PropertyDeltaImpl;
import com.evolveum.midpoint.prism.impl.match.MatchingRuleRegistryImpl;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelatorDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

/**
 * An attribute definition (obtained typically from the connector),
 * optionally refined by information from `schemaHandling` section of a resource definition.
 *
 * The implementation consists of a pair of {@link #nativeDefinition} and {@link #customizationBean},
 * plus some auxiliary information for faster access.
 *
 * This class intentionally does NOT inherit from {@link PrismPropertyDefinitionImpl}. Instead, a large part of the required
 * functionality is delegated to {@link #nativeDefinition} which provides analogous functionality.
 *
 * @see NativeShadowAttributeDefinition
 */
public class ResourceAttributeDefinitionImpl<T>
        extends ShadowItemDefinitionImpl<ResourceAttribute<T>, T, NativeShadowAttributeDefinition<T>, ResourceAttributeDefinitionType>
        implements ResourceAttributeDefinition<T>, ShadowItemDefinitionTemp {

    private ResourceAttributeDefinitionImpl(
            @NotNull NativeShadowAttributeDefinition<T> nativeDefinition,
            @NotNull ResourceAttributeDefinitionType customizationBean,
            boolean ignored) throws SchemaException {
        super(nativeDefinition, customizationBean, ignored);
    }

    private ResourceAttributeDefinitionImpl(
            @NotNull LayerType layer,
            @NotNull NativeShadowAttributeDefinition<T> nativeDefinition,
            @NotNull ResourceAttributeDefinitionType customizationBean,
            @NotNull Map<LayerType, PropertyLimitations> limitationsMap,
            @NotNull PropertyAccessType accessOverride) {
        super(layer, nativeDefinition, customizationBean, limitationsMap, accessOverride);
    }

    @Override
    ResourceAttribute<T> instantiateFromQualifiedName(QName name) {
        return new ResourceAttributeImpl<>(name, this);
    }

    /**
     * This is the main creation point.
     *
     * @throws SchemaException If there's a problem with the customization bean.
     */
    public static <T> ResourceAttributeDefinition<T> create(
            @NotNull NativeShadowAttributeDefinition<T> nativeDefinition,
            @Nullable ResourceAttributeDefinitionType customizationBean,
            boolean ignored)
            throws SchemaException {

        return new ResourceAttributeDefinitionImpl<>(
                toImmutable(nativeDefinition),
                toImmutable(customizationBean != null ?
                        customizationBean : new ResourceAttributeDefinitionType()),
                ignored);
    }

    /** This is the creation point from native form only. */
    public static <T> ResourceAttributeDefinition<T> create(
            @NotNull NativeShadowAttributeDefinition<T> nativeDefinition) throws SchemaException {
        return create(nativeDefinition, null, false);
    }

    public @NotNull ResourceAttributeDefinitionImpl<T> forLayer(@NotNull LayerType layer) {
        if (layer == currentLayer) {
            return this;
        } else {
            return new ResourceAttributeDefinitionImpl<>(
                    layer,
                    nativeDefinition,
                    customizationBean,
                    limitationsMap,
                    accessOverride.clone() // TODO do we want to preserve also the access override?
            );
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @NotNull
    @Override
    public ResourceAttributeDefinitionImpl<T> clone() {
        return new ResourceAttributeDefinitionImpl<>(
                currentLayer,
                nativeDefinition,
                customizationBean,
                limitationsMap,
                accessOverride.clone());
    }

    @Override
    public Boolean isSecondaryIdentifierOverride() {
        return customizationBean.isSecondaryIdentifier();
    }

    @Override
    public QName getMatchingRuleQName() {
        return MiscUtil.orElseGet(
                customizationBean.getMatchingRule(),
                nativeDefinition::getMatchingRuleQName);
    }

    @Override
    public @NotNull MatchingRule<T> getMatchingRule() {
        return MatchingRuleRegistryImpl.instance()
                .getMatchingRuleSafe(getMatchingRuleQName(), getTypeName());
    }

    @Override
    public @NotNull Class<T> getTypeClass() {
        return XsdTypeMapper.toJavaType(getTypeName()); // FIXME
    }

    @Override
    public @Nullable Collection<? extends DisplayableValue<T>> getAllowedValues() {
        return nativeDefinition.getAllowedValues();
    }

    @Override
    public @Nullable Collection<? extends DisplayableValue<T>> getSuggestedValues() {
        return nativeDefinition.getSuggestedValues();
    }

    @Override
    public @Nullable T defaultValue() {
        return nativeDefinition.defaultValue();
    }

    @Override
    public boolean isDisplayNameAttribute() {
        return Boolean.TRUE.equals(
                customizationBean.isDisplayNameAttribute());
    }

    @Override
    public @Nullable ItemCorrelatorDefinitionType getCorrelatorDefinition() {
        return customizationBean.getCorrelator();
    }

    @Override
    public ResourceAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation) {
        // No deep cloning, because the constituents are immutable.
        return clone();
    }

    @Override
    public void revive(PrismContext prismContext) {
        // TODO is this [still] needed?
        customizationBean.asPrismContainerValue().revive(prismContext);
    }

    @Override
    public void debugDumpShortToString(StringBuilder sb) {
        sb.append(this); // TODO improve if needed
    }

    @Override
    public boolean isValidFor(@NotNull QName elementQName, @NotNull Class<? extends ItemDefinition<?>> clazz, boolean caseInsensitive) {
        return clazz.isAssignableFrom(this.getClass())
                && QNameUtil.match(elementQName, getItemName(), caseInsensitive);
    }

    @Override
    public <T2 extends ItemDefinition<?>> T2 findItemDefinition(@NotNull ItemPath path, @NotNull Class<T2> clazz) {
        //noinspection unchecked
        return LivePrismItemDefinition.matchesThisDefinition(path, clazz, this) ? (T2) this : null;
    }

    @Override
    public @NotNull PropertyDelta<T> createEmptyDelta(ItemPath path) {
        return new PropertyDeltaImpl<>(path, this);
    }

    @Override
    public @NotNull PrismPropertyDefinition.PrismPropertyDefinitionMutator<T> mutator() {
        throw new UnsupportedOperationException("Refined attribute definition can not be mutated: " + this);
    }

    @Override
    protected void extendToString(StringBuilder sb) {
        var matchingRuleQName = getMatchingRuleQName();
        if (matchingRuleQName != null) {
            sb.append(",MR=").append(PrettyPrinter.prettyPrint(matchingRuleQName));
        }
    }

    @Override
    public @NotNull ItemDefinition<PrismProperty<T>> cloneWithNewName(@NotNull ItemName itemName) {
        throw new UnsupportedOperationException("Implement if needed");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceAttributeDefinitionImpl<?> that)) {
            return false;
        }
        return super.equals(o); // no own fields to compare
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String getHumanReadableDescription() {
        return toString(); // FIXME
    }
}
