/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.ReferenceValueSearchPanel;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchSpecialItemPanel;
import com.evolveum.midpoint.web.component.search.SpecialSearchItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class TenantSearchItem extends SpecialSearchItem {

    private static final Trace LOGGER = TraceManager.getTrace(TenantSearchItem.class);

    private MemberPanelStorage memberStorage;

    public TenantSearchItem(Search search, MemberPanelStorage memberStorage) {
        super(search);
        this.memberStorage = memberStorage;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        throw new UnsupportedOperationException();
//        AbstractRoleType object = getParentVariables(variables);
//        if (object == null) {
//            return null;
//        }
//        PrismContext prismContext = pageBase.getPrismContext();
//        List relations = new ArrayList();
//        if (QNameUtil.match(PrismConstants.Q_ANY, getSupportedRelations().getDefaultRelationAllowAny())) {
//            relations.addAll(getSupportedRelations().getAvailableRelationList());
//        } else {
//            relations.add(getSupportedRelations().getDefaultRelationAllowAny());
//        }
//
//        ObjectFilter filter;
//        Class type = getSearch().getTypeClass();
//        S_AtomicFilterExit q = prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
//                .block()
//                .item(AssignmentType.F_TARGET_REF)
//                .ref(MemberOperationsHelper.createReferenceValuesList(object, relations));
//
//        if (!getMemberPanelStorage().isTenantEmpty()) {
//            q = q.and().item(AssignmentType.F_TENANT_REF).ref(getMemberPanelStorage().getTenant().getOid());
//        }
//
//        if (!getMemberPanelStorage().isProjectEmpty()) {
//            q = q.and().item(AssignmentType.F_ORG_REF).ref(getMemberPanelStorage().getProject().getOid());
//        }
//        filter = q.endBlock().buildFilter();
//        return filter;
    }

    private UserInterfaceFeatureType getTenantConfig() {
        return memberStorage.getTenantSearchItem();
    }

    @Override
    public SearchSpecialItemPanel createSpecialSearchPanel(String id, Consumer<AjaxRequestTarget> searchPerformedConsumer) {
        IModel tenantModel = new PropertyModel(getMemberPanelStorage(), MemberPanelStorage.F_TENANT) {
            @Override
            public void setObject(Object object) {
                if (object == null) {
                    getMemberPanelStorage().resetTenantRef();
                } else {
                    super.setObject(object);
                }
            }
        };
        PrismReferenceDefinition tenantRefDef = getTenantDefinition();
        SearchSpecialItemPanel panel = new SearchSpecialItemPanel(id, tenantModel) {
            @Override
            protected WebMarkupContainer initSearchItemField(String id) {
                ReferenceValueSearchPanel searchItemField = new ReferenceValueSearchPanel(id, getModelValue(), tenantRefDef) {
                    @Override
                    protected void referenceValueUpdated(ObjectReferenceType ort, AjaxRequestTarget target) {
                        searchPerformedConsumer.accept(target);
                    }

                    @Override
                    public Boolean isItemPanelEnabled() {
                        return !Boolean.TRUE.equals(getMemberPanelStorage().getIndirect());
                    }

                    @Override
                    protected List<QName> getAllowedRelations() {
                        return Collections.singletonList(RelationTypes.MEMBER.getRelation());
                    }
                };
                return searchItemField;
            }

            @Override
            protected IModel<String> createLabelModel() {
                return Model.of(WebComponentUtil.getTranslatedPolyString(getTenantConfig().getDisplay().getLabel()));
            }

            @Override
            protected IModel<String> createHelpModel() {
                if (getTenantConfig().getDisplay().getHelp() != null){
                    return Model.of(WebComponentUtil.getTranslatedPolyString(getTenantConfig().getDisplay().getHelp()));
                }
                String help = tenantRefDef.getHelp();
                if (StringUtils.isNotEmpty(help)) {
                    return getPageBase().createStringResource(help);
                }
                return Model.of(tenantRefDef.getDocumentation());
            }
        };
        panel.add(new VisibleBehaviour(() -> getMemberPanelStorage() == null
                || !Boolean.TRUE.equals(getMemberPanelStorage().getIndirect())));
        return panel;
    }

    private <R extends AbstractRoleType> R getParentVariables(VariablesMap variables) {
        if (variables == null) {
            return null;
        }
        try {
            return (R) variables.getValue(ExpressionConstants.VAR_PARENT_OBJECT, AbstractRoleType.class);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load parent object.");
        }
        return null;
    }

    public MemberPanelStorage getMemberPanelStorage() {
        return memberStorage;
    }

    public PrismReferenceDefinition getTenantDefinition() {
        return null;
    }
}
