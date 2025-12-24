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
  Datasource,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
  Widget,
  widgetType,
  widgetTypeHasTimewindow
} from '@shared/models/widget.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { EntityAliases } from '@shared/models/alias.models';
import { Filters } from '@shared/models/query/query.models';
import { MapModelDefinition } from '@shared/models/widget/maps/map-model.definition';
import { ApiUsageModelDefinition } from '@shared/models/widget/home-widgets/api-usage-model.definition';

export interface WidgetModelDefinition<T = any> {
  testWidget(widget: Widget): boolean;
  prepareExportInfo(dashboard: Dashboard, widget: Widget): T;
  updateFromExportInfo(widget: Widget, entityAliases: EntityAliases, filters: Filters, info: T): void;
  datasources(widget: Widget): Datasource[];
  hasTimewindow(widget: Widget): boolean;
  datasourcesHasAggregation(widget: Widget): boolean;
  datasourcesHasOnlyComparisonAggregation(widget: Widget): boolean;
}

const widgetModelRegistry: WidgetModelDefinition[] = [
  MapModelDefinition,
  ApiUsageModelDefinition
];

export const findWidgetModelDefinition = (widget: Widget): WidgetModelDefinition => {
  return widgetModelRegistry.find(def => def.testWidget(widget));
}

export const widgetHasTimewindow = (widget: Widget): boolean => {
  const widgetDefinition = findWidgetModelDefinition(widget);
  if (widgetDefinition) {
    return widgetDefinition.hasTimewindow(widget);
  }
  return widgetTypeHasTimewindow(widget.type)
    || (widget.type === widgetType.latest && datasourcesHasAggregation(widget.config.datasources));
};

export const widgetDatasourcesHasAggregation = (widget: Widget): boolean => {
  const widgetDefinition = findWidgetModelDefinition(widget);
  if (widgetDefinition) {
    return widgetDefinition.datasourcesHasAggregation(widget);
  }
  return widget.type === widgetType.latest && datasourcesHasAggregation(widget.config.datasources);
};

export const widgetDatasourcesHasOnlyComparisonAggregation = (widget: Widget): boolean => {
  const widgetDefinition = findWidgetModelDefinition(widget);
  if (widgetDefinition) {
    return widgetDefinition.datasourcesHasOnlyComparisonAggregation(widget);
  }
  return widget.type === widgetType.latest && datasourcesHasOnlyComparisonAggregation(widget.config.datasources);
};
