package com.atlassian.confluence.extra.jira.metrics;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.google.common.base.Function;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class RequestTimingAppLinkRequestProxyFactory
{
    public static ApplicationLink proxyApplicationLink(final JiraIssuesMacroRenderEvent.Builder metrics, Callable<ApplicationLink> applicationLinkSupplier) throws Exception
    {
        final ApplicationLink applicationLink = fetchAppLink(applicationLinkSupplier, metrics.applinkResolutionTimer());

        return proxyMethodsMatchingReturnType(applicationLink, ApplicationLinkRequestFactory.class, new Function<ApplicationLinkRequestFactory, ApplicationLinkRequestFactory>()
        {
            @Override
            public ApplicationLinkRequestFactory apply(final ApplicationLinkRequestFactory requestFactory)
            {
                return proxyMethodsMatchingReturnType(requestFactory, ApplicationLinkRequest.class, new Function<ApplicationLinkRequest, ApplicationLinkRequest>()
                {
                    @Override
                    public ApplicationLinkRequest apply(final ApplicationLinkRequest request)
                    {
                        return proxy(request, requestExecutionAdvisor(metrics.appLinkRequestTimer()));
                    }
                });
            }
        });
    }

    private static ApplicationLink fetchAppLink(final Callable<ApplicationLink> applicationLinkSupplier, final Timer timer) throws Exception
    {
        timer.start();
        try
        {
            return applicationLinkSupplier.call();
        }
        finally
        {
            timer.stop();
        }
    }

    private static NameMatchMethodPointcutAdvisor requestExecutionAdvisor(final Timer timer)
    {
        final NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(requestExecutionAdvice(timer));
        advisor.setMappedName("execute*");
        return advisor;
    }

    private static Advisor returnTypeAdvisor(final Class<?> matchedReturnType, final Advice advice)
    {
        return new StaticMethodMatcherPointcutAdvisor(advice)
        {
            @Override
            public boolean matches(final Method method, final Class targetClass)
            {
                return matchedReturnType.equals(method.getReturnType());
            }
        };
    }

    private static Advice requestExecutionAdvice(final Timer timer)
    {
        return new MethodInterceptor()
        {
            @Override
            public Object invoke(final MethodInvocation invocation) throws Throwable
            {
                timer.start();
                try
                {
                    return invocation.proceed();
                }
                finally
                {
                    timer.stop();
                }
            }
        };
    }

    private static <T, R> T proxyMethodsMatchingReturnType(final T target, Class<R> returnType, final Function<R, R> returnValueProxyCreator)
    {
        return proxy(target, returnTypeAdvisor(returnType, new MethodInterceptor()
        {
            @Override
            public Object invoke(final MethodInvocation invocation) throws Throwable
            {
                //noinspection unchecked
                final R returnValue = (R) invocation.proceed();
                return returnValueProxyCreator.apply(returnValue);
            }
        }));
    }

    private static <T> T proxy(final T target, final Advisor advisor)
    {
        final ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.addAdvisor(advisor);
        //noinspection unchecked
        return (T) proxyFactory.getProxy();
    }
}
