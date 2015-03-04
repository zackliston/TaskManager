package com.zackliston.taskmanager;

/**
 * Created by Zack Liston on 3/2/15.
 */
public abstract class TaskWorker implements Runnable
{
    //region Variables
    private InternalWorkItem workItem;
    private TaskFinishedInterface taskFinishedDelegate;
    private boolean taskFailed;
    private boolean isFinalAttempt;
    private boolean hasCalledTaskFinished;
    //endregion

    //region Getters/Setters
    protected InternalWorkItem workItem() {
        return workItem;
    }

    protected void setWorkItem(InternalWorkItem workItem) {
        this.workItem = workItem;
    }

    protected TaskFinishedInterface taskFinishedDelegate() {
        return taskFinishedDelegate;
    }

    protected void setTaskFinishedDelegate(TaskFinishedInterface taskFinishedDelegate) {
        this.taskFinishedDelegate = taskFinishedDelegate;
    }

    protected boolean isTaskFailed() {
        return taskFailed;
    }

    protected void setTaskFailed(boolean taskFailed) {
        this.taskFailed = taskFailed;
    }

    protected boolean isFinalAttempt() {
        return isFinalAttempt;
    }

    protected void setFinalAttempt(boolean isFinalAttempt) {
        this.isFinalAttempt = isFinalAttempt;
    }

    protected boolean isCancelled() {
        return Thread.currentThread().isInterrupted();
    }

    //endregion

    public void setupWithWorkItem(InternalWorkItem workItem) {
        this.workItem = workItem;
        this.isFinalAttempt = (workItem.getRetryCount() >= (workItem.getMaxRetries()-1));
    }

    protected void taskFinishedWasSuccessful(boolean wasSuccessful) {
        if (!hasCalledTaskFinished) {
            hasCalledTaskFinished = true;
            taskFinishedDelegate.taskWorkerFinishedSuccessfully(this, wasSuccessful);
        }
    }
}
