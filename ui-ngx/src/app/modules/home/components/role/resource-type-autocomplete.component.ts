// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR,
  Validator, AbstractControl, NG_VALIDATORS, ValidationErrors
} from '@angular/forms';
import { Observable, of } from 'rxjs';
import {
  debounceTime,
  distinctUntilChanged,
  map,
  publishReplay,
  refCount,
  startWith,
  switchMap,
  tap
} from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Resource, resourceTypeTranslationMap } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';

interface ResourceTypeInfo {
  name: string;
  value: Resource;
}

@Component({
    selector: 'tb-resource-type-autocomplete',
    templateUrl: './resource-type-autocomplete.component.html',
    styleUrls: ['./resource-type-autocomplete.component.scss'],
    providers: [
      {
        provide: NG_VALUE_ACCESSOR,
        useExisting: forwardRef(() => ResourceTypeAutocompleteComponent),
        multi: true
      },
      {
        provide: NG_VALIDATORS,
        useExisting: forwardRef(() => ResourceTypeAutocompleteComponent),
        multi: true
      }
    ],
    standalone: false
})
export class ResourceTypeAutocompleteComponent implements ControlValueAccessor, OnInit, Validator {

  resourceTypeFormGroup: UntypedFormGroup;

  modelValue: Resource | null;

  get resourceTypeControl(): AbstractControl {
    return this.resourceTypeFormGroup.get('resourceType');
  }

  get errorMessage(): string | null {
    if (this.resourceTypeControl.hasError('required')) {
      return 'permission.resource.resource-required';
    }
    if (this.resourceTypeControl.hasError('resourceDuplicate')) {
      return 'permission.resource-duplicate';
    }
    return null;
  }

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

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  label: string;

  @Input()
  set allowedResources(values: Resource[]) {
    if (values) {
      this.resources = values.map(resource => ({
        name: this.translate.instant(resourceTypeTranslationMap.get(resource)),
        value: resource
      })).sort(this.sortResource);
    } else {
      this.resources = this.buildDefaultResources();
    }
  }

  @Input()
  @coerceBoolean()
  inlineField: boolean;

  @ViewChild('resourceTypeInput', {static: true}) resourceTypeInput: ElementRef<HTMLInputElement>;

  filteredResources: Observable<Array<ResourceTypeInfo>>;
  resources: Array<ResourceTypeInfo> = this.buildDefaultResources();

  searchText = '';

  private onTouched = () => {};

  private dirty = false;

  private resourceDuplicate = false;

  private propagateChange = (v: any) => { };

  private onValidatorChange: () => void;

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private userPermissionsService: UserPermissionsService,
              private fb: UntypedFormBuilder) {
    this.resourceTypeFormGroup = this.fb.group({
      resourceType: [null, () => this.resourceDuplicate ? { resourceDuplicate: true } : null]
    });
  }

  private buildDefaultResources(): Array<ResourceTypeInfo> {
    return this.userPermissionsService
      .getAllowedResources().map(resource => ({
        name: this.translate.instant(resourceTypeTranslationMap.get(resource)),
        value: resource
      })).sort(this.sortResource);
  }

  private sortResource(a: ResourceTypeInfo, b: ResourceTypeInfo): number {
    if (a.value === 'ALL' || b.value === 'ALL') { return a.value === 'ALL' ? -1 : 1; }
    if (a.name > b.name) { return 1; }
    if (a.name < b.name) { return -1; }
    return 0;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  registerOnValidatorChange(fn: () => void): void {
    this.onValidatorChange = fn;
  }

  ngOnInit() {
    this.filteredResources = this.resourceTypeControl.valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          this.updateView(value);
        }),
        startWith<string | ResourceTypeInfo>(''),
        map((value) => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(resource => this.fetchResources(resource) ),
        publishReplay(1),
        refCount()
      );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.resourceTypeFormGroup.disable({emitEvent: false});
    } else {
      this.resourceTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: Resource | null): void {
    this.searchText = '';
    const resourceTypeInfo = this.resources.find(resource => resource.value === value);
    if (resourceTypeInfo) {
      this.modelValue = resourceTypeInfo.value;
    } else {
      this.modelValue = null;
    }
    this.resourceTypeControl.patchValue(resourceTypeInfo, {emitEvent: false});
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.resourceTypeControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: ResourceTypeInfo | string | null) {
    let res: Resource = null;
    if (value && typeof value !== 'string') {
      res = value.value;
    }
    if (this.modelValue !== res) {
      this.modelValue = res;
      this.propagateChange(this.modelValue);
    }
  }

  displayResourceTypeFn(resource?: ResourceTypeInfo): string | undefined {
    if (resource) {
      return resource.name;
    }
    return undefined;
  }

  fetchResources(searchText?: string): Observable<Array<ResourceTypeInfo>> {
    this.searchText = searchText;
    let result = this.resources;
    if (searchText && searchText.length) {
      result = this.resources.filter((resourceTypeInfo) =>
        resourceTypeInfo.name.toLowerCase().includes(searchText.toLowerCase()));
    }
    return of(result);
  }

  clear(value: string = '') {
    this.resourceTypeInput.nativeElement.value = value;
    this.resourceTypeControl.patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.resourceTypeInput.nativeElement.blur();
      this.resourceTypeInput.nativeElement.focus();
    }, 0);
  }

  validate(control: AbstractControl): ValidationErrors | null {
    const resourceDuplicate = control.hasError('resourceDuplicate') && this.modelValue !== null;
    if (resourceDuplicate !== this.resourceDuplicate) {
      this.resourceDuplicate = resourceDuplicate;
      this.resourceTypeControl.updateValueAndValidity({ onlySelf: true, emitEvent: false });
    }
    return resourceDuplicate ? { resourceDuplicate: true } : null;
  }
}
