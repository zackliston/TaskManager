package com.zackliston.taskmanager;

import android.content.Context;
import android.net.ConnectivityManager;

import junit.framework.TestCase;

import org.apache.tools.ant.taskdefs.Exec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.model.RunnerBuilder;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.os.Handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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

        verify(mockService).execute((Runnable)runnableCaptor.capture());
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

        verify(mockService, never()).execute((Runnable)runnableCaptor.capture());
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

        verify(mockService, never()).execute((Runnable)runnableCaptor.capture());
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
        verify(mockBackground).execute((Runnable)runnableCaptor.capture());
        Runnable waitRunnable = (Runnable)runnableCaptor.getValue();

        verify(mockTaskManager, never()).scheduleMoreWork();

        waitRunnable.run();
        verify(mockTaskManager).scheduleMoreWork();
    }
    //endregion
}