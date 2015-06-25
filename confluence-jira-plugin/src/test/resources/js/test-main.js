/*
 * RequireJS setup for Karma
 * See: http://karma-runner.github.io/0.10/plus/requirejs.html
 *
 * This allows Karma to map module paths (/confluence/ic/component) to the appropriate file names
 */

var allTestFiles = [
    'confluence/jim/amd/module-exporter'
];
var TEST_REGEXP = /(-test)\.js$/i;
var pathToModule = function(path) {
    return path.replace(/^\/base\//, '').replace(/\.js$/, '');
};

Object.keys(window.__karma__.files).forEach(function (file) {
    if (TEST_REGEXP.test(file)) {
        // Normalize paths to RequireJS module names.
        allTestFiles.push(pathToModule(file));
    }
});

requirejs.config({
    baseUrl: '/base',

    paths: {
        jquery: 'bower_components/jquery/jquery.js',
        underscore: 'bower_components/underscore/underscore',
        backbone: 'bower_components/backbone/backbone',
        ajs: 'bower_components/aui/src/js/aui',

        // Testing modules
        //server-mock': 'src/test/resources/js/rest/server-mock',
        'confluence/jim/template': 'src/main/resources/amd/template',
        'confluence/jim': 'src/main/resources',
    },

    shim: {
        backbone: {
            deps: ['underscore', 'jquery'],
            exports: 'Backbone'
        },
        underscore: {
            exports: '_'
        },
        ajs: {
            deps: [
                'jquery'
            ],
            exports: 'AJS'
        }
    }
});

// ask Require.js to load these files (all our tests)
// start test run, once Require.js is done
require(allTestFiles, function(){
    window.__karma__.start();
});