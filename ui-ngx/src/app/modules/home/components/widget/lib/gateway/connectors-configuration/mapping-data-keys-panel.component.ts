///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';
import {
  AbstractControl,
  UntypedFormArray,
  UntypedFormBuilder,
  Validators
} from '@angular/forms';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { isDefinedAndNotNull } from '@core/utils';
import {
  MappingDataKey,
  MappingKeysType,
  MappingValueType,
  mappingValueTypesMap,
  noLeadTrailSpacesRegex
} from '@home/components/widget/lib/gateway/gateway-widget.models';

@Component({
  selector: 'tb-mapping-data-keys-panel',
  templateUrl: './mapping-data-keys-panel.component.html',
  styleUrls: ['./mapping-data-keys-panel.component.scss'],
  providers: []
})
export class MappingDataKeysPanelComponent extends PageComponent implements OnInit {

  @Input()
  panelTitle: string;

  @Input()
  addKeyTitle: string;

  @Input()
  deleteKeyTitle: string;

  @Input()
  noKeysText: string;

  @Input()
  keys: Array<MappingDataKey> | {[key: string]: any};

  @Input()
  keysType: string;

  @Input()
  @coerceBoolean()
  rawData = false;

  @Input()
  popover: TbPopoverComponent<MappingDataKeysPanelComponent>;

  @Output()
  keysDataApplied = new EventEmitter<Array<MappingDataKey> | {[key: string]: any}>();

  valueTypeKeys = Object.values(MappingValueType);

  MappingKeysType = MappingKeysType;

  valueTypeEnum = MappingValueType;

  valueTypes = mappingValueTypesMap;

  dataKeyType: DataKeyType;

  keysListFormArray: UntypedFormArray;

  errorText = '';

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    this.keysListFormArray = this.prepareKeysFormArray(this.keys)
  }

  trackByKey(index: number, keyControl: AbstractControl): any {
    return keyControl;
  }

  addKey(): void {
    const dataKeyFormGroup = this.fb.group({
      key: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      value: ['', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]]
    });
    if (this.keysType !== MappingKeysType.CUSTOM) {
      dataKeyFormGroup.addControl('type', this.fb.control(this.rawData ? 'raw' : MappingValueType.STRING));
    }
    this.keysListFormArray.push(dataKeyFormGroup);
  }

  deleteKey($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.keysListFormArray.removeAt(index);
    this.keysListFormArray.markAsDirty();
  }

  cancel() {
    this.popover?.hide();
  }

  applyKeysData() {
    let keys = this.keysListFormArray.value;
    if (this.keysType === MappingKeysType.CUSTOM) {
      keys = {};
      for (let key of this.keysListFormArray.value) {
        keys[key.key] = key.value;
      }
    }
    this.keysDataApplied.emit(keys);
  }

  private prepareKeysFormArray(keys: Array<MappingDataKey> | {[key: string]: any}): UntypedFormArray {
    const keysControlGroups: Array<AbstractControl> = [];
    if (keys) {
      if (this.keysType === MappingKeysType.CUSTOM) {
        keys = Object.keys(keys).map(key => {
          return {key, value: keys[key], type: ''};
        });
      }
      keys.forEach((keyData) => {
        const { key, value, type } = keyData;
        const dataKeyFormGroup = this.fb.group({
          key: [key, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
          value: [value, [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
          type: [type, []]
        });
        keysControlGroups.push(dataKeyFormGroup);
      });
    }
    return this.fb.array(keysControlGroups);
  }

  valueTitle(value: any): string {
    if (isDefinedAndNotNull(value)) {
      if (typeof value === 'object') {
        return JSON.stringify(value);
      }
      return value;
    }
    return '';
  }

}
