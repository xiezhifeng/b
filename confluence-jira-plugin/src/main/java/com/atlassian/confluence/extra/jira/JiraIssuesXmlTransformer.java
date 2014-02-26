package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.core.util.HTMLUtils;
import com.atlassian.gzipfilter.org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

import java.text.DateFormat;
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
    public String valueForFieldDateFormatted(Element rootElement, String fieldName, DateFormat dateFormat)
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
                    return dateFormat.format(valueAsDate);
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

    public Date getEndDateValue(Element rootElement, Date startDate)
    {
        if(startDate != null && rootElement.getChild("timeoriginalestimate") != null)
        {
            String duration = rootElement.getChild("timeoriginalestimate").getValue();
            return calculate(startDate, duration);
        }
        return null;
    }

    private Date calculate(Date startDate, String duration)
    {
        try
        {

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            String[] times = duration.split(",");
            for(String time : times)
            {
                int weekIndex = getIndex(time, "week", "weeks");
                if(weekIndex > 0)
                {
                    int week = Integer.parseInt(time.substring(0, weekIndex).trim());
                    calendar.add(Calendar.WEEK_OF_YEAR, week);
                    continue;
                }

                int dayIndex = getIndex(time, "day", "days");
                if(dayIndex > 0)
                {
                    int day = Integer.parseInt(time.substring(0, dayIndex).trim());
                    calendar.add(Calendar.DAY_OF_YEAR, day);
                    continue;
                }

                int hourIndex = getIndex(time, "hour", "hours");
                if(dayIndex > 0)
                {
                    int hours = Integer.parseInt(time.substring(0, hourIndex).trim());
                    calendar.add(Calendar.HOUR_OF_DAY, hours);
                    continue;
                }

                int minuteIndex = getIndex(time, "minute", "minutes");
                if(dayIndex > 0)
                {
                    int minutes = Integer.parseInt(time.substring(0, minuteIndex).trim());
                    calendar.add(Calendar.MINUTE, minutes);
                    continue;
                }
            }

            return calendar.getTime();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private int getIndex(String time, String name1, String name2)
    {
        if(time.indexOf(name1) > -1) return time.indexOf(name1);
        if(time.indexOf(name2) > -1) return time.indexOf(name2);
        return -1;
    }

    public Date getStartDateValue(Element rootElement)
    {
        try
        {
            Element customFieldsElement = rootElement.getChild("customfields");
            if( customFieldsElement != null )
            {
                List<Element> customFieldElements = (List<Element>) customFieldsElement.getChildren();
                for (Element customFieldElement : customFieldElements)
                {
                    if ("Start Date".equals(customFieldElement.getChild("customfieldname").getValue()))
                    {
                        Element customFieldValuesElement = customFieldElement.getChild("customfieldvalues");
                        List<Element> customFieldValueElements = (List<Element>) customFieldValuesElement.getChildren();


                        for (Element customFieldValueElement : customFieldValueElements)
                        {
                            return GeneralUtil.convertMailFormatDate(customFieldValueElement.getValue());
                        }

                    }
                }
            }
        }
        catch (Exception e)
        {
            return null;
        }

        return null;
    }

    public String getStartDateID(Element rootElement)
    {
        try
        {
            Element customFieldsElement = rootElement.getChild("customfields");
            if( customFieldsElement != null )
            {
                List<Element> customFieldElements = (List<Element>) customFieldsElement.getChildren();
                for (Element customFieldElement : customFieldElements)
                {
                    if ("Start Date".equals(customFieldElement.getChild("customfieldname").getValue()))
                    {
                        return customFieldElement.getAttribute("id").getValue();
                    }
                }
            }
        }
        catch (Exception e)
        {
            return "";
        }

        return "";
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

    public String getJsonIssue(Element rootElement, String group)
    {
        String key = rootElement.getChild("key").getValue();
        String summary = rootElement.getChild("summary").getValue();
        Date startDate = getStartDateValue(rootElement);
        Date endDate = getEndDateValue(rootElement, startDate);

        StringBuilder json = new StringBuilder("{start: ");
        Calendar calendar = Calendar.getInstance();
        if(startDate != null)
        {
            calendar.setTime(startDate);
            json.append("new Date(" + calendar.get(Calendar.YEAR) + "," + calendar.get(Calendar.MONTH) + "," + calendar.get(Calendar.DAY_OF_MONTH) + "), ");
        }
        else
        {
            json.append("null, ");
        }


        json.append("end: ");
        if(endDate != null)
        {
            calendar.setTime(endDate);
            json.append("new Date(" + calendar.get(Calendar.YEAR) + "," + calendar.get(Calendar.MONTH) + "," + calendar.get(Calendar.DAY_OF_MONTH) + "), ");
        }
        else
        {
            json.append("null, ");
        }

        json.append("key: '" + key + "', ");

        String groupValue;
        if(group.equals("components"))
        {
            Element element = rootElement.getChild("component");
            groupValue = element == null ? "" : element.getValue();
        }
        else
        {
            Element element = rootElement.getChild("assignee");
            groupValue = element == null ? "" : element.getValue();
        }

        json.append("group: '" + groupValue + "', ");

        String description = rootElement.getChild("description") == null ? "" : rootElement.getChild("description").getValue();
        json.append("description: '" + StringEscapeUtils.escapeJavaScript(description) + "', ");

        json.append("issuetype: '" + rootElement.getChild("type").getAttribute("iconUrl").getValue() + "', ");
        json.append("issuelink: '" + rootElement.getChild("link").getValue() + "', ");

        json.append("priority: '" + rootElement.getChild("priority") == null ? "" : rootElement.getChild("priority").getValue() + "', ");

        json.append("status: '" + rootElement.getChild("status").getValue() + "', ");
        json.append("summary: '" + StringEscapeUtils.escapeJavaScript(summary) + "'} ");

        return json.toString();
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

    public Set<String> getIssueKeyValues(Element issueLinks)
    {
        Set<String> issueKeyValues = new HashSet<String>();

        if (issueLinks != null)
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
