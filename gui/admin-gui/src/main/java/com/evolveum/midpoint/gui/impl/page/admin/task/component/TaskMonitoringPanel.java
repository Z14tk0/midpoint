/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "monitoring", defaultContainerPath = "activity/monitoring", defaultType = ActivityMonitoringDefinitionType.class)
@PanelInstance(identifier = "monitoring", applicableFor = TaskType.class, childOf = TaskActivityPanel.class)
@PanelDisplay(label = "Monitoring", order = 40)
public class TaskMonitoringPanel extends AbstractObjectMainPanel<TaskType> {

    private static final Trace LOGGER = TraceManager.getTrace(TaskMonitoringPanel.class);
    private static final String ID_MAIN_PANEL = "main";
    private static final String ID_HANDLER = "handler";

    private static final String DOT_CLASS = TaskMonitoringPanel.class.getName() + ".";
    private static final String OPERATION_UPDATE_WRAPPER = DOT_CLASS + "updateWrapper";

    public TaskMonitoringPanel(String id, LoadableModel<PrismObjectWrapper<TaskType>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel activityDefinitionPanel = new SingleContainerPanel(ID_MAIN_PANEL,
                PrismContainerWrapperModel.fromContainerWrapper(getModel(), ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_MONITORING)),
                ActivityMonitoringDefinitionType.COMPLEX_TYPE);
        add(activityDefinitionPanel);

    }
}
