/* global require, module, process, __dirname */

'use strict';

var path = require('path');

module.exports = function(grunt) {

  require('load-grunt-tasks')(grunt);

  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    meta: {
      banner: '/**\n' +
      ' * <%= pkg.description %>\n' +
      ' * @version v<%= pkg.version %> - <%= grunt.template.today("yyyy-mm-dd") %>\n' +
      ' * @link <%= pkg.homepage %>\n' +
      ' * @author <%= pkg.author %>\n' +
      ' * @license MIT License, http://www.opensource.org/licenses/MIT\n' +
      ' */\n'
    },
    connect: {
      devserver: {
        options: {
          port: 9999,
          hostname: '0.0.0.0',
          base: '.'
        }
      }
    },
    dirs: {
      src: 'src',
      dest: 'dist'
    },
    copy: {

    },
    autoprefixer: {
      source: {
        //options: {
          //browsers: ['last 2 version']
        //},
        src: '<%= dirs.dest %>/<%= pkg.name %>.css',
        dest: '<%= dirs.dest %>/<%= pkg.name %>.css'
      }
    },
    concat: {
      options: {
        banner: '<%= meta.banner %>'
      },
      dist: {
        src: ['<%= dirs.src %>/*.js', '<%= dirs.src %>/**/*.js'],
        dest: '<%= dirs.dest %>/<%= pkg.name %>.js'
      }
    },

    sass: {
      dist: {
        files: [{
          expand: true,
          cwd: './src/css',
          src: ['*.scss'],
          dest: './dist',
          ext: '.css'
        }]
      }
    },

    cssmin: {
      combine: {
        files: {
          '<%= dirs.dest %>/<%= pkg.name %>.min.css': ['<%= dirs.dest %>/<%= pkg.name %>.css']
        }
      }
    },
    ngAnnotate: {
      dist: {
        files: {
          '<%= concat.dist.dest %>': ['<%= concat.dist.dest %>']
        }
      }
    },
    uglify: {
      options: {
        banner: '<%= meta.banner %>'
      },
      dist: {
        src: ['<%= concat.dist.dest %>'],
        dest: '<%= dirs.dest %>/<%= pkg.name %>.min.js'
      }
    },
    jshint: {
      files: ['Gruntfile.js', '<%= dirs.src %>/*.js', 'test/unit/*.js'],
      options: {
        curly: false,
        browser: true,
        eqeqeq: true,
        immed: true,
        latedef: true,
        newcap: true,
        noarg: true,
        sub: true,
        undef: true,
        boss: true,
        eqnull: true,
        expr: true,
        node: true,
        globals: {
          exports: true,
          angular: false,
          $: false
        }
      }
    },
    karma: {
      options: {
          // needed to use absolute path for some reason
          configFile: path.join(__dirname, 'test', 'karma.conf.js')
      },
      unit: {
          port: 7101,
          singleRun: false,
          background: true
      },
      continuous: {
          singleRun: true
      }
    },
    changelog: {
      options: {
        dest: 'CHANGELOG.md'
      }
    },
    watch: {
      dev: {
        files: ['<%= dirs.src %>/**'],
        tasks: ['build', 'karma:unit:run']
      },
      test: {
        files: ['test/unit/**'],
        tasks: ['karma:unit:run']
      }
    }
  });

  // Build task.
  grunt.registerTask('build', ['jshint', 'concat', 'ngAnnotate', 'uglify', 'sass', 'autoprefixer', 'cssmin']);

  // Default task.
  grunt.registerTask('default', ['build', 'connect', 'karma:unit', 'watch']);

};
