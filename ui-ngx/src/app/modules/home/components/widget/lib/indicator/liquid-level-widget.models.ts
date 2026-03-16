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

import {
  BackgroundSettings,
  BackgroundType,
  ColorSettings,
  constantColor,
  cssTextFromInlineStyle,
  DateFormatSettings,
  Font,
  lastUpdateAgoDateFormat
} from '@shared/models/widget-settings.models';
import { DataKey, WidgetConfig } from '@shared/models/widget.models';
import { AttributeData, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { forkJoin, Observable, of } from 'rxjs';
import { singleEntityFilterFromDeviceId } from '@shared/models/query/query.models';
import { EntityType } from '@shared/models/entity-type.models';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { EntityService } from '@core/http/entity.service';
import { IAliasController } from '@core/api/widget-api.models';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ResourcesService } from '@core/services/resources.service';
import { FormGroup } from '@angular/forms';
import { TbUnit } from '@shared/models/unit.models';

export interface SvgInfo {
  svg: string;
  limits: SvgLimits;
}

export interface SvgLimits {
  min: number;
  max: number;
}

export interface LevelCardWidgetSettings extends WidgetConfig {
  tankSelectionType: LiquidWidgetDataSourceType;
  selectedShape: Shapes;
  shapeAttributeName: string;
  tankColor: ColorSettings;
  datasourceUnits: CapacityUnits;
  layout: LevelCardLayout;
  volumeSource: LiquidWidgetDataSourceType;
  volumeConstant: number;
  volumeAttributeName: string;
  volumeUnitsSource: LiquidWidgetDataSourceType;
  volumeUnitsAttributeName: string;
  volumeUnits: CapacityUnits;
  volumeFont: Font;
  volumeColor: string;
  liquidColor: ColorSettings;
  valueFont: Font;
  widgetUnitsSource: LiquidWidgetDataSourceType;
  widgetUnitsAttributeName: string;
  valueColor: ColorSettings;
  showBackgroundOverlay: boolean;
  backgroundOverlayColor: ColorSettings;
  showTooltip: boolean;
  showTooltipLevel: boolean;
  tooltipUnits: TbUnit | CapacityUnits;
  tooltipLevelDecimals: number;
  tooltipLevelFont: Font;
  tooltipLevelColor: ColorSettings;
  showTooltipDate: boolean;
  tooltipDateFormat: DateFormatSettings;
  tooltipDateFont: Font;
  tooltipDateColor: string;
  tooltipBackgroundColor: string;
  tooltipBackgroundBlur: number;
  background: BackgroundSettings;
  padding: string;
}

export enum Shapes {
  vOval = 'Vertical Oval',
  vCylinder = 'Vertical Cylinder',
  vCapsule =  'Vertical Capsule',
  rectangle =  'Rectangle',
  hOval = 'Horizontal Oval',
  hEllipse = 'Horizontal Ellipse',
  hDishEnds = 'Horizontal Dish Ends',
  hCylinder = 'Horizontal Cylinder',
  hCapsule = 'Horizontal Capsule',
  hElliptical_2_1 = 'Horizontal 2:1 Elliptical'
}

export enum CapacityUnits {
  percent = '%',
  liters = 'L',
  cubicMillimeter = 'mm³',
  cubicCentimeter = 'cm³',
  cubicMeter = 'm³',
  cubicKilometer = 'km³',
  milliliter = 'mL',
  hectoliter = 'hl',
  cubicInch = 'in³',
  cubicFoot = 'ft³',
  cubicYard = 'yd³',
  fluidOunce = 'fl-oz',
  pint = 'pt',
  quart = 'qt',
  gallon = 'gal',
  oilBarrels = 'bbl'
}

export enum LevelCardLayout {
  simple = 'simple',
  percentage = 'percentage',
  absolute = 'absolute'
}

export enum LiquidWidgetDataSourceType {
  static = 'static',
  attribute = 'attribute'
}

export enum ConversionType {
  to = 'to',
  from = 'from',
}

export const svgMapping = new Map<Shapes, SvgInfo>(
  [
    [
      Shapes.vOval,
      {
        svg: 'assets/widget/liquid-level/shapes/vertical-oval.svg',
        limits: { min: 171, max: 51 }
      }
    ],
    [
      Shapes.vCylinder,
      {
        svg: 'assets/widget/liquid-level/shapes/vertical-cylinder.svg',
        limits: { min: 161, max: 55 }
      }
    ],
    [
      Shapes.vCapsule,
      {
        svg: 'assets/widget/liquid-level/shapes/vertical-capsule.svg',
        limits: { min: 197, max: 25 }
      }
    ],
    [
      Shapes.rectangle,
      {
        svg: 'assets/widget/liquid-level/shapes/rectangle.svg',
        limits: { min: 169, max: 52 }
      }
    ],
    [
      Shapes.hOval,
      {
        svg: 'assets/widget/liquid-level/shapes/horizontal-oval.svg',
        limits: { min: 160, max: 63 }
      }
    ],
    [
      Shapes.hEllipse,
      {
        svg: 'assets/widget/liquid-level/shapes/horizontal-ellipse.svg',
        limits: { min: 160, max: 64 }
      }
    ],
    [
      Shapes.hDishEnds,
      {
        svg: 'assets/widget/liquid-level/shapes/horizontal-dish-ends.svg',
        limits: { min: 173, max: 50 }
      }
    ],
    [
      Shapes.hCylinder,
      {
        svg: 'assets/widget/liquid-level/shapes/horizontal-cylinder.svg',
        limits: { min: 165, max: 56 }
      }
    ],
    [
      Shapes.hCapsule,
      {
        svg: 'assets/widget/liquid-level/shapes/horizontal-capsule.svg',
        limits: { min: 171, max: 52 }
      }
    ],
    [
      Shapes.hElliptical_2_1,
      {
        svg: 'assets/widget/liquid-level/shapes/horizontal-2_1-elliptical.svg',
        limits: { min: 173, max: 50 }
      }
    ]
  ]
);

export const levelCardLayoutTranslations = new Map<LevelCardLayout, string>(
  [
    [LevelCardLayout.simple, 'widgets.liquid-level-card.layout-simple'],
    [LevelCardLayout.percentage, 'widgets.liquid-level-card.layout-percentage'],
    [LevelCardLayout.absolute, 'widgets.liquid-level-card.layout-absolute']
  ]
);

export const LiquidWidgetDataSourceTypeTranslations = new Map<LiquidWidgetDataSourceType, string>(
  [
    [LiquidWidgetDataSourceType.static, 'widgets.liquid-level-card.static'],
    [LiquidWidgetDataSourceType.attribute, 'widgets.liquid-level-card.attribute']
  ]
);

export const ShapesTranslations = new Map<Shapes, string>(
  [
    [Shapes.vOval, 'widgets.liquid-level-card.v-oval'],
    [Shapes.vCylinder, 'widgets.liquid-level-card.v-cylinder'],
    [Shapes.vCapsule, 'widgets.liquid-level-card.v-capsule'],
    [Shapes.rectangle, 'widgets.liquid-level-card.rectangle'],
    [Shapes.hOval, 'widgets.liquid-level-card.h-oval'],
    [Shapes.hEllipse, 'widgets.liquid-level-card.h-ellipse'],
    [Shapes.hDishEnds, 'widgets.liquid-level-card.h-dish-ends'],
    [Shapes.hCylinder, 'widgets.liquid-level-card.h-cylinder'],
    [Shapes.hCapsule, 'widgets.liquid-level-card.h-capsule'],
    [Shapes.hElliptical_2_1, 'widgets.liquid-level-card.h-elliptical_2_1']
  ]
);

export const levelCardDefaultSettings: LevelCardWidgetSettings = {
  tankSelectionType: LiquidWidgetDataSourceType.static,
  selectedShape: Shapes.vCylinder,
  shapeAttributeName: 'tankShape',
  tankColor: constantColor('#242770'),
  datasourceUnits: CapacityUnits.percent,
  layout: LevelCardLayout.percentage,
  showTitle: false,
  title: 'Liquid level',
  titleFont: {
    family: 'Roboto',
    size: 16,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '1.5'
  },
  titleColor: 'rgba(0, 0, 0, 0.87)',
  showTitleIcon: false,
  titleIcon: 'water_drop',
  iconColor: '#5469FF',
  volumeSource: LiquidWidgetDataSourceType.static,
  volumeConstant: 500,
  volumeAttributeName: 'volume',
  volumeUnitsSource: LiquidWidgetDataSourceType.static,
  volumeUnitsAttributeName: 'volumeUnits',
  volumeUnits: CapacityUnits.liters,
  volumeFont: {
    family: 'Roboto',
    size: 14,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '100%'
  },
  volumeColor: 'rgba(0, 0, 0, 0.18)',
  units: CapacityUnits.percent as string,
  widgetUnitsSource: LiquidWidgetDataSourceType.static,
  widgetUnitsAttributeName: 'units',
  decimals: 0,
  liquidColor: constantColor('#7A8BFF'),
  valueFont: {
    family: 'Roboto',
    size: 24,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '100%'
  },
  valueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showBackgroundOverlay: true,
  backgroundOverlayColor: constantColor('rgba(255, 255, 255, 0.76)'),
  showTooltip: true,
  showTooltipLevel: true,
  tooltipUnits: CapacityUnits.percent,
  tooltipLevelDecimals: 0,
  tooltipLevelFont: {
    family: 'Roboto',
    size: 13,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '100%'
  },
  tooltipLevelColor: constantColor('rgba(0, 0, 0, 0.76)'),
  showTooltipDate: true,
  tooltipDateFormat: lastUpdateAgoDateFormat(),
  tooltipDateFont: {
    family: 'Roboto',
    size: 13,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '100%'
  },
  tooltipDateColor: 'rgba(0, 0, 0, 0.76)',
  tooltipBackgroundColor: 'rgba(255, 255, 255, 0.76)',
  tooltipBackgroundBlur: 3,
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: '12px'
};

export const convertLiters = (value: number, units: CapacityUnits, conversionType: ConversionType): number => {
  let factor: number;

  switch (units) {
    case CapacityUnits.liters:
      return value;
    case CapacityUnits.cubicMillimeter:
      factor = 1e6;
      break;
    case CapacityUnits.cubicCentimeter:
    case CapacityUnits.milliliter:
      factor = 1e3;
      break;
    case CapacityUnits.cubicMeter:
      factor = 1e-3;
      break;
    case CapacityUnits.cubicKilometer:
      factor = 1e-15;
      break;
    case CapacityUnits.hectoliter:
      factor = 0.01;
      break;
    case CapacityUnits.cubicInch:
      factor = 61.0237;
      break;
    case CapacityUnits.cubicFoot:
      factor = 1 / 28.3168;
      break;
    case CapacityUnits.cubicYard:
      factor = 1 / 764.555;
      break;
    case CapacityUnits.fluidOunce:
      factor = 33.814;
      break;
    case CapacityUnits.pint:
      factor = 1 / 0.473176;
      break;
    case CapacityUnits.quart:
      factor = 1 / 0.946353;
      break;
    case CapacityUnits.gallon:
      factor = 1 / 3.78541;
      break;
    case CapacityUnits.oilBarrels:
      factor = 1 / 158.987;
      break;
    default:
      return value;
      // throw new Error(`Unknown unit: ${units}`);
  }

  return conversionType === ConversionType.to ? value / factor : value * factor;
};

export const extractValue = <T>(attributes: Array<AttributeData>, attributeName: string): T | undefined => attributes.find(attr => attr.key === attributeName)?.value;

export const valueContainerStyleDefaults = cssTextFromInlineStyle({
  width: '100%',
  height: '100%',
  display: 'flex',
  gap: '8px',
  'text-align': 'center',
  'align-items': 'center',
  'align-content': 'center',
  'justify-content': 'center',
  'font-family': 'Roboto, Helvetica Neue, sans-serif'
});

export const valueTextStyleDefaults = 'letter-spacing: normal; font-style: normal; font-weight: 500; line-height: 100%;' +
  '  font-size: 24px; font-family: Roboto, Helvetica Neue, sans-serif; color: #000000DE';

export const volumeTextStyleDefaults = 'letter-spacing: normal; font-style: normal; font-weight: 500;' +
  ' line-height: 100%; font-size: 14px; font-family: Roboto, Helvetica Neue, sans-serif; color: rgba(0, 0, 0, 0.18)';

export const createAbsoluteLayout = (values?: {inputValue: number | string; volume: number | string},
                                     styles?: {valueStyle: string; volumeStyle: string},
                                     units?: string): string => {
  const inputValue = values?.inputValue ? values?.inputValue : 500;
  const valueTextStyle = styles?.valueStyle ? styles?.valueStyle : valueTextStyleDefaults;
  const volume = values?.volume ? values?.volume : 1000;
  const volumeTextStyle = styles?.volumeStyle ? styles?.volumeStyle : volumeTextStyleDefaults;
  const displayUnits = units ? units : CapacityUnits.liters;

  return `<div xmlns="http://www.w3.org/1999/xhtml" style="${valueContainerStyleDefaults}">
            <div style="display: flex; flex-direction: column; justify-content: center; align-content: center;">
              <label style="${valueTextStyle}">${inputValue}</label>
              <div style="border-top: 1px solid rgba(0, 0, 0, 0.38); height: 1px; width: 100%; margin: 3px 0"></div>
              <label style="${volumeTextStyle}">${volume}</label>
            </div>
            <label style="${valueTextStyle}">${displayUnits}</label>
          </div>`;
};

export const createPercentLayout = (value: number | string = 50, valueTextStyle: string = valueTextStyleDefaults): string =>
  `<div xmlns="http://www.w3.org/1999/xhtml" style="${valueContainerStyleDefaults}">
    <label style="${valueTextStyle}">${value} ${value !== 'N/A' ? CapacityUnits.percent : ''}</label>
  </div>`;

export const optionsFilter = (searchText: string): ((key: DataKey) => boolean) =>
  key => key?.name.toUpperCase().includes(searchText?.toUpperCase());

export const fetchEntityKeysForDevice = (deviceId: string, dataKeyTypes: Array<DataKeyType>,
                                         entityService: EntityService): Observable<Array<DataKey>> => {
  const entityFilter = singleEntityFilterFromDeviceId(deviceId);
  return entityService.getEntityKeysByEntityFilter(
    entityFilter,
    dataKeyTypes, [EntityType.DEVICE],
    {ignoreLoading: true, ignoreErrors: true}
  ).pipe(
    catchError(() => of([]))
  );
};

export const fetchEntityKeys = (entityAliasId: string, dataKeyTypes: Array<DataKeyType>,
                                entityService: EntityService, aliasController: IAliasController): Observable<Array<DataKey>> =>
  aliasController.getAliasInfo(entityAliasId).pipe(
    mergeMap((aliasInfo) => entityService.getEntityKeysByEntityFilter(
      aliasInfo.entityFilter,
      dataKeyTypes,  [],
      {ignoreLoading: true, ignoreErrors: true}
    )),
    catchError(() => of([] as Array<DataKey>))
  );


export const createShapeLayout = (svg: string, layout: LevelCardLayout, sanitizer: DomSanitizer): SafeUrl => {
  if (svg && layout) {
    const parser = new DOMParser();
    const svgImage = parser.parseFromString(svg, 'image/svg+xml');

    if (layout === LevelCardLayout.simple) {
      svgImage.querySelector('.container-overlay').remove();
    } else if (layout === LevelCardLayout.percentage) {
      svgImage.querySelector('.absolute-overlay').remove();
      svgImage.querySelector('.percentage-value-container').innerHTML = createPercentLayout();
    } else {
      svgImage.querySelector('.absolute-value-container').innerHTML = createAbsoluteLayout();
      svgImage.querySelector('.percentage-overlay').remove();
    }

    const encodedSvg = encodeURIComponent(svgImage.documentElement.outerHTML);

    return sanitizer.bypassSecurityTrustResourceUrl(`data:image/svg+xml,${encodedSvg}`);
  }
};

export const loadSvgShapesMapping = (resourcesService: ResourcesService): Observable<Map<Shapes, string>> => {
  const obsArray: Array<Observable<{svg: string; shape: Shapes}>> = [];
  const shapesImageMap: Map<Shapes, string> = new Map();
  svgMapping.forEach((value, shape) => {
    const obs = resourcesService.loadJsonResource<string>(value.svg).pipe(
      map((svg) => ({svg, shape}))
    );

    obsArray.push(obs);
  });

  return forkJoin(obsArray).pipe(
    map(svgData => {
      for (const data of svgData) {
        shapesImageMap.set(data.shape, data.svg);
      }
      return shapesImageMap;
    })
  );
};

export const updatedFormSettingsValidators = (formGroup: FormGroup) => {
  const datasourceUnits: string = formGroup.get('datasourceUnits').value;
  const layout: LevelCardLayout = formGroup.get('layout').value;
  const volumeSource: LiquidWidgetDataSourceType = formGroup.get('volumeSource').value;
  const volumeUnitsSource: LiquidWidgetDataSourceType = formGroup.get('volumeUnitsSource').value;
  const widgetUnitsSource: LiquidWidgetDataSourceType = formGroup.get('widgetUnitsSource').value;
  const showTooltipLevel: boolean = formGroup.get('showTooltipLevel').value;
  const showTooltipDate: boolean = formGroup.get('showTooltipDate').value;
  const showTooltip: boolean = formGroup.get('showTooltip').value;
  const tankSelectionType: LiquidWidgetDataSourceType = formGroup.get('tankSelectionType').value;

  if (tankSelectionType === LiquidWidgetDataSourceType.static) {
    formGroup.get('selectedShape').enable({emitEvent: false});
    formGroup.get('shapeAttributeName').disable({emitEvent: false});
  } else {
    formGroup.get('selectedShape').disable({emitEvent: false});
    formGroup.get('shapeAttributeName').enable({emitEvent: false});
  }

  switch (layout) {
    case LevelCardLayout.simple:
    case LevelCardLayout.percentage:
      formGroup.get('widgetUnitsSource').disable({emitEvent: false});
      formGroup.get('units').disable({emitEvent: false});
      formGroup.get('widgetUnitsAttributeName').disable({emitEvent: false});

      if (datasourceUnits !== CapacityUnits.percent) {
        formGroup.get('volumeSource').enable({emitEvent: false});
        formGroup.get('volumeUnitsSource').enable({emitEvent: false});
        if (volumeSource === LiquidWidgetDataSourceType.static) {
          formGroup.get('volumeConstant').enable({emitEvent: false});
          formGroup.get('volumeAttributeName').disable({emitEvent: false});
        } else {
          formGroup.get('volumeConstant').disable({emitEvent: false});
          formGroup.get('volumeAttributeName').enable({emitEvent: false});
        }
        if (volumeUnitsSource === LiquidWidgetDataSourceType.static) {
          formGroup.get('volumeUnits').enable({emitEvent: false});
          formGroup.get('volumeUnitsAttributeName').disable({emitEvent: false});
        } else {
          formGroup.get('volumeUnits').disable({emitEvent: false});
          formGroup.get('volumeUnitsAttributeName').enable({emitEvent: false});
        }
      } else {
        formGroup.get('volumeSource').disable({emitEvent: false});
        formGroup.get('volumeConstant').disable({emitEvent: false});
        formGroup.get('volumeAttributeName').disable({emitEvent: false});
        formGroup.get('volumeUnitsSource').disable({emitEvent: false});
        formGroup.get('volumeUnits').disable({emitEvent: false});
        formGroup.get('volumeUnitsAttributeName').disable({emitEvent: false});
      }

      if (layout === LevelCardLayout.simple) {
        formGroup.get('decimals')?.disable({emitEvent: false});
        formGroup.get('valueFont').disable({emitEvent: false});
        formGroup.get('valueColor').disable({emitEvent: false});
      } else {
        formGroup.get('decimals')?.enable({emitEvent: false});
        formGroup.get('valueFont').enable({emitEvent: false});
        formGroup.get('valueColor').enable({emitEvent: false});
      }

      formGroup.get('volumeFont').disable({emitEvent: false});
      formGroup.get('volumeColor').disable({emitEvent: false});

      break;
    case LevelCardLayout.absolute:
      formGroup.get('widgetUnitsSource').enable({emitEvent: false});
      if (widgetUnitsSource === LiquidWidgetDataSourceType.static) {
        formGroup.get('units').enable({emitEvent: false});
        formGroup.get('widgetUnitsAttributeName').disable({emitEvent: false});
      } else {
        formGroup.get('units').disable({emitEvent: false});
        formGroup.get('widgetUnitsAttributeName').enable({emitEvent: false});
      }

      formGroup.get('volumeSource').enable({emitEvent: false});
      formGroup.get('volumeUnitsSource').enable({emitEvent: false});
      if (volumeSource === LiquidWidgetDataSourceType.static) {
        formGroup.get('volumeConstant').enable({emitEvent: false});
        formGroup.get('volumeAttributeName').disable({emitEvent: false});
      } else {
        formGroup.get('volumeConstant').disable({emitEvent: false});
        formGroup.get('volumeAttributeName').enable({emitEvent: false});
      }
      if (volumeUnitsSource === LiquidWidgetDataSourceType.static) {
        formGroup.get('volumeUnits').enable({emitEvent: false});
        formGroup.get('volumeUnitsAttributeName').disable({emitEvent: false});
      } else {
        formGroup.get('volumeUnits').disable({emitEvent: false});
        formGroup.get('volumeUnitsAttributeName').enable({emitEvent: false});
      }

      if (formGroup.get('decimals')) {
        formGroup.get('decimals').enable({emitEvent: false});
      }
      formGroup.get('valueFont').enable({emitEvent: false});
      formGroup.get('valueColor').enable({emitEvent: false});

      formGroup.get('volumeFont').enable({emitEvent: false});
      formGroup.get('volumeColor').enable({emitEvent: false});

      break;
  }

  if (showTooltip) {
    formGroup.get('showTooltipLevel').enable({emitEvent: false});
    formGroup.get('showTooltipDate').enable({emitEvent: false});
    formGroup.get('tooltipBackgroundColor').enable({emitEvent: false});
    formGroup.get('tooltipBackgroundBlur').enable({emitEvent: false});

    if (showTooltipLevel) {
      formGroup.get('tooltipLevelDecimals').enable({emitEvent: false});
      formGroup.get('tooltipLevelFont').enable({emitEvent: false});
      formGroup.get('tooltipLevelColor').enable({emitEvent: false});
      formGroup.get('tooltipUnits').enable({emitEvent: false});
    } else {
      formGroup.get('tooltipUnits').disable({emitEvent: false});
      formGroup.get('tooltipLevelDecimals').disable({emitEvent: false});
      formGroup.get('tooltipLevelFont').disable({emitEvent: false});
      formGroup.get('tooltipLevelColor').disable({emitEvent: false});
    }

    if (showTooltipDate) {
      formGroup.get('tooltipDateFormat').enable({emitEvent: false});
      formGroup.get('tooltipDateFont').enable({emitEvent: false});
      formGroup.get('tooltipDateColor').enable({emitEvent: false});
    } else {
      formGroup.get('tooltipDateFormat').disable({emitEvent: false});
      formGroup.get('tooltipDateFont').disable({emitEvent: false});
      formGroup.get('tooltipDateColor').disable({emitEvent: false});
    }
  } else {
    formGroup.get('showTooltipLevel').disable({emitEvent: false});
    formGroup.get('showTooltipDate').disable({emitEvent: false});
    formGroup.get('tooltipBackgroundColor').disable({emitEvent: false});
    formGroup.get('tooltipBackgroundBlur').disable({emitEvent: false});

    formGroup.get('tooltipUnits').disable({emitEvent: false});
    formGroup.get('tooltipLevelDecimals').disable({emitEvent: false});
    formGroup.get('tooltipLevelFont').disable({emitEvent: false});
    formGroup.get('tooltipLevelColor').disable({emitEvent: false});

    formGroup.get('tooltipDateFormat').disable({emitEvent: false});
    formGroup.get('tooltipDateFont').disable({emitEvent: false});
    formGroup.get('tooltipDateColor').disable({emitEvent: false});
  }
};
