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

import { ChangeDetectorRef, Component, Injector, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import {
  DataKey,
  Datasource,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
  DatasourceType,
  WidgetConfig,
} from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  getTimewindowConfig,
  setTimewindowConfig
} from '@home/components/widget/config/timewindow-config-panel.component';
import { formatValue, isDefined, isUndefined } from '@core/utils';
import {
  cssSizeToStrSize,
  DateFormatProcessor,
  DateFormatSettings,
  resolveCssSize
} from '@shared/models/widget-settings.models';
import {
  CapacityUnits,
  createShapeLayout,
  levelCardDefaultSettings,
  LevelCardLayout,
  levelCardLayoutTranslations,
  LevelCardWidgetSettings,
  LiquidWidgetDataSourceType,
  LiquidWidgetDataSourceTypeTranslations,
  loadSvgShapesMapping,
  optionsFilter,
  Shapes,
  ShapesTranslations,
  updatedFormSettingsValidators
} from '@home/components/widget/lib/indicator/liquid-level-widget.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ImageCardsSelectComponent } from '@home/components/widget/lib/settings/common/image-cards-select.component';
import { map, share, tap } from 'rxjs/operators';
import { Observable, of, ReplaySubject } from 'rxjs';
import { ResourcesService } from '@core/services/resources.service';
import { UtilsService } from '@core/services/utils.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-liquid-level-card-basic-config',
    templateUrl: './liquid-level-card-basic-config.component.html',
    styleUrls: ['../basic-config.scss'],
    standalone: false
})
export class LiquidLevelCardBasicConfigComponent extends BasicWidgetConfigComponent {

  @ViewChild('layoutsImageCardsSelect', { static: false }) layoutsImageCardsSelect: ImageCardsSelectComponent;

  @ViewChild('shapesImageCardsSelect', { static: false }) shapesImageCardsSelect: ImageCardsSelectComponent;

  private get datasource(): Datasource {
    if (this.widgetConfig.config.datasources && this.widgetConfig.config.datasources) {
      return this.widgetConfig.config.datasources[0];
    } else {
      return null;
    }
  }

  public get volumeInput(): boolean {
    const datasourceUnits = this.levelCardWidgetConfigForm.get('datasourceUnits').value;
    const layout: LevelCardLayout = this.levelCardWidgetConfigForm.get('layout').value;

    if (layout === LevelCardLayout.absolute) {
      return true;
    }
    return datasourceUnits !== CapacityUnits.percent;
  }

  public get displayTimewindowConfig(): boolean {
    const datasources = this.levelCardWidgetConfigForm.get('datasources').value;
    return datasourcesHasAggregation(datasources);
  }

  public onlyHistoryTimewindow(): boolean {
    const datasources = this.levelCardWidgetConfigForm.get('datasources').value;
    return datasourcesHasOnlyComparisonAggregation(datasources);
  }

  LevelCardLayout = LevelCardLayout;
  LevelCardLayouts = Object.values(LevelCardLayout) as LevelCardLayout[];
  LevelCardLayoutTranslationMap = levelCardLayoutTranslations;

  DataSourceType = LiquidWidgetDataSourceType;
  DataSourceTypes = Object.values(LiquidWidgetDataSourceType) as LiquidWidgetDataSourceType[];
  DataSourceTypeTranslations = LiquidWidgetDataSourceTypeTranslations;

  shapes = Object.values(Shapes) as Shapes[];
  shapesImageMap: Map<Shapes, string> = new Map();
  ShapesTranslationMap = ShapesTranslations;

  levelCardWidgetConfigForm: FormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  totalVolumeValuePreviewFn = this._totalVolumeValuePreviewFn.bind(this);

  datePreviewFn = this._datePreviewFn.bind(this);

  private keySearchText: string;
  private latestKeySearchTextResult: Array<string>;

  private functionTypeKeys: Array<DataKey> = [];

  private lastKeysId: string;
  private lastFetchedKeys: Array<DataKey>;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private cd: ChangeDetectorRef,
              private $injector: Injector,
              private fb: FormBuilder,
              private resourcesService: ResourcesService,
              private sanitizer: DomSanitizer,
              private utils: UtilsService) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): FormGroup {
    return this.levelCardWidgetConfigForm;
  }

  protected setupConfig(widgetConfig: WidgetConfigComponentData) {
    this.createSvgShapesMapping();

    for (const type of this.utils.getPredefinedFunctionsList()) {
      this.functionTypeKeys.push({
        name: type,
        type: DataKeyType.function
      });
    }
    super.setupConfig(widgetConfig);
  }

  protected setupDefaults(configData: WidgetConfigComponentData) {
    this.setupDefaultDatasource(configData, [{ name: 'fuelLevel', label: 'Fuel Level', type: DataKeyType.timeseries }]);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: LevelCardWidgetSettings = {...levelCardDefaultSettings, ...(configData.config.settings || {})};
    const iconSize = resolveCssSize(configData.config.iconSize);

    this.levelCardWidgetConfigForm = this.fb.group({
      timewindowConfig: [getTimewindowConfig(configData.config), []],
      datasources: [configData.config.datasources, []],

      layout: [settings.layout, []],
      tankSelectionType: [settings.tankSelectionType, []],
      selectedShape: [settings.selectedShape, [Validators.required]],
      shapeAttributeName: [settings.shapeAttributeName, [Validators.required]],
      tankColor: [settings.tankColor, []],
      datasourceUnits: [settings.datasourceUnits, [Validators.required]],

      showTitle: [configData.config.showTitle, []],
      title: [configData.config.title, []],
      titleFont: [configData.config.titleFont, []],
      titleColor: [configData.config.titleColor, []],
      showTitleIcon: [configData.config.showTitleIcon, []],
      iconSize: [iconSize[0], [Validators.min(0)]],
      iconSizeUnit: [iconSize[1], []],
      titleIcon: [configData.config.titleIcon, []],
      iconColor: [configData.config.iconColor, []],

      volumeSource: [settings.volumeSource, []],
      volumeConstant: [settings.volumeConstant, [Validators.required, Validators.min(0.1)]],
      volumeAttributeName: [settings.volumeAttributeName, [Validators.required]],
      volumeUnitsSource: [settings.volumeUnitsSource, []],
      volumeUnits: [settings.volumeUnits, [Validators.required]],
      volumeUnitsAttributeName: [settings.volumeUnitsAttributeName, [Validators.required]],
      volumeFont: [settings.volumeFont, []],
      volumeColor: [settings.volumeColor, []],
      units: [settings.units, [Validators.required]],
      widgetUnitsSource: [settings.widgetUnitsSource, [Validators.required]],
      widgetUnitsAttributeName: [settings.widgetUnitsAttributeName, [Validators.required]],
      decimals: [configData.config.decimals, []],
      liquidColor: [settings.liquidColor, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      showTooltip: [settings.showTooltip, []],
      showTooltipLevel: [settings.showTooltipLevel, []],
      tooltipUnits: [settings.tooltipUnits, []],
      tooltipLevelDecimals: [settings.tooltipLevelDecimals, []],
      tooltipLevelFont: [settings.tooltipLevelFont, []],
      tooltipLevelColor: [settings.tooltipLevelColor, []],
      showTooltipDate: [settings.showTooltipDate, []],
      tooltipDateFormat: [settings.tooltipDateFormat, []],
      tooltipDateFont: [settings.tooltipDateFont, []],
      tooltipDateColor: [settings.tooltipDateColor, []],
      tooltipBackgroundColor: [settings.tooltipBackgroundColor, []],
      tooltipBackgroundBlur: [settings.tooltipBackgroundBlur, []],

      background: [settings.background],
      cardButtons: [this.getCardButtons(configData.config), []],
      borderRadius: [configData.config.borderRadius, []],
      padding: [settings.padding, []],

      actions: [configData.config.actions || {}, []]
    });

    this.levelCardWidgetConfigForm.get('selectedShape').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.cd.detectChanges();
      this.layoutsImageCardsSelect?.imageCardsSelectOptions.notifyOnChanges();
    });
  }

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    setTimewindowConfig(this.widgetConfig.config, config.timewindowConfig);
    this.widgetConfig.config.datasources = config.datasources;

    this.widgetConfig.config.showTitle = config.showTitle;
    this.widgetConfig.config.title = config.title;
    this.widgetConfig.config.titleFont = config.titleFont;
    this.widgetConfig.config.titleColor = config.titleColor;

    this.widgetConfig.config.showTitleIcon = config.showTitleIcon;
    this.widgetConfig.config.iconSize = cssSizeToStrSize(config.iconSize, config.iconSizeUnit);
    this.widgetConfig.config.titleIcon = config.titleIcon;
    this.widgetConfig.config.iconColor = config.iconColor;

    this.widgetConfig.config.decimals = config.decimals;

    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};

    this.widgetConfig.config.settings.layout = config.layout;

    this.widgetConfig.config.settings.tankSelectionType = config.tankSelectionType;
    this.widgetConfig.config.settings.selectedShape = config.selectedShape;
    this.widgetConfig.config.settings.shapeAttributeName = config.shapeAttributeName;
    this.widgetConfig.config.settings.tankColor = config.tankColor;
    this.widgetConfig.config.settings.datasourceUnits = config.datasourceUnits;

    this.widgetConfig.config.settings.volumeSource = config.volumeSource;
    this.widgetConfig.config.settings.volumeConstant = config.volumeConstant;
    this.widgetConfig.config.settings.volumeAttributeName = config.volumeAttributeName;
    this.widgetConfig.config.settings.volumeUnitsSource = config.volumeUnitsSource;
    this.widgetConfig.config.settings.volumeUnitsAttributeName = config.volumeUnitsAttributeName;
    this.widgetConfig.config.settings.volumeUnits = config.volumeUnits;
    this.widgetConfig.config.settings.volumeFont = config.volumeFont;
    this.widgetConfig.config.settings.volumeColor = config.volumeColor;
    this.widgetConfig.config.settings.units = config.units;
    this.widgetConfig.config.settings.widgetUnitsSource = config.widgetUnitsSource;
    this.widgetConfig.config.settings.widgetUnitsAttributeName = config.widgetUnitsAttributeName;
    this.widgetConfig.config.settings.liquidColor = config.liquidColor;
    this.widgetConfig.config.settings.valueFont = config.valueFont;
    this.widgetConfig.config.settings.valueColor = config.valueColor;

    this.widgetConfig.config.settings.showTooltip = config.showTooltip;
    this.widgetConfig.config.settings.showTooltipLevel = config.showTooltipLevel;
    this.widgetConfig.config.settings.tooltipUnits = config.tooltipUnits;
    this.widgetConfig.config.settings.tooltipLevelDecimals = config.tooltipLevelDecimals;
    this.widgetConfig.config.settings.tooltipLevelFont = config.tooltipLevelFont;
    this.widgetConfig.config.settings.tooltipLevelColor = config.tooltipLevelColor;
    this.widgetConfig.config.settings.showTooltipDate = config.showTooltipDate;
    this.widgetConfig.config.settings.tooltipDateFormat = config.tooltipDateFormat;
    this.widgetConfig.config.settings.tooltipDateFont = config.tooltipDateFont;
    this.widgetConfig.config.settings.tooltipDateColor = config.tooltipDateColor;
    this.widgetConfig.config.settings.tooltipBackgroundColor = config.tooltipBackgroundColor;
    this.widgetConfig.config.settings.tooltipBackgroundBlur = config.tooltipBackgroundBlur;

    this.widgetConfig.config.settings.background = config.background;
    this.setCardButtons(config.cardButtons, this.widgetConfig.config);
    this.widgetConfig.config.borderRadius = config.borderRadius;
    this.widgetConfig.config.settings.padding = config.padding;

    this.widgetConfig.config.actions = config.actions;
    return this.widgetConfig;
  }

  protected validatorTriggers(): string[] {
    return [
      'showTooltip', 'showTooltipLevel', 'tankSelectionType', 'datasourceUnits', 'showTitleIcon', 'volumeSource',
      'showTooltipDate', 'layout', 'showTitle', 'widgetUnitsSource', 'volumeUnitsSource'
    ];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    updatedFormSettingsValidators(this.levelCardWidgetConfigForm);

    const showTitleIcon: boolean = this.levelCardWidgetConfigForm.get('showTitleIcon').value;
    const showTitle: boolean = this.levelCardWidgetConfigForm.get('showTitle').value;
    if (showTitle) {
      this.levelCardWidgetConfigForm.get('title').enable({emitEvent: false});
      this.levelCardWidgetConfigForm.get('titleFont').enable({emitEvent: false});
      this.levelCardWidgetConfigForm.get('titleColor').enable({emitEvent: false});
      this.levelCardWidgetConfigForm.get('showTitleIcon').enable({emitEvent: false});
      if (showTitleIcon) {
        this.levelCardWidgetConfigForm.get('titleIcon').enable({emitEvent: false});
        this.levelCardWidgetConfigForm.get('iconColor').enable({emitEvent: false});
        this.levelCardWidgetConfigForm.get('iconSize').enable({emitEvent: false});
        this.levelCardWidgetConfigForm.get('iconSizeUnit').enable({emitEvent: false});
      } else {
        this.levelCardWidgetConfigForm.get('titleIcon').disable({emitEvent: false});
        this.levelCardWidgetConfigForm.get('iconColor').disable({emitEvent: false});
        this.levelCardWidgetConfigForm.get('iconSize').disable({emitEvent: false});
        this.levelCardWidgetConfigForm.get('iconSizeUnit').disable({emitEvent: false});
      }
    } else {
      this.levelCardWidgetConfigForm.get('title').disable({emitEvent: false});
      this.levelCardWidgetConfigForm.get('titleFont').disable({emitEvent: false});
      this.levelCardWidgetConfigForm.get('titleColor').disable({emitEvent: false});
      this.levelCardWidgetConfigForm.get('showTitleIcon').disable({emitEvent: false});
      this.levelCardWidgetConfigForm.get('titleIcon').disable({emitEvent: false});
      this.levelCardWidgetConfigForm.get('iconColor').disable({emitEvent: false});
      this.levelCardWidgetConfigForm.get('iconSize').disable({emitEvent: false});
      this.levelCardWidgetConfigForm.get('iconSizeUnit').disable({emitEvent: false});
    }
  }

  private getCardButtons(config: WidgetConfig): string[] {
    const buttons: string[] = [];
    if (isUndefined(config.enableDataExport) || config.enableDataExport) {
      buttons.push('dataExport');
    }
    if (isUndefined(config.enableFullscreen) || config.enableFullscreen) {
      buttons.push('fullscreen');
    }
    return buttons;
  }

  private setCardButtons(buttons: string[], config: WidgetConfig) {
    config.enableDataExport = buttons.includes('dataExport');
    config.enableFullscreen = buttons.includes('fullscreen');
  }

  private _valuePreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.levelCardWidgetConfigForm.get('units').value);
    const decimals: number = this.levelCardWidgetConfigForm.get('decimals').value;
    return formatValue(22, decimals, units, true);
  }

  private _tooltipValuePreviewFn() {
    const units: string = getSourceTbUnitSymbol(this.levelCardWidgetConfigForm.get('tooltipUnits').value);
    const decimals: number = this.levelCardWidgetConfigForm.get('tooltipLevelDecimals').value;
    return formatValue(32, decimals, units, true);
  }

  private _totalVolumeValuePreviewFn() {
    const value = this.levelCardWidgetConfigForm.get('volumeConstant').value;
    const datasourceUnits = this.levelCardWidgetConfigForm.get('datasourceUnits').value;
    const decimals: number = this.widgetConfig.config.decimals;
    let units = getSourceTbUnitSymbol(this.widgetConfig.config.units);

    if (datasourceUnits !== CapacityUnits.percent) {
      units = datasourceUnits;
    }

    return formatValue((isDefined(value) ? value : 500), decimals, units, true);
  }

  private _datePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.levelCardWidgetConfigForm.get('tooltipDateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }

  private createSvgShapesMapping(): void {
    loadSvgShapesMapping(this.resourcesService).subscribe(shapeMap => {
      this.shapesImageMap = shapeMap;

      this.cd.detectChanges();
      this.layoutsImageCardsSelect?.imageCardsSelectOptions.notifyOnChanges();
      this.shapesImageCardsSelect?.imageCardsSelectOptions.notifyOnChanges();
    });
  }

  createShape(svg: string, layout: LevelCardLayout): SafeUrl {
    return createShapeLayout(svg, layout, this.sanitizer);
  }

  public fetchOptions(searchText: string): Observable<Array<string>> {
    if (this.keySearchText !== searchText || !this.lastFetchedKeys) {
      this.keySearchText = searchText;
      const dataKeyFilter = optionsFilter(this.keySearchText);
      return this.getKeys().pipe(
        tap(res => this.lastFetchedKeys !== res ? this.lastFetchedKeys = res : []),
        map(name => name?.filter(dataKeyFilter).map(key => key.name)),
        tap(res => this.latestKeySearchTextResult = res)
      );
    }
    return of(this.latestKeySearchTextResult);
  }

  private getKeys(): Observable<Array<DataKey>> {
    let fetchObservable: Observable<Array<DataKey>>;
    const callbacks = this.widgetConfigComponent.widgetConfigCallbacks;
    if (this.datasource?.type === DatasourceType.function) {
      fetchObservable = of(this.functionTypeKeys);
    } else if (this.datasource?.type === DatasourceType.entity && this.datasource?.entityAliasId ||
      this.datasource?.type === DatasourceType.device && this.datasource?.deviceId) {
      if (this.datasource?.type === DatasourceType.device) {
        if (this.lastKeysId !== this.datasource?.deviceId || !this.lastFetchedKeys) {
          this.lastKeysId = this.datasource.deviceId;
          fetchObservable = callbacks.fetchEntityKeysForDevice(this.datasource?.deviceId, [DataKeyType.attribute]);
        } else {
          fetchObservable = of(this.lastFetchedKeys);
        }
      } else {
        if (this.lastKeysId !== this.datasource?.entityAliasId || !this.lastFetchedKeys) {
          this.lastKeysId = this.datasource.entityAliasId;
          fetchObservable = callbacks.fetchEntityKeys(this.datasource?.entityAliasId, [DataKeyType.attribute]);
        } else {
          fetchObservable = of(this.lastFetchedKeys);
        }
      }
    } else {
      this.lastKeysId = null;
      fetchObservable = of([]);
    }
    return fetchObservable.pipe(
      share({
        connector: () => new ReplaySubject(1),
        resetOnError: false,
        resetOnComplete: false,
        resetOnRefCountZero: false
      })
    );
  }
}
