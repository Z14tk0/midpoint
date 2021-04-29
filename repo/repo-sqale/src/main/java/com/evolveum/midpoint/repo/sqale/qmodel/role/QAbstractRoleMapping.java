/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.assignment.QAssignmentMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignSpecificationType;

/**
 * Mapping between {@link QAbstractRole} and {@link AbstractRoleType}.
 *
 * @param <S> schema type for the abstract role object
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class QAbstractRoleMapping<
        S extends AbstractRoleType, Q extends QAbstractRole<R>, R extends MAbstractRole>
        extends QFocusMapping<S, Q, R> {

    public static final String DEFAULT_ALIAS_NAME = "ar";

    public static final
    QAbstractRoleMapping<AbstractRoleType, QAbstractRole<MAbstractRole>, MAbstractRole> INSTANCE =
            new QAbstractRoleMapping<>(QAbstractRole.TABLE_NAME, DEFAULT_ALIAS_NAME,
                    AbstractRoleType.class, QAbstractRole.CLASS);

    protected QAbstractRoleMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType) {
        super(tableName, defaultAliasName, schemaType, queryType);

        addNestedMapping(F_AUTOASSIGN, AutoassignSpecificationType.class)
                .addItemMapping(AutoassignSpecificationType.F_ENABLED,
                        booleanMapper(q -> q.autoAssignEnabled));
        addItemMapping(F_DISPLAY_NAME,
                polyStringMapper(q -> q.displayNameOrig, q -> q.displayNameNorm));
        addItemMapping(F_IDENTIFIER, stringMapper(q -> q.identifier));
        addItemMapping(F_REQUESTABLE, booleanMapper(q -> q.requestable));
        addItemMapping(F_RISK_LEVEL, stringMapper(q -> q.riskLevel));

        addContainerTableMapping(F_INDUCEMENT, inducementMapping(),
                joinOn((o, a) -> o.oid.eq(a.ownerOid)));
    }

    /** Fixes rigid parametric types of static mapping instance to this instance. */
    @NotNull
    public QAssignmentMapping<R> inducementMapping() {
        //noinspection unchecked
        return (QAssignmentMapping<R>) QAssignmentMapping.INSTANCE_INDUCEMENT;
    }

    @Override
    protected Q newAliasInstance(String alias) {
        //noinspection unchecked
        return (Q) new QAbstractRole<>(MAbstractRole.class, alias);
    }

    @Override
    public AbstractRoleSqlTransformer<S, Q, R> createTransformer(
            SqlTransformerSupport transformerSupport) {
        return new AbstractRoleSqlTransformer<>(transformerSupport, this);
    }
}
