package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.content.render.xhtml.definition.RichTextMacroBody;
import com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.xhtml.api.MacroDefinition;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.RandomUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public abstract class AsyncMacro implements Macro
{

    private static final String FUTURE_TEMPLATE = "templates/extra/jira/future.vm";
    
    private MacroMarshallingFactory macroMarshallingFactory;

    @Override
    public final String execute(Map<String, String> parameters, String body, ConversionContext conversionContext)
            throws MacroExecutionException
    {
        if (getBooleanProperty(conversionContext.getProperty("forceRender", Boolean.FALSE)) || !shouldRenderSynchronously(parameters, body, conversionContext))
        {
            return executeInternal(parameters, body, conversionContext);
        }
        else
        {
            Map<String,Object> context = MacroUtils.defaultVelocityContext();

            int futureId = getNextFutureId();

            context.put("futureId", new Integer(futureId));
            MacroDefinition macroDefinition = new MacroDefinition("jira", new RichTextMacroBody(body), null, parameters);
            try
            {
                Streamable out = macroMarshallingFactory.getStorageMarshaller().marshal(macroDefinition, conversionContext);
                StringWriter writer = new StringWriter();
                out.writeTo(writer);
                context.put("wikiMarkup", writer.toString());
            }
            catch (XhtmlException e)
            {
                throw new MacroExecutionException("Unable to constract macro definition.", e);
            }
            catch (IOException e)
            {
                throw new MacroExecutionException("Unable to constract macro definition.", e);
            }
            context.put("contentId", conversionContext.getEntity().getId());

            return VelocityUtils.getRenderedTemplate(FUTURE_TEMPLATE, context);
        }
    }
    
    protected abstract boolean shouldRenderSynchronously(Map<String, String> parameters, String body, ConversionContext conversionContext);

    public abstract String executeInternal(Map<String, String> parameters, String body, ConversionContext conversionContext) 
            throws MacroExecutionException;

    private static boolean getBooleanProperty(Object value)
    {
        if (value instanceof Boolean)
        {
            return ((Boolean) value).booleanValue();
        }
        else if (value instanceof String)
        {
            return BooleanUtils.toBoolean((String) value);
        }
        else
        {
            return false;
        }
    }
    
    private int getNextFutureId()
    {
        return RandomUtils.nextInt();
    }

    public void setMacroMarshallingFactory(MacroMarshallingFactory macroMarshallingFactory)
    {
        this.macroMarshallingFactory = macroMarshallingFactory;
    }
}
