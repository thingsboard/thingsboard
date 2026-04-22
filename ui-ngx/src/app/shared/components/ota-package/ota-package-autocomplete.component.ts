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
import {
  ControlValueAccessor,
  FormControl,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup
} from '@angular/forms';
import { merge, Observable, of, shareReplay, Subject } from 'rxjs';
import { catchError, debounceTime, map, switchMap, tap } from 'rxjs/operators';
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
import { getEntityDetailsPageURL, isDefinedAndNotNull, objectRequired } from '@core/utils';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { MatFormFieldAppearance } from '@angular/material/form-field';
import { AutocompleteBaseDirective } from '@shared/components/directives/autocomplete-base.directive';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';

@Component({
    selector: 'tb-ota-package-autocomplete',
    templateUrl: './ota-package-autocomplete.component.html',
    styleUrls: ['./ota-package-autocomplete.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => OtaPackageAutocompleteComponent),
            multi: true
        }],
    standalone: false
})
export class OtaPackageAutocompleteComponent extends AutocompleteBaseDirective implements ControlValueAccessor, OnInit, OnDestroy {

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

  private _useFullEntityId = false;

  get useFullEntityId(): boolean {
    return this._useFullEntityId;
  }

  @Input()
  set useFullEntityId(value: boolean) {
    this._useFullEntityId = coerceBooleanProperty(value);
  }

  @Input()
  showDetailsPageLink = false;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

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

  @ViewChild('autocompleteTrigger') autocompleteTrigger: MatAutocompleteTrigger;

  filteredPackages: Observable<Array<OtaPackageInfo>>;

  packageURL: string;

  usePackageLink = true;

  private authUser: AuthUser;

  private cleanFilteredPackages: Subject<Array<OtaPackageInfo>> = new Subject();

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private entityService: EntityService,
              private otaPackageService: OtaPackageService,
              private fb: UntypedFormBuilder) {
    super();
    this.authUser = getCurrentAuthUser(this.store);
    if (this.authUser.authority === Authority.CUSTOMER_USER) {
      this.usePackageLink = false;
    }
    this.otaPackageFormGroup = this.fb.group({
      packageId: [null, objectRequired()]
    });
  }

  protected getControl() {
    return this.otaPackageFormGroup.get('packageId') as FormControl;
  }

  protected getInput() {
    return this.packageInput;
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
        shareReplay(1)
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

  override reset() {
    this.cleanFilteredPackages.next([]);
    super.reset();
  }

  updateView(value: string | EntityId | null) {
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
      this.searchText = searchText ?? '';
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
