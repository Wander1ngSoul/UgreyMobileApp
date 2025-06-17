package com.example.ugreymobileapp;

import java.util.Date;

public class Task {
    private String id;
    private String title;
    private String description;
    private long dueDate; // timestamp
    private int progress;
    private boolean completed;
    private String userId;

    public Task() {}

    public Task(String title, String description, Date dueDate, String userId) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate.getTime();
        this.userId = userId;
        this.progress = 0;
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
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}