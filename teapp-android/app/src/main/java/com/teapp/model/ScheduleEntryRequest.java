package com.teapp.model;

public class ScheduleEntryRequest {
    public String activityId;
    public String dayOfWeek;
    public String timeSlot;
    public String startTime;
    public String endTime;
    public Integer sortOrder;
    public String notes;
    public Integer durationMinutes;
    public Boolean pausable;
    public Boolean requireFullTimer;
}
