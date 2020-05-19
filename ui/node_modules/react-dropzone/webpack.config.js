/* eslint prefer-template: 0 */
/* eslint no-var: 0 */

var path = require('path')

module.exports = {
  entry: './src/index.js',
  devtool: 'source-map',
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: 'index.js',
    libraryTarget: 'umd',
    library: 'Dropzone'
  },
  module: {
    loaders: [
      {
        include: [path.resolve(__dirname, 'src'), path.resolve(__dirname, 'examples')],
        test: /\.js$/,
        loader: 'babel-loader'
      }
    ]
  },
  resolve: {
    // Can require('file') instead of require('file.js') etc.
    extensions: ['', '.js', '.json']
  },
  externals: {
    react: 'react',
    'prop-types': 'prop-types'
  },
  plugins: []
}
