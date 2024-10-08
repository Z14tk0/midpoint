/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.modes;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context.AbstractRoleAnalysisConfiguration;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

public class OutlierDepartmentModeConfiguration extends AbstractRoleAnalysisConfiguration {

    RoleAnalysisService service;
    Task task;
    OperationResult result;
    LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper;

    public OutlierDepartmentModeConfiguration(
            RoleAnalysisService service,
            RoleAnalysisSessionType objectWrapper,
            Task task,
            OperationResult result) {
        super(objectWrapper);
        this.service = service;
        this.task = task;
        this.result = result;
//        this.objectWrapper = objectWrapper;
    }

    @Override
    public void updateConfiguration() {
        RangeType propertyRange = createPropertyRange();
        ClusteringAttributeSettingType clusteringSetting = createClusteringSetting();

        updatePrimaryOptions(null,
                false,
                propertyRange,
                getDefaultAnalysisAttributes(),
                clusteringSetting,
                80.0,
                5,
                2,
                false);

        updateDetectionOptions(2,
                2,
                70.0,
                null,
                RoleAnalysisDetectionProcessType.SKIP,
                new RangeType()
                        .min(2.0)
                        .max(2.0),
                50.0);
    }

    private RangeType createPropertyRange() {
        double minPropertyCount = 2.0;
        double maxPropertyCount = getMaxPropertyCount();
        return new RangeType().min(minPropertyCount).max(maxPropertyCount);
    }

    //TODO let the user choose the department archetype or root for department structure
    private @NotNull ClusteringAttributeSettingType createClusteringSetting() {
        ClusteringAttributeSettingType clusteringSetting = new ClusteringAttributeSettingType();
        ClusteringAttributeRuleType rule = new ClusteringAttributeRuleType()
                .path(FocusType.F_PARENT_ORG_REF.toBean())
                .isMultiValue(true)
                .weight(1.0)
                .similarity(100.0);
        clusteringSetting.getClusteringAttributeRule().add(rule);
        return clusteringSetting;
    }


    @Override
    public AbstractAnalysisSessionOptionType getAnalysisSessionOption() {
        return super.getAnalysisSessionOption();
    }

    @Override
    public RoleAnalysisDetectionOptionType getDetectionOption() {
        return super.getDetectionOption();
    }

    @Override
    public ItemVisibilityHandler getVisibilityHandler() {
        return super.getVisibilityHandler();
    }

    public @NotNull Integer getMaxPropertyCount() {
        Class<? extends ObjectType> propertiesClass = UserType.class;
        if (getProcessMode().equals(RoleAnalysisProcessModeType.USER)) {
            propertiesClass = RoleType.class;
        }

        Integer maxPropertiesObjects;

        maxPropertiesObjects = service.countObjects(propertiesClass, null, null, task, result);

        if (maxPropertiesObjects == null) {
            maxPropertiesObjects = 1000000;
        }
        return maxPropertiesObjects;
    }

}