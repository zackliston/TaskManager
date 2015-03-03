package com.zackliston.taskmanager;

/**
 * Created by Zack Liston on 3/2/15.
 */
public abstract class Manager
{
    public abstract TaskWorker taskWorkerForWorkItem(InternalWorkItem workItem);

    public void workItemDidFail(InternalWorkItem workItem) {}
}
