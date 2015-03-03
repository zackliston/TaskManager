package com.zackliston.taskmanager;

import junit.framework.TestCase;

import com.zackliston.taskmanager.Task;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

@Config(manifest=Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class TaskTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testBaseInit() throws Exception {
        Task task = new Task();

        assertThat(task.getMajorPriority(), is(Task.DEFAULT_MAJOR_PRIORITY));
        assertThat(task.getMinorPriority(), is(Task.DEFAULT_MINOR_PRIORITY));
        assertThat(task.getMaxRetries(), is(Task.DEFAULT_MAX_RETRY_COUNT));
        assertThat("Doesn't require internet", task.isRequiresInternet() == false);
        assertThat("Doesn't hold", task.isShouldHoldAfterMaxRetries() == false);
    }

    @Test
    public void testConvenienceInit() throws Exception {
        String taskType = "taskThing";
        JSONObject jsonObject = new JSONObject("{key:val}");

        Task task = new Task(taskType, jsonObject);

        assertThat(task.getTaskType(), is(taskType));
        assertThat(task.getJsonData(), is(jsonObject));

        assertThat(task.getMajorPriority(), is(Task.DEFAULT_MAJOR_PRIORITY));
        assertThat(task.getMinorPriority(), is(Task.DEFAULT_MINOR_PRIORITY));
        assertThat(task.getMaxRetries(), is(Task.DEFAULT_MAX_RETRY_COUNT));
        assertThat("Doesn't require internet", task.isRequiresInternet() == false);
        assertThat("Doesn't hold", task.isShouldHoldAfterMaxRetries() == false);
    }
}