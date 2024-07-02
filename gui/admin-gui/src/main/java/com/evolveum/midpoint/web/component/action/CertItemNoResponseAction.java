/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.action;

import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

@ActionType(
        identifier = "certItemNoResponse",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "PageCertDecisions.menu.noResponse", icon = "far fa-clock", order = 5))
public class CertItemNoResponseAction extends AbstractCertItemAction {

    public CertItemNoResponseAction() {
        super();
    }

    public CertItemNoResponseAction(GuiActionDto<AccessCertificationWorkItemType> actionDto) {
        super(actionDto);
    }

    @Override
    protected AccessCertificationResponseType getResponse() {
        return AccessCertificationResponseType.NO_RESPONSE;
    }
}
