var path = require('path');

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
  module: {
    rules: [
      {
        test: /\.ts$/,
        enforce: 'pre',
        exclude: /node_modules/,
        use: [
          {
            loader: 'tslint-loader',
            options: {
              configuration: {
                rules: {
                  quotemark: [true, 'double'],
                },
              },
            },
          },
        ],
      },
      {
        test: /\.ts$/,
        use: [
          { loader: 'loader' },
          { loader: 'awesome-typescript-loader' },
        ],
      },
    ],
  },
  devtool: 'cheap-module-source-map',
};
