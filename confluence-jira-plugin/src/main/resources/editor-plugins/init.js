// Executation code
require([
    'ajs',
    'confluence/jim/editor-plugins/jira-paste-link'
], function(
    AJS,
    jiraPasteLink) {

    AJS.bind('init.rte', function() {
        window.tinymce.plugins.Autoconvert.autoConvert.addHandler(jiraPasteLink.pasteHandler);
    });
});
