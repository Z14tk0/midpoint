/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType.F_ITEMS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType.F_SOURCE;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.repo.sqale.ExtensionProcessor;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Mapping between {@link QFocusIdentity} and {@link FocusIdentityType}.
 *
 * @param <OR> type of the owner row
 */
public class QFocusIdentityMapping<OR extends MFocus>
        extends QContainerMapping<FocusIdentityType, QFocusIdentity<OR>, MFocusIdentity, OR> {

    public static final String DEFAULT_ALIAS_NAME = "fi";

    private static QFocusIdentityMapping<?> instance;

    public static <OR extends MFocus> QFocusIdentityMapping<OR> init(
            @NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QFocusIdentityMapping<>(repositoryContext);
        }
        return get();
    }

    public static <OR extends MFocus> QFocusIdentityMapping<OR> get() {
        //noinspection unchecked
        return (QFocusIdentityMapping<OR>) Objects.requireNonNull(instance);
    }

    // We can't declare Class<QFocusIdentity<OR>>.class, so we cheat a bit.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private QFocusIdentityMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QFocusIdentity.TABLE_NAME, DEFAULT_ALIAS_NAME,
                FocusIdentityType.class, (Class) QFocusIdentity.class, repositoryContext);

        addRelationResolver(PrismConstants.T_PARENT,
                // mapping supplier is used to avoid cycles in the initialization code
                TableRelationResolver.usingJoin(
                        QFocusMapping::getFocusMapping,
                        (q, p) -> q.ownerOid.eq(p.oid)));

        addNestedMapping(F_SOURCE, FocusIdentitySourceType.class)
                .addRefMapping(FocusIdentitySourceType.F_RESOURCE_REF,
                        q -> q.sourceResourceRefTargetOid,
                        null,
                        null,
                        QFocusMapping::getFocusMapping);

        // TODO EXTENSION or ATTRIBUTES or something new here?
        addNestedMapping(F_ITEMS, FocusIdentityItemsType.class)
                .addExtensionMapping(FocusIdentityItemsType.F_ORIGINAL,
                        MExtItemHolderType.EXTENSION, q -> q.itemsOriginal, repositoryContext)
                .addExtensionMapping(FocusIdentityItemsType.F_NORMALIZED,
                        MExtItemHolderType.EXTENSION, q -> q.itemsNormalized, repositoryContext);
    }

    @Override
    protected QFocusIdentity<OR> newAliasInstance(String alias) {
        return new QFocusIdentity<>(alias);
    }

    @Override
    public MFocusIdentity newRowObject() {
        return new MFocusIdentity();
    }

    @Override
    public MFocusIdentity newRowObject(OR ownerRow) {
        MFocusIdentity row = newRowObject();
        row.ownerOid = ownerRow.oid;
        return row;
    }

    @Override
    public MFocusIdentity insert(
            FocusIdentityType schemaObject, OR ownerRow, JdbcSession jdbcSession) throws SchemaException {
        MFocusIdentity row = initRowObject(schemaObject, ownerRow);

        FocusIdentitySourceType source = schemaObject.getSource();
        if (source != null) {
            row.fullSource = createFullObject(schemaObject.getSource());

            ObjectReferenceType resourceRef = source.getResourceRef();
            if (resourceRef != null) {
                row.sourceResourceRefTargetOid = UUID.fromString(resourceRef.getOid());
            }
        }

        FocusIdentityItemsType items = schemaObject.getItems();
        if (items != null) {
            row.itemsOriginal = processExtensions(items.getOriginal(), MExtItemHolderType.EXTENSION);
            row.itemsNormalized = processExtensions(items.getNormalized(), MExtItemHolderType.EXTENSION);
        }

        insert(row, jdbcSession);
        return row;
    }

    @Override
    public FocusIdentityType toSchemaObject(MFocusIdentity row) throws SchemaException {
        FocusIdentityType identity = new FocusIdentityType();
        identity.setId(row.cid);
        byte[] fullSource = row.fullSource;
        if (fullSource != null) {
            identity.setSource(parseSchemaObject(
                    Objects.requireNonNull(fullSource),
                    "identity.source for " + row.ownerOid + "," + row.cid,
                    FocusIdentitySourceType.class));
        }

        FocusIdentityItemsType focusIdentityItems = identity.beginItems();
        if (row.itemsOriginal != null) {
            Map<String, Object> itemMap = Jsonb.toMap(row.itemsOriginal);
            new ExtensionProcessor(repositoryContext()).extensionsToContainer(itemMap, focusIdentityItems.beginOriginal());
        }
        if (row.itemsNormalized != null) {
            Map<String, Object> itemMap = Jsonb.toMap(row.itemsNormalized);
            new ExtensionProcessor(repositoryContext()).extensionsToContainer(itemMap, focusIdentityItems.beginNormalized());
        }
        return identity;
    }
}
