package com.atlassian.confluence.plugins.jira;

import com.atlassian.confluence.plugin.descriptor.web.WebInterfaceContext;
import com.atlassian.confluence.user.DarkFeatureEnabledCondition;

public class DarkFeatureDisabledCondition extends DarkFeatureEnabledCondition
{
    @Override
    protected boolean shouldDisplay(WebInterfaceContext context)
    {
        return !super.shouldDisplay(context);
    }

}
