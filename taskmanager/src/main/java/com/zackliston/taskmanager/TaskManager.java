package com.zackliston.taskmanager;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;


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

    protected void scheduleMoreWork() {

    }


    //region Test Helpers
    protected static void tearDownForTest() {
        ourInstance = null;
    }
    //endregion
}
