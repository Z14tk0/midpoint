/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsSubCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.Collection;

/**
 * @author lskublik
 */
public class CorrelationWizardPanel extends AbstractWizardPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationWizardPanel.class);

    public CorrelationWizardPanel(String id, WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createTablePanel()));
    }

    protected CorrelationItemsTableWizardPanel createTablePanel() {
        return new CorrelationItemsTableWizardPanel(getIdOfChoicePanel(), getHelper()) {

            @Override
            protected void showTableForItemRefs(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> rowModel) {
                WizardPanelHelper<ItemsSubCorrelatorType, ResourceDetailsModel> helper = new WizardPanelHelper<>(getAssignmentHolderDetailsModel(), rowModel) {
                    @Override
                    public void onExitPerformed(AjaxRequestTarget target) {
                        if (getValueModel() != null) {
                            try {
                                Collection<?> deltas = getValueModel().getObject().getDeltas();
                                if (!deltas.isEmpty()) {
                                    WebComponentUtil.showToastForRecordedButUnsavedChanges(target, getValueModel().getObject());
                                }
                            } catch (SchemaException e) {
                                LOGGER.error("Couldn't collect deltas from " + getValueModel().getObject(), e);
                            }
                        }
                        showChoiceFragment(target, createTablePanel());
                    }
                };
                showChoiceFragment(target, new CorrelationItemRefsTableWizardPanel(getIdOfChoicePanel(), helper));
            }
        };
    }
}
