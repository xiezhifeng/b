package com.atlassian.confluence.plugins.jiracharts;

import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;

import java.util.Map;

public interface JQLValidator
{
    JQLValidationResult doValidate(Map<String, String> parameters, boolean isVerifyChartSupported) throws MacroExecutionException;
}
