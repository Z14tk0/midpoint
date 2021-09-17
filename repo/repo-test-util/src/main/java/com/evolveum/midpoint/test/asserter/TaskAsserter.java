/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.*;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.evolveum.midpoint.schema.util.task.TaskResolver.empty;
import static com.evolveum.midpoint.util.MiscUtil.assertCheck;

import static java.util.Objects.requireNonNullElseGet;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

@SuppressWarnings("UnusedReturnValue")
public class TaskAsserter<RA> extends AssignmentHolderAsserter<TaskType, RA> {

    private TaskAsserter(PrismObject<TaskType> object) {
        super(object);
    }

    private TaskAsserter(PrismObject<TaskType> object, String details) {
        super(object, details);
    }

    private TaskAsserter(PrismObject<TaskType> object, RA returnAsserter, String details) {
        super(object, returnAsserter, details);
    }

    @SuppressWarnings("unused")
    public static TaskAsserter<Void> forTask(PrismObject<TaskType> object) {
        return new TaskAsserter<>(object);
    }

    public static TaskAsserter<Void> forTask(PrismObject<TaskType> object, String details) {
        return new TaskAsserter<>(object, details);
    }

    // It is insane to override all those methods from superclass.
    // But there is no better way to specify something like <SELF> type in Java.
    // This is lesser evil.
    @Override
    public TaskAsserter<RA> assertOid() {
        super.assertOid();
        return this;
    }

    @Override
    public TaskAsserter<RA> assertOid(String expected) {
        super.assertOid(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertOidDifferentThan(String oid) {
        super.assertOidDifferentThan(oid);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertName() {
        super.assertName();
        return this;
    }

    @Override
    public TaskAsserter<RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertDescription(String expected) {
        super.assertDescription(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertNoDescription() {
        super.assertNoDescription();
        return this;
    }

    @Override
    public TaskAsserter<RA> assertSubtype(String... expected) {
        super.assertSubtype(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertTenantRef(String expectedOid) {
        super.assertTenantRef(expectedOid);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertLifecycleState(String expected) {
        super.assertLifecycleState(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertActiveLifecycleState() {
        super.assertActiveLifecycleState();
        return this;
    }

    public TaskAsserter<RA> display() {
        super.display();
        return this;
    }

    public TaskAsserter<RA> displayOperationResult() {
        OperationResultType resultBean = getTaskBean().getResult();
        if (resultBean != null) {
            IntegrationTestTools.display(desc() + " operation result:\n" + OperationResult.createOperationResult(resultBean).debugDump(1));
        } else {
            IntegrationTestTools.display(desc() + " has no operation result");
        }
        return this;
    }

    public TaskAsserter<RA> display(String message) {
        super.display(message);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertArchetypeRef(String expectedArchetypeOid) {
        return (TaskAsserter<RA>) super.assertArchetypeRef(expectedArchetypeOid);
    }

    @Override
    public TaskAsserter<RA> assertNoItem(ItemPath itemPath) {
        super.assertNoItem(itemPath);
        return this;
    }

    public TaskAsserter<RA> assertProgress(long expected) {
        long actual = defaultIfNull(getObject().asObjectable().getProgress(), 0L);
        assertEquals("Wrong progress", expected, actual);
        return this;
    }

    public TaskAsserter<RA> assertToken(Object expected) {
        Object token;
        try {
            token = ActivityStateUtil.getRootSyncTokenRealValue(getObjectable());
        } catch (SchemaException e) {
            throw new AssertionError(e);
        }
        assertThat(token).as("token").isEqualTo(expected);
        return this;
    }

    @Override
    public TaskAsserter<RA> assertPolyStringProperty(QName propName, String expectedOrig) {
        return (TaskAsserter<RA>) super.assertPolyStringProperty(propName, expectedOrig);
    }

    public SynchronizationInfoAsserter<TaskAsserter<RA>> rootSynchronizationInformation() {
        return synchronizationInformation(ActivityPath.empty());
    }

    public SynchronizationInfoAsserter<TaskAsserter<RA>> synchronizationInformation(ActivityPath activityPath) {
        ActivityStatisticsType statistics = getStatisticsOrNew(activityPath);
        ActivitySynchronizationStatisticsType syncStatistics = requireNonNullElseGet(
                statistics.getSynchronization(), () -> new ActivitySynchronizationStatisticsType(getPrismContext()));

        SynchronizationInfoAsserter<TaskAsserter<RA>> asserter = new SynchronizationInfoAsserter<>(syncStatistics, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    /** Assumes single primitive activity */
    public ActivityItemProcessingStatisticsAsserter<TaskAsserter<RA>> rootItemProcessingInformation() {
        ActivityStatisticsType statistics = getStatisticsOrNew(ActivityPath.empty());
        ActivityItemProcessingStatisticsType itemProcessingStatistics = requireNonNullElseGet(
                statistics.getItemProcessing(), () -> new ActivityItemProcessingStatisticsType(getPrismContext()));

        ActivityItemProcessingStatisticsAsserter<TaskAsserter<RA>> asserter =
                new ActivityItemProcessingStatisticsAsserter<>(itemProcessingStatistics, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public TaskActivityStateAsserter<TaskAsserter<RA>> activityState() {
        TaskActivityStateType activityState = Objects.requireNonNull(
                getObject().asObjectable().getActivityState(), "no activity state");
        TaskActivityStateAsserter<TaskAsserter<RA>> asserter = new TaskActivityStateAsserter<>(activityState, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ActivityStateAsserter<TaskAsserter<RA>> rootActivityState() {
        return activityState(ActivityPath.empty());
    }

    public ActivityStateAsserter<TaskAsserter<RA>> activityState(ActivityPath activityPath) {
        ActivityStateType state = getActivityStateRequired(activityPath);
        ActivityStateAsserter<TaskAsserter<RA>> asserter = new ActivityStateAsserter<>(state, this, "activity state for " + activityPath.toDebugName() + " in " + getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    @SuppressWarnings("unused")
    public ActivityStateOverviewAsserter<TaskAsserter<RA>> rootActivityStateOverview() {
        var overview =
                Objects.requireNonNull(
                        Objects.requireNonNull(
                                Objects.requireNonNull(
                                                getTaskBean().getActivityState(), "no activities state")
                                        .getTree(), "no tree")
                                .getActivity(), "no root activity overview");

        ActivityStateOverviewAsserter<TaskAsserter<RA>> asserter =
                new ActivityStateOverviewAsserter<>(overview, this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    /**
     * Assumes that the whole task tree is fully loaded!
     */
    public ActivityProgressInformationAsserter<TaskAsserter<RA>> progressInformation() {
        ActivityProgressInformationAsserter<TaskAsserter<RA>> asserter =
                new ActivityProgressInformationAsserter<>(
                        ActivityProgressInformation.fromRootTask(getObjectable(), TaskResolver.empty()),
                        this,
                        getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public TaskAsserter<RA> assertClosed() {
        assertExecutionStatus(TaskExecutionStateType.CLOSED);
        assertSchedulingState(TaskSchedulingStateType.CLOSED);
        return this;
    }

    public TaskAsserter<RA> assertSuspended() {
        assertExecutionStatus(TaskExecutionStateType.SUSPENDED);
        assertSchedulingState(TaskSchedulingStateType.SUSPENDED);
        return this;
    }

    public TaskAsserter<RA> assertExecutionStatus(TaskExecutionStateType status) {
        assertEquals("Wrong execution status", status, getTaskBean().getExecutionStatus());
        return this;
    }

    public TaskAsserter<RA> assertSchedulingState(TaskSchedulingStateType state) {
        assertEquals("Wrong scheduling state", state, getTaskBean().getSchedulingState());
        return this;
    }

    private TaskType getTaskBean() {
        return getObject().asObjectable();
    }

    public TaskAsserter<RA> assertSuccess() {
        OperationResultType result = getTaskBean().getResult();
        if (result != null) {
            TestUtil.assertSuccess(result);
        } else {
            assertThat(getTaskBean().getResultStatus())
                    .as("result status")
                    .isIn(OperationResultStatusType.SUCCESS,
                            OperationResultStatusType.NOT_APPLICABLE,
                            OperationResultStatusType.HANDLED_ERROR);
        }
        return this;
    }

    public TaskAsserter<RA> assertHandledError() {
        OperationResultType result = getTaskBean().getResult();
        if (result != null) {
            TestUtil.assertStatus(result, OperationResultStatusType.HANDLED_ERROR);
        } else {
            assertThat(getTaskBean().getResultStatus())
                    .as("result status")
                    .isEqualTo(OperationResultStatusType.HANDLED_ERROR);
        }
        return this;
    }

    public TaskAsserter<RA> assertPartialError() {
        OperationResultType result = getTaskBean().getResult();
        if (result != null) {
            TestUtil.assertPartialError(result);
        } else {
            assertThat(getTaskBean().getResultStatus())
                    .as("result status")
                    .isEqualTo(OperationResultStatusType.PARTIAL_ERROR);
        }
        return this;
    }

    public TaskAsserter<RA> assertFatalError() {
        OperationResultType result = getTaskBean().getResult();
        if (result != null) {
            TestUtil.assertFatalError(result);
        } else {
            assertThat(getTaskBean().getResultStatus())
                    .as("result status")
                    .isEqualTo(OperationResultStatusType.FATAL_ERROR);
        }
        return this;
    }

    public TaskAsserter<RA> assertCategory(String category) {
        assertEquals(category, getTaskBean().getCategory());
        return this;
    }

    public TaskAsserter<RA> assertBinding(TaskBindingType binding) {
        assertEquals(binding, getTaskBean().getBinding());
        return this;
    }

    @Override
    public AssignmentsAsserter<TaskType, TaskAsserter<RA>, RA> assignments() {
        AssignmentsAsserter<TaskType, TaskAsserter<RA>, RA> asserter = new AssignmentsAsserter<>(this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ObjectReferenceAsserter<UserType, RA> owner() {
        ObjectReferenceAsserter<UserType, RA> ownerAsserter = new ObjectReferenceAsserter<>(getTaskBean().getOwnerRef().asReferenceValue(), UserType.class);
        copySetupTo(ownerAsserter);
        return ownerAsserter;
    }

    public TaskAsserter<TaskAsserter<RA>> subtaskForPath(ActivityPath activityPath) {
        TaskType subtask =
                MiscUtil.extractSingletonRequired(
                        ActivityTreeUtil.getSubtasksForPath(getObjectable(), activityPath, empty()),
                        () -> new AssertionError("More than one subtask for activity path '" + activityPath + "'"),
                        () -> new AssertionError("No subtask for activity path '" + activityPath + "' found"));

        TaskAsserter<TaskAsserter<RA>> asserter = new TaskAsserter<>(subtask.asPrismObject(), this, "subtask for path '" +
                activityPath + "' in " + getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public TaskAsserter<RA> assertSubtasks(int count) {
        assertThat(getObjectable().getSubtaskRef()).as("subtasks").hasSize(count);
        return this;
    }

    public TaskAsserter<TaskAsserter<RA>> subtask(int index) {
        List<ObjectReferenceType> subtasks = getObjectable().getSubtaskRef();
        assertCheck(subtasks.size() > index, "Expected to see at least %s subtask(s), but only %s are present",
                index + 1, subtasks.size());

        return subtask(subtasks, index);
    }

    public TaskAsserter<TaskAsserter<RA>> subtask(String name) {
        List<String> otherNames = new ArrayList<>();

        List<ObjectReferenceType> subtasks = getObjectable().getSubtaskRef();
        for (int i = 0; i < subtasks.size(); i++) {
            TaskType subtask = subtaskFromRef(subtasks, i);
            String subtaskName = subtask.getName().getOrig();
            if (subtaskName.equals(name)) {
                return subtask(subtasks, i);
            } else {
                otherNames.add(subtaskName);
            }
        }
        throw new AssertionError("No subtask with the name '" + name + "' found. Subtasks: " + otherNames);
    }

    private @NotNull TaskAsserter<TaskAsserter<RA>> subtask(List<ObjectReferenceType> subtasks, int index) {
        TaskType subtask = subtaskFromRef(subtasks, index);

        TaskAsserter<TaskAsserter<RA>> asserter = new TaskAsserter<>(subtask.asPrismObject(), this,
                "subtask #" + index + " in " + getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    private @NotNull TaskType subtaskFromRef(List<ObjectReferenceType> subtasks, int index) {
        ObjectReferenceType subtaskRef = subtasks.get(index);
        TaskType subtask = (TaskType) ObjectTypeUtil.getObjectFromReference(subtaskRef);
        assertThat(subtask).withFailMessage(() -> "Reference for subtask #" + index + " contains no object").isNotNull();
        return subtask;
    }

    public TaskAsserter<RA> assertLastTriggerScanTimestamp(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        // Trigger Scan is running as a root activity.
        TestUtil.assertBetween("last scan timestamp in " + desc(), start, end, getLastScanTimestamp(ActivityPath.empty()));
        return this;
    }

    public TaskAsserter<RA> assertLastScanTimestamp(ActivityPath activityPath, XMLGregorianCalendar start,
            XMLGregorianCalendar end) {
        TestUtil.assertBetween("last scan timestamp in " + desc(), start, end, getLastScanTimestamp(activityPath));
        return this;
    }

    public XMLGregorianCalendar getLastScanTimestamp(ActivityPath activityPath) {
        return getActivityWorkState(activityPath, ScanWorkStateType.class)
                .getLastScanTimestamp();
    }

    public TaskAsserter<RA> assertCachingProfiles(String... expected) {
        assertThat(getCachingProfiles()).as("caching profiles").containsExactlyInAnyOrder(expected);
        return this;
    }

    private Collection<String> getCachingProfiles() {
        TaskExecutionEnvironmentType env = getObjectable().getExecutionEnvironment();
        return env != null ? env.getCachingProfile() : List.of();
    }

    @SuppressWarnings("SameParameterValue")
    private <T extends AbstractActivityWorkStateType> T getActivityWorkState(ActivityPath activityPath, Class<T> expectedClass) {
        AbstractActivityWorkStateType workState = getActivityStateRequired(activityPath).getWorkState();
        assertThat(workState).as("work state").isInstanceOf(expectedClass);
        //noinspection unchecked
        return (T) workState;
    }

    private @NotNull ActivityStateType getActivityStateRequired(ActivityPath activityPath) {
        ActivityStateType state = ActivityStateUtil.getActivityState(getTaskBean(), activityPath);
        assertThat(state).withFailMessage("No task activity state").isNotNull();
        return state;
    }

    private ActivityStatisticsType getStatisticsOrNew(ActivityPath activityPath) {
        ActivityStateType state = getActivityStateRequired(activityPath);
        return requireNonNullElseGet(
                state.getStatistics(),
                () -> new ActivityStatisticsType(getPrismContext()));
    }

    /**
     * Loads immediate subtasks, if they are not loaded yet.
     */
    public TaskAsserter<RA> loadImmediateSubtasks(OperationResult result) throws SchemaException {
        TaskType task = getObjectable();
        if (!task.getSubtaskRef().isEmpty()) {
            return this; // assuming subtasks are already loaded
        }

        doLoadImmediateSubtasks(task, result);
        return this;
    }

    private void doLoadImmediateSubtasks(TaskType task, OperationResult result) throws SchemaException {
        ObjectQuery query = getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_PARENT).eq(task.getTaskIdentifier())
                .build();
        SearchResultList<PrismObject<TaskType>> children =
                getRepositoryService().searchObjects(TaskType.class, query, null, result);

        task.getSubtaskRef().clear();
        children.forEach(child ->
                task.getSubtaskRef().add(
                        ObjectTypeUtil.createObjectRefWithFullObject(child, PrismContext.get())));
    }

    /**
     * Loads all subtasks i.e. the whole subtree.
     */
    public TaskAsserter<RA> loadSubtasksDeeply(OperationResult result) throws SchemaException {
        doLoadSubtasksDeeply(getObjectable(), result);
        return this;
    }

    private void doLoadSubtasksDeeply(TaskType task, OperationResult result) throws SchemaException {
        doLoadImmediateSubtasks(task, result);
        List<ObjectReferenceType> subtaskRefList = task.getSubtaskRef();
        for (int i = 0; i < subtaskRefList.size(); i++) {
            TaskType subtask = subtaskFromRef(subtaskRefList, i);
            doLoadSubtasksDeeply(subtask, result);
        }
    }

    public TaskAsserter<RA> assertExecutionGroup(String expected) {
        assertThat(getExecutionGroup()).as("execution group").isEqualTo(expected);
        return this;
    }

    private String getExecutionGroup() {
        TaskType task = getObjectable();
        return task.getExecutionConstraints() != null ? task.getExecutionConstraints().getGroup() : null;
    }

    public TaskAsserter<RA> sendOid(Consumer<String> consumer) {
        super.sendOid(consumer);
        return this;
    }
}
