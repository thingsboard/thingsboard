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
import {
  DataKey,
  Datasource,
  DatasourceType,
  WidgetSettings,
  WidgetSettingsComponent
} from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue, isDefined } from '@core/utils';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { DateFormatProcessor, DateFormatSettings } from '@shared/models/widget-settings.models';
import {
  CapacityUnits,
  createShapeLayout,
  fetchEntityKeys,
  fetchEntityKeysForDevice,
  levelCardDefaultSettings,
  LevelCardLayout,
  levelCardLayoutTranslations,
  LiquidWidgetDataSourceType,
  LiquidWidgetDataSourceTypeTranslations,
  loadSvgShapesMapping,
  optionsFilter,
  Shapes,
  ShapesTranslations,
  updatedFormSettingsValidators
} from '@home/components/widget/lib/indicator/liquid-level-widget.models';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';
import { ImageCardsSelectComponent } from '@home/components/widget/lib/settings/common/image-cards-select.component';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { Observable, of, ReplaySubject } from 'rxjs';
import { map, share, tap } from 'rxjs/operators';
import { ResourcesService } from '@core/services/resources.service';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { UtilsService } from '@core/services/utils.service';
import { EntityService } from '@core/http/entity.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-liquid-level-card-widget-settings',
    templateUrl: './liquid-level-card-widget-settings.component.html',
    styleUrls: [],
    standalone: false
})
export class LiquidLevelCardWidgetSettingsComponent extends WidgetSettingsComponent {

  @ViewChild('layoutsImageCardsSelect', { static: false }) layoutsImageCardsSelect: ImageCardsSelectComponent;

  @ViewChild('shapesImageCardsSelect', { static: false }) shapesImageCardsSelect: ImageCardsSelectComponent;

  public get volumeInput(): boolean {
    const datasourceUnits = this.levelCardWidgetSettingsForm.get('datasourceUnits').value;
    const layout: LevelCardLayout = this.levelCardWidgetSettingsForm.get('layout').value;

    if (layout === LevelCardLayout.absolute) {
      return true;
    }
    return datasourceUnits !== CapacityUnits.percent;
  }

  private get datasource(): Datasource {
    if (this.widgetConfig.config.datasources && this.widgetConfig.config.datasources) {
      return this.widgetConfig.config.datasources[0];
    } else {
      return null;
    }
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

  levelCardWidgetSettingsForm: FormGroup;

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
              private $injector: Injector,
              private fb: FormBuilder,
              private resourcesService: ResourcesService,
              private sanitizer: DomSanitizer,
              private cd: ChangeDetectorRef,
              private utils: UtilsService,
              private entityService: EntityService) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.levelCardWidgetSettingsForm;
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
    this.createSvgShapesMapping();

    for (const type of this.utils.getPredefinedFunctionsList()) {
      this.functionTypeKeys.push({
        name: type,
        type: DataKeyType.function
      });
    }
  }

  protected defaultSettings(): WidgetSettings {
    return levelCardDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.levelCardWidgetSettingsForm = this.fb.group({
      tankSelectionType: [settings.tankSelectionType, []],
      selectedShape: [settings.selectedShape, [Validators.required]],
      shapeAttributeName: [settings.shapeAttributeName, [Validators.required]],
      tankColor: [settings.tankColor, []],
      datasourceUnits: [settings.datasourceUnits, [Validators.required]],

      layout: [settings.layout, []],

      volumeSource: [settings.volumeSource, []],
      volumeConstant: [settings.volumeConstant, [Validators.required, Validators.min(0.1)]],
      volumeAttributeName: [settings.volumeAttributeName, [Validators.required]],
      volumeUnitsSource: [settings.volumeUnitsSource, []],
      volumeUnits: [settings.volumeUnits, [Validators.required]],
      volumeUnitsAttributeName: [settings.volumeUnitsAttributeName, [Validators.required]],
      volumeFont: [settings.volumeFont, []],
      volumeColor: [settings.volumeColor, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],
      units: [settings.units, [Validators.required]],
      widgetUnitsSource: [settings.widgetUnitsSource, [Validators.required]],
      widgetUnitsAttributeName: [settings.widgetUnitsAttributeName, [Validators.required]],
      showBackgroundOverlay: [settings.showBackgroundOverlay, []],
      backgroundOverlayColor: [settings.backgroundOverlayColor, []],

      liquidColor: [settings.liquidColor, []],
      background: [settings.background],
      padding: [settings.padding, []],

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
    });

    this.levelCardWidgetSettingsForm.get('selectedShape').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.cd.detectChanges();
      this.layoutsImageCardsSelect?.imageCardsSelectOptions.notifyOnChanges();
    });
  }

  protected validatorTriggers(): string[] {
    return [
      'showBackgroundOverlay', 'showTooltip', 'showTooltipLevel', 'tankSelectionType', 'datasourceUnits',
      'showTooltipDate', 'layout', 'volumeSource', 'widgetUnitsSource', 'volumeUnitsSource'
    ];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    updatedFormSettingsValidators(this.levelCardWidgetSettingsForm);

    if (this.levelCardWidgetSettingsForm.get('showBackgroundOverlay').value) {
      this.levelCardWidgetSettingsForm.get('backgroundOverlayColor').enable({emitEvent: false});
    } else {
      this.levelCardWidgetSettingsForm.get('backgroundOverlayColor').disable({emitEvent: false});
    }
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

  private _valuePreviewFn(): string {
    const units: string = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(32, decimals, units, true);
  }

  private _tooltipValuePreviewFn() {
    const units: string = getSourceTbUnitSymbol(this.levelCardWidgetSettingsForm.get('tooltipUnits').value);
    const decimals: number = this.levelCardWidgetSettingsForm.get('tooltipLevelDecimals').value;
    return formatValue(32, decimals, units, true);
  }

  private _totalVolumeValuePreviewFn() {
    const value = this.levelCardWidgetSettingsForm.get('volumeConstant').value;
    const datasourceUnits = this.levelCardWidgetSettingsForm.get('datasourceUnits').value;
    const decimals: number = this.widgetConfig.config.decimals;
    let units: string = getSourceTbUnitSymbol(this.widgetConfig.config.units);

    if (datasourceUnits !== CapacityUnits.percent) {
      units = datasourceUnits;
    }

    return formatValue((isDefined(value) ? value : 500), decimals, units, true);
  }

  private _datePreviewFn(): string {
    const dateFormat: DateFormatSettings = this.levelCardWidgetSettingsForm.get('tooltipDateFormat').value;
    const processor = DateFormatProcessor.fromSettings(this.$injector, dateFormat);
    processor.update(Date.now());
    return processor.formatted;
  }

  public fetchOptions(searchText: string): Observable<Array<string>> {
    if (this.keySearchText !== searchText) {
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
    if (this.datasource?.type === DatasourceType.function) {
      fetchObservable = of(this.functionTypeKeys);
    } else if (this.datasource?.type === DatasourceType.entity && this.datasource?.entityAliasId ||
      this.datasource?.type === DatasourceType.device && this.datasource?.deviceId) {
      if (this.datasource?.type === DatasourceType.device) {
        if (this.lastKeysId !== this.datasource?.deviceId || !this.lastFetchedKeys) {
          this.lastKeysId = this.datasource.deviceId;
          fetchObservable = fetchEntityKeysForDevice(this.datasource?.deviceId, [DataKeyType.attribute],
            this.entityService);
        } else {
          fetchObservable = of(this.lastFetchedKeys);
        }
      } else {
        if (this.lastKeysId !== this.datasource?.entityAliasId || !this.lastFetchedKeys) {
          this.lastKeysId = this.datasource.entityAliasId;
          fetchObservable = fetchEntityKeys(this.datasource?.entityAliasId, [DataKeyType.attribute],
            this.entityService, this.aliasController);
        } else {
          fetchObservable = of(this.lastFetchedKeys);
        }
      }
    } else {
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
