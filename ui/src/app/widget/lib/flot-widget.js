/*
 * Copyright © 2016-2017 The Thingsboard Authors
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
import 'flot.curvedlines/curvedLines';

/* eslint-disable angular/angularelement */
export default class TbFlot {
    constructor(ctx, chartType) {

        this.ctx = ctx;
        this.chartType = chartType || 'line';
        var settings = ctx.settings;

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

        function seriesInfoDiv(label, color, value, units, trackDecimals, active, percent, valueFormatFunction) {
            var divElement = $('<div></div>');
            divElement.css({
                display: "flex",
                alignItems: "center",
                justifyContent: "flex-start"
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
                valueContent = valueFormatFunction(value);
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

        if (this.chartType === 'pie') {
            ctx.tooltipFormatter = function(item) {
                var units = item.series.dataKey.units && item.series.dataKey.units.length ? item.series.dataKey.units : tbFlot.ctx.trackUnits;
                var decimals = angular.isDefined(item.series.dataKey.decimals) ? item.series.dataKey.decimals : tbFlot.ctx.trackDecimals;
                var divElement = seriesInfoDiv(item.series.dataKey.label, item.series.dataKey.color,
                    item.datapoint[1][0][1], units, decimals, true, item.series.percent, item.series.dataKey.tooltipValueFormatFunction);
                return divElement.prop('outerHTML');
            };
        } else {
            ctx.tooltipFormatter = function(hoverInfo, seriesIndex) {
                var content = '';
                var timestamp = parseInt(hoverInfo.time);
                var date = moment(timestamp).format('YYYY-MM-DD HH:mm:ss');
                var dateDiv = $('<div>' + date + '</div>');
                dateDiv.css({
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    padding: "4px",
                    fontWeight: "700"
                });
                content += dateDiv.prop('outerHTML');
                for (var i = 0; i < hoverInfo.seriesHover.length; i++) {
                    var seriesHoverInfo = hoverInfo.seriesHover[i];
                    if (tbFlot.ctx.tooltipIndividual && seriesHoverInfo.index !== seriesIndex) {
                        continue;
                    }
                    var units = seriesHoverInfo.units && seriesHoverInfo.units.length ? seriesHoverInfo.units : tbFlot.ctx.trackUnits;
                    var decimals = angular.isDefined(seriesHoverInfo.decimals) ? seriesHoverInfo.decimals : tbFlot.ctx.trackDecimals;
                    var divElement = seriesInfoDiv(seriesHoverInfo.label, seriesHoverInfo.color,
                        seriesHoverInfo.value, units, decimals, seriesHoverInfo.index === seriesIndex, null, seriesHoverInfo.tooltipValueFormatFunction);
                    content += divElement.prop('outerHTML');
                }
                return content;
            };
        }

        ctx.trackDecimals = ctx.decimals;

        ctx.trackUnits = ctx.units;

        ctx.tooltipIndividual = this.chartType === 'pie' || (angular.isDefined(settings.tooltipIndividual) ? settings.tooltipIndividual : false);
        ctx.tooltipCumulative = angular.isDefined(settings.tooltipCumulative) ? settings.tooltipCumulative : false;

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
                autoHighlight: ctx.tooltipIndividual === true
            },
            selection : { mode : ctx.isMobile ? null : 'x' },
            legend : {
                show: false
            }
        };

        if (this.chartType === 'line' || this.chartType === 'bar' || this.chartType === 'state') {
            options.xaxis = {
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
                if (settings.xaxis.showLabels === false) {
                    options.xaxis.tickFormatter = function() {
                        return '';
                    };
                }
                options.xaxis.font.color = settings.xaxis.color || options.xaxis.font.color;
                options.xaxis.label = settings.xaxis.title || null;
                options.xaxis.labelFont.color = options.xaxis.font.color;
                options.xaxis.labelFont.size = options.xaxis.font.size+2;
                options.xaxis.labelFont.weight = "bold";
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
                formatted += ' ' + this.tickUnits;
                return formatted;
            }

            this.yaxis.tickFormatter = ctx.yAxisTickFormatter;

            if (settings.yaxis) {
                this.yaxis.font.color = settings.yaxis.color || this.yaxis.font.color;
                this.yaxis.min = angular.isDefined(settings.yaxis.min) ? settings.yaxis.min : null;
                this.yaxis.max = angular.isDefined(settings.yaxis.max) ? settings.yaxis.max : null;
                this.yaxis.label = settings.yaxis.title || null;
                this.yaxis.labelFont.color = this.yaxis.font.color;
                this.yaxis.labelFont.size = this.yaxis.font.size+2;
                this.yaxis.labelFont.weight = "bold";
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
                    options.xaxis.tickLength = 0;
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

            options.crosshair = {
                mode: 'x'
            }

            options.series = {
                stack: settings.stack === true
            }

            if (this.chartType === 'line' && settings.smoothLines) {
                options.series.curvedLines = {
                    active: true,
                    monotonicFit: true
                }
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
                        fill: 0.9
                }
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
                        show: settings.showLabels === true
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
            if (settings.stroke) {
                options.series.pie.stroke.color = settings.stroke.color || '#fff';
                options.series.pie.stroke.width = settings.stroke.width || 0;
            }

            if (options.series.pie.label.show) {
                options.series.pie.label.formatter = function (label, series) {
                    return "<div class='pie-label'>" + series.dataKey.label + "<br/>" + Math.round(series.percent) + "%</div>";
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

        var tooltipValueFormatFunction = null;
        if (this.ctx.settings.tooltipValueFormatter && this.ctx.settings.tooltipValueFormatter.length) {
            try {
                tooltipValueFormatFunction = new Function('value', this.ctx.settings.tooltipValueFormatter);
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
                    series.dataKey.tooltipValueFormatFunction = new Function('value', keySettings.tooltipValueFormatter);
                } catch (e) {
                    series.dataKey.tooltipValueFormatFunction = tooltipValueFormatFunction;
                }
            }
            series.lines = {
                fill: keySettings.fillLines === true,
                show: this.chartType === 'line' ? keySettings.showLines !== false : keySettings.showLines === true
            };

            if (angular.isDefined(keySettings.lineWidth)) {
                series.lines.lineWidth = keySettings.lineWidth;
            }

            series.points = {
                show: false,
                radius: 8
            };
            if (keySettings.showPoints === true) {
                series.points.show = true;
                series.points.lineWidth = 5;
                series.points.radius = 3;
            }

            if (this.chartType === 'line' && this.ctx.settings.smoothLines && !series.points.show) {
                series.curvedLines = {
                    apply: true
                }
            }

            var lineColor = tinycolor(series.dataKey.color);
            lineColor.setAlpha(.75);

            series.highlightColor = lineColor.toRgbString();

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
                yaxis.hidden = false;
            }
        }

        this.options.colors = colors;
        this.options.yaxes = angular.copy(this.yaxes);
        if (this.chartType === 'line' || this.chartType === 'bar' || this.chartType === 'state') {
            if (this.chartType === 'bar') {
                this.options.series.bars.barWidth = this.subscription.timeWindow.interval * 0.6;
            }
            this.options.xaxis.min = this.subscription.timeWindow.minTime;
            this.options.xaxis.max = this.subscription.timeWindow.maxTime;
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
            this.ctx.plot = $.plot(this.$element, this.pieData, this.options);
        } else {
            this.ctx.plot = $.plot(this.$element, this.subscription.data, this.options);
        }
    }

    createYAxis(keySettings, units) {
        var yaxis = angular.copy(this.yaxis);

        var label = keySettings.axisTitle && keySettings.axisTitle.length ? keySettings.axisTitle : yaxis.label;
        var tickDecimals = angular.isDefined(keySettings.axisTickDecimals) ? keySettings.axisTickDecimals : 0;
        var position = keySettings.axisPosition && keySettings.axisPosition.length ? keySettings.axisPosition : "left";

        var min = angular.isDefined(keySettings.axisMin) ? keySettings.axisMin : yaxis.min;
        var max = angular.isDefined(keySettings.axisMax) ? keySettings.axisMax : yaxis.max;

        yaxis.label = label;
        yaxis.min = min;
        yaxis.max = max;
        yaxis.tickUnits = units;
        yaxis.tickDecimals = tickDecimals;
        yaxis.alignTicksWithAxis = position == "right" ? 1 : null;
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
                                var newIndex = -1;
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

                    this.options.xaxis.min = this.subscription.timeWindow.minTime;
                    this.options.xaxis.max = this.subscription.timeWindow.maxTime;
                    if (this.chartType === 'bar') {
                        this.options.series.bars.barWidth = this.subscription.timeWindow.interval * 0.6;
                    }

                    if (axisVisibilityChanged) {
                        this.redrawPlot();
                    } else {
                        this.ctx.plot.getOptions().xaxes[0].min = this.subscription.timeWindow.minTime;
                        this.ctx.plot.getOptions().xaxes[0].max = this.subscription.timeWindow.maxTime;
                        if (this.chartType === 'bar') {
                            this.ctx.plot.getOptions().series.bars.barWidth = this.subscription.timeWindow.interval * 0.6;
                        }
                        this.ctx.plot.setData(this.subscription.data);
                        this.ctx.plot.setupGrid();
                        this.ctx.plot.draw();
                    }
                } else if (this.chartType === 'pie') {
                    if (this.ctx.animatedPie) {
                        this.nextPieDataAnimation(true);
                    } else {
                        this.ctx.plot.setData(this.subscription.data);
                        this.ctx.plot.draw();
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

    resize() {
        if (this.ctx.plot) {
            this.ctx.plot.resize();
            if (this.chartType !== 'pie') {
                this.ctx.plot.setupGrid();
            }
            this.ctx.plot.draw();
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
                {
                    "key": "fontColor",
                    "type": "color"
                },
                "fontSize"
            ]
        }
    }

    static get settingsSchema() {
        return {
            "schema": {
                "type": "object",
                "title": "Settings",
                "properties": {
                    "stack": {
                        "title": "Stacking",
                        "type": "boolean",
                        "default": false
                    },
                    "smoothLines": {
                        "title": "Display smooth (curved) lines",
                        "type": "boolean",
                        "default": false
                    },
                    "shadowSize": {
                        "title": "Shadow size",
                        "type": "number",
                        "default": 4
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
                    },
                    "tooltipIndividual": {
                        "title": "Hover individual points",
                        "type": "boolean",
                        "default": false
                    },
                    "tooltipCumulative": {
                        "title": "Show cumulative values in stacking mode",
                        "type": "boolean",
                        "default": false
                    },
                    "tooltipValueFormatter": {
                        "title": "Tooltip value format function, f(value)",
                        "type": "string",
                        "default": ""
                    },
                    "grid": {
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
                    },
                    "xaxis": {
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
                            "titleAngle": {
                                "title": "Axis title's angle in degrees",
                                "type": "number",
                                "default": 0
                            },
                            "color": {
                                "title": "Ticks color",
                                "type": "string",
                                "default": null
                            }
                        }
                    },
                    "yaxis": {
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
                            "titleAngle": {
                                "title": "Axis title's angle in degrees",
                                "type": "number",
                                "default": 0
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
                            }
                        }
                    }
                },
                "required": []
            },
            "form": [
                "stack",
                "smoothLines",
                "shadowSize",
                {
                    "key": "fontColor",
                    "type": "color"
                },
                "fontSize",
                "tooltipIndividual",
                "tooltipCumulative",
                {
                    "key": "tooltipValueFormatter",
                    "type": "javascript"
                },
                {
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
                },
                {
                    "key": "xaxis",
                    "items": [
                        "xaxis.showLabels",
                        "xaxis.title",
                        "xaxis.titleAngle",
                        {
                            "key": "xaxis.color",
                            "type": "color"
                        }
                    ]
                },
                {
                    "key": "yaxis",
                    "items": [
                        "yaxis.min",
                        "yaxis.max",
                        "yaxis.showLabels",
                        "yaxis.title",
                        "yaxis.titleAngle",
                        {
                            "key": "yaxis.color",
                            "type": "color"
                        },
                        {
                            "key": "yaxis.ticksFormatter",
                            "type": "javascript"
                        }
                    ]
                }

            ]
        }
    }

    static get pieDatakeySettingsSchema() {
        return {}
    }

    static datakeySettingsSchema(defaultShowLines) {
        return {
                "schema": {
                "type": "object",
                    "title": "DataKeySettings",
                    "properties": {
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
                    "tooltipValueFormatter": {
                        "title": "Tooltip value format function, f(value)",
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
                        "default": 0
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
                "showLines",
                "fillLines",
                "showPoints",
                {
                    "key": "tooltipValueFormatter",
                    "type": "javascript"
                },
                "showSeparateAxis",
                "axisMin",
                "axisMax",
                "axisTitle",
                "axisTickDecimals",
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
                if (this.ctx.plot) {
                    this.ctx.plot.destroy();
                    if (this.chartType === 'pie' && this.ctx.animatedPie) {
                        this.ctx.plot = $.plot(this.$element, this.pieData, this.options);
                    } else {
                        this.ctx.plot = $.plot(this.$element, this.subscription.data, this.options);
                    }
                }
            }
        }
    }

    redrawPlot() {
        if (this.ctx.plot) {
            this.ctx.plot.destroy();
            if (this.chartType === 'pie' && this.ctx.animatedPie) {
                this.ctx.plot = $.plot(this.$element, this.pieData, this.options);
            } else {
                this.ctx.plot = $.plot(this.$element, this.subscription.data, this.options);
            }
        }
    }

    destroy() {
        if (this.ctx.plot) {
            this.ctx.plot.destroy();
            this.ctx.plot = null;
        }
    }

    enableMouseEvents() {
        this.$element.css('pointer-events','');
        this.$element.addClass('mouse-events');
        this.options.selection = { mode : 'x' };

        var tbFlot = this;

        if (!this.flotHoverHandler) {
            this.flotHoverHandler =  function (event, pos, item) {
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
                        if (angular.isNumber(hoverInfo.time)) {
                            hoverInfo.seriesHover.sort(function (a, b) {
                                return b.value - a.value;
                            });
                            tooltipHtml = tbFlot.ctx.tooltipFormatter(hoverInfo, item ? item.seriesIndex : -1);
                        }
                    }

                    if (tooltipHtml) {
                        tbFlot.ctx.tooltip.html(tooltipHtml)
                            .css({top: pageY+5, left: 0})
                            .fadeIn(200);

                        var windowWidth = $( window ).width();  //eslint-disable-line
                        var tooltipWidth = tbFlot.ctx.tooltip.width();
                        var left = pageX+5;
                        if (windowWidth - pageX < tooltipWidth + 50) {
                            left = pageX - tooltipWidth - 10;
                        }
                        tbFlot.ctx.tooltip.css({
                            left: left
                        });

                        if (multipleModeTooltip) {
                            for (var i = 0; i < hoverInfo.seriesHover.length; i++) {
                                var seriesHoverInfo = hoverInfo.seriesHover[i];
                                tbFlot.ctx.plot.highlight(seriesHoverInfo.index, seriesHoverInfo.hoverIndex);
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
                tbFlot.ctx.tooltip.stop(true);
                tbFlot.ctx.tooltip.hide();
                tbFlot.ctx.plot.unhighlight();
                tbFlot.isMouseInteraction = false;
            };
            this.$element.bind('mouseleave', this.mouseleaveHandler);
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
        var i, series, value, hoverIndex, hoverDistance, pointTime, minDistance, minTime;
        var last_value = 0;
        var results = {
            seriesHover: []
        };
        for (i = 0; i < seriesList.length; i++) {
            series = seriesList[i];
            hoverIndex = this.findHoverIndexFromData(pos.x, series);
            if (series.data[hoverIndex] && series.data[hoverIndex][0]) {
                hoverDistance = pos.x - series.data[hoverIndex][0];
                pointTime = series.data[hoverIndex][0];

                if (!minDistance
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
                    hoverIndex = this.findHoverIndexFromDataPoints(pos.x, series, hoverIndex);
                }
                results.seriesHover.push({
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
                });
            }
        }
        results.time = minTime;
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
}

/* eslint-enable angular/angularelement */