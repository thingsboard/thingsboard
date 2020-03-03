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

export let TbMapWidgetV2: MapWidgetStaticInterface;
TbMapWidgetV2 = class TbMapWidgetV2 implements MapWidgetInterface {
    map: LeafletMap;
    provider: MapProviders;
    schema;

    constructor(mapProvider: MapProviders, drawRoutes, ctx, $element) {
        console.log(ctx.settings);

        if (!$element) {
            $element = ctx.$container[0];
        }
        this.provider = mapProvider;
        const baseOptions: MapOptions = {
            initCallback: () => { },
            defaultZoomLevel: 8,
            dontFitMapBounds: false,
            disableScrollZooming: false,
            minZoomLevel: drawRoutes ? 18 : 15,
            mapProvider: mapProvider,
            mapUrl: ctx?.settings?.mapImageUrl,
            credentials: '',
            defaultCenterPosition: [0, 0],
            markerClusteringSetting: null
        }
        let MapClass = providerSets[mapProvider]?.MapClass;
        if (!MapClass) {
            return;
        }
        this.map = new MapClass($element, { ...baseOptions, ...ctx.settings })
        if(mapProvider !== "image-map")
        this.map.createMarker({ lat: 0, lng: 0 }, { color: '#FD2785' })
        else
        this.map.createMarker({ x: 500, y: 500 }, { color: '#6D2785' });
        this.schema = providerSets[mapProvider]?.schema;
    }

    onInit() {
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