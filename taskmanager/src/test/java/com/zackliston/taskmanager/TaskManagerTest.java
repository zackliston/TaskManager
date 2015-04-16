package com.zackliston.taskmanager;

import android.content.Context;
import android.net.ConnectivityManager;

import junit.framework.TestCase;

import org.apache.tools.ant.taskdefs.Exec;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.model.RunnerBuilder;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.net.NetworkInfo;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.*;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Config(manifest=Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class TaskManagerTest {

    private TaskManager taskManager;

    @Before
    public void setUp() throws Exception {
        Context mockContext = mock(Context.class);
        ConnectivityManager mockConnectivityManager = mock(ConnectivityManager.class);

        when(mockContext.getSystemService(mockContext.CONNECTIVITY_SERVICE)).thenReturn(mockConnectivityManager);
        taskManager = new TaskManager(Robolectric.application);
    }

    @After
    public void tearDown() throws Exception {
        TaskManager.tearDownForTest();
    }

    //region Test initialization
    @Test
    public void testGetInstance() throws Exception {
        assertThat(taskManager.workItemDatabaseHelper, notNullValue());
        assertThat(taskManager.executorService, notNullValue());
        assertThat(taskManager.backgroundService, notNullValue());
        assertThat(taskManager.mainHandler, notNullValue());
        assertThat(taskManager.connectivityManager, notNullValue());

        assertThat("Is running", taskManager.isRunning);
        assertThat("Is not waiting", taskManager.isWaitingForStopCompletion == false);
        assertThat(taskManager.registeredManagers, notNullValue());
    }
    //endregion

    //region Test Getters/Setters
    @Test
    public void testSetIsWaitingForStopCompletionIsNotWaitingIsRunning() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);

        mockTaskManager.isRunning = true;

        ArgumentCaptor runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        ExecutorService mockService = mock(ExecutorService.class);
        mockTaskManager.backgroundService = mockService;

        mockTaskManager.setIsWaitingForStopCompletion(false);
        assertThat("Is not waiting", mockTaskManager.isWaitingForStopCompletion == false);

        verify(mockService).execute((Runnable) runnableCaptor.capture());
        Runnable runnable = (Runnable) runnableCaptor.getValue();
        runnable.run();

        verify(mockTaskManager).scheduleMoreWork();
    }

    @Test
    public void testSetIsWaitingForStopCompletionIsWaiting() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        mockTaskManager.isRunning = true;

        ArgumentCaptor runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        ExecutorService mockService = mock(ExecutorService.class);
        mockTaskManager.backgroundService = mockService;

        mockTaskManager.setIsWaitingForStopCompletion(true);
        assertThat("Is Waiting", mockTaskManager.isWaitingForStopCompletion);

        verify(mockService, never()).execute((Runnable) runnableCaptor.capture());
    }

    @Test
    public void testSetIsWaitingForStopCompletionIsNoWaitingIsNotRunning() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        mockTaskManager.isRunning = false;

        ArgumentCaptor runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        ExecutorService mockService = mock(ExecutorService.class);
        mockTaskManager.backgroundService = mockService;

        mockTaskManager.setIsWaitingForStopCompletion(false);
        assertThat("Is not waiting", mockTaskManager.isWaitingForStopCompletion == false);

        verify(mockService, never()).execute((Runnable) runnableCaptor.capture());
    }

    //endregion

    //region Test Start/Stop
    @Test
    public void testStopAsync() throws Exception {
        taskManager.isRunning = true;
        taskManager.isWaitingForStopCompletion = false;

        Runnable mockNetworkBlock = mock(Runnable.class);
        Runnable mockCompletionBlock = mock(Runnable.class);

        Handler mockMainHanlder = mock(Handler.class);
        taskManager.mainHandler = mockMainHanlder;

        ExecutorService mockExecutorService = mock(ExecutorService.class);
        taskManager.executorService = mockExecutorService;

        ExecutorService mockBackground = mock(ExecutorService.class);
        taskManager.backgroundService = mockBackground;
        ArgumentCaptor runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        taskManager.stopAsynchronously(mockNetworkBlock, mockCompletionBlock);
        assertThat("Is not running", taskManager.isRunning == false);
        assertThat("Is waiting", taskManager.isWaitingForStopCompletion);

        verify(mockNetworkBlock).run();
        verify(mockExecutorService).shutdownNow();
        verify(mockBackground).execute((Runnable) runnableCaptor.capture());
        verify(mockMainHanlder, never()).post(mockCompletionBlock);
        assertThat("Is waiting", taskManager.isWaitingForStopCompletion);

        Runnable waitRunnable = (Runnable) runnableCaptor.getValue();
        waitRunnable.run();

        assertThat("Is not waiting", taskManager.isWaitingForStopCompletion == false);
        verify(mockExecutorService).awaitTermination(30, TimeUnit.SECONDS);

        verify(mockMainHanlder).post(mockCompletionBlock);
    }

    @Test
    public void testStopAndWait() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);

        mockTaskManager.isRunning = true;
        mockTaskManager.isWaitingForStopCompletion = false;

        Runnable mockNetworkBlock = mock(Runnable.class);
        ExecutorService mockExecutorService = mock(ExecutorService.class);
        mockTaskManager.executorService = mockExecutorService;

        mockTaskManager.stopAndWait(mockNetworkBlock);

        verify(mockNetworkBlock).run();
        verify(mockTaskManager).setIsWaitingForStopCompletion(true);
        verify(mockExecutorService).shutdownNow();
        verify(mockExecutorService).awaitTermination(30, TimeUnit.SECONDS);
        verify(mockTaskManager).setIsWaitingForStopCompletion(false);
    }

    @Test
    public void testResume() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);

        mockTaskManager.isRunning = false;

        ExecutorService mockBackground = mock(ExecutorService.class);
        mockTaskManager.backgroundService = mockBackground;
        ArgumentCaptor runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        mockTaskManager.resume();

        assertThat("Is running", mockTaskManager.isRunning);
        verify(mockBackground).execute((Runnable) runnableCaptor.capture());
        Runnable waitRunnable = (Runnable) runnableCaptor.getValue();

        verify(mockTaskManager, never()).scheduleMoreWork();

        waitRunnable.run();
        verify(mockTaskManager).scheduleMoreWork();
    }
    //endregion

    //region Test QueueTasks
    @Test
    public void testQueueTaskSuccess() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        when(mockDb.addNewWorkItem(Matchers.any(InternalWorkItem.class))).thenReturn(true);

        ArgumentCaptor workItemCaptor = ArgumentCaptor.forClass(InternalWorkItem.class);

        String taskType = "taskT";
        int major = 32;
        int minor = 2;
        JSONObject json = new JSONObject("{key:value}");
        boolean requiresInternet = true;
        int maxRetryCount = 3242234;
        boolean shouldHold = true;

        Task task = new Task(taskType, json);
        task.setMajorPriority(major);
        task.setMinorPriority(minor);
        task.setRequiresInternet(requiresInternet);
        task.setMaxRetries(maxRetryCount);
        task.setShouldHoldAfterMaxRetries(shouldHold);

        boolean success = mockTaskManager.queueTask(task);

        assertThat(success, is(true));

        verify(mockDb).addNewWorkItem((InternalWorkItem) workItemCaptor.capture());
        verify(mockTaskManager).scheduleMoreWork();

        InternalWorkItem workItem = (InternalWorkItem) workItemCaptor.getValue();
        assertThat(workItem.getTaskType(), is(taskType));
        assertThat(workItem.getMajorPriority(), is(major));
        assertThat(workItem.getMinorPriority(), is(minor));
        assertThat(workItem.getJsonData(), is(json));
        assertThat(workItem.getState(), is(WorkItemState.READY));
        assertThat(workItem.getRetryCount(), is(0));
        assertThat(workItem.isRequiresInternet(), is(requiresInternet));
        assertThat(workItem.getMaxRetries(), is(maxRetryCount));
        assertThat(workItem.isShouldHold(), is(shouldHold));

        long milliseconds = System.currentTimeMillis();
        int timeDifference = (int) milliseconds - workItem.getTimeCreated();

        assertThat("Time is almost the same", timeDifference < 100);
    }

    @Test
    public void testQueueTaskFailure() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        when(mockDb.addNewWorkItem(Matchers.any(InternalWorkItem.class))).thenReturn(false);

        ArgumentCaptor workItemCaptor = ArgumentCaptor.forClass(InternalWorkItem.class);

        String taskType = "taskT";
        int major = 32;
        int minor = 2;
        JSONObject json = new JSONObject("{key:value}");
        boolean requiresInternet = true;
        int maxRetryCount = 3242234;
        boolean shouldHold = true;

        Task task = new Task(taskType, json);
        task.setMajorPriority(major);
        task.setMinorPriority(minor);
        task.setRequiresInternet(requiresInternet);
        task.setMaxRetries(maxRetryCount);
        task.setShouldHoldAfterMaxRetries(shouldHold);

        boolean success = mockTaskManager.queueTask(task);

        assertThat(success, is(false));

        verify(mockDb).addNewWorkItem((InternalWorkItem) workItemCaptor.capture());
        verify(mockTaskManager, never()).scheduleMoreWork();

        InternalWorkItem workItem = (InternalWorkItem) workItemCaptor.getValue();
        assertThat(workItem.getTaskType(), is(taskType));
        assertThat(workItem.getMajorPriority(), is(major));
        assertThat(workItem.getMinorPriority(), is(minor));
        assertThat(workItem.getJsonData(), is(json));
        assertThat(workItem.getState(), is(WorkItemState.READY));
        assertThat(workItem.getRetryCount(), is(0));
        assertThat(workItem.isRequiresInternet(), is(requiresInternet));
        assertThat(workItem.getMaxRetries(), is(maxRetryCount));
        assertThat(workItem.isShouldHold(), is(shouldHold));

        long milliseconds = System.currentTimeMillis();
        int timeDifference = (int) milliseconds - workItem.getTimeCreated();

        assertThat("Time is almost the same", timeDifference < 100);
    }

    @Test
    public void testQueueTaskIsWaiting() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        mockTaskManager.isWaitingForStopCompletion = true;
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        String taskType = "taskT";
        Task task = new Task(taskType, null);

        boolean success = mockTaskManager.queueTask(task);

        assertThat(success, is(false));

        verify(mockDb, never()).addNewWorkItem(Matchers.any(InternalWorkItem.class));
        verify(mockTaskManager, never()).scheduleMoreWork();
    }

    @Test
    public void testQueueTaskNoTaskType() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        mockTaskManager.isWaitingForStopCompletion = false;
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        String taskType = null;
        Task task = new Task(taskType, null);

        boolean success = mockTaskManager.queueTask(task);

        assertThat(success, is(false));

        verify(mockDb, never()).addNewWorkItem(Matchers.any(InternalWorkItem.class));
        verify(mockTaskManager, never()).scheduleMoreWork();
    }

    @Test
    public void testQueueTaskEmptyTaskType() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        mockTaskManager.isWaitingForStopCompletion = false;
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        String taskType = "";
        Task task = new Task(taskType, null);

        boolean success = mockTaskManager.queueTask(task);

        assertThat(success, is(false));

        verify(mockDb, never()).addNewWorkItem(Matchers.any(InternalWorkItem.class));
        verify(mockTaskManager, never()).scheduleMoreWork();
    }

    @Test
    public void testQueueTaskArraySuccess() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        when(mockDb.addNewWorkItem(Matchers.any(InternalWorkItem.class))).thenReturn(true);

        ArgumentCaptor workItemCaptor1 = ArgumentCaptor.forClass(InternalWorkItem.class);

        String taskType1 = "taskT";
        int major1 = 32;
        int minor1 = 2;
        JSONObject json1 = new JSONObject("{key:value}");
        boolean requiresInternet1 = true;
        int maxRetryCount1 = 3242234;
        boolean shouldHold1 = true;

        Task task1 = new Task(taskType1, json1);
        task1.setMajorPriority(major1);
        task1.setMinorPriority(minor1);
        task1.setRequiresInternet(requiresInternet1);
        task1.setMaxRetries(maxRetryCount1);
        task1.setShouldHoldAfterMaxRetries(shouldHold1);

        String taskType2 = "taaaskT";
        int major2 = 332;
        int minor2 = 5632;
        JSONObject json2 = new JSONObject("{key:other}");
        boolean requiresInternet2 = false;
        int maxRetryCount2 = 3234;
        boolean shouldHold2 = false;

        Task task2 = new Task(taskType2, json2);
        task2.setMajorPriority(major2);
        task2.setMinorPriority(minor2);
        task2.setRequiresInternet(requiresInternet2);
        task2.setMaxRetries(maxRetryCount2);
        task2.setShouldHoldAfterMaxRetries(shouldHold2);

        ArrayList<Task> taskArray = new ArrayList<>();
        taskArray.add(task1);
        taskArray.add(task2);

        boolean success = mockTaskManager.queueTaskArray(taskArray);

        assertThat(success, is(true));

        verify(mockDb, times(2)).addNewWorkItem((InternalWorkItem) workItemCaptor1.capture());
        verify(mockTaskManager).scheduleMoreWork();

        List<InternalWorkItem> workItems = workItemCaptor1.getAllValues();

        InternalWorkItem workItem1 = workItems.get(0);
        assertThat(workItem1.getTaskType(), is(taskType1));
        assertThat(workItem1.getMajorPriority(), is(major1));
        assertThat(workItem1.getMinorPriority(), is(minor1));
        assertThat(workItem1.getJsonData(), is(json1));
        assertThat(workItem1.getState(), is(WorkItemState.READY));
        assertThat(workItem1.getRetryCount(), is(0));
        assertThat(workItem1.isRequiresInternet(), is(requiresInternet1));
        assertThat(workItem1.getMaxRetries(), is(maxRetryCount1));
        assertThat(workItem1.isShouldHold(), is(shouldHold1));

        long milliseconds = System.currentTimeMillis();
        int timeDifference = (int) milliseconds - workItem1.getTimeCreated();
        assertThat("Time is almost the same", timeDifference < 100);

        InternalWorkItem workItem2 = workItems.get(1);
        assertThat(workItem2.getTaskType(), is(taskType2));
        assertThat(workItem2.getMajorPriority(), is(major2));
        assertThat(workItem2.getMinorPriority(), is(minor2));
        assertThat(workItem2.getJsonData(), is(json2));
        assertThat(workItem2.getState(), is(WorkItemState.READY));
        assertThat(workItem2.getRetryCount(), is(0));
        assertThat(workItem2.isRequiresInternet(), is(requiresInternet2));
        assertThat(workItem2.getMaxRetries(), is(maxRetryCount2));
        assertThat(workItem2.isShouldHold(), is(shouldHold2));

        milliseconds = System.currentTimeMillis();
        timeDifference = (int) milliseconds - workItem2.getTimeCreated();
        assertThat("Time is almost the same", timeDifference < 100);
    }

    @Test
    public void testQueueTaskArrayFailure() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        when(mockDb.addNewWorkItem(Matchers.any(InternalWorkItem.class))).thenReturn(false);

        ArgumentCaptor workItemCaptor1 = ArgumentCaptor.forClass(InternalWorkItem.class);

        String taskType1 = "taskT";
        int major1 = 32;
        int minor1 = 2;
        JSONObject json1 = new JSONObject("{key:value}");
        boolean requiresInternet1 = true;
        int maxRetryCount1 = 3242234;
        boolean shouldHold1 = true;

        Task task1 = new Task(taskType1, json1);
        task1.setMajorPriority(major1);
        task1.setMinorPriority(minor1);
        task1.setRequiresInternet(requiresInternet1);
        task1.setMaxRetries(maxRetryCount1);
        task1.setShouldHoldAfterMaxRetries(shouldHold1);

        String taskType2 = "taaaskT";
        int major2 = 332;
        int minor2 = 5632;
        JSONObject json2 = new JSONObject("{key:other}");
        boolean requiresInternet2 = false;
        int maxRetryCount2 = 3234;
        boolean shouldHold2 = false;

        Task task2 = new Task(taskType2, json2);
        task2.setMajorPriority(major2);
        task2.setMinorPriority(minor2);
        task2.setRequiresInternet(requiresInternet2);
        task2.setMaxRetries(maxRetryCount2);
        task2.setShouldHoldAfterMaxRetries(shouldHold2);

        ArrayList<Task> taskArray = new ArrayList<>();
        taskArray.add(task1);
        taskArray.add(task2);

        boolean success = mockTaskManager.queueTaskArray(taskArray);

        assertThat(success, is(false));

        verify(mockDb, times(2)).addNewWorkItem((InternalWorkItem) workItemCaptor1.capture());
        verify(mockTaskManager, never()).scheduleMoreWork();

        List<InternalWorkItem> workItems = workItemCaptor1.getAllValues();

        InternalWorkItem workItem1 = workItems.get(0);
        assertThat(workItem1.getTaskType(), is(taskType1));
        assertThat(workItem1.getMajorPriority(), is(major1));
        assertThat(workItem1.getMinorPriority(), is(minor1));
        assertThat(workItem1.getJsonData(), is(json1));
        assertThat(workItem1.getState(), is(WorkItemState.READY));
        assertThat(workItem1.getRetryCount(), is(0));
        assertThat(workItem1.isRequiresInternet(), is(requiresInternet1));
        assertThat(workItem1.getMaxRetries(), is(maxRetryCount1));
        assertThat(workItem1.isShouldHold(), is(shouldHold1));

        long milliseconds = System.currentTimeMillis();
        int timeDifference = (int) milliseconds - workItem1.getTimeCreated();
        assertThat("Time is almost the same", timeDifference < 100);

        InternalWorkItem workItem2 = workItems.get(1);
        assertThat(workItem2.getTaskType(), is(taskType2));
        assertThat(workItem2.getMajorPriority(), is(major2));
        assertThat(workItem2.getMinorPriority(), is(minor2));
        assertThat(workItem2.getJsonData(), is(json2));
        assertThat(workItem2.getState(), is(WorkItemState.READY));
        assertThat(workItem2.getRetryCount(), is(0));
        assertThat(workItem2.isRequiresInternet(), is(requiresInternet2));
        assertThat(workItem2.getMaxRetries(), is(maxRetryCount2));
        assertThat(workItem2.isShouldHold(), is(shouldHold2));

        milliseconds = System.currentTimeMillis();
        timeDifference = (int) milliseconds - workItem2.getTimeCreated();
        assertThat("Time is almost the same", timeDifference < 100);
    }

    @Test
    public void testQueueTaskArrayIsWaiting() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        mockTaskManager.isWaitingForStopCompletion = true;
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        String taskType = "taskT";
        Task task = new Task(taskType, null);
        Task task2 = new Task(taskType, null);

        ArrayList<Task> taskArray = new ArrayList<>(2);
        taskArray.add(task);
        taskArray.add(task2);

        boolean success = mockTaskManager.queueTaskArray(taskArray);

        assertThat(success, is(false));

        verify(mockDb, never()).addNewWorkItem(Matchers.any(InternalWorkItem.class));
        verify(mockTaskManager, never()).scheduleMoreWork();
    }

    @Test
    public void testQueueTaskArrayNoTaskType() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        mockTaskManager.isWaitingForStopCompletion = false;
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        String taskType = null;
        Task task = new Task(taskType, null);
        Task task2 = new Task(taskType, null);

        ArrayList<Task> taskArray = new ArrayList<>(2);
        taskArray.add(task);
        taskArray.add(task2);

        boolean success = mockTaskManager.queueTaskArray(taskArray);

        assertThat(success, is(false));

        verify(mockDb, never()).addNewWorkItem(Matchers.any(InternalWorkItem.class));
        verify(mockTaskManager, never()).scheduleMoreWork();
    }

    @Test
    public void testQueueTaskArrayEmptyTaskType() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        mockTaskManager.isWaitingForStopCompletion = false;
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        String taskType = "";
        Task task = new Task(taskType, null);
        Task task2 = new Task(taskType, null);

        ArrayList<Task> taskArray = new ArrayList<>(2);
        taskArray.add(task);
        taskArray.add(task2);

        boolean success = mockTaskManager.queueTaskArray(taskArray);

        assertThat(success, is(false));

        verify(mockDb, never()).addNewWorkItem(Matchers.any(InternalWorkItem.class));
        verify(mockTaskManager, never()).scheduleMoreWork();
    }

    //endregion

    //region Test Manipulating Tasks
    @Test
    public void testRemoveTasksOfType() throws Exception {
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        taskManager.workItemDatabaseHelper = mockDb;

        String taskType = "thisisatype";
        taskManager.removeTasksOfType(taskType);

        verify(mockDb).deleteWorkItemsWithTaskType(taskType);
    }

    @Test
    public void testChangePriorityOfTasksOfType() throws Exception {
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        taskManager.workItemDatabaseHelper = mockDb;

        String taskType = "thisisatype";
        int newMajorPriority = 9932;

        taskManager.changePriorityOfTasksOfType(taskType, newMajorPriority);

        verify(mockDb).changePriorityOfTaskType(taskType, newMajorPriority);
    }

    @Test
    public void testCountOfTasksOfType() throws Exception {
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        taskManager.workItemDatabaseHelper = mockDb;

        int count = 83;
        String taskType = "thisisatype";

        when(mockDb.countOfWorkItemsWithTaskType(taskType)).thenReturn(count);

        int returnedValue = taskManager.countOfTasksWithType(taskType);

        assertThat(returnedValue, is(count));
        verify(mockDb).countOfWorkItemsWithTaskType(taskType);
    }

    @Test
    public void testCountOfTasksNotHolding() throws Exception {
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        taskManager.workItemDatabaseHelper=mockDb;

        int count = 83;
        when(mockDb.countOfWorkItemsNotHolding()).thenReturn(count);

        int returnedCount = taskManager.countOfTasksNotHolding();

        assertThat(returnedCount, is(count));
        verify(mockDb).countOfWorkItemsNotHolding();
    }

    @Test
    public void testRestartHoldingTasks() throws Exception {
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        taskManager.workItemDatabaseHelper=mockDb;

        taskManager.restartHoldingTasks();

        verify(mockDb).restartHoldingTasks();
    }
    //endregion

    //region Test Manager Registration
    @Test
    public void testRegisterManagerSuccess() throws Exception {
        String taskType = "taskTypeValue";
        Manager manager = new Manager() {
            @Override
            public TaskWorker taskWorkerForWorkItem(InternalWorkItem workItem) {
                return null;
            }
        };

        assertThat(taskManager.registeredManagers.size(), is(0));

        taskManager.registerManagerForTaskType(manager, taskType);

        assertThat(taskManager.registeredManagers.size(), is(1));

        Manager managerForTaskType = taskManager.registeredManagers.get(taskType);
        assertThat(managerForTaskType, is(manager));

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRegisterManagerExistingManagerForType() throws Exception {
        String taskType = "taskTypeValue";
        Manager existingManager = new Manager() {
            @Override
            public TaskWorker taskWorkerForWorkItem(InternalWorkItem workItem) {
                return null;
            }
        };
        taskManager.registeredManagers.put(taskType, existingManager);

        Manager newManager = new Manager() {
            @Override
            public TaskWorker taskWorkerForWorkItem(InternalWorkItem workItem) {
                return null;
            }
        };

        taskManager.registerManagerForTaskType(newManager, taskType);

        assertThat(taskManager.registeredManagers.size(), is(1));

        Manager managerForTaskType = taskManager.registeredManagers.get(taskType);
        assertThat(managerForTaskType, is(existingManager));
    }

    @Test
    public void testRegisterManagerSameManagerAlreadyRegistered() throws Exception {
        String taskType = "taskTypeValue";
        Manager existingManager = new Manager() {
            @Override
            public TaskWorker taskWorkerForWorkItem(InternalWorkItem workItem) {
                return null;
            }
        };
        taskManager.registeredManagers.put(taskType, existingManager);

        taskManager.registerManagerForTaskType(existingManager, taskType);

        assertThat(taskManager.registeredManagers.size(), is(1));

        Manager managerForTaskType = taskManager.registeredManagers.get(taskType);
        assertThat(managerForTaskType, is(existingManager));
    }

    @Test
    public void testRemoveRegisteredManagerForAllTaskTypes() throws Exception {
        String typeToRemove1 = "removeType";
        String typeToRemove2 = "removeType2";
        String typeToKeep = "keeptype";

        Manager removeManager = new Manager() {
            @Override
            public TaskWorker taskWorkerForWorkItem(InternalWorkItem workItem) {
                return null;
            }
        };
        Manager keepManager = new Manager() {
            @Override
            public TaskWorker taskWorkerForWorkItem(InternalWorkItem workItem) {
                return null;
            }
        };

        taskManager.registeredManagers.put(typeToRemove1, removeManager);
        taskManager.registeredManagers.put(typeToKeep, keepManager);
        taskManager.registeredManagers.put(typeToRemove2, removeManager);

        taskManager.removeRegisteredManagerForAllTaskTypes(removeManager);

        assertThat(taskManager.registeredManagers.size(), is(1));
        Manager remainingManager = taskManager.registeredManagers.get(typeToKeep);
        assertThat(remainingManager, is(keepManager));
    }
    //endregion

    //region Test Schedule More Work
    @Test
    public void testScheduleMoreWorkAlreadyAtCapacity() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        mockTaskManager.countOfCurrentlyRunningTasks = TaskManager.MAX_NUMBER_CONCURRENT_OPERATIONS;
        int expectedNumberOfCalls = TaskManager.MAX_NUMBER_CONCURRENT_OPERATIONS - mockTaskManager.countOfCurrentlyRunningTasks;

        doReturn(true).when(mockTaskManager).createAndQueueNextTaskWorker();

        mockTaskManager.scheduleMoreWork();

        verify(mockTaskManager, times(expectedNumberOfCalls)).createAndQueueNextTaskWorker();
        assertThat(mockTaskManager.countOfCurrentlyRunningTasks, is(TaskManager.MAX_NUMBER_CONCURRENT_OPERATIONS));
        assertThat(mockTaskManager.workTimer, notNullValue());
    }

    @Test
    public void testScheduleMoreWork() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        mockTaskManager.countOfCurrentlyRunningTasks = 1;
        int expectedNumberOfCalls = TaskManager.MAX_NUMBER_CONCURRENT_OPERATIONS - mockTaskManager.countOfCurrentlyRunningTasks;

        doReturn(true).when(mockTaskManager).createAndQueueNextTaskWorker();

        mockTaskManager.scheduleMoreWork();

        verify(mockTaskManager, times(expectedNumberOfCalls)).createAndQueueNextTaskWorker();
        assertThat(mockTaskManager.countOfCurrentlyRunningTasks, is(TaskManager.MAX_NUMBER_CONCURRENT_OPERATIONS));
        assertThat(mockTaskManager.workTimer, notNullValue());
    }

    @Test
    public void testScheduleMoreWorkCreateFailed() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        int initialRunningTaskCount = 1;
        mockTaskManager.countOfCurrentlyRunningTasks = initialRunningTaskCount;

        doReturn(false).when(mockTaskManager).createAndQueueNextTaskWorker();

        mockTaskManager.scheduleMoreWork();

        verify(mockTaskManager, times(1)).createAndQueueNextTaskWorker();
        assertThat(mockTaskManager.countOfCurrentlyRunningTasks, is(initialRunningTaskCount));
        assertThat(mockTaskManager.workTimer, notNullValue());
    }
    //endregion

    //region Test Create and Queue More Work
    @Test
    public void testCreateAndQueueNextTaskWorkerNoInternet() throws Exception {
        String taskType = "taskT";
        Manager mockManager = mock(Manager.class);
        taskManager.registeredManagers.put(taskType, mockManager);

        Set<String> taskTypes = taskManager.registeredManagers.keySet();

        InternalWorkItem workItem = new InternalWorkItem();
        workItem.setState(WorkItemState.READY);
        workItem.setTaskType(taskType);

        TaskWorker worker = new TaskWorker() {
            @Override
            public void run() {

            }
        };

        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        taskManager.workItemDatabaseHelper = mockDb;

        ConnectivityManager mockConnetivityManager = mock(ConnectivityManager.class);
        taskManager.connectivityManager = mockConnetivityManager;

        ExecutorService mockExecutorService = mock(ExecutorService.class);
        taskManager.executorService = mockExecutorService;

        NetworkInfo mockNetworkInfo = mock(NetworkInfo.class);
        when(mockNetworkInfo.isConnected()).thenReturn(false);
        when(mockConnetivityManager.getActiveNetworkInfo()).thenReturn(mockNetworkInfo);
        when(mockDb.getNextWorkItemForTaskTypes(taskTypes, false)).thenReturn(workItem);
        when(mockManager.taskWorkerForWorkItem(workItem)).thenReturn(worker);

        boolean success = taskManager.createAndQueueNextTaskWorker();
        assertThat(success, is(true));

        ArgumentCaptor workItemCaptor = ArgumentCaptor.forClass(InternalWorkItem.class);
        verify(mockDb).updateWorkItem((InternalWorkItem) workItemCaptor.capture());

        InternalWorkItem updatedWorkItem = (InternalWorkItem)workItemCaptor.getValue();
        assertThat(updatedWorkItem.getState(), is(WorkItemState.EXECUTING));

        ArgumentCaptor taskWorkerCaptor = ArgumentCaptor.forClass(TaskWorker.class);
        verify(mockExecutorService).execute((TaskWorker)taskWorkerCaptor.capture());

        TaskWorker executedTaskWorker = (TaskWorker)taskWorkerCaptor.getValue();
        assertThat(executedTaskWorker.taskFinishedDelegate(), notNullValue());

        verify(mockManager).taskWorkerForWorkItem(Matchers.any(InternalWorkItem.class));
        verify(mockDb).getNextWorkItemForTaskTypes(taskTypes, false);
        verify(mockNetworkInfo).isConnected();
        verify(mockConnetivityManager).getActiveNetworkInfo();
    }

    @Test
    public void testCreateAndQueueNextTaskWorkerHasInternet() throws Exception {
        String taskType = "taskT";
        Manager mockManager = mock(Manager.class);
        taskManager.registeredManagers.put(taskType, mockManager);

        Set<String> taskTypes = taskManager.registeredManagers.keySet();

        InternalWorkItem workItem = new InternalWorkItem();
        workItem.setState(WorkItemState.READY);
        workItem.setTaskType(taskType);

        TaskWorker worker = new TaskWorker() {
            @Override
            public void run() {

            }
        };

        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        taskManager.workItemDatabaseHelper = mockDb;

        ConnectivityManager mockConnetivityManager = mock(ConnectivityManager.class);
        taskManager.connectivityManager = mockConnetivityManager;

        ExecutorService mockExecutorService = mock(ExecutorService.class);
        taskManager.executorService = mockExecutorService;

        NetworkInfo mockNetworkInfo = mock(NetworkInfo.class);
        when(mockNetworkInfo.isConnected()).thenReturn(true);
        when(mockConnetivityManager.getActiveNetworkInfo()).thenReturn(mockNetworkInfo);
        when(mockDb.getNextWorkItemForTaskTypes(taskTypes, true)).thenReturn(workItem);
        when(mockManager.taskWorkerForWorkItem(workItem)).thenReturn(worker);

        boolean success = taskManager.createAndQueueNextTaskWorker();
        assertThat(success, is(true));

        ArgumentCaptor workItemCaptor = ArgumentCaptor.forClass(InternalWorkItem.class);
        verify(mockDb).updateWorkItem((InternalWorkItem)workItemCaptor.capture());

        InternalWorkItem updatedWorkItem = (InternalWorkItem)workItemCaptor.getValue();
        assertThat(updatedWorkItem.getState(), is(WorkItemState.EXECUTING));

        ArgumentCaptor taskWorkerCaptor = ArgumentCaptor.forClass(TaskWorker.class);
        verify(mockExecutorService).execute((TaskWorker)taskWorkerCaptor.capture());

        TaskWorker executedTaskWorker = (TaskWorker)taskWorkerCaptor.getValue();
        assertThat(executedTaskWorker.taskFinishedDelegate(), notNullValue());

        verify(mockManager).taskWorkerForWorkItem(Matchers.any(InternalWorkItem.class));
        verify(mockDb).getNextWorkItemForTaskTypes(taskTypes, true);
        verify(mockNetworkInfo).isConnected();
        verify(mockConnetivityManager).getActiveNetworkInfo();
    }

    @Test
    public void testCreateAndQueueNextTaskWorkerNoWorkItem() throws Exception {
        String taskType = "taskT";
        Manager mockManager = mock(Manager.class);
        taskManager.registeredManagers.put(taskType, mockManager);

        Set<String> taskTypes = taskManager.registeredManagers.keySet();

        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        taskManager.workItemDatabaseHelper = mockDb;

        ConnectivityManager mockConnetivityManager = mock(ConnectivityManager.class);
        taskManager.connectivityManager = mockConnetivityManager;

        ExecutorService mockExecutorService = mock(ExecutorService.class);
        taskManager.executorService = mockExecutorService;

        NetworkInfo mockNetworkInfo = mock(NetworkInfo.class);
        when(mockNetworkInfo.isConnected()).thenReturn(false);
        when(mockConnetivityManager.getActiveNetworkInfo()).thenReturn(mockNetworkInfo);
        when(mockDb.getNextWorkItemForTaskTypes(taskTypes, false)).thenReturn(null);

        boolean success = taskManager.createAndQueueNextTaskWorker();
        assertThat(success, is(false));

        verify(mockDb, never()).updateWorkItem(Matchers.any(InternalWorkItem.class));
        verify(mockExecutorService, never()).execute(Matchers.any(TaskWorker.class));

        verify(mockManager, never()).taskWorkerForWorkItem(Matchers.any(InternalWorkItem.class));
        verify(mockDb).getNextWorkItemForTaskTypes(taskTypes, false);
        verify(mockNetworkInfo).isConnected();
        verify(mockConnetivityManager).getActiveNetworkInfo();
    }

    //endregion

    //region Test TaskFinished Interface
    @Test
    public void testTaskFinishedSuccess() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        int initialTasksRunning = 3;
        mockTaskManager.countOfCurrentlyRunningTasks = initialTasksRunning;

        boolean success = true;
        InternalWorkItem workItem = new InternalWorkItem();
        TaskWorker worker = new TaskWorker() {
            @Override
            public void run() {

            }
        };
        worker.setWorkItem(workItem);

        mockTaskManager.taskWorkerFinishedSuccessfully(worker, success);

        verify(mockDb).deleteWorkItem(workItem);
        verify(mockTaskManager).scheduleMoreWork();

        assertThat(mockTaskManager.countOfCurrentlyRunningTasks, is(initialTasksRunning-1));
    }

    @Test
    public void testTaskFinishedFailureRetryLessThanMax() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        int initialTasksRunning = 43;
        mockTaskManager.countOfCurrentlyRunningTasks = initialTasksRunning;


        boolean success = false;
        int maxRetryCount = 30;
        int currentRetryCount = 20;

        InternalWorkItem workItem = new InternalWorkItem();
        workItem.setMaxRetries(maxRetryCount);
        workItem.setRetryCount(currentRetryCount);
        workItem.setState(WorkItemState.EXECUTING);
        TaskWorker worker = new TaskWorker() {
            @Override
            public void run() {

            }
        };
        worker.setWorkItem(workItem);

        mockTaskManager.taskWorkerFinishedSuccessfully(worker, success);

        ArgumentCaptor captor = ArgumentCaptor.forClass(InternalWorkItem.class);

        verify(mockDb).updateWorkItem((InternalWorkItem) captor.capture());

        InternalWorkItem newWorkItem = (InternalWorkItem)captor.getValue();
        assertThat(newWorkItem.getRetryCount(), is(currentRetryCount+1));
        assertThat(newWorkItem.getState(), is(WorkItemState.READY));

        verify(mockTaskManager).scheduleMoreWork();

        assertThat(mockTaskManager.countOfCurrentlyRunningTasks, is(initialTasksRunning-1));
    }

    @Test
    public void testTaskFinishedFailureRetryMaxShouldNotHold() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        int initialTasksRunning = 33;
        mockTaskManager.countOfCurrentlyRunningTasks = initialTasksRunning;


        String taskType = "taskt";
        Manager mockManager = mock(Manager.class);
        taskManager.registeredManagers.put(taskType, mockManager);

        boolean success = false;
        int maxRetryCount = 30;
        int currentRetryCount = 29;

        InternalWorkItem workItem = new InternalWorkItem();
        workItem.setMaxRetries(maxRetryCount);
        workItem.setTaskType(taskType);
        workItem.setRetryCount(currentRetryCount);
        workItem.setState(WorkItemState.EXECUTING);
        workItem.setShouldHold(false);
        TaskWorker worker = new TaskWorker() {
            @Override
            public void run() {

            }
        };
        worker.setWorkItem(workItem);

        mockTaskManager.taskWorkerFinishedSuccessfully(worker, success);


        verify(mockManager).workItemDidFail(workItem);
        verify(mockDb).deleteWorkItem(workItem);
        verify(mockTaskManager).scheduleMoreWork();

        assertThat(mockTaskManager.countOfCurrentlyRunningTasks, is(initialTasksRunning-1));
    }

    @Test
    public void testTaskFinishedFailureRetryMaxShouldHold() throws Exception {
        TaskManager mockTaskManager = spy(taskManager);
        WorkItemDatabaseHelper mockDb = mock(WorkItemDatabaseHelper.class);
        mockTaskManager.workItemDatabaseHelper = mockDb;

        int initialTasksRunning = 63;
        mockTaskManager.countOfCurrentlyRunningTasks = initialTasksRunning;

        String taskType = "taskt";
        Manager mockManager = mock(Manager.class);
        taskManager.registeredManagers.put(taskType, mockManager);

        boolean success = false;
        int maxRetryCount = 30;
        int currentRetryCount = 29;

        InternalWorkItem workItem = new InternalWorkItem();
        workItem.setMaxRetries(maxRetryCount);
        workItem.setTaskType(taskType);
        workItem.setRetryCount(currentRetryCount);
        workItem.setState(WorkItemState.EXECUTING);
        workItem.setShouldHold(true);
        TaskWorker worker = new TaskWorker() {
            @Override
            public void run() {

            }
        };
        worker.setWorkItem(workItem);

        mockTaskManager.taskWorkerFinishedSuccessfully(worker, success);

        ArgumentCaptor captor = ArgumentCaptor.forClass(InternalWorkItem.class);

        verify(mockDb).updateWorkItem((InternalWorkItem) captor.capture());

        InternalWorkItem newWorkItem = (InternalWorkItem)captor.getValue();
        assertThat(newWorkItem.getRetryCount(), is(currentRetryCount+1));
        assertThat(newWorkItem.getState(), is(WorkItemState.HOLDING));
        verify(mockManager).workItemDidFail(workItem);
        verify(mockTaskManager).scheduleMoreWork();

        assertThat(mockTaskManager.countOfCurrentlyRunningTasks, is(initialTasksRunning-1));
    }

    //endregion
}