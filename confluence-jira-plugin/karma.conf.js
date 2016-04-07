var productionFilesPath = 'src/main/resources/**/*.js';
var testFilesPath = 'src/test/resources/js/**/*.test.js';

var dependencies = [
    // Globals
    'bower_components/jquery/jquery.js',
    'bower_components/underscore/underscore.js',
    'bower_components/backbone/backbone.js',
    'src/test/resources/js/mock/global-vars.js',

    // mock code
    {pattern: 'src/test/resources/js/mock/*.js', included: false},

    // HTML Fixtures
    {pattern: 'src/test/resources/fixtures/*.html', included: false },

    // production code
    {pattern: productionFilesPath, included: false},

    // Modules required by tests.
    {pattern: 'node_modules/requirejs-text/text.js', included: false}
];

dependencies.push('test-main.js');

module.exports = function(config) {
    config.set({
        frameworks: [
            'requirejs',
            'qunit',
            'sinon'
        ],
        files: dependencies.concat([
            { pattern: testFilesPath, included: false }
        ]),

        reportSlowerThan: 100,
        reporters: ['progress', 'junit', 'coverage'],
        colors: true,
        coverageReporter: {
            reporters: [
                { type: 'html', subdir: 'html' },
                { type: 'lcov', subdir: 'lcov' }
            ]
        },
        browsers: ['PhantomJS'],
        singleRun: true
    });
};

module.exports.dependencies = dependencies;
