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

    protected void setId(int id) {
        this.id = id;
    }

    public String getTaskType() {
        return taskType;
    }

    protected void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    protected String getData() {
        return data;
    }

    protected void setData(String data) {
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

    protected void setJsonData(JSONObject jsonData) {
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

    protected void setState(WorkItemState state) {
        this.state = state;
    }

    public int getMajorPriority() {
        return majorPriority;
    }

    protected void setMajorPriority(int majorPriority) {
        this.majorPriority = majorPriority;
    }

    public int getMinorPriority() {
        return minorPriority;
    }

    protected void setMinorPriority(int minorPriority) {
        this.minorPriority = minorPriority;
    }

    public int getRetryCount() {
        return retryCount;
    }

    protected void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getTimeCreated() {
        return timeCreated;
    }

    protected void setTimeCreated(int timeCreated) {
        this.timeCreated = timeCreated;
    }

    public boolean isRequiresInternet() {
        return requiresInternet;
    }

    protected void setRequiresInternet(boolean requiresInternet) {
        this.requiresInternet = requiresInternet;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    protected void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean isShouldHold() {
        return shouldHold;
    }

    protected void setShouldHold(boolean shouldHold) {
        this.shouldHold = shouldHold;
    }
    //endregion

}
