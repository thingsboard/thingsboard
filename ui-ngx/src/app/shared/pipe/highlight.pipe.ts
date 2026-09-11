// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'highlight',
    standalone: false
})
export class HighlightPipe implements PipeTransform {
  transform(text: string, search: string, includes = false, flags = 'i'): string {
    const pattern = search
      .replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, '\\$&');
    const regex = new RegExp((!includes ? '^' : '') + pattern, flags);
    return search ? text.replace(regex, match => `<b>${match}</b>`) : text;
  }
}
