"use strict";

var gulp = require('gulp'),
    uglify = require('gulp-uglify'),
    less = require('gulp-less'),
    rename = require('gulp-rename'),
    minify = require('gulp-cssnano'),
    sourcemaps = require('gulp-sourcemaps'),
    wrap = require('gulp-wrap'),
    concat = require('gulp-concat'),
    autoprefixer = require('gulp-autoprefixer'),
    path = require('path');

var outputFolder = 'dist/';
var moduleName = 'mdPickers';

gulp.task('assets', function() {
  return gulp.src(['src/core/**/*.less', 'src/components/**/*.less'])
        .pipe(concat('mdPickers.less'))
        .pipe(less())
        .pipe(autoprefixer())
        .pipe(gulp.dest(outputFolder))
        .pipe(rename({suffix: '.min'}))
        .pipe(minify())
        .pipe(gulp.dest(outputFolder));
});

gulp.task('build-app', function() {  
    return gulp.src(['src/mdPickers.js', 'src/core/**/*.js', 'src/components/**/*.js'])
        .pipe(concat('mdPickers.js'))
        .pipe(wrap('(function() {\n"use strict";\n<%= contents %>\n})();'))
        .pipe(sourcemaps.init())
        .pipe(gulp.dest(outputFolder))
        .pipe(rename({suffix: '.min'}))
        .pipe(uglify())
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(outputFolder));
});

gulp.task('watch', function() {
    gulp.watch('src/**/*', ['assets', 'build-app']);
});

gulp.task('default', ['assets', 'build-app']);