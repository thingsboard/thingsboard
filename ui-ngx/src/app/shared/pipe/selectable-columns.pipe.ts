// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Pipe, PipeTransform } from '@angular/core';
import { DisplayColumn } from '@home/components/widget/lib/table-widget.models';

@Pipe({
    name: 'selectableColumns',
    standalone: false
})
export class SelectableColumnsPipe implements PipeTransform {
  transform(allColumns: DisplayColumn[]): DisplayColumn[] {
    return allColumns.filter(column => column.selectable);
  }
}
