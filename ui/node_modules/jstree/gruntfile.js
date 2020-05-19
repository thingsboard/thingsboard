/*global module:false, require:false, __dirname:false*/
var _ = require('lodash');

module.exports = function(grunt) {
  grunt.util.linefeed = "\n";

  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    concat: {
      options : {
        separator : "\n"
      },
      dist: {
        src: ['src/<%= pkg.name %>.js', 'src/<%= pkg.name %>.*.js', 'src/vakata-jstree.js'],
        dest: 'dist/<%= pkg.name %>.js'
      }
    },
    copy: {
      libs : {
        files : [
          { expand: true, cwd : 'libs/', src: ['*'], dest: 'dist/libs/' }
        ]
      },
      docs : {
        files : [
          { expand: true, cwd : 'dist/', src: ['**/*'], dest: 'docs/assets/dist/' }
        ]
      }
    },
    uglify: {
      options: {
        banner: '/*! <%= pkg.title || pkg.name %> - v<%= pkg.version %> - <%= grunt.template.today("yyyy-mm-dd") %> - (<%= _.map(pkg.licenses, "type").join(", ") %>) */\n',
        preserveComments: false,
        //sourceMap: "dist/jstree.min.map",
        //sourceMappingURL: "jstree.min.map",
        report: "min",
        output: {
                ascii_only: true
        },
        compress: {
                hoist_funs: false,
                loops: false,
                unused: false
        }
      },
      dist: {
        src: ['<%= concat.dist.dest %>'],
        dest: 'dist/<%= pkg.name %>.min.js'
      }
    },
    qunit: {
      files: ['test/unit/**/*.html']
    },
    jshint: {
      options: {
        'curly' : true,
        'eqeqeq' : true,
        'latedef' : true,
        'newcap' : true,
        'noarg' : true,
        'sub' : true,
        'undef' : true,
        'boss' : true,
        'eqnull' : true,
        'browser' : true,
        'trailing' : true,
        'globals' : {
          'console' : true,
          'jQuery' : true,
          'browser' : true,
          'XSLTProcessor' : true,
          'ActiveXObject' : true
        }
      },
      beforeconcat: ['src/<%= pkg.name %>.js', 'src/<%= pkg.name %>.*.js'],
      afterconcat: ['dist/<%= pkg.name %>.js']
    },
    dox: {
      files: {
        src: ['src/*.js'],
        dest: 'docs'
      }
    },
    amd : {
      files: {
        src: ['dist/jstree.js'],
        dest: 'dist/jstree.js'
      }
    },
    less: {
      production: {
        options : {
          cleancss : true,
          compress : true
        },
        files: {
          "dist/themes/default/style.min.css" : "src/themes/default/style.less",
          "dist/themes/default-dark/style.min.css" : "src/themes/default-dark/style.less"
        }
      },
      development: {
        files: {
          "src/themes/default/style.css" : "src/themes/default/style.less",
          "dist/themes/default/style.css" : "src/themes/default/style.less",
          "src/themes/default-dark/style.css" : "src/themes/default-dark/style.less",
          "dist/themes/default-dark/style.css" : "src/themes/default-dark/style.less"
        }
      }
    },
    watch: {
      js : {
        files: ['src/**/*.js'],
        tasks: ['js'],
        options : {
          atBegin : true
        }
      },
      css : {
        files: ['src/**/*.less','src/**/*.png','src/**/*.gif'],
        tasks: ['css'],
        options : {
          atBegin : true
        }
      },
    },
    resemble: {
      options: {
        screenshotRoot: 'test/visual/screenshots/',
        url: 'http://127.0.0.1/jstree/test/visual/',
        gm: false
      },
      desktop: {
        options: {
          width: 1280,
        },
        src: ['desktop'],
        dest: 'desktop',
      },
      mobile: {
        options: {
          width: 360,
        },
        src: ['mobile'],
        dest: 'mobile'
      }
    },
    imagemin: {
      dynamic: {
        options: {                       // Target options
          optimizationLevel: 7,
          pngquant : true
        },
        files: [{
          expand: true,                  // Enable dynamic expansion
          cwd:  'src/themes/default/',    // Src matches are relative to this path
          src: ['**/*.{png,jpg,gif}'],   // Actual patterns to match
          dest: 'dist/themes/default/'   // Destination path prefix
        },{
          expand: true,                  // Enable dynamic expansion
          cwd:  'src/themes/default-dark/',    // Src matches are relative to this path
          src: ['**/*.{png,jpg,gif}'],   // Actual patterns to match
          dest: 'dist/themes/default-dark/'   // Destination path prefix
        }]
      }
    },
    replace: {
      files: {
        src: ['dist/*.js', 'bower.json', 'component.json', 'jstree.jquery.json'],
        overwrite: true,
        replacements: [
          {
            from: '{{VERSION}}',
            to: "<%= pkg.version %>"
          },
          {
            from: /"version": "[^"]+"/g,
            to: "\"version\": \"<%= pkg.version %>\""
          },
        ]
      }
    }
  });

  grunt.loadNpmTasks('grunt-contrib-jshint');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-qunit');
  grunt.loadNpmTasks('grunt-resemble-cli');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-imagemin');
  grunt.loadNpmTasks('grunt-text-replace');

  grunt.registerMultiTask('amd', 'Clean up AMD', function () {
    var s, d;
    this.files.forEach(function (f) {
      s = f.src[0];
      d = f.dest;
    });
    grunt.file.copy(s, d, {
      process: function (contents) {
        contents = contents.replace(/\s*if\(\$\.jstree\.plugins\.[a-z]+\)\s*\{\s*return;\s*\}/ig, '');
        contents = contents.replace(/\/\*globals[^\/]+\//ig, '');
        //contents = contents.replace(/\(function \(factory[\s\S]*?undefined/mig, '(function ($, undefined');
        //contents = contents.replace(/\}\)\);/g, '}(jQuery));');
        contents = contents.replace(/\(function \(factory[\s\S]*?undefined\s*\)[^\n]+/mig, '');
        contents = contents.replace(/\}\)\);/g, '');
        contents = contents.replace(/\s*("|')use strict("|');/g, '');
        contents = contents.replace(/\s*return \$\.fn\.jstree;/g, '');
        return grunt.file.read('src/intro.js') + contents + grunt.file.read('src/outro.js');
      }
    });
  });

  grunt.registerMultiTask('dox', 'Generate dox output ', function() {
    var exec = require('child_process').exec,
        path = require('path'),
        done = this.async(),
        doxPath = path.resolve(__dirname),
        formatter = [doxPath, 'node_modules', '.bin', 'dox'].join(path.sep);
    exec(formatter + ' < "dist/jstree.js" > "docs/jstree.json"', {maxBuffer: 5000*1024}, function(error, stout, sterr){
      if (error) {
        grunt.log.error(formatter);
        grunt.log.error("WARN: "+ error);
      }
      if (!error) {
        grunt.log.writeln('dist/jstree.js doxxed.');
        done();
      }
    });
  });

  grunt.util.linefeed = "\n";

  // Default task.
  //grunt.registerTask('default', ['jshint:beforeconcat','concat','amd','jshint:afterconcat','copy:libs','uglify','less','imagemin','replace','copy:docs','qunit','resemble','dox']);
  grunt.registerTask('default', ['jshint:beforeconcat','concat','amd','jshint:afterconcat','copy:libs','uglify','less','imagemin','replace','copy:docs','dox']);
  grunt.registerTask('js', ['concat','amd','uglify']);
  grunt.registerTask('css', ['copy','less']);

};
