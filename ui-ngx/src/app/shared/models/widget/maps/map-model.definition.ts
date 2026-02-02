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

import { EntityAliases, EntityAliasInfo, getEntityAliasId } from '@shared/models/alias.models';
import { FilterInfo, Filters, getFilterId } from '@shared/models/query/query.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { Datasource, DatasourceType, Widget } from '@shared/models/widget.models';
import {
  BaseMapSettings,
  MapDataLayerSettings,
  MapDataSourceSettings,
  mapDataSourceSettingsToDatasource,
  MapType
} from '@shared/models/widget/maps/map.models';
import { WidgetModelDefinition } from '@shared/models/widget/widget-model.definition';

interface AliasFilterPair {
  alias?: EntityAliasInfo,
  filter?: FilterInfo
}

interface MapDataLayerDsInfo extends AliasFilterPair {
  additionalDsInfo?: {[dsIndex: number]: AliasFilterPair}
}

type ExportDataSourceInfo = {[dataLayerIndex: number]: MapDataLayerDsInfo};

interface MapDatasourcesInfo {
  trips?: ExportDataSourceInfo;
  markers?: ExportDataSourceInfo;
  polygons?: ExportDataSourceInfo;
  circles?: ExportDataSourceInfo;
  additionalDataSources?: ExportDataSourceInfo;
}

export const MapModelDefinition: WidgetModelDefinition<MapDatasourcesInfo> = {
  testWidget(widget: Widget): boolean {
    if (widget?.config?.settings) {
      const settings = widget.config.settings;
      if (settings.mapType && [MapType.image, MapType.geoMap].includes(settings.mapType)) {
        if (settings.trips && Array.isArray(settings.trips)) {
          return true;
        }
        if (settings.markers && Array.isArray(settings.markers)) {
          return true;
        }
        if (settings.polygons && Array.isArray(settings.polygons)) {
          return true;
        }
        if (settings.circles && Array.isArray(settings.circles)) {
          return true;
        }
      }
    }
    return false;
  },
  prepareExportInfo(dashboard: Dashboard, widget: Widget): MapDatasourcesInfo {
    const settings: BaseMapSettings = widget.config.settings as BaseMapSettings;
    const info: MapDatasourcesInfo = {};
    if (settings.trips?.length) {
      info.trips = prepareExportDataSourcesInfo(dashboard, settings.trips);
    }
    if (settings.markers?.length) {
      info.markers = prepareExportDataSourcesInfo(dashboard, settings.markers);
    }
    if (settings.polygons?.length) {
      info.polygons = prepareExportDataSourcesInfo(dashboard, settings.polygons);
    }
    if (settings.circles?.length) {
      info.circles = prepareExportDataSourcesInfo(dashboard, settings.circles);
    }
    if (settings.additionalDataSources?.length) {
      info.additionalDataSources = prepareExportDataSourcesInfo(dashboard, settings.additionalDataSources);
    }
    return info;
  },
  updateFromExportInfo(widget: Widget, entityAliases: EntityAliases, filters: Filters, info: MapDatasourcesInfo): void {
    const settings: BaseMapSettings = widget.config.settings as BaseMapSettings;
    if (info?.trips) {
      updateMapDatasourceFromExportInfo(entityAliases, filters, settings.trips, info.trips);
    }
    if (info?.markers) {
      updateMapDatasourceFromExportInfo(entityAliases, filters, settings.markers, info.markers);
    }
    if (info?.polygons) {
      updateMapDatasourceFromExportInfo(entityAliases, filters, settings.polygons, info.polygons);
    }
    if (info?.circles) {
      updateMapDatasourceFromExportInfo(entityAliases, filters, settings.circles, info.circles);
    }
    if (info?.additionalDataSources) {
      updateMapDatasourceFromExportInfo(entityAliases, filters, settings.additionalDataSources, info.additionalDataSources);
    }
  },
  datasources(widget: Widget): Datasource[] {
    const settings: BaseMapSettings = widget.config.settings as BaseMapSettings;
    const datasources: Datasource[] = [];
    if (settings.trips?.length) {
      datasources.push(...getMapDataLayersDatasources(settings.trips));
    }
    if (settings.markers?.length) {
      datasources.push(...getMapDataLayersDatasources(settings.markers));
    }
    if (settings.polygons?.length) {
      datasources.push(...getMapDataLayersDatasources(settings.polygons));
    }
    if (settings.circles?.length) {
      datasources.push(...getMapDataLayersDatasources(settings.circles));
    }
    if (settings.additionalDataSources?.length) {
      datasources.push(...getMapDataLayersDatasources(settings.additionalDataSources));
    }
    return datasources;
  }
};

const updateMapDatasourceFromExportInfo = (entityAliases: EntityAliases,
                                                            filters: Filters, settings: MapDataLayerSettings[] | MapDataSourceSettings[],
                                                            info: MapDataLayerDsInfo | {[dsIndex: number]: AliasFilterPair}): void => {
  for (const dsIndexStr of Object.keys(info)) {
    const dsIndex = Number(dsIndexStr);
    const dsInfo = info[dsIndex];
    if (settings[dsIndex]) {
      if (settings[dsIndex].dsType === DatasourceType.entity) {
        if (settings[dsIndex].dsType === DatasourceType.entity) {
          if (dsInfo.alias) {
            settings[dsIndex].dsEntityAliasId = getEntityAliasId(entityAliases, dsInfo.alias);
          }
          if (dsInfo.filter) {
            settings[dsIndex].dsFilterId = getFilterId(filters, dsInfo.filter);
          }
        }
      }
      if (dsInfo.additionalDsInfo && (settings[dsIndex] as MapDataLayerSettings).additionalDataSources?.length) {
        updateMapDatasourceFromExportInfo(entityAliases,
          filters, (settings[dsIndex] as MapDataLayerSettings).additionalDataSources, dsInfo.additionalDsInfo);
      }
    }
  }
}

const prepareExportDataSourcesInfo = (dashboard: Dashboard, settings: MapDataLayerSettings[] | MapDataSourceSettings[]): ExportDataSourceInfo => {
  const info: ExportDataSourceInfo = {};
  settings.forEach((dsSettings, index) => {
    prepareExportDataSourceInfo(dashboard, info, dsSettings, index);
  });
  return info;
}

const prepareExportDataSourceInfo = (dashboard: Dashboard, info: ExportDataSourceInfo, settings: MapDataLayerSettings | MapDataSourceSettings, index: number): void => {
  const dsInfo: MapDataLayerDsInfo = {};
  const aliasAndFilter = prepareAliasAndFilterPair(dashboard, settings);
  if (aliasAndFilter) {
    dsInfo.alias = aliasAndFilter.alias;
    dsInfo.filter = aliasAndFilter.filter;
  }
  if ((settings as MapDataLayerSettings).additionalDataSources?.length && settings.dsType !== DatasourceType.function) {
    (settings as MapDataLayerSettings).additionalDataSources.forEach((ds, index) => {
      const dsAliasAndFilter = prepareAliasAndFilterPair(dashboard, ds);
      if (dsAliasAndFilter) {
        if (!dsInfo.additionalDsInfo) {
          dsInfo.additionalDsInfo = {};
        }
        dsInfo.additionalDsInfo[index] = dsAliasAndFilter;
      }
    });
  }
  if (!!dsInfo.alias || !!dsInfo.filter || !!dsInfo.additionalDsInfo) {
    info[index] = dsInfo;
  }
}

const prepareAliasAndFilterPair = (dashboard: Dashboard, settings: MapDataSourceSettings): AliasFilterPair => {
  const aliasAndFilter: AliasFilterPair = {};
  if (settings.dsType === DatasourceType.entity) {
    const entityAlias = dashboard.configuration.entityAliases[settings.dsEntityAliasId];
    if (entityAlias) {
      aliasAndFilter.alias = {
        alias: entityAlias.alias,
        filter: entityAlias.filter
      };
    }
    if (settings.dsFilterId && dashboard.configuration.filters) {
      const filter = dashboard.configuration.filters[settings.dsFilterId];
      if (filter) {
        aliasAndFilter.filter = {
          filter: filter.filter,
          keyFilters: filter.keyFilters,
          editable: filter.editable
        };
      }
    }
  }
  if (!!aliasAndFilter.alias || !!aliasAndFilter.filter) {
    return aliasAndFilter;
  } else {
    return null;
  }
}

const getMapDataLayersDatasources = (settings: MapDataLayerSettings[] | MapDataSourceSettings[]): Datasource[] => {
  const datasources: Datasource[] = [];
  settings.forEach((dsSettings) => {
    datasources.push(mapDataSourceSettingsToDatasource(dsSettings));
    if ((dsSettings as MapDataLayerSettings).additionalDataSources?.length) {
      (dsSettings as MapDataLayerSettings).additionalDataSources.forEach((ds) => {
        datasources.push(mapDataSourceSettingsToDatasource(ds));
      });
    }
  });
  return datasources;
};
