package com.zackliston.taskmanager;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import com.zackliston.taskmanager.InternalWorkItem;

import java.util.Arrays;
import java.util.ArrayList;

/**
 * Created by Zack Liston on 2/25/15.
 */
public class WorkItemDatabaseHelper extends SQLiteOpenHelper
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

    private static final String[] DEFAULT_COLUMNS = {DEFAULT_ID_COLUMN, TASK_TYPE_COLUMN, STATE_COLUMN, DATA_COLUMN, MAJOR_PRIORITY_COLUMN, MINOR_PRIORITY_COLUMN, RETRY_COUNT_COLUMN, TIME_CREATED_COLUMN, REQUIRES_INTERNET_COLUMN, MAX_RETRIES_COLUMN, SHOULD_HOLD_COLUMN};

    //endregion

    //region Initialize
    protected WorkItemDatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    //endregion

    //region Setup
    @Override
    public void onCreate(SQLiteDatabase db)
    {
        // Create statement
        String CREATE_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS " + WORK_ITEM_TABLE_NAME + " ( " +
                DEFAULT_ID_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TASK_TYPE_COLUMN + " TEXT, " +
                STATE_COLUMN + " INTEGER, " +
                DATA_COLUMN + " TEXT, " +
                MAJOR_PRIORITY_COLUMN + " INTEGER, " +
                MINOR_PRIORITY_COLUMN + " INTEGER, " +
                RETRY_COUNT_COLUMN + " INTEGER, " +
                TIME_CREATED_COLUMN + " INTEGER, " +
                REQUIRES_INTERNET_COLUMN + " INTEGER, " +
                MAX_RETRIES_COLUMN + " INTEGER, " +
                SHOULD_HOLD_COLUMN + " INTEGER )";

        db.execSQL(CREATE_TABLE_STATEMENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        this.onCreate(db);
    }
    //endregion

    //region Protected Methods
    protected InternalWorkItem getNextWorkItemForTaskTypes(ArrayList<String> taskTypes, boolean hasInternet) {
        ArrayList<String> args = new ArrayList<>();
        args.add(""+WorkItemState.READY.value());
        args.addAll(taskTypes);

        String stringWithCorrectNumberOfQuestionMarks = queryStringForTaskTypeArray(taskTypes);
        String getNextQueryString = "" + STATE_COLUMN + " = ? AND " + TASK_TYPE_COLUMN + " IN " + stringWithCorrectNumberOfQuestionMarks;

        if (!hasInternet) {
            getNextQueryString = getNextQueryString + " AND " + REQUIRES_INTERNET_COLUMN + " = ? ";
            args.add("0");
        }

        String orderByString = "" + MAJOR_PRIORITY_COLUMN + " DESC, " + MINOR_PRIORITY_COLUMN + " DESC, " + RETRY_COUNT_COLUMN + " ASC, " + TIME_CREATED_COLUMN + " DESC";

        String[] stringArgs = new String[args.size()];
        args.toArray(stringArgs);

        Cursor resultCursor = getReadableDatabase().query(WORK_ITEM_TABLE_NAME, DEFAULT_COLUMNS, getNextQueryString, stringArgs, null, null, orderByString, "1");

        return workItemFromCursor(resultCursor);
    }

    protected boolean addNewWorkItem(InternalWorkItem workItem) {
        int requiresInternet = (workItem.isRequiresInternet()) ? 1 : 0;
        int shouldHold = (workItem.isShouldHold()) ? 1 : 0;

        ContentValues values = new ContentValues();
        values.put(TASK_TYPE_COLUMN, workItem.getTaskType());
        values.put(DATA_COLUMN, workItem.getData());
        values.put(MAJOR_PRIORITY_COLUMN, workItem.getMajorPriority());
        values.put(MINOR_PRIORITY_COLUMN, workItem.getMinorPriority());
        values.put(RETRY_COUNT_COLUMN, workItem.getRetryCount());
        values.put(TIME_CREATED_COLUMN, workItem.getTimeCreated());
        values.put(REQUIRES_INTERNET_COLUMN, requiresInternet);
        values.put(MAX_RETRIES_COLUMN, workItem.getMaxRetries());
        values.put(SHOULD_HOLD_COLUMN, shouldHold);

        if (workItem.getState() != null) {
            values.put(STATE_COLUMN, workItem.getState().value());
        } else {
            System.out.println("You must have a state for the work item before adding it.");
            return false;
        }

        long returnValue = getWritableDatabase().insert(WORK_ITEM_TABLE_NAME, null, values);
        return  (returnValue == -1) ? false : true;
    }

    protected boolean updateWorkItem(InternalWorkItem workItem) {
        int requiresInternet = (workItem.isRequiresInternet()) ? 1 : 0;
        int shouldHold = (workItem.isShouldHold()) ? 1 : 0;

        ContentValues values = new ContentValues();
        values.put(TASK_TYPE_COLUMN, workItem.getTaskType());
        values.put(DATA_COLUMN, workItem.getData());
        values.put(MAJOR_PRIORITY_COLUMN, workItem.getMajorPriority());
        values.put(MINOR_PRIORITY_COLUMN, workItem.getMinorPriority());
        values.put(RETRY_COUNT_COLUMN, workItem.getRetryCount());
        values.put(TIME_CREATED_COLUMN, workItem.getTimeCreated());
        values.put(REQUIRES_INTERNET_COLUMN, requiresInternet);
        values.put(MAX_RETRIES_COLUMN, workItem.getMaxRetries());
        values.put(SHOULD_HOLD_COLUMN, shouldHold);

        if (workItem.getState() != null) {
            values.put(STATE_COLUMN, workItem.getState().value());
        } else {
            System.out.println("You must have a state for the work item before updating it.");
            return false;
        }

        String[] args = {workItem.getId()+""};
        int numberOfRowsAffected = getWritableDatabase().update(WORK_ITEM_TABLE_NAME, values, DEFAULT_ID_COLUMN + " == ?", args);
        return (numberOfRowsAffected == 1) ? true : false;
    }

    protected  boolean deleteWorkItem(InternalWorkItem workItem) {
        String[] args = {workItem.getId()+""};
        int numberOfRowsAffected = getWritableDatabase().delete(WORK_ITEM_TABLE_NAME, DEFAULT_ID_COLUMN + " == ?", args);

        return (numberOfRowsAffected == 1) ? true : false;
    }

    //endregion

    //region Helpers

    private String queryStringForTaskTypeArray(ArrayList taskTypes) {
        String query = "(";
        for (int i=1; i<taskTypes.size(); i++) {
            query = query + "?, ";
        }
        query = query + "?)";

        return query;
    }

    private InternalWorkItem workItemFromCursor(Cursor cursor) {
        if (cursor.getCount() == 0) {
            return null;
        } else {
            cursor.moveToFirst();
        }

        boolean requiresInternet = (cursor.getInt(cursor.getColumnIndex(REQUIRES_INTERNET_COLUMN)) == 1) ? true : false;
        boolean shouldHold = (cursor.getInt(cursor.getColumnIndex(SHOULD_HOLD_COLUMN)) == 1) ? true : false;

        InternalWorkItem workItem = new InternalWorkItem();
        workItem.setId(cursor.getInt(cursor.getColumnIndex(DEFAULT_ID_COLUMN)));
        workItem.setTaskType(cursor.getString(cursor.getColumnIndex(TASK_TYPE_COLUMN)));
        workItem.setState(WorkItemState.valueOf(cursor.getInt(cursor.getColumnIndex(STATE_COLUMN))));
        workItem.setData(cursor.getString(cursor.getColumnIndex(DATA_COLUMN)));
        workItem.setMajorPriority(cursor.getInt(cursor.getColumnIndex(MAJOR_PRIORITY_COLUMN)));
        workItem.setMinorPriority(cursor.getInt(cursor.getColumnIndex(MINOR_PRIORITY_COLUMN)));
        workItem.setRetryCount(cursor.getInt(cursor.getColumnIndex(RETRY_COUNT_COLUMN)));
        workItem.setTimeCreated(cursor.getInt(cursor.getColumnIndex(TIME_CREATED_COLUMN)));

        workItem.setRequiresInternet(requiresInternet);
        workItem.setShouldHold(shouldHold);

        return workItem;
    }

    //endregion
}

