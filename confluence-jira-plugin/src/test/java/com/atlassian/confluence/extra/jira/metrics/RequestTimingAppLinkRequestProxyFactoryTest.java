package com.atlassian.confluence.extra.jira.metrics;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.auth.AuthenticationProvider;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseHandler;
import com.atlassian.sal.api.net.ReturningResponseHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;
import java.util.concurrent.Callable;

import static com.atlassian.sal.api.net.Request.MethodType.GET;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequestTimingAppLinkRequestProxyFactoryTest
{
    @Mock
    ApplicationLink applicationLink;

    @Mock
    ApplicationLinkRequestFactory requestFactory;

    @Mock
    ApplicationLinkRequest request;

    @Mock
    private JiraIssuesMacroRenderEvent.Builder metrics;

    @Mock
    private Timer requestTimer;

    @Mock
    private Timer appLinkResolutionTimer;

    @Mock
    ApplicationLinkResponseHandler applicationLinkResponseHandler;

    @Mock
    ResponseHandler<Response> responseHandler;

    @Mock
    ReturningResponseHandler<Response, String> returningResponseHandler;

    private ApplicationLink proxiedAppLink;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        when(applicationLink.createAuthenticatedRequestFactory()).thenReturn(requestFactory);
        when(applicationLink.createAuthenticatedRequestFactory(any(Class.class))).thenReturn(requestFactory);
        when(applicationLink.createImpersonatingAuthenticatedRequestFactory()).thenReturn(requestFactory);
        when(applicationLink.createNonImpersonatingAuthenticatedRequestFactory()).thenReturn(requestFactory);

        when(requestFactory.createRequest(any(Request.MethodType.class), anyString())).thenReturn(request);

        when(metrics.appLinkRequestTimer()).thenReturn(requestTimer);
        when(metrics.applinkResolutionTimer()).thenReturn(appLinkResolutionTimer);

        proxiedAppLink = RequestTimingAppLinkRequestProxyFactory.proxyApplicationLink(metrics, new Callable<ApplicationLink>()
        {
            @Override
            public ApplicationLink call() throws Exception
            {
                return applicationLink;
            }
        });
    }

    @Test
    public void requestExecutionReturnsResultAndIncrementsMetricsCounter() throws Exception
    {
        when(request.execute()).thenReturn("bar");
        final String response = proxiedAppLink.createAuthenticatedRequestFactory().createRequest(GET, "foo").execute();
        assertThat(response, is("bar"));

        verify(requestTimer, times(1)).start();
        verify(requestTimer, times(1)).stop();
    }

    @Test
    public void allCreateRequestFactoryMethodsAreProxied() throws Exception
    {
        when(request.execute()).thenReturn("bar");

        assertThat(proxiedAppLink.createAuthenticatedRequestFactory().createRequest(GET, "foo").execute(), is("bar"));
        assertThat(proxiedAppLink.createAuthenticatedRequestFactory(AuthenticationProvider.class).createRequest(GET, "foo").execute(), is("bar"));
        assertThat(proxiedAppLink.createImpersonatingAuthenticatedRequestFactory().createRequest(GET, "foo").execute(), is("bar"));
        assertThat(proxiedAppLink.createNonImpersonatingAuthenticatedRequestFactory().createRequest(GET, "foo").execute(), is("bar"));

        verify(requestTimer, times(4)).start();
        verify(requestTimer, times(4)).stop();
    }

    @Test
    public void allRequestExecuteMethodsAreProxied() throws Exception
    {
        final ApplicationLinkRequest proxiedRequest = proxiedAppLink.createAuthenticatedRequestFactory().createRequest(GET, "foo");

        when(request.execute()).thenReturn("bar");
        assertThat(proxiedRequest.execute(), is("bar"));

        when(request.execute(applicationLinkResponseHandler)).thenReturn("lolwut");
        assertThat(proxiedRequest.execute(applicationLinkResponseHandler), is((Object) "lolwut"));

        when(request.executeAndReturn(returningResponseHandler)).thenReturn("monkeytrousers");
        assertThat(proxiedRequest.executeAndReturn(returningResponseHandler), is((Object) "monkeytrousers"));

        proxiedRequest.execute(responseHandler);

        verify(requestTimer, times(4)).start();
        verify(requestTimer, times(4)).stop();
    }

    @Test
    public void otherMethodsAreNotProxied() throws Exception
    {
        when(applicationLink.getDisplayUrl()).thenReturn(new URI("foo"));
        assertThat(proxiedAppLink.getDisplayUrl(), is(new URI("foo")));

        when(requestFactory.getAuthorisationURI()).thenReturn(new URI("bar"));
        assertThat(proxiedAppLink.createAuthenticatedRequestFactory().getAuthorisationURI(), is(new URI("bar")));

        when(request.getHeaders()).thenReturn(singletonMap("simian", singletonList("waistcoat")));
        assertThat(proxiedAppLink.createAuthenticatedRequestFactory().createRequest(GET, "lulwat").getHeaders(), is(singletonMap("simian", singletonList("waistcoat"))));

        verifyZeroInteractions(requestTimer);
    }
}