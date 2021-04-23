///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, mergeMap, share, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { BaseData } from '@shared/models/base-data';
import { EntityService } from '@core/http/entity.service';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { FirmwareInfo } from '@shared/models/firmware.models';
import { FirmwareService } from '@core/http/firmware.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';

@Component({
  selector: 'tb-firmware-autocomplete',
  templateUrl: './firmware-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => FirmwareAutocompleteComponent),
    multi: true
  }]
})
export class FirmwareAutocompleteComponent implements ControlValueAccessor, OnInit {

  firmwareFormGroup: FormGroup;

  modelValue: string | null;

  @Input()
  labelText: string;

  @Input()
  requiredText: string;

  @Input()
  useFullEntityId = false;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  @ViewChild('firmwareInput', {static: true}) firmwareInput: ElementRef;
  @ViewChild('firmwareInput', {read: MatAutocompleteTrigger}) firmwareAutocomplete: MatAutocompleteTrigger;

  filteredFirmwares: Observable<Array<FirmwareInfo>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private entityService: EntityService,
              private firmwareService: FirmwareService,
              private fb: FormBuilder) {
    this.firmwareFormGroup = this.fb.group({
      firmwareId: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredFirmwares = this.firmwareFormGroup.get('firmwareId').valueChanges
      .pipe(
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = this.useFullEntityId ? value.id : value.id.id;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.title) : ''),
        mergeMap(name => this.fetchFirmware(name)),
        share()
      );
  }

  ngAfterViewInit(): void {
  }

  getCurrentEntity(): BaseData<EntityId> | null {
    const currentRuleChain = this.firmwareFormGroup.get('firmwareId').value;
    if (currentRuleChain && typeof currentRuleChain !== 'string') {
      return currentRuleChain as BaseData<EntityId>;
    } else {
      return null;
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.firmwareFormGroup.disable({emitEvent: false});
    } else {
      this.firmwareFormGroup.enable({emitEvent: false});
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  writeValue(value: string | EntityId | null): void {
    this.searchText = '';
    if (value != null && value !== '') {
      let firmwareId = '';
      if (typeof value === 'string') {
        firmwareId = value;
      } else if (value.entityType && value.id) {
        firmwareId = value.id;
      }
      if (firmwareId !== '') {
        this.entityService.getEntity(EntityType.FIRMWARE, firmwareId, {ignoreLoading: true, ignoreErrors: true}).subscribe(
          (entity) => {
            this.modelValue = entity.id.id;
            this.firmwareFormGroup.get('firmwareId').patchValue(entity, {emitEvent: false});
          },
          () => {
            this.modelValue = null;
            this.firmwareFormGroup.get('firmwareId').patchValue('', {emitEvent: false});
            if (value !== null) {
              this.propagateChange(this.modelValue);
            }
          }
        );
      } else {
        this.modelValue = null;
        this.firmwareFormGroup.get('firmwareId').patchValue('', {emitEvent: false});
      }
    } else {
      this.modelValue = null;
      this.firmwareFormGroup.get('firmwareId').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.firmwareFormGroup.get('firmwareId').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  reset() {
    this.firmwareFormGroup.get('firmwareId').patchValue('', {emitEvent: false});
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayFirmwareFn(firmware?: FirmwareInfo): string | undefined {
    return firmware ? `${firmware.title} (${firmware.version})` : undefined;
  }

  fetchFirmware(searchText?: string): Observable<Array<FirmwareInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(50, 0, searchText, {
      property: 'title',
      direction: Direction.ASC
    });
    return this.firmwareService.getFirmwares(pageLink, true, {ignoreLoading: true}).pipe(
      map((data) => data && data.data.length ? data.data : null)
    );
  }

  clear() {
    this.firmwareFormGroup.get('firmwareId').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.firmwareInput.nativeElement.blur();
      this.firmwareInput.nativeElement.focus();
    }, 0);
  }

  get placeholderText(): string {
    return this.labelText || 'firmware.firmware';
  }

  get requiredErrorText(): string {
    return this.requiredText || 'firmware.firmware-required';
  }

  firmwareTitleText(firmware: FirmwareInfo): string {
    return `${firmware.title} (${firmware.version})`;
  }
}
