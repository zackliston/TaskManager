package com.zackliston.taskmanager;

/**
 * Created by Zack Liston on 3/2/15.
 */
public abstract class Manager
{
    protected abstract TaskWorker taskWorkerForWorkItem(InternalWorkItem workItem);

    protected void workItemDidFail(InternalWorkItem workItem) {}
}
