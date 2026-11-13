// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
// tsconfig.json emits ES modules, which node can't load through ts-node's CommonJS hook
require('ts-node').register({compilerOptions: {module: 'commonjs'}});
