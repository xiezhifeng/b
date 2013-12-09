package it.webdriver.com.atlassian.confluence.helper;

import com.google.common.base.Function;

public class FunctionExecutor<F>
{
    private final int retryTime;
    private final Throwable wrappedException;
    private final Function function;
    
    public FunctionExecutor(int retryTime, Throwable wrappedException, Function function) {
        super();
        this.retryTime = retryTime;
        this.wrappedException = wrappedException;
        this.function = function;
    }
    
    
    
}
