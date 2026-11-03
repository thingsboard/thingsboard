// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Inject, Pipe, PipeTransform } from '@angular/core';
import { WINDOW } from '@core/services/window.service';

// @dynamic
@Pipe({
    name: 'keyboardShortcut',
    standalone: false
})
export class KeyboardShortcutPipe implements PipeTransform {

  constructor(@Inject(WINDOW) private window: Window) {}

  transform(value: string): string {
    if (!value) {
      return;
    }
    const keys = value.split('-');
    const isOSX = /Mac OS X/.test(this.window.navigator.userAgent);

    const seperator = (!isOSX || keys.length > 2) ? '+' : '';

    const abbreviations = {
      M: isOSX ? '⌘' : 'Ctrl',
      A: isOSX ? 'Option' : 'Alt',
      S: 'Shift'
    };

    return keys.map((key, index) => {
      const last = index === keys.length - 1;
      return last ? key : abbreviations[key];
    }).join(seperator);
  }

}
