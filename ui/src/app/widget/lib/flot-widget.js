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

/* eslint-disable angular/angularelement */
export default class TbFlot {
    constructor(ctx, chartType) {

        this.ctx = ctx;
        this.chartType = chartType || 'line';

        var colors = [];
        for (var i in ctx.data) {
            var series = ctx.data[i];
            series.label = series.dataKey.label;
            colors.push(series.dataKey.color);
            var keySettings = series.dataKey.settings;

            series.lines = {
                fill: keySettings.fillLines || false,
                show: keySettings.showLines || true
            };

            series.points = {
                show: false,
                radius: 8
            };
            if (keySettings.showPoints === true) {
                series.points.show = true;
                series.points.lineWidth = 5;
                series.points.radius = 3;
            }

            var lineColor = tinycolor(series.dataKey.color);
            lineColor.setAlpha(.75);

            series.highlightColor = lineColor.toRgbString();

        }

        var tbFlot = this;

        ctx.tooltip = $('#flot-series-tooltip');
        if (ctx.tooltip.length === 0) {
            ctx.tooltip = $("<div id=flot-series-tooltip' class='flot-mouse-value'></div>");
            ctx.tooltip.css({
                fontSize: "12px",
                fontFamily: "Roboto",
                lineHeight: "24px",
                opacity: "1",
                backgroundColor: "rgba(0,0,0,0.7)",
                color: "#fff",
                position: "absolute",
                display: "none",
                zIndex: "100",
                padding: "2px 8px",
                borderRadius: "4px"
            }).appendTo("body");
        }

        ctx.tooltipFormatter = function(item) {
            var label = item.series.label;
            var color = item.series.color;
            var content = '';
            if (tbFlot.chartType === 'line') {
                var timestamp = parseInt(item.datapoint[0]);
                var date = moment(timestamp).format('YYYY-MM-DD HH:mm:ss');
                content += '<b>' + date + '</b></br>';
            }
            var lineSpan = $('<span></span>');
            lineSpan.css({
                backgroundColor: color,
                width: "20px",
                height: "3px",
                display: "inline-block",
                verticalAlign: "middle",
                marginRight: "5px"
            });
            content += lineSpan.prop('outerHTML');

            var labelSpan = $('<span>' + label + ':</span>');
            labelSpan.css({
                marginRight: "10px"
            });
            content += labelSpan.prop('outerHTML');
            var value = tbFlot.chartType === 'line' ? item.datapoint[1] : item.datapoint[1][0][1];
            content += ' <b>' + value.toFixed(ctx.trackDecimals);
            if (settings.units) {
                content += ' ' + settings.units;
            }
            if (tbFlot.chartType === 'pie') {
                content += ' (' + Math.round(item.series.percent) + ' %)';
            }
            content += '</b>';
            return content;
        };

        var settings = ctx.settings;
        ctx.trackDecimals = angular.isDefined(settings.decimals) ? settings.decimals : 1;

        var font = {
            color: settings.fontColor || "#545454",
            size: settings.fontSize || 10,
            family: "Roboto"
        };

        var options = {
            colors: colors,
            title: null,
            subtitle: null,
            shadowSize: settings.shadowSize || 4,
            HtmlText: false,
            grid: {
                hoverable: true,
                mouseActiveRadius: 10,
                autoHighlight: true
            },
            selection : { mode : ctx.isMobile ? null : 'x' },
            legend : {
                show: true,
                position : 'nw',
                labelBoxBorderColor: '#CCCCCC',
                backgroundColor : '#F0F0F0',
                backgroundOpacity: 0.85,
                font: angular.copy(font)
            }
        };
        if (settings.legend) {
            options.legend.show = settings.legend.show !== false;
            options.legend.position = settings.legend.position || 'nw';
            options.legend.labelBoxBorderColor = settings.legend.labelBoxBorderColor || null;
            options.legend.backgroundColor = settings.legend.backgroundColor || null;
            options.legend.backgroundOpacity = angular.isDefined(settings.legend.backgroundOpacity) ?
                settings.legend.backgroundOpacity : 0.85;
        }

        if (this.chartType === 'line') {
            options.xaxis = {
                mode: 'time',
                timezone: 'browser',
                font: angular.copy(font),
                labelFont: angular.copy(font)
            };
            options.yaxis = {
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
            if (settings.yaxis) {
                if (settings.yaxis.showLabels === false) {
                    options.yaxis.tickFormatter = function() {
                        return '';
                    };
                }
                options.yaxis.font.color = settings.yaxis.color || options.yaxis.font.color;
                options.yaxis.label = settings.yaxis.title || null;
                options.yaxis.labelFont.color = options.yaxis.font.color;
                options.yaxis.labelFont.size = options.yaxis.font.size+2;
                options.yaxis.labelFont.weight = "bold";
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
                    options.yaxis.tickLength = 0;
                }
            }

            options.xaxis.min = ctx.timeWindow.minTime;
            options.xaxis.max = ctx.timeWindow.maxTime;
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
                    return "<div class='pie-label'>" + label + "<br/>" + Math.round(series.percent) + "%</div>";
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

        if (this.chartType === 'pie' && this.ctx.animatedPie) {
            this.ctx.pieDataAnimationDuration = 250;
            this.ctx.pieData = angular.copy(this.ctx.data);
            this.ctx.pieRenderedData = [];
            this.ctx.pieTargetData = [];
            for (i in this.ctx.data) {
                this.ctx.pieTargetData[i] = (this.ctx.data[i].data && this.ctx.data[i].data[0])
                    ? this.ctx.data[i].data[0][1] : 0;
            }
            this.pieDataRendered();
            this.ctx.plot = $.plot(this.ctx.$container, this.ctx.pieData, this.options);
        } else {
            this.ctx.plot = $.plot(this.ctx.$container, this.ctx.data, this.options);
        }
        this.checkMouseEvents();
    }

    update() {
        if (!this.isMouseInteraction) {
            if (this.chartType === 'line') {
                this.ctx.plot.getOptions().xaxes[0].min = this.ctx.timeWindow.minTime;
                this.ctx.plot.getOptions().xaxes[0].max = this.ctx.timeWindow.maxTime;
            }
            if (this.chartType === 'line') {
                this.ctx.plot.setData(this.ctx.data);
                this.ctx.plot.setupGrid();
                this.ctx.plot.draw();
            } else if (this.chartType === 'pie') {
                if (this.ctx.animatedPie) {
                    this.nextPieDataAnimation(true);
                } else {
                    this.ctx.plot.setData(this.ctx.data);
                    this.ctx.plot.draw();
                }
            }
        }
    }

    pieDataRendered() {
        for (var i in this.ctx.pieTargetData) {
            var value = this.ctx.pieTargetData[i] ? this.ctx.pieTargetData[i] : 0;
            this.ctx.pieRenderedData[i] = value;
            if (!this.ctx.pieData[i].data[0]) {
                this.ctx.pieData[i].data[0] = [0,0];
            }
            this.ctx.pieData[i].data[0][1] = value;
        }
    }

    nextPieDataAnimation(start) {
        if (start) {
            this.finishPieDataAnimation();
            this.ctx.pieAnimationStartTime = this.ctx.pieAnimationLastTime = Date.now();
            for (var i in this.ctx.data) {
                this.ctx.pieTargetData[i] = (this.ctx.data[i].data && this.ctx.data[i].data[0])
                    ? this.ctx.data[i].data[0][1] : 0;
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
                for (var i in this.ctx.pieTargetData) {
                    var prevValue = this.ctx.pieRenderedData[i];
                    var targetValue = this.ctx.pieTargetData[i];
                    var value = prevValue + (targetValue - prevValue) * progress;
                    if (!this.ctx.pieData[i].data[0]) {
                        this.ctx.pieData[i].data[0] = [0,0];
                    }
                    this.ctx.pieData[i].data[0][1] = value;
                }
                this.ctx.plot.setData(this.ctx.pieData);
                this.ctx.plot.draw();
                this.ctx.pieAnimationLastTime = time;
            }
            this.nextPieDataAnimation(false);
        }
    }

    finishPieDataAnimation() {
        this.pieDataRendered();
        this.ctx.plot.setData(this.ctx.pieData);
        this.ctx.plot.draw();
    }

    resize() {
        this.ctx.plot.resize();
        if (this.chartType === 'line') {
            this.ctx.plot.setupGrid();
        }
        this.ctx.plot.draw();
    }

    checkMouseEvents() {
        if (this.ctx.isMobile || this.ctx.isEdit) {
            this.disableMouseEvents();
        } else if (!this.ctx.isEdit) {
            this.enableMouseEvents();
        }
    }

    enableMouseEvents() {
        this.ctx.$container.css('pointer-events','');
        this.ctx.$container.addClass('mouse-events');
        this.options.selection = { mode : 'x' };

        var tbFlot = this;

        if (!this.flotHoverHandler) {
            this.flotHoverHandler =  function (event, pos, item) {
                if (item) {
                    var pageX = item.pageX || pos.pageX;
                    var pageY = item.pageY || pos.pageY;
                    tbFlot.ctx.tooltip.html(tbFlot.ctx.tooltipFormatter(item))
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
                } else {
                    tbFlot.ctx.tooltip.stop(true);
                    tbFlot.ctx.tooltip.hide();
                }
            };
            this.ctx.$container.bind('plothover', this.flotHoverHandler);
        }

        if (!this.flotSelectHandler) {
            this.flotSelectHandler =  function (event, ranges) {
                tbFlot.ctx.plot.clearSelection();
                tbFlot.ctx.timewindowFunctions.onUpdateTimewindow(ranges.xaxis.from, ranges.xaxis.to);
            };
            this.ctx.$container.bind('plotselected', this.flotSelectHandler);
        }
        if (!this.dblclickHandler) {
            this.dblclickHandler =  function () {
                tbFlot.ctx.timewindowFunctions.onResetTimewindow();
            };
            this.ctx.$container.bind('dblclick', this.dblclickHandler);
        }
        if (!this.mousedownHandler) {
            this.mousedownHandler =  function () {
                tbFlot.isMouseInteraction = true;
            };
            this.ctx.$container.bind('mousedown', this.mousedownHandler);
        }
        if (!this.mouseupHandler) {
            this.mouseupHandler =  function () {
                tbFlot.isMouseInteraction = false;
            };
            this.ctx.$container.bind('mouseup', this.mouseupHandler);
        }
        if (!this.mouseleaveHandler) {
            this.mouseleaveHandler =  function () {
                tbFlot.ctx.tooltip.stop(true);
                tbFlot.ctx.tooltip.hide();
                tbFlot.isMouseInteraction = false;
            };
            this.ctx.$container.bind('mouseleave', this.mouseleaveHandler);
        }
    }

    disableMouseEvents() {
        this.ctx.$container.css('pointer-events','none');
        this.ctx.$container.removeClass('mouse-events');
        this.options.selection = { mode : null };

        if (this.flotHoverHandler) {
            this.ctx.$container.unbind('plothover', this.flotHoverHandler);
            this.flotHoverHandler = null;
        }

        if (this.flotSelectHandler) {
            this.ctx.$container.unbind('plotselected', this.flotSelectHandler);
            this.flotSelectHandler = null;
        }
        if (this.dblclickHandler) {
            this.ctx.$container.unbind('dblclick', this.dblclickHandler);
            this.dblclickHandler = null;
        }
        if (this.mousedownHandler) {
            this.ctx.$container.unbind('mousedown', this.mousedownHandler);
            this.mousedownHandler = null;
        }
        if (this.mouseupHandler) {
            this.ctx.$container.unbind('mouseup', this.mouseupHandler);
            this.mouseupHandler = null;
        }
        if (this.mouseleaveHandler) {
            this.ctx.$container.unbind('mouseleave', this.mouseleaveHandler);
            this.mouseleaveHandler = null;
        }
    }
}

/* eslint-enable angular/angularelement */