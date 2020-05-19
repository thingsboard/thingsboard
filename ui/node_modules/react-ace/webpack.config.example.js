const webpack = require('webpack');
const path = require('path');


module.exports = {
  devtool: 'source-map',
  entry: [
    './example/index',
  ],
  output: {
    path: path.join(__dirname, 'example/static'),
    filename: 'bundle.js',
    publicPath: '/static/',
  },
  plugins: [
    new webpack.optimize.OccurrenceOrderPlugin(),
    new webpack.HotModuleReplacementPlugin(),
  ],
  module: {
    loaders: [{
      test: /\.jsx?$/,
      loaders: ['babel-loader'],
      exclude: /node_modules/,
    }],
  },
  devServer: {
    hot: true,
    contentBase: path.join(__dirname, 'example'),
    compress: true,
    port: 9000,
  },
};
