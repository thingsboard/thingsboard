/*
 * Copyright © 2016-2019 The Thingsboard Authors
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
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const CompressionPlugin = require('compression-webpack-plugin');

const webpack = require('webpack');
const path = require('path');
const dirTree = require('directory-tree');
const jsonminify = require("jsonminify");

const PUBLIC_RESOURCE_PATH = '/static/';

var langs = [];
dirTree('./src/app/locale/', {extensions:/\.json$/}, (item) => {
    /* It is expected what the name of a locale file has the following format: */
    /* 'locale.constant-LANG_CODE[_REGION_CODE].json', e.g. locale.constant-es.json or locale.constant-zh_CN.json*/
    langs.push(item.name.slice(item.name.lastIndexOf('-') + 1, -5));
});

module.exports = {
    mode: 'production',
    entry: [
        './src/app/app.js',
        'webpack-material-design-icons'
    ],
    output: {
        path: path.resolve(__dirname, 'target/generated-resources/public/static'),
        publicPath: PUBLIC_RESOURCE_PATH,
        filename: 'bundle.[hash].js',
        pathinfo: false
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
            {
                from: './src/thingsboard.ico',
                to: 'thingsboard.ico'
            },
            {
                from: './src/app/locale',
                to: 'locale',
                ignore: [ '*.js' ],
                transform: function(content, path) {
                    return Buffer.from(jsonminify(content.toString()));
                }
            }
        ]),
        new HtmlWebpackPlugin({
            template: './src/index.html',
            filename: '../index.html',
            title: 'ThingsBoard',
            inject: 'body',
        }),
        new MiniCssExtractPlugin({
            filename: 'style.[contentHash].css'
        }),
        new webpack.DefinePlugin({
            THINGSBOARD_VERSION: JSON.stringify(require('./package.json').version),
            '__DEVTOOLS__': false,
            PUBLIC_PATH: PUBLIC_RESOURCE_PATH,
            SUPPORTED_LANGS: JSON.stringify(langs)
        }),
        new CompressionPlugin({
            filename: "[path].gz[query]",
            algorithm: "gzip",
            test: /\.js$|\.css$|\.svg$|\.ttf$|\.woff$|\.woff2$|\.eot$|\.json$/,
            threshold: 10240,
            minRatio: 0.8
        })
    ],
    node: {
        tls: "empty",
        fs: "empty"
    },
    module: {
        rules: [
            {
                test: /\.jsx$/,
                use: [
                    {
                        loader: 'babel-loader',
                        options: {
                            cacheDirectory: true
                        }
                    }
                ],
                exclude: /node_modules/,
                include: __dirname,
            },
            {
                test: /\.js$/,
                use: [
                    {
                        loader: 'ng-annotate-loader',
                        options: {
                            ngAnnotate: 'ng-annotate-patched',
                            es6: true,
                            explicitOnly: false
                        }
                    },
                    {
                        loader: 'babel-loader',
                        options: {
                            cacheDirectory: true
                        }
                    }
                ],
                exclude: /node_modules/,
                include: __dirname,
            },
            {
                test: /\.js$/,
                use: [
                    {
                        loader: 'eslint-loader',
                        options: {
                            parser: 'babel-eslint'
                        }
                    }
                ],
                exclude: /node_modules|vendor/,
                include: __dirname,
            },
            {
                test: /\.css$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    'css-loader'
                ]
            },
            {
                test: /\.scss$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    'css-loader',
                    'postcss-loader',
                    'sass-loader'
                ]
            },
            {
                test: /\.less$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    'css-loader',
                    'postcss-loader',
                    'less-loader'
                ]
            },
            {
                test: /\.tpl\.html$/,
                use: [
                    {
                        loader: 'ngtemplate-loader',
                        options: {
                            relativeTo: path.resolve(__dirname, './src/app')
                        }
                    },
                    {
                        loader: 'html-loader'
                    },
                    {
                        loader: 'html-minifier-loader',
                        options: {
                            caseSensitive: true,
                            removeComments: true,
                            collapseWhitespace: false,
                            preventAttributesEscaping: true,
                            removeEmptyAttributes: false
                        }
                    }
                ]
            },
            {
                test: /\.(svg)(\?v=[0-9]+\.[0-9]+\.[0-9]+)?$/,
                use: [
                    {
                        loader: 'url-loader',
                        options: {
                            limit: 8192
                        }
                    }
                ]
            },
            {
                test: /\.(png|jpe?g|gif|woff|woff2|ttf|otf|eot|ico)(\?v=[0-9]+\.[0-9]+\.[0-9]+)?$/,
                use: [
                    {
                        loader: 'url-loader',
                        options: {
                            limit: 8192
                        }
                    },
                    {
                        loader: 'img-loader',
                        options: {
                            minimize: true
                        }
                    }
                ]
            }
        ],
    }
};
