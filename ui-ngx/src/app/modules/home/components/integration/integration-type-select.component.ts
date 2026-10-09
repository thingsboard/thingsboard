// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { isNotEmptyStr, isString } from '@core/utils';
import { IntegrationType, IntegrationTypeInfo, integrationTypeInfoMap } from '@shared/models/integration.models';
import { Observable, of } from 'rxjs';
import { distinctUntilChanged, map, mergeMap, share, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { FloatLabelType, MatFormFieldAppearance } from '@angular/material/form-field';

type IntegrationInfo = IntegrationTypeInfo & {type: IntegrationType};

@Component({
    selector: 'tb-integration-type-select',
    templateUrl: 'integration-type-select.component.html',
    styleUrls: ['integration-type-select.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => IntegrationTypeSelectComponent),
            multi: true
        }],
    standalone: false
})
export class IntegrationTypeSelectComponent implements ControlValueAccessor, OnInit {

  integrationTypeFormGroup: FormGroup;
  searchText = '';

  filteredIntegrationTypes: Observable<Array<IntegrationInfo>>;
  modelValue: IntegrationInfo;

  private pristine = true;

  private integrationTypesInfo: Array<IntegrationInfo> = [];

  @ViewChild('integrationTypeInput', {static: true}) integrationTypeInput: ElementRef;
  @ViewChild(MatAutocompleteTrigger) autocomplete: MatAutocompleteTrigger;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
    if (this.requiredValue) {
      this.integrationTypeFormGroup.get('type').setValidators(Validators.required);
    } else {
      this.integrationTypeFormGroup.get('type').clearValidators();
    }
    this.integrationTypeFormGroup.get('type').updateValueAndValidity({emitEvent: false});
  }

  @Input()
  disabled: boolean;

  @Input()
  floatLabel: FloatLabelType = 'auto'

  @Input()
  placeholder = this.translate.instant('integration.select-integration-type');

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  private propagateChange = (_v: any) => { };

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    this.integrationTypeFormGroup = this.fb.group({
      type: ['']
    });
    Object.values(IntegrationType).forEach(integrationType => {
      const integration = integrationTypeInfoMap.get(integrationType);
      this.integrationTypesInfo.push({
        type: integrationType,
        ...integration,
        name: this.translate.instant(integration.name),
        description: integration.description ? this.translate.instant(integration.description) : ''
      });
    });
  }

  ngOnInit() {
    this.filteredIntegrationTypes = this.integrationTypeFormGroup.get('type').valueChanges
      .pipe(
        tap(value => {
          let modelValue: IntegrationInfo;
          if (isString(value) || !value) {
            modelValue = null;
          } else {
            modelValue = this.integrationTypesInfo.find(info => info.type === value.type);
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (isString(value) ? value.trim() : value.type) : ''),
        distinctUntilChanged(),
        mergeMap(name => this.fetchIntegrationTypes(name)),
        share()
      );
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.integrationTypeFormGroup.disable({emitEvent: false});
    } else {
      this.integrationTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: IntegrationType) {
    this.searchText = '';
    const integrationType = value != null && this.integrationTypesInfo.find(integration => integration.type === value);
    if (integrationType) {
      this.modelValue = integrationType;
      this.integrationTypeFormGroup.get('type').patchValue(this.modelValue, {emitEvent: false});
    } else {
      this.modelValue = null;
      this.integrationTypeFormGroup.get('type').patchValue('', {emitEvent: false});
    }
    this.pristine = true;
  }

  onFocus() {
    if (this.pristine) {
      this.integrationTypeFormGroup.get('type').updateValueAndValidity({onlySelf: true});
      this.pristine = false;
    }
  }

  selectedType() {
    if (isNotEmptyStr(this.searchText)) {
      const result = this.filterIntegrationType(this.searchText);
      if (result.length === 1) {
        this.integrationTypeFormGroup.get('type').patchValue(result[0]);
        this.autocomplete.closePanel();
      }
    }
  }

  clear() {
    this.integrationTypeFormGroup.get('type').patchValue('');
    setTimeout(() => {
      this.integrationTypeInput.nativeElement.blur();
      this.integrationTypeInput.nativeElement.focus();
    }, 0);
  }

  displayIntegrationTypeFn(integration?: IntegrationInfo): string {
    return integration?.name;
  }

  private updateView(value: IntegrationInfo | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue?.type || null);
    }
  }

  private fetchIntegrationTypes(searchText?: string): Observable<Array<IntegrationInfo>> {
    this.searchText = searchText;
    let result = this.integrationTypesInfo;
    if (isNotEmptyStr(searchText)) {
      result = this.filterIntegrationType(searchText);
    }
    return of(result);
  }

  private filterIntegrationType(searchText: string): Array<IntegrationInfo> {
    const lowerSearchText = searchText.toLowerCase();
    return this.integrationTypesInfo.filter((integrationInfo) =>
      integrationInfo.name.toLowerCase().includes(lowerSearchText) ||
      integrationInfo.description.toLowerCase().includes(lowerSearchText) ||
      searchText === integrationInfo.type ||
      integrationInfo.tags?.toString().toLowerCase().includes(lowerSearchText)
    );
  }
}
