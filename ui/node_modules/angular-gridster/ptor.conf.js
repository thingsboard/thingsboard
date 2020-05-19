// An example configuration file.
exports.config = {
	// The address of a running selenium server.
	seleniumAddress: 'http://localhost:4444/wd/hub',
	baseUrl: 'http://localhost:9000',

	// Capabilities to be passed to the webdriver instance.
	capabilities: {
		'browserName': 'phantomjs'
	},

	// Spec patterns are relative to the location of the spec file. They may
	// include glob patterns.
	specs: ['test/e2e/*.js'],

	// Options to be passed to Jasmine-node.
	jasmineNodeOpts: {
		showColors: true // Use colors in the command line report.
	}
};
