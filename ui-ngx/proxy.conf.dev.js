/*
 * Proxy dla Angular dev servera uruchamianego w Dockerze.
 * Rozni sie od proxy.conf.js tym, ze celuje w nazwe serwisu compose
 * (tb-node-dev), a nie w localhost - w kontenerze localhost to sam UI.
 *
 * Hosta/port mozna nadpisac zmiennymi TB_BACKEND_HOST / TB_BACKEND_PORT.
 */
const host = process.env.TB_BACKEND_HOST || 'tb-node-dev';
const port = process.env.TB_BACKEND_PORT || '8080';

const forwardUrl = `http://${host}:${port}`;
const wsForwardUrl = `ws://${host}:${port}`;

const http = {target: forwardUrl, secure: false, changeOrigin: true};
const ws = {target: wsForwardUrl, ws: true, secure: false, changeOrigin: true};

// serwuj ciezkie assety wprost z node_modules, bez kopiowania przy kazdym buildzie
const path = require('path');
PROXY_CONFIG['/assets/tinymce'] = {
  target: 'http://localhost:4200',
  bypass: (req, res) => {
    const rel = req.url.replace(/^\/assets\/tinymce\/?/, '');
    res.sendFile(path.resolve('node_modules/tinymce', rel));
    return true;
  },
};

PROXY_CONFIG['/assets/mdi'] = {
  target: 'http://localhost:4200',
  bypass: (req, res) => {
    const rel = req.url.replace(/^\/assets\/mdi\/?/, '');
    res.sendFile(path.resolve('node_modules/@mdi/svg/svg', rel));
    return true;
  },
};

const PROXY_CONFIG = {
  '/api': http,
  '/static/rulenode': http,
  '/static/widgets': http,
  '/oauth2': http,
  '/login/oauth2': http,
  '/api/ws': ws,
};

module.exports = PROXY_CONFIG;
