// Karma configuration
var dependencies = [
    // essential libraries
    'bower_components/jquery/jquery.js',
    'bower_components/underscore/underscore.js',
    'bower_components/backbone/backbone.js',

    {pattern: 'src/test/resources/js/mocks/*.js'},


    // necessary aui vendors
    'bower_components/aui/src/js-vendor/jquery/plugins/jquery.select2.js',
    'bower_components/aui/src/js/polyfills/custom-event.js',

    'bower_components/aui/src/js/aui/internal/animation.js',
    'bower_components/aui/src/js/aui.js',
    'bower_components/aui/src/js/format.js',
    'bower_components/aui/src/js/template.js',

    'bower_components/aui/src/js/keyCode.js',
    'bower_components/aui/src/js/aui/internal/widget.js',
    'bower_components/aui/src/js/blanket.js',
    'bower_components/aui/src/js/layer.js',
    'bower_components/aui/src/js/focus-manager.js',
    'bower_components/aui/src/js/layer-manager.js',
    'bower_components/aui/src/js/layer-manager-global.js',

    'bower_components/aui/src/js/aui-select2.js',
    'bower_components/aui/src/js/dialog2.js',
    'bower_components/aui/src/js/tabs.js',

    // generated soy template
    'target/qunit/dependencies/js/soyutils-min.js',
    'target/qunit/soy/**/*.js',

    //{pattern: 'target/qunit/soy/**/*.soy.js', included: true}, // compiled IC and AUI templates
    {pattern: 'src/main/resources/**/*.js', included: false},
    {pattern: 'src/test/resources/js/rest/*.js', included: false},
    {pattern: 'src/test/resources/js/pageobjects/*.js'},
    'src/test/resources/js/test-main.js'

    // Modules required by tests.
    //{pattern: 'confluence-test/confluence-qunit-test/src/main/resources/mock/mock-range.js', included: false},
    //{pattern: 'node_modules/requirejs-text/text.js', included: false},

    // HTML Fixtures
    //{pattern: 'confluence-frontend-tools/test/**/*_fixture.html', included: false }
];

module.exports = function(config) {
    config.set({

        // base path, that will be used to resolve files and exclude
        basePath: '',


        // frameworks to use
        frameworks: ['requirejs', 'qunit', 'sinon'],


        // list of files / patterns to load in the browser
        files: dependencies.concat([
            { pattern: 'src/test/resources/js/qunit/*-test.js', included: false }
        ]),

        // list of files to exclude
        exclude: [
        ],


        // test results reporter to use
        // possible values: 'dots', 'progress', 'junit', 'growl', 'coverage'
        reporters: ['mocha'],
        junitReporter: {
            outputFile: 'target/surefire-reports/karma-results.xml',
            suite: ''
        },

        // configuration for Istanbul code coverage tool
        preprocessors: {
            'src/main/resources/**/*.js': 'coverage'
        },

        coverageReporter: {
            reporters: [
                { type: 'html', subdir: 'html' },
                { type: 'lcov', subdir: 'lcov' },
                { type: 'text-summary' }
            ]
        },
        // web server port
        port: 9876,


        // enable / disable colors in the output (reporters and logs)
        colors: true,


        // level of logging
        // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
        logLevel: config.LOG_INFO,


        // enable / disable watching file and executing tests whenever any file changes
        autoWatch: true,


        // Start these browsers, currently available:
        // - Chrome
        // - ChromeCanary
        // - Firefox
        // - Opera
        // - Safari (only Mac)
        // - PhantomJS
        // - IE (only Windows)
        // browsers: ['Chrome', 'Safari', 'Firefox', 'Opera', 'IE11 - Win7', 'IE10 - Win7', 'IE9 - Win7'],
        browsers: ['PhantomJS'],


        // If browser does not capture in given timeout [ms], kill it
        captureTimeout: 60000,


        // Continuous Integration mode
        // if true, it capture browsers, run tests and exit
        singleRun: true
    });
};