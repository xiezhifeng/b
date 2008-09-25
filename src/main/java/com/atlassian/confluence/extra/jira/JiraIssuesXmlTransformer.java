package com.atlassian.confluence.extra.jira;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

public class JiraIssuesXmlTransformer
{
    public Element collapseMultiple(Element rootElement, String attrName )
    {
        Element result;
        
        if( attrName.equals("comments") || attrName.equals("attachments"))
        {
            result = new Element(rootElement.getName());

            Element child = rootElement.getChild(attrName);
            if( child != null )
            {
                int count = child.getChildren().size();
                if( count != 0)
                    result.setText(Integer.toString(count));
            }
        }
        else
            result = collapseMultiple(rootElement, attrName, ", ");
        
        return result;
    }
    
    
    private Element collapseMultiple(Element rootElement, String attrName, String connector)
    {
        Element result;
        
        List<Element> children;
        if(StringUtils.isNotBlank(attrName))
            children = rootElement.getChildren(attrName);
        else
            children = rootElement.getChildren();
        
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
