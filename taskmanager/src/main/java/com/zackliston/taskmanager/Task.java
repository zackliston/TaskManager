package com.zackliston.taskmanager;

import org.json.JSONObject;

/**
 * Created by Zack Liston on 3/2/15.
 */
public class Task
{
    public static final int DEFAULT_MAX_RETRY_COUNT = 3;
    public static final int DEFAULT_MAJOR_PRIORITY = 10;
    public static final int DEFAULT_MINOR_PRIORITY = 10;

    //region Variables
    private String taskType;
    private JSONObject jsonData;
    private int majorPriority;
    private int minorPriority;
    private boolean requiresInternet;
    private int maxRetries;
    private boolean shouldHoldAfterMaxRetries;
    //endregion

    //region Initializer
    private Task() {
        majorPriority = DEFAULT_MAJOR_PRIORITY;
        minorPriority = DEFAULT_MINOR_PRIORITY;
        requiresInternet = false;
        maxRetries = DEFAULT_MAX_RETRY_COUNT;
        shouldHoldAfterMaxRetries = false;
    }

    public Task(String taskType, JSONObject jsonData) {
        this();
        this.taskType = taskType;
        this.jsonData = jsonData;
    }
    //endregion

    //region Getters/Setters
    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public JSONObject getJsonData() {
        return jsonData;
    }

    public void setJsonData(JSONObject jsonData) {
        this.jsonData = jsonData;
    }

    public int getMajorPriority() {
        return majorPriority;
    }

    public void setMajorPriority(int majorPriority) {
        this.majorPriority = majorPriority;
    }

    public int getMinorPriority() {
        return minorPriority;
    }

    public void setMinorPriority(int minorPriority) {
        this.minorPriority = minorPriority;
    }

    public boolean isRequiresInternet() {
        return requiresInternet;
    }

    public void setRequiresInternet(boolean requiresInternet) {
        this.requiresInternet = requiresInternet;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean isShouldHoldAfterMaxRetries() {
        return shouldHoldAfterMaxRetries;
    }

    public void setShouldHoldAfterMaxRetries(boolean shouldHoldAfterMaxRetries) {
        this.shouldHoldAfterMaxRetries = shouldHoldAfterMaxRetries;
    }
    //endregion
}
