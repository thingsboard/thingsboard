'use_strict';

module.exports = function(grunt) {

	require('matchdep').filterDev('grunt-*').forEach(grunt.loadNpmTasks);
	require('time-grunt')(grunt);

	grunt.initConfig({
		pkg: grunt.file.readJSON('package.json'),
		bump: {
			options: {
				files: ['package.json', 'bower.json'],
				updateConfigs: [],
				commit: false,
				push: false,
				commitMessage: 'Release v%VERSION%',
				commitFiles: ['package.json', 'bower.json']
			}
		},
		connect: {
			options: {
				port: 9000,
				hostname: 'localhost'
			},
			dev: {
				options: {
					open: true,
					livereload: 35729
				}
			},
			cli: {
				options: {}
			}
		},
		jsbeautifier: {
			options: {
				config: '.jsbeautifyrc'
			},
			files: [
				'demo/**/*.js',
				'src/**/*.js',
				'test/**/*.js',
				'Gruntfile.js',
				'karma.conf.js',
				'bower.json',
				'index.html',
				'ptor.conf.js'
			]
		},
		jshint: {
			options: {
				jshintrc: '.jshintrc'
			},
			files: ['src/*.js', 'test/**/*.js']
		},
		karma: {
			unit: {
				configFile: 'karma.conf.js',
				background: true,
				singleRun: false
			},
			singleRun: {
				configFile: 'karma.conf.js',
				singleRun: true
			}
		},
		less: {
			dist: {
				options: {
					compress: true
				},
				files: {
					"dist/angular-gridster.min.css": "src/angular-gridster.less"
				}
			},
			min: {
				files: {
					"dist/angular-gridster.css": "src/angular-gridster.less"
				}
			}
		},
		protractor: {
			e2e: {
				options: {
					configFile: "ptor.conf.js",
					args: {}
				}
			}
		},
		uglify: {
			dist: {
				options: {
					banner: ['/*',
						' * <%= pkg.name %>',
						' * <%= pkg.homepage %>',
						' *',
						' * @version: <%= pkg.version %>',
						' * @license: <%= pkg.license %>',
						' */\n'
					].join('\n')
				},
				files: {
					'dist/angular-gridster.min.js': ['src/angular-gridster.js']
				}
			}
		},
		watch: {
			dev: {
				files: ['Gruntfile.js', 'karma.conf.js', 'ptor.conf.js', 'src/*', 'test/**/*.js'],
				tasks: ['jsbeautifier', 'jshint', 'uglify', 'less', 'karma:unit:run'],
				options: {
					reload: true,
					livereload: true,
					port: 35729
				}
			},
			e2e: { // separate e2e so livereload doesn't have to wait for e2e tests
				files: ['src/*', 'test/**/*.js'],
				tasks: ['jsbeautifier', 'jshint', 'uglify', 'protractor']
			}
		}
	});

	grunt.registerTask('default', ['jsbeautifier', 'jshint', 'uglify', 'less']);

	grunt.registerTask('dev', ['connect:dev', 'karma:unit:start', 'watch:dev']);
	grunt.registerTask('e2e', ['connect:cli', 'protractor', 'watch:e2e']);
	grunt.registerTask('test', ['connect:cli', 'karma:singleRun', 'protractor']);

};
