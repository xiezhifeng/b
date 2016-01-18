package com.atlassian.confluence.plugins.conluenceview.query;

public abstract class PagingQuery
{
    public static final Integer MAX_ALLOW_ITEM_PER_PAGE = 50;

    private Integer limit;

    private Integer start;

    protected PagingQuery(Builder builder)
    {
        limit = builder.limit;
        start = builder.start;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public Integer getLimit()
    {
        return limit;
    }

    public Integer getStart()
    {
        return start;
    }

    public static class Builder
    {
        private Integer limit;
        private Integer start;

        protected Builder() {}

        protected Integer getMaxAllowItemPerPage()
        {
            return MAX_ALLOW_ITEM_PER_PAGE;
        }

        protected Builder withLimit(Integer limit)
        {
            if (limit > MAX_ALLOW_ITEM_PER_PAGE || limit <= 0)
            {
                limit = getMaxAllowItemPerPage();
            }

            this.limit = limit;
            return this;
        }

        protected Builder withStart(Integer start)
        {
            if (start < 0)
            {
                start = 0;
            }

            this.start = start;
            return this;
        }
    }
}
