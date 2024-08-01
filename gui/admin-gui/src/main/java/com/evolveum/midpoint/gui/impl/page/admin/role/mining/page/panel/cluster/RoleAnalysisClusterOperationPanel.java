/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformPattern;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidInducements;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.refreshCells;

import java.io.Serial;
import java.util.*;

import com.google.common.collect.ListMultimap;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.OperationPanelModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisTableOpPanelItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisTableOpPanelItemPanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;
import com.evolveum.midpoint.web.component.data.RoleAnalysisTable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "clusterDetails")
@PanelInstance(
        identifier = "clusterDetails",
        applicableForType = RoleAnalysisClusterType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisClusterType.operationsPanel",
                icon = GuiStyleConstants.CLASS_ICON_TASK_RESULTS,
                order = 20
        )
)
public class RoleAnalysisClusterOperationPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    public static final String PARAM_CANDIDATE_ROLE_ID = "candidateRoleId";
    public static final String PARAM_DETECTED_PATER_ID = "detectedPatternId";
    public static final String PARAM_TABLE_SETTING = "tableSetting";


    private static final String DOT_CLASS = RoleAnalysisClusterOperationPanel.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

    private static final String ID_DATATABLE = "datatable";
    private static final String ID_OPERATION_PANEL = "panel";

    boolean isOutlierDetection = false;
    Set<String> markedUsers = new HashSet<>();


    private final LoadableDetachableModel<OperationPanelModel> operationPanelModel;
    private final LoadableModel<RoleAnalysisObjectDto> miningOperationChunk;

    public RoleAnalysisClusterOperationPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);

        this.operationPanelModel = new LoadableDetachableModel<>() {
            @Override
            protected OperationPanelModel load() {
                OperationPanelModel model = new OperationPanelModel();
                model.createDetectedPatternModel(getClusterPatterns());
                model.createCandidatesRolesRoleModel(getClusterCandidateRoles());

                Task task = getPageBase().createSimpleTask("Prepare mining structure");
                OperationResult result = task.getResult();

                List<DetectedPattern> patternsToAnalyze = loadPatternsToAnalyze(task, result);
                model.addSelectedPattern(patternsToAnalyze);
                return model;
            }
        };

        this.miningOperationChunk = new LoadableModel<>(false) {

            @Override
            protected RoleAnalysisObjectDto load() {
                return new RoleAnalysisObjectDto(getObjectWrapperObject().asObjectable(), operationPanelModel.getObject().getSelectedPatterns(), getParameterTableSetting(), getPageBase());

            }
        };
    }

    private List<DetectedPattern> getClusterPatterns() {
        RoleAnalysisClusterType clusterType = getObjectWrapperObject().asObjectable();
        return transformDefaultPattern(clusterType);
    }

    private List<DetectedPattern> getClusterCandidateRoles() {
        RoleAnalysisClusterType clusterType = getObjectWrapperObject().asObjectable();
        return loadAllCandidateRoles(clusterType);
    }

    private @NotNull List<DetectedPattern> loadAllCandidateRoles(@NotNull RoleAnalysisClusterType cluster) {
        List<RoleAnalysisCandidateRoleType> clusterCandidateRoles = cluster.getCandidateRoles();
        List<DetectedPattern> candidateRoles = new ArrayList<>();
        Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS); //TODO task name?
        OperationResult result = task.getResult();
        for (RoleAnalysisCandidateRoleType candidateRole : clusterCandidateRoles) {

            RoleAnalysisOperationStatus operationStatus = candidateRole.getOperationStatus();
            boolean isMigrated = operationStatus != null
                    && operationStatus.getOperationChannel() != null
                    && operationStatus.getOperationChannel().equals(RoleAnalysisOperation.MIGRATION);

            if (isMigrated) {
                continue;
            }


            String roleOid = candidateRole.getCandidateRoleRef().getOid();
            //TODO does it make sense to create subresult for each iteration?
            PrismObject<RoleType> rolePrismObject = getPageBase().getRoleAnalysisService().getRoleTypeObject(
                    roleOid, task, result);
            List<String> rolesOidInducements;
            if (rolePrismObject == null) {
                return new ArrayList<>();
            }

            //TODO what is this?
            rolesOidInducements = getRolesOidInducements(rolePrismObject);
            List<String> rolesOidAssignment = getRolesOidAssignment(rolePrismObject.asObjectable());

            Set<String> accessOidSet = new HashSet<>(rolesOidInducements);
            accessOidSet.addAll(rolesOidAssignment);

            ListMultimap<String, String> mappedMembers = getPageBase().getRoleAnalysisService().extractUserTypeMembers(new HashMap<>(),
                    null,
                    Collections.singleton(roleOid),
                    getPageBase().createSimpleTask(OP_PREPARE_OBJECTS),
                    result);

            List<ObjectReferenceType> candidateMembers = candidateRole.getCandidateMembers();
            Set<String> membersOidSet = new HashSet<>();
            for (ObjectReferenceType candidateMember : candidateMembers) {
                String oid = candidateMember.getOid();
                if (oid != null) {
                    membersOidSet.add(oid);
                }
            }

            membersOidSet.addAll(mappedMembers.get(roleOid));
            double clusterMetric = (accessOidSet.size() * membersOidSet.size()) - membersOidSet.size();

            DetectedPattern pattern = new DetectedPattern(
                    accessOidSet,
                    membersOidSet,
                    clusterMetric,
                    null,
                    roleOid);
            pattern.setIdentifier(rolePrismObject.getName().getOrig());
            pattern.setId(candidateRole.getId());
            pattern.setClusterRef(new ObjectReferenceType().oid(cluster.getOid()).type(RoleAnalysisClusterType.COMPLEX_TYPE));

            candidateRoles.add(pattern);
        }
        return candidateRoles;
    }

    private List<DetectedPattern> loadPatternsToAnalyze(Task task, OperationResult result) {
        if (getCandidateRoleContainerId() != null) {
            return analysePattersForCandidateRole(task, result);
        }

        return loadDetectedPattern();
    }

    private List<DetectedPattern> analysePattersForCandidateRole(Task task, OperationResult result) {
        RoleAnalysisClusterType cluster = getObjectWrapperObject().asObjectable();
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        return roleAnalysisService.findDetectedPatterns(cluster, getCandidateRoleContainerId(), task, result);
    }

    public List<String> getCandidateRoleContainerId() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_CANDIDATE_ROLE_ID);
        if (!stringValue.isNull()) {
            String[] split = stringValue.toString().split(",");
            return Arrays.asList(split);
        }
        return null;
    }

    public Long getDetectedPatternContainerId() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_DETECTED_PATER_ID);
        if (!stringValue.isNull()) {
            return Long.valueOf(stringValue.toString());
        }
        return null;
    }


    private List<DetectedPattern> loadDetectedPattern() {
        RoleAnalysisClusterType cluster = getObjectWrapperObject().asObjectable();
        List<RoleAnalysisDetectionPatternType> detectedPattern = cluster.getDetectedPattern();

        for (RoleAnalysisDetectionPatternType pattern : detectedPattern) {
            Long id = pattern.getId();
            if (id.equals(getDetectedPatternContainerId())) {
                return Collections.singletonList(transformPattern(pattern));
            }
        }
        return new ArrayList<>();
    }

    @Override
    protected void initLayout() {
//        PageBase pageBase = getPageBase();
//        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
//        Task task = pageBase.createSimpleTask(OP_PREPARE_OBJECTS);
//        OperationResult result = task.getResult();
//
//        RoleAnalysisProcessModeType mode;
//        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectType();
//        PrismObject<RoleAnalysisSessionType> getParent = roleAnalysisService.
//                getSessionTypeObject(cluster.getRoleAnalysisSessionRef().getOid(), task, result);
//        if (getParent != null) {
//            RoleAnalysisSessionType session = getParent.asObjectable();
//            RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
//            RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
//            if (RoleAnalysisCategoryType.OUTLIERS.equals(analysisCategory)) {
//                isOutlierDetection = true;
//            }
//            mode = analysisOption.getProcessMode();
//            isRoleMode = mode.equals(RoleAnalysisProcessModeType.ROLE);
//        }

        if (isOutlierDetection) {
            markedUsers = new HashSet<>();
            RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();


            Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);
            OperationResult result = task.getResult();

            List<RoleAnalysisOutlierType> searchResultList = roleAnalysisService.findClusterOutliers(
                    getObjectWrapperObject().asObjectable(), task, result);
            if (searchResultList != null && !searchResultList.isEmpty()) {
                for (RoleAnalysisOutlierType outlier : searchResultList) {
                    ObjectReferenceType targetObjectRef = outlier.getTargetObjectRef();
                    if (targetObjectRef != null && targetObjectRef.getOid() != null) {
                        markedUsers.add(targetObjectRef.getOid());
                    }
                }
            }
        }

//        loadDisplayValueOptionModel();

//        WebMarkupContainer webMarkupContainer = new WebMarkupContainer(ID_MAIN_PANEL);
//        webMarkupContainer.setOutputMarkupId(true);

        loadMiningTable();
        initOperationPanel();

//        add(webMarkupContainer);

    }


    public Integer getParameterTableSetting() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
        if (!stringValue.isNull()) {
            return Integer.valueOf(stringValue.toString());
        }
        return null;
    }


    private <B extends MiningBaseTypeChunk, A extends  MiningBaseTypeChunk> void loadMiningTable() {
    //TODO what exactly is this?
//    RoleAnalysisDetectionOptionType detectionOption = getObjectDetailsModels().getObjectType().getDetectionOption();
//        if (detectionOption == null || detectionOption.getFrequencyRange() == null) {
//            return;
//        }

        RoleAnalysisTable<B, A> boxedTablePanel = new RoleAnalysisTable<>(
                ID_DATATABLE,
//                this::getObjectWrapperObject,
//                analysePattern,
//                operationPanelModel,
//                displayValueOptionModel,
                miningOperationChunk) {
//                !isOutlierDetection) {

            @Override
            protected Set<String> getMarkMemberObjects() {
                if (isOutlierDetection()) {
                    return markedUsers;
                }

                return null;
            }

            @Override
            public boolean isOutlierDetection() {
                return isOutlierDetection;
            }

            @Override
            protected List<DetectedPattern> getSelectedPatterns() {
                return RoleAnalysisClusterOperationPanel.this.getSelectedPatterns();
            }

            @Override
            protected IModel<Map<String, String>> getColorPaletteModel() {
                return () -> operationPanelModel.getObject().getPalletColors();
            }
        };
        boxedTablePanel.setOutputMarkupId(true);
        add(boxedTablePanel);

    }



        private void initOperationPanel() {
        RoleAnalysisTableOpPanelItemPanel itemPanel = new RoleAnalysisTableOpPanelItemPanel(ID_OPERATION_PANEL, operationPanelModel) {

            @Override
            public void onPatternSelectionPerform(@NotNull AjaxRequestTarget ajaxRequestTarget) {
                loadDetectedPattern(ajaxRequestTarget);
            }

            @Override
            protected void initHeaderItem(@NotNull RepeatingView headerItems) {
                RoleAnalysisTableOpPanelItem refreshIcon = new RoleAnalysisTableOpPanelItem(
                        headerItems.newChildId(), getModel()) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Contract(pure = true)
                    @Override
                    public @NotNull String appendIconPanelCssClass() {
                        return "bg-white";
                    }

                    @Override
                    protected void performOnClick(AjaxRequestTarget target) {
                        boolean showAsExpandCard = getModelObject().isShowAsExpandCard();
                        getModelObject().setShowAsExpandCard(!showAsExpandCard);
//                        showAsExpandCard = !showAsExpandCard;
                        toggleDetailsNavigationPanelVisibility(target);
                    }

                    @Contract(pure = true)
                    @Override
                    public @NotNull String replaceIconCssClass() {
                        if (getModelObject().isShowAsExpandCard()) {
                            return "fa-2x fa fa-compress text-dark";
                        }
                        return "fa-2x fas fa-expand text-dark";
                    }

                    @Override
                    public @NotNull Component getDescriptionTitleComponent(String id) {
                        Label label = new Label(id, "Table view"); //TODO string resource model
                        label.setOutputMarkupId(true);
                        return label;
                    }

                    @Override
                    protected void addDescriptionComponents() {
                        appendText("Switch table view", null); //TODO string resource model
                    }
                };
                refreshIcon.add(AttributeAppender.replace("class", "btn btn-outline-dark border-0 d-flex"
                        + " align-self-stretch mt-1"));
                headerItems.add(refreshIcon);
            }

        };
        itemPanel.setOutputMarkupId(true);
        add(itemPanel);
    }

    @SuppressWarnings("rawtypes")
        protected void toggleDetailsNavigationPanelVisibility(AjaxRequestTarget target) {
            PageBase page = getPageBase();
            if (page instanceof AbstractPageObjectDetails) {
                AbstractPageObjectDetails<?, ?> pageObjectDetails = ((AbstractPageObjectDetails) page);
                pageObjectDetails.toggleDetailsNavigationPanelVisibility(target);
            }
        }

        protected void loadDetectedPattern(AjaxRequestTarget target) {

//                this.isRelationSelected = false;
                MiningOperationChunk chunk = miningOperationChunk.getObject().getMininingOperationChunk();


                List<MiningUserTypeChunk> users = chunk.getMiningUserTypeChunks();
                List<MiningRoleTypeChunk> roles = chunk.getMiningRoleTypeChunks();

                refreshCells(chunk.getProcessMode(), users, roles, chunk.getMinFrequency(), chunk.getMaxFrequency());



                if (isPatternDetected()) {
                    Task task = getPageBase().createSimpleTask("InitPattern");
                    OperationResult result = task.getResult();

                    getPageBase().getRoleAnalysisService().updateChunkWithPatterns(chunk, getSelectedPatterns(), task, result);

//            initUserBasedDetectionPattern(getPageBase(), users,
//                    roles,
//                    getSelectedPatterns(),
//                    chunk.getMinFrequency(),
//                    chunk.getMaxFrequency(),
//                    task,
//                    result);
                }

                target.add(get(ID_DATATABLE));
        }

    private boolean isPatternDetected() {
        return !getSelectedPatterns().isEmpty();
    }

    private List<DetectedPattern> getSelectedPatterns() {
        return operationPanelModel.getObject().getSelectedPatterns();
    }


}
