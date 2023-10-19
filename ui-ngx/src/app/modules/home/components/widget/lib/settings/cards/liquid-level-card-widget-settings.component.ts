///
/// Copyright © 2016-2023 The Thingsboard Authors
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
import { AbstractControl, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue, isDefined } from '@core/utils';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import {
  DateFormatProcessor,
  DateFormatSettings
} from '@shared/models/widget-settings.models';
import {
  levelCardDefaultSettings,
  LevelCardLayout,
  levelCardLayoutTranslations,
  Shapes,
  shapesTranslations,
  svgMapping,
  CapacityUnits,
  LevelSelectOptions,
  createPercentLayout,
  createAbsoluteLayout,
  optionsFilter,
  fetchEntityKeysForDevice,
  fetchEntityKeys
} from '@home/components/widget/lib/indicator/liquid-level-widget.models';
import { UnitsType } from '@shared/models/unit.models';
import { ImageCardsSelectComponent } from '@home/components/widget/lib/settings/common/image-cards-select.component';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { forkJoin, Observable, of } from 'rxjs';
import { map, publishReplay, refCount, tap } from 'rxjs/operators';
import { ResourcesService } from '@core/services/resources.service';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { UtilsService } from '@core/services/utils.service';
import { EntityService } from '@core/http/entity.service';

@Component({
  selector: 'liquid-level-card-widget-settings',
  templateUrl: './liquid-level-card-widget-settings.component.html',
  styleUrls: []
})
export class LiquidLevelCardWidgetSettingsComponent extends WidgetSettingsComponent {

  @ViewChild('layoutsImageCardsSelect', { static: false }) layoutsImageCardsSelect: ImageCardsSelectComponent;

  @ViewChild('shapesImageCardsSelect', { static: false }) shapesImageCardsSelect: ImageCardsSelectComponent;

  public get volumeInput(): boolean {
    const datasourceUnits = this.levelCardWidgetSettingsForm.get('datasourceUnits').value;
    const layout: LevelCardLayout = this.levelCardWidgetSettingsForm.get('layout').value;
    const widgetUnits = this.levelCardWidgetSettingsForm.get('units').value;
    return !(datasourceUnits === CapacityUnits.percent && layout !== LevelCardLayout.absolute
      || (datasourceUnits === CapacityUnits.percent && widgetUnits === CapacityUnits.percent));
  }

  public get widgetUnitsInput(): boolean {
    const layout: LevelCardLayout = this.levelCardWidgetSettingsForm.get('layout').value;

    if (layout === LevelCardLayout.absolute) {
      const datasourceUnits = this.levelCardWidgetSettingsForm.get('datasourceUnits').value;
      return !(datasourceUnits === CapacityUnits.percent);
    }
    return false;
  }

  public get datasource(): Datasource {
    if (this.widgetConfig.config.datasources && this.widgetConfig.config.datasources) {
      return this.widgetConfig.config.datasources[0];
    } else {
      return null;
    }
  }

  shapeSelectOptions = LevelSelectOptions;

  shapes: Shapes[] = [];

  shapesTranslationMap = shapesTranslations;

  unitsType = UnitsType;

  levelCardLayouts = LevelCardLayout;

  levelCardLayoutTranslationMap = levelCardLayoutTranslations;
  shapesImageMap: Map<Shapes, string> = new Map();

  volumeOptions = LevelSelectOptions;

  levelCardWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  tooltipValuePreviewFn = this._tooltipValuePreviewFn.bind(this);

  totalVolumeValuePreviewFn = this._totalVolumeValuePreviewFn.bind(this);

  datePreviewFn = this._datePreviewFn.bind(this);

  keySearchText: string;

  latestKeySearchTextResult: Array<string>;

  datasources: Array<Datasource>;

  functionTypeKeys: Array<DataKey> = [];

  lastKeysId: string;

  lastFetchedKeys: Array<DataKey>;

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder,
              private resourcesService: ResourcesService,
              private sanitizer: DomSanitizer,
              private cd: ChangeDetectorRef,
              private utils: UtilsService,
              private entityService: EntityService) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.levelCardWidgetSettingsForm;
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
    this.shapes = Object.values(Shapes);
    this.createSvgShapesMapping();

    for (const type of this.utils.getPredefinedFunctionsList()) {
      this.functionTypeKeys.push({
        name: type,
        type: DataKeyType.function
      });
    }
  }

  protected defaultSettings(): WidgetSettings {
    return levelCardDefaultSettings();
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
      volumeConstant: [settings.volumeConstant, [Validators.required]],
      volumeAttributeName: [settings.volumeAttributeName, [Validators.required]],
      volumeUnits: [settings.volumeUnits, [Validators.required]],
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

    this.levelCardWidgetSettingsForm.get('selectedShape').valueChanges.subscribe((shape) => {
      this.cd.detectChanges();
      this.layoutsImageCardsSelect?.imageCardsSelectOptions.notifyOnChanges();
    });
  }

  protected validatorTriggers(): string[] {
    return [
      'showBackgroundOverlay', 'showTooltip', 'showTooltipLevel',
      'tankSelectionType', 'datasourceUnits',
      'showTooltipDate', 'units',
      'layout', 'volumeSource',
      'widgetUnitsSource'
    ];
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
    const emitEventFields = [];

    const datasourceUnits: string = this.levelCardWidgetSettingsForm.get('datasourceUnits').value;
    const layout: LevelCardLayout = this.levelCardWidgetSettingsForm.get('layout').value;
    const volumeSource: AbstractControl<LevelSelectOptions> = this.levelCardWidgetSettingsForm.get('volumeSource');
    const widgetUnits: AbstractControl<string> = this.levelCardWidgetSettingsForm.get('units');
    const tooltipUnits: AbstractControl<string> = this.levelCardWidgetSettingsForm.get('tooltipUnits');
    const widgetUnitsSource: AbstractControl<LevelSelectOptions> = this.levelCardWidgetSettingsForm.get('widgetUnitsSource');
    const showTooltipLevel: AbstractControl<boolean> = this.levelCardWidgetSettingsForm.get('showTooltipLevel');
    const showTooltipDate: AbstractControl<boolean> = this.levelCardWidgetSettingsForm.get('showTooltipDate');
    const showTooltip: boolean = this.levelCardWidgetSettingsForm.get('showTooltip').value;

    if (trigger === 'tankSelectionType') {
      const tankSelectionType: LevelSelectOptions = this.levelCardWidgetSettingsForm.get('tankSelectionType').value;
      if (tankSelectionType === LevelSelectOptions.static) {
        this.levelCardWidgetSettingsForm.get('selectedShape').enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('shapeAttributeName').disable({emitEvent: false});
      } else {
        this.levelCardWidgetSettingsForm.get('selectedShape').disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('shapeAttributeName').enable({emitEvent: false});
      }
      emitEventFields.push('selectedShape', 'shapeAttributeName');
    }

    if (trigger === 'datasourceUnits' || trigger === 'layout' || trigger === 'units') {
      if (datasourceUnits === CapacityUnits.percent && (layout !== LevelCardLayout.absolute)
        || (datasourceUnits === CapacityUnits.percent && widgetUnits?.value === CapacityUnits.percent)
      ) {
        volumeSource.disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('volumeConstant').disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('volumeAttributeName').disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('volumeUnits').disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('volumeFont').disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('volumeColor').disable({emitEvent: false});
      } else {
        volumeSource.enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('volumeConstant').enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('volumeAttributeName').enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('volumeUnits').enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('volumeFont').enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('volumeColor').enable({emitEvent: false});
      }

      if (layout === LevelCardLayout.simple && datasourceUnits === CapacityUnits.percent) {
        this.levelCardWidgetSettingsForm.get('valueFont').disable();
        this.levelCardWidgetSettingsForm.get('valueColor').disable();
      } else {
        this.levelCardWidgetSettingsForm.get('valueFont').enable();
        this.levelCardWidgetSettingsForm.get('valueColor').enable();
      }
      emitEventFields.push('volumeSource', 'volumeConstant', 'volumeAttributeName', 'volumeFont', 'volumeColor', 'volumeUnits');
    } else if (trigger === 'volumeSource') {
      if((datasourceUnits !== CapacityUnits.percent) ||
        (layout === LevelCardLayout.absolute && widgetUnits?.value !== CapacityUnits.percent)) {
        if (volumeSource.value === LevelSelectOptions.static) {
          this.levelCardWidgetSettingsForm.get('volumeConstant').enable({emitEvent: false});
          this.levelCardWidgetSettingsForm.get('volumeAttributeName').disable({emitEvent: false});
        } else {
          this.levelCardWidgetSettingsForm.get('volumeConstant').disable({emitEvent: false});
          this.levelCardWidgetSettingsForm.get('volumeAttributeName').enable({emitEvent: false});
        }
        emitEventFields.push('volumeConstant', 'volumeAttributeName');
      }
    }

    if (trigger === 'showBackgroundOverlay') {
      const showBackgroundOverlay: boolean = this.levelCardWidgetSettingsForm.get('showBackgroundOverlay').value;
      if (showBackgroundOverlay) {
        this.levelCardWidgetSettingsForm.get('backgroundOverlayColor').enable({emitEvent: false});
      } else {
        this.levelCardWidgetSettingsForm.get('backgroundOverlayColor').disable({emitEvent: false});
      }
      emitEventFields.push('backgroundOverlayColor');
    }

    if (trigger === 'showTooltip') {
      if (showTooltip) {
        showTooltipLevel.enable({emitEvent: false});
        showTooltipDate.enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('tooltipBackgroundColor').enable();
        this.levelCardWidgetSettingsForm.get('tooltipBackgroundBlur').enable();
      } else {
        showTooltipLevel.disable({emitEvent: false});
        showTooltipDate.disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('tooltipBackgroundColor').disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('tooltipBackgroundBlur').disable({emitEvent: false});
      }
      emitEventFields.push('showTooltipLevel', 'showTooltipDate', 'tooltipBackgroundColor', 'tooltipBackgroundBlur');
    }

    if (trigger === 'showTooltipLevel') {
      if (showTooltipLevel?.value && !showTooltipLevel.disabled) {
        this.levelCardWidgetSettingsForm.get('tooltipUnits').enable();
        this.levelCardWidgetSettingsForm.get('tooltipLevelDecimals').enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('tooltipLevelFont').enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('tooltipLevelColor').enable({emitEvent: false});
      } else {
        this.levelCardWidgetSettingsForm.get('tooltipUnits').disable();
        this.levelCardWidgetSettingsForm.get('tooltipLevelDecimals').disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('tooltipLevelFont').disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('tooltipLevelColor').disable({emitEvent: false});
      }
      emitEventFields.push('tooltipUnits', 'tooltipLevelDecimals', 'tooltipLevelFont', 'tooltipLevelColor');
    }

    if (trigger === 'showTooltipDate') {
      if (showTooltipDate?.value && !showTooltipDate.disabled) {
        this.levelCardWidgetSettingsForm.get('tooltipDateFormat').enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('tooltipDateFont').enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('tooltipDateColor').enable({emitEvent: false});
      } else {
        this.levelCardWidgetSettingsForm.get('tooltipDateFormat').disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('tooltipDateFont').disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('tooltipDateColor').disable({emitEvent: false});
      }
      emitEventFields.push('tooltipDateFormat', 'tooltipDateFont', 'tooltipDateColor');
    }

    if (trigger === 'layout' || trigger === 'datasourceUnits') {
      if (layout === LevelCardLayout.simple) {
        widgetUnits.disable({emitEvent: false});
        tooltipUnits.disable({emitEvent: false});
        widgetUnitsSource.setValue(LevelSelectOptions.static, {emitEvent: false});
        widgetUnitsSource.disable({emitEvent: false});

        this.levelCardWidgetSettingsForm.get('valueFont').disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('valueColor').disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('widgetUnitsAttributeName').disable({emitEvent: false});
      } else if (layout === LevelCardLayout.percentage) {
        if (widgetUnits.value !== CapacityUnits.percent) {
          widgetUnits.setValue(CapacityUnits.percent, {emitEvent: false});
          widgetUnitsSource.setValue(LevelSelectOptions.static, {emitEvent: false});
        }
        widgetUnits.disable({emitEvent: false});
        widgetUnitsSource.disable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('widgetUnitsAttributeName').disable({emitEvent: false});

        if (tooltipUnits.value !== CapacityUnits.percent) {
          tooltipUnits.setValue(CapacityUnits.percent, {emitEvent: false});
        }
        tooltipUnits.disable({emitEvent: false});
      } else {
        widgetUnits.enable({emitEvent: false});
        tooltipUnits.enable({emitEvent: false});
        widgetUnitsSource.enable({emitEvent: false});

        this.levelCardWidgetSettingsForm.get('valueFont').enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('valueColor').enable({emitEvent: false});
        this.levelCardWidgetSettingsForm.get('widgetUnitsAttributeName').enable({emitEvent: false});
      }
      emitEventFields.push('units', 'tooltipUnits', 'valueFont', 'valueColor', 'widgetUnitsSource', 'widgetUnitsAttributeName');
    } else if (trigger === 'widgetUnitsSource') {
      if (layout !== LevelCardLayout.percentage) {
        if (widgetUnitsSource.value === LevelSelectOptions.static) {
          widgetUnits.enable({emitEvent: false});
          this.levelCardWidgetSettingsForm.get('widgetUnitsAttributeName').disable({emitEvent: false});
        } else {
          widgetUnits.disable({emitEvent: false});
          this.levelCardWidgetSettingsForm.get('widgetUnitsAttributeName').enable({emitEvent: false});
        }
        emitEventFields.push('units', 'widgetUnitsAttributeName');
      }
    }

    for (const controlKey in this.levelCardWidgetSettingsForm.controls) {
      if (emitEventFields.includes(controlKey)) {
        this.levelCardWidgetSettingsForm.controls[controlKey].updateValueAndValidity({emitEvent});
      }
    }
  }

  private createSvgShapesMapping(): void {
    const obsArray: Array<Observable<{svg: string; shape: Shapes}>> = [];
    for (const shape of this.shapes) {
      const svgUrl = svgMapping.get(shape).svg;

      const obs = this.resourcesService.loadJsonResource<string>(svgUrl).pipe(
        map((svg) => ({svg, shape}))
      );

      obsArray.push(obs);
    }

    forkJoin(obsArray).subscribe((svgData) => {
      for (const data of svgData) {
        this.shapesImageMap.set(data.shape, data.svg);
      }

      this.cd.detectChanges();
      this.layoutsImageCardsSelect?.imageCardsSelectOptions.notifyOnChanges();
      this.shapesImageCardsSelect?.imageCardsSelectOptions.notifyOnChanges();
    });
  }

  public createShapeLayout(svg: string, layout: LevelCardLayout): SafeUrl {
    if (svg && layout) {
      const parser = new DOMParser();
      const svgImage = parser.parseFromString(svg, 'image/svg+xml');

      if (layout === this.levelCardLayouts.simple) {
        svgImage.querySelector('.container-overlay').remove();
      } else if (layout === this.levelCardLayouts.percentage) {
        svgImage.querySelector('.absolute-overlay').remove();
        svgImage.querySelector('.percentage-value-container').innerHTML = createPercentLayout();
      } else {
        svgImage.querySelector('.absolute-value-container').innerHTML = createAbsoluteLayout();
        svgImage.querySelector('.percentage-overlay').remove();
      }

      const encodedSvg = encodeURIComponent(svgImage.documentElement.outerHTML);

      return this.sanitizer.bypassSecurityTrustResourceUrl(`data:image/svg+xml,${encodedSvg}`);
    }
  }

  public isRequired(formControlName: string): boolean {
    return this.levelCardWidgetSettingsForm.get(formControlName)?.hasValidator(Validators.required);
  }

  private _valuePreviewFn(): string {
    const units: string = this.widgetConfig.config.units;
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(32, decimals, units, true);
  }

  private _tooltipValuePreviewFn() {
    const units: string = this.levelCardWidgetSettingsForm.get('tooltipUnits').value;
    const decimals: number = this.levelCardWidgetSettingsForm.get('tooltipLevelDecimals').value;
    return formatValue(32, decimals, units, true);
  }

  private _totalVolumeValuePreviewFn() {
    const value = this.levelCardWidgetSettingsForm.get('volumeConstant').value;
    const datasourceUnits = this.levelCardWidgetSettingsForm.get('datasourceUnits').value;
    const decimals: number = this.widgetConfig.config.decimals;
    let units: string = this.widgetConfig.config.units;

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
      publishReplay(1),
      refCount()
    );
  }
}
