///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, Inject, InjectionToken } from '@angular/core';
import {
  AlarmSearchStatus,
  alarmSearchStatusTranslations,
  AlarmSeverity,
  alarmSeverityTranslations
} from '@shared/models/alarm.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { OverlayRef } from '@angular/cdk/overlay';

export const ALARM_FILTER_PANEL_DATA = new InjectionToken<any>('AlarmFilterPanelData');

export interface AlarmFilterPanelData {
  statusList: AlarmSearchStatus[];
  severityList: AlarmSeverity[];
  typeList: string[];
}

@Component({
  selector: 'tb-alarm-filter-panel',
  templateUrl: './alarm-filter-panel.component.html',
  styleUrls: ['./alarm-filter-panel.component.scss']
})
export class AlarmFilterPanelComponent {

  readonly separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  alarmFilterFormGroup: FormGroup;

  result: AlarmFilterPanelData;

  alarmSearchStatuses = [AlarmSearchStatus.ACTIVE,
    AlarmSearchStatus.CLEARED,
    AlarmSearchStatus.ACK,
    AlarmSearchStatus.UNACK];

  alarmSearchStatusTranslationMap = alarmSearchStatusTranslations;

  alarmSeverities = Object.keys(AlarmSeverity);
  alarmSeverityEnum = AlarmSeverity;

  alarmSeverityTranslationMap = alarmSeverityTranslations;

  constructor(@Inject(ALARM_FILTER_PANEL_DATA)
              public data: AlarmFilterPanelData,
              public overlayRef: OverlayRef,
              private fb: FormBuilder) {
    this.alarmFilterFormGroup = this.fb.group(
      {
        alarmStatusList: [this.data.statusList],
        alarmSeverityList: [this.data.severityList],
        alarmTypeList: [this.data.typeList]
      }
    );
  }

  public alarmTypeList(): string[] {
    return this.alarmFilterFormGroup.get('alarmTypeList').value;
  }

  public removeAlarmType(type: string): void {
    const types: string[] = this.alarmFilterFormGroup.get('alarmTypeList').value;
    const index = types.indexOf(type);
    if (index >= 0) {
      types.splice(index, 1);
      this.alarmFilterFormGroup.get('alarmTypeList').setValue(types);
      this.alarmFilterFormGroup.get('alarmTypeList').markAsDirty();
    }
  }

  public addAlarmType(event: MatChipInputEvent): void {
    const input = event.input;
    const value = event.value;

    const types: string[] = this.alarmFilterFormGroup.get('alarmTypeList').value;

    if ((value || '').trim()) {
      types.push(value.trim());
      this.alarmFilterFormGroup.get('alarmTypeList').setValue(types);
      this.alarmFilterFormGroup.get('alarmTypeList').markAsDirty();
    }

    if (input) {
      input.value = '';
    }
  }

  update() {
    this.result = {
      statusList: this.alarmFilterFormGroup.get('alarmStatusList').value,
      severityList: this.alarmFilterFormGroup.get('alarmSeverityList').value,
      typeList: this.alarmFilterFormGroup.get('alarmTypeList').value
    };
    this.overlayRef.dispose();
  }

  cancel() {
    this.overlayRef.dispose();
  }
}

