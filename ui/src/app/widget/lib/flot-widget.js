/*
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import $ from 'jquery';
import tinycolor from 'tinycolor2';
import moment from 'moment';
import 'flot/lib/jquery.colorhelpers';
import 'flot/src/jquery.flot';
import 'flot/src/plugins/jquery.flot.time';
import 'flot/src/plugins/jquery.flot.selection';
import 'flot/src/plugins/jquery.flot.pie';
import 'flot/src/plugins/jquery.flot.crosshair';
import 'flot/src/plugins/jquery.flot.stack';
import 'flot/src/plugins/jquery.flot.symbol';
import 'flot.curvedlines/curvedLines';

/* eslint-disable angular/angularelement */
export default class TbFlot {
    constructor(ctx, chartType) {

        this.ctx = ctx;
        this.chartType = chartType || 'line';
        var settings = ctx.settings;
        this.utils = this.ctx.$scope.$injector.get('utils');
        this.types = this.ctx.$scope.$injector.get('types');

        ctx.tooltip = $('#flot-series-tooltip');
        if (ctx.tooltip.length === 0) {
            ctx.tooltip = $("<div id='flot-series-tooltip' class='flot-mouse-value'></div>");
            ctx.tooltip.css({
                fontSize: "12px",
                fontFamily: "Roboto",
                fontWeight: "300",
                lineHeight: "18px",
                opacity: "1",
                backgroundColor: "rgba(0,0,0,0.7)",
                color: "#D9DADB",
                position: "absolute",
                display: "none",
                zIndex: "100",
                padding: "4px 10px",
                borderRadius: "4px"
            }).appendTo("body");
        }

        var tbFlot = this;

        function seriesInfoDiv(label, color, value, units, trackDecimals, active, percent, valueFormatFunction, timestamp, index) {
            var divElement = $('<div></div>');
            divElement.css({
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between"
            });
            var lineSpan = $('<span></span>');
            lineSpan.css({
                backgroundColor: color,
                width: "20px",
                height: "3px",
                display: "inline-block",
                verticalAlign: "middle",
                marginRight: "5px"
            });
            divElement.append(lineSpan);
            var labelSpan = $('<span>' + label + ':</span>');
            labelSpan.css({
                marginRight: "10px"
            });
            if (active) {
                labelSpan.css({
                    color: "#FFF",
                    fontWeight: "700"
                });
            }
            divElement.append(labelSpan);
            var valueContent;
            if (valueFormatFunction) {
                valueContent = valueFormatFunction(value, tbFlot.ctx, index, timestamp);
            } else {
                valueContent = tbFlot.ctx.utils.formatValue(value, trackDecimals, units);
            }
            if (angular.isNumber(percent)) {
                valueContent += ' (' + Math.round(percent) + ' %)';
            }
            var valueSpan =  $('<span>' + valueContent + '</span>');
            valueSpan.css({
                marginLeft: "auto",
                fontWeight: "700"
            });
            if (active) {
                valueSpan.css({
                    color: "#FFF"
                });
            }
            divElement.append(valueSpan);

            return divElement;
        }

        function seriesInfoDivFromInfo(seriesHoverInfo, seriesIndex) {
            var units = seriesHoverInfo.units && seriesHoverInfo.units.length ? seriesHoverInfo.units : tbFlot.ctx.trackUnits;
            var decimals = angular.isDefined(seriesHoverInfo.decimals) ? seriesHoverInfo.decimals : tbFlot.ctx.trackDecimals;
            var divElement = seriesInfoDiv(seriesHoverInfo.label, seriesHoverInfo.color,
                seriesHoverInfo.value, units, decimals, seriesHoverInfo.index === seriesIndex, null, seriesHoverInfo.tooltipValueFormatFunction,
                seriesHoverInfo.time, seriesHoverInfo.hoverIndex);
            return divElement.prop('outerHTML');
        }

        if (this.chartType === 'pie') {
            ctx.tooltipFormatter = function(item) {
                var units = item.series.dataKey.units && item.series.dataKey.units.length ? item.series.dataKey.units : tbFlot.ctx.trackUnits;
                var decimals = angular.isDefined(item.series.dataKey.decimals) ? item.series.dataKey.decimals : tbFlot.ctx.trackDecimals;
                var divElement = seriesInfoDiv(item.series.dataKey.label, item.series.dataKey.color,
                    item.datapoint[1][0][1], units, decimals, true, item.series.percent, item.series.dataKey.tooltipValueFormatFunction,
                    null, null);
                return divElement.prop('outerHTML');
            };
        } else {
            ctx.tooltipFormatter = function(hoverInfo, seriesIndex) {
                var content = '';

                if (tbFlot.ctx.tooltipIndividual) {
                    var seriesHoverArray;
                    if (hoverInfo[1] && hoverInfo[1].seriesHover.length) {
                        seriesHoverArray = hoverInfo[0].seriesHover.concat(hoverInfo[1].seriesHover);
                    } else {
                        seriesHoverArray = hoverInfo[0].seriesHover;
                    }
                    var found = seriesHoverArray.filter((seriesHover) => {
                        return seriesHover.index === seriesIndex;
                    });
                    if (found && found.length) {
                        let timestamp = parseInt(found[0].time);
                        let date = moment(timestamp).format('YYYY-MM-DD HH:mm:ss');
                        let dateDiv = $('<div>' + date + '</div>');
                        dateDiv.css({
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            padding: "4px",
                            fontWeight: "700"
                        });
                        content += dateDiv.prop('outerHTML');
                        content += seriesInfoDivFromInfo(found[0], seriesIndex);
                    }
                } else {
                    var maxRows;
                    if (hoverInfo[1] && hoverInfo[1].seriesHover.length) {
                        maxRows = 5;
                    } else {
                        maxRows = 15;
                    }
                    var columns = 0;
                    if (hoverInfo[1] && (hoverInfo[1].seriesHover.length > hoverInfo[0].seriesHover.length)) {
                        columns = Math.ceil(hoverInfo[1].seriesHover.length / maxRows);
                    } else {
                        columns = Math.ceil(hoverInfo[0].seriesHover.length / maxRows);
                    }

                    for (var j = 0; j < hoverInfo.length; j++) {
                        var hoverData = hoverInfo[j];
                        if (angular.isNumber(hoverData.time)) {
                            var columnsContent = '';
                            let timestamp = parseInt(hoverData.time);
                            let date = moment(timestamp).format('YYYY-MM-DD HH:mm:ss');
                            let dateDiv = $('<div>' + date + '</div>');
                            dateDiv.css({
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                padding: "4px",
                                fontWeight: "700"
                            });
                            content += dateDiv.prop('outerHTML');

                            var seriesDiv = $('<div></div>');
                            seriesDiv.css({
                                display: "flex",
                                flexDirection: "row"
                            });
                            for (var c = 0; c < columns; c++) {
                                var columnDiv = $('<div></div>');
                                columnDiv.css({
                                    display: "flex",
                                    flexDirection: "column",
                                    flex: "1"
                                });
                                var columnContent = '';
                                for (var i = c*maxRows; i < (c+1)*maxRows; i++) {
                                    if (i >= hoverData.seriesHover.length) {
                                        break;
                                    }
                                    var seriesHoverInfo = hoverData.seriesHover[i];
                                    columnContent += seriesInfoDivFromInfo(seriesHoverInfo, seriesIndex);
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
                    }
                }
                return content;
            };
        }

        ctx.trackDecimals = ctx.decimals;

        ctx.trackUnits = ctx.units;

        ctx.tooltipIndividual = this.chartType === 'pie' || (angular.isDefined(settings.tooltipIndividual) ? settings.tooltipIndividual : false);
        ctx.tooltipCumulative = angular.isDefined(settings.tooltipCumulative) ? settings.tooltipCumulative : false;
        ctx.hideZeros = angular.isDefined(settings.hideZeros) ? settings.hideZeros : false;

        var font = {
            color: settings.fontColor || "#545454",
            size: settings.fontSize || 10,
            family: "Roboto"
        };

        var options = {
            title: null,
            subtitle: null,
            shadowSize: angular.isDefined(settings.shadowSize) ? settings.shadowSize : 4,
            HtmlText: false,
            grid: {
                hoverable: true,
                mouseActiveRadius: 10,
                autoHighlight: ctx.tooltipIndividual === true,
                markings: []
            },
            selection : { mode : ctx.isMobile ? null : 'x' },
            legend : {
                show: false
            }
        };

        if (this.chartType === 'line' || this.chartType === 'bar' || this.chartType === 'state') {
            options.xaxes = [];
            this.xaxis = {
                mode: 'time',
                timezone: 'browser',
                font: angular.copy(font),
                labelFont: angular.copy(font)
            };
            this.yaxis = {
                font: angular.copy(font),
                labelFont: angular.copy(font)
            };
            if (settings.xaxis) {
                this.xaxis.font.color = settings.xaxis.color || this.xaxis.font.color;
                this.xaxis.label = this.utils.customTranslation(settings.xaxis.title, settings.xaxis.title) || null;
                this.xaxis.labelFont.color = this.xaxis.font.color;
                this.xaxis.labelFont.size = this.xaxis.font.size+2;
                this.xaxis.labelFont.weight = "bold";
            }

            ctx.yAxisTickFormatter = function(value/*, axis*/) {
                if (settings.yaxis && settings.yaxis.showLabels === false) {
                    return '';
                }
                if (this.ticksFormatterFunction) {
                    return this.ticksFormatterFunction(value);
                }

                var factor = this.tickDecimals ? Math.pow(10, this.tickDecimals) : 1,
                    formatted = "" + Math.round(value * factor) / factor;
                if (this.tickDecimals != null) {
                    var decimal = formatted.indexOf("."),
                        precision = decimal === -1 ? 0 : formatted.length - decimal - 1;

                    if (precision < this.tickDecimals) {
                        formatted = (precision ? formatted : formatted + ".") + ("" + factor).substr(1, this.tickDecimals - precision);
                    }
                }
                if (this.tickUnits) {
                     formatted += ' ' + this.tickUnits;
                }

                return formatted;
            };

            this.yaxis.tickFormatter = ctx.yAxisTickFormatter;

            if (settings.yaxis) {
                this.yaxis.font.color = settings.yaxis.color || this.yaxis.font.color;
                this.yaxis.min = angular.isDefined(settings.yaxis.min) ? settings.yaxis.min : null;
                this.yaxis.max = angular.isDefined(settings.yaxis.max) ? settings.yaxis.max : null;
                this.yaxis.label = this.utils.customTranslation(settings.yaxis.title, settings.yaxis.title) || null;
                this.yaxis.labelFont.color = this.yaxis.font.color;
                this.yaxis.labelFont.size = this.yaxis.font.size+2;
                this.yaxis.labelFont.weight = "bold";
                if (angular.isNumber(settings.yaxis.tickSize)) {
                    this.yaxis.tickSize = settings.yaxis.tickSize;
                } else {
                    this.yaxis.tickSize = null;
                }
                if (angular.isNumber(settings.yaxis.tickDecimals)) {
                    this.yaxis.tickDecimals = settings.yaxis.tickDecimals
                } else {
                    this.yaxis.tickDecimals = null;
                }
                if (settings.yaxis.ticksFormatter && settings.yaxis.ticksFormatter.length) {
                    try {
                        this.yaxis.ticksFormatterFunction = new Function('value', settings.yaxis.ticksFormatter);
                    } catch (e) {
                        this.yaxis.ticksFormatterFunction = null;
                    }
                }
            }

            options.grid.borderWidth = 1;
            options.grid.color = settings.fontColor || "#545454";

            if (settings.grid) {
                options.grid.color = settings.grid.color || "#545454";
                options.grid.backgroundColor = settings.grid.backgroundColor || null;
                options.grid.tickColor = settings.grid.tickColor || "#DDDDDD";
                options.grid.borderWidth = angular.isDefined(settings.grid.outlineWidth) ?
                    settings.grid.outlineWidth : 1;
                if (settings.grid.verticalLines === false) {
                    this.xaxis.tickLength = 0;
                }
                if (settings.grid.horizontalLines === false) {
                    this.yaxis.tickLength = 0;
                }
                if (angular.isDefined(settings.grid.margin)) {
                    options.grid.margin = settings.grid.margin;
                }
                if (angular.isDefined(settings.grid.minBorderMargin)) {
                    options.grid.minBorderMargin = settings.grid.minBorderMargin;
                }
            }

            options.xaxes[0] = angular.copy(this.xaxis);

            if (settings.xaxis && settings.xaxis.showLabels === false) {
                options.xaxes[0].tickFormatter = function() {
                    return '';
                };
            }

            if (settings.comparisonEnabled) {
                var xaxis = angular.copy(this.xaxis);
                xaxis.position = 'top';
                if (settings.xaxisSecond) {
                    if (settings.xaxisSecond.showLabels === false) {
                        xaxis.tickFormatter = function() {
                            return '';
                        };
                    }
                    xaxis.label = this.utils.customTranslation(settings.xaxisSecond.title, settings.xaxisSecond.title) || null;
                    xaxis.position = settings.xaxisSecond.axisPosition;
                }
                xaxis.tickLength = 0;
                options.xaxes.push(xaxis);

                options.series = {
                    stack: false
                };
            } else {
                options.series = {
                    stack: settings.stack === true
                };
            }

            options.crosshair = {
                mode: 'x'
            };

            if (this.chartType === 'line' && settings.smoothLines) {
                options.series.curvedLines = {
                    active: true,
                    monotonicFit: true
                }
            }

            if (this.chartType === 'line' && isFinite(settings.thresholdsLineWidth)) {
                options.grid.markingsLineWidth = settings.thresholdsLineWidth;
            }

            if (this.chartType === 'bar') {
                options.series.lines = {
                        show: false,
                        fill: false,
                        steps: false
                }
                options.series.bars ={
                        show: true,
                        lineWidth: 0,
                        fill: 0.9,
                        align: settings.barAlignment || "left"
                }
                ctx.defaultBarWidth = settings.defaultBarWidth || 600;
            }

            if (this.chartType === 'state') {
                options.series.lines = {
                    steps: true,
                    show: true
                }
            }

        } else if (this.chartType === 'pie') {
            options.series = {
                pie: {
                    show: true,
                    label: {
                        show: settings.showLabels || settings.showPercentages
                    },
                    radius: settings.radius || 1,
                    innerRadius: settings.innerRadius || 0,
                    stroke: {
                        color: '#fff',
                        width: 0
                    },
                    tilt: settings.tilt || 1,
                    shadow: {
                        left: 5,
                        top: 15,
                        alpha: 0.02
                    }
                }
            }
            options.grid.clickable = true;
                
            if (settings.stroke) {
                options.series.pie.stroke.color = settings.stroke.color || '#fff';
                options.series.pie.stroke.width = settings.stroke.width || 0;
            }

            if (options.series.pie.label.show) {
                options.series.pie.label.formatter = function (label, series) {
                    return "<div class='pie-label'>" +
                        (settings.showLabels ? series.dataKey.label : "") +
                        (settings.showLabels && settings.showPercentages ? "<br/>" : "") +
                        (settings.showPercentages ? Math.round(series.percent) + "%" : "") +
                        "</div>";
                }
                options.series.pie.label.radius = 3/4;
                options.series.pie.label.background = {
                     opacity: 0.8
                };
            }
        }

        //Experimental
        this.ctx.animatedPie = settings.animatedPie === true;

        this.options = options;

        if (this.ctx.defaultSubscription) {
            this.init(this.ctx.$container, this.ctx.defaultSubscription);
        }
    }

    init($element, subscription) {
        this.subscription = subscription;
        this.$element = $element;
        var colors = [];
        this.yaxes = [];
        var yaxesMap = {};
        let predefinedThresholds = [], thresholdsDatasources = [];

        var tooltipValueFormatFunction = null;
        if (this.ctx.settings.tooltipValueFormatter && this.ctx.settings.tooltipValueFormatter.length) {
            try {
                tooltipValueFormatFunction = new Function('value, ctx, index, time', this.ctx.settings.tooltipValueFormatter);
            } catch (e) {
                tooltipValueFormatFunction = null;
            }
        }

        for (var i = 0; i < this.subscription.data.length; i++) {
            var series = this.subscription.data[i];
            colors.push(series.dataKey.color);
            var keySettings = series.dataKey.settings;

            series.dataKey.tooltipValueFormatFunction = tooltipValueFormatFunction;
            if (keySettings.tooltipValueFormatter && keySettings.tooltipValueFormatter.length) {
                try {
                    series.dataKey.tooltipValueFormatFunction = new Function('value, ctx, index, time', keySettings.tooltipValueFormatter);
                } catch (e) {
                    series.dataKey.tooltipValueFormatFunction = tooltipValueFormatFunction;
                }
            }
            series.lines = {
                fill: keySettings.fillLines === true
            };

            if (this.ctx.settings.stack && !this.ctx.settings.comparisonEnabled) {
                series.stack = !keySettings.excludeFromStacking;
            } else {
                series.stack = false;
            }

            if (this.chartType === 'line' || this.chartType === 'state') {
                series.lines.show = keySettings.showLines !== false
            } else {
                series.lines.show = keySettings.showLines === true;
            }

            if (angular.isDefined(keySettings.lineWidth)) {
                series.lines.lineWidth = keySettings.lineWidth;
            }

            series.points = {
                show: false,
                radius: 8
            };
            if (keySettings.showPoints === true) {
                series.points.show = true;
                series.points.lineWidth = angular.isDefined(keySettings.showPointsLineWidth) ? keySettings.showPointsLineWidth : 5;
                series.points.radius = angular.isDefined(keySettings.showPointsRadius) ? keySettings.showPointsRadius : 3;
                series.points.symbol = angular.isDefined(keySettings.showPointShape) ? keySettings.showPointShape : 'circle';
                if (series.points.symbol == 'custom' && keySettings.pointShapeFormatter) {
                    try {
                        series.points.symbol = new Function('ctx, x, y, radius, shadow', keySettings.pointShapeFormatter);
                    } catch (e) {
                        series.points.symbol = 'circle';
                    }
                }

            }

            if (this.chartType === 'line' && this.ctx.settings.smoothLines && !series.points.show) {
                series.curvedLines = {
                    apply: true
                };
            }

            var lineColor = tinycolor(series.dataKey.color);
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
                var units = series.dataKey.units && series.dataKey.units.length ? series.dataKey.units : this.ctx.trackUnits;
                var yaxis;
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
                series.yaxis = series.yaxisIndex+1;
                yaxis.keysInfo[i] = {hidden: false};
                yaxis.show = true;

                if (keySettings.thresholds && keySettings.thresholds.length) {
                    for (let j = 0; j < keySettings.thresholds.length; j++) {
                        let threshold = keySettings.thresholds[j];
                        if (threshold.thresholdValueSource === 'predefinedValue' && isFinite(threshold.thresholdValue)) {
                            let colorIndex = this.subscription.data.length + predefinedThresholds.length;
                            this.generateThreshold(predefinedThresholds, series.yaxis, threshold.lineWidth, threshold.color, colorIndex, threshold.thresholdValue);
                        } else if (threshold.thresholdEntityAlias && threshold.thresholdAttribute) {
                            let entityAliasId = this.ctx.aliasController.getEntityAliasId(threshold.thresholdEntityAlias);
                            if (!entityAliasId) {
                                continue;
                            }

                            let datasource = thresholdsDatasources.filter((datasource) => {
                                return datasource.entityAliasId === entityAliasId;
                            })[0];

                            let dataKey = {
                                type: this.types.dataKeyType.attribute,
                                name: threshold.thresholdAttribute,
                                label: threshold.thresholdAttribute,
                                settings: {
                                    yaxis: series.yaxis,
                                    lineWidth: threshold.lineWidth,
                                    color: threshold.color
                                },
                                _hash: Math.random()
                            };

                            if (datasource) {
                                datasource.dataKeys.push(dataKey);
                            } else {
                                datasource = {
                                    type: this.types.datasourceType.entity,
                                    name: threshold.thresholdEntityAlias,
                                    aliasName: threshold.thresholdEntityAlias,
                                    entityAliasId: entityAliasId,
                                    dataKeys: [ dataKey ]
                                };
                                thresholdsDatasources.push(datasource);
                            }
                        }
                    }
                }
            }
        }

        this.subscribeForThresholdsAttributes(thresholdsDatasources);

        this.options.grid.markings = predefinedThresholds;
        this.predefinedThresholds = predefinedThresholds;

        this.options.colors = colors;
        this.options.yaxes = angular.copy(this.yaxes);
        if (this.chartType === 'line' || this.chartType === 'bar' || this.chartType === 'state') {
            if (this.chartType === 'bar') {
                if (this.subscription.timeWindowConfig.aggregation && this.subscription.timeWindowConfig.aggregation.type === 'NONE') {
                    this.options.series.bars.barWidth = this.ctx.defaultBarWidth;
                } else {
                    this.options.series.bars.barWidth = this.subscription.timeWindow.interval * 0.6;
                }
            }
            this.options.xaxes[0].min = this.subscription.timeWindow.minTime;
            this.options.xaxes[0].max = this.subscription.timeWindow.maxTime;
            if (this.ctx.settings.comparisonEnabled) {
                this.options.xaxes[1].min = this.subscription.comparisonTimeWindow.minTime;
                this.options.xaxes[1].max = this.subscription.comparisonTimeWindow.maxTime;
            }
        }

        this.checkMouseEvents();

        if (this.ctx.plot) {
            this.ctx.plot.destroy();
        }

        if (this.chartType === 'pie' && this.ctx.animatedPie) {
            this.ctx.pieDataAnimationDuration = 250;
            this.pieData = angular.copy(this.subscription.data);
            this.ctx.pieRenderedData = [];
            this.ctx.pieTargetData = [];
            for (i = 0; i < this.subscription.data.length; i++) {
                this.ctx.pieTargetData[i] = (this.subscription.data[i].data && this.subscription.data[i].data[0])
                    ? this.subscription.data[i].data[0][1] : 0;
            }
            this.pieDataRendered();
        }
        this.ctx.plotInited = true;
        this.createPlot();
    }

    createYAxis(keySettings, units) {
        var yaxis = angular.copy(this.yaxis);
        var tickDecimals, tickSize;

        var label = keySettings.axisTitle && keySettings.axisTitle.length ? keySettings.axisTitle : yaxis.label;
        if (angular.isNumber(keySettings.axisTickDecimals)) {
            tickDecimals = keySettings.axisTickDecimals;
        } else {
            tickDecimals = yaxis.tickDecimals;
        }
        if (angular.isNumber(keySettings.axisTickSize)) {
            tickSize = keySettings.axisTickSize;
        } else {
            tickSize = yaxis.tickSize;
        }
        var position = keySettings.axisPosition && keySettings.axisPosition.length ? keySettings.axisPosition : "left";

        var min = angular.isDefined(keySettings.axisMin) ? keySettings.axisMin : yaxis.min;
        var max = angular.isDefined(keySettings.axisMax) ? keySettings.axisMax : yaxis.max;

        yaxis.label = label;
        yaxis.min = min;
        yaxis.max = max;
        yaxis.tickUnits = units;
        yaxis.tickDecimals = tickDecimals;
        yaxis.tickSize = tickSize;
        if (position === "right" && tickSize === null) {
            yaxis.alignTicksWithAxis = 1;
        } else {
            yaxis.alignTicksWithAxis = null;
        }
        yaxis.position = position;

        yaxis.keysInfo = [];

        if (keySettings.axisTicksFormatter && keySettings.axisTicksFormatter.length) {
            try {
                yaxis.ticksFormatterFunction = new Function('value', keySettings.axisTicksFormatter);
            } catch (e) {
                yaxis.ticksFormatterFunction = this.yaxis.ticksFormatterFunction;
            }
        }
        return yaxis;
    }

    subscribeForThresholdsAttributes(datasources) {
        let tbFlot = this;
        let thresholdsSourcesSubscriptionOptions = {
            datasources: datasources,
            useDashboardTimewindow: false,
            type: this.types.widgetType.latest.value,
            callbacks: {
                onDataUpdated: (subscription) => {tbFlot.thresholdsSourcesDataUpdated(subscription.data)}
            }
        };
        this.ctx.subscriptionApi.createSubscription(thresholdsSourcesSubscriptionOptions, true).then(
            (subscription) => {
                tbFlot.thresholdsSourcesSubscription = subscription;
            }
        );
    }

    thresholdsSourcesDataUpdated(data) {
        let allThresholds = angular.copy(this.predefinedThresholds);
        for (let i = 0; i < data.length; i++) {
            let keyData = data[i];
            if (keyData && keyData.data && keyData.data[0]) {
                let attrValue = keyData.data[0][1];
                if (isFinite(attrValue)) {
                    let settings = keyData.dataKey.settings;
                    let colorIndex = this.subscription.data.length + allThresholds.length;
                    this.generateThreshold(allThresholds, settings.yaxis, settings.lineWidth, settings.color, colorIndex, attrValue);
                }
            }
        }

        this.options.grid.markings = allThresholds;
        this.redrawPlot();
    }

    generateThreshold(existingThresholds, yaxis, lineWidth, color, defaultColorIndex, thresholdValue) {
        let marking = {};
        let markingYAxis;

        if (yaxis !== 1) {
            markingYAxis = 'y' + yaxis + 'axis';
        } else {
            markingYAxis = 'yaxis';
        }

        if (isFinite(lineWidth)) {
            marking.lineWidth = lineWidth;
        }

        if (angular.isDefined(color)) {
            marking.color = color;
        } else {
            marking.color = this.utils.getMaterialColor(defaultColorIndex);
        }

        marking[markingYAxis] = {
            from: thresholdValue,
            to: thresholdValue
        };

        let similarMarkings = existingThresholds.filter((existingMarking) => {
            return angular.equals(existingMarking[markingYAxis], marking[markingYAxis]);
        });
        if (!similarMarkings.length) {
            existingThresholds.push(marking);
        }
    }

    update() {
        if (this.updateTimeoutHandle) {
            this.ctx.$scope.$timeout.cancel(this.updateTimeoutHandle);
            this.updateTimeoutHandle = null;
        }
        if (this.subscription) {
            if (!this.isMouseInteraction && this.ctx.plot) {
                if (this.chartType === 'line' || this.chartType === 'bar' || this.chartType === 'state') {

                    var axisVisibilityChanged = false;
                    if (this.yaxis) {
                        for (var i = 0; i < this.subscription.data.length; i++) {
                            var series = this.subscription.data[i];
                            var yaxisIndex = series.yaxisIndex;
                            if (this.yaxes[yaxisIndex].keysInfo[i].hidden != series.dataKey.hidden) {
                                this.yaxes[yaxisIndex].keysInfo[i].hidden = series.dataKey.hidden;
                                axisVisibilityChanged = true;
                            }
                        }
                        if (axisVisibilityChanged) {
                            this.options.yaxes.length = 0;
                            for (var y = 0; y < this.yaxes.length; y++) {
                                var yaxis = this.yaxes[y];
                                var hidden = true;
                                for (var k = 0; k < yaxis.keysInfo.length; k++) {
                                    if (yaxis.keysInfo[k]) {
                                        hidden = hidden && yaxis.keysInfo[k].hidden;
                                    }
                                }
                                yaxis.hidden = hidden;
                                var newIndex = 1;
                                if (!yaxis.hidden) {
                                    this.options.yaxes.push(yaxis);
                                    newIndex = this.options.yaxes.length;
                                }
                                for (k = 0; k < yaxis.keysInfo.length; k++) {
                                    if (yaxis.keysInfo[k]) {
                                        this.subscription.data[k].yaxis = newIndex;
                                    }
                                }

                            }
                            this.options.yaxis = {
                                show: this.options.yaxes.length ? true : false
                            };
                        }
                    }

                    this.options.xaxes[0].min = this.subscription.timeWindow.minTime;
                    this.options.xaxes[0].max = this.subscription.timeWindow.maxTime;
                    if (this.ctx.settings.comparisonEnabled) {
                        this.options.xaxes[1].min = this.subscription.comparisonTimeWindow.minTime;
                        this.options.xaxes[1].max = this.subscription.comparisonTimeWindow.maxTime;
                    }
                    if (this.chartType === 'bar') {
                        if (this.subscription.timeWindowConfig.aggregation && this.subscription.timeWindowConfig.aggregation.type === 'NONE') {
                            this.options.series.bars.barWidth = this.ctx.defaultBarWidth;
                        } else {
                            this.options.series.bars.barWidth = this.subscription.timeWindow.interval * 0.6;
                        }
                    }

                    if (axisVisibilityChanged) {
                        this.redrawPlot();
                    } else {
                        this.ctx.plot.getOptions().xaxes[0].min = this.subscription.timeWindow.minTime;
                        this.ctx.plot.getOptions().xaxes[0].max = this.subscription.timeWindow.maxTime;
                        if (this.ctx.settings.comparisonEnabled) {
                            this.ctx.plot.getOptions().xaxes[1].min = this.subscription.comparisonTimeWindow.minTime;
                            this.ctx.plot.getOptions().xaxes[1].max = this.subscription.comparisonTimeWindow.maxTime;
                        }
                        if (this.chartType === 'bar') {
                            if (this.subscription.timeWindowConfig.aggregation && this.subscription.timeWindowConfig.aggregation.type === 'NONE') {
                                this.ctx.plot.getOptions().series.bars.barWidth = this.ctx.defaultBarWidth;
                            } else {
                                this.ctx.plot.getOptions().series.bars.barWidth = this.subscription.timeWindow.interval * 0.6;
                            }
                        }
                        this.updateData();
                    }
                } else if (this.chartType === 'pie') {
                    if (this.ctx.animatedPie) {
                        this.nextPieDataAnimation(true);
                    } else {
                        this.updateData();
                    }
                }
            } else if (this.isMouseInteraction && this.ctx.plot){
                var tbFlot = this;
                this.updateTimeoutHandle = this.ctx.$scope.$timeout(function() {
                    tbFlot.update();
                }, 30, false);
            }
        }
    }

    updateData() {
        this.ctx.plot.setData(this.subscription.data);
        if (this.chartType !== 'pie') {
            this.ctx.plot.setupGrid();
        }
        this.ctx.plot.draw();
    }

    resize() {
        if (this.resizeTimeoutHandle) {
            this.ctx.$scope.$timeout.cancel(this.resizeTimeoutHandle);
            this.resizeTimeoutHandle = null;
        }
        if (this.ctx.plot && this.ctx.plotInited) {
            var width = this.$element.width();
            var height = this.$element.height();
            if (width && height) {
                this.ctx.plot.resize();
                if (this.chartType !== 'pie') {
                    this.ctx.plot.setupGrid();
                }
                this.ctx.plot.draw();
            } else {
                var tbFlot = this;
                this.resizeTimeoutHandle = this.ctx.$scope.$timeout(function() {
                    tbFlot.resize();
                }, 30, false);
            }
        }
    }


    redrawPlot() {
        if (this.ctx.plot && this.ctx.plotInited) {
            this.ctx.plot.destroy();
            this.ctx.plot = null;
            this.createPlot();
        }
    }

    createPlot() {
        if (this.createPlotTimeoutHandle) {
            this.ctx.$scope.$timeout.cancel(this.createPlotTimeoutHandle);
            this.createPlotTimeoutHandle = null;
        }
        if (this.ctx.plotInited && !this.ctx.plot) {
            var width = this.$element.width();
            var height = this.$element.height();
            if (width && height) {
                if (this.chartType === 'pie' && this.ctx.animatedPie) {
                    this.ctx.plot = $.plot(this.$element, this.pieData, this.options);
                } else {
                    this.ctx.plot = $.plot(this.$element, this.subscription.data, this.options);
                }
            } else {
                var tbFlot = this;
                this.createPlotTimeoutHandle = this.ctx.$scope.$timeout(function() {
                    tbFlot.createPlot();
                }, 30, false);
            }
        }
    }

    destroy() {
        this.cleanup();
        if (this.ctx.plot) {
            this.ctx.plot.destroy();
            this.ctx.plot = null;
            this.ctx.plotInited = false;
        }
    }

    cleanup() {
        if (this.updateTimeoutHandle) {
            this.ctx.$scope.$timeout.cancel(this.updateTimeoutHandle);
            this.updateTimeoutHandle = null;
        }
        if (this.createPlotTimeoutHandle) {
            this.ctx.$scope.$timeout.cancel(this.createPlotTimeoutHandle);
            this.createPlotTimeoutHandle = null;
        }
        if (this.resizeTimeoutHandle) {
            this.ctx.$scope.$timeout.cancel(this.resizeTimeoutHandle);
            this.resizeTimeoutHandle = null;
        }
    }

    checkMouseEvents() {
        var enabled = !this.ctx.isMobile &&  !this.ctx.isEdit;
        if (angular.isUndefined(this.mouseEventsEnabled) || this.mouseEventsEnabled != enabled) {
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

    static get pieSettingsSchema() {
        return {
            "schema": {
                "type": "object",
                "title": "Settings",
                "properties": {
                    "radius": {
                        "title": "Radius",
                        "type": "number",
                        "default": 1
                    },
                    "innerRadius": {
                        "title": "Inner radius",
                        "type": "number",
                        "default": 0
                    },
                    "tilt": {
                        "title": "Tilt",
                        "type": "number",
                        "default": 1
                    },
                    "animatedPie": {
                        "title": "Enable pie animation (experimental)",
                        "type": "boolean",
                        "default": false
                    },
                    "stroke": {
                        "title": "Stroke",
                        "type": "object",
                        "properties": {
                            "color": {
                                "title": "Color",
                                "type": "string",
                                "default": ""
                            },
                            "width": {
                                "title": "Width (pixels)",
                                "type": "number",
                                "default": 0
                            }
                        }
                    },
                    "showLabels": {
                        "title": "Show labels",
                        "type": "boolean",
                        "default": false
                    },
                    "showPercentages": {
                        "title": "Show percentages",
                        "type": "boolean",
                        "default": false
                    },
                    "fontColor": {
                        "title": "Font color",
                        "type": "string",
                        "default": "#545454"
                    },
                    "fontSize": {
                        "title": "Font size",
                        "type": "number",
                        "default": 10
                    }
                },
                "required": []
            },
            "form": [
                "radius",
                "innerRadius",
                "animatedPie",
                "tilt",
                {
                    "key": "stroke",
                    "items": [
                        {
                            "key": "stroke.color",
                            "type": "color"
                        },
                        "stroke.width"
                    ]
                },
                "showLabels",
                "showPercentages",
                {
                    "key": "fontColor",
                    "type": "color"
                },
                "fontSize"
            ]
        }
    }

    static settingsSchema(chartType) {

        var schema = {
            "schema": {
                "type": "object",
                "title": "Settings",
                "properties": {
                }
            }
        };

        var properties = schema["schema"]["properties"];
        properties["stack"] = {
            "title": "Stacking",
            "type": "boolean",
            "default": false
        };
        if (chartType === 'graph') {
            properties["smoothLines"] = {
                "title": "Display smooth (curved) lines",
                "type": "boolean",
                "default": false
            };
        }
        if (chartType === 'bar') {
            properties["defaultBarWidth"] = {
                "title": "Default bar width for non-aggregated data (milliseconds)",
                "type": "number",
                "default": 600
            };
            properties["barAlignment"] = {
                "title": "Bar alignment",
                "type": "string",
                "default": "left"
            };
        }
        if (chartType === 'graph' || chartType === 'bar') {
            properties["thresholdsLineWidth"] = {
                "title": "Default line width for all thresholds",
                "type": "number"
            };
        }
        properties["shadowSize"] = {
            "title": "Shadow size",
            "type": "number",
            "default": 4
        };
        properties["fontColor"] =  {
            "title": "Font color",
            "type": "string",
            "default": "#545454"
        };
        properties["fontSize"] = {
            "title": "Font size",
            "type": "number",
            "default": 10
        };
        properties["tooltipIndividual"] = {
            "title": "Hover individual points",
            "type": "boolean",
            "default": false
        };
        properties["tooltipCumulative"] = {
            "title": "Show cumulative values in stacking mode",
            "type": "boolean",
            "default": false
        };
        properties["tooltipValueFormatter"] = {
            "title": "Tooltip value format function, f(value, ctx, index, time)",
            "type": "string",
            "default": ""
        };
        properties["hideZeros"] = {
            "title": "Hide zero/false values from tooltip",
            "type": "boolean",
            "default": false
        };

        properties["grid"] = {
            "title": "Grid settings",
                "type": "object",
                "properties": {
                "color": {
                    "title": "Primary color",
                        "type": "string",
                        "default": "#545454"
                },
                "backgroundColor": {
                    "title": "Background color",
                        "type": "string",
                        "default": null
                },
                "tickColor": {
                    "title": "Ticks color",
                        "type": "string",
                        "default": "#DDDDDD"
                },
                "outlineWidth": {
                    "title": "Grid outline/border width (px)",
                        "type": "number",
                        "default": 1
                },
                "verticalLines": {
                    "title": "Show vertical lines",
                        "type": "boolean",
                        "default": true
                },
                "horizontalLines": {
                    "title": "Show horizontal lines",
                        "type": "boolean",
                        "default": true
                }
            }
        };

        properties["xaxis"] = {
            "title": "X axis settings",
            "type": "object",
            "properties": {
                "showLabels": {
                    "title": "Show labels",
                    "type": "boolean",
                    "default": true
                },
                "title": {
                    "title": "Axis title",
                    "type": "string",
                    "default": null
                },
                "color": {
                    "title": "Ticks color",
                    "type": "string",
                    "default": null
                }
            }
        };

        properties["yaxis"] = {
            "title": "Y axis settings",
            "type": "object",
            "properties": {
                "min": {
                    "title": "Minimum value on the scale",
                    "type": "number",
                    "default": null
                },
                "max": {
                    "title": "Maximum value on the scale",
                    "type": "number",
                    "default": null
                },
                "showLabels": {
                    "title": "Show labels",
                    "type": "boolean",
                    "default": true
                },
                "title": {
                    "title": "Axis title",
                    "type": "string",
                    "default": null
                },
                "color": {
                    "title": "Ticks color",
                    "type": "string",
                    "default": null
                },
                "ticksFormatter": {
                    "title": "Ticks formatter function, f(value)",
                    "type": "string",
                    "default": ""
                },
                "tickDecimals": {
                    "title": "The number of decimals to display",
                    "type": "number",
                    "default": 0
                },
                "tickSize": {
                    "title": "Step size between ticks",
                    "type": "number",
                    "default": null
                }
            }
        };

        schema["schema"]["required"] = [];
        schema["form"] = ["stack"];
        if (chartType === 'graph') {
            schema["form"].push("smoothLines");
        }
        if (chartType === 'bar') {
            schema["form"].push("defaultBarWidth");
            schema["form"].push({
                "key": "barAlignment",
                "type": "rc-select",
                "multiple": false,
                "items": [
                    {
                        "value": "left",
                        "label": "Left"
                    },
                    {
                        "value": "right",
                        "label": "Right"
                    },
                    {
                        "value": "center",
                        "label": "Center"
                    }
                ]
            });
        }
        if (chartType === 'graph' || chartType === 'bar') {
            schema["form"].push("thresholdsLineWidth");
        }
        schema["form"].push("shadowSize");
        schema["form"].push({
            "key": "fontColor",
            "type": "color"
        });
        schema["form"].push("fontSize");
        schema["form"].push("tooltipIndividual");
        schema["form"].push("tooltipCumulative");
        schema["form"].push({
            "key": "tooltipValueFormatter",
            "type": "javascript"
        });
        schema["form"].push("hideZeros");
        schema["form"].push({
            "key": "grid",
            "items": [
                {
                    "key": "grid.color",
                    "type": "color"
                },
                {
                    "key": "grid.backgroundColor",
                    "type": "color"
                },
                {
                    "key": "grid.tickColor",
                    "type": "color"
                },
                "grid.outlineWidth",
                "grid.verticalLines",
                "grid.horizontalLines"
            ]
        });
        schema["form"].push({
            "key": "xaxis",
            "items": [
                "xaxis.showLabels",
                "xaxis.title",
                {
                    "key": "xaxis.color",
                    "type": "color"
                }
            ]
        });
        schema["form"].push({
            "key": "yaxis",
            "items": [
                "yaxis.min",
                "yaxis.max",
                "yaxis.tickDecimals",
                "yaxis.tickSize",
                "yaxis.showLabels",
                "yaxis.title",
                {
                    "key": "yaxis.color",
                    "type": "color"
                },
                {
                    "key": "yaxis.ticksFormatter",
                    "type": "javascript"
                }
            ]
        });
        if (chartType === 'graph' || chartType === 'bar') {
            schema.groupInfoes = [{
                "formIndex":0,
                "GroupTitle":"Common Settings"
            }];
            schema.form = [schema.form];
            angular.merge(schema.schema.properties, chartSettingsSchemaForComparison.schema.properties);
            schema.schema.required = schema.schema.required.concat(chartSettingsSchemaForComparison.schema.required);
            schema.form.push(chartSettingsSchemaForComparison.form);
            schema.groupInfoes.push({
                "formIndex":schema.groupInfoes.length,
                "GroupTitle":"Comparison Settings"
            });
        }

        return schema;
    }

    static get pieDatakeySettingsSchema() {
        return {
            "schema": {
                "type": "object",
                "title": "DataKeySettings",
                "properties": {
                    "hideDataByDefault": {
                        "title": "Data is hidden by default",
                        "type": "boolean",
                        "default": false
                    },
                    "disableDataHiding": {
                        "title": "Disable data hiding",
                        "type": "boolean",
                        "default": false
                    },
                    "removeFromLegend": {
                        "title": "Remove datakey from legend",
                        "type": "boolean",
                        "default": false
                    }
                },
                "required": []
            },
            "form": [
                "hideDataByDefault",
                "disableDataHiding",
                "removeFromLegend"
            ]
        };
    }

    static datakeySettingsSchema(defaultShowLines, chartType) {

        var schema = {
            "schema": {
                "type": "object",
                "title": "DataKeySettings",
                "properties": {
                    "excludeFromStacking": {
                        "title": "Exclude from stacking(available in \"Stacking\" mode)",
                        "type": "boolean",
                        "default": false
                    },
                    "hideDataByDefault": {
                        "title": "Data is hidden by default",
                        "type": "boolean",
                        "default": false
                    },
                    "disableDataHiding": {
                        "title": "Disable data hiding",
                        "type": "boolean",
                        "default": false
                    },
                    "removeFromLegend": {
                        "title": "Remove datakey from legend",
                        "type": "boolean",
                        "default": false
                    },
                    "showLines": {
                        "title": "Show lines",
                        "type": "boolean",
                        "default": defaultShowLines
                    },
                    "fillLines": {
                        "title": "Fill lines",
                        "type": "boolean",
                        "default": false
                    },
                    "showPoints": {
                        "title": "Show points",
                        "type": "boolean",
                        "default": false
                    },
                    "showPointShape": {
                        "title": "Select point shape:",
                        "type": "string",
                        "default": "circle"
                    },
                    "pointShapeFormatter": {
                        "title": "Point shape format function, f(ctx, x, y, radius, shadow)",
                        "type": "string",
                        "default": "var size = radius * Math.sqrt(Math.PI) / 2;\n" +
                            "ctx.moveTo(x - size, y - size);\n" +
                            "ctx.lineTo(x + size, y + size);\n" +
                            "ctx.moveTo(x - size, y + size);\n" +
                            "ctx.lineTo(x + size, y - size);"
                    },
                    "showPointsLineWidth": {
                        "title": "Line width of points",
                        "type": "number",
                        "default": 5
                    },
                    "showPointsRadius": {
                        "title": "Radius of points",
                        "type": "number",
                        "default": 3
                    },
                    "tooltipValueFormatter": {
                        "title": "Tooltip value format function, f(value, ctx, index, time)",
                        "type": "string",
                        "default": ""
                    },
                    "showSeparateAxis": {
                        "title": "Show separate axis",
                        "type": "boolean",
                        "default": false
                    },
                    "axisMin": {
                        "title": "Minimum value on the axis scale",
                        "type": "number",
                        "default": null
                    },
                    "axisMax": {
                        "title": "Maximum value on the axis scale",
                        "type": "number",
                        "default": null
                    },
                    "axisTitle": {
                        "title": "Axis title",
                        "type": "string",
                        "default": ""
                    },
                    "axisTickDecimals": {
                        "title": "Axis tick number of digits after floating point",
                        "type": "number",
                        "default": null
                    },
                    "axisTickSize": {
                        "title": "Axis step size between ticks",
                        "type": "number",
                        "default": null
                    },
                    "axisPosition": {
                        "title": "Axis position",
                        "type": "string",
                        "default": "left"
                    },
                    "axisTicksFormatter": {
                        "title": "Ticks formatter function, f(value)",
                        "type": "string",
                        "default": ""
                    }
                },
                "required": ["showLines", "fillLines", "showPoints"]
            },
            "form": [
                "hideDataByDefault",
                "disableDataHiding",
                "removeFromLegend",
                "excludeFromStacking",
                "showLines",
                "fillLines",
                "showPoints",
                {
                    "key": "showPointShape",
                    "type": "rc-select",
                    "multiple": false,
                    "items": [
                        {
                            "value": "circle",
                            "label": "Circle"
                        },
                        {
                            "value": "cross",
                            "label": "Cross"
                        },
                        {
                            "value": "diamond",
                            "label": "Diamond"
                        },
                        {
                            "value": "square",
                            "label": "Square"
                        },
                        {
                            "value": "triangle",
                            "label": "Triangle"
                        },
                        {
                            "value": "custom",
                            "label": "Custom function"
                        }
                    ]
                },
                {
                    "key": "pointShapeFormatter",
                    "type": "javascript"
                },
                "showPointsLineWidth",
                "showPointsRadius",
                {
                    "key": "tooltipValueFormatter",
                    "type": "javascript"
                },
                "showSeparateAxis",
                "axisMin",
                "axisMax",
                "axisTitle",
                "axisTickDecimals",
                "axisTickSize",
                {
                    "key": "axisPosition",
                    "type": "rc-select",
                    "multiple": false,
                    "items": [
                        {
                            "value": "left",
                            "label": "Left"
                        },
                        {
                            "value": "right",
                            "label": "Right"
                        }
                    ]
                },
                {
                    "key": "axisTicksFormatter",
                    "type": "javascript"
                }
            ]
        };

        var properties = schema["schema"]["properties"];

        if (chartType === 'graph' || chartType === 'bar') {
            properties["thresholds"] = {
                "title": "Thresholds",
                "type": "array",
                "items": {
                    "title": "Threshold",
                    "type": "object",
                    "properties": {
                        "thresholdValueSource": {
                            "title": "Threshold value source",
                            "type": "string",
                            "default": "predefinedValue"
                        },
                        "thresholdEntityAlias": {
                            "title": "Thresholds source entity alias",
                            "type": "string"
                        },
                        "thresholdAttribute": {
                            "title": "Threshold source entity attribute",
                            "type": "string"
                        },
                        "thresholdValue": {
                            "title": "Threshold value (if predefined value is selected)",
                            "type": "number"
                        },
                        "lineWidth": {
                            "title": "Line width",
                            "type": "number"
                        },
                        "color": {
                            "title": "Color",
                            "type": "string"
                        }
                    }
                },
                "required": []
            };
            schema["form"].push({
                "key": "thresholds",
                "items": [
                    {
                        "key": "thresholds[].thresholdValueSource",
                        "type": "rc-select",
                        "multiple": false,
                        "items": [
                            {
                                "value": "predefinedValue",
                                "label": "Predefined value (Default)"
                            },
                            {
                                "value": "entityAttribute",
                                "label": "Value taken from entity attribute"
                            }
                        ]
                    },
                    "thresholds[].thresholdValue",
                    "thresholds[].thresholdEntityAlias",
                    "thresholds[].thresholdAttribute",
                    {
                        "key": "thresholds[].color",
                        "type": "color"
                    },
                    "thresholds[].lineWidth"
                ]
            });

            properties["comparisonSettings"] = {
                "title": "Comparison Settings",
                "type": "object",
                "properties": {
                    "showValuesForComparison": {
                        "title": "Show historical values for comparison",
                        "type": "boolean",
                        "default": true
                    },
                    "comparisonValuesLabel": {
                        "title": "Historical values label",
                        "type": "string",
                        "default": ""
                    },
                    "color": {
                        "title": "Color",
                        "type": "string",
                        "default": ""
                    }
                },
                "required": ["showValuesForComparison"]
            };
            schema["form"].push({
                "key": "comparisonSettings",
                "items": [
                    "comparisonSettings.showValuesForComparison",
                    "comparisonSettings.comparisonValuesLabel",
                    {
                        "key": "comparisonSettings.color",
                        "type": "color"
                    }
                ]
            });

        }

        return schema;
    }

    enableMouseEvents() {
        this.$element.css('pointer-events','');
        this.$element.addClass('mouse-events');
        this.options.selection = { mode : 'x' };

        var tbFlot = this;

        if (!this.flotHoverHandler) {
            this.flotHoverHandler =  function (event, pos, item) {
                if (!tbFlot.ctx.plot) {
                    return;
                }
                if (!tbFlot.ctx.tooltipIndividual || item) {

                    var multipleModeTooltip = !tbFlot.ctx.tooltipIndividual;

                    if (multipleModeTooltip) {
                        tbFlot.ctx.plot.unhighlight();
                    }

                    var pageX = pos.pageX;
                    var pageY = pos.pageY;

                    var tooltipHtml;

                    if (tbFlot.chartType === 'pie') {
                        tooltipHtml = tbFlot.ctx.tooltipFormatter(item);
                    } else {
                        var hoverInfo = tbFlot.getHoverInfo(tbFlot.ctx.plot.getData(), pos);
                        if (angular.isNumber(hoverInfo[0].time) || (hoverInfo[1] && angular.isNumber(hoverInfo[1].time))) {
                            hoverInfo[0].seriesHover.sort(function (a, b) {
                                return b.value - a.value;
                            });
                            if (hoverInfo[1] && hoverInfo[1].seriesHover.length) {
                                hoverInfo[1].seriesHover.sort(function (a, b) {
                                    return b.value - a.value;
                                });
                            }
                            tooltipHtml = tbFlot.ctx.tooltipFormatter(hoverInfo, item ? item.seriesIndex : -1);
                        }
                    }

                    if (tooltipHtml) {
                        tbFlot.ctx.tooltip.html(tooltipHtml)
                            .css({top: 0, left: 0})
                            .fadeIn(200);

                        var windowWidth = $( window ).width();  //eslint-disable-line
                        var windowHeight = $( window ).height();  //eslint-disable-line
                        var tooltipWidth = tbFlot.ctx.tooltip.width();
                        var tooltipHeight = tbFlot.ctx.tooltip.height();
                        var left = pageX+5;
                        var top = pageY+5;
                        if (windowWidth - pageX < tooltipWidth + 50) {
                            left = pageX - tooltipWidth - 10;
                        }
                        if (windowHeight - pageY < tooltipHeight + 20) {
                            top = pageY - tooltipHeight - 10;
                        }
                        tbFlot.ctx.tooltip.css({
                            top: top,
                            left: left
                        });

                        if (multipleModeTooltip) {
                            for (var j = 0; j < hoverInfo.length; j++) {
                                for (var i = 0; i < hoverInfo[j].seriesHover.length; i++) {
                                    var seriesHoverInfo = hoverInfo[j].seriesHover[i];
                                    tbFlot.ctx.plot.highlight(seriesHoverInfo.index, seriesHoverInfo.hoverIndex);
                                }
                            }
                        }
                    }

                } else {
                    tbFlot.ctx.tooltip.stop(true);
                    tbFlot.ctx.tooltip.hide();
                    tbFlot.ctx.plot.unhighlight();
                }
            };
            this.$element.bind('plothover', this.flotHoverHandler);
        }

        if (!this.flotSelectHandler) {
            this.flotSelectHandler =  function (event, ranges) {
                if (!tbFlot.ctx.plot) {
                    return;
                }
                tbFlot.ctx.plot.clearSelection();
                tbFlot.subscription.onUpdateTimewindow(ranges.xaxis.from, ranges.xaxis.to);
            };
            this.$element.bind('plotselected', this.flotSelectHandler);
        }
        if (!this.dblclickHandler) {
            this.dblclickHandler =  function () {
                tbFlot.subscription.onResetTimewindow();
            };
            this.$element.bind('dblclick', this.dblclickHandler);
        }
        if (!this.mousedownHandler) {
            this.mousedownHandler =  function () {
                tbFlot.isMouseInteraction = true;
            };
            this.$element.bind('mousedown', this.mousedownHandler);
        }
        if (!this.mouseupHandler) {
            this.mouseupHandler =  function () {
                tbFlot.isMouseInteraction = false;
            };
            this.$element.bind('mouseup', this.mouseupHandler);
        }
        if (!this.mouseleaveHandler) {
            this.mouseleaveHandler =  function () {
                if (!tbFlot.ctx.plot) {
                    return;
                }
                tbFlot.ctx.tooltip.stop(true);
                tbFlot.ctx.tooltip.hide();
                tbFlot.ctx.plot.unhighlight();
                tbFlot.isMouseInteraction = false;
            };
            this.$element.bind('mouseleave', this.mouseleaveHandler);
        }

        if (!this.flotClickHandler) {
            this.flotClickHandler =  function (event, pos, item) {
                if (!tbFlot.ctx.plot) {
                    return;
                }
                tbFlot.onPieSliceClick(event, item);
            };
            this.$element.bind('plotclick', this.flotClickHandler);
        }

    }

    disableMouseEvents() {
        this.$element.css('pointer-events','none');
        this.$element.removeClass('mouse-events');
        this.options.selection = { mode : null };

        if (this.flotHoverHandler) {
            this.$element.unbind('plothover', this.flotHoverHandler);
            this.flotHoverHandler = null;
        }

        if (this.flotSelectHandler) {
            this.$element.unbind('plotselected', this.flotSelectHandler);
            this.flotSelectHandler = null;
        }
        if (this.dblclickHandler) {
            this.$element.unbind('dblclick', this.dblclickHandler);
            this.dblclickHandler = null;
        }
        if (this.mousedownHandler) {
            this.$element.unbind('mousedown', this.mousedownHandler);
            this.mousedownHandler = null;
        }
        if (this.mouseupHandler) {
            this.$element.unbind('mouseup', this.mouseupHandler);
            this.mouseupHandler = null;
        }
        if (this.mouseleaveHandler) {
            this.$element.unbind('mouseleave', this.mouseleaveHandler);
            this.mouseleaveHandler = null;
        }
        if (this.flotClickHandler) {
            this.$element.unbind('plotclick', this.flotClickHandler);
            this.flotClickHandler = null;
        }
    }


    findHoverIndexFromData (posX, series) {
        var lower = 0;
        var upper = series.data.length - 1;
        var middle;
        var index = null;
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

    findHoverIndexFromDataPoints (posX, series, last) {
        var ps = series.datapoints.pointsize;
        var initial = last*ps;
        var len = series.datapoints.points.length;
        for (var j = initial; j < len; j += ps) {
            if ((!series.lines.steps && series.datapoints.points[initial] != null && series.datapoints.points[j] == null)
                || series.datapoints.points[j] > posX) {
                return Math.max(j - ps,  0)/ps;
            }
        }
        return j/ps - 1;
    }


    getHoverInfo (seriesList, pos) {
        var i, series, value, hoverIndex, hoverDistance, pointTime, minDistance, minTime, hoverData;
        var last_value = 0;
        var results = [{
            seriesHover: []
        }];
        if (this.ctx.settings.comparisonEnabled) {
            results.push({
                seriesHover: []
            });
            var minDistanceHistorical, minTimeHistorical;
        }
        for (i = 0; i < seriesList.length; i++) {
            series = seriesList[i];
            var posx;
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
                    if (this.ctx.tooltipIndividual || !this.ctx.tooltipCumulative) {
                        value = series.data[hoverIndex][1];
                    } else {
                        last_value += series.data[hoverIndex][1];
                        value = last_value;
                    }
                } else {
                    value = series.data[hoverIndex][1];
                }

                if (series.stack || (series.curvedLines && series.curvedLines.apply)) {
                    hoverIndex = this.findHoverIndexFromDataPoints(posx, series, hoverIndex);
                }
                if (!this.ctx.hideZeros || value) {
                    hoverData = {
                        value: value,
                        hoverIndex: hoverIndex,
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

    pieDataRendered() {
        for (var i = 0; i < this.ctx.pieTargetData.length; i++) {
            var value = this.ctx.pieTargetData[i] ? this.ctx.pieTargetData[i] : 0;
            this.ctx.pieRenderedData[i] = value;
            if (!this.pieData[i].data[0]) {
                this.pieData[i].data[0] = [0,0];
            }
            this.pieData[i].data[0][1] = value;
        }
    }

    nextPieDataAnimation(start) {
        if (start) {
            this.finishPieDataAnimation();
            this.ctx.pieAnimationStartTime = this.ctx.pieAnimationLastTime = Date.now();
            for (var i = 0;  i < this.subscription.data.length; i++) {
                this.ctx.pieTargetData[i] = (this.subscription.data[i].data && this.subscription.data[i].data[0])
                    ? this.subscription.data[i].data[0][1] : 0;
            }
        }
        if (this.ctx.pieAnimationCaf) {
            this.ctx.pieAnimationCaf();
            this.ctx.pieAnimationCaf = null;
        }
        var self = this;
        this.ctx.pieAnimationCaf = this.ctx.$scope.tbRaf(
            function () {
                self.onPieDataAnimation();
            }
        );
    }

    onPieDataAnimation() {
        var time = Date.now();
        var elapsed = time - this.ctx.pieAnimationLastTime;//this.ctx.pieAnimationStartTime;
        var progress = (time - this.ctx.pieAnimationStartTime) / this.ctx.pieDataAnimationDuration;
        if (progress >= 1) {
            this.finishPieDataAnimation();
        } else {
            if (elapsed >= 40) {
                for (var i = 0; i < this.ctx.pieTargetData.length; i++) {
                    var prevValue = this.ctx.pieRenderedData[i];
                    var targetValue = this.ctx.pieTargetData[i];
                    var value = prevValue + (targetValue - prevValue) * progress;
                    if (!this.pieData[i].data[0]) {
                        this.pieData[i].data[0] = [0,0];
                    }
                    this.pieData[i].data[0][1] = value;
                }
                this.ctx.plot.setData(this.pieData);
                this.ctx.plot.draw();
                this.ctx.pieAnimationLastTime = time;
            }
            this.nextPieDataAnimation(false);
        }
    }

    finishPieDataAnimation() {
        this.pieDataRendered();
        this.ctx.plot.setData(this.pieData);
        this.ctx.plot.draw();
    }

    onPieSliceClick($event, item) {
        var descriptors = this.ctx.actionsApi.getActionDescriptors('sliceClick');
        if ($event && descriptors.length) {
            $event.stopPropagation();
            var entityInfo = this.ctx.actionsApi.getActiveEntityInfo();
            var entityId = entityInfo ? entityInfo.entityId : null;
            var entityName = entityInfo ? entityInfo.entityName : null;
            var entityLabel = entityInfo && entityInfo.entityLabel ? entityInfo.entityLabel : null;
            this.ctx.actionsApi.handleWidgetAction($event, descriptors[0], entityId, entityName, item, entityLabel);
        }
    }
}

const chartSettingsSchemaForComparison = {
    "schema": {
        "title": "Comparison Settings",
        "type": "object",
        "properties": {
            "comparisonEnabled": {
                "title": "Enable comparison",
                "type": "boolean",
                "default": false
            },
            "timeForComparison": {
                "title": "Time to show historical data",
                "type": "string",
                "default": "months"
            },
            "xaxisSecond": {
                "title": "Second X axis",
                "type": "object",
                "properties": {
                    "axisPosition": {
                        "title": "Axis position",
                        "type": "string",
                        "default": "top"
                    },
                    "showLabels": {
                        "title": "Show labels",
                        "type": "boolean",
                        "default": true
                    },
                    "title": {
                        "title": "Axis title",
                        "type": "string",
                        "default": null
                    }
                }
            }
        },
        "required": []
    },
    "form": [
        "comparisonEnabled",
        {
            "key": "timeForComparison",
            "type": "rc-select",
            "multiple": false,
            "items": [
                {
                    "value": "days",
                    "label": "Day ago"
                },
                {
                    "value": "weeks",
                    "label": "Week ago"
                },
                {
                    "value": "months",
                    "label": "Month ago (default)"
                },
                {
                    "value": "years",
                    "label": "Year ago"
                }
            ]
        },
        {
            "key": "xaxisSecond",
            "items": [
                {
                    "key": "xaxisSecond.axisPosition",
                    "type": "rc-select",
                    "multiple": false,
                    "items": [
                        {
                            "value": "top",
                            "label": "Top (default)"
                        },
                        {
                            "value": "bottom",
                            "label": "Bottom"
                        }
                    ]
                },
                "xaxisSecond.showLabels",
                "xaxisSecond.title",
            ]
        }
    ]
};

/* eslint-enable angular/angularelement */
