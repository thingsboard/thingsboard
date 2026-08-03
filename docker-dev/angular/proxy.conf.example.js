/**
 * Optional fallback for the locally served Angular UI (ui-ngx).
 * Copy to ui-ngx/proxy.conf.js and start with:
 *   ng serve --proxy-config proxy.conf.js
 *
 * You normally do NOT need this: the dev backend sends wide-open CORS headers,
 * so Angular can call http://localhost:8080 directly. Use the proxy only if the
 * browser blocks the WebSocket or you want same-origin cookies.
 */
const backend = process.env.TB_BACKEND || 'http://localhost:8080';

module.exports = {
  '/api': { target: backend, secure: false, changeOrigin: false },
  '/static': { target: backend, secure: false, changeOrigin: false },
  '/oauth2': { target: backend, secure: false, changeOrigin: false },
  '/login/oauth2': { target: backend, secure: false, changeOrigin: false },
  '/api/ws': { target: backend.replace(/^http/, 'ws'), ws: true, secure: false, changeOrigin: false },
};
