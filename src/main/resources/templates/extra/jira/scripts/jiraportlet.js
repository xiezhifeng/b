jQuery(function($)
{
    var jiraportlet = {

        getIframe : function(container)
        {
            return container.children('iframe');
        },

        getIframeDocument : function(container)
        {
            var iFrame = this.getIframe(container)[0];
            var iFrameDocument = null;

            if (iFrame.contentDocument)
            {
                // Firefox
                iFrameDocument = iFrame.contentDocument;
            }
            else if (iFrame.contentWindow)
            {
                // IE
                iFrameDocument = iFrame.contentWindow.document;
            }
            else if (iFrame.document)
            {
                iFrameDocument = iFrame.document;
            }

            return $(iFrameDocument);
        },

        prepareIframe :function(container)
        {
            var iFrame = this.getIframe(container);
            var iFrameDocument = this.getIframeDocument(container)

            iFrame.attr("doc", iFrameDocument);
            // if iframe document is not created
            if (!iFrame.attr("doc"))
            {
                throw "Could not find document object for inline frame: " + iFrame[0];
            }

            iFrameDocument[0].open();
            iFrameDocument[0].close();

            return iFrame;
        },

        showPortletInIframe : function(container)
        {
            var portletHtml = container.children('input').attr("value");
            var iFrame = this.prepareIframe(container);
            var iFrameDocument = this.getIframeDocument(container);

            iFrame.attr("scrolling", "no");  // remove scrolling
            iFrame.attr("marginwidth", "0");
            iFrame.attr("marginheight", "0");
            iFrame.attr("vspace", "0");
            iFrame.attr("hspace", "0");

            iFrame.css("border", "0");  // remove border
            iFrame.css("overflow", "visible");
            iFrame.css("width", "100%");

            container.css("backgroundColor", "#fff"); // set to white background

            var iFrameBody = iFrameDocument.find("body");

            iFrameBody.html("<div>" + portletHtml + "</div>");
            iFrameBody.css("backgroundColor", "#fff");
            iFrameBody.css("width", "100%");

            this.fixAnchorTarget(iFrameBody);
        },

        fixAnchorTarget : function(iFrameBody)
        {
            var anchors = iFrameBody[0].getElementsByTagName("a");
            for (var i = 0; i < anchors.length; i++)
                anchors[i].target = "_parent";
        },

        setPortletIframeHeight : function(container)
        {
            var iFrame = this.getIframe(container);
            var iFrameDocumentBody = this.getIframeDocument(container).find("body")[0];

            try
            {
                iFrame.css("height", iFrameDocumentBody.scrollHeight + "px");
            }
            catch (err)
            {
                iFrame.css("height", iFrameDocumentBody.offsetHeight + "px");
            }
        },

        initPortlet : function(container)
        {
            this.showPortletInIframe(container);
            this.setPortletIframeHeight(container);
        }
    };

    $("div.jiraportlet").each(function()
    {
        jiraportlet.initPortlet($(this));
    });
});