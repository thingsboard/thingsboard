import { MapProviders, MapOptions } from "./map-models";
import LeafletMap from './leaflet-map';
import { deepClone } from '@core/utils';
import { openstreetMapSettingsSchema, googleMapSettingsSchema, imageMapSettingsSchema, tencentMapSettingsSchema, hereMapSettingsSchema, commonMapSettingsSchema, routeMapSettingsSchema, markerClusteringSettingsSchema, markerClusteringSettingsSchemaGoogle, markerClusteringSettingsSchemaLeaflet } from './schemes';
import { MapWidgetStaticInterface, MapWidgetInterface } from './map-widget.interface';
import { OpenStreetMap, TencentMap, ImageMap, GoogleMap } from './providers';
import { string } from 'prop-types';

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

    constructor(mapProvider: MapProviders, drawRoutes, ctx, useDynamicLocations, $element, isEdit) {
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

let defaultSettings = {
    xPosKeyName: 'xPos',
    yPosKeyName: 'yPos',
    markerOffsetX: 0.5,
    markerOffsetY: 1,
    latKeyName: 'latitude',
    lngKeyName: 'longitude',
    polygonKeyName: 'coordinates',


    //this.locationSettings.tooltipPattern = this.ctx.settings.tooltipPattern || "<b>${entityName}</b><br/><br/><b>Latitude:</b> ${" + this.locationSettings.latKeyName + ":7}<br/><b>Longitude:</b> ${" + this.locationSettings.lngKeyName + ":7}",
    showLabel: false,
    showTooltip: false,
    useDefaultCenterPosition: true,
    showTooltipAction: "click",
    autocloseTooltip: false,
    showPolygon: true,
    labelColor: '#000000',
    /*  this.locationSettings.label = this.ctx.settings.label || "${entityName}",
      this.locationSettings.color = this.ctx.settings.color ? tinycolor(this.ctx.settings.color).toHexString() : "#FE7569",
      this.locationSettings.polygonColor = this.ctx.settings.polygonColor ? tinycolor(this.ctx.settings.polygonColor).toHexString() : "#0000ff",
      this.locationSettings.polygonStrokeColor = this.ctx.settings.polygonStrokeColor ? tinycolor(this.ctx.settings.polygonStrokeColor).toHexString() : "#fe0001",
      this.locationSettings.polygonOpacity = angular.isDefined(this.ctx.settings.polygonOpacity) ? this.ctx.settings.polygonOpacity : 0.5,
      this.locationSettings.polygonStrokeOpacity = angular.isDefined(this.ctx.settings.polygonStrokeOpacity) ? this.ctx.settings.polygonStrokeOpacity : 1,
      this.locationSettings.polygonStrokeWeight = angular.isDefined(this.ctx.settings.polygonStrokeWeight) ? this.ctx.settings.polygonStrokeWeight : 1,
  
      this.locationSettings.useLabelFunction = this.ctx.settings.useLabelFunction === true,
      if(angular.isDefined(this.ctx.settings.labelFunction) && this.ctx.settings.labelFunction.length > 0) {
          try {
              this.locationSettings.labelFunction = new Function('data, dsData, dsIndex', this.ctx.settings.labelFunction),
          } catch (e) {
              this.locationSettings.labelFunction = null,
      }
  }
  
  this.locationSettings.useTooltipFunction = this.ctx.settings.useTooltipFunction === true,
  if (angular.isDefined(this.ctx.settings.tooltipFunction) && this.ctx.settings.tooltipFunction.length > 0) {
      try {
          this.locationSettings.tooltipFunction = new Function('data, dsData, dsIndex', this.ctx.settings.tooltipFunction),
      } catch (e) {
          this.locationSettings.tooltipFunction = null,
      }
  }
  
  this.locationSettings.useColorFunction = this.ctx.settings.useColorFunction === true,
  if (angular.isDefined(this.ctx.settings.colorFunction) && this.ctx.settings.colorFunction.length > 0) {
      try {
          this.locationSettings.colorFunction = new Function('data, dsData, dsIndex', this.ctx.settings.colorFunction),
      } catch (e) {
          this.locationSettings.colorFunction = null,
      }
  }
  this.locationSettings.usePolygonColorFunction = this.ctx.settings.usePolygonColorFunction === true,
  if (angular.isDefined(this.ctx.settings.polygonColorFunction) && this.ctx.settings.polygonColorFunction.length > 0) {
      try {
          this.locationSettings.polygonColorFunction = new Function('data, dsData, dsIndex', this.ctx.settings.polygonColorFunction),
      } catch (e) {
          this.locationSettings.polygonColorFunction = null,
      }
  }
  
  this.locationSettings.useMarkerImageFunction = this.ctx.settings.useMarkerImageFunction === true,
  if (angular.isDefined(this.ctx.settings.markerImageFunction) && this.ctx.settings.markerImageFunction.length > 0) {
      try {
          this.locationSettings.markerImageFunction = new Function('data, images, dsData, dsIndex', this.ctx.settings.markerImageFunction),
      } catch (e) {
          this.locationSettings.markerImageFunction = null,
      }
  }
  
  this.locationSettings.markerImages = this.ctx.settings.markerImages || [],
  
  if (!this.locationSettings.useMarkerImageFunction &&
      angular.isDefined(this.ctx.settings.markerImage) &&
      this.ctx.settings.markerImage.length > 0) {
      this.locationSettings.useMarkerImage = true,
      var url = this.ctx.settings.markerImage,
      var size = this.ctx.settings.markerImageSize || 34,
      this.locationSettings.currentImage = {
          url: url,
          size: size
      },
  }
  
  if (this.drawRoutes) {
      this.locationSettings.strokeWeight = this.ctx.settings.strokeWeight || 2,
          this.locationSettings.strokeOpacity = this.ctx.settings.strokeOpacity || 1.0,*/
}