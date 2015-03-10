package com.zackliston.taskmanager;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;


import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zack Liston on 3/2/15.
 */
public class TaskManager implements TaskFinishedInterface {
    //region constants
    static final int MAX_NUMBER_CONCURRENT_OPERATIONS = 4;
    //endregion

    //region Variables
    WorkItemDatabaseHelper workItemDatabaseHelper;
    ExecutorService executorService;
    ExecutorService backgroundService;
    Handler mainHandler;
    BroadcastReceiver broadcastReceiver;
    Timer workTimer;

    ConnectivityManager connectivityManager;
    HashMap<String, Manager> registeredManagers;

    boolean isRunning;
    boolean isWaitingForStopCompletion;
    int countOfCurrentlyRunningTasks;
    //endregion

    //region Initialization
    private static TaskManager ourInstance;
    public static TaskManager getInstance(Context context) {
        if (ourInstance == null) {
            synchronized (TaskManager.class) {
                if (ourInstance == null) {
                    ourInstance = new TaskManager(context);
                }
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
        countOfCurrentlyRunningTasks = 0;

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                synchronized (ourInstance) {
                    scheduleMoreWork();
                }
            }
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(broadcastReceiver, filter);
    }
    //endregion

    //region Getters/Setters
    void setIsWaitingForStopCompletion(boolean isWaitingForStopCompletion) {
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
                        executorService = Executors.newFixedThreadPool(MAX_NUMBER_CONCURRENT_OPERATIONS);
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
            Executors.newFixedThreadPool(MAX_NUMBER_CONCURRENT_OPERATIONS);
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

    //region Scheduling Work
    void scheduleMoreWork() {
        if (workTimer != null) {
            workTimer.cancel();
        }

        boolean stop = countOfCurrentlyRunningTasks >= MAX_NUMBER_CONCURRENT_OPERATIONS;
        while (!stop) {
            boolean success = createAndQueueNextTaskWorker();
            if (success) {
                countOfCurrentlyRunningTasks++;
            } else {
                stop = true;
            }

            if (countOfCurrentlyRunningTasks >= MAX_NUMBER_CONCURRENT_OPERATIONS) {
                stop = true;
            }
        }

        workTimer = new Timer();
        workTimer.schedule(new ScheduleWorkFromTimer(), 5000);
    }

    boolean createAndQueueNextTaskWorker() {
        boolean isConnected = false;
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                isConnected = networkInfo.isConnected();
            }
        } else {
            System.out.println("There is no Connectivity manager in TaskManager. Only executing tasks that do not require Internet.");
        }

        Set taskTypes = registeredManagers.keySet();
        InternalWorkItem workItem = workItemDatabaseHelper.getNextWorkItemForTaskTypes(taskTypes, isConnected);

        if (workItem == null) {
            return false;
        }

        Manager managerForTaskType = registeredManagers.get(workItem.getTaskType());
        if (managerForTaskType == null) {
            System.out.println("There is no registered manager for task type " + workItem.getTaskType() + " cannot execute.");
            return false;
        }

        workItem.setState(WorkItemState.EXECUTING);
        workItemDatabaseHelper.updateWorkItem(workItem);

        TaskWorker worker = managerForTaskType.taskWorkerForWorkItem(workItem);
        worker.setTaskFinishedDelegate(this);

        executorService.execute(worker);

        return true;
    }
    //endregion

    //region TaskFinished Interface
    public void taskWorkerFinishedSuccessfully(TaskWorker taskWorker, boolean success) {
        synchronized (this) {
            countOfCurrentlyRunningTasks--;
            if (success) {
                workItemDatabaseHelper.deleteWorkItem(taskWorker.workItem());
            } else {
                InternalWorkItem workItem = taskWorker.workItem();
                int oldRetryCount = workItem.getRetryCount();
                workItem.setRetryCount(oldRetryCount+1);

                if (workItem.getRetryCount() >= workItem.getMaxRetries()) {
                    Manager managerForTask = registeredManagers.get(workItem.getTaskType());
                    if (managerForTask != null) {
                        managerForTask.workItemDidFail(workItem);
                    }

                    if (workItem.isShouldHold()) {
                        workItem.setState(WorkItemState.HOLDING);
                        workItemDatabaseHelper.updateWorkItem(workItem);
                    } else {
                        workItemDatabaseHelper.deleteWorkItem(workItem);
                    }

                } else {
                    workItem.setState(WorkItemState.READY);
                    workItemDatabaseHelper.updateWorkItem(workItem);
                }
            }
            scheduleMoreWork();
        }
    }
    //endregion

    //region Timer Helpers
    class ScheduleWorkFromTimer extends TimerTask {
        public void run() {
            synchronized (ourInstance) {
                scheduleMoreWork();
            }
        }
    }
    //endregion

    //region Test Helpers
    static void tearDownForTest() {
        ourInstance = null;
    }
    //endregion
}
