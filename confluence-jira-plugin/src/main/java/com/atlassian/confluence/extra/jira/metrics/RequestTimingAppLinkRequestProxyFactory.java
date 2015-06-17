package com.atlassian.confluence.extra.jira.metrics;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.event.ApplicationLinkEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventListenerRegistrar;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

public class RequestTimingAppLinkRequestProxyFactory implements InitializingBean, DisposableBean
{
    private final EventListenerRegistrar eventListenerRegistrar;

    // This should really be an atlassian-cache Cache, but at time of writing this plugin only used atlassian-cache 0.1,
    // which is awful. When that gets upgraded to something modern (i.e. atlassian-cache 2.x+), then make this a
    // local cache with proper loading semantics.
    private final Map<ApplicationId, ApplicationLink> proxiedAppLinkCache = new ConcurrentHashMap<ApplicationId, ApplicationLink>();

    public RequestTimingAppLinkRequestProxyFactory(final EventListenerRegistrar eventListenerRegistrar)
    {
        this.eventListenerRegistrar = requireNonNull(eventListenerRegistrar);
    }

    @Override
    public void destroy() throws Exception
    {
        eventListenerRegistrar.unregister(this);
        proxiedAppLinkCache.clear();
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        eventListenerRegistrar.register(this);
    }

    @EventListener
    public void onAppLinkEvent(ApplicationLinkEvent event)
    {
        proxiedAppLinkCache.clear();
    }

    public ApplicationLink getProxiedAppLink(final Supplier<JiraIssuesMacroMetrics> metricsSupplier, ApplicationLink appLink)
    {
        final ApplicationLink cached = proxiedAppLinkCache.get(appLink.getId());
        if (cached != null)
        {
            return cached;
        }
        else
        {
            final ApplicationLink proxiedAppLink = proxyApplicationLink(metricsSupplier, appLink);
            proxiedAppLinkCache.put(appLink.getId(), proxiedAppLink);
            return proxiedAppLink;
        }
    }

    public static ApplicationLink proxyApplicationLink(final Supplier<JiraIssuesMacroMetrics> metricsSupplier, ApplicationLink applicationLink)
    {
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
                        return proxy(request, requestExecutionAdvisor(metricsSupplier.get().appLinkRequestTimer()));
                    }
                });
            }
        });
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
