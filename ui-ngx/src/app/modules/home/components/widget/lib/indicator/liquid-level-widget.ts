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


import { WidgetContext } from '@home/models/widget-component.models';
import { isDefined, isDefinedAndNotNull, isNumber, isString } from '@core/utils';
import {
  CapacityUnits,
  ConversionType,
  convertLiters, createAbsoluteLayout, createPercentLayout,
  extractValue,
  levelCardDefaultSettings,
  LevelCardLayout,
  LevelCardWidgetSettings,
  LevelSelectOptions,
  Shapes,
  SvgInfo,
  SvgLimits,
  svgMapping
} from '@home/components/widget/lib/indicator/liquid-level-widget.models';
import { Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import {
  ColorProcessor,
  cssTextFromInlineStyle,
  DateFormatProcessor,
  inlineTextStyle
} from '@shared/models/widget-settings.models';
import ITooltipsterInstance = JQueryTooltipster.ITooltipsterInstance;
import { ResourcesService } from '@core/services/resources.service';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'tb-liquid-level-widget',
  template: ''
})
export class LiquidLevelWidgetComponent implements OnInit {

  @Input()
  ctx: WidgetContext;

  svgParams: SvgInfo;

  svg: JQuery<SVGElement>;

  tooltip: ITooltipsterInstance;

  overlayContainer: JQuery<HTMLElement>;

  shape: Shapes;

  settings: LevelCardWidgetSettings;

  tankColor: ColorProcessor;

  volumeColor: ColorProcessor;

  valueColor: ColorProcessor;

  liquidColor: ColorProcessor;

  backgroundOverlayColor: ColorProcessor;

  tooltipLevelColor: ColorProcessor;

  tooltipDateFormat:  DateFormatProcessor;

  tooltipDateColor: ColorProcessor;

  tooltipBackgroundColor: ColorProcessor;

  volume: number;

  tooltipContent: string;

  widgetUnits: string;

  constructor() {
  }

  ngOnInit(): void {
    this.ctx.$scope.liquidLevelWidget = this;
    this.settings = {...levelCardDefaultSettings(), ...this.ctx.widgetConfig, ...this.ctx.settings};
    this.declareStyles();
  }

  public onInit() {
    this.getData().subscribe(data => {
      if (data) {
        const { svg, volume, units } = data;
        if (svg && isString(svg) && this.ctx.$container) {
          this.ctx.$container.html(svg);
          this.svg = this.ctx.$container.find('svg');
          this.svg.on('click', this.cardClick.bind(this));
          this.createSVG();
          this.createValueElement();

          if (this.settings.showTooltip) {
            this.createTooltip();
          }
        }

        if (isDefined(volume) && isNumber(volume)) {
          this.volume = volume;
        }

        if (units) {
          this.widgetUnits = units;
        }

        this.update();
      }
    });
  }

  private declareStyles():void {
    this.tankColor = ColorProcessor.fromSettings(this.settings.tankColor);
    this.volumeColor = ColorProcessor.fromSettings(this.settings.volumeColor);
    this.valueColor = ColorProcessor.fromSettings(this.settings.valueColor);
    this.liquidColor =  ColorProcessor.fromSettings(this.settings.liquidColor);
    this.backgroundOverlayColor = ColorProcessor.fromSettings(this.settings.backgroundOverlayColor);
    this.tooltipLevelColor = ColorProcessor.fromSettings(this.settings.tooltipLevelColor);
    this.tooltipDateColor = ColorProcessor.fromSettings(this.settings.tooltipDateColor);
    this.tooltipBackgroundColor = ColorProcessor.fromSettings(this.settings.tooltipBackgroundColor);

    this.tooltipDateFormat = DateFormatProcessor.fromSettings(this.ctx.$injector, this.settings.tooltipDateFormat);
  }

  private getData(): Observable<{ svg: string; volume: number; units: string }> {
    const entityId: EntityId = {
      entityType: this.ctx.datasources[0].entityType,
      id: this.ctx.datasources[0].entityId
    };

    return this.getShape(entityId).pipe(
      switchMap(shape => {
        this.shape = shape;
        this.svgParams = svgMapping.get(shape);
        if (this.svgParams) {
          return this.loadSVG(this.svgParams.svg).pipe(
            switchMap( svg =>
              this.getSecondaryResources(entityId).pipe(
                map(({ volume, units }) =>
                  ({ svg, volume, units })
                )
              )
            )
          );
        }

        return of(null);
      })
    );
  }

  public update() {
    if (this.svg) {
      this.updateData();
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

  private updateData() {
    const data = this.ctx.data[0]?.data[0]?.map(value => Number(value));
    if (isDefinedAndNotNull(data) && data.length && typeof Number(data[1]) === 'number') {
      const percentage = this.convertInputData(Number(data[1]));
      this.updateSvg(percentage);
      this.updateValueElement(this.convertOutputData(percentage), percentage);

      if (this.settings.showTooltip) {
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
        distance: -33,
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
            backgroundColor: this.getTooltipBackground(),
            backdropFilter: `blur(${this.settings.tooltipBackgroundBlur}px)`,
            width: '100%'
          };

          $(helper.tooltip).css('max-width', this.svg.width() + 'px');
          $(helper.tooltip).find('.tooltipster-box').css(tooltipsterBoxStyles);
          $(helper.tooltip).find('.tooltipster-arrow').empty();

          instance.reposition();
        }
      }).tooltipster('instance');
    });
  }

  private createValueElement(): void {
    const containerOverlay = this.ctx.$container.find('.container-overlay');
    const percentageOverlay = this.ctx.$container.find('.percentage-overlay');
    const absoluteOverlay = this.ctx.$container.find('.absolute-overlay');

    if (this.settings.layout === LevelCardLayout.absolute) {
      this.overlayContainer = absoluteOverlay;
      percentageOverlay.css('visibility', 'hidden');
      if (!this.settings.showBackgroundOverlay) {
        absoluteOverlay.css('visibility', 'hidden');
      }
    } else if (this.settings.layout === LevelCardLayout.percentage) {
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

  private loadSVG(url: string): Observable<string> {
    const resourcesService = this.ctx.$injector.get(ResourcesService);
    return resourcesService.loadJsonResource(url);
  }

  private getShape(entityId: EntityId): Observable<Shapes> {
    if (this.settings.tankSelectionType === LevelSelectOptions.attribute && entityId.id !== NULL_UUID) {
      return this.ctx.attributeService.getEntityAttributes(entityId, null,
        [this.settings.shapeAttributeName]).pipe(
        map(attributes =>
          extractValue(attributes, this.settings.shapeAttributeName, this.settings.selectedShape)
        )
      );
    }

    return of(this.settings.selectedShape);
  }

  private getSecondaryResources(entityId: EntityId): Observable<{ volume: number; units: string }> {
    const attributeNames = this.prepareAttributeNames();

    if (!attributeNames.length || entityId.id === NULL_UUID) {
      return of({
        volume: this.settings.volumeConstant,
        units: this.settings.units
      });
    }

    return this.ctx.attributeService.getEntityAttributes(entityId, null, attributeNames).pipe(
      map(attributes => ({
        volume: extractValue(attributes, this.settings.volumeAttributeName, this.settings.volumeConstant),
        units: extractValue(attributes, this.settings.widgetUnitsAttributeName, this.settings.units)
      }))
    );
  }

  private prepareAttributeNames(): string[] {
    const names = [];
    if (this.settings.volumeSource !== LevelSelectOptions.static) {
      names.push(this.settings.volumeAttributeName);
    }
    if (this.settings.widgetUnitsSource !== LevelSelectOptions.static) {
      names.push(this.settings.widgetUnitsAttributeName);
    }
    return names;
  }

  private updateSvg(percentage: number) {
    const yLimits: SvgLimits = {
      min: this.svgParams.limits.min,
      max: this.svgParams.limits.max
    };

    const newYPos = this.calculatePosition(percentage, yLimits);
    this.updateShapeColor(percentage);
    this.updateLevel(newYPos, percentage);
  }

  private calculatePosition(percentage, limits): number {
    return limits.min + (percentage / 100) * (limits.max - limits.min);
  }

  private updateTooltip(value: number[]): void {
    this.tooltipContent = this.getTooltipContent(value);

    if (this.tooltip) {
      this.tooltip.content(this.tooltipContent);
    }
  }

  private updateLevel(newY: number, percentage: number): void {
    this.liquidColor.update(percentage);
    const fill = this.ctx.$container.find('.tb-liquid-fill');
    const surfaces = this.ctx.$container.find('.tb-liquid-surface');
    const surfacePositionAttr = this.shape !== Shapes.vCylinder ? 'y' : 'cy';
    const animationSpeed = 500;

    if (percentage > 1) {
      fill.css('visibility', 'visible');
      fill.animate({y : newY}, animationSpeed);
      fill.attr('fill', this.liquidColor.color);

      surfaces.each((index, element) => {
        const $element = $(element);
        $element.css('visibility', 'visible');
        $element.animate({[surfacePositionAttr]: newY}, animationSpeed);
        if ($element.hasClass('tb-liquid')) {
          $element.attr('fill', this.liquidColor.color);
        }
      });
    } else {
      fill.css('visibility', 'hidden').stop().clearQueue();
      surfaces.each((index, element) => {
        $(element).css('visibility', 'hidden').stop().clearQueue();
      });
    }
  }

  private updateShapeColor(value): void {
    const shapeStrokes = this.ctx.$container.find('.tb-shape-stroke');
    const shapeFill = this.ctx.$container.find('.tb-shape-fill');
    this.tankColor.update(value);

    shapeStrokes.each((index, element) => {
      $(element).attr('stroke', this.tankColor.color);
    });

    shapeFill.each((index, element) => {
      $(element).attr('fill', this.tankColor.color);
    });
  }

  private updateValueElement(data: number, percentage: number): void {
    let content: string;
    let container: JQuery<HTMLElement>;

    const value = convertLiters(data, this.widgetUnits as CapacityUnits, ConversionType.from)
      .toFixed(this.settings.decimals || 0);
    const valueTextStyle = cssTextFromInlineStyle({...inlineTextStyle(this.settings.valueFont),
                                                          color: this.valueColor.color});
    this.backgroundOverlayColor.update(percentage);
    if (this.overlayContainer) {
      this.overlayContainer.attr('fill', this.backgroundOverlayColor.color);
    }

    if (this.settings.layout === LevelCardLayout.absolute) {
      this.volumeColor.update(percentage);

      const volumeInLiters: number = convertLiters(this.volume, this.settings.volumeUnits as CapacityUnits, ConversionType.to);
      const volume = convertLiters(volumeInLiters, this.widgetUnits as CapacityUnits, ConversionType.from)
        .toFixed(this.settings.decimals || 0);
      const volumeTextStyle = cssTextFromInlineStyle({...inlineTextStyle(this.settings.volumeFont),
                                                             color: this.volumeColor.color});

      container = this.ctx.$container.find('.absolute-value-container');
      content = createAbsoluteLayout({inputValue: value, volume},
        {valueStyle: valueTextStyle, volumeStyle: volumeTextStyle}, this.widgetUnits);

    } else if (this.settings.layout === LevelCardLayout.percentage) {
      container = this.ctx.$container.find('.percentage-value-container');
      content = createPercentLayout(value, valueTextStyle);
    }

    if (content && container) {
      container.html(content);
    }
  }

  private getTooltipContent(value?: number[]): string {
    const contentValue = value || [0, 0];

    if (contentValue[1]) {
      contentValue[1] = this.convertTooltipData(contentValue[1]);
    }

    this.tooltipLevelColor.update(contentValue[1]);
    this.tooltipDateColor.update(contentValue[0]);
    this.tooltipDateFormat.update(contentValue[0]);
    this.tooltipBackgroundColor.update(contentValue);

    const levelTextStyle = cssTextFromInlineStyle({...inlineTextStyle(this.settings.tooltipLevelFont),
      color: this.tooltipLevelColor.color});

    const dateTextStyle = cssTextFromInlineStyle({...inlineTextStyle(this.settings.tooltipDateFont),
      color: this.tooltipDateColor.color, overflow: 'hidden', 'text-overflow': 'ellipsis', 'white-space': 'nowrap'});

    let content = `<div style="display: flex; flex-direction: column;
                               gap: 8px; background-color: transparent">`;

    if (this.settings.showTooltipLevel) {
      const levelValue = contentValue[1]?.toFixed(this.settings.tooltipLevelDecimals) + this.settings.tooltipUnits;
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
    return `<div style="display: flex; justify-content: space-between; gap: 8px">
                <label style="color: rgba(0, 0, 0, 0.38); font-size: 12px; font-weight: 400; white-space: nowrap;">
                    ${labelText}
                </label>
                <label style="${textStyle}">
                  ${contentValue}
                </label>
            </div>`;
  }

  private getTooltipBackground(): string {
    return this.tooltipBackgroundColor.color;
  }

  private convertInputData(value: number): number {
    if (this.settings.datasourceUnits !== CapacityUnits.percent) {
      return (convertLiters(value, this.settings.datasourceUnits, ConversionType.to) /
        convertLiters(this.volume, this.settings.volumeUnits, ConversionType.to)) * 100;
    }

    return value;
  }

  private convertOutputData(value: number): number {
    if (this.widgetUnits !== CapacityUnits.percent) {
      return convertLiters(this.volume * (value / 100), this.settings.volumeUnits, ConversionType.to);
    }

    return value;
  }

  private convertTooltipData(value: number): number {
    const percentage = this.convertInputData(value);
    if (this.settings.tooltipUnits !== CapacityUnits.percent) {
      const liters = this.convertOutputData(percentage);

      return convertLiters(liters, this.settings.tooltipUnits, ConversionType.from);
    } else {
      return percentage;
    }
  }

  public cardClick($event) {
    this.ctx.actionsApi.cardClick($event);
  }
}
