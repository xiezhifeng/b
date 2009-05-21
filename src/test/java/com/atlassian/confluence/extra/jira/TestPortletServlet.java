package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.importexport.resource.DownloadResourceNotFoundException;
import com.atlassian.confluence.importexport.resource.DownloadResourceReader;
import com.atlassian.confluence.importexport.resource.ExportDownloadResourceManager;
import com.atlassian.confluence.importexport.resource.UnauthorizedDownloadResourceException;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.user.User;
import com.atlassian.user.impl.DefaultUser;
import junit.framework.TestCase;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TestPortletServlet extends TestCase
{
    private PortletServlet portletServlet;

    private ExportDownloadResourceManager exportDownloadResourceManager;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private User authenticatedUser;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        exportDownloadResourceManager = mock(ExportDownloadResourceManager.class);

        portletServlet = new PortletServlet();
        portletServlet.setExportDownloadResourceManager(exportDownloadResourceManager);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        authenticatedUser = new DefaultUser("admin");
        AuthenticatedUserThreadLocal.setUser(authenticatedUser);
    }

    @Override
    protected void tearDown() throws Exception
    {
        AuthenticatedUserThreadLocal.setUser(null);
        super.tearDown();
    }

    public void testIframeContentNotFoundIfNotPermitted() throws Exception
    {
        when(request.getParameter("resource")).thenReturn("foo");
        when(exportDownloadResourceManager.getResourceReader(anyString(), anyString(), anyMap())).thenThrow(new UnauthorizedDownloadResourceException());

        portletServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    public void testIframeContentNotFoundIfDoesNotExists() throws Exception
    {
        when(request.getParameter("resource")).thenReturn("foo");
        when(exportDownloadResourceManager.getResourceReader(anyString(), anyString(), anyMap())).thenThrow(new DownloadResourceNotFoundException());

        portletServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    public void testIframeContentServedAsHtml() throws Exception
    {
        String html = "<html><head><title>foo</title></head><body>bar</body></html>";
        byte[] htmlBytes = html.getBytes("UTF-8");
        DownloadResourceReader downloadResourceReader = mock(DownloadResourceReader.class);
        ByteArrayOutputStream servletOutputStreamBack = new ByteArrayOutputStream();
        ServletOutputStream servletOutputStream = new TestServletOutputStream(servletOutputStreamBack);

        when(request.getParameter("resource")).thenReturn("foo");
        when(exportDownloadResourceManager.getResourceReader(anyString(), anyString(), anyMap())).thenReturn(downloadResourceReader);
        when(downloadResourceReader.getStreamForReading()).thenReturn(new ByteArrayInputStream(htmlBytes));
        when(response.getOutputStream()).thenReturn(servletOutputStream);

        portletServlet.doGet(request, response);

        assertEquals(
                html,
                new String(servletOutputStreamBack.toByteArray(), "UTF-8")
        );

        // The methods below should never be called: See http://developer.atlassian.com/jira/browse/CONFJIRA-150.
        verify(response, never()).setContentLength(anyInt());
        verify(response, never()).setContentType(anyString());
    }

    private static class TestServletOutputStream extends ServletOutputStream
    {
        private final OutputStream outputStream;

        private TestServletOutputStream(OutputStream outputStream)
        {
            this.outputStream = outputStream;
        }

        public void write(int b) throws IOException
        {
            outputStream.write(b);
        }
    }
}
