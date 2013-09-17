package com.atlassian.confluence.plugins.jiracharts;

import java.util.Map;

import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;

public interface JQLValidator
{
    JQLValidationResult doValidate(Map<String, String> parameters) throws MacroExecutionException;
}
