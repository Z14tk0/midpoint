/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.sleep;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.codeborne.selenide.Selenide;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

public class PostAuthenticationTests extends AbstractSchrodingerTest {

    private static final File SYSTEM_CONFIGURATION_POST_AUTH_ACTIVE_FILE = new File("./src/test/resources/configuration/objects/systemconfig/system-configuration-post-auth-active.xml");
    private static final File SYSTEM_CONFIGURATION_POST_AUTH_NON_ACTIVE_FILE = new File("./src/test/resources/configuration/objects/systemconfig/system-configuration-post-auth-non-active.xml");
    private static final File USER_TEST_TITIAN_FILE = new File("./src/test/resources/configuration/objects/users/user-titian-post-auth.xml");
    private static final File USER_TEST_BOTTICELLI_FILE = new File("./src/test/resources/configuration/objects/users/user-botticelli-post-auth.xml");
    private static final File ROLE_POST_AUTHENTICATION_AUTHORIZATION_FILE = new File("./src/test/resources/configuration/objects/roles/post-authentication-authorization.xml");
    private static final File SECURITY_POLICY_POST_AUTH_DEFAULT_FILE = new File("./src/test/resources/configuration/objects/securitypolicies/post-auth-no-form-default-policy.xml");
    private static final File CUSTOM_FORM_POST_AUTH_FILE = new File("./src/test/resources/configuration/objects/form/post-authentication-form.xml");

    protected static final String TEST_USER_TITIAN_NAME= "titian";
    protected static final String TEST_USER_BOTTICELLI_NAME= "botticelli";

    protected static final String TEST_USER_TITIAN_PASSWORD= "5ecr3t";
    protected static final String ROLE_POST_AUTHENTICATION_AUTHORIZATION_NAME= "Post authentication authorization role";
    protected static final String TEST_GROUP_BEFORE_POST_AUTH_FLOW = "beforePostAuthFlow";
    protected static final String TEST_FLOW_WITHOUT_POST_AUTH_ROLE_ASSIGNED = "flowWithoutPostAuthRoleAssigned";

    protected static final String ACTIVATION_STATE_ENABLED_VALUE = "Enabled";
    protected static final String ACTIVATION_STATE_ARCHIVAED_VALUE = "Archived";

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(ROLE_POST_AUTHENTICATION_AUTHORIZATION_FILE, CUSTOM_FORM_POST_AUTH_FILE, SECURITY_POLICY_POST_AUTH_DEFAULT_FILE,
                SYSTEM_CONFIGURATION_POST_AUTH_NON_ACTIVE_FILE, USER_TEST_TITIAN_FILE, USER_TEST_BOTTICELLI_FILE);
    }

    @Test (groups = TEST_GROUP_BEFORE_POST_AUTH_FLOW)
    public void forcedActivationStatusProposedEnabled(){

    basicPage.listUsers()
                .table()
                    .search()
                        .byName()
                        .inputValue(TEST_USER_TITIAN_NAME)
                    .updateSearch()
                .and()
                    .clickByName(TEST_USER_TITIAN_NAME)
                      .assertActivationStateEquals(ACTIVATION_STATE_ENABLED_VALUE);
    }

    @Test (groups = TEST_GROUP_BEFORE_POST_AUTH_FLOW)
    public void forcedActivationStatusProposedArchived(){

    basicPage.listUsers()
                .table()
                    .search()
                        .byName()
                        .inputValue(TEST_USER_BOTTICELLI_NAME)
                    .updateSearch()
                .and()
                    .clickByName(TEST_USER_BOTTICELLI_NAME)
                        .checkReconcile()
                        .clickSave()
            .listUsers()
                .table()
                    .search()
                        .byName()
                        .inputValue(TEST_USER_BOTTICELLI_NAME)
                    .updateSearch()
                .and()
                    .clickByName(TEST_USER_BOTTICELLI_NAME)
                        .assertActivationStateEquals(ACTIVATION_STATE_ARCHIVAED_VALUE);
    }

//TODO issue listed in Jira under MID-4996
    @Test (dependsOnGroups = {TEST_GROUP_BEFORE_POST_AUTH_FLOW}, alwaysRun = true)
    public void flowWithoutPostAuthRoleAssigned(){
        midPoint.logout();
        midPoint.formLogin()
                .loginWithReloadLoginPage(TEST_USER_TITIAN_NAME,TEST_USER_TITIAN_PASSWORD)
                .dynamicForm();

        Selenide.sleep(5000);
    }

    @Test (dependsOnMethods = {TEST_FLOW_WITHOUT_POST_AUTH_ROLE_ASSIGNED}, alwaysRun = true)
    public void flowWithPostAuthRoleAssigned(){
        Selenide.clearBrowserCookies();
        Selenide.refresh();

        midPoint.formLogin()
                .loginWithReloadLoginPage(getUsername(), getPassword());

        //todo midpoint opens the previous page before logout
        open("/self/dashboard");

        addObjectFromFile(SYSTEM_CONFIGURATION_POST_AUTH_ACTIVE_FILE);

        ListUsersPage usersPage = basicPage.listUsers();
        UserPage parent = usersPage
                .table()
                .search()
                .byName()
                .inputValue(TEST_USER_TITIAN_NAME)
                .updateSearch()
                .and()
                .clickByName(TEST_USER_TITIAN_NAME)
                .selectTabAssignments()
                .clickAddAssignemnt()
                .table()
                .search()
                .byName()
                .inputValue(ROLE_POST_AUTHENTICATION_AUTHORIZATION_NAME)
                .updateSearch()
                .and()
                .selectCheckboxByName(ROLE_POST_AUTHENTICATION_AUTHORIZATION_NAME)
                .and()
                .clickAdd().and();
        sleep(1000);
//                .and()
        parent.checkKeepDisplayingResults()
                .clickSave()
                .feedback()
                .isSuccess();

    midPoint.logout();
    midPoint.formLogin()
            .loginWithReloadLoginPage(TEST_USER_TITIAN_NAME,TEST_USER_TITIAN_PASSWORD)
                .dynamicForm();

}


}
