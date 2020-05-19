'use strict';

var gulp = require('gulp'),
    uglify = require('gulp-uglify'),
    $ = require('gulp-load-plugins')({lazy: true});

gulp.task('lint', function() {
    return gulp.src([
        'angular-material-icons.js',
        'demo.js'
    ])
        .pipe($.jshint())
        .pipe($.jshint.reporter('jshint-stylish', {verbose: true}));
});

gulp.task('minify', function() {
    return gulp.src([
        'angular-material-icons.js'
    ])
        .pipe($.uglify())
        .pipe($.rename('angular-material-icons.min.js'))
        .pipe(gulp.dest('.'));
});

gulp.task('default', ['lint', 'minify']);

module.exports = gulp;
