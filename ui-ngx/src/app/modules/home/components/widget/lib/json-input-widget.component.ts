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

import { Component, DestroyRef, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { Datasource, DatasourceData, DatasourceType, WidgetConfig } from '@shared/models/widget.models';
import { IWidgetSubscription } from '@core/api/widget-api.models';
import { UntypedFormBuilder, UntypedFormGroup, ValidatorFn } from '@angular/forms';
import { AttributeService } from '@core/http/attribute.service';
import { AttributeData, AttributeScope, DataKeyType, LatestTelemetry } from '@shared/models/telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { createLabelFromDatasource, isDefinedAndNotNull } from '@core/utils';
import { Observable } from 'rxjs';
import { jsonRequired } from '@shared/components/json-object-edit.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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
    styleUrls: ['./json-input-widget.component.scss'],
    standalone: false
})
export class JsonInputWidgetComponent extends PageComponent implements OnInit {

  @Input()
  ctx: WidgetContext;

  public settings: JsonInputWidgetSettings;
  private widgetConfig: WidgetConfig;
  private subscription: IWidgetSubscription;
  private datasource: Datasource;

  labelValue: string;

  datasourceDetected = false;
  errorMessage: string;

  isFocused: boolean;
  originalValue: any;
  attributeUpdateFormGroup: UntypedFormGroup;

  toastTargetId = 'json-input-widget' + this.utils.guid();

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private fb: UntypedFormBuilder,
              private attributeService: AttributeService,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {
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
      this.ctx.widgetTitle = this.settings.widgetTitle;
    }
    if (this.settings.labelValue && this.settings.labelValue.length) {
      const label = createLabelFromDatasource(this.datasource, this.settings.labelValue);
      this.labelValue = this.utils.customTranslation(label, label);
    } else {
      this.labelValue = this.translate.instant('widgets.input-widgets.value');
    }
  }

  private validateDatasources() {
    this.datasourceDetected = isDefinedAndNotNull(this.datasource);
    if (!this.datasourceDetected) {
      return;
    }
    if (this.datasource.type === DatasourceType.entity) {
      if (this.settings.widgetMode === JsonInputWidgetMode.ATTRIBUTE) {
        if (this.datasource.dataKeys[0].type === DataKeyType.attribute) {
          if (this.settings.attributeScope !== AttributeScope.SERVER_SCOPE && this.datasource.entityType !== EntityType.DEVICE) {
            this.errorMessage = 'widgets.input-widgets.not-allowed-entity';
          }
        } else {
          this.errorMessage = 'widgets.input-widgets.no-attribute-selected';
        }
      } else {
        if (this.datasource.dataKeys[0].type !== DataKeyType.timeseries) {
          this.errorMessage = 'widgets.input-widgets.no-timeseries-selected';
        }
      }
    } else {
      this.errorMessage = 'widgets.input-widgets.no-entity-selected';
    }
  }

  private buildForm() {
    const validators: ValidatorFn[] = [];
    if (this.settings.attributeRequired) {
      validators.push(jsonRequired);
    }
    this.attributeUpdateFormGroup = this.fb.group({
      currentValue: [{}, validators]
    });
    this.attributeUpdateFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.ctx.detectChanges();
    });
  }

  private updateWidgetData(data: Array<DatasourceData>) {
    if (!this.errorMessage) {
      let value = null;
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
        [attributeToSave],
        {}
      );
    } else {
      saveAttributeObservable = this.attributeService.saveEntityTimeseries(
        entityId,
        LatestTelemetry.LATEST_TELEMETRY,
        [attributeToSave],
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
