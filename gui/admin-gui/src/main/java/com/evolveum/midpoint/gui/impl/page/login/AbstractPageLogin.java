/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import java.io.Serial;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.schema.util.AuthenticationSequenceTypeUtil;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.menu.top.LocaleTextPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceType;

/**
 * @author lskublik
 */
public abstract class AbstractPageLogin extends PageAdminLTE {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SEQUENCE = "sequence";
    private static final String ID_PANEL_TITLE = "panelTitle";
    private static final String ID_PANEL_DESCRIPTION = "panelDescription";
    private static final String ID_SWITCH_TO_DEFAULT_SEQUENCE = "switchToDefaultSequence";
    private static final String ID_BACK_BUTTON = "back";

    private static final Trace LOGGER = TraceManager.getTrace(AbstractPageLogin.class);

    public AbstractPageLogin(PageParameters parameters) {
        super(parameters);
    }

    public AbstractPageLogin() {
        super(null);
    }

    @Override
    protected void addDefaultBodyStyle(TransparentWebMarkupContainer body) {
        body.add(AttributeModifier.replace("class", "login-page"));
        body.add(AttributeModifier.replace("style", "")); //TODO hack :) because PageBase has min-height defined.
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnDomReadyHeaderItem.forScript("$(\"input[name='username']\").focus();"));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Label panelTitle = new Label(ID_PANEL_TITLE, getLoginPanelTitleModel());
        panelTitle.setOutputMarkupId(true);
        panelTitle.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getLoginPanelTitleModel().getObject())));
        add(panelTitle);

        Label panelDescription = new Label(ID_PANEL_DESCRIPTION, getLoginPanelDescriptionModel());
        panelDescription.setOutputMarkupId(true);
        panelDescription.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getLoginPanelDescriptionModel().getObject())));
        add(panelDescription);


        String sequenceName = getSequenceName();
        Label sequence = new Label(ID_SEQUENCE, createStringResource("AbstractPageLogin.authenticationSequence", sequenceName));
        sequence.add(new VisibleBehaviour(() -> !StringUtils.isEmpty(sequenceName)));
        add(sequence);

        AjaxButton toDefault = new AjaxButton(ID_SWITCH_TO_DEFAULT_SEQUENCE, createStringResource("AbstractPageLogin.switchToDefault")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                AuthUtil.clearMidpointAuthentication();
                setResponsePage(getMidpointApplication().getHomePage());
            }
        };
        toDefault.add(new VisibleBehaviour(() -> !StringUtils.isEmpty(sequenceName)));
        add(toDefault);

        initCustomLayout();

        addFeedbackPanel();

        add(new LocaleTextPanel("locale"));

        AjaxButton backButton = new AjaxButton(ID_BACK_BUTTON) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed();
            }
        };
        backButton.setOutputMarkupId(true);
        backButton.add(new VisibleBehaviour(this::isBackButtonVisible));
        add(backButton);
    }

    protected abstract boolean isBackButtonVisible();

    protected abstract void initCustomLayout();

    protected abstract IModel<String> getLoginPanelTitleModel();

    protected abstract IModel<String> getLoginPanelDescriptionModel();

    private String getSequenceName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            return null;
        }
        AuthenticationSequenceType sequence = mpAuthentication.getSequence();
        if (!AuthenticationSequenceTypeUtil.isDefaultChannel(sequence)
                && AuthenticationSequenceTypeUtil.hasChannelId(sequence, SecurityPolicyUtil.DEFAULT_CHANNEL)) {
            return AuthenticationSequenceTypeUtil.getSequenceDisplayName(sequence);

        }
        return null;
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        showExceptionMessage();
    }

    private void showExceptionMessage() {
        ServletWebRequest req = (ServletWebRequest) RequestCycle.get().getRequest();
        HttpServletRequest httpReq = req.getContainerRequest();
        HttpSession httpSession = httpReq.getSession();

        Exception ex = (Exception) httpSession.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (ex == null) {
            return;
        }
        String msg = ex.getMessage();
        if (StringUtils.isEmpty(msg)) {
            msg = "web.security.provider.unavailable";
        }

        String[] msgs = msg.split(";");
        for (String message : msgs) {
            message = getLocalizationService().translate(message, null, getLocale(), message);
            error(message);
        }

        httpSession.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

    @Override
    protected void onAfterRender() {
        super.onAfterRender();
    }

    protected void saveException(Exception exception) {
        ServletWebRequest req = (ServletWebRequest) RequestCycle.get().getRequest();
        HttpServletRequest httpReq = req.getContainerRequest();
        HttpSession httpSession = httpReq.getSession();
        httpSession.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
    }

    protected void cancelPerformed() {
        setResponsePage(getMidpointApplication().getHomePage());
    }

    protected boolean isModuleApplicable(ModuleAuthentication moduleAuthentication) {
        return moduleAuthentication != null && (AuthenticationModuleNameConstants.LOGIN_FORM.equals(moduleAuthentication.getModuleTypeName())
                || AuthenticationModuleNameConstants.LDAP.equals(moduleAuthentication.getModuleTypeName()));
    }

}
