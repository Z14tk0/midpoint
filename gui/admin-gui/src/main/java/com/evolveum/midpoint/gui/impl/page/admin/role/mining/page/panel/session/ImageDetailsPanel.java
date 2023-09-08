/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.Tools.getImageScaleScript;

import com.evolveum.midpoint.model.api.ModelService;

import com.evolveum.midpoint.task.api.Task;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.chunk.PrepareExpandStructure;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.image.CustomImageResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

public class ImageDetailsPanel extends BasePanel<String> implements Popupable {

    private static final String ID_IMAGE = "image";

    String clusterOid;
    OperationResult result = new OperationResult("GetObject");

    public ImageDetailsPanel(String id, IModel<String> messageModel, String clusterOid) {
        super(id, messageModel);
        this.clusterOid = clusterOid;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Task task = ((PageBase) getPage()).createSimpleTask("loadObject");
        ModelService modelService = ((PageBase) getPage()).getModelService();

        RoleAnalysisClusterType cluster = getClusterTypeObject(modelService, clusterOid, result, task).asObjectable();
        String oid = cluster.getRoleAnalysisSessionRef().getOid();
        PrismObject<RoleAnalysisSessionType> parentClusterByOid = getSessionTypeObject(modelService, result, oid, task);
        RoleAnalysisProcessModeType processMode = parentClusterByOid.asObjectable().getProcessMode();

        MiningOperationChunk miningOperationChunk = new PrepareExpandStructure().executeOperation(cluster, true, processMode,
                modelService, result, task);

        CustomImageResource imageResource;

        imageResource = new CustomImageResource(miningOperationChunk, processMode);

        Image image = new Image(ID_IMAGE, imageResource);

        image.add(new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(AjaxRequestTarget target) {
                target.appendJavaScript(getImageScaleScript());

            }

            @Override
            public void renderHead(Component component, IHeaderResponse response) {
                super.renderHead(component, response);
                response.render(OnDomReadyHeaderItem.forScript(getImageScaleScript()));

            }
        });

        add(image);

    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 1000;
    }

    @Override
    public int getHeight() {
        return 800;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("");
    }

}