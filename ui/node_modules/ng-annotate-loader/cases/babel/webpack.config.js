var path = require('path');

module.exports = {
  context: __dirname,
  entry: './file-to-annotate.js',
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
  module: {
    rules: [
      {
        test: /\.js$/,
        use: [
          { loader: 'loader' },
          { loader: 'babel-loader', options: { presets: ['es2015'] } },
        ],
      },
    ],
  },
  devtool: 'source-map',
};
