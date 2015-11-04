package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.Renderer;
import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.user.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.lang.BooleanUtils;

/**
 * Servlet that processes render requests from the refresh event
 */
public final class RefreshRenderer extends HttpServlet
{

    /**
     * 
     */
    private static final long serialVersionUID = -7180537531857451119L;

    private ContentEntityManager contentEntityManager;
    private PermissionManager permissionManager;
    private Renderer viewRenderer;
    private I18NBeanFactory i18NBeanFactory;

    public RefreshRenderer(Renderer viewRenderer, ContentEntityManager contentEntityManager, PermissionManager permissionManager, I18NBeanFactory i18nBeanFactory)
    {
        this.viewRenderer = viewRenderer;
        this.contentEntityManager = contentEntityManager;
        this.permissionManager = permissionManager;
        this.i18NBeanFactory = i18nBeanFactory;
    }

    private String convertPageWikiToHtml(long id, String wiki, String columnName, String order, boolean clearCache) throws ServletException
    {
        ConversionContext conversionContext = null;
        if (id == -1)
        {
            // the default welcome page is detected
            conversionContext = new DefaultConversionContext(new PageContext());
        }
        else
        {
            ContentEntityObject ceo = contentEntityManager.getById(id);
            if (ceo != null)
            {
                assertCanView(ceo);
                conversionContext = new DefaultConversionContext(ceo.toPageContext());
            }
            else
            { // this case is unlikely but possible
                conversionContext = new DefaultConversionContext(new PageContext());
            }
        }
        // conversionContext should be available now
        conversionContext.setProperty(DefaultJiraCacheManager.PARAM_CLEAR_CACHE, clearCache);
        conversionContext.setProperty("orderColumnName", columnName);
        conversionContext.setProperty("order", order);
        conversionContext.setProperty(JiraIssuesMacro.PARAM_PLACEHOLDER, Boolean.FALSE);
        return viewRenderer.render(wiki, conversionContext);
    }

    private void assertCanView(ContentEntityObject ceo) throws ServletException
    {
        User user = AuthenticatedUserThreadLocal.get();
        if (!permissionManager.hasPermission(user, Permission.VIEW, ceo))
            throw new ServletException(i18NBeanFactory.getI18NBean().getText("jiraissues.error.notpermitted"));
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException
    {
        String pageIdString = httpServletRequest.getParameter("pageId");
        String wikiMarkup = httpServletRequest.getParameter("wikiMarkup");
        String columnName = httpServletRequest.getParameter("columnName");
        String order = httpServletRequest.getParameter("order");
        // this parameter is to explicitly ask the render to use the cached data. Normally, the cache will
        // be cleared if user clicks on the Refresh button. However, cache will be used if it is from the asynchronous rendering
        boolean clearCache = BooleanUtils.toBoolean(httpServletRequest.getParameter("clearCache"));

        long pageId = Long.parseLong(pageIdString);

        String result = convertPageWikiToHtml(pageId, wikiMarkup, columnName, order, clearCache);

        httpServletResponse.setContentType("text/html");

        final PrintWriter printWriter = httpServletResponse.getWriter();
        printWriter.print(result);
        printWriter.flush();
    }
}
