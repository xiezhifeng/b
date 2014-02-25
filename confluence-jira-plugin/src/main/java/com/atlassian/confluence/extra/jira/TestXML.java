package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.cache.CompressingStringCache;
import com.atlassian.confluence.extra.jira.cache.SimpleStringCache;
import com.atlassian.confluence.extra.jira.cache.StringCache;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.core.util.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class TestXML extends HttpServlet
{

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    {
        try {
            response.getWriter().write(FileUtils.getResourceContent("/Users/ndang/Downloads/SearchRequest.xml"));
        } catch (Exception e) {
            System.out.print(e);
        }
    }

}
