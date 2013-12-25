package com.atlassian.confluence.extra.jira;

import static com.atlassian.confluence.extra.jira.FlexigridResponseGenerator.DATE_VALUE_FORMAT;
import com.atlassian.confluence.util.GeneralUtil;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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

    /**
     * Returns field value as a string in date format if possible.
     *
     * @param rootElement
     * The &quot;/rss/channel/item&quot; element in JIRA's Issue Navigator XML.
     * @param fieldName
     * The field name to return the value of
     * @return
     * The value of the field if one is found. It will be in the format of
     * {@link com.atlassian.confluence.extra.jira.FlexigridResponseGenerator#DATE_VALUE_FORMAT} if it can be
     * interpreted as date. Otherwise, the value is returned as is.
     */
    public String valueForFieldDateFormatted(Element rootElement, String fieldName)
    {
        Element valueForField = valueForField(rootElement, fieldName);
        if (null != valueForField)
        {
            String value = valueForField.getValue();
            Date valueAsDate;

            try
            {
                if (StringUtils.isNotBlank(value) && null != (valueAsDate = GeneralUtil.convertMailFormatDate(value)))
                {
                    return new SimpleDateFormat(DATE_VALUE_FORMAT).format(valueAsDate);
                }
                else
                {
                    return value;
                }
            }
            catch (ParseException pe)
            {
                return value;
            }
        }

        return null;
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

    public String findIconUrl( Element xmlItemField)
    {
        String iconUrl = "";

        if( xmlItemField != null )
        {
            iconUrl = StringUtils.defaultString(xmlItemField.getAttributeValue("iconUrl"));
        }
        return iconUrl;
    }

    public List<String> getIssueKeyValues(Element issueLinks)
    {
        List<String> issueKeyValues = new ArrayList<String>();

        if(issueLinks != null)
        {
            Iterator<Element> issueKeys = issueLinks.getDescendants(new ElementFilter("issuekey"));
            while (issueKeys.hasNext())
            {
                issueKeyValues.add(issueKeys.next().getValue());
            }
        }
        return issueKeyValues;
    }
}
