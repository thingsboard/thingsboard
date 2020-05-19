var paths = require('./gulp/config').paths;

var gulp = require('gulp');
var serve = require('gulp-serve');
var gulpSequence = require('gulp-sequence');
var del = require('del');
var bump = require('gulp-bump');
var templateCache = require('gulp-angular-templatecache');
var KarmaServer = require('karma').Server;


var jsBuild = require('./gulp/jsBuild');
var cssBuild = require('./gulp/cssBuild');
var indexBuild = require('./gulp/indexBuild');



gulp.task('jsSrcBuild', jsBuild.getDevSrc());
gulp.task('jsAppBuild', jsBuild.getDevApp());
gulp.task('jsReleaseBuild', jsBuild.release);
gulp.task('cssBuild', cssBuild.getDev());
gulp.task('cssReleaseBuild', cssBuild.release);
gulp.task('indexBuild', indexBuild.inject);



// -- main tasks. use these to watch and build and release

gulp.task('default', gulpSequence('buildLocal', ['serve', 'watch']));
gulp.task('buildLocal', gulpSequence(
  'clean',
  [
    'jsSrcBuild',
    'jsAppBuild',
    'cssBuild',
    'copyPartials',
    'copyIcons'
  ],
  'indexBuild'
));

gulp.task('build', gulpSequence('buildIconCache', ['jsReleaseBuild', 'cssReleaseBuild'], 'cleanIconCache'));
gulp.task('docs', gulpSequence(
  'buildLocal',
  'copyPublicToDocs',
  'copyBowerToDocs'
));

gulp.task('copyPublicToDocs', function () {
  return gulp.src('public/**/*')
    .pipe(gulp.dest('docs/'));
});

gulp.task('copyBowerToDocs', function () {
  return gulp.src('bower_components/**/*')
    .pipe(gulp.dest('docs/'));
});



gulp.task('clean', function () {
  return del(paths.dest);
});


gulp.task('copyPartials', function () {
  return gulp.src(paths.partials, {base: paths.app})
    .pipe(gulp.dest(paths.dest));
});

gulp.task('copyIcons', function () {
  return gulp.src(paths.icons, {base: paths.src})
    .pipe(gulp.dest(paths.dest));
});

gulp.task('buildIconCache', function () {
  return gulp.src(paths.icons)
    .pipe(templateCache({module: 'material.components.expansionPanels'}))
    .pipe(gulp.dest(paths.src));
});

gulp.task('cleanIconCache', function () {
  return del('src/templates.js');
});

gulp.task('serve', serve({
  root: ['public', 'bower_components'],
  port: 8080
}));



gulp.task('test-karma', function (done) {
  new KarmaServer({
    configFile: __dirname + '/karma.conf.js',
    singleRun: true
  }, function (errorCode) {
    if (errorCode !== 0) {
      console.log('Karma exited with error code ' + errorCode);
      done();
      return process.exit(errorCode);
    }
    done();
  }).start();
});

gulp.task('test', gulpSequence('build', 'test-karma'));




gulp.task('watch', function () {
  gulp.watch(paths.scripts, function (event) {
    jsBuild.getDevSrc(event.path)()
      .on('end', function () {
        if (event.type !== 'changed') { indexBuild.inject(); }
      });
  });

  gulp.watch(paths.appScripts, function (event) {
    jsBuild.getDevApp(event.path)()
      .on('end', function () {
        if (event.type !== 'changed') { indexBuild.inject(); }
      });
  });


  gulp.watch(paths.css.concat(paths.appCss), function (event) {
    cssBuild.getDev(event.path)()
      .on('end', function () {
        if (event.type !== 'changed') { indexBuild.inject(); }
      });
  });


  gulp.watch(paths.partials, function (event) {
    return gulp.src(event.path, {base: paths.app})
      .pipe(gulp.dest(paths.dest));
  });
});






gulp.task('major', function(){
  gulp.src(['./bower.json', './package.json'])
  .pipe(bump({type:'major'}))
  .pipe(gulp.dest('./'));
});

gulp.task('minor', function(){
  gulp.src(['./bower.json', './package.json'])
  .pipe(bump({type:'minor'}))
  .pipe(gulp.dest('./'));
});

gulp.task('patch', function(){
  gulp.src(['./bower.json', './package.json'])
  .pipe(bump({type:'patch'}))
  .pipe(gulp.dest('./'));
});

gulp.task('prerelease', function(){
  gulp.src(['./bower.json', './package.json'])
  .pipe(bump({type:'prerelease'}))
  .pipe(gulp.dest('./'));
});
