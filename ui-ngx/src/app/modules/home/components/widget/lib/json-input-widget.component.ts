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

import {Component, Input, OnInit} from '@angular/core';
import {PageComponent} from '@shared/components/page.component';
import {WidgetContext} from '@home/models/widget-component.models';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {UtilsService} from '@core/services/utils.service';
import {TranslateService} from '@ngx-translate/core';
import {Datasource, DatasourceData, DatasourceType, WidgetConfig} from '@shared/models/widget.models';
import {IWidgetSubscription} from '@core/api/widget-api.models';
import {FormBuilder, FormGroup, ValidatorFn, Validators} from '@angular/forms';
import {AttributeService} from '@core/http/attribute.service';
import {AttributeData, AttributeScope, DataKeyType, LatestTelemetry} from '@shared/models/telemetry/telemetry.models';
import {EntityId} from '@shared/models/id/entity-id';
import {EntityType} from '@shared/models/entity-type.models';
import {createLabelFromDatasource} from '@core/utils';
import {Observable} from 'rxjs';

enum JsonInputWidgetMode {
  ATTRIBUTE = 'ATTRIBUTE',
  TIME_SERIES = 'TIME_SERIES',
}

interface JsonInputWidgetSettings {
  widgetTitle: string;
  widgetMode: JsonInputWidgetMode;
  attributeScope?: AttributeScope;
  showLabel: boolean;
  labelValue?: string;
  attributeRequired: boolean;
  showResultMessage: boolean;
}

@Component({
  selector: 'tb-json-input-widget ',
  templateUrl: './json-input-widget.component.html',
  styleUrls: ['./json-input-widget.component.scss']
})
export class JsonInputWidgetComponent extends PageComponent implements OnInit {

  @Input()
  ctx: WidgetContext;

  public settings: JsonInputWidgetSettings;
  private widgetConfig: WidgetConfig;
  private subscription: IWidgetSubscription;
  private datasource: Datasource;

  labelValue: string;

  entityDetected = false;
  dataKeyDetected = false;
  isValidParameter = false;
  errorMessage: string;

  isFocused: boolean;
  originalValue: any;
  attributeUpdateFormGroup: FormGroup;

  toastTargetId = 'json-input-widget' + this.utils.guid();

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private fb: FormBuilder,
              private attributeService: AttributeService,
              private translate: TranslateService) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.jsonInputWidget = this;
    this.settings = this.ctx.settings;
    this.widgetConfig = this.ctx.widgetConfig;
    this.subscription = this.ctx.defaultSubscription;
    this.datasource = this.subscription.datasources[0];
    this.initializeConfig();
    this.validateDatasources();
    this.buildForm();
    this.ctx.updateWidgetParams();
  }

  private initializeConfig() {
    if (this.settings.widgetTitle && this.settings.widgetTitle.length) {
      const title = createLabelFromDatasource(this.datasource, this.settings.widgetTitle);
      this.ctx.widgetTitle = this.utils.customTranslation(title, title);
    } else {
      this.ctx.widgetTitle = this.ctx.widgetConfig.title;
    }

    if (this.settings.labelValue && this.settings.labelValue.length) {
      const label = createLabelFromDatasource(this.datasource, this.settings.labelValue);
      this.labelValue = this.utils.customTranslation(label, label);
    } else {
      this.labelValue = this.translate.instant('widgets.input-widgets.value');
    }
  }

  private validateDatasources() {
    if (this.datasource?.type === DatasourceType.entity) {
      this.entityDetected = true;
      if (this.datasource.dataKeys.length) {
        this.dataKeyDetected = true;

        if (this.settings.widgetMode === JsonInputWidgetMode.ATTRIBUTE) {
          if (this.datasource.dataKeys[0].type === DataKeyType.attribute) {
            if (this.settings.attributeScope === AttributeScope.SERVER_SCOPE || this.datasource.entityType === EntityType.DEVICE) {
              this.isValidParameter = true;
            } else {
              this.errorMessage = 'widgets.input-widgets.not-allowed-entity';
            }
          } else {
            this.errorMessage = 'widgets.input-widgets.no-attribute-selected';
          }
        } else {
          if (this.datasource.dataKeys[0].type === DataKeyType.timeseries) {
            this.isValidParameter = true;
          } else {
            this.errorMessage = 'widgets.input-widgets.no-timeseries-selected';
          }
        }

      }
    }
  }

  private buildForm() {
    const validators: ValidatorFn[] = [];
    if (this.settings.attributeRequired) {
      validators.push(Validators.required);
    }
    this.attributeUpdateFormGroup = this.fb.group({
      currentValue: [{}, validators]
    });
    this.attributeUpdateFormGroup.valueChanges.subscribe( () => {
      this.ctx.detectChanges();
    });
  }

  private updateWidgetData(data: Array<DatasourceData>) {
    if (this.isValidParameter) {
      let value = {};
      if (data[0].data[0][1] !== '') {
        try {
          value = JSON.parse(data[0].data[0][1]);
        } catch (e) {
          value = data[0].data[0][1];
        }
      }
      this.originalValue = value;
      if (!this.isFocused) {
        this.attributeUpdateFormGroup.get('currentValue').patchValue(this.originalValue);
        this.ctx.detectChanges();
      }
    }
  }

  public onDataUpdated() {
    this.updateWidgetData(this.subscription.data);
  }

  public save() {
    this.isFocused = false;

    const attributeToSave: AttributeData = {
      key: this.datasource.dataKeys[0].name,
      value: this.attributeUpdateFormGroup.get('currentValue').value
    };

    const entityId: EntityId = {
      entityType: this.datasource.entityType,
      id: this.datasource.entityId
    };

    let saveAttributeObservable: Observable<any>;
    if (this.settings.widgetMode === JsonInputWidgetMode.ATTRIBUTE) {
      saveAttributeObservable = this.attributeService.saveEntityAttributes(
        entityId,
        this.settings.attributeScope,
        [ attributeToSave ],
        {}
      );
    } else {
      saveAttributeObservable = this.attributeService.saveEntityTimeseries(
        entityId,
        LatestTelemetry.LATEST_TELEMETRY,
        [ attributeToSave ],
        {}
      );
    }
    saveAttributeObservable.subscribe(
      () => {
        this.attributeUpdateFormGroup.markAsPristine();
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
  }

  public discard() {
    this.attributeUpdateFormGroup.reset({currentValue: this.originalValue}, {emitEvent: false});
    this.attributeUpdateFormGroup.markAsPristine();
    this.isFocused = false;
  }
}
