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

export enum ItemType {
  WIDGET = 'WIDGET',
  DASHBOARD = 'DASHBOARD',
  CALCULATED_FIELD = 'CALCULATED_FIELD',
  RULE_CHAIN = 'RULE_CHAIN',
  DEVICE = 'DEVICE'
}

export const itemTypeTranslations = new Map<ItemType, string>(
  [
    [ItemType.WIDGET, 'item.type-widget'],
    [ItemType.DASHBOARD, 'item.type-dashboard'],
    [ItemType.CALCULATED_FIELD, 'item.type-calculated-field'],
    [ItemType.RULE_CHAIN, 'item.type-rule-chain'],
    [ItemType.DEVICE, 'item.type-device']
  ]
);

export enum WidgetCategory {
  CHARTS_GRAPHS = 'CHARTS_GRAPHS',
  GAUGES_INDICATORS = 'GAUGES_INDICATORS',
  CONTROLS = 'CONTROLS',
  MAPS_LOCATION = 'MAPS_LOCATION',
  TABLES_LISTS = 'TABLES_LISTS',
  CARDS_INFO = 'CARDS_INFO',
  SCADA = 'SCADA',
  INPUT_FORMS = 'INPUT_FORMS'
}

export const widgetCategoryTranslations = new Map<WidgetCategory, string>([
  [WidgetCategory.CHARTS_GRAPHS, 'iot-hub.category.charts-graphs'],
  [WidgetCategory.GAUGES_INDICATORS, 'iot-hub.category.gauges-indicators'],
  [WidgetCategory.CONTROLS, 'iot-hub.category.controls'],
  [WidgetCategory.MAPS_LOCATION, 'iot-hub.category.maps-location'],
  [WidgetCategory.TABLES_LISTS, 'iot-hub.category.tables-lists'],
  [WidgetCategory.CARDS_INFO, 'iot-hub.category.cards-info'],
  [WidgetCategory.SCADA, 'iot-hub.category.scada'],
  [WidgetCategory.INPUT_FORMS, 'iot-hub.category.input-forms']
]);

export enum DashboardCategory {
  MONITORING = 'MONITORING',
  ANALYTICS = 'ANALYTICS',
  DEVICE_MANAGEMENT = 'DEVICE_MANAGEMENT',
  USER_MANAGEMENT = 'USER_MANAGEMENT',
  ASSET_MANAGEMENT = 'ASSET_MANAGEMENT',
  SCADA = 'SCADA',
  REPORTING = 'REPORTING',
  OVERVIEW = 'OVERVIEW',
  OPERATIONS = 'OPERATIONS'
}

export const dashboardCategoryTranslations = new Map<DashboardCategory, string>([
  [DashboardCategory.MONITORING, 'iot-hub.category.monitoring'],
  [DashboardCategory.ANALYTICS, 'iot-hub.category.analytics'],
  [DashboardCategory.DEVICE_MANAGEMENT, 'iot-hub.category.device-management'],
  [DashboardCategory.USER_MANAGEMENT, 'iot-hub.category.user-management'],
  [DashboardCategory.ASSET_MANAGEMENT, 'iot-hub.category.asset-management'],
  [DashboardCategory.SCADA, 'iot-hub.category.scada'],
  [DashboardCategory.REPORTING, 'iot-hub.category.reporting'],
  [DashboardCategory.OVERVIEW, 'iot-hub.category.overview'],
  [DashboardCategory.OPERATIONS, 'iot-hub.category.operations']
]);

export enum CalcFieldCategory {
  AGGREGATION = 'AGGREGATION',
  GEOSPATIAL = 'GEOSPATIAL',
  STATISTICAL = 'STATISTICAL',
  ENERGY = 'ENERGY',
  ENVIRONMENTAL = 'ENVIRONMENTAL',
  PREDICTIVE = 'PREDICTIVE',
  CUSTOM_FORMULA = 'CUSTOM_FORMULA'
}

export const calcFieldCategoryTranslations = new Map<CalcFieldCategory, string>([
  [CalcFieldCategory.AGGREGATION, 'iot-hub.category.aggregation'],
  [CalcFieldCategory.GEOSPATIAL, 'iot-hub.category.geospatial'],
  [CalcFieldCategory.STATISTICAL, 'iot-hub.category.statistical'],
  [CalcFieldCategory.ENERGY, 'iot-hub.category.energy'],
  [CalcFieldCategory.ENVIRONMENTAL, 'iot-hub.category.environmental'],
  [CalcFieldCategory.PREDICTIVE, 'iot-hub.category.predictive'],
  [CalcFieldCategory.CUSTOM_FORMULA, 'iot-hub.category.custom-formula']
]);

export enum RuleChainCategory {
  DATA_PROCESSING = 'DATA_PROCESSING',
  ALERTING = 'ALERTING',
  DEVICE_CONNECTIVITY = 'DEVICE_CONNECTIVITY',
  INTEGRATION = 'INTEGRATION',
  ANALYTICS = 'ANALYTICS'
}

export const ruleChainCategoryTranslations = new Map<RuleChainCategory, string>([
  [RuleChainCategory.DATA_PROCESSING, 'iot-hub.category.data-processing'],
  [RuleChainCategory.ALERTING, 'iot-hub.category.alerting'],
  [RuleChainCategory.DEVICE_CONNECTIVITY, 'iot-hub.category.device-connectivity'],
  [RuleChainCategory.INTEGRATION, 'iot-hub.category.integration'],
  [RuleChainCategory.ANALYTICS, 'iot-hub.category.analytics']
]);

export enum DeviceCategory {
  SENSORS = 'SENSORS',
  GATEWAYS = 'GATEWAYS',
  CONTROLLERS = 'CONTROLLERS',
  ACTUATORS = 'ACTUATORS',
  TRACKERS = 'TRACKERS',
  METERS = 'METERS'
}

export const deviceCategoryTranslations = new Map<DeviceCategory, string>([
  [DeviceCategory.SENSORS, 'iot-hub.category.sensors'],
  [DeviceCategory.GATEWAYS, 'iot-hub.category.gateways'],
  [DeviceCategory.CONTROLLERS, 'iot-hub.category.controllers'],
  [DeviceCategory.ACTUATORS, 'iot-hub.category.actuators'],
  [DeviceCategory.TRACKERS, 'iot-hub.category.trackers'],
  [DeviceCategory.METERS, 'iot-hub.category.meters']
]);

export enum UseCase {
  SMART_HOME = 'SMART_HOME',
  INDUSTRIAL_IOT = 'INDUSTRIAL_IOT',
  ENERGY_MANAGEMENT = 'ENERGY_MANAGEMENT',
  FLEET_MANAGEMENT = 'FLEET_MANAGEMENT',
  AGRICULTURE = 'AGRICULTURE',
  SMART_CITY = 'SMART_CITY',
  HEALTHCARE = 'HEALTHCARE',
  RETAIL = 'RETAIL',
  WATER_UTILITIES = 'WATER_UTILITIES'
}

export const useCaseTranslations = new Map<UseCase, string>([
  [UseCase.SMART_HOME, 'iot-hub.use-case.smart-home'],
  [UseCase.INDUSTRIAL_IOT, 'iot-hub.use-case.industrial-iot'],
  [UseCase.ENERGY_MANAGEMENT, 'iot-hub.use-case.energy-management'],
  [UseCase.FLEET_MANAGEMENT, 'iot-hub.use-case.fleet-management'],
  [UseCase.AGRICULTURE, 'iot-hub.use-case.agriculture'],
  [UseCase.SMART_CITY, 'iot-hub.use-case.smart-city'],
  [UseCase.HEALTHCARE, 'iot-hub.use-case.healthcare'],
  [UseCase.RETAIL, 'iot-hub.use-case.retail'],
  [UseCase.WATER_UTILITIES, 'iot-hub.use-case.water-utilities']
]);

export function getCategoriesForType(type: ItemType): Map<string, string> {
  switch (type) {
    case ItemType.WIDGET:
      return widgetCategoryTranslations as Map<string, string>;
    case ItemType.DASHBOARD:
      return dashboardCategoryTranslations as Map<string, string>;
    case ItemType.CALCULATED_FIELD:
      return calcFieldCategoryTranslations as Map<string, string>;
    case ItemType.RULE_CHAIN:
      return ruleChainCategoryTranslations as Map<string, string>;
    case ItemType.DEVICE:
      return deviceCategoryTranslations as Map<string, string>;
    default:
      return new Map();
  }
}
