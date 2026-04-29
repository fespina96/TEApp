package com.teapp.model;

public class ActivityRequest {
    public String name;
    public String description;
    public String category;
    public String iconName;
    public String color;
    public String pictogramUrl;
    public Integer durationMinutes;
    public boolean pausable;

    public ActivityRequest(String name, String description, String category,
                           String color, String pictogramUrl,
                           Integer durationMinutes, boolean pausable) {
        this.name            = name;
        this.description     = description;
        this.category        = category;
        this.color           = color;
        this.pictogramUrl    = pictogramUrl;
        this.durationMinutes = durationMinutes;
        this.pausable        = pausable;
    }
}
