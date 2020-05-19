var webpack = require('webpack'),
  port = 3000;

module.exports = {
  entry: [
    'webpack-dev-server/client?http://localhost:' + port,
    'webpack/hot/only-dev-server',
    './lib/app.js'
  ],
  output: {
    'path': './app/',
    'filename': 'app.js',
    'publicPath': '/'
  },
  module: {
    loaders: [
      {test: /\.js$/, exclude: /node_modules/, loaders: ['react-hot', 'babel']}
    ]
  },
  plugins: [
    new webpack.HotModuleReplacementPlugin()
  ],
  devtool: 'cheap-source-map',
  devServer: {
    port: port,
    info: false,
    historyApiFallback: true,
    hot: true,
    contentBase: './app',
    host: 'localhost'
  }
};
