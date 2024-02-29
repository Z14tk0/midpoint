/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Either a resource object, or a repository shadow (after being adopted by provisioning, i.e. with the definitions applied).
 *
 * Conditions:
 *
 * . the definition is known ({@link #getObjectDefinition()})
 * . the bean has the definitions applied
 * . the resource OID is filled-in (unless {@link #canHaveNoResourceOid()} is `true`).
 *
 * See {@link #checkConsistence()}.
 */
public interface AbstractShadow extends ShortDumpable, DebugDumpable, Cloneable {

    static AbstractShadow of(@NotNull ShadowType bean) {
        return new Impl(bean);
    }

    static AbstractShadow of(@NotNull PrismObject<ShadowType> prismObject) {
        return new Impl(prismObject.asObjectable());
    }

    /**
     * Returns the {@link ShadowType} bean backing this object.
     *
     * It should meet the criteria for individual subtypes.
     */
    @NotNull ShadowType getBean();

    default @Nullable String getOid() {
        return getBean().getOid();
    }

    /** Currently, returns "plain" reference (only type + OID). This may change in the future. Returns null if there's no OID. */
    default @Nullable ObjectReferenceType getRef() {
        var oid = getOid();
        return oid != null ? ObjectTypeUtil.createObjectRef(oid, ObjectTypes.SHADOW) : null;
    }

    /** Returns the definition corresponding to this shadow. */
    default @NotNull ResourceObjectDefinition getObjectDefinition() {
        return ShadowUtil.getResourceObjectDefinition(getBean());
    }

    default @NotNull PrismObject<ShadowType> getPrismObject() {
        return getBean().asPrismObject();
    }

    default boolean isDead() {
        return ShadowUtil.isDead(getBean());
    }

    default boolean doesExist() {
        return ShadowUtil.isExists(getBean());
    }

    default boolean isImmutable() {
        return getBean().isImmutable();
    }

    default void shortDump(StringBuilder sb) {
        sb.append(ShadowUtil.shortDumpShadow(getBean()));
    }

    default @Nullable ResourceObjectIdentifiers getIdentifiers() throws SchemaException {
        return ResourceObjectIdentifiers
                .optionalOf(getObjectDefinition(), getBean())
                .orElse(null);
    }

    default @NotNull Collection<ResourceAttribute<?>> getAllIdentifiers() {
        return ShadowUtil.getAllIdentifiers(getBean());
    }

    /** Returns the identifiers as a (detached) container, suitable e.g. for including into association value. */
    default @NotNull ResourceAttributeContainer getIdentifiersAsContainer() throws SchemaException {
        ResourceAttributeContainer identifiersContainer = ObjectFactory.createResourceAttributeContainer(
                ShadowAssociationValueType.F_IDENTIFIERS, getAttributesContainerDefinition());
        identifiersContainer.getValue().addAll(
                Item.cloneCollection(getAllIdentifiers()));
        return identifiersContainer;
    }

    default boolean hasPrimaryIdentifier() throws SchemaException {
        return getIdentifiers() instanceof ResourceObjectIdentifiers.WithPrimary;
    }

    default @NotNull ResourceObjectIdentifiers getIdentifiersRequired() throws SchemaException {
        return ResourceObjectIdentifiers.of(getObjectDefinition(), getBean());
    }

    default @NotNull ResourceObjectIdentification<?> getIdentificationRequired() throws SchemaException {
        return ResourceObjectIdentification.of(
                getObjectDefinition(),
                getIdentifiersRequired());
    }

    default <T> @Nullable ResourceAttribute<T> getPrimaryIdentifierAttribute() {
        //noinspection unchecked
        return (ResourceAttribute<T>) getAttributesContainer().getPrimaryIdentifier();
    }

    default @Nullable ResourceObjectIdentification.WithPrimary getPrimaryIdentification() throws SchemaException {
        var identification = getIdentification();
        return identification instanceof ResourceObjectIdentification.WithPrimary withPrimary ? withPrimary : null;
    }

    default @Nullable Object getPrimaryIdentifierValueFromAttributes() throws SchemaException {
        ResourceObjectIdentifiers identifiers = getIdentifiers();
        if (identifiers == null) {
            return null;
        }
        var primaryIdentifier = identifiers.getPrimaryIdentifier();
        if (primaryIdentifier == null) {
            return null;
        }
        return primaryIdentifier.getOrigValue();
    }

    default @Nullable ResourceObjectIdentification<?> getIdentification() throws SchemaException {
        var identifiers = getIdentifiers();
        if (identifiers != null) {
            return ResourceObjectIdentification.of(getObjectDefinition(), identifiers);
        } else {
            return null;
        }
    }

    /** Updates the in-memory representation. */
    default void updateWith(@NotNull Collection<? extends ItemDelta<?, ?>> modifications) throws SchemaException {
        ObjectDeltaUtil.applyTo(getPrismObject(), modifications);
    }

    /** Replaces the in-memory representation with the new content but the same definition. Returns a new instance. */
    @NotNull AbstractShadow withNewContent(@NotNull ShadowType newBean);

    /** Do not call if {@link #canHaveNoResourceOid()} is `true`. */
    default @NotNull String getResourceOid() {
        return MiscUtil.stateNonNull(
                Referencable.getOid(getBean().getResourceRef()),
                "No resource OID in %s", this);
    }

    default @NotNull QName getObjectClassName() throws SchemaException {
        return MiscUtil.stateNonNull(
                getBean().getObjectClass(),
                "No object class name in %s", this);
    }

    default PolyString determineShadowName() throws SchemaException {
        return ShadowUtil.determineShadowName(getBean());
    }

    default @NotNull ResourceAttributeContainer getAttributesContainer() {
        return MiscUtil.stateNonNull(
                ShadowUtil.getAttributesContainer(getBean()),
                "No attributes container in %s", this);
    }

    default @NotNull Collection<ResourceAttribute<?>> getAttributes() {
        return ShadowUtil.getAttributes(getBean());
    }

    /** Should correspond to {@link #getObjectDefinition()}. */
    default @NotNull ResourceAttributeContainerDefinition getAttributesContainerDefinition() {
        return Objects.requireNonNull(
                getAttributesContainer().getDefinition(),
                () -> "No attributes container definition in " + this);
    }

    default @Nullable <X> ResourceAttribute<X> findAttribute(@NotNull QName name) {
        return getAttributesContainer().findAttribute(name);
    }

    /** These checks are to be executed even in production (at least when creating the object). */
    default void checkConsistence() {
        getResourceOid(); // checks the presence
        stateCheck(getObjectDefinition() != null, "No object definition in %s", this);
        if (InternalsConfig.consistencyChecks) {
            getAttributesContainer().checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);
            checkAttributeDefinitions();
            ShadowAssociationsContainer associationsContainer = getAssociationsContainer();
            if (associationsContainer != null) {
                associationsContainer.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);
            }
        }
    }

    /** TODO merge with {@link #checkConsistence()} */
    default void checkConsistenceComplex(String desc) {
        checkConsistence();
        ShadowUtil.checkConsistence(getPrismObject(), desc);
    }

    default void checkAttributeDefinitions() {
        ResourceObjectDefinition objectDefinition = getObjectDefinition();
        for (ResourceAttribute<?> attribute : getAttributes()) {
            var attrDef = MiscUtil.stateNonNull(
                    attribute.getDefinition(),
                    "Attribute %s with no definition in %s", attribute, this);
            var attrDefFromObjectDef = objectDefinition.findAttributeDefinitionStrictlyRequired(attribute.getElementName());
            if (!attrDef.equals(attrDefFromObjectDef)) {
                throw new IllegalStateException(
                        "Attribute %s has a definition (%s) that does not match the one from object definition (%s from %s) in %s"
                                .formatted(attribute, attrDef, attrDefFromObjectDef, objectDefinition, this));
            }
        }
    }

    AbstractShadow clone();

    default void applyDefinition(@NotNull ResourceObjectDefinition newDefinition) throws SchemaException {
        getAttributesContainer().applyDefinition(
                newDefinition.toResourceAttributeContainerDefinition());
        checkConsistence();
    }

    default PolyStringType getName() {
        return getBean().getName();
    }

    default <T> @NotNull List<T> getAttributeRealValues(QName attrName) {
        return ShadowUtil.getAttributeValues(getPrismObject(), attrName);
    }

    default <T> @Nullable T getAttributeValue(QName attrName) {
        return MiscUtil.extractSingleton(getAttributeRealValues(attrName));
    }

    default @Nullable <T> ResourceAttribute<T> getAttribute(QName attrName) {
        return ShadowUtil.getAttribute(getPrismObject(), attrName);
    }

    default @NotNull <T> ResourceAttribute<T> getAttributeRequired(QName attrName) {
        return MiscUtil.stateNonNull(
                getAttribute(attrName),
                "No '%s' in %s", attrName, this);
    }

    /**
     * @see ShadowUtil#getAssociationsContainer(ShadowType)
     */
    default @Nullable ShadowAssociationsContainer getAssociationsContainer() {
        return ShadowUtil.getAssociationsContainer(getBean());
    }

    /**
     * Similar to {@link #getAssociationsContainer()} but never returns `null`.
     *
     * @see ShadowUtil#getAssociations(ShadowType)
     */
    default @NotNull Collection<ShadowAssociation> getAssociations() {
        return ShadowUtil.getAssociations(getBean());
    }

    default @Nullable ShadowKindType getKind() {
        return getBean().getKind();
    }

    default @NotNull QName getObjectClass() {
        return MiscUtil.stateNonNull(
                getBean().getObjectClass(),
                "No object class in %s", this);
    }

    default boolean isProtectedObject() {
        return BooleanUtils.isTrue(getBean().isProtectedObject());
    }

    default void applyDelta(@NotNull ItemDelta<?, ?> itemDelta) throws SchemaException {
        itemDelta.applyTo(getPrismObject());
    }

    default Object getHumanReadableNameLazily() {
        return ShadowUtil.getHumanReadableNameLazily(getPrismObject());
    }

    default boolean canHaveNoResourceOid() {
        return false;
    }

    /**
     * The default implementation. Other specific implementations reside in particular modules like `provisioning-impl`.
     * (At least for now, until they are stabilized and proved to be generally useful.
     */
    class Impl implements AbstractShadow {

        @NotNull private final ShadowType bean;

        private Impl(@NotNull ShadowType bean) {
            this.bean = bean;
            checkConsistence();
        }

        @Override
        public @NotNull ShadowType getBean() {
            return bean;
        }

        @Override
        public @NotNull AbstractShadow withNewContent(@NotNull ShadowType newBean) {
            return new Impl(newBean);
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public AbstractShadow clone() {
            return new Impl(bean.clone());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Impl impl = (Impl) o;
            return Objects.equals(bean, impl.bean);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bean);
        }

        @Override
        public String toString() {
            return shortDump();
        }

        @Override
        public String debugDump(int indent) {
            return bean.debugDump(indent); // at least for now
        }
    }
}