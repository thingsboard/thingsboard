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

import L from 'leaflet';
import { GenericFunction, ShowTooltipAction, WidgetToolipSettings } from './map-models';
import { Datasource, FormattedData } from '@app/shared/models/widget.models';
import { fillDataPattern, isDefinedAndNotNull, isString, processDataPattern, safeExecuteTbFunction } from '@core/utils';
import { parseWithTranslation } from '@home/components/widget/lib/maps-legacy/common-maps-utils';
import { CompiledTbFunction } from '@shared/models/js-function.models';

export function createTooltip(target: L.Layer,
                              settings: Partial<WidgetToolipSettings>,
                              datasource: Datasource,
                              autoClose = false,
                              showTooltipAction = ShowTooltipAction.click,
                              content?: string | HTMLElement
): L.Popup {
    const popup = L.popup();
    popup.setContent(content);
    target.bindPopup(popup, { autoClose, closeOnClick: false });
    if (showTooltipAction === ShowTooltipAction.hover) {
        target.off('click');
        target.on('mouseover', () => {
            target.openPopup();
        });
        target.on('mousemove', (e) => {
            // @ts-ignore
            popup.setLatLng(e.latlng);
        });
        target.on('mouseout', () => {
            target.closePopup();
        });
    }
    target.on('popupopen', () => {
      bindPopupActions(popup, settings, datasource);
      (target as any)._popup._closeButton.addEventListener('click', (event: Event) => {
        event.preventDefault();
      });
    });
    return popup;
}

export function bindPopupActions(popup: L.Popup, settings: Partial<WidgetToolipSettings>,
                                 datasource: Datasource) {
  const actions = popup.getElement().getElementsByClassName('tb-custom-action');
  Array.from(actions).forEach(
    (element: HTMLElement) => {
      const actionName = element.getAttribute('data-action-name');
      if (element && settings.tooltipAction[actionName]) {
        element.onclick = ($event) =>
        {
          settings.tooltipAction[actionName]($event, datasource);
          return false;
        };
      }
    });
}

export function isCutPolygon(data): boolean {
  if (data.length > 1 && Array.isArray(data[0]) && (Array.isArray(data[0][0]) || data[0][0] instanceof L.LatLng)) {
    return true;
  }
  return false;
}

export function isJSON(data: string): boolean {
  try {
    const parseData = JSON.parse(data);
    return !Array.isArray(parseData);
  } catch (e) {
    return false;
  }
}

export interface LabelSettings {
  showLabel: boolean;
  useLabelFunction: boolean;
  parsedLabelFunction: CompiledTbFunction<GenericFunction>;
  label: string;
}

export function entitiesParseName(entities: FormattedData[], labelSettings: LabelSettings):  FormattedData[] {
  const div = document.createElement('div');
  for (const entity of entities) {
    if (labelSettings?.showLabel) {
      const pattern = labelSettings.useLabelFunction ? safeExecuteTbFunction(labelSettings.parsedLabelFunction,
        [entity, entities, entity.dsIndex]) : labelSettings.label;
      const markerLabelText = parseWithTranslation.prepareProcessPattern(pattern, true);
      const replaceInfoLabelMarker = processDataPattern(pattern, entity);
      div.innerHTML = fillDataPattern(markerLabelText, replaceInfoLabelMarker, entity);
      entity.entityParseName = div.textContent || div.innerText || '';
    } else {
      entity.entityParseName = entity.entityName;
    }
  }
  return entities;
}

export const isValidLatitude = (latitude: any): boolean =>
  isDefinedAndNotNull(latitude) &&
  !isString(latitude) &&
  !isNaN(latitude) && isFinite(latitude) && Math.abs(latitude) <= 90;

export const isValidLongitude = (longitude: any): boolean =>
  isDefinedAndNotNull(longitude) &&
  !isString(longitude) &&
  !isNaN(longitude) && isFinite(longitude) && Math.abs(longitude) <= 180;

export const isValidLatLng = (latitude: any, longitude: any): boolean =>
  isValidLatitude(latitude) && isValidLongitude(longitude);
