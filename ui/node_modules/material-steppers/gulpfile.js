'use strict';

var gulp = require('gulp'),
    rename = require('gulp-rename'),
    uglify = require('gulp-uglify'),
    ts = require('gulp-typescript'),
    less = require('gulp-less'),
    autoprefixer = require('autoprefixer'),
    postcss = require('gulp-postcss'),
    cssnano = require('cssnano'),
    browserSync = require('browser-sync').create(),
    templateCache = require('gulp-angular-templatecache'),
    concat = require('gulp-concat'),
    orderedMergeStream = require('ordered-merge-stream');

var tsProject = ts.createProject('tsconfig.json', {
    noExternalResolve: true
});

var production = true;

gulp.task('default', ['serve']);
gulp.task('build', ['js', 'css']);


gulp.task('templates', function () {
    return gulp.src('lib/**/*.tpl.html')
        .pipe(templateCache('material-steppers-tpl.js', {
            root: 'mdSteppers',
            module: 'mdSteppers'
        }))
        // .pipe(gulp.dest('dist'))
        .pipe(gulp.dest('demo'));
});

gulp.task('serve', ['build'], function (callback) {
    production = false;
    browserSync.init({
        server: {
            baseDir: "./demo",
            routes: {
                "/lib": "mdSteppers"
            }
        },
        ui: false
    });
    gulp.watch("lib/*.ts", ['js']);
    gulp.watch("lib/*.html", ['js']);
    gulp.watch("lib/*.less", ['css']);
    gulp.watch("demo/*.html").on('change', browserSync.reload);
});

gulp.task('css', function (callback) {
    var stream = gulp.src('lib/*.less')
        .pipe(less())
        .pipe(postcss([autoprefixer({
            browsers: [
                'ie >= 10',
                'ie_mob >= 10',
                'ff >= 30',
                'chrome >= 34',
                'safari >= 7',
                'opera >= 23',
                'ios >= 7',
                'android >= 4.4',
                'bb >= 10'
            ]
        })]))
        .pipe(gulp.dest('dist'))
        .pipe(gulp.dest('demo'))
        .pipe(rename({ extname: '.min.css' }))
        .pipe(gulp.dest('demo'))
        .pipe(postcss([cssnano({
            discardComments: { removeAll: true }
        })]))
        .pipe(gulp.dest('dist'));
    return stream;
});


gulp.task('js', ['templates'], function (callback) {
    var stream = gulp.src('lib/*.ts')
        .pipe(ts(tsProject))
        .pipe(gulp.dest('dist'))
        .pipe(gulp.dest('demo'));

    return orderedMergeStream([stream, gulp.src('demo/*-tpl.js')])
        .pipe(concat('material-steppers.js'))
        .pipe(gulp.dest('dist'))
        .pipe(gulp.dest('demo'))
        .pipe(rename({ extname: '.min.js' }))
        .pipe(gulp.dest('demo'))
        .pipe(uglify())
        .pipe(gulp.dest('dist'));
});