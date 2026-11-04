// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
/// <reference lib="webworker" />

import { ServiceManager } from 'ace-linters/build/service-manager';
import { CssService } from 'ace-linters/build/css-service';

const manager = new ServiceManager(self);

manager.registerService('css', {
  features: {
    signatureHelp: false,
  },
  module: () => Promise.resolve({ CssService }),
  className: 'CssService',
  modes: 'css',
});
