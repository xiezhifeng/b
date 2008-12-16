var jiraportlet = {
    getParams : function(jiraportletDiv) {
        var param = {};

        jiraportletDiv.children("fieldset.hidden").find("input").each(function () {
            param[this.name] = this.value;
        });

        return param;
    },

    getIframeDocument : function(iFrame) {
        return iFrame.length > 0 ? iFrame.get(0).contentWindow.document : null;
    },

    resizeIframes : function() {
        jQuery("div.jiraportlet > iframe").each(function() {
            var jQueryThis = jQuery(this);
            var params = jiraportlet.getParams(jQueryThis.parent());
            var portletHeight = jQuery.trim(params["portletHeight"]);
            var portletWidth = jQuery.trim(params["portletWidth"]);

            jQueryThis.css("height", portletHeight.length > 0 ? portletHeight : this.contentWindow.document.body.scrollHeight);
            jQueryThis.css("width", portletWidth.length > 0 ? portletWidth : this.contentWindow.document.body.scrollWidth);

            jQuery(this.contentWindow.document.body).css("background-color", "#fff");
        });
    },

    makePortletLinksOpenInParentWindow : function() {
        jQuery("div.jiraportlet > iframe").each(function() {
            var iFrameDocument = jiraportlet.getIframeDocument(jQuery(this));

            if (iFrameDocument != null) {
                jQuery(iFrameDocument).find("a, map > area").each(function() {
                    var jQueryThis = jQuery(this);

                    if (jQueryThis.attr("target") != "_parent") {
                        jQueryThis.attr("target", "_parent");
                    }
                });
            }
        });
    }
};

jQuery(function($) {
    $("div.jiraportlet").each(function() {
        setInterval("jiraportlet.makePortletLinksOpenInParentWindow()", 1000);
        setInterval("jiraportlet.resizeIframes()", 1000); /* Resize every second, because we don't know when all the CSS in an iframe will finish loading */
    });
});