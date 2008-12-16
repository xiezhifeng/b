package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.security.GateKeeper;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.importexport.resource.ExportDownloadResourceManager;
import com.atlassian.confluence.importexport.resource.DownloadResourceReader;
import com.atlassian.confluence.importexport.resource.UnauthorizedDownloadResourceException;
import com.atlassian.confluence.importexport.resource.DownloadResourceNotFoundException;
import com.atlassian.user.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class PortletServlet extends HttpServlet
{
    private ExportDownloadResourceManager exportDownloadResourceManager;

    public void setExportDownloadResourceManager(ExportDownloadResourceManager exportDownloadResourceManager)
    {
        this.exportDownloadResourceManager = exportDownloadResourceManager;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String resourcePath = StringUtils.defaultString(request.getParameter("resource"));
        User user = AuthenticatedUserThreadLocal.getUser();
        String userName = null == user ? null : user.getName();
        InputStream in = null;
        OutputStream out = null;

        try
        {
            DownloadResourceReader downloadResourceReader =
                    exportDownloadResourceManager.getResourceReader(userName, resourcePath, Collections.emptyMap());

            in = downloadResourceReader.getStreamForReading();
            out = response.getOutputStream();

            IOUtils.copy(in, out);

            response.setContentLength((int) downloadResourceReader.getContentLength());
            response.setContentType("text/html"); /* Inline frame content */
        }
        catch (DownloadResourceNotFoundException drnfe)
        {
            /* Just tell remote client that resource does not exist */
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        catch (UnauthorizedDownloadResourceException udre)
        {
            /* Just tell remote client that resource does not exist */
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        finally
        {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }
}
