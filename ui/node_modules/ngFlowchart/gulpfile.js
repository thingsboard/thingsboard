'use strict';

var gulp = require('gulp'),
  concat = require('gulp-concat'),
  merge2 = require('merge2'),
  del = require('del'),
  open = require('gulp-open'),
  connect = require('gulp-connect'),
  ngAnnotate = require('gulp-ng-annotate'),
  ngFilesort = require('gulp-angular-filesort'),
  ngHtml2Js = require('gulp-ng-html2js'),
  bowerFiles = require('main-bower-files'),
  karma = require('karma').server;
  //postcss      = require('gulp-postcss'),
  //sourcemaps   = require('gulp-sourcemaps'),
  //autoprefixer = require('autoprefixer-core');

var safeReload = 0; // Semaphore for the reload task, should not run at same time as build tasks. If 0 it is save to run reload.

var jsFilter = {
  filter: /\.js$/i
};

gulp.task('vendorScripts', function() {
  safeReload++;
  var vendorFiles = ['app/bower_components/jquery/dist/jquery.js'];
  vendorFiles = vendorFiles.concat(bowerFiles(jsFilter));
  var ret = gulp.src(vendorFiles)
    .pipe(concat('vendor.js'))
    .pipe(gulp.dest('dist/'));
  safeReload--;
  return ret;
});

gulp.task('flowchartScripts', function() {
  safeReload++;
  var ret = merge2(
    gulp.src(['app/flowchart/*.js', 'app/bower_components/bind-polyfill/index.js', '!app/flowchart/*_test.js'])
      .pipe(ngAnnotate())
      .pipe(ngFilesort()),
    gulp.src('app/flowchart/*.html')
      .pipe(ngHtml2Js({
        moduleName: 'flowchart-templates',
        prefix: 'flowchart/'
      }))
  )
    .pipe(concat('ngFlowchart.js'))
    .pipe(gulp.dest('dist'));
  safeReload--;
  return ret;
});

gulp.task('connect', ['build'], function() {
  connect.server({
    root: ['dist'],
    port: 8000,
    livereload: true
  });
});


gulp.task('open', function() {
  var options = {
    url: 'http://localhost:' + 8000
  };
  gulp.src('dist/index.html')
    .pipe(open('', options));
});

gulp.task('watch', function() {
  gulp.watch('app/flowchart/flowchart.css', ['flowchartCss']);
  gulp.watch(['app/flowchart/*.js', '!app/flowchart/*_test.js', 'app/flowchart/*.html'], ['flowchartScripts']);
  gulp.watch('dist/**', ['reload']);
});

gulp.task('reload', function() {
  if (safeReload === 0) {
    return gulp.src('dist/**')
      .pipe(connect.reload());
  }
});

gulp.task('flowchartCss', function() {
  gulp.src('app/flowchart/flowchart.css')
    .pipe(gulp.dest('dist'));
  //gulp.src('dist/onedatastyle.css')
  //  .pipe(postcss([autoprefixer({ browsers: ['last 2 version'] }) ]))
  //  .pipe(gulp.dest('dist/compiled/'))
});

gulp.task('test', function(done) {
  karma.start({
    configFile: __dirname + '/karma.conf.js',
    singleRun: true
  }, function() {
    done();
  });
});

gulp.task('clean', function(done) {
  del(['dist/ngFlowchart.js', 'dist/vendor.js', 'dist/flowchart.css'], done);
});

gulp.task('build', ['flowchartScripts', 'flowchartCss', 'vendorScripts']);
gulp.task('default', ['connect', 'open', 'watch']);




