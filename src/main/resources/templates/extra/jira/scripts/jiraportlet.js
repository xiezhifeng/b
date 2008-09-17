var jiraportlet = {

    getIframe : function(macroId)
    {
        return $('jiraportlet_iframe_' + macroId);
    },

    getIframeDocument : function(iFrameElement)
    {
        var iFrameDocument = null;

        if (iFrameElement.contentDocument)
        {
            // Firefox
            iFrameDocument = iFrameElement.contentDocument;
        }
        else if (iFrameElement.contentWindow)
        {
            // IE
            iFrameDocument = iFrameElement.contentWindow.document;
        }
        else if (iFrameElement.document)
        {
            iFrameDocument = iFrameElement.document;
        }

        return iFrameDocument;
    },

    prepareIframe :function(macroId)
    {
        var iframe = this.getIframe(macroId);
        iframe.doc = this.getIframeDocument(iframe);

        // if iframe document is not created
        if (iframe.doc == null)
        {
            throw "Document cannot be created.";
        }

        iframe.doc.open();
        iframe.doc.close();

        return iframe;
    },

    showPortletInIframe : function(macroId)
    {
        var portletHtml = $('jiraportlet_content_' + macroId).value;
        var iframeDiv = $('jiraportlet_iframe_container_' + macroId);
        var iframe = this.prepareIframe(macroId);

        iframe.scrolling = "no";  // remove scrolling
        iframe.marginwidth = "0";
        iframe.marginheight = "0";
        iframe.vspace = "0";
        iframe.hspace = "0";
        iframe.style.border = "0";  // remove border
        iframe.style.overflow = "visible";
        iframe.style.width = "100%";
        iframe.doc.body.style.backgroundColor = "#fff"; // set to white background
        iframeDiv.style.backgroundColor = "#fff"; // set to white background

        var iframeportlet = iframe.doc.createElement("div");

        iframeportlet.style.width = "100%";
        iframeportlet.innerHTML = portletHtml;
        
        iframe.doc.body.appendChild(iframeportlet);

        this.fixAnchorTarget(iframeportlet);
    },

    fixAnchorTarget : function(portlet)
    {
        var anchors = portlet.getElementsByTagName("a");
        for (var i = 0; i < anchors.length; i++)
        {
            anchors[i].target = "_parent";
        }
    },

    setPortletIframeHeight : function(macroId)
    {
        var iFrame = this.getIframe(macroId);
        var iFrameDocument = this.getIframeDocument(iFrame);
        var iFrameDocumentBody = iFrameDocument.body;

        try
        {
            iFrame.style.height = iFrameDocumentBody.scrollHeight + "px";
        }
        catch (err)
        {
            iFrame.style.height = iFrameDocumentBody.offsetHeight + "px";
        }
    }
}