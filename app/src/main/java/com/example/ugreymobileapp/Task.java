package com.example.ugreymobileapp;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Task {
    private String id;
    private String title;
    private String description;
    private long dueDate;
    private boolean completed;
    private String userId;

    public Task() {
        // Пустой конструктор необходим для Firebase
    }

    public Task(String title, String description, Date dueDate, String userId) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate.getTime();
        this.userId = userId;
        this.completed = false;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return title + " (" + sdf.format(new Date(dueDate)) + ")";
    }
}