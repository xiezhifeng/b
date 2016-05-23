package com.atlassian.confluence.extra.jira.helper;

import java.util.List;
import java.util.Map;

public class FieldInfo {
    public String id;
    public String name;
    public boolean custom;
    public boolean orderable;
    public boolean navigable;
    public boolean searchable;
    public List<String> clauseNames;
    public Map<String, Object> schema;
}
