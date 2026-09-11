// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'nospace',
    standalone: false
})
export class NospacePipe implements PipeTransform {

  transform(value: string, args?: any): string {
    return (!value) ? '' : value.replace(/ /g, '');
  }

}
