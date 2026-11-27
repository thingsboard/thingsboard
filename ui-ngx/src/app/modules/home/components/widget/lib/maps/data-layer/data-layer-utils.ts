// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  DataLayerTooltipSettings,
  DataLayerTooltipTrigger, processTooltipTemplate,
  TbMapDatasource
} from '@shared/models/widget/maps/map.models';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { FormattedData } from '@shared/models/widget.models';
import L from 'leaflet';
import { DataLayerPatternProcessor } from '@home/components/widget/lib/maps/data-layer/map-data-layer';

interface TooltipState {
  processor: DataLayerPatternProcessor;
  data: FormattedData<TbMapDatasource>;
  content?: string;
}

const tooltipStates = new WeakMap<L.Popup, TooltipState>();

export const createTooltip = (map: TbMap<any>,
                              layer: L.Layer,
                              settings: DataLayerTooltipSettings,
                              data: FormattedData<TbMapDatasource>,
                              canOpen: () => boolean): L.Popup => {
  const tooltip = L.popup({autoClose: settings.autoclose, closeOnClick: false});
  (tooltip as any)._source = layer;
  const open = (beforeOpen?: () => void) => {
    if ((tooltip as any)._prepareOpen((layer as any)._latlng)) {
      beforeOpen?.();
      renderTooltip(tooltip, map.getData());
      tooltip.openOn(map.getMap());
    }
  };
  layer.on('move', (e) => {
    tooltip.setLatLng((e as any).latlng);
  });
  layer.on('remove', () => {
    tooltip.close();
  });
  if (settings.trigger === DataLayerTooltipTrigger.click) {
    layer.on('click', (e) => {
      L.DomEvent.stop(e);
      if (tooltip.isOpen()) {
        tooltip.close();
      } else if (canOpen()) {
        open(() => map.deselectItem());
      }
    });
  } else if (settings.trigger === DataLayerTooltipTrigger.hover) {
    layer.on('mouseover', () => {
      if (canOpen()) {
        open();
      }
    });
    layer.on('mousemove', (e) => {
      tooltip.setLatLng(e.latlng);
    });
    layer.on('mouseout', () => {
      tooltip.close();
    });
  }
  layer.on('popupopen', () => {
    bindTooltipActions(map, tooltip, settings, tooltipStates.get(tooltip)?.data ?? data);
  });
  return tooltip;
}

export const updateTooltip = (map: TbMap<any>,
                              tooltip: L.Popup,
                              settings: DataLayerTooltipSettings,
                              processor: DataLayerPatternProcessor,
                              data: FormattedData<TbMapDatasource>,
                              dsData: FormattedData<TbMapDatasource>[]): void => {
  tooltipStates.set(tooltip, {processor, data, content: tooltipStates.get(tooltip)?.content});
  if (tooltip.isOpen()) {
    if (renderTooltip(tooltip, dsData) && tooltip.getElement()) {
      bindTooltipActions(map, tooltip, settings, data);
    }
  }
}

const renderTooltip = (tooltip: L.Popup, dsData: FormattedData<TbMapDatasource>[]): boolean => {
  const state = tooltipStates.get(tooltip);
  let changed = false;
  if (state) {
    const content = processTooltipTemplate(state.processor.processPattern(state.data, dsData));
    if (content !== state.content) {
      state.content = content;
      tooltip.setContent(content);
      changed = true;
    }
  }
  return changed;
}

const bindTooltipActions = (map: TbMap<any>, tooltip: L.Popup, settings: DataLayerTooltipSettings, data: FormattedData<TbMapDatasource>): void => {
  const actions = tooltip.getElement().getElementsByClassName('tb-custom-action');
  Array.from(actions).forEach(
    (element: HTMLElement) => {
      const actionName = element.getAttribute('data-action-name');
      if (settings?.tagActions) {
        const action = settings.tagActions.find(action => action.name === actionName);
        if (action) {
          element.onclick = ($event) =>
          {
            map.dataItemClick($event, action, tooltipStates.get(tooltip)?.data ?? data);
            return false;
          };
        }
      }
    }
  );
}
