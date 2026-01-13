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

import { Component, ElementRef, EventEmitter, forwardRef, Input, OnInit, Output, ViewChild } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { ENTER } from '@angular/cdk/keycodes';
import { Observable, of } from 'rxjs';
import { filter, map, mergeMap, reduce, share, switchMap, tap } from 'rxjs/operators';
import { EntityService } from '@core/http/entity.service';
import { EntityType } from '@shared/models/entity-type.models';
import { Device } from '@shared/models/device.models';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { DeviceService } from '@core/http/device.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Component({
  selector: 'tb-entity-gateway-select',
  templateUrl: './entity-gateway-select.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntityGatewaySelectComponent),
    multi: true
  }]
})

export class EntityGatewaySelectComponent implements ControlValueAccessor, OnInit {
  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  set newGatewayType(value: string){
    this.gatewayType = value;
  }

  @Input()
  deviceName: string;

  @Input()
  isStateForm: boolean;

  @Output()
  private gatewayNameExist = new EventEmitter();

  constructor(private store: Store<AppState>,
              private entityService: EntityService,
              private dialogService: DialogService,
              private deviceService: DeviceService,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
  }

  private gatewayType = 'Gateway';
  private dirty: boolean;
  private requiredValue: boolean;
  private gatewayList: Array<Device>;

  searchText = '';
  filteredGateways: Observable<Array<Device>>;
  selectDeviceGatewayFormGroup: UntypedFormGroup;
  modelValue: string | null;

  @ViewChild('deviceGatewayInput', {static: true}) deviceGatewayInput: ElementRef<HTMLInputElement>;
  private propagateChange = (v: any) => { };

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.selectDeviceGatewayFormGroup = this.fb.group({
      gateway: this.fb.control({value: null, disabled: this.isStateForm})
    });
    this.loadGatewayList();
    this.filteredGateways = this.selectDeviceGatewayFormGroup.get('gateway').valueChanges
      .pipe(
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.id.id;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchGateway(name) ),
        share()
      );
  }

  fetchGateway(searchText?: string): Observable<Array<Device>> {
    this.searchText = searchText;
    let result = [];
    if (searchText && searchText.length) {
      result = this.gatewayList.filter((gateway) => gateway.name.toLowerCase().includes(searchText.toLowerCase()));
    } else {
      result = this.gatewayList;
    }
    return of(result);
  }

  onFocus() {
    if (this.dirty) {
      this.selectDeviceGatewayFormGroup.get('gateway').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  displayGatewayFn(gateway?: Device): string | undefined {
    return gateway ? gateway.name : undefined;
  }

  setDisabledState(isDisabled: boolean): void {
  }

  writeValue(value: string | null): void {
    if(value === null){
      this.searchText = '';
      this.selectDeviceGatewayFormGroup.get('gateway').patchValue('', {emitEvent: false});
      this.dirty = true;
    }
  }

  clear(value: string = '', hideList?: boolean) {
    this.searchText = value;
    this.selectDeviceGatewayFormGroup.get('gateway').patchValue(value, {emitEvent: true});
    if(!hideList) {
      setTimeout(() => {
        this.deviceGatewayInput.nativeElement.blur();
        this.deviceGatewayInput.nativeElement.focus();
      }, 0);
    }
  }

  textIsNotEmpty(text: string): boolean {
    return !!text && text.length > 0;
  }

  gatewayNameEnter($event: KeyboardEvent) {
    if ($event.keyCode === ENTER) {
      if (!this.modelValue) {
        this.createGateway($event, this.searchText);
      }
    }
  }

  createGateway($event: Event, gatewayName: string) {
    $event.preventDefault();
    $event.stopPropagation();
    const title = this.translate.instant('gateway.create-new-gateway');
    const content = this.translate.instant('gateway.create-new-gateway-text', {gatewayName});
    this.dialogService.confirm(title, content, null, null, true).subscribe(value => {
      if(value){
        this.createDeviceGateway(gatewayName);
      } else {
        this.clear('', true);
      }
    });
  }

  private createDeviceGateway(gatewayName: string){
    this.deviceService.findByName(gatewayName, {ignoreErrors: true}).subscribe(value => {
      this.gatewayNameExist.emit(gatewayName)
    }, () => {
      const newGateway: Device = {
        name: gatewayName,
        label: null,
        type: this.gatewayType,
        additionalInfo: {
          gateway: true
        }
      };

      this.deviceService.saveDevice(newGateway).subscribe(
        (device) => {
          this.searchText = '';
          this.gatewayList.push(device);
          this.selectDeviceGatewayFormGroup.get('gateway').patchValue(device, {emitEvent: true});
        }
      );
    })
  }

  private loadGatewayList(): void {
    let listObservable: Observable<any[]>;
    if (getCurrentAuthUser(this.store).authority === Authority.SYS_ADMIN) {
      listObservable = of([]);
    } else {
      const entityNameFilter = this.isStateForm && this.deviceName ? this.deviceName : '';
      listObservable = this.entityService.getEntitiesByNameFilter(EntityType.DEVICE, entityNameFilter,
        -1, '', {ignoreLoading: true});
    }
    listObservable.pipe(
      map((devices) => devices ? devices.filter((device) =>
        (device as Device)?.additionalInfo?.gateway): []),
    ).subscribe((devices) => {
      this.gatewayList = devices;
      if (!this.searchText) {
        if (this.gatewayList.length) {
          let foundGateway: Device = null;
          if (this.deviceName) {
            foundGateway = this.gatewayList.find((gateway) => gateway.name === this.deviceName);
          }
          if (!foundGateway) {
            foundGateway = this.gatewayList[0];
          }
          if (foundGateway) {
            this.selectDeviceGatewayFormGroup.get('gateway').patchValue(foundGateway, {emitEvent: true});
          }
        }
      }
    })
  }

  private updateView(modelValue: any) {
    this.propagateChange(modelValue);
  }
}
