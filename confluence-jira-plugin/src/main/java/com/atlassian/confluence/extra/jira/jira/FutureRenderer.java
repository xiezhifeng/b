/*
Copyright 2008 Atlassian

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.Renderer;
import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.WikiStyleRenderer;
import com.atlassian.user.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet that processes render requests from the future macro
 */
public final class FutureRenderer extends HttpServlet
{

    private ContentEntityManager contentEntityManager;
    private PermissionManager permissionManager;
    private Renderer viewRenderer;

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

    private String convertPageWikiToHtmlForFuture(long id, String wiki) throws ServletException
    {
        ContentEntityObject ceo = contentEntityManager.getById(id);
        assertCanView(ceo);
        ConversionContext conversionContext = new DefaultConversionContext(ceo.toPageContext());
        conversionContext.setProperty("forceRender", true);
        return viewRenderer.render(wiki, conversionContext);
    }

    private void assertCanView(ContentEntityObject ceo) throws ServletException
    {
        User user = AuthenticatedUserThreadLocal.getUser();
        if (!permissionManager.hasPermission(user, Permission.VIEW, ceo))
            throw new ServletException("You're not allowed to view that page, or it does not exist.");
    }

    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException
    {
        String pageIdString = httpServletRequest.getParameter("pageId");
        String wikiMarkup = httpServletRequest.getParameter("wikiMarkup");

        long pageId = Long.parseLong(pageIdString);

        String result = convertPageWikiToHtmlForFuture(pageId, wikiMarkup);

        httpServletResponse.setContentType("text/html");

        final PrintWriter printWriter = httpServletResponse.getWriter();
        printWriter.print(result);
        printWriter.flush();
    }
}
