/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_DIR;

import java.io.File;

import com.evolveum.midpoint.util.exception.SecurityViolationException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests the use of expression profiles in various contexts.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestExpressionProfiles extends AbstractEmptyModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestExpressionProfiles.class);

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "profiles");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final DummyTestResource RESOURCE_SIMPLE_TARGET = new DummyTestResource(
            TEST_DIR, "resource-simple-target.xml", "2003a0c3-62a3-413d-9941-6fecaef84a16", "simple-target");

    private static final TestObject<ArchetypeType> ARCHETYPE_RESTRICTED_ROLE = TestObject.file(
            TEST_DIR, "archetype-restricted-role.xml", "a2242707-43cd-4f18-b986-573cb468693d");
    private static final TestObject<RoleType> ROLE_RESTRICTED_GOOD = TestObject.file(
            TEST_DIR, "role-restricted-good.xml", "ca8c4ffd-98ed-4ca7-9097-44db924155c9");
    private static final TestObject<RoleType> ROLE_RESTRICTED_BAD_FOCUS_MAPPING = TestObject.file(
            TEST_DIR, "role-restricted-bad-focus-mapping.xml", "eee455ef-312c-4bc7-a541-3add09d8e90d");
    private static final TestObject<RoleType> ROLE_RESTRICTED_BAD_CONSTRUCTION_MAPPING = TestObject.file(
            TEST_DIR, "role-restricted-bad-construction-mapping.xml", "c8cae775-2c3b-49bb-98e3-482a095316ef");

    // initialized only in specific tests
    private static final TestObject<RoleType> ROLE_RESTRICTED_AUTO_GOOD = TestObject.file(
            TEST_DIR, "role-restricted-auto-good.xml", "a6ace69f-ecfb-457b-97ea-0b5a8b4a6ad3");
    private static final TestObject<RoleType> ROLE_RESTRICTED_AUTO_FILTER_EXPRESSION = TestObject.file(
            TEST_DIR, "role-restricted-auto-filter-expression.xml", "885bc0b4-2493-4658-a523-8a3f7eeb770b");
    private static final TestObject<RoleType> ROLE_RESTRICTED_AUTO_BAD_MAPPING_EXPRESSION = TestObject.file(
            TEST_DIR, "role-restricted-auto-bad-mapping-expression.xml", "26f61dc6-efff-4614-aac6-25753b81512f");
    private static final TestObject<RoleType> ROLE_RESTRICTED_AUTO_BAD_MAPPING_CONDITION = TestObject.file(
            TEST_DIR, "role-restricted-auto-bad-mapping-condition.xml", "eceab160-de05-4b1b-9d09-27cc789252c3");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_SIMPLE_TARGET.initAndTest(this, initTask, initResult);

        initTestObjects(initTask, initResult,
                ARCHETYPE_RESTRICTED_ROLE,
                ROLE_RESTRICTED_GOOD,
                ROLE_RESTRICTED_BAD_FOCUS_MAPPING,
                ROLE_RESTRICTED_BAD_CONSTRUCTION_MAPPING);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /** "Correct" restricted role is used. */
    @Test
    public void test100RestrictedRoleGood() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("user with correct restricted role is added");
        UserType user = new UserType()
                .name("test100")
                .assignment(ROLE_RESTRICTED_GOOD.assignmentTo());
        var userOid = addObject(user.asPrismObject(), task, result);

        then("user is created");
        assertSuccess(result);
        assertUserAfter(userOid)
                .assertDescription("My name is 'test100'")
                .assertLiveLinks(1);
        assertDummyAccountByUsername(RESOURCE_SIMPLE_TARGET.name, "test100")
                .display();
    }

    /** "Incorrect" restricted role is used: bad focus mapping. */
    @Test
    public void test110RestrictedRoleBadFocusMapping() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("user with incorrect restricted role (bad focus mapping) is added");
        UserType user = new UserType()
                .name("test110")
                .assignment(ROLE_RESTRICTED_BAD_FOCUS_MAPPING.assignmentTo());
        try {
            addObject(user.asPrismObject(), task, result);
            fail("unexpected success");
        } catch (SecurityViolationException e) {
            assertExpectedException(e)
                    .hasMessageContaining("Denied access to functionality of script")
                    .hasMessageContaining("Access to Groovy method java.lang.System#setProperty denied (applied expression profile 'restricted')");
            assertFailure(result);
        }
    }

    /** "Incorrect" restricted role is used: bad mapping in construction. */
    @Test
    public void test120RestrictedRoleBadConstructionMapping() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("user with incorrect restricted role is added");
        UserType user = new UserType()
                .name("test120")
                .assignment(ROLE_RESTRICTED_BAD_CONSTRUCTION_MAPPING.assignmentTo());
        try {
            addObject(user.asPrismObject(), task, result);
            fail("unexpected success");
        } catch (SecurityViolationException e) {
            assertExpectedException(e)
                    .hasMessageContaining("Denied access to functionality of script")
                    .hasMessageContaining("Access to Groovy method java.lang.System#setProperty denied (applied expression profile 'restricted')");
            assertFailure(result);
        }
    }

    /** "Correct" restricted auto-assigned role is used. */
    @Test
    public void test150RestrictedRoleAutoGood() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("auto-assigned role is imported");
        ROLE_RESTRICTED_AUTO_GOOD.init(this, task, result);

        try {
            when("user that should get auto role is added");
            UserType user = new UserType()
                    .name("test150")
                    .costCenter("auto");
            var userOid = addObject(user.asPrismObject(), task, result);

            then("user is created");
            assertSuccess(result);
            assertUserAfter(userOid)
                    .assignments()
                    .single()
                    .assertRole(ROLE_RESTRICTED_AUTO_GOOD.oid);
        } finally {
            deleteObject(RoleType.class, ROLE_RESTRICTED_AUTO_GOOD.oid);
        }
    }

    /** This checks that filter expressions are not supported - hence, safe. :) */
    @Test
    public void test160RestrictedRoleAutoFilterExpression() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("auto-assigned role is imported");
        ROLE_RESTRICTED_AUTO_FILTER_EXPRESSION.init(this, task, result);

        try {
            when("user that should get auto role is added");
            UserType user = new UserType()
                    .name("test160");
            var userOid = addObject(user.asPrismObject(), task, result);

            then("user is created but without assignments");
            assertSuccess(result);
            assertUserAfter(userOid)
                    .assertAssignments(0);
        } finally {
            deleteObject(RoleType.class, ROLE_RESTRICTED_AUTO_FILTER_EXPRESSION.oid);
        }
    }

    /** Non-compliant script in mapping expression. */
    @Test
    public void test170RestrictedRoleAutoBadMappingExpression() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("auto-assigned role is imported");
        ROLE_RESTRICTED_AUTO_BAD_MAPPING_EXPRESSION.init(this, task, result);

        try {
            when("user that should get auto role is added");
            UserType user = new UserType()
                    .name("test170")
                    .costCenter("auto");
            try {
                addObject(user.asPrismObject(), task, result);
                fail("unexpected success");
            } catch (SecurityViolationException e) {
                assertExpectedException(e)
                        .hasMessageContaining("Denied access to functionality of script in expression in mapping in autoassign mapping")
                        .hasMessageContaining("restricted-auto-bad-mapping-expression") // object name
                        .hasMessageContaining("@autoassign/focus/mapping/123") // item path of the offending mapping TODO fix after MID-8943 is fixed
                        .hasMessageContaining("Access to Groovy method java.lang.System#setProperty denied (applied expression profile 'restricted')");
                assertFailure(result);
            }
        } finally {
            deleteObject(RoleType.class, ROLE_RESTRICTED_AUTO_BAD_MAPPING_EXPRESSION.oid);
        }
    }

    /** Non-compliant script in mapping condition. */
    @Test
    public void test180RestrictedRoleAutoBadMappingCondition() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("auto-assigned role is imported");
        ROLE_RESTRICTED_AUTO_BAD_MAPPING_CONDITION.init(this, task, result);

        try {
            when("user that should get auto role is added");
            UserType user = new UserType()
                    .name("test180")
                    .costCenter("auto");
            try {
                addObject(user.asPrismObject(), task, result);
                fail("unexpected success");
            } catch (SecurityViolationException e) {
                assertExpectedException(e)
                        .hasMessageContaining("Denied access to functionality of script in condition in mapping in autoassign mapping")
                        .hasMessageContaining("restricted-auto-bad-mapping-condition") // object name
                        .hasMessageContaining("@autoassign/focus/mapping/456") // item path of the offending mapping TODO fix after MID-8943 is fixed
                        .hasMessageContaining("Access to Groovy method java.lang.System#setProperty denied (applied expression profile 'restricted')");
                assertFailure(result);
            }
        } finally {
            deleteObject(RoleType.class, ROLE_RESTRICTED_AUTO_BAD_MAPPING_CONDITION.oid);
        }
    }
}
