jQuery(function($) {
    var jiraportlet = {
        getParams : function(jiraportletDiv) {
            var param = {};

            jiraportletDiv.children("fieldset.hidden").find("input").each(function () {
                param[this.name] = this.value;
            });

            return param;
        },

        getIframeDocument : function(iFrame) {
            var iframeDocument = iFrame[0].contentWindow || iFrame[0].contentDocument;
            return iframeDocument.document || iframeDocument;
        },

        resizeIframe : function(iFrame) {
            var params = jiraportlet.getParams(iFrame.parent());
            var iframeBody = this.getIframeDocument(iFrame).body;

            // The width affects the iFrame body height, so we set the width first then calculate the height. Rather obscure bug.
            var iframeBodyWidth = Math.max(iframeBody.scrollWidth, iframeBody.clientWidth) + "px";
            var width = $.trim(params["portletWidth"]) || iframeBodyWidth;
            iFrame.css("width", width);

            var iframeBodyHeight = Math.max(iframeBody.scrollHeight, iframeBody.clientHeight) + "px";
            var height = $.trim(params["portletHeight"]) || iframeBodyHeight;
            iFrame.css("height", height);

        },

        makePortletLinksOpenInParentWindow : function(iFrame) {
            $("a, map > area", this.getIframeDocument(iFrame).body).each(function() {
                var link = $(this);
                if (link.attr("target") != "_parent")
                    link.attr("target", "_parent");
            });
        },

        init : function() {
            $("div.jiraportlet > iframe").load(function() {
                var iFrame = $(this);

                $(jiraportlet.getIframeDocument(iFrame).body).css("background-color", "#fff");
                jiraportlet.resizeIframe(iFrame);
                jiraportlet.makePortletLinksOpenInParentWindow(iFrame);
            });
        }
    };

    jiraportlet.init();
});