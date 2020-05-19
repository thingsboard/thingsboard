const html = require('html-template-tag')

/**
 * Generates HTML templates from list of test sheets
 */
module.exports = (stylesheets, helpers, spec, less) => {
    if (!Array.isArray(helpers)) {
        helpers = [helpers]
    }
    return html`
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    
    <title>Less.js Spec Runner</title>

    <!-- for each test, generate CSS/LESS link tags -->
    $${stylesheets.map(function(fullLessName) {
        var pathParts = fullLessName.split('/');
        var fullCssName = fullLessName.replace(/less/g, 'css');
        var lessName = pathParts[pathParts.length - 1];
        var name = lessName.split('.')[0];
        return `
    <!-- the tags to be generated -->
    <link id="original-less:test-less-${name}" title="test-less-${name}" rel="stylesheet/less" type="text/css" href="../../${fullLessName}">
    <link id="expected-less:test-less-${name}" rel="stylesheet" type="text/css" href="../../${fullCssName}">
    ` }).join('')}

    $${helpers.map(helper => `
        <script src="../../${helper}"></script>
    `).join('')}

    <link rel="stylesheet" href="../../node_modules/mocha/mocha.css">
</head>

<body>
    <!-- content -->
    <div id="mocha"></div>
    <script src="../../node_modules/mocha/mocha.js"></script>
    <script src="../../node_modules/mocha-teamcity-reporter/lib/teamcityBrowser.js"></script>
    <script src="../../node_modules/chai/chai.js"></script>
    <script>
        expect = chai.expect
        mocha.setup({
            ui: 'bdd',
            timeout: 1500
        });
    </script>
    <script src="common.js"></script>
    <script src="../../${spec}"></script>
    <script src="${less || 'less.min.js'}"></script>
    <script>
        /** Saucelabs config */
        onload = function() {
            var runner = mocha.run();

            var failedTests = [];
            runner.on('end', function() {
                window.mochaResults = runner.stats;
                window.mochaResults.reports = failedTests;
            });

            runner.on('fail', logFailure);

            function logFailure(test, err){
                var flattenTitles = function(test){
                    var titles = [];
                    while (test.parent.title) {
                        titles.push(test.parent.title);
                        test = test.parent;
                    }
                    return titles.reverse();
                };

                failedTests.push({name: test.title, result: false, message: err.message, stack: err.stack, titles: flattenTitles(test) });
            };
        };
    </script>
</body>
</html>
`
}