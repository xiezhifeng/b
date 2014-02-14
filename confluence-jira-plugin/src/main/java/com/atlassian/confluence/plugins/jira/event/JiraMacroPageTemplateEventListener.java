package com.atlassian.confluence.plugins.jira.event;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.DisposableBean;

import com.atlassian.confluence.event.events.template.TemplateUpdateEvent;
import com.atlassian.confluence.pages.templates.PageTemplate;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;

public class JiraMacroPageTemplateEventListener implements DisposableBean
{

    private static final String JIRA_ISSUE_MACRO_TYPE_REG = "<ac:placeholder ac:type=\"jira\">";
    private static final Pattern JIRA_ISSUE_MACRO_PATERN = Pattern.compile(JIRA_ISSUE_MACRO_TYPE_REG);
    private static final String EVENT_NAME = "confluence.template.instructional.create.jira";

    private EventPublisher eventPublisher;

    public JiraMacroPageTemplateEventListener(EventPublisher eventPublisher)
    {
        this.eventPublisher = eventPublisher;
        eventPublisher.register(this);
    }

    @EventListener
    public void templateUpdateEvent(TemplateUpdateEvent pageUpdateEvent)
    {
     
        // is created mode.
        if (pageUpdateEvent.getOldTemplate() == null)
        {
            int events = getNumJiraMacroInTemplate(pageUpdateEvent.getNewTemplate());

            if (events > 0)
            {
                eventPublisher.publish(new JiraMacroTemplateAnalyticEvent(EVENT_NAME, String.valueOf(events)));
            }
        }
        else
        {
            int numberNewEvents = getNumJiraMacroInTemplate(pageUpdateEvent.getNewTemplate());
            int numberOldEvents = getNumJiraMacroInTemplate(pageUpdateEvent.getOldTemplate());
            int events = numberNewEvents - numberOldEvents;
            
            if (events > 0)
            {
                eventPublisher.publish(new JiraMacroTemplateAnalyticEvent(EVENT_NAME, String.valueOf(events)));
            }
        }
    }

    @Override
    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }

    private int getNumJiraMacroInTemplate(PageTemplate template)
    {
        int numMacro = 0;
        String content = template.getContent();
        if (StringUtils.isNotBlank(content))
        {
            Matcher matcher = JIRA_ISSUE_MACRO_PATERN.matcher(content);
            while (matcher.find())
            {
                numMacro ++;
            }
        }
        return numMacro;
    }
}
