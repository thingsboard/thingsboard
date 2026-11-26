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
  tooltipStates.set(tooltip, {processor, data});
  if (tooltip.isOpen()) {
    renderTooltip(tooltip, dsData);
    if (tooltip.getElement()) {
      bindTooltipActions(map, tooltip, settings, data);
    }
  }
}

const renderTooltip = (tooltip: L.Popup, dsData: FormattedData<TbMapDatasource>[]): void => {
  const state = tooltipStates.get(tooltip);
  if (state) {
    tooltip.setContent(processTooltipTemplate(state.processor.processPattern(state.data, dsData)));
  }
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
            map.dataItemClick($event, action, data);
            return false;
          };
        }
      }
    }
  );
}
