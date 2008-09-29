package com.atlassian.confluence.extra.jira;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

public class JiraIssuesXmlTransformer
{
    public static final List<String> BUILTIN_RSS_FIELDS = Arrays.asList(new String[] { 
            "description", "environment", "key", "summary", "type", "parent",
            "priority", "status", "version", "resolution", "security", "assignee", "reporter",
            "created", "updated", "due", "component", "votes", "comments", "attachments",
            "subtasks", "fixVersion", "timeoriginalestimate", "timeestimate"  });

    /*
    @returns true if column is one of the built-in fields 
     */
    public boolean isColumnBuiltIn(String columnName)
    {
        return BUILTIN_RSS_FIELDS.contains(columnName);
    }
    
    public boolean isColumnMultivalued(String columnName)
    {
        return columnName.equalsIgnoreCase("version") || columnName.equalsIgnoreCase("component") ||
                columnName.equalsIgnoreCase("comments") || columnName.equalsIgnoreCase("attachments");
    }
    
    public Element collapseMultiple(Element rootElement, String childName )
    {
        Element result;
        
        if( childName.equalsIgnoreCase("comments") || childName.equalsIgnoreCase("attachments"))
        {
            result = new Element(rootElement.getName());

            Element child = rootElement.getChild(childName);
            if( child != null )
            {
                int count = child.getChildren().size();
                if( count != 0)
                    result.setText(Integer.toString(count));
            }
        }
        else
            result = collapseMultiple(rootElement, childName, ", ");
        
        return result;
    }
    
    public Element valueForField(Element rootElement, String fieldName)
    {
        return valueForField(rootElement, fieldName, null);
    }
    
    public Element valueForField(Element rootElement, String fieldName, Map columnMap)
    {
        // First, check if this is a builtin that isn't in the list above
        Element result = findBuiltinChild(rootElement, fieldName);
            
        if( result == null)
        {
            result = new Element(rootElement.getName());
        
            // TODO: maybe do this on first time only somehow?
            Element customFieldsElement = rootElement.getChild("customfields");
            List customFieldList = customFieldsElement.getChildren();
    
            String value = "";
            
            // go through all the children and find which has the right customfieldname
            Iterator customFieldListIterator = customFieldList.iterator();
            while(customFieldListIterator.hasNext())
            {
                Element customFieldElement = (Element)customFieldListIterator.next();
                String customFieldName = customFieldElement.getChild("customfieldname").getValue();
    
                String customFieldId = customFieldElement.getAttributeValue("id");
                updateColumnMap(columnMap, customFieldId, customFieldName);
                
                if(customFieldName.equalsIgnoreCase(fieldName))
                {
                    Element customFieldValuesElement = customFieldElement.getChild("customfieldvalues");
                    List customFieldValuesList = customFieldValuesElement.getChildren();
                    Iterator customFieldValuesListIterator = customFieldValuesList.iterator();
                    while(customFieldValuesListIterator.hasNext())
                        value += ((Element)customFieldValuesListIterator.next()).getValue()+" ";
                }
            }
            result.setText(value);
        }
        
        return result;
    }
    
    private Element findBuiltinChild(Element rootElement, String fieldName)
    {
        List<Element> children = rootElement.getChildren(fieldName);
        
        if( children.size() == 1)
        {
            return children.get(0);
        }

        return null;
    }

    private void updateColumnMap(Map columnMap, String columnId, String columnName)
    {
        if (columnMap != null && !columnMap.containsKey(columnName))
        {
            columnMap.put(columnName, columnId);
        }
    }    
 
    
    private Element collapseMultiple(Element rootElement, String attrName, String connector)
    {
        Element result;
        
        List<Element> children;
        if(StringUtils.isNotBlank(attrName))
            children = rootElement.getChildren(attrName);
        else
            children = Collections.emptyList();
        
        if( children.size() == 1)
        {
            result = children.get(0);
        }
        else
        {
            result = new Element(rootElement.getName());
            
            StringBuffer value = new StringBuffer();
            connector = StringUtils.defaultString(connector);
            for (Iterator<Element> iter = children.iterator(); iter.hasNext();)
            {
                Element attrElement = iter.next();
                value.append(attrElement.getValue());
                if(iter.hasNext())
                    value.append(connector);
            }
    
            result.setText(value.toString());
        }
        
        return result;
    }
}
