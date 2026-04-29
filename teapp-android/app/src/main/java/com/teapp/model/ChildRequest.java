package com.teapp.model;

public class ChildRequest {
    public String name;
    public String dateOfBirth;
    public String avatarColor;
    public String notes;

    public ChildRequest(String name, String dateOfBirth, String avatarColor, String notes) {
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.avatarColor = avatarColor;
        this.notes = notes;
    }
}
