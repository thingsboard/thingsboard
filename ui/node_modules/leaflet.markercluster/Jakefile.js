/*
Leaflet.markercluster building, testing and linting scripts.

To use, install Node, then run the following commands in the project root:

    npm install -g jake
    npm install

To check the code for errors and build Leaflet from source, run "jake".
To run the tests, run "jake test".

For a custom build, open build/build.html in the browser and follow the instructions.
*/

var path = require('path');

desc('Check Leaflet.markercluster source for errors with JSHint');
task('lint', function(){
		jake.exec('jshint', {
			printStdout: true
		}, function () {
			console.log('\tCheck passed.\n');
			complete();
		});
});

desc('Combine Leaflet.markercluster source files');
task('build', ['lint'], function(){
	jake.exec('npm run-script rollup', function() { console.log('Rolled up.'); });
});

desc('Compress bundled files');
task('uglify', ['build'], function(){
	jake.exec('npm run-script uglify', function() { console.log('Uglyfied.'); });
});

desc('Run PhantomJS tests');
task('test', ['lint'], function() {

	var karma = require('karma'),
	testConfig = {configFile : path.join(__dirname, './spec/karma.conf.js')};

	testConfig.browsers = ['PhantomJS'];

	function isArgv(optName) {
		 return process.argv.indexOf(optName) !== -1;
	}

	if (isArgv('--chrome')) {
		testConfig.browsers.push('Chrome');
	}
	if (isArgv('--safari')) {
		testConfig.browsers.push('Safari');
	}
	if (isArgv('--ff')) {
		testConfig.browsers.push('Firefox');
	}
	if (isArgv('--ie')) {
		testConfig.browsers.push('IE');
	}

	if (isArgv('--cov')) {
		testConfig.preprocessors = {
			'src/**/*.js': 'coverage'
		};
		testConfig.coverageReporter = {
			type : 'html',
			dir : 'coverage/'
		};
		testConfig.reporters = ['coverage'];
	}

	console.log('Running tests...');

	var server = new karma.Server(testConfig, function(exitCode) {
		if (!exitCode) {
			console.log('\tTests ran successfully.\n');
			complete();
		} else {
			process.exit(exitCode);
		}
	});
	server.start();
});

task('default', ['build', 'uglify']);
