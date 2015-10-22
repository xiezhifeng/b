package com.atlassian.confluence.extra.jira.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.confluence.xhtml.api.MacroDefinitionHandler;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class TestDefaultJiraMacroFinderService
{
    @Mock
    private XhtmlContent xhtmlContent;

    @Test
    public void testCanFindJiraIssueMacro() throws Exception
    {
        AbstractPage page = mock(AbstractPage.class);
        xhtmlContent = mockXhtmlContent(createSingleJiraMacroDefinition("CONFDEV-19009"),
                createMacroDefinition("info", new HashMap<String,String>()));
        DefaultJiraMacroFinderService service = new DefaultJiraMacroFinderService(xhtmlContent);

        List<MacroDefinition> result = service.findJiraMacros(page, null);
        assertEquals(1, result.size());
        assertEquals("jira", result.iterator().next().getName());
    }

    @Test
    public void testCanFindMultipleMacros() throws Exception
    {
        AbstractPage page = mock(AbstractPage.class);
        xhtmlContent = mockXhtmlContent(createSingleJiraMacroDefinition("CONFDEV-19009"),
                createSingleJiraMacroDefinition("CONFDEV-1"),
                createMacroDefinition("info", new HashMap<String,String>()));

        DefaultJiraMacroFinderService service = new DefaultJiraMacroFinderService(xhtmlContent);

        List<MacroDefinition> result = service.findJiraMacros(page, null);
        assertEquals(2, result.size());
        for (MacroDefinition definition : result)
            assertEquals("jira", definition.getName());
    }

    @Test
    public void testCanFindMacroWithPredicate() throws Exception
    {
        AbstractPage page = mock(AbstractPage.class);
        xhtmlContent = mockXhtmlContent(createSingleJiraMacroDefinition("CONFDEV-19009"),
                createSingleJiraMacroDefinition("CONFDEV-1"),
                createMacroDefinition("info", Maps.<String,String>newHashMap()));

        DefaultJiraMacroFinderService service = new DefaultJiraMacroFinderService(xhtmlContent);

        Predicate<MacroDefinition> predicate = new Predicate<MacroDefinition>()
        {
            @Override
            public boolean apply(MacroDefinition def)
            {
                return "CONFDEV-19009".equals(def.getParameters().get("key"));
            }
        };

        List<MacroDefinition> result = service.findJiraMacros(page, predicate);
        assertEquals(1, result.size());
        assertEquals("CONFDEV-19009", result.iterator().next().getParameters().get("key"));
    }

    private MacroDefinition createSingleJiraMacroDefinition(String key)
    {
        return createMacroDefinition("jira", ImmutableMap.<String,String>builder().put("key", key).build());
    }

    private MacroDefinition createMacroDefinition(String name, Map<String,String> parameters)
    {
        MacroDefinition definition = new MacroDefinition();
        definition.setName(name);
        definition.setParameters(parameters);
        return definition;
    }

    private XhtmlContent mockXhtmlContent(final MacroDefinition... macroDefinitions) throws Exception
    {
        XhtmlContent xhtmlContent = mock(XhtmlContent.class);
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                MacroDefinitionHandler handler = (MacroDefinitionHandler) args[2];
                for (MacroDefinition definition : macroDefinitions)
                {
                    handler.handle(definition);
                }
                return null;
            }
        }).when(xhtmlContent).handleMacroDefinitions(anyString(), any(ConversionContext.class),
                any(MacroDefinitionHandler.class));
        return xhtmlContent;
    }

}
