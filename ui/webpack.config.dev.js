/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* eslint-disable */

const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const webpack = require('webpack');
const path = require('path');

/* devtool: 'cheap-module-eval-source-map', */

module.exports = {
    devtool: 'source-map',
    entry: [
        './src/app/app.js',
        'webpack-hot-middleware/client?reload=true',
        'webpack-material-design-icons'
    ],
    output: {
        path: path.resolve(__dirname, 'target/generated-resources/public/static'),
        publicPath: '/',
        filename: 'bundle.js',
    },
    plugins: [
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery",
            "window.jQuery": "jquery",
            tinycolor: "tinycolor2",
            tv4: "tv4",
            moment: "moment"
        }),
        new CopyWebpackPlugin([
            { from: './src/thingsboard.ico', to: 'thingsboard.ico' }
        ]),
        new webpack.HotModuleReplacementPlugin(),
        new HtmlWebpackPlugin({
            template: './src/index.html',
            filename: 'index.html',
            title: 'ThingsBoard',
            inject: 'body',
        }),
        new webpack.optimize.OccurrenceOrderPlugin(),
        new webpack.NoErrorsPlugin(),
        new ExtractTextPlugin('style.[contentHash].css', {
            allChunks: true,
        }),
        new webpack.DefinePlugin({
            THINGSBOARD_VERSION: JSON.stringify(require('./package.json').version),
            '__DEVTOOLS__': false,
            'process.env': {
                NODE_ENV: JSON.stringify('development'),
            },
        }),
    ],
    node: {
        tls: "empty",
        fs: "empty"
    },
    module: {
        loaders: [
            {
                test: /\.jsx$/,
                loader: 'babel',
                exclude: /node_modules/,
                include: __dirname,
            },
            {
                test: /\.js$/,
                loaders: ['ng-annotate', 'babel'],
                exclude: /node_modules/,
                include: __dirname,
            },
            {
                test: /\.js$/,
                loader: "eslint-loader?{parser: 'babel-eslint'}",
                exclude: /node_modules|vendor/,
                include: __dirname,
            },
            {
                test: /\.css$/,
                loader: ExtractTextPlugin.extract('style-loader', 'css-loader'),
            },
            {
                test: /\.scss$/,
                loader: ExtractTextPlugin.extract('style-loader', 'css-loader!postcss-loader!sass-loader'),
            },
            {
                test: /\.less$/,
                loader: ExtractTextPlugin.extract('style-loader', 'css-loader!postcss-loader!less-loader'),
            },
            {
                test: /\.tpl\.html$/,
                loader: 'ngtemplate?relativeTo=' + (path.resolve(__dirname, './src/app')) + '/!html!html-minifier-loader'
            },
            {
                test: /\.(svg)(\?v=[0-9]+\.[0-9]+\.[0-9]+)?$/,
                loader: 'url?limit=8192'
            },
            {
                test: /\.(png|jpe?g|gif|woff|woff2|ttf|otf|eot|ico)(\?v=[0-9]+\.[0-9]+\.[0-9]+)?$/,
                loaders: [
                    'url?limit=8192',
                    'img?minimize'
                ]
            },
        ],
    },
    'html-minifier-loader': {
        caseSensitive: true,
        removeComments: true,
        collapseWhitespace: false,
        preventAttributesEscaping: true,
        removeEmptyAttributes: false
    }
};
