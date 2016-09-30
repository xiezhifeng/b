package com.atlassian.confluence.extra.jira;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface ConfluenceJiraPluginSettingManager
{
    /**
     * Setting JIRA Issues Macro cache timeout in minutes
     * @param minutes
     */
    void setCacheTimeoutInMinutes(@Nonnull Optional<Integer> minutes);

    /**
     * @return Jira Issue Macro cache timeout in minutes.
     */
    @Nonnull
    Optional<Integer> getCacheTimeoutInMinutes();
}
