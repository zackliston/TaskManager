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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.os.Handler;

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
        taskManager = TaskManager.getInstance(mockContext);
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

        assertThat("Is running",taskManager.isRunning);
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
        Runnable runnable = (Runnable)runnableCaptor.getValue();
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
        verify(mockBackground).execute((Runnable)runnableCaptor.capture());
        verify(mockMainHanlder, never()).post(mockCompletionBlock);
        assertThat("Is waiting", taskManager.isWaitingForStopCompletion);

        Runnable waitRunnable = (Runnable)runnableCaptor.getValue();
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
        Runnable waitRunnable = (Runnable)runnableCaptor.getValue();

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

        verify(mockDb).addNewWorkItem((InternalWorkItem)workItemCaptor.capture());
        verify(mockTaskManager).scheduleMoreWork();

        InternalWorkItem workItem = (InternalWorkItem)workItemCaptor.getValue();
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
        int timeDifference = (int)milliseconds - workItem.getTimeCreated();

        assertThat("Time is almost the same", timeDifference<100);
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

        verify(mockDb).addNewWorkItem((InternalWorkItem)workItemCaptor.capture());
        verify(mockTaskManager, never()).scheduleMoreWork();

        InternalWorkItem workItem = (InternalWorkItem)workItemCaptor.getValue();
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
        int timeDifference = (int)milliseconds - workItem.getTimeCreated();

        assertThat("Time is almost the same", timeDifference<100);
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

    //endregion
}