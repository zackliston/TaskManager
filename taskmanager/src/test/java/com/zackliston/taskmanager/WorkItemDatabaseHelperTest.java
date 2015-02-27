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

import java.math.MathContext;
import java.sql.SQLInput;
import java.util.ArrayList;
import java.util.Random;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.mockito.Mockito.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

/**
 * Created by Zack Liston on 2/27/15.
 */

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
        databaseHelper.getReadableDatabase().execSQL("DROP TABLE " + WORK_ITEM_TABLE_NAME);
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

        ArrayList<String> types = new ArrayList<String>();
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

        ArrayList<String> types = new ArrayList<String>();
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

        ArrayList<String> types = new ArrayList<>();
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

        ArrayList<String> types = new ArrayList<>();
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

        ArrayList<String> types = new ArrayList<>();
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

        ArrayList<String> types = new ArrayList<>();
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

        ArrayList<String> types = new ArrayList<>();
        types.add(taskType);
        InternalWorkItem nextWorkItem = databaseHelper.getNextWorkItemForTaskTypes(types, false);
        assertThat(nextWorkItem, notNullValue());
        assertThat(nextWorkItem.getData(), is(highPriorityData));
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
