// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1

export interface HomeChartTooltipRow {
  color: string;
  name: string;
  value: string;
}

const escapeHtml = (text: string): string =>
  text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

export const homeChartTooltipHtml = (header: string, rows: HomeChartTooltipRow[]): string => {
  const items = rows.map(row => '<span style="display: flex; align-items: center; gap: 6px; color: rgba(255, 255, 255, 0.8);">' +
    `<i style="flex: none; width: 8px; height: 8px; border-radius: 50%; background: ${row.color}; ` +
    'box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.6);"></i>' +
    `${escapeHtml(row.name)}<b style="margin-left: auto; padding-left: 8px; font-weight: 500; color: #fff; ` +
    `font-variant-numeric: tabular-nums;">${escapeHtml(row.value)}</b></span>`).join('');
  return '<div style="display: flex; flex-direction: column; gap: 2px; font: 400 12px/16px Roboto, sans-serif; letter-spacing: 0.2px; ' +
    `color: #fff; white-space: nowrap;"><b style="font-weight: 500;">${escapeHtml(header)}</b>${items}</div>`;
};

export const homeChartTooltipOptions = {
  trigger: 'axis' as const,
  confine: true,
  axisPointer: {type: 'line' as const, lineStyle: {color: 'rgba(0, 0, 0, 0.38)', type: 'dashed' as const, width: 1}},
  position: (point: number[], _params: unknown, _dom: unknown, _rect: unknown, size: {contentSize: number[]; viewSize: number[]}) =>
    [point[0] > size.viewSize[0] * 0.6 ? point[0] - 10 - size.contentSize[0] : point[0] + 10, 0],
  padding: [6, 8],
  backgroundColor: 'rgba(0, 0, 0, 0.87)',
  borderWidth: 0,
  extraCssText: 'border-radius: 6px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.16); min-width: 96px; box-sizing: border-box;'
};

export const homeChartHoverSymbol = (color: string) => ({
  symbol: 'circle',
  symbolSize: 8,
  itemStyle: {color, borderColor: '#fff', borderWidth: 2},
  emphasis: {scale: false}
});
