import { MapProviders, MapOptions } from "./map-models";
import LeafletMap from './leaflet-map';
import {
    openstreetMapSettingsSchema,
    googleMapSettingsSchema,
    imageMapSettingsSchema,
    tencentMapSettingsSchema,
    commonMapSettingsSchema,
    routeMapSettingsSchema,
    markerClusteringSettingsSchema,
    markerClusteringSettingsSchemaLeaflet,
    hereMapSettingsSchema
} from './schemes';
import { MapWidgetStaticInterface, MapWidgetInterface } from './map-widget.interface';
import { OpenStreetMap, TencentMap, GoogleMap, HEREMap, ImageMap } from './providers';
import { parseData, parseArray, parseFunction } from './maps-utils';

export let TbMapWidgetV2: MapWidgetStaticInterface;
TbMapWidgetV2 = class TbMapWidgetV2 implements MapWidgetInterface {
    map: LeafletMap;
    provider: MapProviders;
    schema;
    data;

    constructor(mapProvider: MapProviders, private drawRoutes, ctx, $element) {
        this.data = ctx.data;
        if (!$element) {
            $element = ctx.$container[0];
        }
        this.provider = mapProvider;
        let MapClass = providerSets[mapProvider]?.MapClass;
        let settings = this.initSettings(ctx?.settings);
        if (!MapClass) {
            return;
        }
        this.map = new MapClass($element, settings);
        this.schema = providerSets[mapProvider]?.schema;
    }

    onInit() {
    }

    initSettings(settings: any) {
        const functionParams = ['data', 'dsData', 'dsIndex'];
        const customOptions = {
            mapProvider: this.provider,
            mapUrl: settings?.mapImageUrl,
            labelFunction: parseFunction(settings.labelFunction, functionParams),
            tooltipFunction: parseFunction(settings.tooltipFunction, functionParams),
            colorFunction: parseFunction(settings.colorFunction, functionParams),
            polygonColorFunction: parseFunction(settings.polygonColorFunction, functionParams),
            markerImageFunction: parseFunction(settings.markerImageFunction, ['data', 'images', 'dsData', 'dsIndex']),
            tooltipPattern: settings.tooltipPattern ||
                "<b>${entityName}</b><br/><br/><b>Latitude:</b> ${" + settings.latKeyName + ":7}<br/><b>Longitude:</b> ${" + settings.lngKeyName + ":7}",
            label: settings.label || "${entityName}",
            currentImage: (settings.useMarkerImage && settings.markerImage?.length) ? {
                url: settings.markerImage,
                size: settings.markerImageSize || 34
            } : null
        }
        return { ...defaultSettings, ...settings, ...customOptions, }
    }

    update() {
        if (this.drawRoutes)
            this.map.updatePolylines(parseArray(this.data));
        this.map.updateMarkers(parseData(this.data));
    }

    onDataUpdated() {
    }

    onResize() {
        this.map.onResize();//not work
    }

    getSettingsSchema(): Object {
        return this.schema;
    }

    resize() {
        this.map?.invalidateSize();
        this.map.onResize();
    }

    public static dataKeySettingsSchema(): Object {
        return {};
    }

    public static settingsSchema(mapProvider, drawRoutes): Object {
        const providerInfo = providerSets[mapProvider];
        let schema = providerInfo.schema;
        schema.groupInfoes = [];

        function addGroupInfo(title: string) {
            schema.groupInfoes.push({
                "formIndex": schema.groupInfoes?.length || 0,
                "GroupTitle": title
            });
        }

        function mergeSchema(newSchema) {
            Object.assign(schema.schema.properties, newSchema.schema.properties);
            schema.schema.required = schema.schema.required.concat(newSchema.schema.required);
            schema.form.push(newSchema.form);//schema.form.concat(commonMapSettingsSchema.form);
        }

        if (providerInfo.name)
            addGroupInfo(providerInfo.name + ' Map Settings');
        schema.form = [schema.form];

        mergeSchema(commonMapSettingsSchema);
        addGroupInfo("Common Map Settings");

        if (drawRoutes) {
            mergeSchema(routeMapSettingsSchema);
            addGroupInfo("Route Map Settings");
        } else if (mapProvider !== 'image-map') {
            let clusteringSchema: any = {
                schema: {
                    properties: {
                        ...markerClusteringSettingsSchemaLeaflet.schema.properties,
                        ...markerClusteringSettingsSchema.schema.properties
                    },
                    required: {
                        ...markerClusteringSettingsSchemaLeaflet.schema.required,
                        ...markerClusteringSettingsSchema.schema.required
                    }
                },
                form: [
                    ...markerClusteringSettingsSchemaLeaflet.form,
                    ...markerClusteringSettingsSchema.form
                ]
            };
            mergeSchema(clusteringSchema);
            addGroupInfo("Markers Clustering Settings");
        }

        return schema;
    }

    public static actionSources(): Object {
        return {
            'markerClick': {
                name: 'widget-action.marker-click',
                multiple: false
            },
            'polygonClick': {
                name: 'widget-action.polygon-click',
                multiple: false
            },
            'tooltipAction': {
                name: 'widget-action.tooltip-tag-action',
                multiple: true
            }
        };
    }

    onDestroy() {
    }
}

const providerSets = {
    'openstreet-map': {
        MapClass: OpenStreetMap,
        schema: openstreetMapSettingsSchema,
        name: "Openstreet"
    },
    'tencent-map': {
        MapClass: TencentMap,
        schema: tencentMapSettingsSchema,
        name: "Tencent"
    },
    'google-map': {
        MapClass: GoogleMap,
        schema: googleMapSettingsSchema,
        name: "Openstreet"
    },
    'here': {
        MapClass: HEREMap,
        schema: hereMapSettingsSchema,
        name: "HERE"
    },
    'image-map': {
        MapClass: ImageMap,
        schema: imageMapSettingsSchema
    }
}

const defaultSettings = {
    xPosKeyName: 'xPos',
    yPosKeyName: 'yPos',
    markerOffsetX: 0.5,
    markerOffsetY: 1,
    latKeyName: 'latitude',
    lngKeyName: 'longitude',
    polygonKeyName: 'coordinates',
    showLabel: false,
    showTooltip: false,
    useDefaultCenterPosition: false,
    showTooltipAction: "click",
    autocloseTooltip: false,
    showPolygon: true,
    labelColor: '#000000',
    color: "#FE7569",
    polygonColor: "#0000ff",
    polygonStrokeColor: "#fe0001",
    polygonOpacity: 0.5,
    polygonStrokeOpacity: 1,
    polygonStrokeWeight: 1,
    useLabelFunction: true,
    markerImages: [],
    strokeWeight: 2,
    strokeOpacity: 1.0,
    initCallback: () => { },
    defaultZoomLevel: 8,
    dontFitMapBounds: false,
    disableScrollZooming: false,
    minZoomLevel: 16,
    credentials: '',
    defaultCenterPosition: [0, 0],
    markerClusteringSetting: null,
}