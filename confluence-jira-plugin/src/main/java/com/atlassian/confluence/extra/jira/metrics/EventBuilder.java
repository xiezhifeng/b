package com.atlassian.confluence.extra.jira.metrics;

public interface EventBuilder
{
    EventBuilder DEVNULL = new EventBuilder()
    {
        @Override
        public void publish()
        {
            //
        }
    };


    void publish();
}
