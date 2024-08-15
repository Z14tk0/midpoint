/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile;

import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.component.tile.mining.pattern.RoleAnalysisPatternTileModel;
import com.evolveum.midpoint.gui.impl.component.tile.mining.pattern.RoleAnalysisPatternTilePanel;
import com.evolveum.midpoint.gui.impl.component.tile.mining.session.RoleAnalysisSessionTile;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisClusterOccupationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetailsPopup;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PageableListView;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisDetectedPatternTileTable extends BasePanel<String> {

    private static final String DOT_CLASS = RoleAnalysisDetectedPatternTileTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
    private static final String ID_DATATABLE = "datatable";
    PageBase pageBase;
    IModel<List<Toggle<ViewToggle>>> items;

    public RoleAnalysisDetectedPatternTileTable(
            @NotNull String id,
            @NotNull PageBase pageBase,
            @NotNull LoadableDetachableModel<List<DetectedPattern>> detectedPatternList) {
        super(id);
        this.pageBase = pageBase;
        this.items = new LoadableModel<>(false) {

            @Override
            protected @NotNull List<Toggle<ViewToggle>> load() {
                List<Toggle<ViewToggle>> list = new ArrayList<>();

                Toggle<ViewToggle> asList = new Toggle<>("fa-solid fa-table-list", null);

                ViewToggle object = getTable().getViewToggleModel().getObject();

                asList.setValue(ViewToggle.TABLE);
                asList.setActive(object == ViewToggle.TABLE);
                list.add(asList);

                Toggle<ViewToggle> asTile = new Toggle<>("fa-solid fa-table-cells", null);
                asTile.setValue(ViewToggle.TILE);
                asTile.setActive(object == ViewToggle.TILE);
                list.add(asTile);

                return list;
            }
        };
        add(initTable(detectedPatternList));
    }

    public TileTablePanel<RoleAnalysisPatternTileModel<DetectedPattern>, DetectedPattern> initTable(
            @NotNull LoadableDetachableModel<List<DetectedPattern>> detectedPatternList) {

        RoleMiningProvider<DetectedPattern> provider = new RoleMiningProvider<>(
                this, new ListModel<>(detectedPatternList.getObject()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<DetectedPattern> object) {
                super.setObject(object);
            }

        }, true);

        provider.setSort(DetectedPattern.F_METRIC, SortOrder.DESCENDING);
        return new TileTablePanel<>(
                ID_DATATABLE,
                Model.of(ViewToggle.TILE),
                UserProfileStorage.TableId.PANEL_DETECTED_PATTERN) {

            @Override
            protected String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            protected List<IColumn<DetectedPattern, String>> createColumns() {
                return RoleAnalysisDetectedPatternTileTable.this.initColumns();
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment",
                        RoleAnalysisDetectedPatternTileTable.this);

                AjaxIconButton refreshTable = new AjaxIconButton("refreshTable",
                        Model.of("fa fa-refresh"),
                        Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onRefresh(ajaxRequestTarget);
                    }
                };

                refreshTable.setOutputMarkupId(true);
                refreshTable.add(AttributeModifier.replace("title",
                        createStringResource("Refresh table")));
                refreshTable.add(new TooltipBehavior());
                fragment.add(refreshTable);
                TogglePanel<ViewToggle> viewToggle = new TogglePanel<>("viewToggle", items) {

                    @Override
                    protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
                        getViewToggleModel().setObject(item.getObject().getValue());
//                        RoleAnalysisSessionTileTable.this.getTable().refresh();
                        target.add(RoleAnalysisDetectedPatternTileTable.this);
                    }
                };

                viewToggle.add(AttributeModifier.replace("title", createStringResource("Change view")));
                viewToggle.add(new TooltipBehavior());
                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected WebMarkupContainer createTilesButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment",
                        RoleAnalysisDetectedPatternTileTable.this);

                AjaxIconButton refreshTable = new AjaxIconButton("refreshTable",
                        Model.of("fa fa-refresh"), Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onRefresh(ajaxRequestTarget);
                    }
                };

                refreshTable.setOutputMarkupId(true);
                fragment.add(refreshTable);

                TogglePanel<ViewToggle> viewToggle = new TogglePanel<>("viewToggle", items) {

                    @Override
                    protected void itemSelected(@NotNull AjaxRequestTarget target, @NotNull IModel<Toggle<ViewToggle>> item) {
                        getViewToggleModel().setObject(item.getObject().getValue());
                        getTable().refreshSearch();
                        target.add(RoleAnalysisDetectedPatternTileTable.this);
                    }
                };

                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected void onInitialize() {
                super.onInitialize();
            }

            @Override
            protected String getTilesFooterCssClasses() {
                return "card-footer";
            }

            @Override
            protected String getTilesContainerAdditionalClass() {
                return " m-0";
            }

            @Override
            protected ISortableDataProvider<?, ?> createProvider() {
                return provider;
            }

            @Override
            protected PageableListView<?, ?> createTilesPanel(String tilesId, ISortableDataProvider<DetectedPattern, String> provider1) {
                return super.createTilesPanel(tilesId, provider1);
            }

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected RoleAnalysisPatternTileModel createTileObject(DetectedPattern pattern) {
                Long id = pattern.getId();
                String name = "Role suggestion #" + id;
                IModel<String> processMode = extractProcessMode(pageBase, pattern);
                return new RoleAnalysisPatternTileModel<>(pattern, name, processMode.getObject());
            }

            @Override
            protected String getTileCssStyle() {
                return " min-height:170px ";
            }

            @Override
            protected String getTileCssClasses() {
                return "col-3 p-2";
            }

            @Override
            protected String getTileContainerCssClass() {
                return "row justify-content-left ";
            }

            @Override
            protected Component createTile(String id, IModel<RoleAnalysisPatternTileModel<DetectedPattern>> model) {
                return new RoleAnalysisPatternTilePanel<>(id, model);
            }
        };
    }

    protected CompiledObjectCollectionView getObjectCollectionView() {
        return null;
    }

    public List<IColumn<DetectedPattern, String>> initColumns() {

        List<IColumn<DetectedPattern, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Override
            protected DisplayType getIconDisplayType(IModel<DetectedPattern> rowModel) {
                return createDisplayType(GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON, "", "");

            }
        });

        columns.add(new AbstractColumn<>(getHeaderTitle("")) {

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId, IModel<DetectedPattern> rowModel) {

                if (rowModel.getObject() != null && rowModel.getObject() != null) {
                    DetectedPattern pattern = rowModel.getObject();
                    double reductionFactorConfidence = pattern.getReductionFactorConfidence();
                    String formattedReductionFactorConfidence = String.format("%.2f", reductionFactorConfidence);
                    double metric = pattern.getMetric();
                    String formattedMetric = String.format("%.2f", metric);

                    IconWithLabel icon = new IconWithLabel(componentId, Model.of(formattedReductionFactorConfidence + "% ")) {
                        @Contract(pure = true)
                        @Override
                        public @NotNull String getIconCssClass() {
                            return "fa fa-arrow-down";
                        }

                        @Override
                        protected @NotNull Component getSubComponent(String id) {
                            Label label = new Label(id, Model.of("(" + formattedMetric + ")"));
                            label.add(AttributeAppender.append("class", "text-muted"));
                            return label;
                        }
                    };

                    item.add(icon);
                }
            }

            @Override
            public String getSortProperty() {
                return DetectedPattern.F_METRIC;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleMining.cluster.table.column.header.reduction.factor.confidence")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisCluster.table.header.cluster.attribute.statistic.help");
                    }
                };

            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                if (rowModel.getObject() == null || rowModel.getObject() == null) {
                    item.add(new EmptyPanel(componentId));
                    return;
                }

                DetectedPattern pattern = rowModel.getObject();
                if (pattern == null || pattern.getRoles() == null || pattern.getUsers() == null) {
                    item.add(new EmptyPanel(componentId));
                    return;
                }

                IModel<String> roleObjectCount = Model.of(String.valueOf(pattern.getRoles().size()));
                IModel<String> userObjectCount = Model.of(String.valueOf(pattern.getUsers().size()));

                RoleAnalysisClusterOccupationPanel occupationPanel = new RoleAnalysisClusterOccupationPanel(componentId) {
                    @Contract("_ -> new")
                    @Override
                    public @NotNull Component createFirstPanel(String idFirstPanel) {
                        return new IconWithLabel(idFirstPanel, userObjectCount) {
                            @Override
                            public String getIconCssClass() {
                                return "fa fa-user object-user-color";
                            }
                        };
                    }

                    @Contract("_ -> new")
                    @Override
                    public @NotNull Component createSecondPanel(String idSecondPanel) {
                        return new IconWithLabel(idSecondPanel, roleObjectCount) {
                            @Override
                            public String getIconCssClass() {
                                return "fe fe-role object-role-color";
                            }
                        };
                    }

                    @Override
                    public @NotNull Component createSeparatorPanel(String idSeparatorPanel) {
                        Label separator = new Label(idSeparatorPanel, "");
                        separator.add(AttributeModifier.replace("class",
                                "d-flex align-items-center gap-3 fa-solid fa-grip-lines-vertical"));
                        separator.setOutputMarkupId(true);
                        add(separator);
                        return separator;
                    }
                };

                occupationPanel.setOutputMarkupId(true);
                item.add(occupationPanel);

            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisCluster.table.header.cluster.occupation")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisCluster.table.header.cluster.attribute.statistic.help");
                    }
                };
            }

        });

        columns.add(new AbstractColumn<>(
                createStringResource("")) {

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisCluster.table.header.cluster.attribute.statistic")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisCluster.table.header.cluster.attribute.statistic.help");
                    }
                };
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> cellItem, String componentId,
                    IModel<DetectedPattern> model) {
                String confidence = "";
                if (model.getObject() != null && model.getObject() != null) {
                    DetectedPattern pattern = model.getObject();
                    confidence = String.format("%.2f", pattern.getItemsConfidence()) + "%";
                } else {
                    cellItem.add(new EmptyPanel(componentId));
                }

                CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                        .setBasicIcon("fas fa-chart-bar", LayeredIconCssStyle.IN_ROW_STYLE);

                AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(componentId, iconBuilder.build(),
                        Model.of(confidence)) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (model.getObject() != null) {
                            RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                                    ((PageBase) getPage()).getMainPopupBodyId(),
                                    model);
                            ((PageBase) getPage()).showMainPopup(component, target);
                        }
                    }

                };
                objectButton.titleAsLabel(true);
                objectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm "));
                objectButton.add(AttributeAppender.append("style", "width:120px"));

                cellItem.add(objectButton);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        });

        columns.add(new AbstractExportableColumn<>(getHeaderTitle("display")) {

            @Override
            public IModel<?> getDataModel(IModel<DetectedPattern> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<DetectedPattern>> item, String componentId,
                    IModel<DetectedPattern> rowModel) {

                if (rowModel.getObject() == null) {
                    item.add(new EmptyPanel(componentId));
                } else {
                    RepeatingView repeatingView = new RepeatingView(componentId);
                    item.add(AttributeAppender.append("class", "d-flex align-items-center justify-content-center"));
                    item.add(repeatingView);

                    AjaxCompositedIconSubmitButton migrationButton = buildCandidateButton(repeatingView.newChildId(), rowModel);
                    migrationButton.add(AttributeAppender.append("class", "mr-1"));
                    repeatingView.add(migrationButton);

                    AjaxCompositedIconSubmitButton exploreButton = buildExploreButton(repeatingView.newChildId(), rowModel);
                    repeatingView.add(exploreButton);
                }

            }

            @Override
            public Component getHeader(String componentId) {
                LabelWithHelpPanel display = new LabelWithHelpPanel(componentId,
                        getHeaderTitle("display")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisCluster.table.header.cluster.attribute.statistic.help");
                    }
                };
                display.setOutputMarkupId(true);
                display.add(AttributeAppender.append("class", "d-flex align-items-center justify-content-center"));
                return display;
            }

        });

        return columns;
    }

    private static @NotNull IModel<String> extractProcessMode(@NotNull PageBase pageBase, @NotNull DetectedPattern pattern) {
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("getClusterOptionType");
        OperationResult result = task.getResult();
        PrismObject<RoleAnalysisClusterType> clusterPrism = roleAnalysisService
                .getClusterTypeObject(pattern.getClusterRef().getOid(), task, result);
        if (clusterPrism == null) {
            return Model.of("");
        } else {
            RoleAnalysisOptionType analysisOptionType = roleAnalysisService.resolveClusterOptionType(clusterPrism, task, result);
            RoleAnalysisProcessModeType processMode = analysisOptionType.getProcessMode();
            RoleAnalysisCategoryType analysisCategory = analysisOptionType.getAnalysisCategory();
            return Model.of(processMode.value() + "/" + analysisCategory.value());
        }
    }

    @SuppressWarnings("unchecked")
    private TileTablePanel<RoleAnalysisSessionTile<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>> getTable() {
        return (TileTablePanel<RoleAnalysisSessionTile<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>>)
                get(createComponentPath(ID_DATATABLE));
    }

    protected StringResourceModel getHeaderTitle(String identifier) {
        return createStringResource("RoleMining.cluster.table.column.header." + identifier);
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildCandidateButton(String componentId, IModel<DetectedPattern> rowModel) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_PLUS_CIRCLE, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                iconBuilder.build(),
                createStringResource("RoleMining.button.title.candidate")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);
                OperationResult result = task.getResult();
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                DetectedPattern object = rowModel.getObject();
                ObjectReferenceType clusterRef = object.getClusterRef();
                @NotNull String status = roleAnalysisService
                        .recomputeAndResolveClusterOpStatus(clusterRef.getOid(), result, task, true, null);

                if (status.equals("processing")) {
                    warn("Couldn't start detection. Some process is already in progress.");
                    LOGGER.error("Couldn't start detection. Some process is already in progress.");
                    target.add(getFeedbackPanel());
                    return;
                }
                DetectedPattern pattern = rowModel.getObject();
                if (pattern == null) {
                    return;
                }

                Set<String> roles = pattern.getRoles();
                Set<String> users = pattern.getUsers();
                Long patternId = pattern.getId();

                Set<PrismObject<RoleType>> candidateInducements = new HashSet<>();

                for (String roleOid : roles) {
                    PrismObject<RoleType> roleObject = roleAnalysisService
                            .getRoleTypeObject(roleOid, task, result);
                    if (roleObject != null) {
                        candidateInducements.add(roleObject);
                    }
                }

                PrismObject<RoleType> businessRole = new RoleType().asPrismObject();

                List<BusinessRoleDto> roleApplicationDtos = new ArrayList<>();

                for (String userOid : users) {
                    PrismObject<UserType> userObject = WebModelServiceUtils.loadObject(UserType.class, userOid,
                            getPageBase(), task, result);
//                            roleAnalysisService
//                            .getUserTypeObject(userOid, task, result);
                    if (userObject != null) {
                        roleApplicationDtos.add(new BusinessRoleDto(userObject,
                                businessRole, candidateInducements, getPageBase()));
                    }
                }

                PrismObject<RoleAnalysisClusterType> prismObjectCluster = roleAnalysisService
                        .getClusterTypeObject(clusterRef.getOid(), task, result);

                if (prismObjectCluster == null) {
                    return;
                }

                BusinessRoleApplicationDto operationData = new BusinessRoleApplicationDto(
                        prismObjectCluster, businessRole, roleApplicationDtos, candidateInducements);
                operationData.setPatternId(patternId);

                PageRole pageRole = new PageRole(operationData.getBusinessRole(), operationData);
                setResponsePage(pageRole);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeAppender.append("class", "btn btn-success btn-sm"));
        return migrationButton;
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildExploreButton(String componentId, IModel<DetectedPattern> rowModel) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(componentId,
                iconBuilder.build(),
                createStringResource("RoleAnalysis.explore.button.title")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                DetectedPattern pattern = rowModel.getObject();
                PageParameters parameters = new PageParameters();
                String clusterOid = pattern.getClusterRef().getOid();
                parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                parameters.add("panelId", "clusterDetails");
                parameters.add(PARAM_DETECTED_PATER_ID, pattern.getId());
                StringValue fullTableSetting = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
                if (fullTableSetting != null && fullTableSetting.toString() != null) {
                    parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
                }

                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisClusterType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        return migrationButton;
    }

    public IModel<List<Toggle<ViewToggle>>> getItems() {
        return items;
    }

    @Override
    public PageBase getPageBase() {
        return pageBase;
    }

    protected void onRefresh(AjaxRequestTarget target) {

    }

}
