var path = require('path');
require('../../index.js');

module.exports = {
    entry: {
        test: path.join(__dirname, "my-directive/my-directive.js")
    },
    module: {
        preLoaders: [
            { test: /\.js$/, loader: 'baggage?[file].html' }
        ],
        loaders: [
            // replace ../../../index.js with ngtemplate
            { test: /\.html$/, loader: "../../../index.js?relativeTo=" + __dirname + "!html" }
        ]
    },
    output: {
        path: __dirname,
        publicPath: "/",
        filename: "bundle.js",
        sourceMapFilename: "bundle.map"
    }
};
