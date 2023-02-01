/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import static com.evolveum.midpoint.schema.util.SimulationMetricReferenceTypeUtil.getDisplayableIdentifier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.box.SmallBoxData;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.widget.MetricWidgetPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/simulations/result/${RESULT_OID}",
                        matchUrlForSecurity = "/admin/simulations/result/?*")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATIONS_ALL_URL,
                        label = "PageSimulationResults.auth.simulationsAll.label",
                        description = "PageSimulationResults.auth.simulationsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SIMULATION_RESULT_URL,
                        label = "PageSimulationResults.auth.simulationResult.label",
                        description = "PageSimulationResults.auth.simulationResult.description")
        }
)
public class PageSimulationResult extends PageAdmin implements SimulationPage {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageSimulationResult.class);

    private static final String DOT_CLASS = PageSimulationResult.class.getName() + ".";

    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_DETAILS = "details";
    private static final String ID_LABEL = "label";
    private static final String ID_VALUE = "value";
    private static final String ID_WIDGETS = "widgets";
    private static final String ID_WIDGET = "widget";

    private IModel<SimulationResultType> resultModel;

    private IModel<List<ResultDetail>> detailsModel;

    public PageSimulationResult() {
        this(new PageParameters());
    }

    public PageSimulationResult(PageParameters parameters) {
        super(parameters);

        initModels();
        initLayout();
    }

    private void initModels() {
        resultModel = new LoadableDetachableModel<>() {
            @Override
            protected SimulationResultType load() {
                return loadSimulationResult(PageSimulationResult.this);
            }
        };

        detailsModel = new LoadableModel<>() {

            @Override
            protected List<ResultDetail> load() {
                List<ResultDetail> list = new ArrayList<>();
                list.add(new ResultDetail("PageSimulationResult.startTimestamp",
                        () -> WebComponentUtil.translateMessage(ValueDisplayUtil.toStringValue(new PrismPropertyValueImpl(resultModel.getObject().getStartTimestamp())))));
                list.add(new ResultDetail("PageSimulationResult.endTimestamp",
                        () -> WebComponentUtil.translateMessage(ValueDisplayUtil.toStringValue(new PrismPropertyValueImpl(resultModel.getObject().getEndTimestamp())))));
                list.add(new ResultDetail("PageSimulationResult.finishedIn", () -> "30 minutes")); // todo proper calculation
                list.add(new ResultDetail("PageSimulationResult.rootTask", null) {

                    @Override
                    public Component createValueComponent(String id) {
                        return new Label(id); // todo task name with link
                    }
                });
                list.add(new ResultDetail("PageSimulationResult.taskStatus", null) {

                    @Override
                    public Component createValueComponent(String id) {
                        return createTaskStateLabel(id, resultModel, PageSimulationResult.this);
                    }
                });
                list.add(new ResultDetail("PageSimulationResult.productionConfiguration", null) {

                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, () -> {
                            ConfigurationSpecificationType specification = resultModel.getObject().getConfigurationUsed();
                            if (specification == null || BooleanUtils.isNotFalse(specification.isProductionConfiguration())) {
                                return getString("PageSimulationResult.production");
                            }

                            return getString("PageSimulationResult.development");
                        });
                        label.add(AttributeModifier.replace("class", "badge badge-success"));
                        return label;
                    }
                });

                return list;
            }
        };
    }

    private void initLayout() {
        NavigationPanel navigation = new NavigationPanel(ID_NAVIGATION) {

            @Override
            protected @NotNull VisibleEnableBehaviour getNextVisibilityBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected IModel<String> createTitleModel() {
                return () -> WebComponentUtil.getDisplayNameOrName(resultModel.getObject().asPrismObject());
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageSimulationResult.this.onBackPerformed(target);
            }
        };
        add(navigation);

        ListView<ResultDetail> details = new ListView<>(ID_DETAILS, detailsModel) {
            @Override
            protected void populateItem(ListItem<ResultDetail> item) {
                item.add(new Label(ID_LABEL, () -> getString(item.getModelObject().label)));
                item.add(item.getModelObject().createValueComponent(ID_VALUE));
            }
        };
        add(details);

        // todo implement
        IModel<List<DashboardWidgetType>> data = () -> {

            List<SimulationMetricValuesType> metrics = resultModel.getObject().getMetric();
            return metrics.stream().map(m -> {
                DashboardWidgetType dw = new DashboardWidgetType();
                dw.setDescription(getDisplayableIdentifier(m.getRef()));
//                dw.setTitle("TODO");//todo + SimulationMetricValuesTypeUtil.getValue(m));
//                dw.setSmallBoxCssClass("bg-info");
//                dw.setLinkText("More info");
//                dw.setIcon("fa fa-database");

                return dw;
            }).collect(Collectors.toList());
        };

        ListView<DashboardWidgetType> widgets = new ListView<>(ID_WIDGETS, data) {

            @Override
            protected void populateItem(ListItem<DashboardWidgetType> item) {
                item.add(new MetricWidgetPanel(ID_WIDGET, item.getModel()) {

                    @Override
                    protected void onOpenPerformed(AjaxRequestTarget target) {
                        openTagMetricPerformed(target);
                    }
                });
            }
        };
        add(widgets);
    }

    private void onWidgetClick(AjaxRequestTarget target, SmallBoxData data) {
        System.out.println();
    }

    private void onBackPerformed(AjaxRequestTarget target) {
        //todo implement
        redirectBack();
    }

    private void openTagMetricPerformed(AjaxRequestTarget target) {
        PageParameters params = new PageParameters();
        params.add(SimulationPage.PAGE_PARAMETER_RESULT_OID, getPageParameterResultOid());
//        params.add(SimulationPage.PAGE_PARAMETER_TAG_OID, null);    // todo add tag oid somehow
        navigateToNext(PageSimulationResultObjects.class, params);
    }

    private static class ResultDetail implements Serializable {

        String label;

        IModel<String> value;

        public ResultDetail(String label, IModel<String> value) {
            this.label = label;
            this.value = value;
        }

        public Component createValueComponent(String id) {
            Label label = new Label(id, value);
            label.setRenderBodyOnly(true);
            return label;
        }
    }

    public static Component createTaskStateLabel(String id, IModel<SimulationResultType> model, PageBase page) {
        IModel<TaskExecutionStateType> stateModel = () -> {
            SimulationResultType result = model.getObject();
            if (result == null || result.getRootTaskRef() == null) {
                return null;
            }

            PrismObject<TaskType> task = WebModelServiceUtils.loadObject(result.getRootTaskRef(), page);
            return task != null ? task.asObjectable().getExecutionState() : null;
        };

        Label label = new Label(id, () -> {
            if (model.getObject().getEndTimestamp() != null) {
                return null;
            }

            TaskExecutionStateType state = stateModel.getObject();
            if (state == null) {
                return null;
            }

            return page.getString(state);
        });
        label.add(AttributeAppender.replace("class", () -> {
            TaskExecutionStateType state = stateModel.getObject();
            if (state == TaskExecutionStateType.RUNNABLE || state == TaskExecutionStateType.RUNNING) {
                return Badge.State.SUCCESS.getCss();
            }

            return Badge.State.SECONDARY.getCss();
        }));

        return label;
    }
}
