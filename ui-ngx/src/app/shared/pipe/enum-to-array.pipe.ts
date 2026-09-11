// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'enumToArray',
    standalone: false
})
export class EnumToArrayPipe implements PipeTransform {
  transform(data: object): string[] {
    const keys = Object.keys(data);
    return keys.slice(keys.length / 2);
  }
}
