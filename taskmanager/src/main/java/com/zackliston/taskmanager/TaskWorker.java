package com.zackliston.taskmanager;

/**
 * Created by Zack Liston on 3/2/15.
 */
public abstract class TaskWorker implements Runnable
{
    private InternalWorkItem workItem;
    private TaskFinishedInterface taskFinishedDelegate;
    private boolean taskFailed;
    private boolean isFinalAttempt;
    private boolean hasCalledTaskFinished;

    public InternalWorkItem workItem() {
        return workItem;
    }

    public void setWorkItem(InternalWorkItem workItem) {
        this.workItem = workItem;
    }

    public TaskFinishedInterface taskFinishedDelegate() {
        return taskFinishedDelegate;
    }

    public void setTaskFinishedDelegate(TaskFinishedInterface taskFinishedDelegate) {
        this.taskFinishedDelegate = taskFinishedDelegate;
    }

    public boolean isTaskFailed() {
        return taskFailed;
    }

    public void setTaskFailed(boolean taskFailed) {
        this.taskFailed = taskFailed;
    }

    public boolean isFinalAttempt() {
        return isFinalAttempt;
    }

    public void setFinalAttempt(boolean isFinalAttempt) {
        this.isFinalAttempt = isFinalAttempt;
    }

    public void setupWithWorkItem(InternalWorkItem workItem) {
        this.workItem = workItem;
        this.isFinalAttempt = (workItem.getRetryCount() >= (workItem.getMaxRetries()-1));
    }

    public void taskFinishedWasSuccessful(boolean wasSuccessful) {
        if (!hasCalledTaskFinished) {
            hasCalledTaskFinished = true;
            taskFinishedDelegate.taskWorkerFinishedSuccessfully(this, wasSuccessful);
        }
    }
}
