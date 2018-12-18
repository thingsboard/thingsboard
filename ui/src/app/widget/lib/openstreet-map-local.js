/*
 * Copyright © 2016-2018 西安长城数字软件有限公司
 *
 */
import 'leaflet/dist/leaflet.css';
import  L from 'leaflet';
import 'leaflet-providers';
import 'leaflet-draw/dist/leaflet.draw.css';
import 'leaflet-draw';
import drawLocals from 'leaflet-draw-locales';
import 'leaflet.markercluster/dist/MarkerCluster.css';
import 'leaflet.markercluster/dist/MarkerCluster.Default.css';
import 'leaflet.markercluster/dist/leaflet.markercluster';

export default class TbOpenStreetMapLocal {

    constructor($containerElement, utils, initCallback, defaultZoomLevel, dontFitMapBounds, minZoomLevel, mapProvider, makeClusterOptions) {

        this.utils = utils;
        this.defaultZoomLevel = defaultZoomLevel;
        this.dontFitMapBounds = dontFitMapBounds;
        this.minZoomLevel = minZoomLevel;
        this.tooltips = [];

        if (!mapProvider) {
            mapProvider = "openstreetMapLocal.Bright";
        }

        this.map = L.map($containerElement[0]).setView([0, 0], this.defaultZoomLevel || 8);

        var tileLayer = L.tileLayer.provider(mapProvider);

        tileLayer.addTo(this.map);

        //使用自定义半径及图标创建函数创建一个标注组来统一管理标注（点要素）
        this.markerClusterGroup = L.markerClusterGroup(makeClusterOptions);
        this.map.addLayer(this.markerClusterGroup);
        this.markerClusterGroup.on('clusterclick', function (a) {
            a.layer.spiderfy();
        });          

        if (initCallback) {
            setTimeout(initCallback, 0); //eslint-disable-line
        }

    }

    inited() {
        return angular.isDefined(this.map);
    }
    
    /**
     * 是否在地图上显示图层控制工具条，由用户在dashboard界面地图部件的高级设置中的开关来调用使用上面的的showLayerControl方法来控制。
     * 该方法在map_widget2组件中创建工具条之后和修改设置方法中调用
     * @param {*} showControl 
     */
    showLayerControl(showControl){
        if (!showControl)
            this.layerControl.remove();
        else
            this.layerControl.addTo(this.map);  //添加图层控制工具条
    }
    /**
     * 是否在地图上显示绘图工具条，由用户在dashboard界面地图部件的高级设置中的开关来调用使用上面的showDrawToolBar方法来控制。
     * 该方法在map_widget2组件中创建工具条之后和修改设置方法中调用
     * @param {*} showToolBar 
     */
    showDrawToolBar(showToolBar) {
        if (!showToolBar)
            this.drawControl.remove();
        else
            this.drawControl.addTo(this.map);    //添加绘图工具条
    }    
    /**
     * 该方法在map_widget2组件的构造函数中中调用来初始化图层控制工具条和绘图工具条。ctx参数用来
     * @param {*} ctx 
     */
    makeToolBar(ctx){
        if (ctx){
            drawLocals('zh');
            this.layerControl = L.control.layers({}, {}, { hideSingleBase: true }); //创建图层控制工具条，但未添加到地图上
                                                                                    //是否在地图上显示图层控制工具条，由用户在dashboard界面地图部件的高级设置中的开关来调用使用上面的的showLayerControl方法来控制。

            var drawnItems = new L.FeatureGroup();                       //创建绘图特征组，新绘制的要素将作为图层在后面的draw:created事件函数中被加到该组中
            this.map.addLayer(drawnItems);                               //把绘图特征组加到地图上

            var MyCustomMarker = L.Icon.extend({                         //定义标注图标
                options: {
                    shadowUrl: null,
                    iconAnchor: new L.Point(12, 12),
                    iconSize: new L.Point(36, 48),
                    iconUrl: 'static/images/leaflet/marker.png'
                }
            });

            this.drawControl = new L.Control.Draw({       //创建工具条并自定义定义绘图工具条中的工具。注意：工具条尚未加到地图上。是否在地图上显示绘图工具条，由用户在dashboard界面地图部件的高级设置中的开关来调用使用上面的showDrawToolBar方法来控制。
                position: 'topright',
                draw: {
                    polyline: {
                        shapeOptions: {
                            color: '#f357a1',
                            weight: 10
                        }
                    },
                    polygon: {
                        allowIntersection: false,           // 限制现状为简单多边形
                        showArea: true,
                        drawError: {
                            color: '#e1e100',               // 当交点出现时错误提示文本的颜色
                            message: '<strong>喔!<strong> 不允许这么绘制!'
                        },
                        shapeOptions: {
                            color: '#bada55'
                        }
                    },
                    circle: true,
                    marker: {
                        icon: new MyCustomMarker()
                    }
                },
                edit: {
                    featureGroup: drawnItems,
                    remove: true
                }
            });

            var opMap = this;  //为了在下面的事件函数中使用实例变量，因为事件函数中不能使用this访问实例变量
            this.map.on("draw:created", function (e) {            //处理绘图完成事件
                var type = e.layerType,
                    layer = e.layer;

                if (type === 'marker') {                                 
                    layer.bindPopup('坐标: ' + layer.toGeoJSON().geometry.coordinates[0] + ' , ' + layer.toGeoJSON().geometry.coordinates[1]); //添加点击后弹出窗口
                    layer.bindTooltip('经度: ' + layer.toGeoJSON().geometry.coordinates[0] + '<br/>维度: ' + layer.toGeoJSON().geometry.coordinates[1]);  
                    opMap.markerClusterGroup.addLayer(layer);
                }else{
                    drawnItems.addLayer(layer);                       //把绘制的图层添加到标图组中,以便使用图层控制来统一管理
                }

                if (drawnItems.getLayers().length == 1){          //仅在初始会绘制时，才添加
                    opMap.layerControl.addOverlay(drawnItems, "手工标图");          //在图层控制工具条上添加标图图层组控制复选框
                }

                // //=================================================尝试自动更新地理围栏配置参数，仅作实验用， 
                // var layerGeoJson = layer.toGeoJSON();
                // if (type === 'polygon' || type === 'circle') {
                //     layerGeoJson.properties = {
                //         stroke: true,
                //         color: "#0000EF",
                //         weight: 3,
                //         opacity: 1.0,
                //         dashArray: "15,10",
                //         dashOffset: null,
                //         fill: true,
                //         fillColor: "#09900",
                //         fillOpacity: 0.2
                //     };
                // } 

                // var targetGeoJsonIndex = -1;
                // var localSettings = ctx.settings;
                // for (let index = 0; index < localSettings.geoJsonLayers.length; index++) {          //寻找手工标图组
                //     const geojsonType = localSettings.geoJsonLayers[index].type;
                //     if (geojsonType === 'FeatureCollection') {
                //         const name = localSettings.geoJsonLayers[index].name;
                //         if (name === '手工标图') {
                //             targetGeoJsonIndex = index;
                //             break;
                //         }
                //     }
                // }

                // if (targetGeoJsonIndex >= 0) {
                //     ctx.settings.geoJsonLayers[targetGeoJsonIndex].features.push(layerGeoJson);                                                   
                // } else {   //追加新绘制要素到手工标图组
                //     var geojsonItem = {
                //         type: "FeatureCollection",
                //         name: "手工标图",
                //         crs: {
                //             type: "name",
                //             properties: { name: "urn:ogc:def:crs:OGC:1.3:CRS84" }
                //         },
                //         features: [layerGeoJson]
                //     };
                //     ctx.settings.geoJsonLayers.push(geojsonItem);                                 //原先没有绘制过，添加新的手工标图组
                // }
                // localSettings = ctx.settings;                          
                //=================================================实验代码结束


                //================================================尝试自定义工具条开始
                // var customControl = L.Control.extend({

                //     options: {
                //         position: 'topleft'
                //     },

                //     onAdd: function (map) {
                //         var container = L.DomUtil.create('div', 'leaflet-bar leaflet-control leaflet-control-custom');

                //         container.style.backgroundColor = 'white';
                //         container.style.backgroundImage = "url(https://t1.gstatic.com/images?q=tbn:ANd9GcR6FCUMW5bPn8C4PbKak2BJQQsmC-K9-mbYBeFZm1ZM2w2GRy40Ew)";
                //         container.style.backgroundSize = "30px 30px";
                //         container.style.width = '30px';
                //         container.style.height = '30px';

                //         container.onclick = function () {
                //             //console.log('buttonClicked');
                //             map.stop();
                //         }

                //         return container;
                //     }
                // })

                // this.map.addControl(new customControl());
                //================================================尝试自定义工具条代码结束
            }); 
        }    
    }

    updateMarkerLabel(marker, settings) {
        marker.unbindTooltip();
        marker.bindTooltip('<div style="color: '+ settings.labelColor +';"><b>'+settings.labelText+'</b></div>',
            { className: 'tb-marker-label', permanent: true, direction: 'top', offset: marker.tooltipOffset });
    }

    updateMarkerColor(marker, color) {
        this.createDefaultMarkerIcon(marker, color, (iconInfo) => {
            marker.setIcon(iconInfo.icon);
        });
    }

    updateMarkerIcon(marker, settings) {
        this.createMarkerIcon(marker, settings, (iconInfo) => {
            marker.setIcon(iconInfo.icon);
            if (settings.showLabel) {
                marker.unbindTooltip();
                marker.tooltipOffset = [0, -iconInfo.size[1] + 10];
                marker.bindTooltip('<div style="color: '+ settings.labelColor +';"><b>'+settings.labelText+'</b></div>',
                    { className: 'tb-marker-label', permanent: true, direction: 'top', offset: marker.tooltipOffset });
            }
        });
    }

    createMarkerIcon(marker, settings, onMarkerIconReady) {
        var currentImage = settings.currentImage;
        var opMap = this;
        if (currentImage && currentImage.url) {
            this.utils.loadImageAspect(currentImage.url).then(
                (aspect) => {
                    if (aspect) {
                        var width;
                        var height;
                        if (aspect > 1) {
                            width = currentImage.size;
                            height = currentImage.size / aspect;
                        } else {
                            width = currentImage.size * aspect;
                            height = currentImage.size;
                        }
                        var icon = L.icon({
                            iconUrl: currentImage.url,
                            iconSize: [width, height],
                            iconAnchor: [width/2, height],
                            popupAnchor: [0, -height]
                        });
                        var iconInfo = {
                            size: [width, height],
                            icon: icon
                        };
                        onMarkerIconReady(iconInfo);
                    } else {
                        opMap.createDefaultMarkerIcon(marker, settings.color, onMarkerIconReady);
                    }
                }
            );
        } else {
            this.createDefaultMarkerIcon(marker, settings.color, onMarkerIconReady);
        }
    }

    createDefaultMarkerIcon(marker, color, onMarkerIconReady) {
        var icon = L.icon({
            iconUrl: 'static/images/leaflet/marker.png',
            iconSize: [21, 34],
            iconAnchor: [10, 34],
            popupAnchor: [0, -34],
            shadowUrl: 'static/images/leaflet/marker_shadow.png',
            shadowSize: [40, 37],
            shadowAnchor: [12, 35]
        });        
        var iconInfo = {
            size: [21, 34],
            icon: icon
        };
        onMarkerIconReady(iconInfo);
    }

    createMarker(location, settings, onClickListener, markerArgs) {
        var marker = L.marker(location, {});
        var opMap = this;
        this.createMarkerIcon(marker, settings, (iconInfo) => {
            marker.setIcon(iconInfo.icon);
            if (settings.showLabel) {
                marker.tooltipOffset = [0, -iconInfo.size[1] + 10];
                marker.bindTooltip('<div style="color: '+ settings.labelColor +';"><b>'+settings.labelText+'</b></div>',
                    { className: 'tb-marker-label', permanent: true, direction: 'top', offset: marker.tooltipOffset });
            }            
            opMap.markerClusterGroup.addLayer(marker);  //marker.addTo(opMap.map);  //使用markerClusterGroup来统一管理marker
        });

        if (settings.displayTooltip) {
            this.createTooltip(marker, settings.tooltipPattern, settings.tooltipReplaceInfo, settings.autocloseTooltip, markerArgs);
        }

        if (onClickListener) {
            marker.on('click', onClickListener);
        }

        return marker;
    }

    removeMarker(marker) {        
        this.markerClusterGroup.removeLayer(marker);  //this.map.removeLayer(marker);  //使用markerClusterGroup来统一管理marker
    }

    createTooltip(marker, pattern, replaceInfo, autoClose, markerArgs) {
        var popup = L.popup();
        popup.setContent('');
        marker.bindPopup(popup, {autoClose: autoClose, closeOnClick: false});
        this.tooltips.push( {
            markerArgs: markerArgs,
            popup: popup,
            pattern: pattern,
            replaceInfo: replaceInfo
        });
    }

    updatePolylineColor(polyline, settings, color) {
        var style = {
            color: color,
            opacity: settings.strokeOpacity,
            weight: settings.strokeWeight
        };
        polyline.setStyle(style);
    }

    createPolyline(locations, settings) {
        var polyline = L.polyline(locations,
            {
                color: settings.color,
                opacity: settings.strokeOpacity,
                weight: settings.strokeWeight
            }
        ).addTo(this.map);
        return polyline;
    }

    removePolyline(polyline) {
        this.map.removeLayer(polyline);
    }

    fitBounds(bounds) {
        if (bounds.isValid()) {
            if (this.dontFitMapBounds && this.defaultZoomLevel) {
                this.map.setZoom(this.defaultZoomLevel, {animate: false});
                this.map.panTo(bounds.getCenter(), {animate: false});
            } else {
                var tbMap = this;
                this.map.once('zoomend', function() {
                    if (!tbMap.defaultZoomLevel && tbMap.map.getZoom() > tbMap.minZoomLevel) {
                        tbMap.map.setZoom(tbMap.minZoomLevel, {animate: false});
                    }
                });
                this.map.fitBounds(bounds, {padding: [50, 50], animate: false});
            }
        }
    }

    createLatLng(lat, lng) {
        return L.latLng(lat, lng);
    }


    //创建geojson多边形要素，创建地理围栏到地图中
    createDefence(geoJSONObject) {  
        var defenceLayer = L.geoJson(geoJSONObject, {
            style:function (feature) {
            return  {
                        stroke: feature.properties.stroke || true,
                        color: feature.properties.color || '#FF3388',
                        weight: feature.properties.weight || 3,
                        opacity: feature.properties.opacity || 1.0,
                        dashArray: feature.properties.dashArray || '15,10,5',
                        dashOffset: feature.properties.dashOffset || 'null',
                        fill: feature.properties.fill || true,
                        fillColor: feature.properties.fillColor || '*',
                        fillOpacity: feature.properties.fillOpacity || 0.01
                    };
            },
            onEachFeature:function onEachFeature(feature, layer) {
                if(feature.properties && feature.properties.popupContent) {
                    layer.bindPopup(feature.properties.popupContent);
                }else if (feature.properties && feature.properties.name) {
                    layer.bindPopup("地理围栏名称：" + feature.properties.name + "<BR/><a href='http://www.e-u.cn' target='_blank'>查看详细</a> ");
                }else{
                    layer.bindPopup("地理围栏编号："+feature.properties.id +"<BR/><a href='http://www.e-u.cn' target='_blank'>查看详细</a> ");  
                }
            }
        }).addTo(this.map);

        var defenceGroup = new L.FeatureGroup();
        this.map.addLayer(defenceGroup);  
        this.layerControl.addOverlay(defenceGroup, geoJSONObject.name);  

        defenceLayer.eachLayer(
            function (l) {
                defenceGroup.addLayer(l);
            });
    }

    extendBoundsWithMarker(bounds, marker) {
        bounds.extend(marker.getLatLng());
    }

    getMarkerPosition(marker) {
        return marker.getLatLng();
    }

    setMarkerPosition(marker, latLng) {
        marker.setLatLng(latLng);
    }

    getPolylineLatLngs(polyline) {
        return polyline.getLatLngs();
    }

    setPolylineLatLngs(polyline, latLngs) {
        polyline.setLatLngs(latLngs);
    }

    createBounds() {
        return L.latLngBounds();
    }

    extendBounds(bounds, polyline) {
        if (polyline && polyline.getLatLngs()) {
            bounds.extend(polyline.getBounds());
        }
    }

    invalidateSize() {
        this.map.invalidateSize(true);
    }

    getTooltips() {
        return this.tooltips;
    }

}
