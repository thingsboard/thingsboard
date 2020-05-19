module.exports = {
  module: {
    loaders: [
      { test: /\.jsx?$/, loaders: ['babel-loader'], exclude: /node_modules/ },
    ],
  },
  output: {
    library: 'ReactAce',
    libraryTarget: 'umd',
  },
  resolve: {
    extensions: ['.jsx', '.js', '.tsx'],
  },
};
