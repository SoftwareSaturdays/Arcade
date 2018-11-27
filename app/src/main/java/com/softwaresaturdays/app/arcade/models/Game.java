package com.softwaresaturdays.app.arcade.models;

public class Game {
    private String title;

    // All scores need the value and a user id
    private String uid1;
    private double top1;

    private String uid2;
    private double top2;

    private String uid3;
    private double top3;

    public Game() {
        this("");
    }

    public Game(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUid1() {
        return uid1;
    }

    public void setUid1(String uid1) {
        this.uid1 = uid1;
    }

    public double getTop1() {
        return top1;
    }

    public void setTop1(double top1, String uid1) {
        this.top1 = top1;
        this.uid1 = uid1;
    }

    public String getUid2() {
        return uid2;
    }

    public void setUid2(String uid2) {
        this.uid2 = uid2;
    }

    public double getTop2() {
        return top2;
    }

    public void setTop2(double top2, String uid2) {
        this.top2 = top2;
        this.uid2 = uid2;
    }

    public String getUid3() {
        return uid3;
    }

    public void setUid3(String uid3) {
        this.uid3 = uid3;
    }

    public double getTop3() {
        return top3;
    }

    public void setTop3(double top3, String uid3) {
        this.top3 = top3;
        this.uid3 = uid3;
    }
}
