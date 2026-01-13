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
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnInit,
  TemplateRef,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { isDefined, isDefinedAndNotNull, isEmptyStr, isNotEmptyStr, isNumeric, isUndefinedOrNull } from '@core/utils';
import {
  CapacityUnits,
  ConversionType,
  convertLiters,
  createAbsoluteLayout,
  createPercentLayout,
  extractValue,
  levelCardDefaultSettings,
  LevelCardLayout,
  LevelCardWidgetSettings,
  LiquidWidgetDataSourceType,
  Shapes,
  SvgInfo,
  SvgLimits,
  svgMapping
} from '@home/components/widget/lib/indicator/liquid-level-widget.models';
import { forkJoin, Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import {
  backgroundStyle,
  ColorProcessor,
  ComponentStyle,
  cssTextFromInlineStyle,
  DateFormatProcessor,
  inlineTextStyle,
  overlayStyle
} from '@shared/models/widget-settings.models';
import { ResourcesService } from '@core/services/resources.service';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { TranslateService } from '@ngx-translate/core';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { DataEntry } from '@shared/models/widget.models';
import {
  getSourceTbUnitSymbol,
  isNotEmptyTbUnits,
  isTbUnitMapping,
  TbUnit,
  TbUnitConverter
} from '@shared/models/unit.models';
import { UnitService } from '@core/services/unit.service';
import ITooltipsterInstance = JQueryTooltipster.ITooltipsterInstance;

@Component({
  selector: 'tb-liquid-level-widget',
  templateUrl: './liquid-level-widget.component.html',
  styleUrls: ['./liquid-level-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class LiquidLevelWidgetComponent implements OnInit {

  @ViewChild('liquidLevelContent', {static: true})
  liquidLevelContent: ElementRef<HTMLElement>;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  hasCardClickAction = false;

  errorsMsg: string[] = [];

  private svgParams: SvgInfo;

  private svg: JQuery<SVGElement>;
  private tooltip: ITooltipsterInstance;
  private overlayContainer: JQuery<HTMLElement>;
  private shape: Shapes;

  private settings: LevelCardWidgetSettings;

  private tankColor: ColorProcessor;
  private valueColor: ColorProcessor;
  private liquidColor: ColorProcessor;
  private backgroundOverlayColor: ColorProcessor;
  private tooltipLevelColor: ColorProcessor;

  private tooltipDateFormat:  DateFormatProcessor;

  private volume: number;
  private tooltipContent: string;
  private widgetUnits: TbUnit;
  private widgetUnitsSymbol: string;
  private widgetUnitsConvertor: TbUnitConverter;
  private volumeUnits: string;
  private tooltipUnitSymbol: string;
  private tooltipUnitConvertor: TbUnitConverter;

  private capacityUnits = Object.values(CapacityUnits);

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private cd: ChangeDetectorRef,
              private resourcesService: ResourcesService,
              private translate: TranslateService,
              private unitService: UnitService) {
  }

  ngOnInit(): void {
    this.ctx.$scope.liquidLevelWidget = this;
    this.settings = {...levelCardDefaultSettings, ...this.ctx.settings};
    this.declareStyles();

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    this.hasCardClickAction = this.ctx.actionsApi.getActionDescriptors('cardClick').length > 0;

    this.getData().subscribe(data => {
      if (data) {
        const { svg, volume, units, volumeUnits } = data;
        if (svg && isNotEmptyStr(svg) && this.liquidLevelContent.nativeElement) {
          const jQueryContainerElement = $(this.liquidLevelContent.nativeElement);
          jQueryContainerElement.html(svg);
          this.svg = jQueryContainerElement.find('svg');
          this.createSVG();
          this.createValueElement();

          if (this.settings.showTooltip && (this.settings.showTooltipLevel || this.settings.showTooltipDate)) {
            this.createTooltip();
          }
        }

        if (isDefined(volume) && !isNaN(Number(volume))) {
          this.volume = Number(volume);
        }

        if (volumeUnits) {
          this.volumeUnits = volumeUnits;
        }

        if (units) {
          this.widgetUnits = units;
        }

        this.widgetUnitsSymbol = this.unitService.getTargetUnitSymbol(this.widgetUnits);
        this.widgetUnitsConvertor = this.unitService.geUnitConverter(this.widgetUnits);

        this.tooltipUnitSymbol = this.unitService.getTargetUnitSymbol(this.settings.tooltipUnits);
        this.tooltipUnitConvertor = this.unitService.geUnitConverter(this.settings.tooltipUnits);

        this.update(true);
      }
    });
  }

  private declareStyles(): void {
    this.tankColor = ColorProcessor.fromSettings(this.settings.tankColor);
    this.valueColor = ColorProcessor.fromSettings(this.settings.valueColor);
    this.liquidColor =  ColorProcessor.fromSettings(this.settings.liquidColor);
    this.backgroundOverlayColor = ColorProcessor.fromSettings(this.settings.backgroundOverlayColor);
    this.tooltipLevelColor = ColorProcessor.fromSettings(this.settings.tooltipLevelColor);

    this.tooltipDateFormat = DateFormatProcessor.fromSettings(this.ctx.$injector, this.settings.tooltipDateFormat);
  }

  private getData(): Observable<{ svg: string; volume: number; units: TbUnit; volumeUnits: string}> {
    if (this.ctx.datasources?.length) {
      const entityId: EntityId = {
        entityType: this.ctx.datasources[0].entityType,
        id: this.ctx.datasources[0].entityId
      };

      return this.getShape(entityId).pipe(
        switchMap(shape => {
          this.shape = shape;
          this.svgParams = svgMapping.get(shape);
          if (this.svgParams) {
            return forkJoin([
              this.resourcesService.loadJsonResource<string>(this.svgParams.svg),
              this.getTankersParams(entityId)
            ]).pipe(
              map(params => ({svg: params[0], ...params[1]}))
            );
          }
          return of(null);
        })
      );
    }

    return of(null);
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public update(ignoreAnimation?: boolean) {
    if (this.svg) {
      this.updateData(ignoreAnimation);
    }
  }

  public destroy() {

  }

  private createSVG() {
    if (this.svg) {
      this.svg.css('display', 'block')
        .css('overflow', 'hidden')
        .css('width', '100%')
        .css('height', '100%');
    }
  }

  private updateData(ignoreAnimation?: boolean) {
    const data = this.ctx.data[0]?.data[0];
    if (data && isDefinedAndNotNull(data[1])) {
      const percentage = isNumeric(data[1]) ? this.convertInputData(data[1]) : 0;
      this.updateSvg(percentage, ignoreAnimation);
      this.updateValueElement(data[1], percentage);

      if (this.settings.showTooltip && (this.settings.showTooltipLevel || this.settings.showTooltipDate)) {
        this.updateTooltip(data);
      }
    }
  }

  private createTooltip(): void {
    this.tooltipContent = this.getTooltipContent();

    import('tooltipster').then(() => {
      if ($.tooltipster && this.tooltip) {
        this.tooltip.destroy();
        this.tooltip = null;
      }
      this.tooltip = this.svg.tooltipster({
        contentAsHTML: true,
        theme: 'tooltipster-shadow',
        side: 'top',
        delay: 10,
        distance: this.settings.showTitle ? -33 : -63,
        triggerClose: {
          click: true,
          tap: true,
          scroll: true,
          mouseleave: true
        },
        animation: 'grow',
        updateAnimation: null,
        trackOrigin: true,
        functionBefore: (instance, helper) => {
          instance.content(this.tooltipContent);
        },
        functionReady: (instance, helper) => {
          const tooltipsterBoxStyles: JQuery.PlainObject = {
            backgroundColor: this.settings.tooltipBackgroundColor,
            backdropFilter: `blur(${this.settings.tooltipBackgroundBlur}px)`,
            width: '100%',
            height: '100%'
          };

          $(helper.tooltip).css('max-width', this.svg.width() + 'px');
          $(helper.tooltip).css('max-height', this.svg.height() + 'px');
          $(helper.tooltip).find('.tooltipster-box').css(tooltipsterBoxStyles);
          $(helper.tooltip).find('.tooltipster-arrow').empty();

          instance.reposition();
        }
      }).tooltipster('instance');
    });
  }

  private createValueElement(): void {
    const jQueryContainerElement = $(this.liquidLevelContent.nativeElement);
    const containerOverlay = jQueryContainerElement.find('.container-overlay');
    const percentageOverlay = jQueryContainerElement.find('.percentage-overlay');
    const absoluteOverlay = jQueryContainerElement.find('.absolute-overlay');

    if (this.settings.layout === LevelCardLayout.absolute && !this.errorsMsg.length) {
      this.overlayContainer = absoluteOverlay;
      percentageOverlay.css('visibility', 'hidden');
      if (!this.settings.showBackgroundOverlay) {
        absoluteOverlay.css('visibility', 'hidden');
      }
    } else if (this.settings.layout === LevelCardLayout.percentage && !this.errorsMsg.length) {
      this.overlayContainer = percentageOverlay;
      absoluteOverlay.css('visibility', 'hidden');
      if (!this.settings.showBackgroundOverlay) {
        percentageOverlay.css('visibility', 'hidden');
      }
    } else {
      containerOverlay.css('visibility', 'hidden');
    }

    if (this.overlayContainer) {
      this.overlayContainer.attr('fill', this.backgroundOverlayColor.color);
      this.overlayContainer.removeAttr('fill-opacity');
    }
  }

  private getShape(entityId: EntityId): Observable<Shapes> {
    if (this.settings.tankSelectionType === LiquidWidgetDataSourceType.attribute && entityId.id !== NULL_UUID) {
      return this.ctx.attributeService.getEntityAttributes(entityId, null, [this.settings.shapeAttributeName])
        .pipe(map(attributes => {
            const shape = extractValue<Shapes>(attributes, this.settings.shapeAttributeName);
            if (!shape || !svgMapping.has(shape)) {
              this.createdErrorMsg(this.settings.shapeAttributeName, isUndefinedOrNull(shape) || isEmptyStr(shape));
              return this.settings.selectedShape;
            }
            return shape;
          }
        ));
    }
    return of(this.settings.selectedShape);
  }

  private getTankersParams(entityId: EntityId): Observable<{ volume: number; units: TbUnit; volumeUnits: string }> {
    const isVolumeStatic = this.settings.layout !== LevelCardLayout.absolute
      && this.settings.datasourceUnits === CapacityUnits.percent
      || this.settings.volumeSource === LiquidWidgetDataSourceType.static;
    const isUnitStatic =  this.settings.layout !== LevelCardLayout.absolute ||
      this.settings.widgetUnitsSource === LiquidWidgetDataSourceType.static;
    const isVolumeUnitStatic = this.settings.layout !== LevelCardLayout.absolute
      && this.settings.datasourceUnits === CapacityUnits.percent
      || this.settings.volumeUnitsSource === LiquidWidgetDataSourceType.static;

    const attributeKeys: string[] = [];

    if (!isVolumeStatic) {
      attributeKeys.push(this.settings.volumeAttributeName);
    }

    if (!isUnitStatic) {
      attributeKeys.push(this.settings.widgetUnitsAttributeName);
    }

    if (!isVolumeUnitStatic) {
      attributeKeys.push(this.settings.volumeUnitsAttributeName);
    }

    if (!attributeKeys.length || entityId.id === NULL_UUID) {
      return of({
        volume: this.settings.volumeConstant,
        volumeUnits: this.settings.volumeUnits,
        units: this.settings.units
      });
    }

    return this.ctx.attributeService.getEntityAttributes(entityId, null, attributeKeys).pipe(
      map(attributes => {
        let volume = isVolumeStatic ? this.settings.volumeConstant :
          extractValue<number>(attributes, this.settings.volumeAttributeName);
        let volumeUnits = isVolumeUnitStatic ? this.settings.volumeUnits :
          extractValue<string>(attributes, this.settings.volumeUnitsAttributeName);
        let units = isUnitStatic ? this.settings.units :
          extractValue<string>(attributes, this.settings.widgetUnitsAttributeName);

        if (!isVolumeStatic && (!volume || !isNumeric(volume) || volume < 0.1)) {
          this.createdErrorMsg(this.settings.volumeAttributeName, isUndefinedOrNull(volume) || isEmptyStr(volume));
          volume = this.settings.volumeConstant;
        }

        if (!isUnitStatic) {
          if (isNotEmptyTbUnits(units) && !isTbUnitMapping(units)) {
            const normalizeUnits = (units as string).normalize().trim();
            units = this.capacityUnits.find(unit => unit.normalize() === normalizeUnits);
          }
          if (isUndefinedOrNull(units) || !isNotEmptyStr(units)) {
            this.createdErrorMsg(this.settings.widgetUnitsAttributeName, isUndefinedOrNull(units) || isEmptyStr(units));
            units = this.settings.units;
          }
        }

        if (!isVolumeUnitStatic) {
          if (isNotEmptyStr(volumeUnits)) {
            const normalizeUnits = volumeUnits.normalize().trim();
            volumeUnits = this.capacityUnits.find(unit => unit.normalize() === normalizeUnits);
          }
          if (isUndefinedOrNull(volumeUnits) || !isNotEmptyStr(volumeUnits)) {
            this.createdErrorMsg(this.settings.widgetUnitsAttributeName,
              isUndefinedOrNull(volumeUnits) || isEmptyStr(volumeUnits));
            volumeUnits = this.settings.volumeUnits;
          }
        }

        return {
          volume,
          volumeUnits,
          units
        };
      })
    );
  }

  private createdErrorMsg(attributeName: string, isEmpty = false) {
    if (isEmpty) {
      this.errorsMsg.push(this.translate.instant('widgets.liquid-level-card.attribute-key-not-set', {attributeName}));
    } else {
      this.errorsMsg.push(this.translate.instant('widgets.liquid-level-card.attribute-key-invalid', {attributeName}));
    }
    this.cd.markForCheck();
  }

  private updateSvg(percentage: number, ignoreAnimation?: boolean) {
    const yLimits: SvgLimits = {
      min: this.svgParams.limits.min,
      max: this.svgParams.limits.max
    };

    const newYPos = this.calculatePosition(percentage, yLimits);
    this.updateShapeColor(percentage);
    this.updateLevel(newYPos, percentage, ignoreAnimation);
  }

  private calculatePosition(percentage: number, limits: SvgLimits): number {
    if (percentage >= 100) {
      return limits.max;
    } if (percentage <= 0) {
      return limits.min;
    }
    return limits.min + (percentage / 100) * (limits.max - limits.min);
  }

  private updateTooltip(value: DataEntry): void {
    this.tooltipContent = this.getTooltipContent(value);

    if (this.tooltip) {
      this.tooltip.content(this.tooltipContent);
    }
  }

  private updateLevel(newY: number, percentage: number, ignoreAnimation = false): void {
    this.liquidColor.update(percentage);
    const jQueryContainerElement = $(this.liquidLevelContent.nativeElement);
    const fill = jQueryContainerElement.find('.tb-liquid-fill');
    const surfaces = jQueryContainerElement.find('.tb-liquid-surface');
    const surfacePositionAttr = this.shape !== Shapes.vCylinder ? 'y' : 'cy';
    const animationSpeed = 500;

    const levelColor = this.errorsMsg.length ? 'transparent' : this.liquidColor.color;

    if (ignoreAnimation) {
      fill.css({y : newY});
    } else {
      fill.animate({y : newY}, animationSpeed);
    }
    fill.attr('fill', levelColor);

    surfaces.each((index, element) => {
      const $element = $(element);
      if (ignoreAnimation) {
        $element.css({[surfacePositionAttr]: newY});
      } else {
        $element.animate({[surfacePositionAttr]: newY}, animationSpeed);
      }
      if ($element.hasClass('tb-liquid')) {
        $element.attr('fill', levelColor);
      }
    });
  }

  private updateShapeColor(value: number): void {
    const jQueryContainerElement = $(this.liquidLevelContent.nativeElement);
    const shapeStrokes = jQueryContainerElement.find('.tb-shape-stroke');
    const shapeFill = jQueryContainerElement.find('.tb-shape-fill');
    this.tankColor.update(value);

    const shapeColor = this.errorsMsg.length ? '#CACACA' : this.tankColor.color;

    shapeStrokes.each((index, element) => {
      $(element).attr('stroke', shapeColor);
    });

    shapeFill.each((index, element) => {
      $(element).attr('fill', shapeColor);
    });
  }

  private updateValueElement(data: any, percentage: number): void {
    let content: string;
    let container: JQuery<HTMLElement>;
    const jQueryContainerElement = $(this.liquidLevelContent.nativeElement);
    let value = 'N/A';

    if (isNumeric(data)) {
      value = this.widgetUnitsConvertor(convertLiters(this.convertOutputData(percentage), this.widgetUnits as CapacityUnits, ConversionType.from))
          .toFixed(this.settings.decimals || 0);
    }
    this.valueColor.update(value);
    const valueTextStyle = cssTextFromInlineStyle({...inlineTextStyle(this.settings.valueFont),
                                                          color: this.valueColor.color});
    this.backgroundOverlayColor.update(percentage);
    if (this.overlayContainer) {
      this.overlayContainer.attr('fill', this.backgroundOverlayColor.color);
    }

    if (this.settings.layout === LevelCardLayout.absolute) {
      let volume: number | string;
      if (this.widgetUnits !== CapacityUnits.percent) {
        const volumeInLiters: number = convertLiters(this.volume, this.volumeUnits as CapacityUnits, ConversionType.to);
        volume = this.widgetUnitsConvertor(convertLiters(volumeInLiters, this.widgetUnits as CapacityUnits, ConversionType.from))
          .toFixed(this.settings.decimals || 0);
      } else {
        volume = this.widgetUnitsConvertor(this.volume).toFixed(this.settings.decimals || 0);
      }

      const volumeTextStyle = cssTextFromInlineStyle({...inlineTextStyle(this.settings.volumeFont),
                                                             color: this.settings.volumeColor});

      container = jQueryContainerElement.find('.absolute-value-container');
      content = createAbsoluteLayout({inputValue: value, volume},
        {valueStyle: valueTextStyle, volumeStyle: volumeTextStyle}, this.widgetUnitsSymbol);

    } else if (this.settings.layout === LevelCardLayout.percentage) {
      container = jQueryContainerElement.find('.percentage-value-container');
      content = createPercentLayout(value, valueTextStyle);
    }

    if (content && container) {
      container.html(content);
    }
  }

  private getTooltipContent(value?: DataEntry): string {
    const contentValue = value || [0, ''];
    let tooltipValue: string | number = 'N/A';

    if (isNumeric(contentValue[1])) {
      tooltipValue = this.convertTooltipData(contentValue[1]);
    }

    this.tooltipLevelColor.update(tooltipValue);
    this.tooltipDateFormat.update(contentValue[0]);

    const levelTextStyle = cssTextFromInlineStyle({...inlineTextStyle(this.settings.tooltipLevelFont),
      color: this.tooltipLevelColor.color});

    const dateTextStyle = cssTextFromInlineStyle({...inlineTextStyle(this.settings.tooltipDateFont),
      color: this.settings.tooltipDateColor, overflow: 'hidden', 'text-overflow': 'ellipsis', 'white-space': 'nowrap'});

    let content = `<div style="display: flex; flex-direction: column;
                               gap: 8px; background-color: transparent">`;

    if (this.settings.showTooltipLevel) {
      const levelValue = typeof tooltipValue == 'number'
          ? `${this.tooltipUnitConvertor(tooltipValue).toFixed(this.settings.tooltipLevelDecimals)}&nbsp;${this.tooltipUnitSymbol}`
          : 'N/A';
      content += this.createTooltipContent(
        this.ctx.translate.instant('widgets.liquid-level-card.level'),
        levelValue,
        levelTextStyle
      );
    }

    if (this.settings.showTooltipDate) {
      content += this.createTooltipContent(
        this.ctx.translate.instant('widgets.liquid-level-card.last-update'),
        this.tooltipDateFormat.formatted,
        dateTextStyle
      );
    }

    content += '</div>';

    return content;
  }

  private createTooltipContent(labelText: string, contentValue: string, textStyle: string): string {
    return `<div style="display: flex; justify-content: space-between; gap: 8px; align-items: baseline">
                <label style="color: rgba(0, 0, 0, 0.38); font-size: 12px; font-weight: 400; white-space: nowrap;">
                    ${labelText}
                </label>
                <label style="${textStyle}">
                  ${contentValue}
                </label>
            </div>`;
  }

  private convertInputData(value: any): number {
    if (this.settings.datasourceUnits !== CapacityUnits.percent) {
      return (convertLiters(Number(value), this.settings.datasourceUnits, ConversionType.to) /
        convertLiters(this.volume, this.volumeUnits as CapacityUnits, ConversionType.to)) * 100;
    }

    return Number(value);
  }

  private convertOutputData(value: number): number {
    if (this.widgetUnits !== CapacityUnits.percent) {
      return convertLiters(this.volume * (value / 100), this.volumeUnits as CapacityUnits, ConversionType.to);
    }

    return value;
  }

  private convertTooltipData(value: number): number {
    const percentage = this.convertInputData(value);
    if (this.settings.tooltipUnits !== CapacityUnits.percent) {
      const liters = this.convertOutputData(percentage);
      return convertLiters(liters, getSourceTbUnitSymbol(this.settings.tooltipUnits) as any, ConversionType.from)
    } else {
      return percentage;
    }
  }

  public cardClick($event: Event) {
    this.ctx.actionsApi.cardClick($event);
  }
}
