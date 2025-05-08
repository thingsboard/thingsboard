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

import { Widget } from '@shared/models/widget.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { EntityAliases } from '@shared/models/alias.models';
import { Filters } from '@shared/models/query/query.models';
import { MapExportDefinition } from '@shared/models/widget/maps/map-export.models';

export interface WidgetExportDefinition<T = any> {
  testWidget(widget: Widget): boolean;
  prepareExportInfo(dashboard: Dashboard, widget: Widget): T;
  updateFromExportInfo(widget: Widget, entityAliases: EntityAliases, filters: Filters, info: T): void;
}

const widgetExportDefinitions: WidgetExportDefinition[] = [
  MapExportDefinition
];

export const getWidgetExportDefinition = (widget: Widget): WidgetExportDefinition => {
  return widgetExportDefinitions.find(def => def.testWidget(widget));
}
