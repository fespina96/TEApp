package com.teapp.model;

public class ActivityStepRequest {
    public String title;
    public String description;
    public String imageBase64;
    public String pictogramUrl;

    public ActivityStepRequest(String title, String description,
                                String imageBase64, String pictogramUrl) {
        this.title        = title;
        this.description  = description;
        this.imageBase64  = imageBase64;
        this.pictogramUrl = pictogramUrl;
    }
}
