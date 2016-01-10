package com.atlassian.confluence.plugins.conluenceview.rest.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.atlassian.confluence.plugins.conluenceview.rest.dto.GenericResponseDto;

import org.apache.commons.httpclient.HttpStatus;

@Provider
public class CacheTokenNotFoundExceptionMapper implements ExceptionMapper<CacheTokenNotFoundException>
{
    @Override
    public Response toResponse(CacheTokenNotFoundException exception)
    {
        return Response
                .ok(new GenericResponseDto.Builder()
                        .withStatus(HttpStatus.SC_OK)
                        .withErrorMessage("Cache token not found")
                        .build())
                .build();
    }
}
