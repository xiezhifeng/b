package com.atlassian.confluence.plugins.jiracharts.render;

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

/**
 * Servlet that processes render requests from the refresh event
 */
public final class TwoDimensionalShowMoreRenderer extends HttpServlet
{

    private ContentEntityManager contentEntityManager;

    private PermissionManager permissionManager;

    private Renderer viewRenderer;

    private I18NBeanFactory i18NBeanFactory;

    public void setViewRenderer(Renderer viewRenderer)
    {
        this.viewRenderer = viewRenderer;
    }

    public void setContentEntityManager(ContentEntityManager contentEntityManager)
    {
        this.contentEntityManager = contentEntityManager;
    }

    public void setPermissionManager(PermissionManager permissionManager)
    {
        this.permissionManager = permissionManager;
    }

    public void setI18NBeanFactory(I18NBeanFactory i18nBeanFactory)
    {
        i18NBeanFactory = i18nBeanFactory;
    }

    private String convertPageWikiToHtml(long id, String wiki, boolean isShowMore) throws ServletException
    {
        ConversionContext conversionContext;
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
            else { // this case is unlikely but possible
                conversionContext = new DefaultConversionContext(new PageContext());
            }
        }
        conversionContext.setProperty("isShowMore", isShowMore);
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
        boolean isShowMore = Boolean.valueOf(httpServletRequest.getParameter("isShowMore"));
        long pageId = Long.parseLong(pageIdString);
        String result = convertPageWikiToHtml(pageId, wikiMarkup, isShowMore);
        httpServletResponse.setContentType("text/html");
        final PrintWriter printWriter = httpServletResponse.getWriter();
        printWriter.print(result);
        printWriter.flush();
    }
}
