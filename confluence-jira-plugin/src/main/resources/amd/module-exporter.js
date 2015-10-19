/**
 * This module is cloned from CONF master. Because our plugin want to support old version CONF
 * and also want to export several AMD modules as global variable to be compatible with legacy code.
 */
define('confluence/jim/amd/module-exporter', [], function() {
    'use strict';

    var ModuleExporter = {};

    /**
     * Get/set the value at a compound namespace, gracefully adding values where missing.
     * @param {string} namespace
     * @param {Object} [value={}]
     * @param {Object} [context=window]
     * @deprecated please create AMD modules in the appropriate place in the /webapp/static/ folder!
     *             Read {@link https://extranet.atlassian.com/display/JIRADEV/JIRA+JavaScript+Documentation} for more.
     */
    ModuleExporter.namespace = function (namespace, value, context) {
        var names = namespace.split(".");
        context = context || window;
        for (var i = 0, n = names.length - 1; i < n; i++) {
            var x = context[names[i]];
            context = (x != null) ? x : context[names[i]] = {};
        }
        if (context[names[i]]) {
            if (window.console && window.console.warn) {
                window.console.warn('Value of "' + namespace + '" was overridden');
            }
        }
        context[names[i]] = value || context[names[i]] || {};
        return context[names[i]];
    };

    /**
     * Wrapper function for AMD "require" that handles common error scenarios. This function is required in unit tests
     * when requiring a module in the same file where the module is defined (which usually throws an exception).
     *
     * @method safeRequire
     * @param {String} moduleName
     * @return {Object|undefined}
     */
    ModuleExporter.safeRequire = function (moduleName, cb) {
        if (define && define.amd === undefined) {
            var module = require(moduleName);
            if (cb) {
                cb(module);
            }
        }
    };

    ModuleExporter.exportModuleAsGlobal = function (moduleName, namespace, cb) {
        ModuleExporter.safeRequire(moduleName, function(module) {
            ModuleExporter.namespace(namespace, module);
            if (cb) {
                cb(module);
            }
        });
    };

    return ModuleExporter;
});