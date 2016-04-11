var allTestFiles = [];
var TEST_REGEXP = /(.test)\.js$/i;

var pathToModule = function(path) {
    return path.replace(/^\/base\//, '').replace(/\.js$/, '');
};

Object.keys(window.__karma__.files).forEach(function(file) {
    if (TEST_REGEXP.test(file)) {
        // Normalize paths to RequireJS module names.
        allTestFiles.push(pathToModule(file));
    }
});

define('ajs', function() {
    return window.AJS;
});

require.config({
    'baseUrl': '/base',
    'paths': {
        jquery: 'bower_components/jquery/jquery',
        underscore: 'bower_components/underscore/underscore',
        backbone: 'bower_components/backbone/backbone',
        text: 'node_modules/requirejs-text/text',

        'fixtures': 'src/test/resources/fixtures',

        'confluence/jim': 'src/main/resources',
    },
    shim: {
        backbone: {
            deps: ['underscore', 'jquery'],
            exports: 'Backbone'
        },
        underscore: {
            exports: '_'
        }
    }
});

require(allTestFiles, function() {
    window.__karma__.start();
});
