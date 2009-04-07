package com.atlassian.confluence.extra.jira;

import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JiraIssuesXmlTransformer
{   
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
    
    public Element valueForField(Element rootElement, String fieldName, Map<String, String> columnMap)
    {
        // First, check if this is a builtin that isn't in the list above
        Element result = findSimpleBuiltinField(rootElement, fieldName);
            
        if( result == null)
        {
            result = new Element(rootElement.getName());
        
            // TODO: maybe do this on first time only somehow?
            Element customFieldsElement = rootElement.getChild("customfields");
            if( customFieldsElement != null ) 
            {
                @SuppressWarnings("unchecked")
                List<Element> customFieldElements = (List<Element>) customFieldsElement.getChildren();
                StringBuilder valueBuilder = new StringBuilder();
                
                // go through all the children and find which has the right customfieldname
                for (Element customFieldElement : customFieldElements)
                {
                    String customFieldName = customFieldElement.getChild("customfieldname").getValue();
                    String customFieldId = customFieldElement.getAttributeValue("id");
                    
                    updateColumnMap(columnMap, customFieldId, customFieldName);
                    
                    if(StringUtils.equalsIgnoreCase(customFieldName, fieldName))
                    {
                        Element customFieldValuesElement = customFieldElement.getChild("customfieldvalues");
                        @SuppressWarnings("unchecked")
                        List<Element> customFieldValueElements = (List<Element>) customFieldValuesElement.getChildren();

                        valueBuilder.setLength(0);

                        for (Element customFieldValueElement : customFieldValueElements)
                            valueBuilder.append(customFieldValueElement.getValue()).append(' ');
                    }
                }
                
                result.setText(valueBuilder.toString());
            }
        }
        
        return result;
    }
    
    protected Element findSimpleBuiltinField(Element rootElement, String fieldName)
    {
        @SuppressWarnings("unchecked")
        List<Element> children = rootElement.getChildren(fieldName);
        
        if( children.size() == 1)
        {
            return children.get(0);
        }

        return null;
    }

    private void updateColumnMap(Map<String, String> columnMap, String columnId, String columnName)
    {
        if (columnMap != null && !columnMap.containsKey(columnName))
        {
            columnMap.put(columnName, columnId);
        }
    }    

    @SuppressWarnings("unchecked")
    protected Element collapseMultiple(Element rootElement, String attrName, String connector)
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

    public String findIconUrl( Element xmlItemField, Map iconMap )
    {
        String iconUrl = "";

        if( xmlItemField != null )
        {
            String value = xmlItemField.getValue();

            // first look for icon in user-set mapping, and then check in the xml returned from jira
            iconUrl = (String) iconMap.get(value);
            if(StringUtils.isBlank(iconUrl) )
                iconUrl = StringUtils.defaultString(xmlItemField.getAttributeValue("iconUrl"));
        }

        return iconUrl;
    }
}
