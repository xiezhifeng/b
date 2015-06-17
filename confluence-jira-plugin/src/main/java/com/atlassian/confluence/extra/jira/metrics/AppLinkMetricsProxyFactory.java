package com.atlassian.confluence.extra.jira.metrics;

import java.lang.reflect.Method;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;

import com.google.common.base.Function;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

public class AppLinkMetricsProxyFactory
{
    public static ApplicationLink proxyApplicationLink(final JiraIssuesMacroRenderEvent.Builder metrics, ApplicationLink applicationLink)
            throws Exception
    {
        return proxyMethodsMatchingReturnType(applicationLink, ApplicationLinkRequestFactory.class, new Function<ApplicationLinkRequestFactory, ApplicationLinkRequestFactory>()
        {
            @Override
            public ApplicationLinkRequestFactory apply(final ApplicationLinkRequestFactory input)
            {
                return metricsProxy(input, metrics);
            }
        });
    }

    private static ApplicationLinkRequestFactory metricsProxy(ApplicationLinkRequestFactory appLinkRequestFactory, final JiraIssuesMacroRenderEvent.Builder metrics)
    {
        return proxyMethodsMatchingReturnType(appLinkRequestFactory, ApplicationLinkRequest.class, new Function<ApplicationLinkRequest, ApplicationLinkRequest>()
        {
            @Override
            public ApplicationLinkRequest apply(final ApplicationLinkRequest input)
            {
                return proxyAppLinkRequest(input, metrics);
            }
        });
    }

    public static ApplicationLinkRequest proxyAppLinkRequest(ApplicationLinkRequest appLinkRequest, final JiraIssuesMacroRenderEvent.Builder metrics)
    {
        final ProxyFactory proxyFactory = new ProxyFactory(appLinkRequest);

        final NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(new AppLinkRequestMetricsInterceptor(metrics));
        advisor.setMappedName("execute*");
        proxyFactory.addAdvisor(advisor);

        return (ApplicationLinkRequest) proxyFactory.getProxy();
    }

    private static class ReturnTypeAdvisor extends StaticMethodMatcherPointcutAdvisor
    {
        private final Class<?> matchedReturnType;

        public ReturnTypeAdvisor(final Class<?> matchedReturnType, final Advice advice)
        {
            super(advice);
            this.matchedReturnType = matchedReturnType;
        }

        @Override
        public boolean matches(final Method method, final Class targetClass)
        {
            return matchedReturnType.equals(method.getReturnType());
        }
    }

    private static class AppLinkRequestMetricsInterceptor implements MethodInterceptor
    {
        private final JiraIssuesMacroRenderEvent.Builder metrics;

        public AppLinkRequestMetricsInterceptor(final JiraIssuesMacroRenderEvent.Builder metrics)
        {
            this.metrics = metrics;
        }

        @Override
        public Object invoke(final MethodInvocation invocation) throws Throwable
        {
            metrics.appLinkRequestStart();
            try
            {
                return invocation.proceed();
            }
            finally
            {
                metrics.appLinkRequestFinish();
            }
        }
    }

    @SuppressWarnings ("unchecked")
    private static <T, R> T proxyMethodsMatchingReturnType(final T target, Class<R> returnType, final Function<R, R> returnValueProxyCreator)
    {
        final ProxyFactory proxyFactory = new ProxyFactory(target);

        proxyFactory.addAdvisor(new ReturnTypeAdvisor(returnType, new MethodInterceptor()
        {
            @Override
            public Object invoke(final MethodInvocation invocation) throws Throwable
            {
                return returnValueProxyCreator.apply((R) invocation.proceed());
            }
        }));

        return (T) proxyFactory.getProxy();
    }
}
