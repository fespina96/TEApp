package com.teapp.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleEntry implements Serializable {
    public String id;
    public ActivityItem activity;
    public String dayOfWeek;
    public String timeSlot;
    public String startTime;
    public String endTime;
    public int sortOrder;
    public String notes;
    public Integer durationMinutes;
    public Boolean pausable;
    public Boolean requireFullTimer;
    public List<String> completedDates;

    public boolean isCompletedToday() {
        if (completedDates == null) return false;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return completedDates.contains(today);
    }

    public static String todayIso() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }
}
