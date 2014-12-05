(function($, _){

    function goSamples() {
        // save the body for goViewSource() before we modify it
        window.bodyHTML = document.body.innerHTML;
        window.bodyHTML = window.bodyHTML.replace(/</g, "&lt;");
        window.bodyHTML = window.bodyHTML.replace(/>/g, "&gt;");

        // look for links to API documentation and convert them
        _traverseDOM(document);

        // add list of samples for navigation
        var menu = document.createElement("div");
        menu.id = "menu";
        menu.innerHTML = myMenu;
        document.body.insertBefore(menu, document.body.firstChild);

        // when the page loads, change the class of navigation LI's
        var url = window.location.href;
        var lindex = url.lastIndexOf('/');
        url = url.slice(lindex+1).toLowerCase();  // include "/" to avoid matching prefixes
        var lis = document.getElementById("sections").getElementsByTagName("li");
        var l = lis.length;
        var listed = false;
        for (var i = 0; i < l; i++) {
            var li = lis[i].parentNode;
            if (!li.href) continue;
            var lowerhref = li.href.toLowerCase();
            if (lowerhref.indexOf(url) !== -1) {
                lis[i].className = "selected";
                listed = true;
            }
        }
        if (!listed) {
            lis[lis.length -1].className = "selected";
        }

    }

// Traverse the whole document and replace <a>TYPENAME</a> with:
//    <a href="../api/symbols/TYPENAME.html">TYPENAME</a>
// and <a>TYPENAME.MEMBERNAME</a> with:
//    <a href="../api/symbols/TYPENAME.html#MEMBERNAME">TYPENAME.MEMBERNAME</a>
    function _traverseDOM(node) {
        if (node.nodeType === 1 && node.nodeName === "A" && !node.getAttribute("href")) {
            var text = node.innerHTML.split(".");
            if (text.length === 1) {
                node.setAttribute("href", "../api/symbols/" + text[0] + ".html");
                node.setAttribute("target", "api");
            } else if (text.length === 2) {
                node.setAttribute("href", "../api/symbols/" + text[0] + ".html" + "#" + text[1]);
                node.setAttribute("target", "api");
            } else {
                alert("Unknown API reference: " + node.innerHTML);
            }
        }
        for (var i = 0; i < node.childNodes.length; i++) {
            _traverseDOM(node.childNodes[i]);
        }
    }

})(AJS.$, window._);


