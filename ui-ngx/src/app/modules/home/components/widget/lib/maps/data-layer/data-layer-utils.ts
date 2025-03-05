///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  DataLayerTooltipSettings,
  DataLayerTooltipTrigger, processTooltipTemplate,
  TbMapDatasource
} from '@shared/models/widget/maps/map.models';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { FormattedData } from '@shared/models/widget.models';
import L from 'leaflet';
import { DataLayerPatternProcessor } from '@home/components/widget/lib/maps/data-layer/map-data-layer';

export const createTooltip = (map: TbMap<any>,
                              layer: L.Layer,
                              settings: DataLayerTooltipSettings,
                              data: FormattedData<TbMapDatasource>,
                              canOpen: () => boolean): L.Popup => {
  const tooltip = L.popup();
  layer.bindPopup(tooltip, {autoClose: settings.autoclose, closeOnClick: false});
  layer.off('click');
  if (settings.trigger === DataLayerTooltipTrigger.click) {
    layer.on('click', () => {
      if (tooltip.isOpen()) {
        layer.closePopup();
      } else if (canOpen()) {
        layer.openPopup();
      }
    });
  } else if (settings.trigger === DataLayerTooltipTrigger.hover) {
    layer.on('mouseover', () => {
      if (canOpen()) {
        layer.openPopup();
      }
    });
    layer.on('mousemove', (e) => {
      tooltip.setLatLng(e.latlng);
    });
    layer.on('mouseout', () => {
      layer.closePopup();
    });
  }
  layer.on('popupopen', () => {
    bindTooltipActions(map, tooltip, settings, data);
    (layer as any)._popup._closeButton.addEventListener('click', (event: Event) => {
      event.preventDefault();
    });
  });
  return tooltip;
}

export const updateTooltip = (map: TbMap<any>,
                              tooltip: L.Popup,
                              settings: DataLayerTooltipSettings,
                              processor: DataLayerPatternProcessor,
                              data: FormattedData<TbMapDatasource>,
                              dsData: FormattedData<TbMapDatasource>[]): void => {
  let tooltipTemplate = processor.processPattern(data, dsData);
  tooltipTemplate = processTooltipTemplate(tooltipTemplate);
  tooltip.setContent(tooltipTemplate);
  if (tooltip.isOpen() && tooltip.getElement()) {
    bindTooltipActions(map, tooltip, settings, data);
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
