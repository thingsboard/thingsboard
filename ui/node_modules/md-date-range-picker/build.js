/**
 * Custom builder
 * TODO: create a better builder that support building html templates which is not hard coded 
 *       use sass
 */
var path = require('path');
var version = require('./package.json').version;
console.log('Build version ',version);

console.log('clean started...');
var rimraf = require('rimraf');
rimraf(path.join(__dirname, 'dist', '*'), build);

function build(){
    console.log('build started...');
    try{
    var date = new Date().toLocaleDateString();
    var fs = require('fs');
    var jsp = require("uglify-js").parser;
    var pro = require("uglify-js").uglify;
    var htm = require('html-minifier').minify;
    var CleanCSS = require('clean-css')
    var htmlMinOpt = {
        collapseInlineTagWhitespace: true,
        collapseWhitespace: true,
        caseSensitive: true,
        quoteCharacter: '"',
        removeComments: true
    }
    var appJsPath = path.join(__dirname, 'src', 'md-date-range-picker.js');
    var appHtmlPath = path.join(__dirname, 'src', 'md-date-range-picker.html');
    var appCssPath = path.join(__dirname, 'src', 'md-date-range-picker.css');
    var appJsOut = path.join(__dirname, 'dist', 'md-date-range-picker.js');
    var appCssOut = path.join(__dirname, 'dist', 'md-date-range-picker.css');
    var appCssMinOut = path.join(__dirname, 'dist', 'md-date-range-picker.min.css');
    var appJsMinOut = path.join(__dirname, 'dist', 'md-date-range-picker.min.js');
    var jsTemplate = fs.readFileSync(appJsPath, 'utf8');
    var htmlTemplate = fs.readFileSync(appHtmlPath, 'utf8');
    var cssTemplate = fs.readFileSync(appCssPath, 'utf8');
    var css = new CleanCSS().minify(cssTemplate);
    var appjs = jsTemplate.replace('./md-date-range-picker.html', htm(htmlTemplate, htmlMinOpt).replace(/\'/g, '\\\'')).replace('templateUrl:','template:'); //extra carefull here
    var ast = jsp.parse(appjs); // parse code and get the initial AST

    ast = pro.ast_mangle(ast); // get a new AST with mangled names
    ast = pro.ast_squeeze(ast); // get an AST with compression optimizations
    final_code = pro.gen_code(ast); // compressed code here
    console.log('writing file '+appJsOut+'...');
    fs.writeFile(appJsOut, appjs.replace('${builddate}',date).replace('${version}',version), 
        function(){
            console.log('writing file '+appJsMinOut+'...');
            fs.writeFile(appJsMinOut, final_code, 
                function () {
                    console.log('writing file '+appCssOut+'...');
                    fs.writeFile(appCssOut, cssTemplate.replace('${builddate}',date).replace('${version}',version),
                        function() {
                            console.log('writing file '+appCssMinOut+'...');
                            fs.writeFile(appCssMinOut, css.styles, 
                                function() {
                                    console.log('build success...');
                                }
                            ); //write min css
                        }
                    ); //write css
                }            
            ); //write minifieds
        }
    ); //write unminified
    }catch(e){
        console.error(e);
    }

}
