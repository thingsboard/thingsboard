// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, Inject, OnInit, ViewChild } from '@angular/core';
import {
  AlarmSeverity,
  alarmSeverityTranslations,
  AlarmStatus,
  alarmStatusTranslations,
  DAY,
  DialogComponent
} from '@shared/public-api';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/public-api';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material/chips';
import { AlarmsCountMapping } from './alarms-count-mapping.models';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { Observable, of } from 'rxjs';
import { map, mergeMap, share, startWith } from 'rxjs/operators';

export interface AlarmsCountMappingDialogData {
  isAdd: boolean;
  mapping?: AlarmsCountMapping;
}

@Component({
    selector: 'tb-alarms-count-mapping-dialog',
    templateUrl: './alarms-count-mapping-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class AlarmsCountMappingDialogComponent
  extends DialogComponent<AlarmsCountMappingDialogComponent, AlarmsCountMapping> implements OnInit {

  @ViewChild('alarmStatusInput', {static: false}) alarmStatusInput: ElementRef<HTMLInputElement>;
  @ViewChild('alarmSeverityInput', {static: false}) alarmSeverityInput: ElementRef<HTMLInputElement>;

  statusFormControl: UntypedFormControl;
  severityFormControl: UntypedFormControl;

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  alarmStatusTranslationsMap = alarmStatusTranslations;
  alarmSeverityTranslationsMap = alarmSeverityTranslations;

  alarmsCountMappingFormGroup: UntypedFormGroup;

  isAdd: boolean;
  mapping: AlarmsCountMapping;
  specifyInterval: boolean;

  displayStatusFn = this.displayStatus.bind(this);
  private alarmStatusList: AlarmStatus[] = [];
  filteredAlarmStatus: Observable<Array<AlarmStatus>>;
  alarmStatusSearchText = '';

  displaySeverityFn = this.displaySeverity.bind(this);
  private alarmSeverityList: AlarmSeverity[] = [];
  filteredAlarmSeverity: Observable<Array<AlarmSeverity>>;
  alarmSeveritySearchText = '';

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmsCountMappingDialogData,
              public dialogRef: MatDialogRef<AlarmsCountMappingDialogComponent, AlarmsCountMapping>,
              public fb: UntypedFormBuilder,
              private translate: TranslateService) {
    super(store, router, dialogRef);
    this.isAdd = data.isAdd;
    this.mapping = data.mapping;
    if (this.isAdd && !this.mapping) {
      this.mapping = {
        latestInterval: 0,
        target: ''
      }
    }
    this.specifyInterval = this.mapping.latestInterval > 0;

    for (const field of Object.keys(AlarmStatus)) {
      this.alarmStatusList.push(AlarmStatus[field]);
    }
    for (const field of Object.keys(AlarmSeverity)) {
      this.alarmSeverityList.push(AlarmSeverity[field]);
    }
    this.statusFormControl = new UntypedFormControl('');
    this.severityFormControl = new UntypedFormControl('');
    this.filteredAlarmStatus = this.statusFormControl.valueChanges
      .pipe(
        startWith(''),
        map((value) => value ? value : ''),
        mergeMap(name => this.fetchAlarmStatus(name)),
        share()
      );
    this.filteredAlarmSeverity = this.severityFormControl.valueChanges
      .pipe(
        startWith(''),
        map((value) => value ? value : ''),
        mergeMap(name => this.fetchAlarmSeverity(name)),
        share()
      );
  }

  ngOnInit(): void {
    this.alarmsCountMappingFormGroup = this.fb.group({
      target: [this.mapping.target, [Validators.required]],
      latestInterval: [this.mapping.latestInterval, []],
      typesList: [this.mapping.typesList ? this.mapping.typesList : [], []],
      severityList: [this.mapping.severityList ? this.mapping.severityList : [], []],
      statusList: [this.mapping.statusList ? this.mapping.statusList : [], []]
    });
    if (!this.specifyInterval) {
      this.alarmsCountMappingFormGroup.get('latestInterval').disable({emitEvent: false});
    }
  }

  specifyIntervalChange() {
    if (this.specifyInterval) {
      this.alarmsCountMappingFormGroup.get('latestInterval').setValue(DAY, {emitEvent: true});
      this.alarmsCountMappingFormGroup.get('latestInterval').enable({emitEvent: true});
    } else {
      this.alarmsCountMappingFormGroup.get('latestInterval').setValue(0, {emitEvent: true});
      this.alarmsCountMappingFormGroup.get('latestInterval').disable({emitEvent: true});
    }
    this.alarmsCountMappingFormGroup.get('latestInterval').markAsDirty();
  }

  removeKey(key: any, keysField: string): void {
    const keys: any[] = this.alarmsCountMappingFormGroup.get(keysField).value;
    const index = keys.indexOf(key);
    if (index >= 0) {
      keys.splice(index, 1);
      this.alarmsCountMappingFormGroup.get(keysField).setValue(keys, {emitEvent: true});
      this.alarmsCountMappingFormGroup.get(keysField).markAsDirty();
    }
  }

  addKey(event: MatChipInputEvent, keysField: string): void {
    const input = event.input;
    let value = event.value;
    if ((value || '').trim()) {
      value = value.trim();
      let keys: string[] = this.alarmsCountMappingFormGroup.get(keysField).value;
      if (!keys || keys.indexOf(value) === -1) {
        if (!keys) {
          keys = [];
        }
        keys.push(value);
        this.alarmsCountMappingFormGroup.get(keysField).setValue(keys, {emitEvent: true});
        this.alarmsCountMappingFormGroup.get(keysField).markAsDirty();
      }
    }
    if (input) {
      input.value = '';
    }
  }

  displayStatus(status?: AlarmStatus): string | undefined {
    return status ? this.translate.instant(alarmStatusTranslations.get(status)) : undefined;
  }

  onAlarmStatusInputFocus() {
    this.statusFormControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
  }

  private getAlarmStatusList(): Array<AlarmStatus> {
    return this.alarmStatusList.filter((listItem) => {
      return this.alarmsCountMappingFormGroup.get('statusList').value.indexOf(listItem) === -1;
    });
  }

  private fetchAlarmStatus(searchText?: string): Observable<Array<AlarmStatus>> {
    const alarmStatusList = this.getAlarmStatusList();
    this.alarmStatusSearchText = searchText;
    if (this.alarmStatusSearchText && this.alarmStatusSearchText.length) {
      const search = this.alarmStatusSearchText.toUpperCase();
      return of(alarmStatusList.filter(field =>
        this.translate.instant(alarmStatusTranslations.get(AlarmStatus[field])).toUpperCase().includes(search)));
    } else {
      return of(alarmStatusList);
    }
  }

  alarmStatusSelected(event: MatAutocompleteSelectedEvent): void {
    this.addAlarmStatus(event.option.value);
    this.clearAlarmStatus('');
  }

  addAlarmStatus(status: AlarmStatus): void {
    let alarmStatusList: AlarmStatus[] = this.alarmsCountMappingFormGroup.get('statusList').value;
    if (!alarmStatusList) {
      alarmStatusList = [];
    }
    const index = alarmStatusList.indexOf(status);
    if (index === -1) {
      alarmStatusList.push(status);
      this.alarmsCountMappingFormGroup.get('statusList').setValue(alarmStatusList);
      this.alarmsCountMappingFormGroup.get('statusList').markAsDirty();
    }
  }

  clearAlarmStatus(value: string = '') {
    this.alarmStatusInput.nativeElement.value = value;
    this.statusFormControl.patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.alarmStatusInput.nativeElement.blur();
      this.alarmStatusInput.nativeElement.focus();
    }, 0);
  }

  displaySeverity(severity?: AlarmSeverity): string | undefined {
    return severity ? this.translate.instant(alarmSeverityTranslations.get(severity)) : undefined;
  }

  onAlarmSeverityInputFocus() {
    this.severityFormControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
  }

  private getAlarmSeverityList(): Array<AlarmSeverity> {
    return this.alarmSeverityList.filter((listItem) => {
      return this.alarmsCountMappingFormGroup.get('severityList').value.indexOf(listItem) === -1;
    });
  }

  private fetchAlarmSeverity(searchText?: string): Observable<Array<AlarmSeverity>> {
    const alarmSeverityList = this.getAlarmSeverityList();
    this.alarmSeveritySearchText = searchText;
    if (this.alarmSeveritySearchText && this.alarmSeveritySearchText.length) {
      const search = this.alarmSeveritySearchText.toUpperCase();
      return of(alarmSeverityList.filter(field =>
        this.translate.instant(alarmSeverityTranslations.get(AlarmSeverity[field])).toUpperCase().includes(search)));
    } else {
      return of(alarmSeverityList);
    }
  }

  alarmSeveritySelected(event: MatAutocompleteSelectedEvent): void {
    this.addAlarmSeverity(event.option.value);
    this.clearAlarmSeverity('');
  }

  addAlarmSeverity(severity: AlarmSeverity): void {
    let alarmSeverityList: AlarmSeverity[] = this.alarmsCountMappingFormGroup.get('severityList').value;
    if (!alarmSeverityList) {
      alarmSeverityList = [];
    }
    const index = alarmSeverityList.indexOf(severity);
    if (index === -1) {
      alarmSeverityList.push(severity);
      this.alarmsCountMappingFormGroup.get('severityList').setValue(alarmSeverityList);
      this.alarmsCountMappingFormGroup.get('severityList').markAsDirty();
    }
  }

  clearAlarmSeverity(value: string = '') {
    this.alarmSeverityInput.nativeElement.value = value;
    this.severityFormControl.patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.alarmSeverityInput.nativeElement.blur();
      this.alarmSeverityInput.nativeElement.focus();
    }, 0);
  }

  cancel(): void {
    this.dialogRef.close();
  }

  save(): void {
    this.mapping = this.alarmsCountMappingFormGroup.value;
    this.dialogRef.close(this.mapping);
  }

}
