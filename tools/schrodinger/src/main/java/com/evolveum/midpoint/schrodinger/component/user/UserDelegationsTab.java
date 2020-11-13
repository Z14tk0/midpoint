/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component.user;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.DelegationDetailsPanel;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.component.modal.ObjectBrowserModal;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import com.evolveum.midpoint.schrodinger.util.Utils;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserDelegationsTab extends Component<UserPage> {

    public UserDelegationsTab(UserPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public ObjectBrowserModal<UserDelegationsTab> clickAddDelegation() {
        SelenideElement button = $(Schrodinger.byDataId("assignmentsMenu")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .$(Schrodinger.byElementAttributeValue("button", "data-toggle", "dropdown"));
        button.click();
        button.waitWhile(Condition.attribute("aria-expanded", "true"), MidPoint.TIMEOUT_MEDIUM_6_S);
        button.$(By.linkText("Add delegation")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new ObjectBrowserModal<>(this, Utils.getModalWindowSelenideElement());
    }

    public ConfirmationModal<UserDelegationsTab> clickDeleteDelegation() {
        SelenideElement button = $(Schrodinger.byDataId("assignmentsMenu")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .$(Schrodinger.byElementAttributeValue("button", "data-toggle", "dropdown"));
        button.click();
        button.waitUntil(Condition.attribute("aria-expanded", "true"), MidPoint.TIMEOUT_MEDIUM_6_S);
        button.$(By.linkText("Delete delegation")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new ConfirmationModal<>(this, Utils.getModalWindowSelenideElement());
    }

    public UserDelegationsTab clickAllDelegationsCheckBox() {
        SelenideElement checkbox = $(Schrodinger.byDataId("assignmentsCheckAll")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        checkbox.click();
        checkbox.waitUntil(Condition.attribute("checked", "checked"), MidPoint.TIMEOUT_DEFAULT_2_S);
        return this;
    }

    public DelegationDetailsPanel<UserDelegationsTab> getDelegationDetailsPanel(String delegateToUser) {
        return new DelegationDetailsPanel<>(this,
                $(Schrodinger.byAncestorFollowingSiblingDescendantOrSelfElementEnclosedValue("div", "data-s-id",
                        "delegationsPanel", "class", "name",  delegateToUser))
                        .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }
}
