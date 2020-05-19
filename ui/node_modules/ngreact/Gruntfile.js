module.exports = function(grunt) {

  grunt.initConfig({
    watch : {
      files: ['ngReact.js', 'tests/*.js'],
      tasks: ['uglify', 'karma:background:run'],
    },
    uglify: {
      build : {
        options: {
          mangle: {
            except: ['angular', 'React']
          }
        },
        files: {
          'ngReact.min.js' : 'ngReact.js'
        }
      }
    },
    karma: {
      options: {
        configFile: 'karma.config.js'
      },
      background: {
        autoWatch: false,
        background: true,
        singleRun: false
      },
      single: {
        autoWatch: false,
        singleRun: true
      }
    },
    docco: {
      build : {
        src: ['ngReact.js'],
        options: {
          output: 'docs/'
        }
      }
    }
  });

  require('load-grunt-tasks')(grunt);

  grunt.registerTask('default', ['uglify', 'karma:single']);

  grunt.registerTask('build', ['uglify']);
  grunt.registerTask('test', ['karma:single']);
  grunt.registerTask('docs', ['docco']);
};
