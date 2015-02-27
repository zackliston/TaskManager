package com.zackliston.taskmanager;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class InternalWorkItemTest {

    private InternalWorkItem workItem;

    @Before
    public void setup() {
        workItem = new InternalWorkItem();
    }

    @After
    public void tearDown() {
        workItem = null;
    }

    @Test
    public void testSetData() throws Exception {
        String data = "{\"key\":\"value1\"}";
        workItem.setData(data);

        assertThat(workItem.getJsonData().toString(), is(data));
    }

    @Test
    public void testGetJsonData() throws Exception {
        workItem.setJsonData(null);
        String data = "{\"key\":\"value1\"}";
        workItem.setData(data);


        assertThat(workItem.getJsonData().toString(), is(data));
    }

    @Test
    public void testSetJsonData() throws Exception {
        JSONObject object = new JSONObject("{key:value}");
        workItem.setJsonData(object);

        String data = workItem.getData();
        JSONObject returnedObject = workItem.getJsonData();
        assertThat(returnedObject, is(object));
        assertThat(returnedObject.toString(), is(data));

    }
}