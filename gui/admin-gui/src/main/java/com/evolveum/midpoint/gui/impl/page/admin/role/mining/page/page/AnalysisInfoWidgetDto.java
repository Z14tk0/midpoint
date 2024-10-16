package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionOverviewPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.model.IdentifyWidgetItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetailsPopup;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.RoleAnalysisAspectsWebUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;

public class AnalysisInfoWidgetDto implements Serializable {

    private List<IdentifyWidgetItem> outlierModelData;
    private List<IdentifyWidgetItem> patternModelData;

    public RoleAnalysisOutlierType getTopOutliers() {
        return topOutliers;
    }

    transient RoleAnalysisOutlierType topOutliers;

    boolean isOutlierLoaded;
    boolean isPatternLoaded;

    public AnalysisInfoWidgetDto() {
        this.outlierModelData = new ArrayList<>();
        this.patternModelData = new ArrayList<>();
        this.isOutlierLoaded = false;
        this.isPatternLoaded = false;
        loadLoadingStateModel(outlierModelData);
        loadLoadingStateModel(patternModelData);
    }

    public void loadSessionOutlierModels(
            @NotNull RoleAnalysisSessionType session,
            @NotNull PageBase pageBase,
            @NotNull OperationResult result,
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull Task task) {

        String sessionOid = session.getOid();
        List<RoleAnalysisOutlierType> topSessionOutliers = roleAnalysisService.getSessionOutliers(sessionOid, null, task, result);
        List<RoleAnalysisOutlierType> outliers = topSessionOutliers.subList(0, Math.min(topSessionOutliers.size(), 5));

        topOutliers = outliers.isEmpty() ? null : outliers.get(0);

        List<IdentifyWidgetItem> detailsModel = new ArrayList<>();
        String targetPartitionOid = session.getOid();
        PolyStringType sessionName = session.getName();
        boolean isCluster = false;

        IModel<List<IdentifyWidgetItem>> sessionWidgetModelOutliers = loadOutlierWidgetModels(
                pageBase, outliers, isCluster, targetPartitionOid, sessionName, detailsModel);

        outlierModelData.clear();
        outlierModelData.addAll(sessionWidgetModelOutliers.getObject());
        isOutlierLoaded = true;
    }

    public void loadSessionPatternModels(
            @NotNull RoleAnalysisSessionType session,
            @NotNull PageBase pageBase,
            @NotNull OperationResult result) {
        IModel<List<IdentifyWidgetItem>> sessionWidgetModelPatterns = getSessionWidgetModelPatterns(
                session, result, pageBase, 5);
        patternModelData.clear();
        patternModelData.addAll(sessionWidgetModelPatterns.getObject());
        isPatternLoaded = true;

    }

    public void loadOutlierModels(OperationResult result, RoleAnalysisService roleAnalysisService, PageBase pageBase) {
        List<RoleAnalysisOutlierType> topFiveOutliers;

        SearchResultList<PrismObject<RoleAnalysisOutlierType>> searchResultList = roleAnalysisService
                .searchOutliersRepo(null, result);

        if (searchResultList == null) {
            outlierModelData.clear();
            outlierModelData.addAll(new ArrayList<>());
            isOutlierLoaded = true;
            return;
        }

        List<RoleAnalysisOutlierType> outlierList = new ArrayList<>();
        for (PrismObject<RoleAnalysisOutlierType> roleAnalysisOutlierTypePrismObject : searchResultList) {
            RoleAnalysisOutlierType roleAnalysisOutlierType = roleAnalysisOutlierTypePrismObject.asObjectable();
            outlierList.add(roleAnalysisOutlierType);
        }

        List<RoleAnalysisOutlierType> sortedOutliers = outlierList.stream()
                .sorted(Comparator.comparingDouble(RoleAnalysisOutlierType::getOverallConfidence).reversed())
                .toList();

        List<RoleAnalysisOutlierType> list = new ArrayList<>();
        long limit = 3;
        for (RoleAnalysisOutlierType sortedOutlier : sortedOutliers) {
            if (limit-- == 0) {break;}
            list.add(sortedOutlier);
        }
        topFiveOutliers = list;

        List<IdentifyWidgetItem> detailsModelOutliers = new ArrayList<>();

//        Task task = pageBase.createSimpleTask("loadOutlierModels");
//        SearchResultList<PrismObject<RoleAnalysisOutlierType>> topOutliers1 = roleAnalysisService.getTopOutliers(3, task, result);
        if (!topFiveOutliers.isEmpty()) {
            loadOutlierModel(detailsModelOutliers, topFiveOutliers, pageBase);
        }

        outlierModelData.clear();
        outlierModelData.addAll(detailsModelOutliers);
        isOutlierLoaded = true;
    }

    //TODO remove duplicates
    private void loadOutlierModel(
            @NotNull List<IdentifyWidgetItem> detailsModel,
            @NotNull List<RoleAnalysisOutlierType> topFiveOutliers,
            @NotNull PageBase pageBase) {
        for (RoleAnalysisOutlierType topFiveOutlier : topFiveOutliers) {

            Set<String> anomalies = new HashSet<>();
            List<RoleAnalysisOutlierPartitionType> outlierPartitions = topFiveOutlier.getPartition();
            RoleAnalysisOutlierPartitionType topPartition = null;
            for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                topPartition = resolveTopPartition(outlierPartition, topPartition);
                loadAnomaliesSet(outlierPartition, anomalies);
            }
            Double overallConfidence = topFiveOutlier.getOverallConfidence();
            if (overallConfidence == null) {
                overallConfidence = 0.0;
            }
            BigDecimal bd = BigDecimal.valueOf(overallConfidence);
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            overallConfidence = bd.doubleValue();
            String formattedConfidence = String.format("%.2f", overallConfidence);

            String description = anomalies.size() + " anomalies were detected within " + outlierPartitions.size() + " session";
            RoleAnalysisOutlierPartitionType finalTopPartition = topPartition;
            IdentifyWidgetItem identifyWidgetItem = new IdentifyWidgetItem(
                    IdentifyWidgetItem.ComponentType.OUTLIER,
                    Model.of(GuiStyleConstants.CLASS_ICON_OUTLIER),
                    Model.of(topFiveOutlier.getName().getOrig()),
                    Model.of(description),
                    Model.of(formattedConfidence + "%"),
                    Model.of("name")) {
                @Override
                public void onActionComponentClick(AjaxRequestTarget target) {
                    RoleAnalysisPartitionOverviewPanel panel = new RoleAnalysisPartitionOverviewPanel(
                            pageBase.getMainPopupBodyId(),
                            Model.of(finalTopPartition), Model.of(topFiveOutlier)) {
                        @Override
                        public IModel<String> getTitle() {
                            return createStringResource(
                                    "RoleAnalysisPartitionOverviewPanel.title.most.impact.partition");
                        }
                    };
                    panel.setOutputMarkupId(true);
                    pageBase.showMainPopup(panel, target);
                }

                @Override
                public @NotNull Component createValueTitleComponent(String id) {
                    Label label = new Label(id, Model.of());
                    label.setOutputMarkupId(true);
                    label.add(AttributeModifier.append(CLASS_CSS, "fa fa-level-up fa-sm text-danger"));
                    label.add(new VisibleBehaviour(() -> getDescription() != null));
                    return label;
                }

                @Override
                public @NotNull Component createTitleComponent(String id) {
                    AjaxLinkPanel linkPanel = new AjaxLinkPanel(id, Model.of(topFiveOutlier.getName())) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            PageParameters parameters = new PageParameters();
                            String outlierOid = topFiveOutlier.getOid();
                            parameters.add(OnePageParameterEncoder.PARAMETER, outlierOid);
                            Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                    .getObjectDetailsPage(RoleAnalysisOutlierType.class);
                            pageBase.navigateToNext(detailsPageClass, parameters);
                        }
                    };
                    linkPanel.setOutputMarkupId(true);
                    return linkPanel;

                }
            };
            detailsModel.add(identifyWidgetItem);
        }
    }

    private static void loadAnomaliesSet(
            @NotNull RoleAnalysisOutlierPartitionType outlierPartition,
            @NotNull Set<String> anomalies) {
        List<DetectedAnomalyResult> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
        for (DetectedAnomalyResult detectedAnomaly : detectedAnomalyResult) {
            if (detectedAnomaly.getTargetObjectRef() != null) {
                anomalies.add(detectedAnomaly.getTargetObjectRef().getOid());
            }
        }
    }

    private static @NotNull RoleAnalysisOutlierPartitionType resolveTopPartition(
            @NotNull RoleAnalysisOutlierPartitionType outlierPartition,
            @Nullable RoleAnalysisOutlierPartitionType topPartition) {
        Double overallConfidence = outlierPartition.getPartitionAnalysis().getOverallConfidence();
        if (overallConfidence == null) {
            overallConfidence = 0.0;
        }

        if (topPartition == null
                || overallConfidence > topPartition.getPartitionAnalysis().getOverallConfidence()) {
            topPartition = outlierPartition;
        }
        return topPartition;
    }

    public void loadPatternModelsAsync(OperationResult result, RoleAnalysisService roleAnalysisService, PageBase pageBase, Task task) {
        @NotNull List<DetectedPattern> clusterSearchResult;
        clusterSearchResult = roleAnalysisService.findTopPatters(task, result);

        List<DetectedPattern> sortedPatterns = clusterSearchResult.stream()
                .sorted(Comparator.comparingDouble(DetectedPattern::getMetric).reversed())
                .toList();

        List<DetectedPattern> topThreePatterns = new ArrayList<>();
        long limit = 3;
        for (DetectedPattern sortedPattern : sortedPatterns) {
            if (limit-- == 0) {break;}
            topThreePatterns.add(sortedPattern);
        }

        int allUserOwnedRoleAssignments = roleAnalysisService.countUserOwnedRoleAssignment(result);

        List<IdentifyWidgetItem> identifyWidgetItems = loadPatternModel(topThreePatterns, allUserOwnedRoleAssignments, pageBase);
        patternModelData.clear();
        patternModelData.addAll(identifyWidgetItems);
        isPatternLoaded = true;
    }

    //TODO remove duplicates
    private @NotNull List<IdentifyWidgetItem> loadPatternModel(
            @NotNull List<DetectedPattern> topPatterns,
            int allUserOwnedRoleAssignments,
            @NotNull PageBase pageBase) {
        List<IdentifyWidgetItem> detailsModel = new ArrayList<>();
        for (int i = 0; i < topPatterns.size(); i++) {
            DetectedPattern pattern = topPatterns.get(i);
            double relationsMetric = pattern.getMetric();
            double percentagePart = 0;
            if (relationsMetric != 0 && allUserOwnedRoleAssignments != 0) {
                percentagePart = (relationsMetric / allUserOwnedRoleAssignments) * 100;
            }
            String formattedReductionFactorConfidence = String.format("%.2f", percentagePart);
            double itemsConfidence = pattern.getItemsConfidence();
            String formattedItemConfidence = String.format("%.1f", itemsConfidence);
            String description =
                    relationsMetric +
                            "x relationships with a attribute score of  " +
                            formattedItemConfidence + "%";
            String patternName = "Role suggestion #" + (i + 1);
            IdentifyWidgetItem identifyWidgetItem = new IdentifyWidgetItem(
                    IdentifyWidgetItem.ComponentType.PATTERN,
                    Model.of(GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON),
                    Model.of(),
                    Model.of(description),
                    Model.of(formattedReductionFactorConfidence + "%"),
                    Model.of("name")) {

                @Override
                public @NotNull Component createValueTitleComponent(String id) {
                    Label label = new Label(id, Model.of());
                    label.setOutputMarkupId(true);
                    label.add(AttributeModifier.append(CLASS_CSS, "fa fa-arrow-down fa-sm text-success"));
                    label.add(new VisibleBehaviour(() -> getDescription() != null));
                    return label;
                }

                @Override
                public @NotNull Component createScoreComponent(String id) {
                    Component scoreComponent = super.createScoreComponent(id);
                    scoreComponent.add(AttributeModifier.replace("class", "text-success"));
                    return scoreComponent;
                }

                @Override
                public void onActionComponentClick(AjaxRequestTarget target) {
                    RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                            pageBase.getMainPopupBodyId(),
                            Model.of(pattern));
                    pageBase.showMainPopup(component, target);
                }

                @Override
                public @NotNull Component createTitleComponent(String id) {
                    AjaxLinkPanel linkPanel = new AjaxLinkPanel(id, Model.of(patternName)) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            PageParameters parameters = new PageParameters();
                            String clusterOid = pattern.getClusterRef().getOid();
                            parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
                            parameters.add("panelId", "clusterDetails");
                            parameters.add(PARAM_DETECTED_PATER_ID, pattern.getId());
                            StringValue fullTableSetting = pageBase.getPageParameters().get(PARAM_TABLE_SETTING);
                            if (fullTableSetting != null && fullTableSetting.toString() != null) {
                                parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
                            }

                            Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                    .getObjectDetailsPage(RoleAnalysisClusterType.class);
                            pageBase.navigateToNext(detailsPageClass, parameters);
                        }
                    };
                    linkPanel.setOutputMarkupId(true);
                    return linkPanel;

                }
            };
            detailsModel.add(identifyWidgetItem);
        }
        return detailsModel;
    }

    private void loadLoadingStateModel(
            @NotNull List<IdentifyWidgetItem> detailsModel) {
        String description = "Loading...";
        IdentifyWidgetItem identifyWidgetItem = new IdentifyWidgetItem(
                IdentifyWidgetItem.ComponentType.OUTLIER,
                Model.of(GuiStyleConstants.CLASS_ICON_OUTLIER),
                Model.of(""),
                Model.of(description),
                Model.of(""),
                Model.of("")) {

            @Override
            public boolean isLoading() {
                return true;
            }

            @Override
            public @NotNull Component createValueTitleComponent(String id) {
                WebMarkupContainer panel = new WebMarkupContainer(id);
                panel.setOutputMarkupId(true);
                return panel;
            }

            @Override
            public @NotNull Component createTitleComponent(String id) {
                WebMarkupContainer panel = new WebMarkupContainer(id);
                panel.setOutputMarkupId(true);
                return panel;

            }
        };
        detailsModel.add(identifyWidgetItem);
    }

    public List<IdentifyWidgetItem> getPatternModelData() {
        return patternModelData;
    }

    public boolean isPatternLoaded() {
        return isPatternLoaded;
    }

    public List<IdentifyWidgetItem> getOutlierModelData() {
        return outlierModelData;
    }

    public boolean isOutlierLoaded() {
        return isOutlierLoaded;
    }

}