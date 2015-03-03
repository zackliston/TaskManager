package com.zackliston.taskmanager;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;


import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zack Liston on 3/2/15.
 */
public class TaskManager {
    //region constants
    private static final int MAX_NUMBER_CONCURRENT_OPERATIONS = 4;
    //endregion

    //region Variables
    protected WorkItemDatabaseHelper workItemDatabaseHelper;
    protected ExecutorService executorService;
    protected ExecutorService backgroundService;
    protected Handler mainHandler;
    protected ConnectivityManager connectivityManager;
    protected HashMap<String, Manager> registeredManagers;

    protected boolean isRunning;
    protected boolean isWaitingForStopCompletion;

    //endregion

    //region Initialization
    private static TaskManager ourInstance;
    public static TaskManager getInstance(Context context) {
        if (ourInstance == null) {
            synchronized (TaskManager.class) {
                ourInstance = new TaskManager(context);
            }
        }
        return ourInstance;
    }

    private TaskManager(Context context) {
        workItemDatabaseHelper = new WorkItemDatabaseHelper(context);
        executorService = Executors.newFixedThreadPool(MAX_NUMBER_CONCURRENT_OPERATIONS);
        backgroundService = Executors.newCachedThreadPool();
        mainHandler = new Handler(context.getMainLooper());
        connectivityManager = (ConnectivityManager)context.getSystemService(context.CONNECTIVITY_SERVICE);

        registeredManagers = new HashMap<>();

        isRunning = true;
        isWaitingForStopCompletion = false;
    }
    //endregion

    //region Getters/Setters
    protected void setIsWaitingForStopCompletion(boolean isWaitingForStopCompletion) {
        this.isWaitingForStopCompletion = isWaitingForStopCompletion;

        if (!isWaitingForStopCompletion && isRunning) {
            backgroundService.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (ourInstance) {
                        scheduleMoreWork();
                    }
                }
            });
        }
    }
    //endregion

    //region Starting/Stopping
    public synchronized void stopAsynchronously(Runnable networkCancellationBlock, final Runnable completionBlock) {
        isRunning = false;
        setIsWaitingForStopCompletion(true);

        executorService.shutdownNow();

        if (networkCancellationBlock != null) {
            networkCancellationBlock.run();
        }

        backgroundService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    executorService.awaitTermination(30, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    System.out.println("Error stopping TaskManager asynchronously " + exception.toString());
                } finally {
                    setIsWaitingForStopCompletion(false);
                    if (completionBlock != null) {
                        mainHandler.post(completionBlock);
                    }
                }
            }
        });
    }

    public synchronized void stopAndWait(Runnable networkCancellationBlock) {
        isRunning = false;
        setIsWaitingForStopCompletion(true);

        if (networkCancellationBlock != null) {
            networkCancellationBlock.run();
        }

        executorService.shutdownNow();
        try {
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            System.out.println("Error stopping TaskManager asynchronously " + exception.toString());
        } finally {
            setIsWaitingForStopCompletion(false);
        }
    }

    public synchronized void resume() {
        isRunning = true;
        backgroundService.execute(new Runnable() {
            @Override
            public void run() {
                scheduleMoreWork();
            }
        });
    }
    //endregion

    //region Queueing Tasks
    public boolean queueTask(Task task) {
        if (isWaitingForStopCompletion || task.getTaskType() == null || task.getTaskType().length() < 1) {
            return false;
        }

        boolean success;
        synchronized (this) {
            InternalWorkItem workItem = new InternalWorkItem();
            workItem.setTaskType(task.getTaskType());
            workItem.setMajorPriority(task.getMajorPriority());
            workItem.setMinorPriority(task.getMinorPriority());
            workItem.setJsonData(task.getJsonData());
            workItem.setState(WorkItemState.READY);
            workItem.setRetryCount(0);
            workItem.setRequiresInternet(task.isRequiresInternet());
            workItem.setTimeCreated((int) System.currentTimeMillis());
            workItem.setMaxRetries(task.getMaxRetries());
            workItem.setShouldHold(task.isShouldHoldAfterMaxRetries());

            success = workItemDatabaseHelper.addNewWorkItem(workItem);

            if (success) {
                scheduleMoreWork();
            }
        }

        return success;
    }

    public boolean queueTaskArray(ArrayList<Task> taskArray) {
        if (isWaitingForStopCompletion) {
            return false;
        }

        boolean success = true;
        synchronized (this) {
            for (Task task: taskArray) {
                if (task.getTaskType() == null || task.getTaskType().length() < 1) {
                    success = false;
                    continue;
                }

                InternalWorkItem workItem = new InternalWorkItem();
                workItem.setTaskType(task.getTaskType());
                workItem.setMajorPriority(task.getMajorPriority());
                workItem.setMinorPriority(task.getMinorPriority());
                workItem.setJsonData(task.getJsonData());
                workItem.setState(WorkItemState.READY);
                workItem.setRetryCount(0);
                workItem.setRequiresInternet(task.isRequiresInternet());
                workItem.setTimeCreated((int) System.currentTimeMillis());
                workItem.setMaxRetries(task.getMaxRetries());
                workItem.setShouldHold(task.isShouldHoldAfterMaxRetries());

                boolean individualSuccess = workItemDatabaseHelper.addNewWorkItem(workItem);
                if (!individualSuccess) {
                    success = individualSuccess;
                }
            }
            if (success) {
                scheduleMoreWork();
            }
        }

        return success;
    }
    //endregion

    //region Manipulating Tasks
    public synchronized void removeTasksOfType(String taskType) {
        workItemDatabaseHelper.deleteWorkItemsWithTaskType(taskType);
    }

    public synchronized void changePriorityOfTasksOfType(String taskType, int newMajorPriority) {
        workItemDatabaseHelper.changePriorityOfTaskType(taskType, newMajorPriority);
    }

    public synchronized int countOfTasksWithType(String taskType) {
        return workItemDatabaseHelper.countOfWorkItemsWithTaskType(taskType);
    }

    public synchronized int countOfTasksNotHolding() {
        return workItemDatabaseHelper.countOfWorkItemsNotHolding();
    }

    public synchronized void restartHoldingTasks() {
        workItemDatabaseHelper.restartHoldingTasks();
    }
    //endregion

    //region Manager Registration
    public synchronized void registerManagerForTaskType(Manager manager, String taskType) throws UnsupportedOperationException {
        Manager existingManager = registeredManagers.get(taskType);
        if (existingManager != null) {
            if (!existingManager.equals(manager)) {
                throw new UnsupportedOperationException("Only one manager for each TaskType can be registered");
            }
        }
        registeredManagers.put(taskType, manager);
    }

    public synchronized void removeRegisteredManagerForAllTaskTypes(Manager manager) {
         ArrayList<String> keysToRemove = new ArrayList<>(registeredManagers.size());

        for (String key: registeredManagers.keySet()) {
            Manager managerForKey = registeredManagers.get(key);
            if (managerForKey.equals(manager)) {
                keysToRemove.add(key);
            }
        }

        for (String keyToRemove: keysToRemove) {
            registeredManagers.remove(keyToRemove);
        }
    }
    //endregion

    protected void scheduleMoreWork() {

    }


    //region Test Helpers
    protected static void tearDownForTest() {
        ourInstance = null;
    }
    //endregion
}
