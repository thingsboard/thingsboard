// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
const forwardUrl = "http://localhost:18080";
const wsForwardUrl = "ws://localhost:18080";
const ruleNodeUiforwardUrl = forwardUrl;

const PROXY_CONFIG = {
  "/api/thing-model": {
    "target": "http://localhost:8090",
    "secure": false,
    "pathRewrite": {
      "^/api/thing-model": "/api"
    },
  },
  "/api": {
    "target": forwardUrl,
    "secure": false,
  },
  "/static/rulenode": {
    "target": ruleNodeUiforwardUrl,
    "secure": false,
  },
  "/static/widgets": {
    "target": forwardUrl,
    "secure": false,
  },
  "/oauth2": {
    "target": forwardUrl,
    "secure": false,
  },
  "/login/oauth2": {
    "target": forwardUrl,
    "secure": false,
  },
  "/api/ws": {
    "target": wsForwardUrl,
    "ws": true,
    "secure": false
  },
};

module.exports = PROXY_CONFIG;
