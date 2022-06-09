/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface WizardStep extends Serializable {

    default void init(WizardModel wizard) {
    }

    default String getStepId() {
        return null;
    }

    default String appendCssToWizard() {
        return null;
    }

    default Component createHeaderContent(String id) {
        return new BadgePanel(id, getTitleBadges());
    }

    default IModel<String> getTitle() {
        String key = getClass().getSimpleName() + ".title";
        return () -> Application.get().getResourceSettings().getLocalizer().getString(key, null, key);
    }

    default IModel<List<Badge>> getTitleBadges() {
        return Model.ofList(new ArrayList<>());
    }

    default VisibleEnableBehaviour getHeaderBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    default VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    default VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }
}