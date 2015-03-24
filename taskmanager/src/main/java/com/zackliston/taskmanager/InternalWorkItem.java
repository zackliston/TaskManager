package com.zackliston.taskmanager;


import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Zack Liston on 2/25/15.
 */

public class InternalWorkItem
{

    //region Properties
    private int id;
    private String taskType;
    private String data;
    private JSONObject jsonData;
    private WorkItemState state;
    private int majorPriority;
    private int minorPriority;
    private int retryCount;
    private int timeCreated;
    private boolean requiresInternet;
    private int maxRetries;
    private boolean shouldHold;
    //endregion

    //region Getters/Setters
    public int getId() {
        return id;
    }

    void setId(int id) {
        this.id = id;
    }

    public String getTaskType() {
        return taskType;
    }

    void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getData() {
        return data;
    }

    void setData(String data) {
        this.data = data;
        this.jsonData = null;
    }

    public JSONObject getJsonData() {
        if (jsonData == null && this.data != null) {
            try {
                jsonData = new JSONObject(this.data);
            } catch (JSONException exception) {
                System.out.println("Could not parse data into json object " + this.data + exception);
            }
        }
        return jsonData;
    }

    void setJsonData(JSONObject jsonData) {
        this.jsonData = jsonData;
        if (jsonData != null) {
            this.data = jsonData.toString();
        } else {
            this.data = null;
        }
    }

    public WorkItemState getState() {
        return state;
    }

    void setState(WorkItemState state) {
        this.state = state;
    }

    public int getMajorPriority() {
        return majorPriority;
    }

    void setMajorPriority(int majorPriority) {
        this.majorPriority = majorPriority;
    }

    public int getMinorPriority() {
        return minorPriority;
    }

    void setMinorPriority(int minorPriority) {
        this.minorPriority = minorPriority;
    }

    public int getRetryCount() {
        return retryCount;
    }

    void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getTimeCreated() {
        return timeCreated;
    }

    void setTimeCreated(int timeCreated) {
        this.timeCreated = timeCreated;
    }

    public boolean isRequiresInternet() {
        return requiresInternet;
    }

    void setRequiresInternet(boolean requiresInternet) {
        this.requiresInternet = requiresInternet;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean isShouldHold() {
        return shouldHold;
    }

    void setShouldHold(boolean shouldHold) {
        this.shouldHold = shouldHold;
    }
    //endregion

}
