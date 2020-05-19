'use strict'
import babelCompiler from 'babel-core'
import gulp from 'gulp'
import eslint from 'gulp-eslint'
import istanbul from 'gulp-istanbul'
import mocha from 'gulp-mocha'

const configFiles = './gulpfile.babel.js'
  , srcFiles = 'index.js'
  , testFiles = 'test/*.js'

gulp.task('lint', () =>
  gulp.src([configFiles, srcFiles, testFiles])
    .pipe(eslint())
    .pipe(eslint.failOnError())
)

gulp.task('build', ['lint'])

gulp.task('test', ['build'], cb => {
  gulp.src(['index.js'])
    .pipe(istanbul())
    .pipe(istanbul.hookRequire())
    .on('finish', () => {
      gulp.src([testFiles])
        .pipe(mocha({
          compilers: {
            js: babelCompiler
          }
        }))
        .pipe(istanbul.writeReports())
        .on('end', cb)
    })
})
