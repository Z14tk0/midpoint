/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public interface RefinedResourceSchema extends ResourceSchema, DebugDumpable {
    List<? extends RefinedObjectClassDefinition> getRefinedDefinitions();

    List<? extends RefinedObjectClassDefinition> getRefinedDefinitions(ShadowKindType kind);

    ResourceSchema getOriginalResourceSchema();

    default RefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, ShadowType shadow) {
        return getRefinedDefinition(kind, ShadowUtil.getIntent(shadow));
    }

    /**
     * if null accountType is provided, default account definition is returned.
     */
    RefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, String intent);

    /**
     * Returns an object class definition for given {@link ResourceShadowDiscriminator}.
     *
     * Basic algorithm for determination of the "core" i.e. structural object class definition is the following:
     *
     * 1. if `kind` is provided, a definition with matching `kind` and `intent` is returned.
     * 2. if `kind` is not provided but `objectclass` is, a [suitable] definition with matching `objectclass`
     *    is returned - see {@link #getRefinedDefinition(QName)},
     * 3. if neither `kind` nor `objectclass` is provided, no definition is returned.
     *
     * This behavior (point 2 in particular) may change in the future.
     */
    CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(
            @NotNull ResourceShadowDiscriminator discriminator);

    CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(PrismObject<ShadowType> shadow) throws
            SchemaException;

    CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(PrismObject<ShadowType> shadow,
            Collection<QName> additionalAuxiliaryObjectClassQNames) throws SchemaException;

    CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(QName structuralObjectClassQName,
            ShadowKindType kind, String intent);

    /**
     * If no intents are provided, default account definition is returned.
     * We check whether there is only one relevant rOCD.
     */
    RefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, Collection<String> intents) throws SchemaException;

    /**
     * Returns refined object class definition with a matching name. If there are more matching ones,
     * we return the first with {@link RefinedObjectClassDefinition#isDefault()} being `true`. Otherwise
     * we return the arbitrary one. (This may change in the future.)
     */
    RefinedObjectClassDefinition getRefinedDefinition(QName objectClassName);

    default RefinedObjectClassDefinition getDefaultRefinedDefinition(ShadowKindType kind) {
        return getRefinedDefinition(kind, (String)null);
    }

    default PrismObjectDefinition<ShadowType> getObjectDefinition(ShadowKindType kind, String intent) {
        return getRefinedDefinition(kind, intent).getObjectDefinition();
    }

    default PrismObjectDefinition<ShadowType> getObjectDefinition(ShadowKindType kind, ShadowType shadow) {
        return getObjectDefinition(kind, ShadowUtil.getIntent(shadow));
    }

    RefinedObjectClassDefinition findRefinedDefinitionByObjectClassQName(ShadowKindType kind, QName objectClass);

    ObjectClassComplexTypeDefinition findObjectClassDefinition(QName objectClassQName);

    LayerRefinedResourceSchema forLayer(LayerType layer);

    static RefinedResourceSchema getRefinedSchema(PrismObject<ResourceType> resource) throws SchemaException {
        return RefinedResourceSchemaImpl.getRefinedSchema(resource);
    }

    static ResourceSchema getResourceSchema(PrismObject<ResourceType> resource, PrismContext prismContext)
            throws SchemaException {
        return RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
    }
}
