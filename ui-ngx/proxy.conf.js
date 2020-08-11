/*
 * Copyright © 2016-2020 The Thingsboard Authors
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
const ruleNodeUiforwardHost = "localhost";
const ruleNodeUiforwardPort = 8080;

const PROXY_CONFIG = {
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
  },
  "/static/rulenode": {
    "target": `http://${ruleNodeUiforwardHost}:${ruleNodeUiforwardPort}`,
    "secure": false,
  },
  "/static": {
    "target": "http://localhost:8080",
    "secure": false,
  },
  "/api/ws": {
    "target": "ws://localhost:8080",
    "ws": true,
  },
};

module.exports = PROXY_CONFIG;
