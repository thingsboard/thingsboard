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
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { BaseData } from '@shared/models/base-data';
import { EntityService } from '@core/http/entity.service';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { OtaPackageInfo, OtaUpdateTranslation, OtaUpdateType } from '@shared/models/ota-package.models';
import { OtaPackageService } from '@core/http/ota-package.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData } from "@shared/models/page/page-data";

@Component({
  selector: 'tb-ota-package-autocomplete',
  templateUrl: './ota-package-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => OtaPackageAutocompleteComponent),
    multi: true
  }]
})
export class OtaPackageAutocompleteComponent implements ControlValueAccessor, OnInit {

  otaPackageFormGroup: FormGroup;

  modelValue: string | EntityId | null;

  @Input()
  type = OtaUpdateType.FIRMWARE;

  @Input()
  deviceProfileId: string;

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

  @ViewChild('packageInput', {static: true}) packageInput: ElementRef;

  filteredPackages: Observable<Array<OtaPackageInfo>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private entityService: EntityService,
              private otaPackageService: OtaPackageService,
              private fb: FormBuilder) {
    this.otaPackageFormGroup = this.fb.group({
      packageId: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredPackages = this.otaPackageFormGroup.get('packageId').valueChanges
      .pipe(
        debounceTime(150),
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
        distinctUntilChanged(),
        switchMap(name => this.fetchPackages(name)),
        share()
      );
  }

  ngAfterViewInit(): void {
  }

  getCurrentEntity(): BaseData<EntityId> | null {
    const currentRuleChain = this.otaPackageFormGroup.get('packageId').value;
    if (currentRuleChain && typeof currentRuleChain !== 'string') {
      return currentRuleChain as BaseData<EntityId>;
    } else {
      return null;
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.otaPackageFormGroup.disable({emitEvent: false});
    } else {
      this.otaPackageFormGroup.enable({emitEvent: false});
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  writeValue(value: string | EntityId | null): void {
    this.searchText = '';
    if (value != null && value !== '') {
      let packageId = '';
      if (typeof value === 'string') {
        packageId = value;
      } else if (value.entityType && value.id) {
        packageId = value.id;
      }
      if (packageId !== '') {
        this.entityService.getEntity(EntityType.OTA_PACKAGE, packageId, {ignoreLoading: true, ignoreErrors: true}).subscribe(
          (entity) => {
            this.modelValue = this.useFullEntityId ? entity.id : entity.id.id;
            this.otaPackageFormGroup.get('packageId').patchValue(entity, {emitEvent: false});
          },
          () => {
            this.modelValue = null;
            this.otaPackageFormGroup.get('packageId').patchValue('', {emitEvent: false});
            if (value !== null) {
              this.propagateChange(this.modelValue);
            }
          }
        );
      } else {
        this.modelValue = null;
        this.otaPackageFormGroup.get('packageId').patchValue('', {emitEvent: false});
        this.propagateChange(null);
      }
    } else {
      this.modelValue = null;
      this.otaPackageFormGroup.get('packageId').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.otaPackageFormGroup.get('packageId').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  reset() {
    this.otaPackageFormGroup.get('packageId').patchValue('', {emitEvent: false});
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayPackageFn(packageInfo?: OtaPackageInfo): string | undefined {
    return packageInfo ? `${packageInfo.title} (${packageInfo.version})` : undefined;
  }

  fetchPackages(searchText?: string): Observable<Array<OtaPackageInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(50, 0, searchText, {
      property: 'title',
      direction: Direction.ASC
    });
    return this.otaPackageService.getOtaPackagesInfoByDeviceProfileId(pageLink, this.deviceProfileId, this.type,
                                                                {ignoreLoading: true}).pipe(
      catchError(() => of(emptyPageData<OtaPackageInfo>())),
      map((data) => data && data.data.length ? data.data : null)
    );
  }

  clear() {
    this.otaPackageFormGroup.get('packageId').patchValue('', {emitEvent: false});
    setTimeout(() => {
      this.packageInput.nativeElement.blur();
      this.packageInput.nativeElement.focus();
    }, 0);
  }

  get placeholderText(): string {
    return this.labelText || OtaUpdateTranslation.get(this.type).label;
  }

  get requiredErrorText(): string {
    return this.requiredText || OtaUpdateTranslation.get(this.type).required;
  }

  get notFoundPackage(): string {
    return OtaUpdateTranslation.get(this.type).noFound;
  }

  get notMatchingPackage(): string {
    return OtaUpdateTranslation.get(this.type).noMatching;
  }

  get hintText(): string {
    return OtaUpdateTranslation.get(this.type).hint;
  }

  packageTitleText(firpackageInfomware: OtaPackageInfo): string {
    return `${firpackageInfomware.title} (${firpackageInfomware.version})`;
  }
}
