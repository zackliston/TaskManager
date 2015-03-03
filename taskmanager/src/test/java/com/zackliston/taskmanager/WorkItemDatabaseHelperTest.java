package com.zackliston.taskmanager;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.mockito.Mockito.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

/**
 * Created by Zack Liston on 2/27/15.
 */

@Config(manifest=Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class WorkItemDatabaseHelperTest
{
    //region Constants
    private static final int    DATABASE_VERSION        = 1;
    private static final String DATABASE_NAME           = "WorkItemDB";
    private static final String WORK_ITEM_TABLE_NAME    = "work_item";
    private static final String DEFAULT_ID_COLUMN       = "id";
    private static final String TASK_TYPE_COLUMN        = "task_type";
    private static final String STATE_COLUMN            = "state";
    private static final String DATA_COLUMN             = "data";
    private static final String MAJOR_PRIORITY_COLUMN   = "major_priority";
    private static final String MINOR_PRIORITY_COLUMN   = "minor_priority";
    private static final String RETRY_COUNT_COLUMN      = "retry_count";
    private static final String TIME_CREATED_COLUMN     = "time_created";
    private static final String REQUIRES_INTERNET_COLUMN = "requires_internet";
    private static final String MAX_RETRIES_COLUMN      = "max_retries";
    private static final String SHOULD_HOLD_COLUMN      = "should_hold";

    private static final String[] DEFAULT_COLUMNS = {TASK_TYPE_COLUMN, DEFAULT_ID_COLUMN, STATE_COLUMN, DATA_COLUMN, MAJOR_PRIORITY_COLUMN, MINOR_PRIORITY_COLUMN, RETRY_COUNT_COLUMN, TIME_CREATED_COLUMN, REQUIRES_INTERNET_COLUMN, MAX_RETRIES_COLUMN, SHOULD_HOLD_COLUMN};

    //endregion

    private WorkItemDatabaseHelper databaseHelper;

    @Before
    public void setUp() {
        databaseHelper = new WorkItemDatabaseHelper(Robolectric.application);
    }

    @After
    public void tearDown() {
        databaseHelper.getWritableDatabase().execSQL("DROP TABLE " + WORK_ITEM_TABLE_NAME);
        databaseHelper = null;
    }

    //region Test add/update/delete

    @Test
    public void testAddWorkItemSuccess() throws Exception {
        final String taskType = "taskTypeaa";
        final WorkItemState state = WorkItemState.EXECUTING;
        final String data = "someData";
        final int majorP = 4;
        final int minorP = 333;
        final int retry = 90;
        final int timeCreated = 98474;
        final boolean requiresInternet = true;
        final int maxRetry = 4234;
        final boolean shouldHold = true;

        InternalWorkItem workItem = new InternalWorkItem();
        workItem.setTaskType(taskType);
        workItem.setState(state);
        workItem.setData(data);
        workItem.setMajorPriority(majorP);
        workItem.setMinorPriority(minorP);
        workItem.setRetryCount(retry);
        workItem.setTimeCreated(timeCreated);
        workItem.setRequiresInternet(requiresInternet);
        workItem.setMaxRetries(maxRetry);
        workItem.setShouldHold(shouldHold);

        boolean success = databaseHelper.addNewWorkItem(workItem);
        assertThat("Successful", success);

        Cursor cursor = databaseHelper.getReadableDatabase().query(WORK_ITEM_TABLE_NAME, DEFAULT_COLUMNS, null, null, null, null, null);

        assertThat(cursor.getCount(), is(1));
        cursor.moveToFirst();

        assertThat(cursor.getString(cursor.getColumnIndex(TASK_TYPE_COLUMN)), is(taskType));
        assertThat(cursor.getString(cursor.getColumnIndex(DATA_COLUMN)), is(data));
        assertThat(cursor.getInt(cursor.getColumnIndex(STATE_COLUMN)), is(state.value()));
        assertThat(cursor.getInt(cursor.getColumnIndex(MAJOR_PRIORITY_COLUMN)), is(majorP));
        assertThat(cursor.getInt(cursor.getColumnIndex(MINOR_PRIORITY_COLUMN)), is(minorP));
        assertThat(cursor.getInt(cursor.getColumnIndex(RETRY_COUNT_COLUMN)), is(retry));
        assertThat(cursor.getInt(cursor.getColumnIndex(TIME_CREATED_COLUMN)), is(timeCreated));
        assertThat(cursor.getInt(cursor.getColumnIndex(REQUIRES_INTERNET_COLUMN)), is(1));
        assertThat(cursor.getInt(cursor.getColumnIndex(MAX_RETRIES_COLUMN)), is(maxRetry));
        assertThat(cursor.getInt(cursor.getColumnIndex(SHOULD_HOLD_COLUMN)), is(1));
    }

    @Test
    public void testAddWorkItemFailure() throws Exception {
        WorkItemDatabaseHelper mockHelper = mock(WorkItemDatabaseHelper.class);
        SQLiteDatabase mockDatabase = mock(SQLiteDatabase.class);

        when(mockHelper.addNewWorkItem(Matchers.any(InternalWorkItem.class))).thenCallRealMethod();

        when(mockHelper.getWritableDatabase()).thenReturn(mockDatabase);
        when(mockDatabase.insert(Matchers.anyString(), Matchers.anyString(), Matchers.any(ContentValues.class))).thenReturn((long) -1);

        InternalWorkItem workItem = new InternalWorkItem();
        boolean success = mockHelper.addNewWorkItem(workItem);
        assertThat("Failed", success == false);
    }

    @Test
    public void testUpdateWorkItemSuccess() throws Exception {
        InternalWorkItem initialWorkItem = new InternalWorkItem();
        initialWorkItem.setState(WorkItemState.HOLDING);
        databaseHelper.addNewWorkItem(initialWorkItem);

        String[] columns = {DEFAULT_ID_COLUMN};
        Cursor initialCursor = databaseHelper.getReadableDatabase().query(WORK_ITEM_TABLE_NAME, columns, null, null, null, null, null);
        initialCursor.moveToFirst();

        assertThat(initialCursor.getCount(), is(1));
        int workItemId = initialCursor.getInt(initialCursor.getColumnIndex(DEFAULT_ID_COLUMN));

        final String taskType = "taskTypeaa";
        final WorkItemState state = WorkItemState.HOLDING;
        final String data = "someData";
        final int majorP = 4;
        final int minorP = 333;
        final int retry = 90;
        final int timeCreated = 98474;
        final boolean requiresInternet = true;
        final int maxRetry = 4234;
        final boolean shouldHold = true;

        InternalWorkItem updatedWorkItem = new InternalWorkItem();
        updatedWorkItem.setId(workItemId);
        updatedWorkItem.setTaskType(taskType);
        updatedWorkItem.setState(state);
        updatedWorkItem.setData(data);
        updatedWorkItem.setMajorPriority(majorP);
        updatedWorkItem.setMinorPriority(minorP);
        updatedWorkItem.setRetryCount(retry);
        updatedWorkItem.setTimeCreated(timeCreated);
        updatedWorkItem.setRequiresInternet(requiresInternet);
        updatedWorkItem.setMaxRetries(maxRetry);
        updatedWorkItem.setShouldHold(shouldHold);

        boolean success = databaseHelper.updateWorkItem(updatedWorkItem);
        assertThat("succeeded", success);

        Cursor cursor = databaseHelper.getReadableDatabase().query(WORK_ITEM_TABLE_NAME, DEFAULT_COLUMNS, null, null, null, null, null);

        assertThat(cursor.getCount(), is(1));
        cursor.moveToFirst();

        assertThat(cursor.getString(cursor.getColumnIndex(TASK_TYPE_COLUMN)), is(taskType));
        assertThat(cursor.getString(cursor.getColumnIndex(DATA_COLUMN)), is(data));
        assertThat(cursor.getInt(cursor.getColumnIndex(STATE_COLUMN)), is(state.value()));
        assertThat(cursor.getInt(cursor.getColumnIndex(MAJOR_PRIORITY_COLUMN)), is(majorP));
        assertThat(cursor.getInt(cursor.getColumnIndex(MINOR_PRIORITY_COLUMN)), is(minorP));
        assertThat(cursor.getInt(cursor.getColumnIndex(RETRY_COUNT_COLUMN)), is(retry));
        assertThat(cursor.getInt(cursor.getColumnIndex(TIME_CREATED_COLUMN)), is(timeCreated));
        assertThat(cursor.getInt(cursor.getColumnIndex(REQUIRES_INTERNET_COLUMN)), is(1));
        assertThat(cursor.getInt(cursor.getColumnIndex(MAX_RETRIES_COLUMN)), is(maxRetry));
        assertThat(cursor.getInt(cursor.getColumnIndex(SHOULD_HOLD_COLUMN)), is(1));
    }

    @Test
    public void testUpdateWorkItemFailure() throws Exception {
        WorkItemDatabaseHelper mockHelper = mock(WorkItemDatabaseHelper.class);
        SQLiteDatabase mockDatabase = mock(SQLiteDatabase.class);

        when(mockHelper.updateWorkItem(Matchers.any(InternalWorkItem.class))).thenCallRealMethod();

        when(mockHelper.getWritableDatabase()).thenReturn(mockDatabase);
        when(mockDatabase.update(Matchers.anyString(), Matchers.any(ContentValues.class), Matchers.anyString(), Matchers.any(String[].class))).thenReturn(0);

        InternalWorkItem workItem = new InternalWorkItem();
        boolean success = mockHelper.updateWorkItem(workItem);
        assertThat("Failed", success == false);
    }

    @Test
    public void testDeleteWorkItemSuccess() throws Exception {
        InternalWorkItem initialWorkItem = new InternalWorkItem();
        initialWorkItem.setState(WorkItemState.EXECUTING);
        databaseHelper.addNewWorkItem(initialWorkItem);

        String[] columns = {DEFAULT_ID_COLUMN};
        Cursor initialCursor = databaseHelper.getReadableDatabase().query(WORK_ITEM_TABLE_NAME, columns, null, null, null, null, null);
        initialCursor.moveToFirst();

        assertThat(initialCursor.getCount(), is(1));
        int workItemId = initialCursor.getInt(initialCursor.getColumnIndex(DEFAULT_ID_COLUMN));

        InternalWorkItem workItemToDelete = new InternalWorkItem();
        workItemToDelete.setId(workItemId);

        boolean success = databaseHelper.deleteWorkItem(workItemToDelete);
        assertThat("Succeeded", success);

        Cursor cursor = databaseHelper.getReadableDatabase().query(WORK_ITEM_TABLE_NAME, DEFAULT_COLUMNS, null, null, null, null, null);
        assertThat(cursor.getCount(), is(0));
    }

    @Test
    public void testDeleteWorkItemFailure() throws Exception {
        WorkItemDatabaseHelper mockHelper = mock(WorkItemDatabaseHelper.class);
        SQLiteDatabase mockDatabase = mock(SQLiteDatabase.class);

        when(mockHelper.deleteWorkItem(Matchers.any(InternalWorkItem.class))).thenCallRealMethod();

        when(mockHelper.getWritableDatabase()).thenReturn(mockDatabase);
        when(mockDatabase.delete(Matchers.anyString(), Matchers.anyString(), Matchers.any(String[].class))).thenReturn(0);

        InternalWorkItem workItem = new InternalWorkItem();
        boolean success = mockHelper.deleteWorkItem(workItem);
        assertThat("Failed", success == false);
    }

    //endregion

    //region Test getNextWorkItem

    @Test
    public void testGetNextWorkItemNoneReady() throws Exception {
        String type = "testType";

        InternalWorkItem testItem1 = new InternalWorkItem();
        testItem1.setTaskType(type);
        testItem1.setState(WorkItemState.EXECUTING);

        InternalWorkItem testItem2 = new InternalWorkItem();
        testItem2.setTaskType(type);
        testItem2.setState(WorkItemState.HOLDING);

        databaseHelper.addNewWorkItem(testItem1);
        databaseHelper.addNewWorkItem(testItem2);

        HashSet<String> types = new HashSet<String>();
        types.add(type);
        InternalWorkItem nextItem = databaseHelper.getNextWorkItemForTaskTypes(types, false);

        assertThat(nextItem, nullValue());
    }

    @Test
    public void testGetNextWorkItemNoRecognizedTypes() throws Exception {
        String type = "testType";

        InternalWorkItem testItem1 = new InternalWorkItem();
        testItem1.setTaskType(type);
        testItem1.setState(WorkItemState.READY);

        InternalWorkItem testItem2 = new InternalWorkItem();
        testItem2.setTaskType(type);
        testItem2.setState(WorkItemState.READY);

        databaseHelper.addNewWorkItem(testItem1);
        databaseHelper.addNewWorkItem(testItem2);

        HashSet<String> types = new HashSet<String>();
        types.add("Different Type");
        InternalWorkItem nextItem = databaseHelper.getNextWorkItemForTaskTypes(types, false);

        assertThat(nextItem, nullValue());
    }

    @Test
    public void testGetNextWorkItemOnlyProvidedType() throws Exception {
        String typeA = "testA";
        String typeB = "typeB";
        String firstData = "firstData";
        String secondData = "secondData";

        InternalWorkItem testItem1 = new InternalWorkItem();
        testItem1.setTaskType(typeA);
        testItem1.setState(WorkItemState.READY);
        testItem1.setData(firstData);

        InternalWorkItem testItem2 = new InternalWorkItem();
        testItem2.setTaskType(typeB);
        testItem2.setState(WorkItemState.READY);
        testItem2.setData(secondData);

        databaseHelper.addNewWorkItem(testItem1);
        databaseHelper.addNewWorkItem(testItem2);

        HashSet<String> types = new HashSet<>();
        types.add(typeA);

        InternalWorkItem nextItem = databaseHelper.getNextWorkItemForTaskTypes(types, false);
        assertThat(nextItem, notNullValue());
        assertThat(nextItem.getData(), is(testItem1.getData()));

        databaseHelper.deleteWorkItem(nextItem);

        InternalWorkItem secondReturnedItem = databaseHelper.getNextWorkItemForTaskTypes(types, false);
        assertThat(secondReturnedItem, nullValue());
    }

    @Test
    public void testGetNextWorkItemSingleHighestPriority() throws Exception {
        String taskType = "taskTypea";
        String lowPriorityData = "lowPriorityDat";
        String highPriorityData = "highPriority";

        InternalWorkItem highPriorityItem = new InternalWorkItem();
        highPriorityItem.setTaskType(taskType);
        highPriorityItem.setData(highPriorityData);
        highPriorityItem.setState(WorkItemState.READY);
        highPriorityItem.setMajorPriority(5);

        databaseHelper.addNewWorkItem(highPriorityItem);

        for (int i=0; i<10; i++) {
            InternalWorkItem workItem = new InternalWorkItem();
            workItem.setTaskType(taskType);
            workItem.setData(lowPriorityData);
            workItem.setState(WorkItemState.READY);

            workItem.setMajorPriority(randInt(0,4));
            workItem.setMinorPriority(randInt(0,100));

            databaseHelper.addNewWorkItem(workItem);
        }

        HashSet<String> types = new HashSet<>();
        types.add(taskType);
        InternalWorkItem nextWorkItem = databaseHelper.getNextWorkItemForTaskTypes(types, false);
        assertThat(nextWorkItem, notNullValue());
        assertThat(nextWorkItem.getData(), is(highPriorityData));
    }

    @Test
    public void testGetNextWorkItemMultipleHighestPrioritySingleHighestMinorPriority() throws Exception {
        String taskType = "taskTypea";
        String lowPriorityData = "lowPriorityDat";
        String highPriorityData = "highPriority";

        InternalWorkItem highPriorityItem = new InternalWorkItem();
        highPriorityItem.setTaskType(taskType);
        highPriorityItem.setData(highPriorityData);
        highPriorityItem.setState(WorkItemState.READY);
        highPriorityItem.setMajorPriority(5);
        highPriorityItem.setMinorPriority(10);

        databaseHelper.addNewWorkItem(highPriorityItem);

        for (int i=0; i<10; i++) {
            InternalWorkItem workItem = new InternalWorkItem();
            workItem.setTaskType(taskType);
            workItem.setData(lowPriorityData);
            workItem.setState(WorkItemState.READY);

            workItem.setMajorPriority(randInt(4,5));
            workItem.setMinorPriority(randInt(0,9));

            databaseHelper.addNewWorkItem(workItem);
        }

        HashSet<String> types = new HashSet<>();
        types.add(taskType);
        InternalWorkItem nextWorkItem = databaseHelper.getNextWorkItemForTaskTypes(types, false);
        assertThat(nextWorkItem, notNullValue());
        assertThat(nextWorkItem.getData(), is(highPriorityData));
    }

    @Test
    public void testGetNextWorkItemMultipleHighestPriorityMultipleHighestMinorPrioritySingleLowestRetryCount() throws Exception {
        String taskType = "taskTypea";
        String lowPriorityData = "lowPriorityDat";
        String highPriorityData = "highPriority";

        InternalWorkItem highPriorityItem = new InternalWorkItem();
        highPriorityItem.setTaskType(taskType);
        highPriorityItem.setData(highPriorityData);
        highPriorityItem.setState(WorkItemState.READY);
        highPriorityItem.setMajorPriority(5);
        highPriorityItem.setMinorPriority(10);
        highPriorityItem.setRetryCount(1);

        databaseHelper.addNewWorkItem(highPriorityItem);

        for (int i=0; i<10; i++) {
            InternalWorkItem workItem = new InternalWorkItem();
            workItem.setTaskType(taskType);
            workItem.setData(lowPriorityData);
            workItem.setState(WorkItemState.READY);

            workItem.setMajorPriority(5);
            workItem.setMinorPriority(10);
            workItem.setRetryCount(randInt(2,5));

            databaseHelper.addNewWorkItem(workItem);
        }

        HashSet<String> types = new HashSet<>();
        types.add(taskType);
        InternalWorkItem nextWorkItem = databaseHelper.getNextWorkItemForTaskTypes(types, false);
        assertThat(nextWorkItem, notNullValue());
        assertThat(nextWorkItem.getData(), is(highPriorityData));
    }

    @Test
    public void testGetNextWorkItemMultipleHighestPriorityMultipleHighestMinorPrioritySingleMultipleLowestRetryCountSingleMostRecent() throws Exception {
        String taskType = "taskTypea";
        String lowPriorityData = "lowPriorityDat";
        String highPriorityData = "highPriority";

        InternalWorkItem highPriorityItem = new InternalWorkItem();
        highPriorityItem.setTaskType(taskType);
        highPriorityItem.setData(highPriorityData);
        highPriorityItem.setState(WorkItemState.READY);
        highPriorityItem.setMajorPriority(5);
        highPriorityItem.setMinorPriority(10);
        highPriorityItem.setRetryCount(1);
        highPriorityItem.setTimeCreated(200);

        databaseHelper.addNewWorkItem(highPriorityItem);

        for (int i=0; i<10; i++) {
            InternalWorkItem workItem = new InternalWorkItem();
            workItem.setTaskType(taskType);
            workItem.setData(lowPriorityData);
            workItem.setState(WorkItemState.READY);

            workItem.setMajorPriority(5);
            workItem.setMinorPriority(10);
            workItem.setRetryCount(1);
            workItem.setTimeCreated(randInt(0,100));

            databaseHelper.addNewWorkItem(workItem);
        }

        HashSet<String> types = new HashSet<>();
        types.add(taskType);
        InternalWorkItem nextWorkItem = databaseHelper.getNextWorkItemForTaskTypes(types, false);
        assertThat(nextWorkItem, notNullValue());
        assertThat(nextWorkItem.getData(), is(highPriorityData));
    }

    @Test
    public void testGetNextWorkItemNoInternetRequiresInternet() throws Exception {
        String taskType = "taskTypea";
        String lowPriorityData = "lowPriorityDat";
        String highPriorityData = "highPriority";

        InternalWorkItem highPriorityItem = new InternalWorkItem();
        highPriorityItem.setTaskType(taskType);
        highPriorityItem.setData(highPriorityData);
        highPriorityItem.setState(WorkItemState.READY);
        highPriorityItem.setMajorPriority(50);

        highPriorityItem.setRequiresInternet(true);

        databaseHelper.addNewWorkItem(highPriorityItem);

        InternalWorkItem workItem = new InternalWorkItem();
        workItem.setTaskType(taskType);
        workItem.setData(lowPriorityData);
        workItem.setState(WorkItemState.READY);

        workItem.setMajorPriority(5);
        workItem.setRequiresInternet(false);

        databaseHelper.addNewWorkItem(workItem);

        HashSet<String> types = new HashSet<>();
        types.add(taskType);
        InternalWorkItem nextWorkItem = databaseHelper.getNextWorkItemForTaskTypes(types, false);
        assertThat(nextWorkItem, notNullValue());
        assertThat(nextWorkItem.getData(), is(lowPriorityData));

        databaseHelper.deleteWorkItem(nextWorkItem);

        InternalWorkItem noInternetItem = databaseHelper.getNextWorkItemForTaskTypes(types, false);
        assertThat(noInternetItem, nullValue());

        InternalWorkItem internetItem = databaseHelper.getNextWorkItemForTaskTypes(types, true);
        assertThat(internetItem, notNullValue());
        assertThat(internetItem.getData(), is(highPriorityData));
    }

    //endregion

    //region Test Item Manipulation Methods

    @Test
    public void testDeleteWorkItemsWithTaskType() throws Exception {
        String deleteType = "deleteType";
        String keepType = "keepType";

        InternalWorkItem delete1 = new InternalWorkItem();
        delete1.setTaskType(deleteType);
        delete1.setState(WorkItemState.READY);
        InternalWorkItem delete2 = new InternalWorkItem();
        delete2.setTaskType(deleteType);
        delete2.setState(WorkItemState.READY);

        InternalWorkItem keep1 = new InternalWorkItem();
        keep1.setTaskType(keepType);
        keep1.setState(WorkItemState.READY);
        InternalWorkItem keep2 = new InternalWorkItem();
        keep2.setTaskType(keepType);
        keep2.setState(WorkItemState.READY);

        databaseHelper.addNewWorkItem(delete1);
        databaseHelper.addNewWorkItem(keep2);
        databaseHelper.addNewWorkItem(delete2);
        databaseHelper.addNewWorkItem(keep1);

        databaseHelper.deleteWorkItemsWithTaskType(deleteType);

        Cursor cursor = databaseHelper.getReadableDatabase().query(WORK_ITEM_TABLE_NAME, DEFAULT_COLUMNS, null, null, null, null, null);
        assertThat(cursor.getCount(), is(2));

        cursor.moveToFirst();
        String returnedType1 = cursor.getString(cursor.getColumnIndex(TASK_TYPE_COLUMN));

        cursor.moveToNext();
        String returnedType2 = cursor.getString(cursor.getColumnIndex(TASK_TYPE_COLUMN));

        assertThat(returnedType1, is(keepType));
        assertThat(returnedType2, is(keepType));
    }

    @Test
    public void testChangePriorityOfTasksWithType() throws Exception {

        String changeType = "changeType";
        String keepType = "keepType";
        int originalMajorPriotiy = 5;
        int newMajorPriority = 8;

        InternalWorkItem change1 = new InternalWorkItem();
        change1.setTaskType(changeType);
        change1.setState(WorkItemState.READY);
        change1.setMajorPriority(originalMajorPriotiy);

        InternalWorkItem change2 = new InternalWorkItem();
        change2.setTaskType(changeType);
        change2.setState(WorkItemState.READY);
        change2.setMajorPriority(originalMajorPriotiy);

        InternalWorkItem keep1 = new InternalWorkItem();
        keep1.setTaskType(keepType);
        keep1.setState(WorkItemState.READY);
        keep1.setMajorPriority(originalMajorPriotiy);

        InternalWorkItem keep2 = new InternalWorkItem();
        keep2.setTaskType(keepType);
        keep2.setState(WorkItemState.READY);
        keep2.setMajorPriority(originalMajorPriotiy);

        databaseHelper.addNewWorkItem(change1);
        databaseHelper.addNewWorkItem(keep2);
        databaseHelper.addNewWorkItem(change2);
        databaseHelper.addNewWorkItem(keep1);

        databaseHelper.changePriorityOfTaskType(changeType, newMajorPriority);

        Cursor aCursor = databaseHelper.getReadableDatabase().query(WORK_ITEM_TABLE_NAME, DEFAULT_COLUMNS, null, null, null, null, null);
        assertThat(aCursor.getCount(), is(4));

        aCursor.moveToFirst();
        while (!aCursor.isAfterLast()) {
            String returnedType1 = aCursor.getString(aCursor.getColumnIndex(TASK_TYPE_COLUMN));
            int majorP = aCursor.getInt(aCursor.getColumnIndex(MAJOR_PRIORITY_COLUMN));

            if (returnedType1.equals(changeType)) {
                assertThat(majorP, is(newMajorPriority));
            } else if (returnedType1.equals(keepType)) {
                assertThat(majorP, is(originalMajorPriotiy));
            } else {
                assertThat("Should be one of the two provided types", false);
            }
            aCursor.moveToNext();
        }
    }

    @Test
    public void testRestartHoldingTasks() throws Exception {
        String readyData = "ready";
        String executingData = "executing";
        String holdingData = "holding";

        InternalWorkItem readyItem = new InternalWorkItem();
        readyItem.setState(WorkItemState.READY);
        readyItem.setData(readyData);

        InternalWorkItem executingItem = new InternalWorkItem();
        executingItem.setState(WorkItemState.EXECUTING);
        executingItem.setData(executingData);

        InternalWorkItem hold1 = new InternalWorkItem();
        hold1.setState(WorkItemState.HOLDING);
        hold1.setData(holdingData);

        InternalWorkItem hold2 = new InternalWorkItem();
        hold2.setState(WorkItemState.HOLDING);
        hold2.setData(holdingData);

        databaseHelper.addNewWorkItem(readyItem);
        databaseHelper.addNewWorkItem(executingItem);
        databaseHelper.addNewWorkItem(hold1);
        databaseHelper.addNewWorkItem(hold2);

        databaseHelper.restartHoldingTasks();

        Cursor aCursor = databaseHelper.getReadableDatabase().query(WORK_ITEM_TABLE_NAME, DEFAULT_COLUMNS, null, null, null, null, null);
        assertThat(aCursor.getCount(), is(4));

        aCursor.moveToFirst();
        while (!aCursor.isAfterLast()) {
            String returnedData = aCursor.getString(aCursor.getColumnIndex(DATA_COLUMN));
            WorkItemState state = WorkItemState.valueOf(aCursor.getInt(aCursor.getColumnIndex(STATE_COLUMN)));

            if (returnedData.equals(readyData)) {
                assertThat(state, is(WorkItemState.READY));
            } else if (returnedData.equals(executingData)) {
                assertThat(state, is(WorkItemState.EXECUTING));
            } else if (returnedData.equals(holdingData)) {
                assertThat(state, is(WorkItemState.READY));
            } else {
                assertThat("Should be one of the three provided datas", false);
            }
            aCursor.moveToNext();
        }
    }

    @Test
    public void testRestartExecutingTasks() throws Exception {
        String readyData = "ready";
        String executingData = "executing";
        String holdingData = "holding";

        InternalWorkItem readyItem = new InternalWorkItem();
        readyItem.setState(WorkItemState.READY);
        readyItem.setData(readyData);

        InternalWorkItem executingItem = new InternalWorkItem();
        executingItem.setState(WorkItemState.EXECUTING);
        executingItem.setData(executingData);

        InternalWorkItem executingItem2 = new InternalWorkItem();
        executingItem2.setState(WorkItemState.EXECUTING);
        executingItem2.setData(executingData);

        InternalWorkItem hold1 = new InternalWorkItem();
        hold1.setState(WorkItemState.HOLDING);
        hold1.setData(holdingData);


        databaseHelper.addNewWorkItem(readyItem);
        databaseHelper.addNewWorkItem(executingItem);
        databaseHelper.addNewWorkItem(hold1);
        databaseHelper.addNewWorkItem(executingItem2);

        databaseHelper.restartExecutingTasks();

        Cursor aCursor = databaseHelper.getReadableDatabase().query(WORK_ITEM_TABLE_NAME, DEFAULT_COLUMNS, null, null, null, null, null);
        assertThat(aCursor.getCount(), is(4));

        aCursor.moveToFirst();
        while (!aCursor.isAfterLast()) {
            String returnedData = aCursor.getString(aCursor.getColumnIndex(DATA_COLUMN));
            WorkItemState state = WorkItemState.valueOf(aCursor.getInt(aCursor.getColumnIndex(STATE_COLUMN)));

            if (returnedData.equals(readyData)) {
                assertThat(state, is(WorkItemState.READY));
            } else if (returnedData.equals(executingData)) {
                assertThat(state, is(WorkItemState.READY));
            } else if (returnedData.equals(holdingData)) {
                assertThat(state, is(WorkItemState.HOLDING));
            } else {
                assertThat("Should be one of the three provided datas", false);
            }
            aCursor.moveToNext();
        }
    }

    @Test
    public void testCountOfWorkItemsWithType() throws Exception {
        String countType = "Count";
        String noCountType = "noCount";

        InternalWorkItem count1 = new InternalWorkItem();
        count1.setTaskType(countType);
        count1.setState(WorkItemState.HOLDING);

        InternalWorkItem count2 = new InternalWorkItem();
        count2.setTaskType(countType);
        count2.setState(WorkItemState.EXECUTING);

        InternalWorkItem noCount = new InternalWorkItem();
        noCount.setTaskType(noCountType);
        noCount.setState(WorkItemState.READY);

        databaseHelper.addNewWorkItem(count1);
        databaseHelper.addNewWorkItem(count2);
        databaseHelper.addNewWorkItem(noCount);

        int count = databaseHelper.countOfWorkItemsWithTaskType(countType);
        assertThat(count, is(2));
    }

    @Test
    public void testCountOfWorkItemsNotHolding() throws Exception {

        InternalWorkItem count1 = new InternalWorkItem();
        count1.setState(WorkItemState.HOLDING);

        InternalWorkItem count2 = new InternalWorkItem();
        count2.setState(WorkItemState.EXECUTING);

        InternalWorkItem noCount = new InternalWorkItem();
        noCount.setState(WorkItemState.READY);

        InternalWorkItem notHolding = new InternalWorkItem();
        notHolding.setState(WorkItemState.EXECUTING);

        databaseHelper.addNewWorkItem(count1);
        databaseHelper.addNewWorkItem(count2);
        databaseHelper.addNewWorkItem(noCount);
        databaseHelper.addNewWorkItem(notHolding);

        int count = databaseHelper.countOfWorkItemsNotHolding();
        assertThat(count, is(3));
    }

    @Test
    public void testResetDatabase() throws Exception {
        for (int i=0; i<20; i++) {
            InternalWorkItem workItem = new InternalWorkItem();

            if (i<10) {
                workItem.setState(WorkItemState.HOLDING);
            } else if (i<15) {
                workItem.setState(WorkItemState.EXECUTING);
            } else {
                workItem.setState(WorkItemState.READY);
            }
            databaseHelper.addNewWorkItem(workItem);
        }

        databaseHelper.resetDatabase();

        Cursor aCursor = databaseHelper.getReadableDatabase().query(WORK_ITEM_TABLE_NAME, DEFAULT_COLUMNS, null, null, null, null, null);
        assertThat(aCursor.getCount(), is(0));
    }
    //endregion

    //region Helpers

    public static int randInt(int min, int max) {

        // Usually this can be a field rather than a method variable
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    //endregion
}
