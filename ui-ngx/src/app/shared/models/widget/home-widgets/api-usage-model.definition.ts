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
import { FilterInfo, Filters } from '@shared/models/query/query.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { Datasource, DatasourceType, Widget } from '@shared/models/widget.models';
import { WidgetModelDefinition } from '@shared/models/widget/widget-model.definition';
import {
  ApiUsageWidgetSettings,
  getUniqueDataKeys
} from '@home/components/widget/lib/settings/cards/api-usage-settings.component.models';

interface AliasFilterPair {
  alias?: EntityAliasInfo;
  filter?: FilterInfo;
}

interface ApiUsageDatasourcesInfo {
  ds?: AliasFilterPair;
}

export const ApiUsageModelDefinition: WidgetModelDefinition<ApiUsageDatasourcesInfo> = {
  testWidget(widget: Widget): boolean {
    if (widget?.config?.settings) {
      const settings = widget.config.settings;
      if (settings.apiUsageDataKeys && Array.isArray(settings.apiUsageDataKeys)) {
        return true;
      }
    }
    return false;
  },
  prepareExportInfo(dashboard: Dashboard, widget: Widget): ApiUsageDatasourcesInfo {
    const settings: ApiUsageWidgetSettings = widget.config.settings as ApiUsageWidgetSettings;
    const info: ApiUsageDatasourcesInfo = {};
    if (settings.dsEntityAliasId) {
      info.ds = prepareExportDataSourcesInfo(dashboard, settings.dsEntityAliasId);
    }
    return info;
  },
  updateFromExportInfo(widget: Widget, entityAliases: EntityAliases, filters: Filters, info: ApiUsageDatasourcesInfo): void {
    const settings: ApiUsageWidgetSettings = widget.config.settings as ApiUsageWidgetSettings;
    if (info?.ds?.alias) {
      settings.dsEntityAliasId = getEntityAliasId(entityAliases, info.ds.alias);
    }
  },
  datasources(widget: Widget): Datasource[] {
    const settings: ApiUsageWidgetSettings = widget.config.settings as ApiUsageWidgetSettings;
    const datasources: Datasource[] = [];
    if (settings.apiUsageDataKeys?.length && settings.dsEntityAliasId) {
      datasources.push({
        type: DatasourceType.entity,
        name: '',
        entityAliasId: settings.dsEntityAliasId,
        dataKeys: getUniqueDataKeys(settings.apiUsageDataKeys)
      });
    }
    return datasources;
  },
  hasTimewindow(): boolean {
    return false;
  },
  datasourcesHasAggregation(): boolean {
    return false;
  },
  datasourcesHasOnlyComparisonAggregation(): boolean {
    return false;
  }
};

const prepareExportDataSourcesInfo = (dashboard: Dashboard, settings: string): AliasFilterPair => {
  const aliasAndFilter: AliasFilterPair = {};
  const entityAlias = dashboard.configuration.entityAliases[settings];
  if (entityAlias) {
    aliasAndFilter.alias = {
      alias: entityAlias.alias,
      filter: entityAlias.filter
    };
  }
  return aliasAndFilter;
}
