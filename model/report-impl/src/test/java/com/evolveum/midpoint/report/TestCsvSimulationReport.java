/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;
import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvSimulationReport extends TestCsvReport {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "simulation");

    private static final TestObject<ArchetypeType> ARCHETYPE_BLUE =
            TestObject.file(TEST_DIR, "archetype-blue.xml", "75beee71-edaa-4737-8793-8dd66eb4babf"); // structural
    private static final TestObject<ArchetypeType> ARCHETYPE_MAGENTA =
            TestObject.file(TEST_DIR, "archetype-magenta.xml", "0a7b2a81-d682-4dd0-803d-d47cbee6cc96"); // auxiliary
    private static final TestObject<ArchetypeType> ROLE_TESTER =
            TestObject.file(TEST_DIR, "role-tester.xml", "0d4faf1c-2677-41a4-9026-e0319a335767");
    private static final TestObject<ArchetypeType> ROLE_ADMIN =
            TestObject.file(TEST_DIR, "role-admin.xml", "b9e47537-0d85-4a6c-aeb2-24d4c06db7a6");
    private static final TestObject<ArchetypeType> ROLE_DEVELOPER =
            TestObject.file(TEST_DIR, "role-developer.xml", "d9ed4375-b0af-4cea-ae53-f67ff1e0ca67");
    private static final TestObject<ArchetypeType> ORG_HQ =
            TestObject.file(TEST_DIR, "org-hq.xml", "8692de4f-51b3-472f-9489-5a7bdc12e891");

    private static final int EXISTING_USERS = 10;

    private static final int OBJECT_REPORT_COLUMNS = 11;

    private static final int C_ID = 0;
    private static final int C_OID = 1;
    private static final int C_NAME = 2;
    private static final int C_TYPE = 3;
    private static final int C_ARCHETYPE = 4;
    private static final int C_RESOURCE = 5;
    private static final int C_KIND = 6;
    private static final int C_INTENT = 7;
    private static final int C_TAG = 8;
    private static final int C_STATE = 9;
    private static final int C_MARK = 10;
    private static final int C_ITEM_CHANGED = 11;
    // Item-level
    private static final int C_OLD_VALUES = 12;
    private static final int C_NEW_VALUES = 13;
    private static final int C_VALUES_ADDED = 14;
    private static final int C_VALUES_DELETED = 15;
    private static final int C_I_RELATED_ASSIGNMENT = 16;
    private static final int C_I_RELATED_ASSIGNMENT_ID = 17;
    private static final int C_I_RELATED_ASSIGNMENT_TARGET = 18;
    private static final int C_I_RELATED_ASSIGNMENT_RELATION = 19;
    private static final int C_I_RELATED_ASSIGNMENT_RESOURCE = 20;
    private static final int C_I_RELATED_ASSIGNMENT_KIND = 21;
    private static final int C_I_RELATED_ASSIGNMENT_INTENT = 22;
    // Value-level
    private static final int C_VALUE_STATE = 12;
    private static final int C_VALUE = 13;
    private static final int C_V_RELATED_ASSIGNMENT = 14;
    private static final int C_V_RELATED_ASSIGNMENT_ID = 15;
    private static final int C_V_RELATED_ASSIGNMENT_TARGET = 16;
    private static final int C_V_RELATED_ASSIGNMENT_RELATION = 17;
    private static final int C_V_RELATED_ASSIGNMENT_RESOURCE = 18;
    private static final int C_V_RELATED_ASSIGNMENT_KIND = 19;
    private static final int C_V_RELATED_ASSIGNMENT_INTENT = 20;

    private List<UserType> existingUsers;

    @BeforeMethod
    public void onNativeOnly() {
        skipIfNotNativeRepository();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // Only for Native repo, as Generic repo does not support simulations
        if (!isNativeRepository()) {
            return;
        }
        super.initSystem(initTask, initResult);

        var resources = repositoryService.searchObjects(ResourceType.class, null, createNoFetchCollection(), initResult);
        display("resources", resources);

        CommonInitialObjects.addMarks(this, initTask, initResult);

        ARCHETYPE_BLUE.init(this, initTask, initResult);
        ARCHETYPE_MAGENTA.init(this, initTask, initResult);
        ROLE_TESTER.init(this, initTask, initResult);
        ROLE_ADMIN.init(this, initTask, initResult);
        ROLE_DEVELOPER.init(this, initTask, initResult);
        ORG_HQ.init(this, initTask, initResult);

        existingUsers = modelObjectCreatorFor(UserType.class)
                .withObjectCount(EXISTING_USERS)
                .withNamePattern("existing-%04d")
                .withCustomizer((u, number) -> u.getAssignment().addAll(standardArchetypeAssignments()))
                .execute(initResult);

        RESOURCE_DUMMY_OUTBOUND.initAndTest(this, initTask, initResult);

        REPORT_SIMULATION_OBJECTS.init(this, initTask, initResult);
        REPORT_SIMULATION_OBJECTS_BY_MARKS.init(this, initTask, initResult);
        REPORT_SIMULATION_ITEMS_CHANGED.init(this, initTask, initResult);
        REPORT_SIMULATION_VALUES_CHANGED.init(this, initTask, initResult);
    }

    private static List<AssignmentType> standardArchetypeAssignments() {
        return List.of(
                new AssignmentType().targetRef(ARCHETYPE_BLUE.ref()),
                new AssignmentType().targetRef(ARCHETYPE_MAGENTA.ref()));
    }

    @Test
    public void test100CreateNewUsers() throws Exception {
        int users = 10;

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("users are created in simulation mode");
        var simulationResult = executeWithSimulationResult(
                task, result,
                () -> modelObjectCreatorFor(UserType.class)
                        .withObjectCount(users)
                        .withNamePattern("new-%04d")
                        .withCustomizer((u, number) -> u.getAssignment().addAll(standardArchetypeAssignments()))
                        .execute(result));

        then("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .assertSize(users);

        when("object-level report is created");
        var lines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("it is OK");
        assertCsv(lines, "after")
                .sortBy(C_NAME)
                .display()
                .assertRecords(users)
                .assertColumns(OBJECT_REPORT_COLUMNS)
                .record(0)
                .assertValue(C_NAME, "new-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ARCHETYPE, "blue")
                .assertValue(C_RESOURCE, "")
                .assertValue(C_KIND, "")
                .assertValue(C_INTENT, "")
                .assertValue(C_TAG, "")
                .assertValue(C_STATE, "Added")
                .assertValues(C_MARK, "Focus activated")
                .end();

        when("object-level report is created (by marks)");
        var byMarksLines = REPORT_SIMULATION_OBJECTS_BY_MARKS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("it is OK");
        assertCsv(byMarksLines, "after")
                .sortBy(C_NAME)
                .display()
                .assertRecords(users)
                .assertColumns(OBJECT_REPORT_COLUMNS)
                .record(0)
                .assertValue(C_NAME, "new-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Added")
                .assertValues(C_MARK, "Focus activated")
                .end();
    }

    @Test
    public void test110DisableAndRenameUsers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("users are renamed and optionally disabled in simulation mode");
        var simulationResult = executeWithSimulationResult(
                task, result,
                () -> {
                    for (int i = 0; i < existingUsers.size(); i++) {
                        UserType existingUser = existingUsers.get(i);
                        modelService.executeChanges(
                                List.of(deltaFor(UserType.class)
                                        .optimizing()
                                        .item(UserType.F_NAME)
                                        .replace(PolyString.fromOrig(existingUser.getName().getOrig() + "-renamed"))
                                        .item(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)
                                        .old().replace(i%2 == 0 ? ActivationStatusType.DISABLED : null)
                                        .asObjectDelta(existingUser.getOid())),
                                null, task, result);
                    }
                });

        then("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .assertSize(EXISTING_USERS);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .sortBy(C_NAME)
                .display()
                .assertRecords(EXISTING_USERS)
                .assertColumns(OBJECT_REPORT_COLUMNS)
                .record(0)
                .assertValue(C_OID, existingUsers.get(0).getOid())
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValues(C_MARK, "Focus renamed", "Focus deactivated")
                .end()
                .record(1)
                .assertValue(C_OID, existingUsers.get(1).getOid())
                .assertValue(C_NAME, "existing-0001-renamed")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValues(C_MARK, "Focus renamed");

        when("object-level report (by marks) is created");
        var byMarksLines = REPORT_SIMULATION_OBJECTS_BY_MARKS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(byMarksLines, "after")
                .sortBy(C_NAME, C_MARK)
                .display()
                .assertRecords((int) (EXISTING_USERS * 1.5))
                .assertColumns(OBJECT_REPORT_COLUMNS)
                .record(0)
                .assertValue(C_OID, existingUsers.get(0).getOid())
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValues(C_MARK, "Focus deactivated")
                .end()
                .record(1)
                .assertValue(C_OID, existingUsers.get(0).getOid())
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValues(C_MARK, "Focus renamed")
                .end()
                .record(2)
                .assertValue(C_OID, existingUsers.get(1).getOid())
                .assertValue(C_NAME, "existing-0001-renamed")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValues(C_MARK, "Focus renamed");

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .sortBy(C_NAME, C_ITEM_CHANGED)
                .display()
                .assertRecords(15) // 10 changes of name, 5 changes of administrative status
                .record(0)
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_ITEM_CHANGED, "activation/administrativeStatus")
                .assertValue(C_OLD_VALUES, "")
                .assertValue(C_NEW_VALUES, "Disabled")
                .assertValue(C_VALUES_ADDED, "Disabled")
                .assertValue(C_VALUES_DELETED, "")
                .end()
                .record(1)
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_ITEM_CHANGED, "name")
                .assertValue(C_OLD_VALUES, "existing-0000")
                .assertValue(C_NEW_VALUES, "existing-0000-renamed")
                .assertValue(C_VALUES_ADDED, "existing-0000-renamed")
                .assertValue(C_VALUES_DELETED, "existing-0000")
                .end();

        when("item-level report is created - all items");
        var itemsLines2 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .withParameter(PARAM_INCLUDE_OPERATIONAL_ITEMS, true)
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines2, "after")
                .parse()
                .display()
                .assertRecords((a) -> a.hasSizeGreaterThan(50)); // too many

        when("item-level report is created - 'name' only");
        var itemsLines3 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .withParameter(PARAM_PATHS_TO_INCLUDE, UserType.F_NAME.toBean())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines3, "after")
                .parse()
                .display()
                .assertRecords(10);

        when("item-level report is created - 'activation/effectiveStatus' only");
        var itemsLines4 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .withParameter(PARAM_PATHS_TO_INCLUDE, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS.toBean())
                .withParameter(PARAM_SHOW_IF_NO_DETAILS, false)
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines4, "after")
                .parse()
                .display()
                .assertRecords(5);

        when("value-level report is created (default)");
        var valuesLines1 = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(valuesLines1, "after")
                .sortBy(C_NAME, C_ITEM_CHANGED, C_VALUE_STATE, C_VALUE)
                .display()
                .assertRecords(25) // 20x name (ADD/DELETE), 5x administrativeStatus (ADD)
                .record(0)
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_ITEM_CHANGED, "activation/administrativeStatus")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "Disabled")
                .end()
                .record(1)
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_ITEM_CHANGED, "name")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "existing-0000-renamed")
                .end()
                .record(2)
                .assertValue(C_NAME, "existing-0000-renamed")
                .assertValue(C_ITEM_CHANGED, "name")
                .assertValue(C_VALUE_STATE, "DELETED")
                .assertValue(C_VALUE, "existing-0000")
                .end();

        when("value-level report is created - all items");
        var valuesLines2 = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .withParameter(PARAM_INCLUDE_OPERATIONAL_ITEMS, true)
                .execute(result);

        then("CSV is OK");
        assertCsv(valuesLines2, "after")
                .parse()
                .display()
                .assertRecords((a) -> a.hasSizeGreaterThan(50)); // too many
    }

    @Test
    public void test120AddAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("account is added (simulated)");
        String userOid = existingUsers.get(0).getOid();
        var simulationResult = executeWithSimulationResult(
                List.of(createModifyUserAddAccount(userOid, RESOURCE_DUMMY_OUTBOUND.get())),
                task, result);
        assertSuccess(result);

        then("simulation result is OK");
        var processedObjects = assertProcessedObjects(simulationResult, "after")
                .display()
                .assertSize(2)
                .getProcessedObjects();

        String shadowOid = processedObjects.stream()
                .filter(po -> ShadowType.class.equals(po.getType()))
                .map(po -> po.getOid())
                .findFirst().orElseThrow(AssertionError::new);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_MARK, "")
                .end()
                .record(1)
                .assertValue(C_NAME, "")
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Added")
                .assertValue(C_MARK, "Projection activated");

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .assertRecords(2);

        when("value-level report is created (default)");
        var valuesLines = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        // These assertions are quite fragile; may change if the report changes.
        assertCsv(valuesLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_MARK, "")
                .assertValue(C_ITEM_CHANGED, "linkRef")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, shadowOid)
                .end()
                .record(1)
                .assertValue(C_NAME, "")
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Added")
                .assertValue(C_MARK, "Projection activated");
    }

    @Test
    public void test130DeleteAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("user with account exist");
        String userOid = existingUsers.get(0).getOid();
        executeChanges(
                createModifyUserAddAccount(userOid, RESOURCE_DUMMY_OUTBOUND.get()),
                null, task, result);
        assertSuccess(result);

        when("account is deleted (simulated)");
        var simulationResult = executeWithSimulationResult(
                List.of(createModifyUserDeleteAccount(userOid, RESOURCE_DUMMY_OUTBOUND.get())),
                task, result);
        assertSuccess(result);

        then("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .assertSize(2);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_MARK, "")
                .end()
                .record(1)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Deleted")
                .assertValue(C_MARK, "Projection deactivated");

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .assertRecords(2);

        when("value-level report is created (default)");
        var valuesLines = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        // These assertions are quite fragile; may change if the report changes.
        assertCsv(valuesLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_MARK, "")
                .assertValue(C_ITEM_CHANGED, "linkRef")
                .assertValue(C_VALUE_STATE, "DELETED")
                .assertValue(C_VALUE, "existing-0000")
                .end()
                .record(1)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Deleted")
                .assertValue(C_MARK, "Projection deactivated");

        then("finally deleting the account");
        executeChanges(
                createModifyUserDeleteAccount(userOid, RESOURCE_DUMMY_OUTBOUND.get()),
                null, task, result);
        assertSuccess(result);
    }

    @Test
    public void test140AssignAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("account is assigned (simulated)");
        String userOid = existingUsers.get(0).getOid();
        var simulationResult = executeWithSimulationResult(
                List.of(createModifyUserAssignAccount(userOid, RESOURCE_DUMMY_OUTBOUND.oid)),
                task, result);
        assertSuccess(result);

        then("simulation result is OK");
        var processedObjects = assertProcessedObjects(simulationResult, "after")
                .display()
                .assertSize(2)
                .getProcessedObjects();

        String shadowOid = processedObjects.stream()
                .filter(po -> ShadowType.class.equals(po.getType()))
                .map(po -> po.getOid())
                .findFirst().orElseThrow(AssertionError::new);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                //.assertValue(5, "")
                .end()
                .record(1)
                .assertValue(C_NAME, "")
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Added")
                .assertValue(C_MARK, "Projection activated");

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .assertRecords(3); // assignment, linkRef, shadow

        when("value-level report is created (default)");
        var valuesLines = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        assertCsv(valuesLines, "after")
                .assertRecords(5)
                .withNumericColumns(C_ID)
                .sortBy(C_ID, C_ITEM_CHANGED, C_VALUE_STATE, C_VALUE)
                .record(0)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ARCHETYPE, "blue")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_MARK, "Focus assignments changed")
                .assertValue(C_ITEM_CHANGED, "assignment")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, a -> a.startsWith("-> resource-outbound:Account/default"))
                .assertValuesEqual(C_VALUE, C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_RESOURCE, "resource-outbound")
                .assertValue(C_V_RELATED_ASSIGNMENT_KIND, "Account")
                .assertValue(C_V_RELATED_ASSIGNMENT_INTENT, "default")
                .end()
                .record(1)
                .assertValue(C_VALUE_STATE, "UNCHANGED")
                .assertValue(C_VALUE, a -> a.startsWith("-> blue"))
                .assertValuesEqual(C_VALUE, C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_TARGET, "blue")
                .end()
                .record(2)
                .assertValue(C_VALUE_STATE, "UNCHANGED")
                .assertValue(C_VALUE, a -> a.startsWith("-> magenta"))
                .assertValuesEqual(C_VALUE, C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_TARGET, "magenta")
                .end()
                .record(3)
                .assertValue(C_NAME, "existing-0000")
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_MARK, "Focus assignments changed")
                .assertValue(C_ITEM_CHANGED, "linkRef")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, shadowOid)
                .end()
                .record(4)
                .assertValue(C_NAME, "")
                .assertValue(C_RESOURCE, "resource-outbound")
                .assertValue(C_KIND, "Account")
                .assertValue(C_INTENT, "default")
                .assertValue(C_TAG, "")
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Added")
                .assertValue(C_MARK, "Projection activated");
    }

    @Test
    public void test150ModifyUserWithAssignedAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user");
        String userName = getTestNameShort();
        UserType user = new UserType()
                .name(userName)
                .fullName("Jack Sparrow")
                .assignment(new AssignmentType()
                        .construction(new ConstructionType()
                                .resourceRef(RESOURCE_DUMMY_OUTBOUND.oid, ResourceType.COMPLEX_TYPE)))
                .assignment(new AssignmentType()
                        .targetRef(ARCHETYPE_BLUE.ref()));
        addObject(user.asPrismObject(), task, result);

        when("account is modified via user (simulated)");
        var simulationResult = executeWithSimulationResult(
                List.of(deltaFor(UserType.class)
                        .item(UserType.F_FULL_NAME)
                        .replace(PolyString.fromOrig("Jackie Sparrow"))
                        .item(UserType.F_LOCALITY)
                        .replace(PolyString.fromOrig("Caribbean"))
                        .asObjectDelta(user.getOid())),
                task, result);
        assertSuccess(result);

        then("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .assertSize(2);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ARCHETYPE, "blue")
                .assertValue(C_STATE, "Modified")
                .end()
                .record(1)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_RESOURCE, "resource-outbound")
                .assertValue(C_KIND, "Account")
                .assertValue(C_INTENT, "default")
                .assertValue(C_TAG, "")
                .assertValue(C_STATE, "Modified");

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .assertRecords(4); // fullName/location for user, fullname/locality for shadow

        when("value-level report is created (default)");
        var valuesLines = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        assertCsv(valuesLines, "after")
                .assertRecords(6)
                .withNumericColumns(C_ID)
                .sortBy(C_ID, C_ITEM_CHANGED, C_VALUE_STATE, C_VALUE)
                .record(0)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ARCHETYPE, "blue")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_ITEM_CHANGED, "fullName")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "Jackie Sparrow")
                .end()
                .record(1)
                .assertValue(C_ITEM_CHANGED, "fullName")
                .assertValue(C_VALUE_STATE, "DELETED")
                .assertValue(C_VALUE, "Jack Sparrow")
                .end()
                .record(2)
                .assertValue(C_ITEM_CHANGED, "locality")
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "Caribbean")
                .end()
                .record(3)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_RESOURCE, "resource-outbound")
                .assertValue(C_KIND, "Account")
                .assertValue(C_INTENT, "default")
                .assertValue(C_TAG, "")
                .assertValue(C_ITEM_CHANGED, "attributes/fullname")
                .assertValue(C_VALUE_STATE, "ADDED")
                //.assertValue(C_VALUE, "FIXME")
                .end()
                .record(4)
                .assertValue(C_ITEM_CHANGED, "attributes/fullname")
                .assertValue(C_VALUE_STATE, "DELETED")
                //.assertValue(C_VALUE, "FIXME")
                .end()
                .record(5)
                .assertValue(C_ITEM_CHANGED, "attributes/location")
                .assertValue(C_VALUE_STATE, "ADDED")
                //.assertValue(C_VALUE, "FIXME")
                .end();
    }

    @Test
    public void test160ModifyAssignments() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user");
        String userName = getTestNameShort();
        UserType user = new UserType()
                .name(userName)
                .fullName("Jack Sparrow")
                .assignment(new AssignmentType()
                        .construction(new ConstructionType()
                                .resourceRef(RESOURCE_DUMMY_OUTBOUND.ref())
                                .kind(ShadowKindType.ACCOUNT)
                                .intent("default")))
                .assignment(new AssignmentType().targetRef(ARCHETYPE_BLUE.ref()))
                .assignment(new AssignmentType().targetRef(ROLE_TESTER.ref()))
                .assignment(new AssignmentType().targetRef(ROLE_ADMIN.ref()));
        addObject(user.asPrismObject(), task, result);
        var userReloaded = repositoryService.getObject(UserType.class, user.getOid(), null, result);

        long adminAssignmentId = findAssignmentByTargetRequired(userReloaded, ROLE_ADMIN.oid).getId();
        long testerAssignmentId = findAssignmentByTargetRequired(userReloaded, ROLE_TESTER.oid).getId();
        long blueAssignmentId = findAssignmentByTargetRequired(userReloaded, ARCHETYPE_BLUE.oid).getId();
        long dummyAssignmentId = findAssignmentByResourceRequired(userReloaded, RESOURCE_DUMMY_OUTBOUND.oid).getId();

        ItemPath pathTesterAssignment = ItemPath.create(UserType.F_ASSIGNMENT, testerAssignmentId, AssignmentType.F_ORG_REF);
        ItemPath pathDummyAssignment = ItemPath.create(
                UserType.F_ASSIGNMENT, dummyAssignmentId, AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);



        when("assignments are modified (simulated)");
        var simulationResult = executeWithSimulationResult(
                List.of(deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .delete(new AssignmentType().id(adminAssignmentId))
                        .item(pathTesterAssignment)
                        .replace(ORG_HQ.ref())
                        .item(pathDummyAssignment)
                        .replace(ActivationStatusType.DISABLED)
                        .asObjectDelta(user.getOid())),
                task, result);
        assertSuccess(result);

        then("simulation result is OK");
        assertProcessedObjects(simulationResult, "after")
                .display()
                .assertSize(2);

        when("object-level report is created");
        var objectsLines = REPORT_SIMULATION_OBJECTS.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(objectsLines, "after")
                .assertRecords(2)
                .record(0)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ARCHETYPE, "blue")
                .assertValue(C_STATE, "Modified")
                .end()
                .record(1)
                .assertValue(C_NAME, userName)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_RESOURCE, "resource-outbound")
                .assertValue(C_KIND, "Account")
                .assertValue(C_INTENT, "default")
                .assertValue(C_TAG, "")
                .assertValue(C_STATE, "Deleted");

        when("item-level report is created (default)");
        var itemsLines1 = REPORT_SIMULATION_ITEMS_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        // Hacking assignment/[1]/activation/administrativeStatus vs assignment/1/activation/administrativeStatus
        String pathDummyAssignmentAsString = prismContext.toUniformPath(pathDummyAssignment).toString();
        String pathTesterAssignmentAsString = prismContext.toUniformPath(pathTesterAssignment).toString();

        boolean dummyIsFirst = pathDummyAssignmentAsString.compareTo(pathTesterAssignmentAsString) < 0;
        System.out.println("dummy first: " + dummyIsFirst);

        then("CSV is OK");
        assertCsv(itemsLines1, "after")
                .withNumericColumns(C_ID)
                .sortBy(C_ID, C_ITEM_CHANGED)
                .display()
                .assertRecords(5) // 3x assignment, 1x linkRef, 1x shadow
                .record(0)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "assignment")
                .assertValue(C_I_RELATED_ASSIGNMENT, "") // no single related assignment
                .end()
                .record(dummyIsFirst ? 1 : 2)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, pathDummyAssignmentAsString)
                .assertValueNotEmpty(C_I_RELATED_ASSIGNMENT)
                .assertValue(C_I_RELATED_ASSIGNMENT_ID, String.valueOf(dummyAssignmentId))
                .assertValue(C_I_RELATED_ASSIGNMENT_TARGET, "")
                .assertValue(C_I_RELATED_ASSIGNMENT_RELATION, "")
                .assertValue(C_I_RELATED_ASSIGNMENT_RESOURCE, "resource-outbound")
                .assertValue(C_I_RELATED_ASSIGNMENT_KIND, "Account")
                .assertValue(C_I_RELATED_ASSIGNMENT_INTENT, "default")
                .end()
                .record(dummyIsFirst ? 2 : 1)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, pathTesterAssignmentAsString)
                .assertValueNotEmpty(C_I_RELATED_ASSIGNMENT)
                .assertValue(C_I_RELATED_ASSIGNMENT_ID, String.valueOf(testerAssignmentId))
                .assertValue(C_I_RELATED_ASSIGNMENT_TARGET, "tester")
                .assertValue(C_I_RELATED_ASSIGNMENT_RELATION, "default")
                .assertValue(C_I_RELATED_ASSIGNMENT_RESOURCE, "")
                .assertValue(C_I_RELATED_ASSIGNMENT_KIND, "")
                .assertValue(C_I_RELATED_ASSIGNMENT_INTENT, "")
                .end()
                .record(3)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "linkRef")
                .assertValue(C_I_RELATED_ASSIGNMENT, "")
                .end()
                .record(4)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Deleted")
                .end();

        when("value-level report is created (default)");
        var valuesLines = REPORT_SIMULATION_VALUES_CHANGED.export()
                .withDefaultParametersValues(simulationResult.getSimulationResultRef())
                .execute(result);

        then("CSV is OK");
        assertCsv(valuesLines, "after")
                .withNumericColumns(C_ID)
                .sortBy(C_ID, C_ITEM_CHANGED, C_VALUE_STATE, C_VALUE)
                .display()
                .assertRecords(8)
                // "assignment";"DELETED";"-> admin [4]";"-> admin [4]";"4";"admin";"default";"";"";""
                .record(0)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "assignment")
                .assertValue(C_VALUE_STATE, "DELETED")
                .assertValueNotEmpty(C_VALUE)
                .assertValuesEqual(C_VALUE, C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_ID, String.valueOf(adminAssignmentId))
                .assertValue(C_V_RELATED_ASSIGNMENT_TARGET, "admin")
                .assertValue(C_V_RELATED_ASSIGNMENT_RELATION, "default")
                .assertValue(C_V_RELATED_ASSIGNMENT_RESOURCE, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_KIND, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_INTENT, "")
                .end()
                // "assignment";"MODIFIED";"-> resource-outbound:Account/default [1]";"-> resource-outbound:Account/default [1]";"1";"";"";"resource-outbound";"Account";"default"
                .record(1)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "assignment")
                .assertValue(C_VALUE_STATE, "MODIFIED")
                .assertValueNotEmpty(C_VALUE)
                .assertValuesEqual(C_VALUE, C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_ID, String.valueOf(dummyAssignmentId))
                .assertValue(C_V_RELATED_ASSIGNMENT_TARGET, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_RELATION, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_RESOURCE, "resource-outbound")
                .assertValue(C_V_RELATED_ASSIGNMENT_KIND, "Account")
                .assertValue(C_V_RELATED_ASSIGNMENT_INTENT, "default")
                .end()
                // "assignment";"MODIFIED";"-> tester [3]";"-> tester [3]";"3";"tester";"default";"";"";""
                .record(2)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "assignment")
                .assertValue(C_VALUE_STATE, "MODIFIED")
                .assertValueNotEmpty(C_VALUE)
                .assertValuesEqual(C_VALUE, C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_ID, String.valueOf(testerAssignmentId))
                .assertValue(C_V_RELATED_ASSIGNMENT_TARGET, "tester")
                .assertValue(C_V_RELATED_ASSIGNMENT_RELATION, "default")
                .assertValue(C_V_RELATED_ASSIGNMENT_RESOURCE, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_KIND, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_INTENT, "")
                .end()
                // "assignment";"UNCHANGED";"-> blue [2]";"-> blue [2]";"2";"blue";"default";"";"";""
                .record(3)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, "assignment")
                .assertValue(C_VALUE_STATE, "UNCHANGED")
                .assertValueNotEmpty(C_VALUE)
                .assertValuesEqual(C_VALUE, C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_ID, String.valueOf(blueAssignmentId))
                .assertValue(C_V_RELATED_ASSIGNMENT_TARGET, "blue")
                .assertValue(C_V_RELATED_ASSIGNMENT_RELATION, "default")
                .assertValue(C_V_RELATED_ASSIGNMENT_RESOURCE, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_KIND, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_INTENT, "")
                .end()
                // "assignment/[1]/activation/administrativeStatus";"ADDED";"Disabled";"-> resource-outbound:Account/default Disabled [1]";"1";"";"";"resource-outbound";"Account";"default"
                .record(dummyIsFirst ? 4 : 5)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, pathDummyAssignmentAsString)
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "Disabled")
                .assertValueNotEmpty(C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_ID, String.valueOf(dummyAssignmentId))
                .assertValue(C_V_RELATED_ASSIGNMENT_TARGET, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_RELATION, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_RESOURCE, "resource-outbound")
                .assertValue(C_V_RELATED_ASSIGNMENT_KIND, "Account")
                .assertValue(C_V_RELATED_ASSIGNMENT_INTENT, "default")
                .end()
                // "assignment/[3]/orgRef";"ADDED";"hq";"-> tester [3]";"3";"tester";"default";"";"";""
                .record(dummyIsFirst ? 5 : 4)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_ITEM_CHANGED, pathTesterAssignmentAsString)
                .assertValue(C_VALUE_STATE, "ADDED")
                .assertValue(C_VALUE, "hq")
                .assertValueNotEmpty(C_V_RELATED_ASSIGNMENT)
                .assertValue(C_V_RELATED_ASSIGNMENT_ID, String.valueOf(testerAssignmentId))
                .assertValue(C_V_RELATED_ASSIGNMENT_TARGET, "tester")
                .assertValue(C_V_RELATED_ASSIGNMENT_RELATION, "default")
                .assertValue(C_V_RELATED_ASSIGNMENT_RESOURCE, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_KIND, "")
                .assertValue(C_V_RELATED_ASSIGNMENT_INTENT, "")
                .end()
                .record(6)
                .assertValue(C_TYPE, "UserType")
                .assertValue(C_STATE, "Modified")
                .assertValue(C_ITEM_CHANGED, "linkRef")
                .assertValue(C_VALUE_STATE, "DELETED")
                .assertValue(C_VALUE, userName)
                .end()
                .record(7)
                .assertValue(C_TYPE, "ShadowType")
                .assertValue(C_STATE, "Deleted")
                .end();
    }
}
