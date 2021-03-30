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

import { Component, ElementRef, Input, NgZone, OnDestroy, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Overlay } from '@angular/cdk/overlay';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { DataKey, Datasource, DatasourceData, DatasourceType, WidgetConfig } from '@shared/models/widget.models';
import { IWidgetSubscription } from '@core/api/widget-api.models';
import { createLabelFromDatasource, isDefined, isDefinedAndNotNull, isEqual, isUndefined } from '@core/utils';
import { EntityType } from '@shared/models/entity-type.models';
import * as _moment from 'moment';
import { FormBuilder, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { RequestConfig } from '@core/http/http-utils';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeData, AttributeScope, LatestTelemetry } from '@shared/models/telemetry/telemetry.models';
import { forkJoin, Observable, Subject } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { ResizeObserver } from '@juggle/resize-observer';
import { takeUntil } from 'rxjs/operators';

type FieldAlignment = 'row' | 'column';

type MultipleInputWidgetDataKeyType = 'server' | 'shared' | 'timeseries';
type MultipleInputWidgetDataKeyValueType = 'string' | 'double' | 'integer' |
                                           'booleanCheckbox' | 'booleanSwitch' |
                                           'dateTime' | 'date' | 'time';
type MultipleInputWidgetDataKeyEditableType = 'editable' | 'disabled' | 'readonly';

interface MultipleInputWidgetSettings {
  widgetTitle: string;
  showActionButtons: boolean;
  updateAllValues: boolean;
  saveButtonLabel: string;
  resetButtonLabel: string;
  showResultMessage: boolean;
  showGroupTitle: boolean;
  groupTitle: string;
  fieldsAlignment: FieldAlignment;
  fieldsInRow: number;
  attributesShared?: boolean;
}

interface MultipleInputWidgetDataKeySettings {
  dataKeyType: MultipleInputWidgetDataKeyType;
  dataKeyValueType: MultipleInputWidgetDataKeyValueType;
  required: boolean;
  isEditable: MultipleInputWidgetDataKeyEditableType;
  disabledOnDataKey: string;
  dataKeyHidden: boolean;
  step?: number;
  minValue?: number;
  maxValue?: number;
  requiredErrorMessage?: string;
  invalidDateErrorMessage?: string;
  minValueErrorMessage?: string;
  maxValueErrorMessage?: string;
  icon: string;
  inputTypeNumber?: boolean;
  readOnly?: boolean;
  disabledOnCondition?: boolean;
}

interface MultipleInputWidgetDataKey extends DataKey {
  formId?: string;
  settings: MultipleInputWidgetDataKeySettings;
  isFocused: boolean;
  value?: any;
}

interface MultipleInputWidgetSource {
  datasource: Datasource;
  keys: MultipleInputWidgetDataKey[];
}

@Component({
  selector: 'tb-multiple-input-widget ',
  templateUrl: './multiple-input-widget.component.html',
  styleUrls: ['./multiple-input-widget.component.scss']
})
export class MultipleInputWidgetComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('formContainer', {static: true}) formContainerRef: ElementRef<HTMLElement>;

  @Input()
  ctx: WidgetContext;

  private formResize$: ResizeObserver;
  public settings: MultipleInputWidgetSettings;
  private widgetConfig: WidgetConfig;
  private subscription: IWidgetSubscription;
  private datasources: Array<Datasource>;
  private destroy$ = new Subject();
  public sources: Array<MultipleInputWidgetSource> = [];

  isVerticalAlignment: boolean;
  inputWidthSettings: string;
  changeAlignment: boolean;
  saveButtonLabel: string;
  resetButtonLabel: string;

  entityDetected = false;
  isAllParametersValid = true;

  multipleInputFormGroup: FormGroup;

  toastTargetId = 'multiple-input-widget' + this.utils.guid();

  constructor(protected store: Store<AppState>,
              private elementRef: ElementRef,
              private ngZone: NgZone,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private utils: UtilsService,
              private fb: FormBuilder,
              private attributeService: AttributeService,
              private translate: TranslateService) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.multipleInputWidget = this;
    this.settings = this.ctx.settings;
    this.widgetConfig = this.ctx.widgetConfig;
    this.subscription = this.ctx.defaultSubscription;
    this.datasources = this.subscription.datasources;
    this.initializeConfig();
    this.updateDatasources();
    this.buildForm();
    this.ctx.updateWidgetParams();
    this.formResize$ = new ResizeObserver(() => {
      this.resize();
    });
    this.formResize$.observe(this.formContainerRef.nativeElement);
  }

  ngOnDestroy(): void {
    if (this.formResize$) {
      this.formResize$.disconnect();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeConfig() {

    if (this.settings.widgetTitle && this.settings.widgetTitle.length) {
      this.ctx.widgetTitle = this.utils.customTranslation(this.settings.widgetTitle, this.settings.widgetTitle);
    } else {
      this.ctx.widgetTitle = this.ctx.widgetConfig.title;
    }

    this.settings.groupTitle = this.settings.groupTitle || '${entityName}';

    if (this.settings.saveButtonLabel && this.settings.saveButtonLabel.length) {
      this.saveButtonLabel = this.utils.customTranslation(this.settings.saveButtonLabel, this.settings.saveButtonLabel);
    } else {
      this.saveButtonLabel = this.translate.instant('action.save');
    }
    if (this.settings.resetButtonLabel && this.settings.resetButtonLabel.length) {
      this.resetButtonLabel = this.utils.customTranslation(this.settings.resetButtonLabel, this.settings.resetButtonLabel);
    } else {
      this.resetButtonLabel = this.translate.instant('action.undo');
    }

    // For backward compatibility
    if (isUndefined(this.settings.showActionButtons)) {
      this.settings.showActionButtons = true;
    }
    if (isUndefined(this.settings.fieldsAlignment)) {
      this.settings.fieldsAlignment = 'row';
    }
    if (isUndefined(this.settings.fieldsInRow)) {
      this.settings.fieldsInRow = 2;
    }
    // For backward compatibility

    this.isVerticalAlignment = !(this.settings.fieldsAlignment === 'row');

    if (!this.isVerticalAlignment && this.settings.fieldsInRow) {
      this.inputWidthSettings = 100 / this.settings.fieldsInRow + '%';
    }

    this.updateWidgetDisplaying();
  }

  private updateDatasources() {
    if (this.datasources && this.datasources.length) {
      this.entityDetected = true;
      let keyIndex = 0;
      this.datasources.forEach((datasource) => {
        const source: MultipleInputWidgetSource = {
          datasource,
          keys: []
        };
        if (datasource.type === DatasourceType.entity) {
          datasource.dataKeys.forEach((dataKey: MultipleInputWidgetDataKey) => {
            if ((datasource.entityType !== EntityType.DEVICE) && (dataKey.settings.dataKeyType === 'shared')) {
              this.isAllParametersValid = false;
            }
            if (dataKey.units) {
              dataKey.label += ' (' + dataKey.units + ')';
            }
            dataKey.formId = (++keyIndex) + '';
            dataKey.isFocused = false;

            // For backward compatibility
            if (isUndefined(dataKey.settings.dataKeyType)) {
              if (this.settings.attributesShared) {
                dataKey.settings.dataKeyType = 'shared';
              } else {
                dataKey.settings.dataKeyType = 'server';
              }
            }

            if (isUndefined(dataKey.settings.dataKeyValueType)) {
              if (dataKey.settings.inputTypeNumber) {
                dataKey.settings.dataKeyValueType = 'double';
              } else {
                dataKey.settings.dataKeyValueType = 'string';
              }
            }

            if (isUndefined(dataKey.settings.isEditable)) {
              if (dataKey.settings.readOnly) {
                dataKey.settings.isEditable = 'readonly';
              } else {
                dataKey.settings.isEditable = 'editable';
              }
            }
            // For backward compatibility

            source.keys.push(dataKey);
          });
        } else {
          this.entityDetected = false;
        }
        this.sources.push(source);
      });
    }
  }

  private buildForm() {
    this.multipleInputFormGroup = this.fb.group({});
    this.sources.forEach((source) => {
      const addedFormControl = {};
      const waitFormControl = {};
      for (const key of this.visibleKeys(source)) {
        const validators: ValidatorFn[] = [];
        if (key.settings.required) {
          validators.push(Validators.required);
        }
        if (key.settings.dataKeyValueType === 'integer') {
          validators.push(Validators.pattern(/^-?[0-9]+$/));
        }
        if (key.settings.dataKeyValueType === 'integer' || key.settings.dataKeyValueType === 'double') {
          if (isDefinedAndNotNull(key.settings.minValue) && isFinite(key.settings.minValue)) {
            validators.push(Validators.min(key.settings.minValue));
          }
          if (isDefinedAndNotNull(key.settings.maxValue) && isFinite(key.settings.maxValue)) {
            validators.push(Validators.max(key.settings.maxValue));
          }
        }
        const formControl = this.fb.control(
          { value: key.value,
                      disabled: key.settings.isEditable === 'disabled' || key.settings.disabledOnCondition},
          validators
         );
        if (this.settings.showActionButtons) {
          addedFormControl[key.name] = formControl;
          if (key.settings.isEditable === 'editable' && key.settings.disabledOnDataKey) {
            if (addedFormControl.hasOwnProperty(key.settings.disabledOnDataKey)) {
              addedFormControl[key.settings.disabledOnDataKey].valueChanges.pipe(
                takeUntil(this.destroy$)
              ).subscribe((value) => {
                if (!value) {
                  formControl.disable({emitEvent: false});
                } else {
                  formControl.enable({emitEvent: false});
                }
              });
            } else {
              waitFormControl[key.settings.disabledOnDataKey] = formControl;
            }
          }

          if (waitFormControl.hasOwnProperty(key.name)) {
            formControl.valueChanges.pipe(
              takeUntil(this.destroy$)
            ).subscribe((value) => {
              if (!value) {
                waitFormControl[key.name].disable({emitEvent: false});
              } else {
                waitFormControl[key.name].enable({emitEvent: false});
              }
            });
          }
        }
        this.multipleInputFormGroup.addControl(key.formId, formControl);
      }
    });
  }

  private updateWidgetData(data: Array<DatasourceData>) {
    let dataIndex = 0;
    this.sources.forEach((source) => {
      source.keys.forEach((key) => {
        const keyData = data[dataIndex].data;
        if (keyData && keyData.length) {
          let value;
          switch (key.settings.dataKeyValueType) {
            case 'dateTime':
            case 'date':
              if (isDefinedAndNotNull(keyData[0][1]) && keyData[0][1] !== '') {
                value = _moment(keyData[0][1]).toDate();
              } else {
                value = null;
              }
              break;
            case 'time':
              value = _moment().startOf('day').add(keyData[0][1], 'ms').toDate();
              break;
            case 'booleanCheckbox':
            case 'booleanSwitch':
              value = (keyData[0][1] === 'true');
              break;
            default:
              value = keyData[0][1];
          }
          key.value = value;
        }

        if (key.settings.isEditable === 'editable' && key.settings.disabledOnDataKey) {
          const conditions = data.filter((item) => {
            return source.datasource === item.datasource && item.dataKey.name === key.settings.disabledOnDataKey;
          });
          if (conditions && conditions.length) {
            if (conditions[0].data.length) {
              if (conditions[0].data[0][1] === 'false') {
                key.settings.disabledOnCondition = true;
              } else {
                key.settings.disabledOnCondition = !conditions[0].data[0][1];
              }
            }
          }
        }

        if (!key.settings.dataKeyHidden) {
          if (key.settings.isEditable === 'disabled' || key.settings.disabledOnCondition) {
            this.multipleInputFormGroup.get(key.formId).disable({emitEvent: false});
          } else {
            this.multipleInputFormGroup.get(key.formId).enable({emitEvent: false});
          }
          const dirty = this.multipleInputFormGroup.get(key.formId).dirty;
          if (!key.isFocused && !dirty) {
            this.multipleInputFormGroup.get(key.formId).patchValue(key.value, {emitEvent: false});
          }
        }
        dataIndex++;
      });
    });
  }

  private updateWidgetDisplaying() {
    this.changeAlignment = (this.ctx.$container && this.ctx.$container[0].offsetWidth < 620);
  }

  public onDataUpdated() {
    this.updateWidgetData(this.subscription.data);
    this.ctx.detectChanges();
  }

  private resize() {
    this.updateWidgetDisplaying();
    this.ctx.detectChanges();
  }

  public getGroupTitle(datasource: Datasource): string {
    const groupTitle = createLabelFromDatasource(datasource, this.settings.groupTitle);
    return this.utils.customTranslation(groupTitle, groupTitle);
  }

  public getErrorMessageText(keySettings: MultipleInputWidgetDataKeySettings, errorType: string): string {
    let errorMessage;
    let defaultMessage;
    let messageValues;
    switch (errorType) {
      case 'required':
        errorMessage = keySettings.requiredErrorMessage;
        defaultMessage = '';
        break;
      case 'min':
        errorMessage = keySettings.minValueErrorMessage;
        defaultMessage = 'widgets.input-widgets.min-value-error';
        messageValues = {
          value: keySettings.minValue
        };
        break;
      case 'max':
        errorMessage = keySettings.maxValueErrorMessage;
        defaultMessage = 'widgets.input-widgets.max-value-error';
        messageValues = {
          value: keySettings.maxValue
        };
        break;
      case 'invalidDate':
        errorMessage = keySettings.invalidDateErrorMessage;
        defaultMessage = 'widgets.input-widgets.invalid-date';
        break;
      default:
        return '';
    }
    return this.getTranslatedErrorText(errorMessage, defaultMessage, messageValues);
  }

  public getTranslatedErrorText(errorMessage: string, defaultMessage: string, messageValues?: object): string {
    let messageText;
    if (errorMessage && errorMessage.length) {
      messageText = this.utils.customTranslation(errorMessage, errorMessage);
    } else if (defaultMessage && defaultMessage.length) {
      if (!messageValues) {
        messageValues = {};
      }
      messageText = this.translate.instant(defaultMessage, messageValues);
    } else {
      messageText = '';
    }
    return messageText;
  }

  public visibleKeys(source: MultipleInputWidgetSource): MultipleInputWidgetDataKey[] {
    return source.keys.filter(key => !key.settings.dataKeyHidden);
  }

  public datePickerType(keyType: MultipleInputWidgetDataKeyValueType): string {
    switch (keyType) {
      case 'dateTime':
        return 'datetime';
      case 'date':
        return 'date';
      case 'time':
        return 'time';
    }
  }

  public focusInputElement($event: Event) {
    ($event.target as HTMLInputElement).select();
  }

  public inputChanged(source: MultipleInputWidgetSource, key: MultipleInputWidgetDataKey) {
    if (!this.settings.showActionButtons) {
      const currentValue = this.multipleInputFormGroup.get(key.formId).value;
      if (!key.settings.required || (key.settings.required && isDefined(currentValue))) {
        const dataToSave: MultipleInputWidgetSource = {
          datasource: source.datasource,
          keys: [key]
        };
        this.save(dataToSave);
      }
    }
  }

  public save(dataToSave?: MultipleInputWidgetSource) {
    if (document && document.activeElement) {
      (document.activeElement as HTMLElement).blur();
    }
    const config: RequestConfig = {
      ignoreLoading: !this.settings.showActionButtons
    };
    let data: Array<MultipleInputWidgetSource>;
    if (dataToSave) {
      data = [dataToSave];
    } else {
      data = this.sources;
    }
    const tasks: Observable<any>[] = [];
    data.forEach((toSave) => {
      const serverAttributes: AttributeData[] = [];
      const sharedAttributes: AttributeData[] = [];
      const telemetry: AttributeData[] = [];
      for (const key of this.visibleKeys(toSave)) {
        const currentValue = this.multipleInputFormGroup.get(key.formId).value;
        if (!isEqual(currentValue, key.value) || this.settings.updateAllValues) {
          const attribute: AttributeData = {
            key: key.name,
            value: null
          };
          if (currentValue) {
            switch (key.settings.dataKeyValueType) {
              case 'dateTime':
              case 'date':
                attribute.value = currentValue.getTime();
                break;
              case 'time':
                attribute.value = currentValue.getTime() - _moment().startOf('day').valueOf();
                break;
              default:
                attribute.value = currentValue;
            }
          } else {
            if (currentValue === '') {
              attribute.value = null;
            } else {
              attribute.value = currentValue;
            }
          }

          switch (key.settings.dataKeyType) {
            case 'shared':
              sharedAttributes.push(attribute);
              break;
            case 'timeseries':
              telemetry.push(attribute);
              break;
            default:
              serverAttributes.push(attribute);
          }
        }
      }
      const entityId: EntityId = {
        entityType: toSave.datasource.entityType,
        id: toSave.datasource.entityId
      };
      if (serverAttributes.length) {
        tasks.push(this.attributeService.saveEntityAttributes(
          entityId,
          AttributeScope.SERVER_SCOPE,
          serverAttributes,
          config
        ));
      }
      if (sharedAttributes.length) {
        tasks.push(this.attributeService.saveEntityAttributes(
          entityId,
          AttributeScope.SHARED_SCOPE,
          sharedAttributes,
          config
        ));
      }
      if (telemetry.length) {
        tasks.push(this.attributeService.saveEntityTimeseries(
          entityId,
          LatestTelemetry.LATEST_TELEMETRY,
          telemetry,
          config
        ));
      }
    });
    if (tasks.length) {
      forkJoin(tasks).subscribe(
        () => {
          this.multipleInputFormGroup.markAsPristine();
          this.ctx.detectChanges();
          if (this.settings.showResultMessage) {
            this.ctx.showSuccessToast(this.translate.instant('widgets.input-widgets.update-successful'),
              1000, 'bottom', 'left', this.toastTargetId);
          }
        },
        () => {
          if (this.settings.showResultMessage) {
            this.ctx.showErrorToast(this.translate.instant('widgets.input-widgets.update-failed'),
              'bottom', 'left', this.toastTargetId);
          }
        });
    } else {
      this.multipleInputFormGroup.markAsPristine();
      this.ctx.detectChanges();
    }
  }

  public discardAll() {
    this.multipleInputFormGroup.reset(undefined, {emitEvent: false});
    this.sources.forEach((source) => {
      for (const key of this.visibleKeys(source)) {
        this.multipleInputFormGroup.get(key.formId).patchValue(key.value, {emitEvent: false});
      }
    });
    this.multipleInputFormGroup.markAsPristine();
  }
}
