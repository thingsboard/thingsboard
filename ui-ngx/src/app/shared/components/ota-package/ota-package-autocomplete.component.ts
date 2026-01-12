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

import { Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { merge, Observable, of, Subject } from 'rxjs';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { OtaPackageInfo, OtaUpdateTranslation, OtaUpdateType } from '@shared/models/ota-package.models';
import { OtaPackageService } from '@core/http/ota-package.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData } from '@shared/models/page/page-data';
import { getEntityDetailsPageURL, isDefinedAndNotNull } from '@core/utils';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';

@Component({
  selector: 'tb-ota-package-autocomplete',
  templateUrl: './ota-package-autocomplete.component.html',
  styleUrls: ['./ota-package-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => OtaPackageAutocompleteComponent),
    multi: true
  }]
})
export class OtaPackageAutocompleteComponent implements ControlValueAccessor, OnInit, OnDestroy {

  otaPackageFormGroup: UntypedFormGroup;

  modelValue: string | EntityId | null;

  private otaUpdateType: OtaUpdateType = OtaUpdateType.FIRMWARE;

  get type(): OtaUpdateType {
    return this.otaUpdateType;
  }

  @Input()
  set type(value ) {
    this.otaUpdateType = value ? value : OtaUpdateType.FIRMWARE;
    this.reset();
  }

  private deviceProfileIdValue: string;

  get deviceProfileId(): string {
    return this.deviceProfileIdValue;
  }

  @Input()
  set deviceProfileId(value: string) {
    if (this.deviceProfileIdValue !== value) {
      if (this.deviceProfileIdValue) {
        this.reset();
      }
      this.deviceProfileIdValue = value;
    }
  }

  @Input()
  labelText: string;

  @Input()
  requiredText: string;

  @Input()
  useFullEntityId = false;

  @Input()
  showDetailsPageLink = false;

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
  packageURL: string;

  usePackageLink = true;

  private authUser: AuthUser;

  private dirty = false;
  private cleanFilteredPackages: Subject<Array<OtaPackageInfo>> = new Subject();

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private entityService: EntityService,
              private otaPackageService: OtaPackageService,
              private fb: UntypedFormBuilder) {
    this.authUser = getCurrentAuthUser(this.store);
    if (this.authUser.authority === Authority.CUSTOMER_USER) {
      this.usePackageLink = false;
    }
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
    const getPackages = this.otaPackageFormGroup.get('packageId').valueChanges
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
        switchMap(name => this.fetchPackages(name)),
        share()
      );

    this.filteredPackages = merge(this.cleanFilteredPackages, getPackages);
  }

  ngOnDestroy() {
    this.cleanFilteredPackages.complete();
    this.cleanFilteredPackages = null;
  }

  getCurrentEntity(): OtaPackageInfo | null {
    const currentPackage = this.otaPackageFormGroup.get('packageId').value;
    if (currentPackage && typeof currentPackage !== 'string') {
      return currentPackage as OtaPackageInfo;
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
            if (this.usePackageLink) {
              this.packageURL = getEntityDetailsPageURL(entity.id.id, EntityType.OTA_PACKAGE);
            }
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
    this.cleanFilteredPackages.next([]);
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
    if (isDefinedAndNotNull(this.deviceProfileId)) {
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
    } else {
      return of([]);
    }
  }

  clear() {
    this.otaPackageFormGroup.get('packageId').patchValue('');
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
