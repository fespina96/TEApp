package com.teapp.model;

import java.util.List;
import java.util.Map;

public class WeeklySchedule {
    public String childId;
    public Map<String, Map<String, List<ScheduleEntry>>> week;
}
