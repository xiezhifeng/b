package com.atlassian.confluence.plugins.jira.event;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.atlassian.confluence.event.events.template.TemplateUpdateEvent;
import com.atlassian.confluence.pages.templates.PageTemplate;
import com.atlassian.event.api.EventPublisher;

public class TestJiraMacroPageTemplateEventListener
{
	private JiraMacroPageTemplateEventListener event;
	
	@Mock
	private EventPublisher eventPublisher;
	
	@Before
	public void setUp()
	{
		initMocks(this);
		event = new JiraMacroPageTemplateEventListener(eventPublisher);
	}


	@Test
	public void testCreateTemplatePageContainsIntructionalTextAsJiraMacro()
	{
		String content = "<ac:placeholder>This is an example of instruction text that will get replaced when a user selects the text and begins typing.</ac:placeholder> <ac:placeholder ac:type=\"jira\">jira issue example. This placeholder will automatically search for a user to mention in the page when the user begins typing.</ac:placeholder>";
		PageTemplate template = new PageTemplate();
		template.setContent(content);
		TemplateUpdateEvent templateUpdateEvent = mock(TemplateUpdateEvent.class);
		when(templateUpdateEvent.getNewTemplate()).thenReturn(template);
		ArgumentCaptor<InstructionalJiraAddedToTemplateEvent> analyticsEvent = ArgumentCaptor.forClass(InstructionalJiraAddedToTemplateEvent.class);
		event.publishAnalyticTemplateEvent(templateUpdateEvent);
		verify(eventPublisher).publish(analyticsEvent.capture());
		assertEquals("1", analyticsEvent.getValue().getInstances());
		assertEquals("confluence.template.instructional.create.jira", analyticsEvent.getValue().getEventName());
		
	}
	
}
