package com.atlassian.confluence.extra.jira.helper;

final public class Epic {

    final private String key;
    final private String name;

    public Epic(String key, String name){
        this.key = key;
        this.name = name;
    }

    public String getKey(){
        return key;
    }

    public String getName(){
        return name;
    }
}
