package com.teapp.model;

import java.io.Serializable;

public class ActivityItem implements Serializable {
    public String id;
    public String name;
    public String description;
    public String category;
    public String iconName;
    public String color;
    public boolean predefined;
    public boolean therapistCreated;
    public String imageBase64;
    public String pictogramUrl;
    public Integer durationMinutes;
    public Boolean pausable;
    public Integer stepCount;
    public String createdAt;
}
