package com.atlassian.confluence.plugins.conluenceview.rest.dto;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.httpclient.HttpStatus;

/**
 * All other DTO should extend this class so that they will have 2 basic information:
 * - status which indicates the status to this result
 * - error message might contain value if status is error
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class GenericResponseDto implements Serializable
{
    private int status = HttpStatus.SC_OK;

    private String errorMessage;

    protected GenericResponseDto(Builder builder)
    {
        this.status = builder.status;
        this.errorMessage = builder.errorMessage;
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
        protected int status = HttpStatus.SC_OK;

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

        public GenericResponseDto build()
        {
            return new GenericResponseDto(this);
        }
    }
}
