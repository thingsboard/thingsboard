/*
 * Copyright © 2016-2026 The Thingsboard Authors
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
const forwardUrl = "https://newgen.iot-platform.io.vn";
const wsForwardUrl = "wss://newgen.iot-platform.io.vn";
const ruleNodeUiforwardUrl = forwardUrl;

const PROXY_CONFIG = {
  "/api": {
    "target": forwardUrl,
    // Dev environment may use self-signed certificates.
    // Disabling verification avoids "self-signed certificate" proxy failures.
    "secure": false,
    "changeOrigin": true,
  },
  "/static/rulenode": {
    "target": ruleNodeUiforwardUrl,
    "secure": false,
    "changeOrigin": true,
  },
  "/static/widgets": {
    "target": forwardUrl,
    "secure": false,
    "changeOrigin": true,
  },
  "/oauth2": {
    "target": forwardUrl,
    "secure": false,
    "changeOrigin": true,
  },
  "/login/oauth2": {
    "target": forwardUrl,
    "secure": false,
    "changeOrigin": true,
  },
  "/api/ws": {
    "target": wsForwardUrl,
    "ws": true,
    "secure": false,
    "changeOrigin": true,
  },
};

module.exports = PROXY_CONFIG;
