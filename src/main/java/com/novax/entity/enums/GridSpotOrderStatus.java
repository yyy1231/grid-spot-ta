package com.novax.entity.enums;

public enum GridSpotOrderStatus {

    READY("ready", 0),
    INITIALIZING("initializing", 1),
    RUNNING("running", 2),
    TERMINATING("terminating", 3),
    TERMINATED("terminated", 4);

    private final String desc;
    private final Integer value;

    GridSpotOrderStatus(String desc, Integer value) {
        this.desc = desc;
        this.value = value;
    }

    public String getDesc() {
        return desc;
    }

    public Integer getValue() {
        return value;
    }


    public static boolean isValidGridType(String orderStatus) {
        for (GridSpotOrderStatus status : GridSpotOrderStatus.values()) {
            if (status.getDesc().equals(orderStatus)) {
                return true;
            }
        }
        return false;
    }

    public static String getDescByValue(int orderStatus) {
        for (GridSpotOrderStatus status : GridSpotOrderStatus.values()) {
            if (status.getValue().equals(orderStatus)) {
                return status.getDesc();
            }
        }
        return "";
    }

    public static GridSpotOrderStatus getByDesc(String orderStatus) {
        for (GridSpotOrderStatus status : GridSpotOrderStatus.values()) {
            if (status.getDesc().equals(orderStatus)) {
                return status;
            }
        }
        return null;
    }

    public static GridSpotOrderStatus getByValue(int orderStatus) {
        for (GridSpotOrderStatus status : GridSpotOrderStatus.values()) {
            if (status.getValue().equals(orderStatus)) {
                return status;
            }
        }
        return null;
    }
}
