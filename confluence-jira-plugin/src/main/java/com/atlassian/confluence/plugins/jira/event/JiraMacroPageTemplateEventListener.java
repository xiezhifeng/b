package com.atlassian.confluence.plugins.jira.event;

import com.atlassian.confluence.event.events.template.TemplateUpdateEvent;
import com.atlassian.confluence.pages.templates.PageTemplate;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JiraMacroPageTemplateEventListener implements DisposableBean
{

    private static final String JIRA_ISSUE_MACRO_TYPE_REG = "<ac:placeholder ac:type=\"jira\">";
    private static final Pattern JIRA_ISSUE_MACRO_PATTERN = Pattern.compile(JIRA_ISSUE_MACRO_TYPE_REG);

    private EventPublisher eventPublisher;

    public JiraMacroPageTemplateEventListener(EventPublisher eventPublisher)
    {
        this.eventPublisher = eventPublisher;
        eventPublisher.register(this);
    }

    @EventListener
    public void publishAnalyticTemplateEvent(TemplateUpdateEvent pageUpdateEvent)
    {
     
        int instances = 0 ;
        // is created mode.
        if (pageUpdateEvent.getOldTemplate() == null)
        {
            instances = getNumJiraMacroInTemplate(pageUpdateEvent.getNewTemplate());
        }
        else
        {
            int numberNewInstances = getNumJiraMacroInTemplate(pageUpdateEvent.getNewTemplate());
            int numberOldInstances = getNumJiraMacroInTemplate(pageUpdateEvent.getOldTemplate());
            instances = numberNewInstances - numberOldInstances;
        }
        
        if (instances > 0)
        {
            eventPublisher.publish(new InstructionalJiraAddedToTemplateEvent(String.valueOf(instances)));
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
            Matcher matcher = JIRA_ISSUE_MACRO_PATTERN.matcher(content);
            while (matcher.find())
            {
                numMacro ++;
            }
        }
        return numMacro;
    }
}
