///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import {BaseData} from '@shared/models/base-data';
import {TenantId} from '@shared/models/id/tenant-id';
import {WidgetsBundleId} from '@shared/models/id/widgets-bundle-id';
import {WidgetTypeId} from '@shared/models/id/widget-type-id';
import { AliasEntityType, EntityType, EntityTypeTranslation } from '@shared/models/entity-type.models';
import { Timewindow } from '@shared/models/time/time.models';

export enum widgetType {
  timeseries = 'timeseries',
  latest = 'latest',
  rpc = 'rpc',
  alarm = 'alarm'
}

export interface WidgetTypeTemplate {
  bundleAlias: string;
  alias: string;
}

export interface WidgetTypeData {
  name: string;
  template: WidgetTypeTemplate;
}

export const widgetTypesData = new Map<widgetType, WidgetTypeData>(
  [
    [
      widgetType.timeseries,
      {
        name: 'widget.timeseries',
        template: {
          bundleAlias: 'charts',
          alias: 'basic_timeseries'
        }
      }
    ],
    [
      widgetType.latest,
      {
        name: 'widget.latest-values',
        template: {
          bundleAlias: 'cards',
          alias: 'attributes_card'
        }
      }
    ],
    [
      widgetType.rpc,
      {
        name: 'widget.rpc',
        template: {
          bundleAlias: 'gpio_widgets',
          alias: 'basic_gpio_control'
        }
      }
    ],
    [
      widgetType.alarm,
      {
        name: 'widget.alarm',
        template: {
          bundleAlias: 'alarm_widgets',
          alias: 'alarms_table'
        }
      }
    ]
  ]
);

export interface WidgetResource {
  url: string;
}

export interface WidgetTypeDescriptor {
  type: widgetType;
  resources: Array<WidgetResource>;
  templateHtml: string;
  templateCss: string;
  controllerScript: string;
  settingsSchema: string;
  dataKeySettingsSchema: string;
  defaultConfig: string;
  sizeX: number;
  sizeY: number;
}

export interface WidgetType extends BaseData<WidgetTypeId> {
  tenantId: TenantId;
  bundleAlias: string;
  alias: string;
  name: string;
  descriptor: WidgetTypeDescriptor;
}

export interface WidgetInfo extends WidgetTypeDescriptor {
  widgetName: string;
  alias: string;
}

export function toWidgetInfo(widgetTypeEntity: WidgetType): WidgetInfo {
  return {
      widgetName: widgetTypeEntity.name,
      alias: widgetTypeEntity.alias,
      type: widgetTypeEntity.descriptor.type,
      sizeX: widgetTypeEntity.descriptor.sizeX,
      sizeY: widgetTypeEntity.descriptor.sizeY,
      resources: widgetTypeEntity.descriptor.resources,
      templateHtml: widgetTypeEntity.descriptor.templateHtml,
      templateCss: widgetTypeEntity.descriptor.templateCss,
      controllerScript: widgetTypeEntity.descriptor.controllerScript,
      settingsSchema: widgetTypeEntity.descriptor.settingsSchema,
      dataKeySettingsSchema: widgetTypeEntity.descriptor.dataKeySettingsSchema,
      defaultConfig: widgetTypeEntity.descriptor.defaultConfig
  };
}

export interface WidgetConfig {
  title?: string;
  titleIcon?: string;
  showTitle?: boolean;
  showTitleIcon?: boolean;
  iconColor?: string;
  iconSize?: number;
  dropShadow?: boolean;
  enableFullscreen?: boolean;
  useDashboardTimewindow?: boolean;
  displayTimewindow?: boolean;
  timewindow?: Timewindow;
  mobileHeight?: number;
  mobileOrder?: number;
  color?: string;
  backgroundColor?: string;
  padding?: string;
  margin?: string;
  widgetStyle?: {[klass: string]: any};
  titleStyle?: {[klass: string]: any};
  [key: string]: any;

  // TODO:
}

export interface Widget {
  id?: string;
  typeId: WidgetTypeId;
  isSystemType: boolean;
  bundleAlias: string;
  typeAlias: string;
  type: widgetType;
  title: string;
  sizeX: number;
  sizeY: number;
  row: number;
  col: number;
  config: WidgetConfig;
}
