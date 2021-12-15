package com.virtualtrainer.machines;

public class Exercise {
    private String name;
    private String type;
    private String sets;
    private String id;

    public Exercise(String Id, String name, String type, String sets) {
        this.name = name;
        this.type = type;
        this.sets = sets;
        this.id  = id;
    }

    public String toString() {
        return  this.name;
    }
}
