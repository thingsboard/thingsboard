// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  inject,
  KeyValueChangeRecord,
  KeyValueChanges,
  KeyValueDiffer,
  KeyValueDiffers,
  Pipe,
  PipeTransform
} from '@angular/core';
import { KeyValue } from '@angular/common';
import { isDefinedAndNotNull } from '@core/utils';

@Pipe({
  name: 'keyValueIsNotEmpty',
  pure: false,
  standalone: true,
})
export class KeyValueIsNotEmptyPipe implements PipeTransform {
  private differs: KeyValueDiffers = inject(KeyValueDiffers);
  private differ!: KeyValueDiffer<string, unknown>;
  private keyValues: Array<KeyValue<string, unknown>> = [];

  // This is a custom implementation of angular keyvalue pipe
  // https://github.com/angular/angular/blob/main/packages/common/src/pipes/keyvalue_pipe.ts
  transform(
    input: Record<string, unknown>,
  ): Array<KeyValue<string, unknown>> {
    if (!input || (!(input instanceof Map) && typeof input !== 'object')) {
      return null;
    }

    this.differ ??= this.differs.find(input).create();

    const differChanges: KeyValueChanges<string, unknown> | null = this.differ.diff(input);

    if (differChanges) {
      this.keyValues = [];
      differChanges.forEachItem((r: KeyValueChangeRecord<string, unknown>) => {
        if (isDefinedAndNotNull(r.currentValue)) {
          this.keyValues.push(this.makeKeyValuePair(r.key, r.currentValue!));
        }
      });
    }

    return this.keyValues;
  }

  private makeKeyValuePair(key: string, value: unknown): KeyValue<string, unknown> {
    return {key, value};
  }
}
