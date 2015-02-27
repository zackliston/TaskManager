package com.zackliston.taskmanager;

/**
 * Created by Zack Liston on 2/27/15.
 */
public enum WorkItemState {
    READY(0),
    EXECUTING(1),
    HOLDING(2);

    private final int value;
    WorkItemState(int value) {
        this.value = value;
    }
    public int value() {
        return this.value;
    }

    public static WorkItemState valueOf(int i) {
        if (i == 0) {
            return READY;
        } else if (i == 1) {
            return EXECUTING;
        } else if (i == 2) {
            return HOLDING;
        }
        return null;
    }
}