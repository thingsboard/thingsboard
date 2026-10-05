// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from '@app/app.module';
import { environment } from '@env/environment';

import $ from 'jquery';

(window as any).jQuery = $;
(window as any).$ = $;

if (environment.production) {
  enableProdMode();
  // Extensions are loaded at runtime via SystemJS as ng-packagr library bundles, which keep
  // bare `ngDevMode` references. The app build replaces `ngDevMode` at compile time (dropping
  // the global assignment in enableProdMode), so define it for runtime-loaded bundles.
  (globalThis as any).ngDevMode = false;
}

platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => console.error(err));
