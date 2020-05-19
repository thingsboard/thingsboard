var gulp = require('gulp'),
	gutil = require('gulp-util'),
	path = require('path'),
	gdebug = require('gulp-debug'),
	seq = require('run-sequence'),
	streamqueue = require('streamqueue'),
	closure = require('gulp-jsclosure'),
	less = require('gulp-less'),
	uglify = require('gulp-uglify'),
	sourcemaps = require('gulp-sourcemaps'),
	cssnano = require('gulp-cssnano'),
	concat = require('gulp-concat'),
	rename = require('gulp-rename'),
	templateCache = require('gulp-angular-templatecache'),
	ngAnnotate = require('gulp-ng-annotate'),
	autoprefix = require('gulp-autoprefixer'),
	livereload = require('gulp-livereload'),
	injectReload = require('gulp-inject-reload'),
	http = require('http'),
	st = require('st'),
	del = require('del'),
	merge = require('merge-stream'),
	header = require('gulp-header');

var debug = false;

var ports = {
	web: 3333,
	livereload: 3334
};

var moduleName = 'mdColorPicker';
var paths = {
	demo: 'demo',
	dist: 'dist/',
	src: {
		demo: ['demo/**/*.*'],
		less: ['src/less/*.less'],
		templates: ['src/templates/*.tpl.html'],
		js: ['src/js/*.js']
	}
};


var pkg = require('./package.json');
var banner = ['/**',
  ' * <%= pkg.name %> - <%= pkg.description %>',
  ' * @version v<%= pkg.version %>',
  ' * @link <%= pkg.homepage %>',
  ' * @license <%= pkg.license %>',
  ' */',
  ''].join('\n');




/*====================================================================
 =                  Compile and minify less and css                  =
 ====================================================================*/

gulp.task('less', function () {
	gulp.src(paths.src.less)
		.pipe(less({strictMath: true}))
		.pipe(concat(moduleName + '.css'))
		.pipe(autoprefix({browsers: ['last 2 versions', 'last 4 Android versions']}))
		.pipe(header(banner, { pkg : pkg } ))
		.pipe(gulp.dest(paths.dist))
		.pipe(rename({extname: '.min.css'}))
		.pipe(cssnano({ safe: true }))
		.pipe(header(banner, { pkg : pkg } ))
		.pipe(gulp.dest(paths.dist))
		.pipe(livereload());
});


/*====================================================================
 =            Compile and minify js generating source maps            =
 ====================================================================*/
// - Orders ng deps automatically
// - Depends on templates task
gulp.task('js', function () {

	var jsStream = gulp.src(paths.src.js);
	var templateStream = gulp.src(paths.src.templates)
		.pipe(templateCache({module: moduleName}));
	merge(jsStream, templateStream)

		.pipe(gdebug())


		//.pipe(debug({title: 'JS: '}))
		//.pipe(sourcemaps.init())
		.pipe(concat(moduleName + '.js'))
		//.pipe(sourcemaps.write('.'))
		.pipe(closure(['angular', 'window', 'tinycolor']))
		.pipe(ngAnnotate())
		.pipe(header(banner, { pkg : pkg } ))
		.pipe(gulp.dest(paths.dist))
		.pipe(rename({suffix: '.min'}))
		.pipe(uglify({
			"compress": {
            	"drop_console": !debug,
				"drop_debugger": !debug
        	}
		}))
		.pipe(header(banner, { pkg : pkg } ))
		.pipe(gulp.dest(paths.dist))
		.pipe(livereload());

});

/*====================================================================
 =            Build the demo and demo resources                     =
 ====================================================================*/

gulp.task('demo-resources', function () {
	gulp.src(['demo/**/*.{js,css}', 'demo/redirect.html'])
		.pipe(gulp.dest('dist/demo'))
		.pipe(livereload());


});


gulp.task('demo', ['demo-resources'], function () {
	gulp.src('demo/index.html')
		.pipe(injectReload({port: ports.livereload}))
		.pipe(gulp.dest('dist/demo'))
		.pipe(livereload());


});

/*===================================================================
 =            Start local demo/dev server                           =
 ===================================================================*/
gulp.task('server', ['build', 'demo'], function () {
	livereload.listen({port: ports.livereload, basePath: "."});
	http.createServer(
		st({path: path.resolve(__dirname, 'dist'), index: 'demo/redirect.html', cache: false})
	).listen(ports.web);


});


/*===================================================================
 =            Watch for source changes and rebuild/reload            =
 ===================================================================*/
gulp.task('watch', ['clean'], function () {

	gutil.log("Started dev server @ http://localhost:" + ports.web + "/demo/index.html");
	//gulp.watch(paths.src.html, ['html']);
	gulp.watch(paths.src.less.concat(paths.src.js.concat(paths.src.templates)), ['build']);
	gulp.watch(paths.src.demo, ['demo']);


	gulp.start('server');


});


/*=========================================
 =            Clean dest folder            =
 =========================================*/

gulp.task('clean', function (cb) {
	return del([paths.dist + '/**/*']);
});


/*======================================
 =            Build Sequence            =
 ======================================*/

gulp.task('build', function (done) {
	var tasks = ['less', 'js', 'demo'];
	seq('clean', tasks, done);
});

/*====================================
 =            Default Task            =
 ====================================*/
gulp.task('default', function () {
	debug = true;
	gulp.start('watch');
});
