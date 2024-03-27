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
import { MatDialog } from '@angular/material/dialog';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKey, DatasourceType, JsonSettingsSchema, widgetType } from '@shared/models/widget.models';
import { dataKeyRowValidator, dataKeyValid } from '@home/components/widget/config/basic/common/data-key-row.component';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { UtilsService } from '@core/services/utils.service';
import { DataKeysCallbacks } from '@home/components/widget/config/data-keys.component.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { WidgetService } from '@core/http/widget.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { ColorSettings } from '@shared/models/widget-settings.models';
import { TranslateService } from '@ngx-translate/core';
import { isDefinedAndNotNull } from '@core/utils';
import { ValueType, valueTypesMap } from '@shared/models/constants';
import { MappingKeysType } from '@home/components/widget/lib/gateway/gateway-widget.models';

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
  keys: string;

  @Input()
  keysType: string;

  @Input()
  @coerceBoolean()
  rawData = false;

  @Input()
  popover: TbPopoverComponent<MappingDataKeysPanelComponent>;

  @Output()
  keysDataApplied = new EventEmitter<Array<any>>();

  valueTypeKeys = Object.keys(ValueType);

  MappingKeysType = MappingKeysType;

  valueTypeEnum = ValueType;

  valueTypes = valueTypesMap;

  dataKeyType: DataKeyType;

  keysListFormArray: UntypedFormArray;

  errorText = '';

  // get dragEnabled(): boolean {
  //   return this.keysFormArray().controls.length > 1;
  // }

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    this.keysListFormArray = this.prepareKeysFormArray(this.keys)
    // this.keysListFormArray.valueChanges.subscribe(
    //   () => {
    //     let keys: DataKey[] = this.keysListFormArray.get('keys').value;
    //     if (keys) {
    //       keys = keys.filter(k => dataKeyValid(k));
    //     }
    //   }
    // );
  }

  // keyDrop(event: CdkDragDrop<string[]>) {
  //   const keysArray = this.keysListFormGroup.get('keys') as UntypedFormArray;
  //   const key = keysArray.at(event.previousIndex);
  //   keysArray.removeAt(event.previousIndex);
  //   keysArray.insert(event.currentIndex, key);
  // }

  // keysFormArray(): UntypedFormArray {
  //   return this.keysListFormArray;
  // }

  trackByKey(index: number, keyControl: AbstractControl): any {
    return keyControl;
  }

  removeKey(index: number) {
    this.keysListFormArray.removeAt(index);
  }

  addKey(): void {
    const dataKeyFormGroup = this.fb.group({
      key: ['', Validators.required],
      value: ['', Validators.required]
    });
    if (this.keysType !== MappingKeysType.CUSTOM) {
      dataKeyFormGroup.addControl('type', this.fb.control(this.rawData ? 'raw' : ValueType.STRING));
      // if (this.rawData) {
      //   dataKeyFormGroup.get('type').disable();
      // }
    }
    // const keyControl = this.fb.control(dataKey, [this.dataKeyValidator]);
    this.keysListFormArray.push(dataKeyFormGroup);
  }

  deleteKey($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.keysListFormArray.removeAt(index);
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

  private prepareKeysFormArray(keys: any): UntypedFormArray {
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
          key: [key, Validators.required],
          value: [value, Validators.required],
          type: [type, []]
        });
        keysControlGroups.push(dataKeyFormGroup);
      });
      console.log(keysControlGroups, 'keysControlGroups');
    }
    return this.fb.array(keysControlGroups);
  }

  valueTitle(value: any): any {
    if (isDefinedAndNotNull(value)) {
      if (typeof value === 'object') {
        return JSON.stringify(value);
      }
      return value;
    }
    return '';
  }

  // private dataKeyValidator(control: AbstractControl): ValidationErrors | null  {
  //   const dataKey: any = control.value;
  //   if (!(!!dataKey && !!dataKey.type && !!dataKey.name)) {
  //     return {
  //       required: true
  //     };
  //   }
  //   return null;
  // }

}
