/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Provides information about resource object attributes.
 */
public interface AttributeDefinitionStore
    extends LocalItemDefinitionStore {

    /**
     * Returns all attribute definitions as an unmodifiable collection.
     * Should be the same content as returned by `getDefinitions`.
     *
     * The returned value is a {@link List} because of the contract of {@link ComplexTypeDefinition#getDefinitions()}.
     */
    @NotNull List<? extends ShadowSimpleAttributeDefinition<?>> getAttributeDefinitions();

    /**
     * Returns all attribute definitions of given type as an unmodifiable collection.
     *
     */
    default @NotNull <AD extends ShadowSimpleAttributeDefinition<?>> Collection<AD> getAttributeDefinitions(Class<AD> type) {
        //noinspection unchecked
        return getAttributeDefinitions().stream()
                .filter(def -> type.isAssignableFrom(def.getClass()))
                .map(def -> (AD) def)
                .toList();
    }

    /**
     * Finds a definition of an attribute with a given name. Returns null if nothing is found.
     */
    default @Nullable <T> ShadowSimpleAttributeDefinition<T> findSimpleAttributeDefinition(QName name) {
        return findSimpleAttributeDefinition(name, false);
    }

    /**
     * Finds a definition of an attribute with a given name. Throws {@link SchemaException} if it's not there.
     */
    default @NotNull <T> ShadowSimpleAttributeDefinition<T> findSimpleAttributeDefinitionRequired(@NotNull QName name)
            throws SchemaException {
        return findSimpleAttributeDefinitionRequired(name, () -> "");
    }

    /**
     * Finds a definition of an attribute with a given name. Throws {@link IllegalStateException} if it's not there.
     */
    default @NotNull ShadowSimpleAttributeDefinition<?> findSimpleAttributeDefinitionStrictlyRequired(@NotNull QName name) {
        return findSimpleAttributeDefinitionStrictlyRequired(name, () -> "");
    }

    /**
     * Finds a definition of an attribute with a given name. Throws {@link SchemaException} if it's not there.
     */
    default @NotNull <T> ShadowSimpleAttributeDefinition<T> findSimpleAttributeDefinitionRequired(
            @NotNull QName name, @NotNull Supplier<String> contextSupplier)
            throws SchemaException {
        return MiscUtil.requireNonNull(
                findSimpleAttributeDefinition(name),
                () -> new SchemaException("No definition of attribute " + name + " in " + this + contextSupplier.get()));
    }

    /**
     * Finds a definition of an attribute with a given name. Throws {@link IllegalStateException} if it's not there.
     */
    default @NotNull ShadowSimpleAttributeDefinition<?> findSimpleAttributeDefinitionStrictlyRequired(
            @NotNull QName name, @NotNull Supplier<String> contextSupplier) {
        return MiscUtil.requireNonNull(
                findSimpleAttributeDefinition(name),
                () -> new IllegalStateException("No definition of attribute " + name + " in " + this + contextSupplier.get()));
    }

    /**
     * Finds a attribute definition by looking at the property name.
     *
     * Returns null if nothing is found.
     *
     * @param name property definition name
     * @param caseInsensitive if true, ignoring the case
     * @return found property definition or null
     */
    default <T> @Nullable ShadowSimpleAttributeDefinition<T> findSimpleAttributeDefinition(QName name, boolean caseInsensitive) {
        //noinspection unchecked
        return findLocalItemDefinition(
                ItemName.fromQName(name), ShadowSimpleAttributeDefinition.class, caseInsensitive);
    }

    /**
     * Finds attribute definition using local name only.
     *
     * BEWARE: Ignores attributes in namespaces other than "ri:" (e.g. icfs:uid and icfs:name).
     */
    @VisibleForTesting
    default <T> ShadowSimpleAttributeDefinition<T> findSimpleAttributeDefinition(String name) {
        return findSimpleAttributeDefinition(
                new QName(MidPointConstants.NS_RI, name));
    }

    /** A convenience variant of {@link #findSimpleAttributeDefinition(String)}. */
    @VisibleForTesting
    default <T> @NotNull ShadowSimpleAttributeDefinition<T> findSimpleAttributeDefinitionRequired(String name) throws SchemaException {
        return findSimpleAttributeDefinitionRequired(
                new QName(MidPointConstants.NS_RI, name));
    }

    /**
     * Returns true if the object class has any index-only attributes.
     */
    default boolean hasIndexOnlyAttributes() {
        return getAttributeDefinitions().stream()
                .anyMatch(ItemDefinition::isIndexOnly);
    }

    /**
     * Returns true if there is an attribute with the given name defined.
     */
    default boolean containsAttributeDefinition(@NotNull QName attributeName) {
        return findSimpleAttributeDefinition(attributeName) != null;
    }

    /** Real values should have no duplicates. */
    @SuppressWarnings("unchecked")
    default <T> @NotNull ShadowSimpleAttribute<T> instantiateAttribute(@NotNull QName attrName, @NotNull T... realValues)
            throws SchemaException {
        //noinspection unchecked
        return ((ShadowSimpleAttributeDefinition<T>) findSimpleAttributeDefinitionRequired(attrName))
                .instantiateFromRealValues(List.of(realValues));
    }

    default @NotNull Collection<ItemName> getAllAttributesNames() {
        return getAttributeDefinitions(ShadowSimpleAttributeDefinition.class).stream()
                .map(ItemDefinition::getItemName)
                .toList();
    }
}
