/*Support function to declare namespace quickly*/
(function () {
    var namespace = function(strNs){
        var parts = strNs.split('.');
        var parent = window;
        var i;

        var len = parts.length;
        for (i = 0; i < len; i += 1) {
            // create a property if it doesn't exist
            if (typeof parent[parts[i]] === "undefined") {
                parent[parts[i]] = {};
            }
            parent = parent[parts[i]];
        }
        return parent;
    };

    window.Confluence = window.Confluence || {};
    window.Confluence.getContextPath = function() {
        return '/confluence';
    };

    window.AJS = window.AJS || {};
    window.AJS.contextPath = function() {
        return 'confluence/';
    };

    window.AJS.Meta = window.AJS.Meta || {};
    window.AJS.Meta.getNumber = function() {
        return 1;
    };

    jQuery.fn.disable = function() {};
    jQuery.fn.enable = function() {};
}());

