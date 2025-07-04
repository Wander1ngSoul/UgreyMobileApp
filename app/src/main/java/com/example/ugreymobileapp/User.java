package com.example.ugreymobileapp;

public class User {
    private String lastName;
    private String firstName;
    private String middleName;
    private String email;
    private String passwordHash;

    public User() {
    }

    public User(String lastName, String firstName, String middleName, String email, String passwordHash) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}