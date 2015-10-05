var webpack = require('webpack');

var path = require('path');
var node_modules = path.resolve(__dirname, 'node_modules');
var pathToReact = path.resolve(node_modules, 'react/dist/react.js');

module.exports = {
  
  entry: "./src/main/assets/main.js",

  resolve: {
      alias: {
        'react': pathToReact
      }
  },

  output: {
    path: __dirname + "/src/main/resources/public/",
    filename: "app.js",
    publicPath: "/"
  },

  module: {
    loaders: [
      { test: /\.jsx?$/, exclude: node_modules, loader: 'babel' },
      { test: /\.css$/, loader: 'style!css'},
      { test: /\.less$/, loader: 'style!css!less'}
    ],
    noParse: [pathToReact]
  }
}
