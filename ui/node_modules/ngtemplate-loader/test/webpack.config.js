var path = require('path');

module.exports = {
    entry: {
        test: path.join(__dirname, "src/entry.js")
    },
    output: {
        path: __dirname,
        publicPath: "/",
        filename: "[name].js",
        sourceMapFilename: "[file].map"
    }
};