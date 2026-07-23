package com.oa2.pojo;

import lombok.Data;

@Data
public class LeaveRequest {
    private String id;
    private int number;
    private String name;
    private String dept_name;
    private String startDate;
    private String endDate;
    private String reason;
    private String status;
}
