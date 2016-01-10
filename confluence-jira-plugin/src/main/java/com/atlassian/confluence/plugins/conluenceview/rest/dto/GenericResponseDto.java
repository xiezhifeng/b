package com.atlassian.confluence.plugins.conluenceview.rest.dto;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.httpclient.HttpStatus;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
/**
 * All other DTO should extend this class so that they will have 2 basic information:
 * - status which indicates the status to this result @see {@link com.atlassian.jira.extra.software.rest.RestStatus}
 * - error message might contain value if status is error
 */
public class GenericResponseDto implements Serializable
{
    private int status = HttpStatus.SC_OK;

    private String errorMessage;

    protected GenericResponseDto(int status, String errorMessage)
    {
            this.status = status;
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public static class Builder
    {
        protected int status;

        protected String errorMessage;

        public Builder()
        {
            this.errorMessage = "";
        }

        public Builder withStatus(int status)
        {
            this.status = status;
            return this;
        }

        public Builder withErrorMessage(String errorMessage)
        {
            this.errorMessage = errorMessage;
            return this;
        }
    }
}
