///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { defaultSettings, hereProviders, MapProviders, providerSets, UnitedMapSettings } from './map-models';
import LeafletMap from './leaflet-map';
import {
    commonMapSettingsSchema,
    mapPolygonSchema,
    mapProviderSchema,
    markerClusteringSettingsSchema,
    markerClusteringSettingsSchemaLeaflet,
    routeMapSettingsSchema
} from './schemes';
import { MapWidgetInterface, MapWidgetStaticInterface } from './map-widget.interface';
import { addCondition, addGroupInfo, addToSchema, initSchema, mergeSchemes } from '@core/schema-utils';
import { of, Subject } from 'rxjs';
import { WidgetContext } from '@app/modules/home/models/widget-component.models';
import { getDefCenterPosition, parseArray, parseData, parseFunction, parseWithTranslation } from './maps-utils';
import {
    Datasource,
    DatasourceType,
    JsonSettingsSchema,
    WidgetActionDescriptor,
    widgetType,
    DatasourceData
} from '@shared/models/widget.models';
import { EntityId } from '@shared/models/id/entity-id';
import { AttributeScope, DataKeyType, LatestTelemetry } from '@shared/models/telemetry/telemetry.models';
import { AttributeService } from '@core/http/attribute.service';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import _ from 'lodash';

// @dynamic
export class MapWidgetController implements MapWidgetInterface {

    constructor(
        public mapProvider: MapProviders,
        private drawRoutes: boolean,
        public ctx: WidgetContext,
        $element: HTMLElement,
        isEdit?: boolean
    ) {
        if (this.map) {
            this.map.map.remove();
            delete this.map;
        }

        this.data = ctx.data;
        if (!$element) {
            $element = ctx.$container[0];
        }
        this.settings = this.initSettings(ctx.settings);
        if (isEdit) {
            this.settings.draggableMarker = true;
        }
        this.settings.tooltipAction = this.getDescriptors('tooltipAction');
        this.settings.markerClick = this.getDescriptors('markerClick');
        this.settings.polygonClick = this.getDescriptors('polygonClick');

        const MapClass = providerSets[this.provider]?.MapClass;
        if (!MapClass) {
            return;
        }
        parseWithTranslation.setTranslate(this.translate);
        this.map = new MapClass($element, this.settings, this.ctx.$injector);
        this.map.setImageAlias(this.subscribeForImageAttribute());
        this.map.saveMarkerLocation = this.setMarkerLocation;
        if (this.settings.draggableMarker) {
            this.map.setDataSources(parseData(this.data));
        }
    }

    map: LeafletMap;
    provider: MapProviders;
    schema: JsonSettingsSchema;
    data: DatasourceData[];
    settings: UnitedMapSettings;

    public static dataKeySettingsSchema(): object {
        return {};
    }

    public static getProvidersSchema(mapProvider: MapProviders, ignoreImageMap = false) {
        const providerSchema = _.cloneDeep(mapProviderSchema);
        if (mapProvider)
            providerSchema.schema.properties.provider.default = mapProvider;
        if (ignoreImageMap) {
            providerSchema.form[0].items = providerSchema.form[0]?.items.filter(item => item.value !== 'image-map');
        }
        return mergeSchemes([providerSchema,
            ...Object.keys(providerSets)?.map(
                (key: string) => {
                    const setting = providerSets[key];
                    return addCondition(setting?.schema, `model.provider === '${setting.name}'`);
                })]);
    }

    public static settingsSchema(mapProvider: MapProviders, drawRoutes: boolean): JsonSettingsSchema {
        const schema = initSchema();
        addToSchema(schema, this.getProvidersSchema(mapProvider));
        addGroupInfo(schema, 'Map Provider Settings');
        addToSchema(schema, commonMapSettingsSchema);
        addGroupInfo(schema, 'Common Map Settings');
        addToSchema(schema, addCondition(mapPolygonSchema, 'model.showPolygon === true', ['showPolygon']));
        addGroupInfo(schema, 'Polygon Settings');
        if (drawRoutes) {
            addToSchema(schema, routeMapSettingsSchema);
            addGroupInfo(schema, 'Route Map Settings');
        } else {
            const clusteringSchema = mergeSchemes([markerClusteringSettingsSchema,
                addCondition(markerClusteringSettingsSchemaLeaflet,
                    `model.useClusterMarkers === true && model.provider !== "image-map"`)])
            addToSchema(schema, clusteringSchema);
            addGroupInfo(schema, 'Markers Clustering Settings');
        }
        return schema;
    }

    public static actionSources(): object {
        return {
            markerClick: {
                name: 'widget-action.marker-click',
                multiple: false
            },
            polygonClick: {
                name: 'widget-action.polygon-click',
                multiple: false
            },
            tooltipAction: {
                name: 'widget-action.tooltip-tag-action',
                multiple: true
            }
        };
    }

    translate = (key: string, defaultTranslation?: string): string => {
        if (key)
            return (this.ctx.$injector.get(UtilsService).customTranslation(key, defaultTranslation || key)
                || this.ctx.$injector.get(TranslateService).instant(key));
        else return '';
    }

    getDescriptors(name: string): { [name: string]: ($event: Event, datasource: Datasource) => void } {
        const descriptors = this.ctx.actionsApi.getActionDescriptors(name);
        const actions = {};
        descriptors.forEach(descriptor => {
            actions[descriptor.name] = ($event: Event, datasource: Datasource) => this.onCustomAction(descriptor, $event, datasource);
        }, actions);
        return actions;
    }

    onInit() {
    }

    private onCustomAction(descriptor: WidgetActionDescriptor, $event: Event, entityInfo: Datasource) {
        if ($event) {
            $event.preventDefault();
            $event.stopPropagation();
        }
        const { entityId, entityName, entityLabel, entityType } = entityInfo;
        this.ctx.actionsApi.handleWidgetAction($event, descriptor, {
            entityType,
            id: entityId
        }, entityName, null, entityLabel);
    }

    setMarkerLocation = (e) => {
        const attributeService = this.ctx.$injector.get(AttributeService);

        const entityId: EntityId = {
            entityType: e.$datasource.entityType,
            id: e.$datasource.entityId
        };
        const attributes = [];
        const timeseries = [];
        const latLngProperties = [this.settings.latKeyName, this.settings.lngKeyName, this.settings.xPosKeyName, this.settings.yPosKeyName];
        e.$datasource.dataKeys.forEach(key => {
            if (latLngProperties.includes(key.name)) {
                const value = {
                    key: key.name,
                    value: e[key.name]
                };
                if (key.type === DataKeyType.attribute) {
                    attributes.push(value)
                }
                if (key.type === DataKeyType.timeseries) {
                    timeseries.push(value)
                }
            }
        });
        if (timeseries.length) {
            attributeService.saveEntityTimeseries(
                entityId,
                LatestTelemetry.LATEST_TELEMETRY,
                timeseries
            ).subscribe(() => { });
        }
        if (attributes.length) {
            attributeService.saveEntityAttributes(
                entityId,
                AttributeScope.SERVER_SCOPE,
                attributes
            ).subscribe(() => { });
        }
    }

    initSettings(settings: UnitedMapSettings): UnitedMapSettings {
        const functionParams = ['data', 'dsData', 'dsIndex'];
        this.provider = settings.provider || this.mapProvider;
        if (this.provider === MapProviders.here && !settings.mapProviderHere) {
            if (settings.mapProvider && hereProviders.includes(settings.mapProvider))
                settings.mapProviderHere = settings.mapProvider
            else settings.mapProviderHere = hereProviders[0];
        }
        const customOptions = {
            provider: this.provider,
            mapUrl: settings?.mapImageUrl,
            labelFunction: parseFunction(settings.labelFunction, functionParams),
            tooltipFunction: parseFunction(settings.tooltipFunction, functionParams),
            colorFunction: parseFunction(settings.colorFunction, functionParams),
            polygonColorFunction: parseFunction(settings.polygonColorFunction, functionParams),
            polygonTooltipFunction: parseFunction(settings.polygonTooltipFunction, functionParams),
            markerImageFunction: parseFunction(settings.markerImageFunction, ['data', 'images', 'dsData', 'dsIndex']),
            labelColor: this.ctx.widgetConfig.color,
            polygonKeyName: settings.polKeyName ? settings.polKeyName : settings.polygonKeyName,
            tooltipPattern: settings.tooltipPattern ||
                '<b>${entityName}</b><br/><br/><b>Latitude:</b> ${' +
                settings.latKeyName + ':7}<br/><b>Longitude:</b> ${' + settings.lngKeyName + ':7}',
            defaultCenterPosition: getDefCenterPosition(settings?.defaultCenterPosition),
            currentImage: (settings.markerImage?.length) ? {
                url: settings.markerImage,
                size: settings.markerImageSize || 34
            } : null
        }
        return { ...defaultSettings, ...settings, ...customOptions, }
    }

    update() {
        if (this.drawRoutes)
            this.map.updatePolylines(parseArray(this.data));
        if (this.settings.showPolygon) {
            this.map.updatePolygons(parseData(this.data));
        }
        this.map.updateMarkers(parseData(this.data));
    }

    resize() {
        this.map?.invalidateSize();
        this.map.onResize();
    }

    subscribeForImageAttribute() {
        const imageEntityAlias = this.settings.imageEntityAlias;
        const imageUrlAttribute = this.settings.imageUrlAttribute;
        if (!imageEntityAlias || !imageUrlAttribute) {
            return of(false);
        }
        const entityAliasId = this.ctx.aliasController.getEntityAliasId(imageEntityAlias);
        if (!entityAliasId) {
            return of(false);
        }
        const datasources = [
            {
                type: DatasourceType.entity,
                name: imageEntityAlias,
                aliasName: imageEntityAlias,
                entityAliasId,
                dataKeys: [
                    {
                        type: DataKeyType.attribute,
                        name: imageUrlAttribute,
                        label: imageUrlAttribute,
                        settings: {},
                        _hash: Math.random()
                    }
                ]
            }
        ];
        const result = new Subject();
        const imageUrlSubscriptionOptions = {
            datasources,
            useDashboardTimewindow: false,
            type: widgetType.latest,
            callbacks: {
                onDataUpdated: (subscription) => {
                    result.next(subscription?.data[0]?.data[0]);
                }
            }
        };
        this.ctx.subscriptionApi.createSubscription(imageUrlSubscriptionOptions, true).subscribe(() => { });
        return result;
    }

    onDestroy() {
    }
}

export let TbMapWidgetV2: MapWidgetStaticInterface = MapWidgetController;


