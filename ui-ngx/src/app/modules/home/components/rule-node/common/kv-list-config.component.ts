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

import { Component, forwardRef, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { KvMapConfigOldComponent } from '@home/components/rule-node/common/kv-map-config-old.component';

@Component({
  selector: 'tb-kv-list-config',
  templateUrl: './kv-map-config-old.component.html',
  styleUrls: ['./kv-map-config-old.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => KvListConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => KvListConfigComponent),
      multi: true,
    }
  ]
})
export class KvListConfigComponent extends KvMapConfigOldComponent implements ControlValueAccessor, OnInit, Validator{

  override writeValue(kvList: any): void {
    const keyValsControls: Array<AbstractControl> = [];
    if (Array.isArray(kvList) && kvList.length > 0) {
      for (const property of kvList) {
        keyValsControls.push(this.fb.group({
          key: [property.key, [Validators.required]],
          value: [property.value, [Validators.required]]
        }))
      }
    }
    this.kvListFormGroup.setControl('keyVals', this.fb.array(keyValsControls), {emitEvent: false});
  }

  public override validate() {
    const kvList: { key: string; value: string }[] = this.kvListFormGroup.get('keyVals').value;
    if (!kvList.length && this.required) {
      return {
        kvMapRequired: true
      };
    }
    if (!this.kvListFormGroup.valid) {
      return {
        kvFieldsRequired: true
      };
    }
    return null;
  }

  protected override updateModel() {
    const kvList: { key: string; value: string }[] = this.kvListFormGroup.get('keyVals').value;
    if (this.required && !kvList.length || !this.kvListFormGroup.valid) {
      this.propagateChange(null);
    } else {
      this.propagateChange(kvList);
    }
  }
}
