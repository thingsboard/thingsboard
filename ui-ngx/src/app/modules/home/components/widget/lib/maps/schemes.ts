export const googleMapSettingsSchema =
{
    "schema": {
        "title": "Google Map Configuration",
        "type": "object",
        "properties": {
            "gmApiKey": {
                "title": "Google Maps API Key",
                "type": "string"
            },
            "gmDefaultMapType": {
                "title": "Default map type",
                "type": "string",
                "default": "roadmap"
            }
        },
        "required": [
        ]
    },
    "form": [
        "gmApiKey",
        {
            "key": "gmDefaultMapType",
            "type": "rc-select",
            "multiple": false,
            "items": [
                {
                    "value": "roadmap",
                    "label": "Roadmap"
                },
                {
                    "value": "satellite",
                    "label": "Satellite"
                },
                {
                    "value": "hybrid",
                    "label": "Hybrid"
                },
                {
                    "value": "terrain",
                    "label": "Terrain"
                }
            ]
        }
    ]
};

export const tencentMapSettingsSchema =
{
    "schema": {
        "title": "Tencent Map Configuration",
        "type": "object",
        "properties": {
            "tmApiKey": {
                "title": "Tencent Maps API Key",
                "type": "string"
            },
            "tmDefaultMapType": {
                "title": "Default map type",
                "type": "string",
                "default": "roadmap"
            }
        },
        "required": [
        ]
    },
    "form": [
        "tmApiKey",
        {
            "key": "tmDefaultMapType",
            "type": "rc-select",
            "multiple": false,
            "items": [
                {
                    "value": "roadmap",
                    "label": "Roadmap"
                },
                {
                    "value": "satellite",
                    "label": "Satellite"
                },
                {
                    "value": "hybrid",
                    "label": "Hybrid"
                },
            ]
        }
    ]
};

export const hereMapSettingsSchema =
{
    "schema": {
        "title": "HERE Map Configuration",
        "type": "object",
        "properties": {
            "mapProvider": {
                "title": "Map layer",
                "type": "string",
                "default": "HERE.normalDay"
            },
            "credentials": {
                "type": "object",
                "properties": {
                    "app_id": {
                        "title": "HERE app id",
                        "type": "string"
                    },
                    "app_code": {
                        "title": "HERE app code",
                        "type": "string"
                    }
                },
                "required": ["app_id", "app_code"]
            }
        },
        "required": []
    },
    "form": [
        {
            "key": "mapProvider",
            "type": "rc-select",
            "multiple": false,
            "items": [
                {
                    "value": "HERE.normalDay",
                    "label": "HERE.normalDay (Default)"
                },
                {
                    "value": "HERE.normalNight",
                    "label": "HERE.normalNight"
                },
                {
                    "value": "HERE.hybridDay",
                    "label": "HERE.hybridDay"
                },
                {
                    "value": "HERE.terrainDay",
                    "label": "HERE.terrainDay"
                }
            ]
        },
        "credentials"
    ]
};

export const openstreetMapSettingsSchema =
{
    "schema": {
        "title": "Openstreet Map Configuration",
        "type": "object",
        "properties": {
            "mapProvider": {
                "title": "Map provider",
                "type": "string",
                "default": "OpenStreetMap.Mapnik"
            },
            "useCustomProvider": {
                "title": "Use custom provider",
                "type": "boolean",
                "default": false
            },
            "customProviderTileUrl": {
                "title": "Custom provider tile URL",
                "type": "string",
                "default": "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            }
        },
        "required": []
    },
    "form": [
        {
            "key": "mapProvider",
            "type": "rc-select",
            "multiple": false,
            "items": [
                {
                    "value": "OpenStreetMap.Mapnik",
                    "label": "OpenStreetMap.Mapnik (Default)"
                },
                {
                    "value": "OpenStreetMap.BlackAndWhite",
                    "label": "OpenStreetMap.BlackAndWhite"
                },
                {
                    "value": "OpenStreetMap.HOT",
                    "label": "OpenStreetMap.HOT"
                },
                {
                    "value": "Esri.WorldStreetMap",
                    "label": "Esri.WorldStreetMap"
                },
                {
                    "value": "Esri.WorldTopoMap",
                    "label": "Esri.WorldTopoMap"
                },
                {
                    "value": "CartoDB.Positron",
                    "label": "CartoDB.Positron"
                },
                {
                    "value": "CartoDB.DarkMatter",
                    "label": "CartoDB.DarkMatter"
                }
            ]
        },
        "useCustomProvider",
        {
            "key": "customProviderTileUrl",
            "condition": "model.useCustomProvider === true",
        }
    ]
};

export const commonMapSettingsSchema =
{
    "schema": {
        "title": "Map Configuration",
        "type": "object",
        "properties": {
            "defaultZoomLevel": {
                "title": "Default map zoom level (0 - 20)",
                "type": "number"
            },
            "useDefaultCenterPosition": {
                "title": "Use default map center position",
                "type": "boolean",
                "default": false
            },
            "defaultCenterPosition": {
                "title": "Default map center position (0,0)",
                "type": "string",
                "default": "0,0"
            },
            "fitMapBounds": {
                "title": "Fit map bounds to cover all markers",
                "type": "boolean",
                "default": true
            },
            "disableScrollZooming": {
                "title": "Disable scroll zooming",
                "type": "boolean",
                "default": false
            },
            "latKeyName": {
                "title": "Latitude key name",
                "type": "string",
                "default": "latitude"
            },
            "lngKeyName": {
                "title": "Longitude key name",
                "type": "string",
                "default": "longitude"
            },
            "showLabel": {
                "title": "Show label",
                "type": "boolean",
                "default": true
            },
            "label": {
                "title": "Label (pattern examples: '${entityName}', '${entityName}: (Text ${keyName} units.)' )",
                "type": "string",
                "default": "${entityName}"
            },
            "useLabelFunction": {
                "title": "Use label function",
                "type": "boolean",
                "default": false
            },
            "labelFunction": {
                "title": "Label function: f(data, dsData, dsIndex)",
                "type": "string"
            },
            "showTooltip": {
                "title": "Show tooltip",
                "type": "boolean",
                "default": true
            },
            "showTooltipAction": {
                "title": "Action for displaying the tooltip",
                "type": "string",
                "default": "click"
            },
            "autocloseTooltip": {
                "title": "Auto-close tooltips",
                "type": "boolean",
                "default": true
            },
            "tooltipPattern": {
                "title": "Tooltip (for ex. 'Text ${keyName} units.' or <link-act name='my-action'>Link text</link-act>')",
                "type": "string",
                "default": "<b>${entityName}</b><br/><br/><b>Latitude:</b> ${latitude:7}<br/><b>Longitude:</b> ${longitude:7}"
            },
            "useTooltipFunction": {
                "title": "Use tooltip function",
                "type": "boolean",
                "default": false
            },
            "tooltipFunction": {
                "title": "Tooltip function: f(data, dsData, dsIndex)",
                "type": "string"
            },
            "color": {
                "title": "Color",
                "type": "string"
            },
            "useColorFunction": {
                "title": "Use color function",
                "type": "boolean",
                "default": false
            },
            "colorFunction": {
                "title": "Color function: f(data, dsData, dsIndex)",
                "type": "string"
            },
            "showPolygon": {
                "title": "Show polygon",
                "type": "boolean",
                "default": false
            },
            "polygonKeyName": {
                "title": "Polygon key name",
                "type": "string",
                "default": "coordinates"
            },
            "polygonColor": {
                "title": "Polygon color",
                "type": "string"
            },
            "polygonOpacity": {
                "title": "Polygon opacity",
                "type": "number",
                "default": 0.5
            },
            "polygonStrokeColor": {
                "title": "Stroke color",
                "type": "string"
            },
            "polygonStrokeOpacity": {
                "title": "Stroke opacity",
                "type": "number",
                "default": 1
            },
            "polygonStrokeWeight": {
                "title": "Stroke weight",
                "type": "number",
                "default": 1
            },
            "usePolygonColorFunction": {
                "title": "Use polygon color function",
                "type": "boolean",
                "default": false
            },
            "polygonColorFunction": {
                "title": "Polygon Color function: f(data, dsData, dsIndex)",
                "type": "string"
            },
            "markerImage": {
                "title": "Custom marker image",
                "type": "string"
            },
            "markerImageSize": {
                "title": "Custom marker image size (px)",
                "type": "number",
                "default": 34
            },
            "useMarkerImageFunction": {
                "title": "Use marker image function",
                "type": "boolean",
                "default": false
            },
            "markerImageFunction": {
                "title": "Marker image function: f(data, images, dsData, dsIndex)",
                "type": "string"
            },
            "markerImages": {
                "title": "Marker images",
                "type": "array",
                "items": {
                    "title": "Marker image",
                    "type": "string"
                }
            }
        },
        "required": []
    },
    "form": [
        "defaultZoomLevel",
        "useDefaultCenterPosition",
        "defaultCenterPosition",
        "fitMapBounds",
        "disableScrollZooming",
        "latKeyName",
        "lngKeyName",
        "showLabel",
        "label",
        "useLabelFunction",
        {
            "key": "labelFunction",
            "type": "javascript"
        },
        "showTooltip",
        {
            "key": "showTooltipAction",
            "type": "rc-select",
            "multiple": false,
            "items": [
                {
                    "value": "click",
                    "label": "Show tooltip on click (Default)"
                },
                {
                    "value": "hover",
                    "label": "Show tooltip on hover"
                }
            ]
        },
        "autocloseTooltip",
        {
            "key": "tooltipPattern",
            "type": "textarea"
        },
        "useTooltipFunction",
        {
            "key": "tooltipFunction",
            "type": "javascript"
        },
        {
            "key": "color",
            "type": "color"
        },
        "useColorFunction",
        {
            "key": "colorFunction",
            "type": "javascript"
        }, "showPolygon", "polygonKeyName",
        {
            "key": "polygonColor",
            "type": "color"
        },
        "polygonOpacity",
        {
            "key": "polygonStrokeColor",
            "type": "color"
        },
        "polygonStrokeOpacity", "polygonStrokeWeight", "usePolygonColorFunction",
        {
            "key": "polygonColorFunction",
            "type": "javascript"
        },
        {
            "key": "markerImage",
            "type": "image"
        },
        "markerImageSize",
        "useMarkerImageFunction",
        {
            "key": "markerImageFunction",
            "type": "javascript"
        },
        {
            "key": "markerImages",
            "items": [
                {
                    "key": "markerImages[]",
                    "type": "image"
                }
            ]
        }
    ]
};

export const routeMapSettingsSchema =
{
    "schema": {
        "title": "Route Map Configuration",
        "type": "object",
        "properties": {
            "strokeWeight": {
                "title": "Stroke weight",
                "type": "number",
                "default": 2
            },
            "strokeOpacity": {
                "title": "Stroke opacity",
                "type": "number",
                "default": 1.0
            }
        },
        "required": []
    },
    "form": [
        "strokeWeight",
        "strokeOpacity"
    ]
};

export const markerClusteringSettingsSchema =
{
    "schema": {
        "title": "Markers Clustering Configuration",
        "type": "object",
        "properties": {
            "useClusterMarkers": {
                "title": "Use map markers clustering",
                "type": "boolean",
                "default": false
            },
            "zoomOnClick": {
                "title": "Zoom when clicking on a cluster",
                "type": "boolean",
                "default": true
            },
            "maxZoom": {
                "title": "The maximum zoom level when a marker can be part of a cluster (0 - 18)",
                "type": "number"
            }
        },
        "required": []
    },
    "form": [
        "useClusterMarkers",
        "zoomOnClick",
        "maxZoom"
    ]
};

export const markerClusteringSettingsSchemaGoogle =
{
    "schema": {
        "title": "Marker Clustering Configuration Google",
        "type": "object",
        "properties": {
            "gridSize": {
                "title": "Maximum radius that a cluster will cover in pixels",
                "type": "number",
                "default": 60
            },
            "minimumClusterSize": {
                "title": "The minimum number of markers in a cluster",
                "type": "number"
            }
        },
        "required": []
    },
    "form": [
        "gridSize",
        "minimumClusterSize"
    ]
};

export const markerClusteringSettingsSchemaLeaflet =
{
    "schema": {
        "title": "Markers Clustering Configuration Leaflet",
        "type": "object",
        "properties": {
            "showCoverageOnHover": {
                "title": "Show the bounds of markers when mouse over a cluster",
                "type": "boolean",
                "default": true
            },
            "animate": {
                "title": "Show animation on markers when zooming",
                "type": "boolean",
                "default": true
            },
            "maxClusterRadius": {
                "title": "Maximum radius that a cluster will cover in pixels",
                "type": "number",
                "default": 80
            },
            "chunkedLoading": {
                "title": "Use chunks for adding markers so that the page does not freeze",
                "type": "boolean",
                "default": false
            },
            "removeOutsideVisibleBounds": {
                "title": "Use lazy load for adding markers",
                "type": "boolean",
                "default": true
            }
        },
        "required": []
    },
    "form": [
        "showCoverageOnHover",
        "animate",
        "maxClusterRadius",
        "chunkedLoading",
        "removeOutsideVisibleBounds"
    ]
};

export const imageMapSettingsSchema =
{
    "schema": {
        "title": "Image Map Configuration",
        "type": "object",
        "properties": {
            "mapImageUrl": {
                "title": "Image map background",
                "type": "string",
                "default": "data:image/svg+xml;base64,PHN2ZyBpZD0ic3ZnMiIgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGhlaWdodD0iMTAwIiB3aWR0aD0iMTAwIiB2ZXJzaW9uPSIxLjEiIHhtbG5zOmNjPSJodHRwOi8vY3JlYXRpdmVjb21tb25zLm9yZy9ucyMiIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgdmlld0JveD0iMCAwIDEwMCAxMDAiPgogPGcgaWQ9ImxheWVyMSIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMCAtOTUyLjM2KSI+CiAgPHJlY3QgaWQ9InJlY3Q0Njg0IiBzdHJva2UtbGluZWpvaW49InJvdW5kIiBoZWlnaHQ9Ijk5LjAxIiB3aWR0aD0iOTkuMDEiIHN0cm9rZT0iIzAwMCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiB5PSI5NTIuODYiIHg9Ii40OTUwNSIgc3Ryb2tlLXdpZHRoPSIuOTkwMTAiIGZpbGw9IiNlZWUiLz4KICA8dGV4dCBpZD0idGV4dDQ2ODYiIHN0eWxlPSJ3b3JkLXNwYWNpbmc6MHB4O2xldHRlci1zcGFjaW5nOjBweDt0ZXh0LWFuY2hvcjptaWRkbGU7dGV4dC1hbGlnbjpjZW50ZXIiIGZvbnQtd2VpZ2h0PSJib2xkIiB4bWw6c3BhY2U9InByZXNlcnZlIiBmb250LXNpemU9IjEwcHgiIGxpbmUtaGVpZ2h0PSIxMjUlIiB5PSI5NzAuNzI4MDkiIHg9IjQ5LjM5NjQ3NyIgZm9udC1mYW1pbHk9IlJvYm90byIgZmlsbD0iIzY2NjY2NiI+PHRzcGFuIGlkPSJ0c3BhbjQ2OTAiIHg9IjUwLjY0NjQ3NyIgeT0iOTcwLjcyODA5Ij5JbWFnZSBiYWNrZ3JvdW5kIDwvdHNwYW4+PHRzcGFuIGlkPSJ0c3BhbjQ2OTIiIHg9IjQ5LjM5NjQ3NyIgeT0iOTgzLjIyODA5Ij5pcyBub3QgY29uZmlndXJlZDwvdHNwYW4+PC90ZXh0PgogIDxyZWN0IGlkPSJyZWN0NDY5NCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIgaGVpZ2h0PSIxOS4zNiIgd2lkdGg9IjY5LjM2IiBzdHJva2U9IiMwMDAiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgeT0iOTkyLjY4IiB4PSIxNS4zMiIgc3Ryb2tlLXdpZHRoPSIuNjM5ODYiIGZpbGw9Im5vbmUiLz4KIDwvZz4KPC9zdmc+Cg=="
            },
            "imageEntityAlias": {
                "title": "Image URL source entity alias",
                "type": "string",
                "default": ""
            },
            "imageUrlAttribute": {
                "title": "Image URL source entity attribute",
                "type": "string",
                "default": ""
            },
            "disableScrollZooming": {
                "title": "Disable scroll zooming",
                "type": "boolean",
                "default": false
            },
            "xPosKeyName": {
                "title": "X position key name",
                "type": "string",
                "default": "xPos"
            },
            "yPosKeyName": {
                "title": "Y position key name",
                "type": "string",
                "default": "yPos"
            },
            "showLabel": {
                "title": "Show label",
                "type": "boolean",
                "default": true
            },
            "label": {
                "title": "Label (pattern examples: '${entityName}', '${entityName}: (Text ${keyName} units.)' )",
                "type": "string",
                "default": "${entityName}"
            },
            "useLabelFunction": {
                "title": "Use label function",
                "type": "boolean",
                "default": false
            },
            "labelFunction": {
                "title": "Label function: f(data, dsData, dsIndex)",
                "type": "string"
            },
            "showTooltip": {
                "title": "Show tooltip",
                "type": "boolean",
                "default": true
            },
            "showTooltipAction": {
                "title": "Action for displaying the tooltip",
                "type": "string",
                "default": "click"
            },
            "autocloseTooltip": {
                "title": "Auto-close tooltips",
                "type": "boolean",
                "default": true
            },
            "tooltipPattern": {
                "title": "Tooltip (for ex. 'Text ${keyName} units.' or <link-act name='my-action'>Link text</link-act>')",
                "type": "string",
                "default": "<b>${entityName}</b><br/><br/><b>X Pos:</b> ${xPos:2}<br/><b>Y Pos:</b> ${yPos:2}"
            },
            "useTooltipFunction": {
                "title": "Use tooltip function",
                "type": "boolean",
                "default": false
            },
            "tooltipFunction": {
                "title": "Tooltip function: f(data, dsData, dsIndex)",
                "type": "string"
            },
            "color": {
                "title": "Color",
                "type": "string"
            },
            "posFunction": {
                "title": "Position conversion function: f(origXPos, origYPos), should return x,y coordinates as double from 0 to 1 each",
                "type": "string",
                "default": "return {x: origXPos, y: origYPos};"
            },
            "markerOffsetX": {
                "title": "Marker X offset relative to position",
                "type": "number",
                "default": 0.5
            },
            "markerOffsetY": {
                "title": "Marker Y offset relative to position",
                "type": "number",
                "default": 1
            },
            "useColorFunction": {
                "title": "Use color function",
                "type": "boolean",
                "default": false
            },
            "colorFunction": {
                "title": "Color function: f(data, dsData, dsIndex)",
                "type": "string"
            },
            "markerImage": {
                "title": "Custom marker image",
                "type": "string"
            },
            "markerImageSize": {
                "title": "Custom marker image size (px)",
                "type": "number",
                "default": 34
            },
            "useMarkerImageFunction": {
                "title": "Use marker image function",
                "type": "boolean",
                "default": false
            },
            "markerImageFunction": {
                "title": "Marker image function: f(data, images, dsData, dsIndex)",
                "type": "string"
            },
            "markerImages": {
                "title": "Marker images",
                "type": "array",
                "items": {
                    "title": "Marker image",
                    "type": "string"
                }
            }
        },
        "required": []
    },
    "form": [
        {
            "key": "mapImageUrl",
            "type": "image"
        },
        "imageEntityAlias",
        "imageUrlAttribute",
        "disableScrollZooming",
        "xPosKeyName",
        "yPosKeyName",
        "showLabel",
        "label",
        "useLabelFunction",
        {
            "key": "labelFunction",
            "type": "javascript"
        },
        "showTooltip",
        {
            "key": "showTooltipAction",
            "type": "rc-select",
            "multiple": false,
            "items": [
                {
                    "value": "click",
                    "label": "Show tooltip on click (Default)"
                },
                {
                    "value": "hover",
                    "label": "Show tooltip on hover"
                }
            ]
        },
        "autocloseTooltip",
        {
            "key": "tooltipPattern",
            "type": "textarea"
        },
        "useTooltipFunction",
        {
            "key": "tooltipFunction",
            "type": "javascript"
        },
        {
            "key": "color",
            "type": "color"
        },
        {
            "key": "posFunction",
            "type": "javascript"
        },
        "markerOffsetX",
        "markerOffsetY",
        "useColorFunction",
        {
            "key": "colorFunction",
            "type": "javascript"
        },
        {
            "key": "markerImage",
            "type": "image"
        },
        "markerImageSize",
        "useMarkerImageFunction",
        {
            "key": "markerImageFunction",
            "type": "javascript"
        },
        {
            "key": "markerImages",
            "items": [
                {
                    "key": "markerImages[]",
                    "type": "image"
                }
            ]
        }
    ]
};

export const mapProviderSchema =
{
    "schema": {
        "title": "Map Provider Configuration",
        "type": "object",
        "properties": {
            "provider": {
                "title": "Map Provider",
                "type": "string",
                "default": "openstreet-map"
            }
        },
        "required": []
    },
    "form": [
        {
            "key": "provider",
            "type": "rc-select",
            "multiple": false,
            "items": [
                {
                    "value": "google-map",
                    "label": "Google maps"
                },
                {
                    "value": "openstreet-map",
                    "label": "Openstreet maps"
                },
                {
                    "value": "here",
                    "label": "HERE maps"
                },
                {
                    "value": "image-map",
                    "label": "Image map"
                },
                {
                    "value": "tencent-map",
                    "label": "Tencent maps"
                }
            ]
        }
    ]
};