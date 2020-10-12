///
/// Copyright © 2016-2020 The Thingsboard Authors
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
import {
  createLabelFromDatasource,
  deepClone,
  insertVariable,
  isDefined, isDefinedAndNotNull,
  isEqual,
  isNumber,
  isUndefined
} from '@app/core/utils';
import { IWidgetSubscription, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import {
  DataKey,
  Datasource,
  DatasourceData,
  DatasourceType,
  JsonSettingsSchema,
  widgetType
} from '@app/shared/models/widget.models';
import {
  ChartType,
  flotDatakeySettingsSchema,
  flotPieDatakeySettingsSchema,
  flotPieSettingsSchema,
  flotSettingsSchema,
  TbFlotAxisOptions,
  TbFlotHoverInfo,
  TbFlotKeySettings,
  TbFlotPlotAxis,
  TbFlotPlotDataSeries,
  TbFlotPlotItem,
  TbFlotSeries,
  TbFlotSeriesHoverInfo,
  TbFlotSettings,
  TbFlotThresholdKeySettings,
  TbFlotThresholdMarking,
  TbFlotTicksFormatterFunction,
  TooltipValueFormatFunction
} from './flot-widget.models';
import * as moment_ from 'moment';
import * as tinycolor_ from 'tinycolor2';
import { AggregationType } from '@shared/models/time/time.models';
import { CancelAnimationFrame } from '@core/services/raf.service';
import { UtilsService } from '@core/services/utils.service';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import Timeout = NodeJS.Timeout;

const tinycolor = tinycolor_;
const moment = moment_;

const flotPieSettingsSchemaValue = flotPieSettingsSchema;
const flotPieDatakeySettingsSchemaValue = flotPieDatakeySettingsSchema;

export class TbFlot {

  private readonly utils: UtilsService;

  private settings: TbFlotSettings;

  private readonly tooltip: JQuery<any>;

  private readonly yAxisTickFormatter: TbFlotTicksFormatterFunction;
  private readonly yaxis: TbFlotAxisOptions;
  private readonly xaxis: TbFlotAxisOptions;
  private yaxes: Array<TbFlotAxisOptions>;

  private readonly options: JQueryPlotOptions;
  private subscription: IWidgetSubscription;
  private $element: JQuery<any>;

  private readonly trackUnits: string;
  private readonly trackDecimals: number;
  private readonly tooltipIndividual: boolean;
  private readonly tooltipCumulative: boolean;
  private readonly hideZeros: boolean;

  private readonly defaultBarWidth: number;

  private thresholdsSourcesSubscription: IWidgetSubscription;
  private predefinedThresholds: TbFlotThresholdMarking[];

  private labelPatternsSourcesSubscription: IWidgetSubscription;
  private labelPatternsSourcesData: DatasourceData[];

  private plotInited = false;
  private plot: JQueryPlot;

  private createPlotTimeoutHandle: Timeout;
  private updateTimeoutHandle: Timeout;
  private resizeTimeoutHandle: Timeout;

  private mouseEventsEnabled: boolean;
  private isMouseInteraction = false;
  private flotHoverHandler = this.onFlotHover.bind(this);
  private flotSelectHandler = this.onFlotSelect.bind(this);
  private dblclickHandler = this.onFlotDblClick.bind(this);
  private mousedownHandler = this.onFlotMouseDown.bind(this);
  private mouseupHandler = this.onFlotMouseUp.bind(this);
  private mouseleaveHandler = this.onFlotMouseLeave.bind(this);
  private flotClickHandler = this.onFlotClick.bind(this);

  private readonly animatedPie: boolean;
  private pieDataAnimationDuration: number;
  private pieData: DatasourceData[];
  private pieRenderedData: any[];
  private pieTargetData: any[];
  private pieAnimationStartTime: number;
  private pieAnimationLastTime: number;
  private pieAnimationCaf: CancelAnimationFrame;

  static pieSettingsSchema(): JsonSettingsSchema {
    return flotPieSettingsSchemaValue;
  }

  static pieDatakeySettingsSchema(): JsonSettingsSchema {
    return flotPieDatakeySettingsSchemaValue;
  }

  static settingsSchema(chartType: ChartType): JsonSettingsSchema {
    return flotSettingsSchema(chartType);
  }

  static datakeySettingsSchema(defaultShowLines: boolean, chartType: ChartType): JsonSettingsSchema {
    return flotDatakeySettingsSchema(defaultShowLines, chartType);
  }

  constructor(private ctx: WidgetContext, private readonly chartType: ChartType) {
    this.chartType = this.chartType || 'line';
    this.settings = ctx.settings as TbFlotSettings;
    this.utils = this.ctx.$injector.get(UtilsService);
    this.tooltip = $('#flot-series-tooltip');
    if (this.tooltip.length === 0) {
      this.tooltip = this.createTooltipElement();
    }

    this.trackDecimals = ctx.decimals;
    this.trackUnits = ctx.units;
    this.tooltipIndividual = this.chartType === 'pie' || (isDefined(this.settings.tooltipIndividual)
      ? this.settings.tooltipIndividual : false);
    this.tooltipCumulative = isDefined(this.settings.tooltipCumulative) ? this.settings.tooltipCumulative : false;
    this.hideZeros = isDefined(this.settings.hideZeros) ? this.settings.hideZeros : false;

    const font = {
      color: this.settings.fontColor || '#545454',
      size: this.settings.fontSize || 10,
      family: 'Roboto'
    };

    this.options = {
      title: null,
      subtitile: null,
      shadowSize: isDefined(this.settings.shadowSize) ? this.settings.shadowSize : 4,
      HtmlText: false,
      grid: {
        hoverable: true,
        mouseActiveRadius: 10,
        autoHighlight: this.tooltipIndividual === true,
        markings: []
      },
      selection : { mode : ctx.isMobile ? null : 'x' },
      legend : {
        show: false
      }
    };

    if (this.chartType === 'line' || this.chartType === 'bar' || this.chartType === 'state') {
      this.options.xaxes = [];
      this.xaxis = {
        mode: 'time',
        timezone: 'browser',
        font: deepClone(font),
        labelFont: deepClone(font)
      };
      this.yaxis = {
        font: deepClone(font),
        labelFont: deepClone(font)
      };
      if (this.settings.xaxis) {
        this.xaxis.font.color = this.settings.xaxis.color || this.xaxis.font.color;
        this.xaxis.label = this.utils.customTranslation(this.settings.xaxis.title, this.settings.xaxis.title) || null;
        this.xaxis.labelFont.color = this.xaxis.font.color;
        this.xaxis.labelFont.size = this.xaxis.font.size + 2;
        this.xaxis.labelFont.weight = 'bold';
      }

      this.yAxisTickFormatter = this.formatYAxisTicks.bind(this);

      this.yaxis.tickFormatter = this.yAxisTickFormatter;

      if (this.settings.yaxis) {
        this.yaxis.font.color = this.settings.yaxis.color || this.yaxis.font.color;
        this.yaxis.min = isDefined(this.settings.yaxis.min) ? this.settings.yaxis.min : null;
        this.yaxis.max = isDefined(this.settings.yaxis.max) ? this.settings.yaxis.max : null;
        this.yaxis.label = this.utils.customTranslation(this.settings.yaxis.title, this.settings.yaxis.title) || null;
        this.yaxis.labelFont.color = this.yaxis.font.color;
        this.yaxis.labelFont.size = this.yaxis.font.size + 2;
        this.yaxis.labelFont.weight = 'bold';
        if (isNumber(this.settings.yaxis.tickSize)) {
          this.yaxis.tickSize = this.settings.yaxis.tickSize;
        } else {
          this.yaxis.tickSize = null;
        }
        if (isNumber(this.settings.yaxis.tickDecimals)) {
          this.yaxis.tickDecimals = this.settings.yaxis.tickDecimals;
        } else {
          this.yaxis.tickDecimals = null;
        }
        if (this.settings.yaxis.ticksFormatter && this.settings.yaxis.ticksFormatter.length) {
          try {
            this.yaxis.ticksFormatterFunction = new Function('value',
                                               this.settings.yaxis.ticksFormatter) as TbFlotTicksFormatterFunction;
          } catch (e) {
            this.yaxis.ticksFormatterFunction = null;
          }
        }
      }

      this.options.grid.borderWidth = 1;
      this.options.grid.color = this.settings.fontColor || '#545454';

      if (this.settings.grid) {
        this.options.grid.color = this.settings.grid.color || '#545454';
        this.options.grid.backgroundColor = this.settings.grid.backgroundColor || null;
        this.options.grid.tickColor = this.settings.grid.tickColor || '#DDDDDD';
        this. options.grid.borderWidth = isDefined(this.settings.grid.outlineWidth) ?
          this.settings.grid.outlineWidth : 1;
        if (this.settings.grid.verticalLines === false) {
          this.xaxis.tickLength = 0;
        }
        if (this.settings.grid.horizontalLines === false) {
          this.yaxis.tickLength = 0;
        }
        if (isDefined(this.settings.grid.margin)) {
          this.options.grid.margin = this.settings.grid.margin;
        }
        if (isDefined(this.settings.grid.minBorderMargin)) {
          this.options.grid.minBorderMargin = this.settings.grid.minBorderMargin;
        }
      }

      this.options.xaxes[0] = deepClone(this.xaxis);
      if (this.settings.xaxis && this.settings.xaxis.showLabels === false) {
        this.options.xaxes[0].tickFormatter = () => {
          return '';
        };
      }

      if (this.settings.comparisonEnabled) {
        const xaxis = deepClone(this.xaxis);
        xaxis.position = 'top';
        if (this.settings.xaxisSecond) {
          if (this.settings.xaxisSecond.showLabels === false) {
            xaxis.tickFormatter = () => {
              return '';
            };
          }
          xaxis.label = this.utils.customTranslation(this.settings.xaxisSecond.title, this.settings.xaxisSecond.title) || null;
          xaxis.position = this.settings.xaxisSecond.axisPosition;
        }
        xaxis.tickLength = 0;
        this.options.xaxes.push(xaxis);

        this.options.series = {
          stack: false
        };
      } else {
        this.options.series = {
          stack: this.settings.stack === true
        };
      }

      this.options.crosshair = {
        mode: 'x'
      };

      if (this.chartType === 'line' && this.settings.smoothLines) {
        this.options.series.curvedLines = {
          active: true,
          monotonicFit: true
        };
      }

      if (this.chartType === 'line' && isFinite(this.settings.thresholdsLineWidth)) {
        this.options.grid.markingsLineWidth = this.settings.thresholdsLineWidth;
      }

      if (this.chartType === 'bar') {
        this.options.series.lines = {
          show: false,
          fill: false,
          steps: false
        };
        this.options.series.bars = {
          show: true,
          lineWidth: 0,
          fill: 0.9,
          align: this.settings.barAlignment || 'left'
        };
        this.defaultBarWidth = this.settings.defaultBarWidth || 600;
      }

      if (this.chartType === 'state') {
        this.options.series.lines = {
          steps: true,
          show: true
        };
      }
    } else if (this.chartType === 'pie') {
      this.options.series = {
        pie: {
          show: true,
          label: {
            show: this.settings.showLabels === true
          },
          radius: this.settings.radius || 1,
          innerRadius: this.settings.innerRadius || 0,
          stroke: {
            color: '#fff',
            width: 0
          },
          tilt: this.settings.tilt || 1,
          shadow: {
            left: 5,
            top: 15,
            alpha: 0.02
          }
        }
      };

      this.options.grid.clickable = true;

      if (this.settings.stroke) {
        this.options.series.pie.stroke.color = this.settings.stroke.color || '#fff';
        this.options.series.pie.stroke.width = this.settings.stroke.width || 0;
      }

      if (this.options.series.pie.label.show) {
        this.options.series.pie.label.formatter = (label, series) => {
          return `<div class='pie-label'>${series.dataKey.label}<br/>${Math.round(series.percent)}%</div>`;
        };
        this.options.series.pie.label.radius = 3 / 4;
        this.options.series.pie.label.background = {
          opacity: 0.8
        };
      }

      // Experimental
      this.animatedPie = this.settings.animatedPie === true;

    }

    if (this.ctx.defaultSubscription) {
      this.init(this.ctx.$container, this.ctx.defaultSubscription);
    }
  }


  private init($element: JQuery<any>, subscription: IWidgetSubscription) {
    this.subscription = subscription;
    this.$element = $element;
    const colors: string[] = [];
    this.yaxes = [];
    const yaxesMap: {[units: string]: TbFlotAxisOptions} = {};
    const predefinedThresholds: TbFlotThresholdMarking[] = [];
    const thresholdsDatasources: Datasource[] = [];
    if (this.settings.customLegendEnabled && this.settings.dataKeysListForLabels?.length) {
      this.labelPatternsSourcesData = [];
      const labelPatternsDatasources: Datasource[] = [];
      this.settings.dataKeysListForLabels.forEach((item) => {
        item.settings = {};
      });
      subscription.datasources.forEach((item) => {
        let datasource: Datasource = {
          type: item.type,
          entityType: item.entityType,
          entityId: item.entityId,
          dataKeys: this.settings.dataKeysListForLabels
        };
        labelPatternsDatasources.push(datasource);
      });
      this.subscribeForLabelPatternsSources(labelPatternsDatasources);
    }

    let tooltipValueFormatFunction: TooltipValueFormatFunction = null;
    if (this.settings.tooltipValueFormatter && this.settings.tooltipValueFormatter.length) {
      try {
        tooltipValueFormatFunction = new Function('value', this.settings.tooltipValueFormatter) as TooltipValueFormatFunction;
      } catch (e) {
        tooltipValueFormatFunction = null;
      }
    }

    for (let i = 0; i < this.subscription.data.length; i++) {
      const series = this.subscription.data[i] as TbFlotSeries;
      colors.push(series.dataKey.color);
      const keySettings = series.dataKey.settings;
      series.dataKey.tooltipValueFormatFunction = tooltipValueFormatFunction;
      if (keySettings.tooltipValueFormatter && keySettings.tooltipValueFormatter.length) {
        try {
          series.dataKey.tooltipValueFormatFunction = new Function('value',
            keySettings.tooltipValueFormatter) as TooltipValueFormatFunction;
        } catch (e) {
          series.dataKey.tooltipValueFormatFunction = tooltipValueFormatFunction;
        }
      }
      series.lines = {
        fill: keySettings.fillLines === true
      };

      if (this.settings.stack && !this.settings.comparisonEnabled) {
        series.stack = !keySettings.excludeFromStacking;
      } else {
        series.stack = false;
      }

      if (this.chartType === 'line' || this.chartType === 'state') {
        series.lines.show = keySettings.showLines !== false;
      } else {
        series.lines.show = keySettings.showLines === true;
      }
      if (isDefined(keySettings.lineWidth) && keySettings.lineWidth !== null) {
        series.lines.lineWidth = keySettings.lineWidth;
      }
      series.points = {
        show: false,
        radius: 8
      };
      if (keySettings.showPoints === true) {
        series.points.show = true;
        series.points.lineWidth = isDefined(keySettings.showPointsLineWidth) ? keySettings.showPointsLineWidth : 5;
        series.points.radius = isDefined(keySettings.showPointsRadius) ? keySettings.showPointsRadius : 3;
        series.points.symbol = isDefined(keySettings.showPointShape) ? keySettings.showPointShape : 'circle';
        if (series.points.symbol === 'custom' && keySettings.pointShapeFormatter) {
          try {
            series.points.symbol = new Function('ctx, x, y, radius, shadow', keySettings.pointShapeFormatter);
          } catch (e) {
            series.points.symbol = 'circle';
          }
        }
      }
      if (this.chartType === 'line' && this.settings.smoothLines && !series.points.show) {
        series.curvedLines = {
          apply: true
        };
      }

      const lineColor = tinycolor(series.dataKey.color);
      lineColor.setAlpha(.75);

      series.highlightColor = lineColor.toRgbString();

      if (series.datasource.isAdditional) {
        series.xaxisIndex = 1;
        series.xaxis = 2;
      } else {
        series.xaxisIndex = 0;
        series.xaxis = 1;
      }

      if (this.yaxis) {
        const units = series.dataKey.units && series.dataKey.units.length ? series.dataKey.units : this.trackUnits;
        let yaxis: TbFlotAxisOptions;
        if (keySettings.showSeparateAxis) {
          yaxis = this.createYAxis(keySettings, units);
          this.yaxes.push(yaxis);
        } else {
          yaxis = yaxesMap[units];
          if (!yaxis) {
            yaxis = this.createYAxis(keySettings, units);
            yaxesMap[units] = yaxis;
            this.yaxes.push(yaxis);
          }
        }
        series.yaxisIndex = this.yaxes.indexOf(yaxis);
        series.yaxis = series.yaxisIndex + 1;
        yaxis.keysInfo[i] = {hidden: false};
        yaxis.show = true;

        if (keySettings.thresholds && keySettings.thresholds.length) {
          for (const threshold of keySettings.thresholds) {
            if (threshold.thresholdValueSource === 'predefinedValue' && isFinite(threshold.thresholdValue)) {
              const colorIndex = this.subscription.data.length + predefinedThresholds.length;
              this.generateThreshold(predefinedThresholds, series.yaxis, threshold.lineWidth,
                threshold.color, colorIndex, threshold.thresholdValue);
            } else if (threshold.thresholdEntityAlias && threshold.thresholdAttribute) {
              const entityAliasId = this.ctx.aliasController.getEntityAliasId(threshold.thresholdEntityAlias);
              if (!entityAliasId) {
                continue;
              }
              let datasource = thresholdsDatasources.filter((thresholdDatasource) => {
                return thresholdDatasource.entityAliasId === entityAliasId;
              })[0];
              const dataKey: DataKey = {
                type: DataKeyType.attribute,
                name: threshold.thresholdAttribute,
                label: threshold.thresholdAttribute,
                settings: {
                  yaxis: series.yaxis,
                  lineWidth: threshold.lineWidth,
                  color: threshold.color
                } as TbFlotThresholdKeySettings,
                _hash: Math.random()
              };
              if (datasource) {
                datasource.dataKeys.push(dataKey);
              } else {
                datasource = {
                  type: DatasourceType.entity,
                  name: threshold.thresholdEntityAlias,
                  aliasName: threshold.thresholdEntityAlias,
                  entityAliasId,
                  dataKeys: [ dataKey ]
                };
                thresholdsDatasources.push(datasource);
              }
            }
          }
        }
      }
      if (this.labelPatternsSourcesData?.length) {
        this.substituteLabelPatterns(series, i);
      }
    }

    this.subscribeForThresholdsAttributes(thresholdsDatasources);
    this.options.grid.markings = predefinedThresholds;
    this.predefinedThresholds = predefinedThresholds;

    this.options.colors = colors;
    this.options.yaxes = deepClone(this.yaxes);
    if (this.chartType === 'line' || this.chartType === 'bar' || this.chartType === 'state') {
      if (this.chartType === 'bar') {
        if (this.subscription.timeWindowConfig.aggregation &&
          this.subscription.timeWindowConfig.aggregation.type === AggregationType.NONE) {
          this.options.series.bars.barWidth = this.defaultBarWidth;
        } else {
          this.options.series.bars.barWidth = this.subscription.timeWindow.interval * 0.6;
        }
      }
      this.options.xaxes[0].min = this.subscription.timeWindow.minTime;
      this.options.xaxes[0].max = this.subscription.timeWindow.maxTime;
      if (this.settings.comparisonEnabled) {
        this.options.xaxes[1].min = this.subscription.comparisonTimeWindow.minTime;
        this.options.xaxes[1].max = this.subscription.comparisonTimeWindow.maxTime;
      }
    }

    this.checkMouseEvents();

    if (this.plot) {
      this.plot.destroy();
    }
    if (this.chartType === 'pie' && this.animatedPie) {
      this.pieDataAnimationDuration = 250;
      this.pieData = deepClone(this.subscription.data);
      this.pieRenderedData = [];
      this.pieTargetData = [];
      for (let i = 0; i < this.subscription.data.length; i++) {
        this.pieTargetData[i] = (this.subscription.data[i].data && this.subscription.data[i].data[0])
          ? this.subscription.data[i].data[0][1] : 0;
      }
      this.pieDataRendered();
    }
    this.plotInited = true;
    this.createPlot();
  }

  public update() {
    if (this.updateTimeoutHandle) {
      clearTimeout(this.updateTimeoutHandle);
      this.updateTimeoutHandle = null;
    }
    if (this.subscription) {
      if (!this.isMouseInteraction && this.plot) {
        if (this.chartType === 'line' || this.chartType === 'bar' || this.chartType === 'state') {

          let axisVisibilityChanged = false;
          if (this.yaxis) {
            for (let i = 0; i < this.subscription.data.length; i++) {
              const series = this.subscription.data[i] as TbFlotSeries;
              const yaxisIndex = series.yaxisIndex;
              if (this.yaxes[yaxisIndex].keysInfo[i].hidden !== series.dataKey.hidden) {
                this.yaxes[yaxisIndex].keysInfo[i].hidden = series.dataKey.hidden;
                axisVisibilityChanged = true;
              }

              if (this.labelPatternsSourcesData?.length) {
                this.substituteLabelPatterns(series, i);
              }
            }
            if (axisVisibilityChanged) {
              this.options.yaxes.length = 0;
              this.yaxes.forEach((yaxis) => {
                let hidden = true;
                yaxis.keysInfo.forEach((info) => {
                  if (info) {
                    hidden = hidden && info.hidden;
                  }
                });
                yaxis.hidden = hidden;
                let newIndex = 1;
                if (!yaxis.hidden) {
                  this.options.yaxes.push(yaxis);
                  newIndex = this.options.yaxes.length;
                }
                for (let k = 0; k < yaxis.keysInfo.length; k++) {
                  if (yaxis.keysInfo[k]) {
                    (this.subscription.data[k] as TbFlotSeries).yaxis = newIndex;
                  }
                }

              });
              this.options.yaxis = {
                show: this.options.yaxes.length ? true : false
              };
            }
          }

          this.options.xaxes[0].min = this.subscription.timeWindow.minTime;
          this.options.xaxes[0].max = this.subscription.timeWindow.maxTime;
          if (this.settings.comparisonEnabled) {
            this.options.xaxes[1].min = this.subscription.comparisonTimeWindow.minTime;
            this.options.xaxes[1].max = this.subscription.comparisonTimeWindow.maxTime;
          }
          if (this.chartType === 'bar') {
            if (this.subscription.timeWindowConfig.aggregation &&
              this.subscription.timeWindowConfig.aggregation.type === AggregationType.NONE) {
              this.options.series.bars.barWidth = this.defaultBarWidth;
            } else {
              this.options.series.bars.barWidth = this.subscription.timeWindow.interval * 0.6;
            }
          }

          if (axisVisibilityChanged) {
            this.redrawPlot();
          } else {
            this.plot.getOptions().xaxes[0].min = this.subscription.timeWindow.minTime;
            this.plot.getOptions().xaxes[0].max = this.subscription.timeWindow.maxTime;
            if (this.settings.comparisonEnabled) {
              this.plot.getOptions().xaxes[1].min = this.subscription.comparisonTimeWindow.minTime;
              this.plot.getOptions().xaxes[1].max = this.subscription.comparisonTimeWindow.maxTime;
            }
            if (this.chartType === 'bar') {
              if (this.subscription.timeWindowConfig.aggregation &&
                this.subscription.timeWindowConfig.aggregation.type === AggregationType.NONE) {
                this.plot.getOptions().series.bars.barWidth = this.defaultBarWidth;
              } else {
                this.plot.getOptions().series.bars.barWidth = this.subscription.timeWindow.interval * 0.6;
              }
            }
            this.updateData();
          }
        } else if (this.chartType === 'pie') {
          if (this.animatedPie) {
            this.nextPieDataAnimation(true);
          } else {
            this.updateData();
          }
        }
      } else if (this.isMouseInteraction && this.plot) {
        this.updateTimeoutHandle = setTimeout(this.update.bind(this), 30);
      }
    }
  }

  public resize() {
    if (this.resizeTimeoutHandle) {
      clearTimeout(this.resizeTimeoutHandle);
      this.resizeTimeoutHandle = null;
    }
    if (this.plot && this.plotInited) {
      const width = this.$element.width();
      const height = this.$element.height();
      if (width && height) {
        this.plot.resize();
        if (this.chartType !== 'pie') {
          this.plot.setupGrid();
        }
        this.plot.draw();
      } else {
        this.resizeTimeoutHandle = setTimeout(this.resize.bind(this), 30);
      }
    }
  }

  public checkMouseEvents() {
    const enabled = !this.ctx.isMobile &&  !this.ctx.isEdit;
    if (isUndefined(this.mouseEventsEnabled) || this.mouseEventsEnabled !== enabled) {
      this.mouseEventsEnabled = enabled;
      if (this.$element) {
        if (enabled) {
          this.enableMouseEvents();
        } else {
          this.disableMouseEvents();
        }
        this.redrawPlot();
      }
    }
  }

  public destroy() {
    this.cleanup();
    if (this.plot) {
      this.plot.destroy();
      this.plot = null;
      this.plotInited = false;
    }
  }

  private cleanup() {
    if (this.updateTimeoutHandle) {
      clearTimeout(this.updateTimeoutHandle);
      this.updateTimeoutHandle = null;
    }
    if (this.createPlotTimeoutHandle) {
      clearTimeout(this.createPlotTimeoutHandle);
      this.createPlotTimeoutHandle = null;
    }
    if (this.resizeTimeoutHandle) {
      clearTimeout(this.resizeTimeoutHandle);
      this.resizeTimeoutHandle = null;
    }
  }

  private createPlot() {
    if (this.createPlotTimeoutHandle) {
      clearTimeout(this.createPlotTimeoutHandle);
      this.createPlotTimeoutHandle = null;
    }
    if (this.plotInited && !this.plot) {
      const width = this.$element.width();
      const height = this.$element.height();
      if (width && height) {
        if (this.chartType === 'pie' && this.animatedPie) {
          this.plot = $.plot(this.$element, this.pieData, this.options) as JQueryPlot;
        } else {
          this.plot = $.plot(this.$element, this.subscription.data, this.options) as JQueryPlot;
        }
      } else {
        this.createPlotTimeoutHandle = setTimeout(this.createPlot.bind(this), 30);
      }
    }
  }

  private updateData() {
    this.plot.setData(this.subscription.data);
    if (this.chartType !== 'pie') {
      this.plot.setupGrid();
    }
    this.plot.draw();
  }

  private redrawPlot() {
    if (this.plot && this.plotInited) {
      this.plot.destroy();
      this.plot = null;
      this.createPlot();
    }
  }

  private createYAxis(keySettings: TbFlotKeySettings, units: string): TbFlotAxisOptions {
    const yaxis = deepClone(this.yaxis);
    let tickDecimals: number;
    let tickSize: number;
    const label = keySettings.axisTitle && keySettings.axisTitle.length ? keySettings.axisTitle : yaxis.label;
    if (isNumber(keySettings.axisTickDecimals)) {
      tickDecimals = keySettings.axisTickDecimals;
    } else {
      tickDecimals = yaxis.tickDecimals;
    }
    if (isNumber(keySettings.axisTickSize)) {
      tickSize = keySettings.axisTickSize;
    } else {
      tickSize = yaxis.tickSize;
    }
    const position = keySettings.axisPosition && keySettings.axisPosition.length ? keySettings.axisPosition : 'left';
    const min = isDefined(keySettings.axisMin) ? keySettings.axisMin : yaxis.min;
    const max = isDefined(keySettings.axisMax) ? keySettings.axisMax : yaxis.max;
    yaxis.label = label;
    yaxis.min = min;
    yaxis.max = max;
    yaxis.tickUnits = units;
    yaxis.tickDecimals = tickDecimals;
    yaxis.tickSize = tickSize;
    if (position === 'right' && tickSize === null) {
      yaxis.alignTicksWithAxis = 1;
    } else {
      yaxis.alignTicksWithAxis = null;
    }
    yaxis.position = position;

    yaxis.keysInfo = [];

    if (keySettings.axisTicksFormatter && keySettings.axisTicksFormatter.length) {
      try {
        yaxis.ticksFormatterFunction = new Function('value', keySettings.axisTicksFormatter) as TbFlotTicksFormatterFunction;
      } catch (e) {
        yaxis.ticksFormatterFunction = this.yaxis.ticksFormatterFunction;
      }
    }
    return yaxis;
  }

  private subscribeForThresholdsAttributes(datasources: Datasource[]) {
    const thresholdsSourcesSubscriptionOptions: WidgetSubscriptionOptions = {
      datasources,
      useDashboardTimewindow: false,
      type: widgetType.latest,
      callbacks: {
        onDataUpdated: (subscription) => {this.thresholdsSourcesDataUpdated(subscription.data)}
      }
    };
    this.ctx.subscriptionApi.createSubscription(thresholdsSourcesSubscriptionOptions, true).subscribe(
      (subscription) => {
        this.thresholdsSourcesSubscription = subscription;
      }
    );
  }

  private thresholdsSourcesDataUpdated(data: DatasourceData[]) {
    const allThresholds = deepClone(this.predefinedThresholds);
    data.forEach((keyData) => {
      if (keyData && keyData.data && keyData.data[0]) {
        const attrValue = keyData.data[0][1];
        if (isFinite(attrValue)) {
          const settings: TbFlotThresholdKeySettings = keyData.dataKey.settings;
          const colorIndex = this.subscription.data.length + allThresholds.length;
          this.generateThreshold(allThresholds, settings.yaxis, settings.lineWidth, settings.color, colorIndex, attrValue);
        }
      }
    });
    this.options.grid.markings = allThresholds;
    this.redrawPlot();
  }

  private generateThreshold(existingThresholds: TbFlotThresholdMarking[], yaxis: number, lineWidth: number,
                            color: string, defaultColorIndex: number, thresholdValue: number) {
    const marking: TbFlotThresholdMarking = {};
    let markingYAxis;

    if (yaxis !== 1) {
      markingYAxis = 'y' + yaxis + 'axis';
    } else {
      markingYAxis = 'yaxis';
    }

    if (isFinite(lineWidth)) {
      marking.lineWidth = lineWidth;
    }

    if (isDefined(color)) {
      marking.color = color;
    } else {
      marking.color = this.utils.getMaterialColor(defaultColorIndex);
    }

    marking[markingYAxis] = {
      from: thresholdValue,
      to: thresholdValue
    };

    const similarMarkings = existingThresholds.filter((existingMarking) => {
      return isEqual(existingMarking[markingYAxis], marking[markingYAxis]);
    });
    if (!similarMarkings.length) {
      existingThresholds.push(marking);
    }
  }

  private subscribeForLabelPatternsSources(datasources: Datasource[]) {
    const labelPatternsSourcesSubscriptionOptions: WidgetSubscriptionOptions = {
      datasources,
      useDashboardTimewindow: false,
      type: widgetType.latest,
      callbacks: {
        onDataUpdated: (subscription) => {
          this.labelPatternsParamsDataUpdated(subscription.data)
        }
      }
    };
    this.ctx.subscriptionApi.createSubscription(labelPatternsSourcesSubscriptionOptions, true).subscribe(
      (subscription) => {
        this.labelPatternsSourcesSubscription = subscription;
      }
    );
  }

  private labelPatternsParamsDataUpdated(data: DatasourceData[]) {
    this.labelPatternsSourcesData = data;
    for (let i = 0; i < this.subscription.data.length; i++) {
      const series = this.subscription.data[i] as TbFlotSeries;
      this.substituteLabelPatterns(series, i);
    }
    this.updateData();
    this.ctx.detectChanges();
  }

  private substituteLabelPatterns(series: TbFlotSeries, seriesIndex: number) {
    let seriesLabelPatternsSourcesData = this.labelPatternsSourcesData.filter((item) => {
      return item.datasource.entityId === series.datasource.entityId;
    });
    let label = createLabelFromDatasource(series.datasource, series.dataKey.pattern);
    for (let i = 0; i < seriesLabelPatternsSourcesData.length; i++) {
      let keyData = seriesLabelPatternsSourcesData[i];
      if (keyData && keyData.data && keyData.data[0]) {
        let attrValue = keyData.data[0][1];
        let attrName = keyData.dataKey.name;
        if (isDefined(attrValue) && (attrValue !== null)) {
          label = insertVariable(label, attrName, attrValue);
        }
      }
    }
    if (isDefined(this.subscription.legendData)) {
      let targetLegendKeyIndex = this.subscription.legendData.keys.findIndex((key) => {
        return key.dataIndex === seriesIndex;
      });
      if (targetLegendKeyIndex !== -1) {
        this.subscription.legendData.keys[targetLegendKeyIndex].dataKey.label = label;
      }
    }
    series.dataKey.label = label;
  }

  private seriesInfoDiv(label: string, color: string, value: any,
                        units: string, trackDecimals: number, active: boolean,
                        percent: number, valueFormatFunction: TooltipValueFormatFunction): JQuery<HTMLElement> {
    const divElement = $('<div></div>');
    divElement.css({
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between'
    });
    const lineSpan = $('<span></span>');
    lineSpan.css({
      backgroundColor: color,
      width: '20px',
      height: '3px',
      display: 'inline-block',
      verticalAlign: 'middle',
      marginRight: '5px'
    });
    divElement.append(lineSpan);
    const labelSpan = $(`<span>${label}:</span>`);
    labelSpan.css({
      marginRight: '10px'
    });
    if (active) {
      labelSpan.css({
        color: '#FFF',
        fontWeight: '700'
      });
    }
    divElement.append(labelSpan);
    let valueContent: string;
    if (valueFormatFunction) {
      valueContent = valueFormatFunction(value);
    } else {
      valueContent = this.ctx.utils.formatValue(value, trackDecimals, units);
    }
    if (isNumber(percent)) {
      valueContent += ' (' + Math.round(percent) + ' %)';
    }
    const valueSpan =  $(`<span>${valueContent}</span>`);
    valueSpan.css({
      marginLeft: 'auto',
      fontWeight: '700'
    });
    if (active) {
      valueSpan.css({
        color: '#FFF'
      });
    }
    divElement.append(valueSpan);
    return divElement;
  }

  private seriesInfoDivFromInfo(seriesHoverInfo: TbFlotSeriesHoverInfo, seriesIndex: number): string {
    const units = seriesHoverInfo.units && seriesHoverInfo.units.length ? seriesHoverInfo.units : this.trackUnits;
    const decimals = isDefinedAndNotNull(seriesHoverInfo.decimals) ? seriesHoverInfo.decimals : this.trackDecimals;
    const divElement = this.seriesInfoDiv(seriesHoverInfo.label, seriesHoverInfo.color,
      seriesHoverInfo.value, units, decimals, seriesHoverInfo.index === seriesIndex, null, seriesHoverInfo.tooltipValueFormatFunction);
    return divElement.prop('outerHTML');
  }

  private createTooltipElement(): JQuery<any> {
    const tooltip = $('<div id="flot-series-tooltip" class="flot-mouse-value"></div>');
    tooltip.css({
      fontSize: '12px',
      fontFamily: 'Roboto',
      fontWeight: '300',
      lineHeight: '18px',
      opacity: '1',
      backgroundColor: 'rgba(0,0,0,0.7)',
      color: '#D9DADB',
      position: 'absolute',
      display: 'none',
      zIndex: '1100',
      padding: '4px 10px',
      borderRadius: '4px'
    }).appendTo('body');
    return tooltip;
  }

  private formatPieTooltip(item: TbFlotPlotItem): string {
    const units = item.series.dataKey.units && item.series.dataKey.units.length ? item.series.dataKey.units : this.trackUnits;
    const decimals = isDefinedAndNotNull(item.series.dataKey.decimals) ? item.series.dataKey.decimals : this.trackDecimals;
    const divElement = this.seriesInfoDiv(item.series.dataKey.label, item.series.dataKey.color,
      item.datapoint[1][0][1], units, decimals, true, item.series.percent, item.series.dataKey.tooltipValueFormatFunction);
    return divElement.prop('outerHTML');
  }

  private formatChartTooltip(hoverInfo: TbFlotHoverInfo[], seriesIndex: number): string {
    let content = '';
    if (this.tooltipIndividual) {
      let seriesHoverArray: TbFlotSeriesHoverInfo[];
      if (hoverInfo[1] && hoverInfo[1].seriesHover.length) {
        seriesHoverArray = hoverInfo[0].seriesHover.concat(hoverInfo[1].seriesHover);
      } else {
        seriesHoverArray = hoverInfo[0].seriesHover;
      }
      const found = seriesHoverArray.filter((seriesHover) => {
        return seriesHover.index === seriesIndex;
      });
      if (found && found.length) {
        const timestamp = parseInt(found[0].time, 10);
        const date = moment(timestamp).format('YYYY-MM-DD HH:mm:ss');
        const dateDiv = $('<div>' + date + '</div>');
        dateDiv.css({
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '4px',
          fontWeight: '700'
        });
        content += dateDiv.prop('outerHTML');
        content += this.seriesInfoDivFromInfo(found[0], seriesIndex);
      }
    } else {
      let maxRows: number;
      if (hoverInfo[1] && hoverInfo[1].seriesHover.length) {
        maxRows = 5;
      } else {
        maxRows = 15;
      }
      let columns = 0;
      if (hoverInfo[1] && (hoverInfo[1].seriesHover.length > hoverInfo[0].seriesHover.length)) {
        columns = Math.ceil(hoverInfo[1].seriesHover.length / maxRows);
      } else {
        columns = Math.ceil(hoverInfo[0].seriesHover.length / maxRows);
      }
      hoverInfo.forEach((hoverData) => {
        if (isNumber(hoverData.time)) {
          let columnsContent = '';
          const timestamp = parseInt(hoverData.time, 10);
          const date = moment(timestamp).format('YYYY-MM-DD HH:mm:ss');
          const dateDiv = $('<div>' + date + '</div>');
          dateDiv.css({
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '4px',
            fontWeight: '700'
          });
          content += dateDiv.prop('outerHTML');

          const seriesDiv = $('<div></div>');
          seriesDiv.css({
            display: 'flex',
            flexDirection: 'row'
          });
          for (let c = 0; c < columns; c++) {
            const columnDiv = $('<div></div>');
            columnDiv.css({
              display: 'flex',
              flexDirection: 'column',
              flex: '1'
            });
            let columnContent = '';
            for (let i = c*maxRows; i < (c+1)*maxRows; i++) {
              if (i >= hoverData.seriesHover.length) {
                break;
              }
              const seriesHoverInfo = hoverData.seriesHover[i];
              columnContent += this.seriesInfoDivFromInfo(seriesHoverInfo, seriesIndex);
            }
            columnDiv.html(columnContent);

            if (columnContent) {
              if (c > 0) {
                columnsContent += '<span style="min-width: 20px;"></span>';
              }
              columnsContent += columnDiv.prop('outerHTML');
            }
          }
          seriesDiv.html(columnsContent);
          content += seriesDiv.prop('outerHTML');
        }
      });
    }
    return content;
  }

  private formatYAxisTicks(value: number, axis?: TbFlotPlotAxis): string {
    if (this.settings.yaxis && this.settings.yaxis.showLabels === false) {
      return '';
    }
    if (axis.options.ticksFormatterFunction) {
      return axis.options.ticksFormatterFunction(value);
    }
    const factor = axis.options.tickDecimals ? Math.pow(10, axis.options.tickDecimals) : 1;
    let formatted = '' + Math.round(value * factor) / factor;
    if (isDefined(axis.options.tickDecimals) && axis.options.tickDecimals !== null) {
      const decimal = formatted.indexOf('.');
      const precision = decimal === -1 ? 0 : formatted.length - decimal - 1;
      if (precision < axis.options.tickDecimals) {
        formatted = (precision ? formatted : formatted + '.') + ('' + factor).substr(1, axis.options.tickDecimals - precision);
      }
    }
    if (axis.options.tickUnits) {
      formatted += ' ' + axis.options.tickUnits;
    }
    return formatted;
  }

  private enableMouseEvents() {
    this.$element.css('pointer-events', '');
    this.$element.addClass('mouse-events');
    this.options.selection = { mode : 'x' };
    this.$element.bind('plothover', this.flotHoverHandler);
    this.$element.bind('plotselected', this.flotSelectHandler);
    this.$element.bind('dblclick', this.dblclickHandler);
    this.$element.bind('mousedown', this.mousedownHandler);
    this.$element.bind('mouseup', this.mouseupHandler);
    this.$element.bind('mouseleave', this.mouseleaveHandler);
    this.$element.bind('plotclick', this.flotClickHandler);
  }

  private disableMouseEvents() {
    this.$element.css('pointer-events', 'none');
    this.$element.removeClass('mouse-events');
    this.options.selection = { mode : null };
    this.$element.unbind('plothover', this.flotHoverHandler);
    this.$element.unbind('plotselected', this.flotSelectHandler);
    this.$element.unbind('dblclick', this.dblclickHandler);
    this.$element.unbind('mousedown', this.mousedownHandler);
    this.$element.unbind('mouseup', this.mouseupHandler);
    this.$element.unbind('mouseleave', this.mouseleaveHandler);
    this.$element.unbind('plotclick', this.flotClickHandler);
  }

  private onFlotHover(e: any, pos: JQueryPlotPoint, item: TbFlotPlotItem) {
    if (!this.plot) {
      return;
    }
    if (!this.tooltipIndividual || item) {
      const multipleModeTooltip = !this.tooltipIndividual;
      if (multipleModeTooltip) {
        this.plot.unhighlight();
      }
      const pageX = pos.pageX;
      const pageY = pos.pageY;

      let tooltipHtml;
      let hoverInfo: TbFlotHoverInfo[];

      if (this.chartType === 'pie') {
        tooltipHtml = this.formatPieTooltip(item);
      } else {
        hoverInfo = this.getHoverInfo(this.plot.getData(), pos);
        if (isNumber(hoverInfo[0].time) || (hoverInfo[1] && isNumber(hoverInfo[1].time))) {
          hoverInfo[0].seriesHover.sort((a, b) => {
            return b.value - a.value;
          });
          if (hoverInfo[1] && hoverInfo[1].seriesHover.length) {
            hoverInfo[1].seriesHover.sort((a, b) => {
              return b.value - a.value;
            });
          }
          tooltipHtml = this.formatChartTooltip(hoverInfo, item ? item.seriesIndex : -1);
        }
      }
      if (tooltipHtml) {
        this.tooltip.html(tooltipHtml)
          .css({top: 0, left: 0})
          .fadeIn(200);

        const windowWidth = $( window ).width();
        const windowHeight = $( window ).height();
        const tooltipWidth = this.tooltip.width();
        const tooltipHeight = this.tooltip.height();
        let left = pageX + 5;
        let top = pageY + 5;
        if (windowWidth - pageX < tooltipWidth + 50) {
          left = pageX - tooltipWidth - 10;
        }
        if (windowHeight - pageY < tooltipHeight + 20) {
          top = pageY - tooltipHeight - 10;
        }
        this.tooltip.css({
          top,
          left
        });
        if (multipleModeTooltip) {
          hoverInfo.forEach((hoverData) => {
            hoverData.seriesHover.forEach((seriesHoverInfo) => {
              this.plot.highlight(seriesHoverInfo.index, seriesHoverInfo.hoverIndex);
            });
          });
        }
      }
    } else {
      this.tooltip.stop(true);
      this.tooltip.hide();
      this.plot.unhighlight();
    }
  }

  private onFlotSelect(e: any, ranges: JQueryPlotSelectionRanges) {
    if (!this.plot) {
      return;
    }
    this.plot.clearSelection();
    this.subscription.onUpdateTimewindow(ranges.xaxis.from, ranges.xaxis.to);
  }

  private onFlotDblClick() {
    this.subscription.onResetTimewindow();
  }

  private onFlotMouseDown() {
    this.isMouseInteraction = true;
  }

  private onFlotMouseUp() {
    this.isMouseInteraction = false;
  }

  private onFlotMouseLeave() {
    if (!this.tooltip) {
      return;
    }
    this.tooltip.stop(true);
    this.tooltip.hide();
    if (this.plot) {
      this.plot.unhighlight();
    }
    this.isMouseInteraction = false;
  }

  private onFlotClick(e: any, pos: JQueryPlotPoint, item: TbFlotPlotItem) {
    if (!this.plot) {
      return;
    }
    this.onPieSliceClick(e, item);
  }

  private getHoverInfo(seriesList: TbFlotPlotDataSeries[], pos: JQueryPlotPoint): TbFlotHoverInfo[] {
    let i: number;
    let series: TbFlotPlotDataSeries;
    let hoverIndex: number;
    let hoverDistance: number;
    let minDistance: number;
    let pointTime: any;
    let minTime: any;
    let minTimeHistorical: any;
    let hoverData: TbFlotSeriesHoverInfo;
    let value: any;
    let lastValue: any;
    let minDistanceHistorical: number;
    const results: TbFlotHoverInfo[] = [{
      seriesHover: []
    }];
    if (this.settings.comparisonEnabled) {
      results.push({
        seriesHover: []
      });
    }
    for (i = 0; i < seriesList.length; i++) {
      series = seriesList[i];
      let posx: number;
      if (series.datasource.isAdditional) {
        posx = pos.x2;
      } else {
        posx = pos.x;
      }
      hoverIndex = this.findHoverIndexFromData(posx, series);
      if (series.data[hoverIndex] && series.data[hoverIndex][0]) {
        hoverDistance = posx - series.data[hoverIndex][0];
        pointTime = series.data[hoverIndex][0];

        if (series.datasource.isAdditional) {
          if (!minDistanceHistorical
            || (hoverDistance >= 0 && (hoverDistance < minDistanceHistorical || minDistanceHistorical < 0))
            || (hoverDistance < 0 && hoverDistance > minDistanceHistorical)) {
            minDistanceHistorical = hoverDistance;
            minTimeHistorical = pointTime;
          }
        } else if (!minDistance
          || (hoverDistance >= 0 && (hoverDistance < minDistance || minDistance < 0))
          || (hoverDistance < 0 && hoverDistance > minDistance)) {
          minDistance = hoverDistance;
          minTime = pointTime;
        }
        if (series.stack) {
          if (this.tooltipIndividual || !this.tooltipCumulative) {
            value = series.data[hoverIndex][1];
          } else {
            lastValue += series.data[hoverIndex][1];
            value = lastValue;
          }
        } else {
          value = series.data[hoverIndex][1];
        }
        if (series.stack || (series.curvedLines && series.curvedLines.apply)) {
          hoverIndex = this.findHoverIndexFromDataPoints(posx, series, hoverIndex);
        }
        if (!this.hideZeros || value) {
          hoverData = {
            value,
            hoverIndex,
            color: series.dataKey.color,
            label: series.dataKey.label,
            units: series.dataKey.units,
            decimals: series.dataKey.decimals,
            tooltipValueFormatFunction: series.dataKey.tooltipValueFormatFunction,
            time: pointTime,
            distance: hoverDistance,
            index: i
          };
          if (series.datasource.isAdditional) {
            results[1].seriesHover.push(hoverData);
          } else {
            results[0].seriesHover.push(hoverData);
          }
        }
      }
    }
    if (results[1] && results[1].seriesHover.length) {
      results[1].time = minTimeHistorical;
    }
    results[0].time = minTime;
    return results;
  }

  private findHoverIndexFromData(posX: number, series: TbFlotPlotDataSeries): number {
    let lower = 0;
    let upper = series.data.length - 1;
    let middle: number;
    const index: number = null;
    while (index === null) {
      if (lower > upper) {
        return Math.max(upper, 0);
      }
      middle = Math.floor((lower + upper) / 2);
      if (series.data[middle][0] === posX) {
        return middle;
      } else if (series.data[middle][0] < posX) {
        lower = middle + 1;
      } else {
        upper = middle - 1;
      }
    }
  }

  private findHoverIndexFromDataPoints(posX: number, series: TbFlotPlotDataSeries, last: number): number {
    const ps = series.datapoints.pointsize;
    const initial = last * ps;
    const len = series.datapoints.points.length;
    let j: number;
    for (j = initial; j < len; j += ps) {
      if ((!series.lines.steps && series.datapoints.points[initial] != null && series.datapoints.points[j] == null)
        || series.datapoints.points[j] > posX) {
        return Math.max(j - ps,  0) / ps;
      }
    }
    return j / ps - 1;
  }

  pieDataRendered() {
    for (let i = 0; i < this.pieTargetData.length; i++) {
      const value = this.pieTargetData[i] ? this.pieTargetData[i] : 0;
      this.pieRenderedData[i] = value;
      if (!this.pieData[i].data[0]) {
        this.pieData[i].data[0] = [0, 0];
      }
      this.pieData[i].data[0][1] = value;
    }
  }

  nextPieDataAnimation(start) {
    if (start) {
      this.finishPieDataAnimation();
      this.pieAnimationStartTime = this.pieAnimationLastTime = Date.now();
      for (let i = 0;  i < this.subscription.data.length; i++) {
        this.pieTargetData[i] = (this.subscription.data[i].data && this.subscription.data[i].data[0])
          ? this.subscription.data[i].data[0][1] : 0;
      }
    }
    if (this.pieAnimationCaf) {
      this.pieAnimationCaf();
      this.pieAnimationCaf = null;
    }
    this.pieAnimationCaf = this.ctx.$scope.raf.raf(this.onPieDataAnimation.bind(this));
  }

  onPieDataAnimation() {
    const time = Date.now();
    const elapsed = time - this.pieAnimationLastTime; // this.pieAnimationStartTime;
    const progress = (time - this.pieAnimationStartTime) / this.pieDataAnimationDuration;
    if (progress >= 1) {
      this.finishPieDataAnimation();
    } else {
      if (elapsed >= 40) {
        for (let i = 0; i < this.pieTargetData.length; i++) {
          const prevValue = this.pieRenderedData[i];
          const targetValue = this.pieTargetData[i];
          const value = prevValue + (targetValue - prevValue) * progress;
          if (!this.pieData[i].data[0]) {
            this.pieData[i].data[0] = [0, 0];
          }
          this.pieData[i].data[0][1] = value;
        }
        this.plot.setData(this.pieData);
        this.plot.draw();
        this.pieAnimationLastTime = time;
      }
      this.nextPieDataAnimation(false);
    }
  }

  private finishPieDataAnimation() {
    this.pieDataRendered();
    this.plot.setData(this.pieData);
    this.plot.draw();
  }

  private onPieSliceClick($event: any, item: TbFlotPlotItem) {
    const descriptors = this.ctx.actionsApi.getActionDescriptors('sliceClick');
    if ($event && descriptors.length) {
      $event.stopPropagation();
      const entityInfo = this.ctx.actionsApi.getActiveEntityInfo();
      const entityId = entityInfo ? entityInfo.entityId : null;
      const entityName = entityInfo ? entityInfo.entityName : null;
      const entityLabel = entityInfo ? entityInfo.entityLabel : null;
      this.ctx.actionsApi.handleWidgetAction($event, descriptors[0], entityId, entityName, item, entityLabel);
    }
  }

}
