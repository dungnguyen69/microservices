package com.fullstack.Backend.constant;

public class constant {
    public static final String DEFAULT_PAGE_NUMBER = "1";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "desc";

    // For request keyword suggestion
    public static final int REQUEST_REQUEST_ID_COLUMN = 0;
    public static final int REQUEST_DEVICE_NAME_COLUMN = 1;
    public static final int REQUEST_DEVICE_SERIAL_NUMBER_COLUMN = 2;
    public static final int REQUEST_REQUESTER_COLUMN = 3;
    public static final int REQUEST_CURRENT_KEEPER_COLUMN = 4;
    public static final int REQUEST_NEXT_KEEPER_COLUMN = 5;
    public static final int REQUEST_APPROVER_COLUMN = 6;

    // Request status
    public static final int APPROVED = 0;
    public static final int CANCELLED = 2;
    public static final int TRANSFERRED = 3;
    public static final int PENDING = 4;
    public static final int RETURNED = 5;
    public static final int EXTENDING = 6;
}
