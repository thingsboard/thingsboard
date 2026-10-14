// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  ComponentRef,
  DestroyRef,
  Directive,
  EventEmitter,
  HostBinding,
  inject,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  isLayoutReportComponentConfig,
  ReportComponentConfig,
  toReportComponentLayoutSettings,
  updateFromReportComponentLayoutSettings
} from '@shared/models/report-component.models';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { FormBuilder, FormGroup } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { genNextLabel, isObject } from '@core/utils';
import {
  pageVariables,
  ReportComponentContext,
  reportComponentTypesData,
  ReportVariable
} from '@home/pages/reporting/template/components/report-component.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { Observable, of } from 'rxjs';
import { DataKey, Datasource, widgetType } from '@shared/models/widget.models';
import { catchError, mergeMap } from 'rxjs/operators';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { defaultFormProperties, FormProperty } from '@shared/models/dynamic-form.models';
import { DataKeySettingsFunction } from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { alarmFields } from '@shared/models/alarm.models';
import { entityFields } from '@shared/models/entity.models';
import { singleEntityFilterFromDeviceId } from '@shared/models/query/query.models';
import { EntityType } from '@shared/models/entity-type.models';
import { TbReportFormat } from '@shared/models/report.models';

@Component({
    selector: 'tb-report-component-config',
    templateUrl: './report-component-config.component.html',
    styleUrls: ['./report-component-config.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ReportComponentConfigComponent implements OnInit, OnChanges {

  @HostBinding('style.width') width = '100%';

  @Input()
  context: ReportComponentContext;

  @Input()
  reportComponent: ReportComponentConfig;

  @Input()
  readonly = false;

  @Output()
  reportComponentUpdated = new EventEmitter();

  @ViewChild('reportConfigContainer', {static: true}) reportConfigContainer: TbAnchorComponent;

  reportConfigForm: FormGroup;

  private reportConfigComponentRef: ComponentRef<AbstractReportComponentConfig>;
  private reportConfigComponent: AbstractReportComponentConfig;

  constructor() {}

  ngOnInit() {
    this.init();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'reportComponent') {
          this.init();
        }
      }
    }
  }

  private init() {
    this.reportConfigForm = null;
    if (this.reportConfigComponentRef) {
      this.reportConfigComponentRef.destroy();
      this.reportConfigComponentRef = null;
    }
    this.reportConfigContainer.viewContainerRef.clear();
    if (this.reportComponent) {
      const typeData =
        reportComponentTypesData.getReportComponentTypeData(this.reportComponent.type, this.reportComponent.subType);
      if (typeData) {
        this.reportConfigComponentRef = this.reportConfigContainer.viewContainerRef.createComponent(typeData.configComponent);
        this.reportConfigComponent = this.reportConfigComponentRef.instance;
        this.reportConfigComponent.context = this.context;
        if (typeData.configContext) {
          for (const key of Object.keys(typeData.configContext)) {
            this.reportConfigComponent[key] = typeData.configContext[key];
          }
        }
        this.reportConfigComponent.reportConfigUpdated.subscribe((updated) => {
          Object.assign(this.reportComponent, updated);
          this.reportComponentUpdated.emit();
        });
        this.reportConfigForm = this.reportConfigComponent.setupConfig(this.reportComponent);
        if (this.readonly) {
          this.reportConfigForm.disable({emitEvent: false});
        }
      }
    }
  }
}

@Directive()
export abstract class AbstractReportComponentConfig<C extends ReportComponentConfig = ReportComponentConfig> implements OnInit {

  @HostBinding('style.height')
  height = '100%';

  @Input()
  context: ReportComponentContext;

  @Output()
  reportConfigUpdated = new EventEmitter<C>();

  public get isPlainFormat(): boolean {
    return this.context.format === TbReportFormat.CSV;
  }

  public get datasource(): Datasource {
    const datasources = this.getDataSources();
    if (datasources && datasources.length) {
      return datasources[0];
    } else {
      return null;
    }
  }

  protected destroyRef: DestroyRef = inject(DestroyRef);
  protected fb: FormBuilder = inject(FormBuilder);

  widgetType = widgetType;

  callbacks: WidgetConfigCallbacks = {
    generateDataKey: this.generateDataKey.bind(this),
    fetchEntityKeys: this.fetchEntityKeys.bind(this),
    fetchEntityKeysForDevice: this.fetchEntityKeysForDevice.bind(this)
  } as any;

  reportConfigForm: FormGroup;

  protected reportComponentConfig: C;

  private hasLayoutConfig = false;

  ngOnInit() {
    const aliasAndFilterCallbacks = this.context.aliasAndFilterCallbacks;
    this.callbacks.createEntityAlias = aliasAndFilterCallbacks.createEntityAlias;
    this.callbacks.editEntityAlias = aliasAndFilterCallbacks.editEntityAlias;
    this.callbacks.createFilter = aliasAndFilterCallbacks.createFilter;
  }

  setupConfig(reportComponentConfig: C): FormGroup {
    this.reportComponentConfig = reportComponentConfig;
    this.reportConfigForm = this.buildForm(reportComponentConfig);
    if (isLayoutReportComponentConfig(reportComponentConfig) && !this.isPlainFormat) {
      this.hasLayoutConfig = true;
      const layoutSettings = toReportComponentLayoutSettings(reportComponentConfig);
      this.reportConfigForm.addControl('layout', this.fb.control(layoutSettings));
    }
    this.reportConfigForm.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateModel();
    });
    return this.reportConfigForm;
  }

  public variables(): ReportVariable[] {
    const dataSources = this.getDataSources();
    const dsVars = dataSources.map(ds => {
      if (ds && ds.dataKeys) {
        return ds.dataKeys.map(key => {
          if (key) {
            const variable: ReportVariable = {
              type: 'entityKey',
              name: (key.label || key.name),
              dataKey: key
            };
            return variable;
          } else {
            return null;
          }
        }).filter(variable => !!variable);
      } else {
        return [];
      }
    });
    let variables = dsVars.flat();
    variables = [...new Map(variables.map(item =>
      [item.name, item])).values()];

    variables.push(...pageVariables);
    variables.sort((a, b) => a.name.localeCompare(b.name));
    return variables;
  }

  public variableNames(): string[] {
    return this.variables().map(value => value.name);
  }

  private updateModel() {
    const value = this.reportConfigForm.getRawValue();
    if (this.hasLayoutConfig) {
      updateFromReportComponentLayoutSettings(value, value.layout);
      delete value.layout;
    }
    const output = this.prepareOutputConfig(value);
    this.reportComponentConfig = {
      type: this.reportComponentConfig.type,
      ...output
    };
    this.reportConfigUpdated.emit(this.reportComponentConfig);
  }

  private fetchEntityKeys(entityAliasId: string, dataKeyTypes: Array<DataKeyType>): Observable<Array<DataKey>> {
    return this.context.aliasController.getAliasInfo(entityAliasId).pipe(
      mergeMap((aliasInfo) => this.context.entityService.getEntityKeysByEntityFilter(
        aliasInfo.entityFilter,
        dataKeyTypes,  [],
        {ignoreLoading: true, ignoreErrors: true}
      ).pipe(
        catchError(() => of([]))
      )),
      catchError(() => of([] as Array<DataKey>))
    );
  }

  private fetchEntityKeysForDevice(deviceId: string, dataKeyTypes: Array<DataKeyType>): Observable<Array<DataKey>> {
    const entityFilter = singleEntityFilterFromDeviceId(deviceId);
    return this.context.entityService.getEntityKeysByEntityFilter(
      entityFilter,
      dataKeyTypes, [EntityType.DEVICE],
      {ignoreLoading: true, ignoreErrors: true}
    ).pipe(
      catchError(() => of([]))
    );
  }

  private generateDataKey(chip: any, type: DataKeyType, dataKeySettingsForm: FormProperty[],
                          isLatestDataKey: boolean, dataKeySettingsFunction: DataKeySettingsFunction): DataKey {
    if (isObject(chip)) {
      return chip;
    } else {
      let label: string = chip;
      if (type === DataKeyType.alarm || type === DataKeyType.entityField) {
        const keyField = type === DataKeyType.alarm ? alarmFields[label] : entityFields[chip];
        if (keyField) {
          label = this.context.translate.instant(keyField.name);
        }
      }
      const datasources = this.getDataSources();
      label = genNextLabel(label, datasources);
      const result: DataKey = {
        name: chip,
        type,
        label,
        color: this.genNextColor(),
        settings: {}
      };
      if (type === DataKeyType.count) {
        result.name = 'count';
      }
      if (dataKeySettingsForm?.length) {
        result.settings = defaultFormProperties(dataKeySettingsForm);
      } else if (dataKeySettingsFunction) {
        const settings = dataKeySettingsFunction(result, isLatestDataKey);
        if (settings) {
          result.settings = settings;
        }
      }
      return result;
    }
  }

  private genNextColor(): string {
    let i = 0;
    const datasources = this.getDataSources();
    if (datasources) {
      datasources.forEach((datasource) => {
        if (datasource && (datasource.dataKeys || datasource.latestDataKeys)) {
          i += ((datasource.dataKeys ? datasource.dataKeys.length : 0) +
            (datasource.latestDataKeys ? datasource.latestDataKeys.length : 0));
        }
      });
    }
    return this.context.utils.getMaterialColor(i);
  }

  protected buildForm(_reportComponentConfig: C): FormGroup {
    return this.fb.group({});
  }

  protected prepareOutputConfig(config: any): C {
    return config;
  }

  protected getDataSources(): Datasource[] {
    if (this.reportConfigForm.get('dataSources')) {
      return this.reportConfigForm.get('dataSources').value;
    } else {
      return [];
    }
  };
}
