///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

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
