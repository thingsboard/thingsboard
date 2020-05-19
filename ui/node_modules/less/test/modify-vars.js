var less;

// Dist fallback for NPM-installed Less (for plugins that do testing)
try {
    less = require('../tmp/less.cjs.js');
}
catch (e) {
    less = require('../dist/less.cjs.js');
}

var fs = require('fs');

var input = fs.readFileSync('./test/less/modifyVars/extended.less', 'utf8');
var expectedCss = fs.readFileSync('./test/css/modifyVars/extended.css', 'utf8');
var options = {
    modifyVars: JSON.parse(fs.readFileSync('./test/less/modifyVars/extended.json', 'utf8'))
};

less.render(input, options, function (err, result) {
    if (err) {
        console.log(err);
    }
    if (result.css === expectedCss) {
        console.log('PASS');
    } else {
        console.log('FAIL');
    }
});
