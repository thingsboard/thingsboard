// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Pipe, PipeTransform } from '@angular/core';

type unit = 'bytes' | 'KB' | 'MB' | 'GB' | 'TB' | 'PB';
type unitPrecisionMap = {
  [u in unit]: number;
};

const defaultPrecisionMap: unitPrecisionMap = {
  bytes: 0,
  KB: 1,
  MB: 1,
  GB: 1,
  TB: 2,
  PB: 2
};

@Pipe({
    name: 'fileSize',
    standalone: false
})
export class FileSizePipe implements PipeTransform {
  private readonly units: unit[] = ['bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];

  transform(bytes: number = 0, precision: number | unitPrecisionMap = defaultPrecisionMap): string {
    if (isNaN(parseFloat(String(bytes))) || !isFinite(bytes)) {
      return '?';
    }

    let unitIndex = 0;

    while (bytes >= 1024) {
      bytes /= 1024;
      unitIndex++;
    }

    const unitSymbol = this.units[unitIndex];

    if (typeof precision === 'number') {
      return `${bytes.toFixed(+precision)} ${unitSymbol}`;
    }
    return `${bytes.toFixed(precision[unitSymbol])} ${unitSymbol}`;
  }
}
