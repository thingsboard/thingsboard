var gulp = require('gulp'),
    karma = require('karma').server,
    concat = require('gulp-concat'),
    uglify = require('gulp-uglify'),
    rename = require('gulp-rename'),
    jshint = require('gulp-jshint'),
    ngAnnotate = require('gulp-ng-annotate'),
    sourceFiles = [
      'src/angularStorage/angularStorage.prefix',
      'src/angularStorage/angularStorage.js',
      'src/angularStorage/directives/**/*.js',
      'src/angularStorage/filters/**/*.js',
      'src/angularStorage/services/**/*.js',
      'src/angularStorage/angularStorage.suffix'
    ],
    lintFiles = [
      'src/angularStorage/**/*.js',
      'test/**/*.js',
      'gulpfile.js'
    ];

gulp.task('lint', function() {
  return gulp.src(lintFiles)
    .pipe(jshint())
    .pipe(jshint.reporter('jshint-stylish'));
});

gulp.task('build', function() {
  gulp.src(sourceFiles)
    .pipe(concat('angular-storage.js'))
    .pipe(ngAnnotate())
    .pipe(gulp.dest('./dist/'))
    .pipe(uglify())
    .pipe(rename('angular-storage.min.js'))
    .pipe(gulp.dest('./dist'));
});

/**
 * Run test once and exit
 */
gulp.task('test', function (done) {
  karma.start({
    configFile: __dirname + '/karma-src.conf.js',
    singleRun: true
  }, done);
});

/**
 * Run test once in all available browsers and exit
 */
gulp.task('test-all', function (done) {
  karma.start({
    configFile: __dirname + '/karma-src-all.conf.js',
    singleRun: true
  }, done);
});

gulp.task('test-debug', function (done) {
  karma.start({
    configFile: __dirname + '/karma-src.conf.js',
    singleRun: false,
    autoWatch: true
  }, done);
});

/**
 * Run test once and exit
 */
gulp.task('test-dist-concatenated', function (done) {
  karma.start({
    configFile: __dirname + '/karma-dist-concatenated.conf.js',
    singleRun: true
  }, done);
});

/**
 * Run test once and exit
 */
gulp.task('test-dist-minified', function (done) {
  karma.start({
    configFile: __dirname + '/karma-dist-minified.conf.js',
    singleRun: true
  }, done);
});

/**
 * Run code coverage tests once and exit
 */
gulp.task('cover', function (done) {
  karma.start({
    configFile: __dirname + '/karma-src.conf.js',
    singleRun: true,
    preprocessors: {
      'src/**/*.js': 'coverage'
    },
    coverageReporter: {
      type: 'html',
      dir: 'reports/coverage'
    },
    reporters: ['mocha', 'coverage']
  }, function() {
    console.log('Code coverage report created: %s', require('path').join(process.cwd(), 'reports', 'coverage'));
    done();
  });
});

gulp.task('default', ['lint', 'test', 'build']);
gulp.task('dist', ['test-dist-concatenated', 'test-dist-minified']);
