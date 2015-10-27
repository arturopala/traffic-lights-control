var webpack = require('webpack');
var ExtractTextPlugin = require('extract-text-webpack-plugin');

var path = require('path');
var node_modules = path.resolve(__dirname, 'node_modules');

module.exports = {
  
  entry: "./src/main/assets/index.js",

  output: {
    path: __dirname + "/src/main/resources/public/",
    filename: "app.js",
    publicPath: "/"
  },

  plugins: [
    new ExtractTextPlugin('style.css', { allChunks: true }),
    new webpack.ProvidePlugin({'fetch': 'imports?this=>global!exports?global.fetch!whatwg-fetch'})
  ],
  
  module: {
    loaders: [
      { test: /\.jsx?$/, exclude: node_modules, loader: 'babel' },
      { test: /\.css$/, loader: ExtractTextPlugin.extract('style-loader', 'css-loader?modules&importLoaders=1&localIdentName=[name]__[local]___[hash:base64:5]!postcss-loader') }
    ]
  },

  // Additional plugins for CSS post processing using postcss-loader
  postcss: [
    require('autoprefixer'), // Automatically include vendor prefixes
    require('postcss-nested') // Enable nested rules, like in Sass
  ],

  devServer: {
    port: 8081,
    historyApiFallback: true
  }
}
