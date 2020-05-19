module.exports = function(grunt) {
	
	grunt.loadNpmTasks('grunt-contrib-clean');
	grunt.loadNpmTasks('grunt-contrib-compress');
	grunt.loadNpmTasks('grunt-contrib-concat');
	grunt.loadNpmTasks('grunt-contrib-copy');
	grunt.loadNpmTasks('grunt-contrib-cssmin');
	grunt.loadNpmTasks('grunt-contrib-uglify');
	grunt.loadNpmTasks('grunt-string-replace');
	grunt.loadNpmTasks('grunt-umd');
	
	grunt.initConfig({
		clean: {
			// clear all files
			dist: ["dist"],
			// this file is minified by the globbing pattern but is actually not needed
			// as sideTip's base style is included in the bundle file
			sideTip: ['dist/css/plugins/tooltipster/sideTip/tooltipster-sideTip.min.css']
		},
		compress: {
			dist: {
				files: [
					{
						expand: true,
						ext: '.css.gz',
						extDot: 'last',
						src: ['dist/css/**/*.min.css']
					},
					{
						expand: true,
						ext: '.js.gz',
						extDot: 'last',
						src: ['dist/js/**/*.min.js']
					}
				],
				options: {
					mode: 'gzip',
					level: 9
				}
			}
		},
		concat: {
			// on the main and bundle files
			banner: {
				expand: true,
				src: ['dist/js/!(*.min).js'],
				options: {
					banner:
						'/**\n' +
						' * tooltipster http://iamceege.github.io/tooltipster/\n' +
						' * A rockin\' custom tooltip jQuery plugin\n' +
						' * Developed by Caleb Jacob and Louis Ameline\n' +
						' * MIT license\n' +
						' */\n'
				}
			},
			// on the main and bundle min files
			bannerMin: {
				expand: true,
				src: ['dist/js/*.min.js'],
				options: {
					banner: '/*! <%= pkg.name %> v<%= pkg.version %> */'
				}
			},
			// bundle = main + sideTip
			bundle: {
				files: [
					{
						dest: 'dist/css/tooltipster.bundle.css',
						src: ['dist/css/tooltipster.main.css', 'src/css/plugins/tooltipster/sideTip/tooltipster-sideTip.css']
					},
					{
						dest: 'dist/js/tooltipster.bundle.js',
						src: ['dist/js/tooltipster.main.js', 'src/js/plugins/tooltipster/sideTip/tooltipster-sideTip.js']
					}
				]
			},
			UMDReturn: {
				expand: true,
				src: ['dist/js/**/!(*.min).js'],
				options: {
					footer: 'return $;'
				}
			}
		},
		copy: {
			dist: {
				files: {
					'dist/css/tooltipster.main.css': 'src/css/tooltipster.css',
					'dist/js/tooltipster.main.js': 'src/js/tooltipster.js',
					'dist/js/plugins/tooltipster/SVG/tooltipster-SVG.js': 'src/js/plugins/tooltipster/SVG/tooltipster-SVG.js'
				}
			}
		},
		cssmin: {
			dist: {
				files: [
					{
						dest: 'dist/css/tooltipster.main.min.css',
						src: 'dist/css/tooltipster.main.css'
					},
					{
						dest: 'dist/css/tooltipster.bundle.min.css',
						src: 'dist/css/tooltipster.bundle.css'
					},
					{
						cwd: 'src/css/plugins',
						dest: 'dist/css/plugins',
						expand: true,
						ext: '.min.css',
						extDot: 'last',
						src: ['**/*.css']
					}
				]
			}
		},
		pkg: grunt.file.readJSON('package.json'),
		'string-replace': {
			dist: {
				files: {
					'dist/js/tooltipster.main.js': 'dist/js/tooltipster.main.js'
				},
				options: {
					replacements: [{
						pattern: 'semVer: \'\'',
						replacement: 'semVer: \'<%= pkg.version %>\''
					}]
				}
			}
		},
		uglify: {
			options: {
				compress: true,
				mangle: true,
				preserveComments: false
			},
			dist: {
				files: [{
					expand: true,
					ext: '.min.js',
					extDot: 'last',
					src: ['dist/js/**/!(*.min).js']
				}]
			}
		},
		umd: {
			// main and bundle
			dist: {
				options: {
					deps: {
						default: [{'jquery': '$'}],
						global: [{jQuery: '$'}]
					},
					src: 'dist/js/**/!(*.min).js'
				}
			},
			// SVG pluging
			svg: {
				options: {
					deps: {
						default: [{'tooltipster': '$'}],
						global: [{jQuery: '$'}]
					},
					src: 'dist/js/plugins/tooltipster/SVG/tooltipster-SVG.js'
				}
			}
		}
	});
	
	grunt.registerTask('default', [
		// 'clean:dist',
		'copy',
		'string-replace',
		'concat:bundle',
		'concat:UMDReturn',
		'umd',
		'uglify',
		'concat:banner',
		'concat:bannerMin',
		'cssmin',
		'clean:sideTip',
		'compress'
	]);
};
