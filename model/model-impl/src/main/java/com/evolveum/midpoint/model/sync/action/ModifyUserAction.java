/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.ActivationDecision;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class ModifyUserAction extends BaseAction {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyUserAction.class);
    private final String actionName;
    private PolicyDecision policyDecision;
    private ActivationDecision activationDecision;

    public ModifyUserAction() {
        this(PolicyDecision.KEEP, ACTION_MODIFY_USER);
    }

    public ModifyUserAction(PolicyDecision policyDecision, String actionName) {
        this(policyDecision, null, actionName);
    }

    public ModifyUserAction(PolicyDecision policyDecision, ActivationDecision activationDecision, String actionName) {
        Validate.notEmpty(actionName, "Action name must not be null or empty.");

        this.policyDecision = policyDecision;
        this.activationDecision = activationDecision;
        this.actionName = actionName;
    }

    protected PolicyDecision getPolicyDecision() {
        return policyDecision;
    }

    public ActivationDecision getActivationDecision() {
        return activationDecision;
    }

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
                                 SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
                                 OperationResult result) throws SynchronizationException {
        super.executeChanges(userOid, change, situation, shadowAfterChange, result);

        if (!(shadowAfterChange instanceof AccountShadowType)) {
            throw new SynchronizationException("Couldn't synchronize shadow of type '"
                    + shadowAfterChange.getClass().getName() + "', only '"
                    + AccountShadowType.class.getName() + "' is supported.");
        }

        OperationResult subResult = result.createSubresult(actionName);
        result.addSubresult(subResult);

        UserType userType = getUser(userOid, subResult);
        if (userType == null) {
            String message = "Can't find user with oid '" + userOid + "'.";
            subResult.computeStatus(message);
            throw new SynchronizationException(message);
        }

        SyncContext context = createSyncContext(userType, change.getResource());
        AccountSyncContext accountContext = createAccountSyncContext(context, change,
                (AccountShadowType) shadowAfterChange);

        try {
            getSynchronizer().synchronizeUser(context, subResult);
        } catch (Exception ex) {
            throw new SynchronizationException("Couldn't update sync context in modify user action.", ex);
        } finally {
            subResult.recomputeStatus("Couldn't update sync context in modify user action.");
        }

        try {
            getExecutor().executeChanges(context, subResult);
        } catch (Exception ex) {
            throw new SynchronizationException("Couldn't execute modify user action.", ex);
        } finally {
            subResult.recomputeStatus("Couldn't execute modify user action.");
        }

        return userOid;
    }

    private SyncContext createSyncContext(UserType user, ResourceType resource) {
        SyncContext context = new SyncContext();
        MidPointObject<UserType> oldUser = new MidPointObject<UserType>(SchemaConstants.I_USER_TYPE);
        oldUser.setObjectType(user);
        context.setUserOld(oldUser);
        context.setUserTypeOld(user);
        context.rememberResource(resource);

        return context;
    }

    private AccountSyncContext createAccountSyncContext(SyncContext context,
                                                        ResourceObjectShadowChangeDescriptionType change,
                                                        AccountShadowType account) {
        ResourceType resource = change.getResource();

        ResourceAccountType resourceAccount = new ResourceAccountType(resource.getOid(), account.getAccountType());
        AccountSyncContext accountContext = context.createAccountSyncContext(resourceAccount);
        accountContext.setResource(resource);
        accountContext.setPolicyDecision(getPolicyDecision());
        accountContext.setActivationDecision(getActivationDecision());
        accountContext.setOid(account.getOid());


//        ObjectDelta<AccountShadowType> delta = createObjectDelta(change.getObjectChange());
//        accountContext.setAccountPrimaryDelta(delta);

//        MidPointObject<AccountShadowType> accountShadow = new MidPointObject<AccountShadowType>(SchemaConstants.I_ACCOUNT_SHADOW_TYPE);
//        accountShadow.setObjectType(account);
//        accountContext.setAccountNew(accountShadow);

        return accountContext;
    }

//    private ChangeType createObjectDelta(ObjectChangeType change) {
//        new ObjectDelta<AccountShadowType>(AccountShadowType.class, getChangeType(change));
//
//        if (change.getObjectChange() instanceof ObjectChangeAdditionType) {
//            return ChangeType.ADD;
//        }
//
//        if (change.getObjectChange() instanceof ObjectChangeDeletionType) {
//            return ChangeType.DELETE;
//        }
//
//        if (change.getObjectChange() instanceof  ObjectChangeModificationType) {
//            return ChangeType.MODIFY;
//        }
//
//        throw new IllegalArgumentException("Object type change was not defined.");
//    }
}
