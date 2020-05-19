var path = require('path');
var UglifyJsPlugin = require('webpack/lib/optimize/UglifyJsPlugin');

module.exports = {
  context: __dirname,
  entry: './file-to-annotate',
  output: {
    path: __dirname + '/dist',
    filename: 'build.js',
  },
  resolveLoader: {
    modules: [
      'node_modules',
      path.resolve(__dirname, '../../'),
    ],
  },
  resolve: {
    extensions: ['.ts'],
    modules: [
      __dirname,
      'node_modules',
    ],
  },
  plugins: [
    /**
     * Plugin: UglifyJsPlugin
     * Description: Minimize all JavaScript output of chunks.
     * Loaders are switched into minimizing mode.
     *
     * See: https://webpack.github.io/docs/list-of-plugins.html#uglifyjsplugin
     */
    new UglifyJsPlugin({
      beautify: false,
      mangle: {screw_ie8: true},
      compress: {
        screw_ie8: true,
      },
      sourceMap: true,
      comments: false,
    }),
  ],
  module: {
    rules: [
      {
        test: /\.ts$/,
        use: [
          { loader: 'loader' },
          { loader: 'awesome-typescript-loader' },
        ],
      },
    ],
  },
  devtool: 'source-map',
};
