// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Datasource, Widget } from '@shared/models/widget.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { EntityAliases } from '@shared/models/alias.models';
import { Filters } from '@shared/models/query/query.models';
import { MapModelDefinition } from '@shared/models/widget/maps/map-model.definition';

export interface WidgetModelDefinition<T = any> {
  testWidget(widget: Widget): boolean;
  prepareExportInfo(dashboard: Dashboard, widget: Widget): T;
  updateFromExportInfo(widget: Widget, entityAliases: EntityAliases, filters: Filters, info: T): void;
  datasources(widget: Widget): Datasource[];
}

const widgetModelRegistry: WidgetModelDefinition[] = [
  MapModelDefinition
];

export const findWidgetModelDefinition = (widget: Widget): WidgetModelDefinition => {
  return widgetModelRegistry.find(def => def.testWidget(widget));
}
