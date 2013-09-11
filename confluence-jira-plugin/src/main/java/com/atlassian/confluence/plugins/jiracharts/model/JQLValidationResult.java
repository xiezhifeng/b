package com.atlassian.confluence.plugins.jiracharts.model;

import java.util.Collections;
import java.util.List;

import com.atlassian.confluence.macro.MacroExecutionException;

/**
 * Contain the validation result after call search API in JIRA
 * @author duy.luong
 *
 */
public class JQLValidationResult {
	private List<String> errorMgs;
	
	private String authUrl;
	
	private int issueCount;
	
	private MacroExecutionException exception;

	public List<String> getErrorMgs() {
		return errorMgs;
	}
	
	public JQLValidationResult(){
		setAuthUrl("");
		setErrorMgs(Collections.EMPTY_LIST);
	}

	public void setErrorMgs(List<String> errorMgs) {
		if (errorMgs == null){
			errorMgs = Collections.EMPTY_LIST;
		}
		this.errorMgs = errorMgs;
	}

	public String getAuthUrl() {
		return authUrl;
	}

	public void setAuthUrl(String oAuthUrl) {
		this.authUrl = oAuthUrl;
	}
	
	public boolean isValidJQL(){
		return getErrorMgs().size() == 0;
	}
	
	public boolean isNeedOAuth(){
		String authUrl = getAuthUrl();
		return authUrl != null && !"".equals(authUrl);
	}

    public MacroExecutionException getException() {
        return exception;
    }

    public void setException(MacroExecutionException exception) {
        this.exception = exception;
    }

    public int getIssueCount() {
        return issueCount;
    }

    public void setIssueCount(int issueCount) {
        this.issueCount = issueCount;
    }
}
