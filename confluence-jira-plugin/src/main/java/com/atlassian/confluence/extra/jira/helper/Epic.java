package com.atlassian.confluence.extra.jira.helper;

final public class Epic {

    final private String key;
    final private String name;
    final private String colour;
    final private String status;

    public Epic(String key, String name, String colour, String status){
        this.key = key;
        this.name = name;
        this.colour = colour;
        this.status = status;
    }

    public String getKey(){
        return key;
    }

    public String getName(){
        return name;
    }

    public String getColour() { return colour; }

    public String getStatus() { return status; }
}
