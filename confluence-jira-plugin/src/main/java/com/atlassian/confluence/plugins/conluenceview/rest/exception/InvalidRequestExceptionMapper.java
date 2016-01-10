package com.atlassian.confluence.plugins.conluenceview.rest.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.atlassian.confluence.plugins.conluenceview.rest.dto.GenericResponseDto;

import org.apache.commons.httpclient.HttpStatus;

@Provider
public class InvalidRequestExceptionMapper implements ExceptionMapper<InvalidRequestException>
{
    @Override
    public Response toResponse(InvalidRequestException exception)
    {
        return Response
            .ok(new GenericResponseDto.Builder()
                    .withStatus(HttpStatus.SC_BAD_REQUEST)
                    .withErrorMessage(exception.getMessage()))
            .build();
    }
}
