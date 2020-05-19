(function (root, factory) {
	if (typeof define === 'function' && define.amd) {
		// AMD. Register as an anonymous module.
		define(['leaflet'], factory);
	} else if (typeof modules === 'object' && module.exports) {
		// define a Common JS module that relies on 'leaflet'
		module.exports = factory(require('leaflet'));
	} else {
		// Assume Leaflet is loaded into global object L already
		factory(L);
	}
}(this, function (L) {
	'use strict';

	L.TileLayer.Provider = L.TileLayer.extend({
		initialize: function (arg, options) {
			var providers = L.TileLayer.Provider.providers;

			var parts = arg.split('.');

			var providerName = parts[0];
			var variantName = parts[1];

			if (!providers[providerName]) {
				throw 'No such provider (' + providerName + ')';
			}

			var provider = {
				url: providers[providerName].url,
				options: providers[providerName].options
			};

			// overwrite values in provider from variant.
			if (variantName && 'variants' in providers[providerName]) {
				if (!(variantName in providers[providerName].variants)) {
					throw 'No such variant of ' + providerName + ' (' + variantName + ')';
				}
				var variant = providers[providerName].variants[variantName];
				var variantOptions;
				if (typeof variant === 'string') {
					variantOptions = {
						variant: variant
					};
				} else {
					variantOptions = variant.options;
				}
				provider = {
					url: variant.url || provider.url,
					options: L.Util.extend({}, provider.options, variantOptions)
				};
			}

			// replace attribution placeholders with their values from toplevel provider attribution,
			// recursively
			var attributionReplacer = function (attr) {
				if (attr.indexOf('{attribution.') === -1) {
					return attr;
				}
				return attr.replace(/\{attribution.(\w*)\}/g,
					function (match, attributionName) {
						return attributionReplacer(providers[attributionName].options.attribution);
					}
				);
			};
			provider.options.attribution = attributionReplacer(provider.options.attribution);

			// Compute final options combining provider options with any user overrides
			var layerOpts = L.Util.extend({}, provider.options, options);
			L.TileLayer.prototype.initialize.call(this, provider.url, layerOpts);
		}
	});

	/**
	 * Definition of providers.
	 * see http://leafletjs.com/reference.html#tilelayer for options in the options map.
	 */

	L.TileLayer.Provider.providers = {
		OpenStreetMap: {
			url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
			options: {
				maxZoom: 19,
				attribution:
					'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
			},
			variants: {
				Mapnik: {},
				DE: {
					url: 'https://{s}.tile.openstreetmap.de/tiles/osmde/{z}/{x}/{y}.png',
					options: {
						maxZoom: 18
					}
				},
				CH: {
					url: 'https://tile.osm.ch/switzerland/{z}/{x}/{y}.png',
					options: {
						maxZoom: 18,
						bounds: [[45, 5], [48, 11]]
					}
				},
				France: {
					url: 'https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png',
					options: {
						maxZoom: 20,
						attribution: '&copy; Openstreetmap France | {attribution.OpenStreetMap}'
					}
				},
				HOT: {
					url: 'https://{s}.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png',
					options: {
						attribution:
							'{attribution.OpenStreetMap}, ' +
							'Tiles style by <a href="https://www.hotosm.org/" target="_blank">Humanitarian OpenStreetMap Team</a> ' +
							'hosted by <a href="https://openstreetmap.fr/" target="_blank">OpenStreetMap France</a>'
					}
				},
				BZH: {
					url: 'https://tile.openstreetmap.bzh/br/{z}/{x}/{y}.png',
					options: {
						attribution: '{attribution.OpenStreetMap}, Tiles courtesy of <a href="http://www.openstreetmap.bzh/" target="_blank">Breton OpenStreetMap Team</a>',
						bounds: [[46.2, -5.5], [50, 0.7]]
					}
				}
			}
		},
		OpenSeaMap: {
			url: 'https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png',
			options: {
				attribution: 'Map data: &copy; <a href="http://www.openseamap.org">OpenSeaMap</a> contributors'
			}
		},
		OpenPtMap: {
			url: 'http://openptmap.org/tiles/{z}/{x}/{y}.png',
			options: {
				maxZoom: 17,
				attribution: 'Map data: &copy; <a href="http://www.openptmap.org">OpenPtMap</a> contributors'
			}
		},
		OpenTopoMap: {
			url: 'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png',
			options: {
				maxZoom: 17,
				attribution: 'Map data: {attribution.OpenStreetMap}, <a href="http://viewfinderpanoramas.org">SRTM</a> | Map style: &copy; <a href="https://opentopomap.org">OpenTopoMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)'
			}
		},
		OpenRailwayMap: {
			url: 'https://{s}.tiles.openrailwaymap.org/standard/{z}/{x}/{y}.png',
			options: {
				maxZoom: 19,
				attribution: 'Map data: {attribution.OpenStreetMap} | Map style: &copy; <a href="https://www.OpenRailwayMap.org">OpenRailwayMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)'
			}
		},
		OpenFireMap: {
			url: 'http://openfiremap.org/hytiles/{z}/{x}/{y}.png',
			options: {
				maxZoom: 19,
				attribution: 'Map data: {attribution.OpenStreetMap} | Map style: &copy; <a href="http://www.openfiremap.org">OpenFireMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)'
			}
		},
		SafeCast: {
			url: 'https://s3.amazonaws.com/te512.safecast.org/{z}/{x}/{y}.png',
			options: {
				maxZoom: 16,
				attribution: 'Map data: {attribution.OpenStreetMap} | Map style: &copy; <a href="https://blog.safecast.org/about/">SafeCast</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)'
			}
		},
		Thunderforest: {
			url: 'https://{s}.tile.thunderforest.com/{variant}/{z}/{x}/{y}.png?apikey={apikey}',
			options: {
				attribution:
					'&copy; <a href="http://www.thunderforest.com/">Thunderforest</a>, {attribution.OpenStreetMap}',
				variant: 'cycle',
				apikey: '<insert your api key here>',
				maxZoom: 22
			},
			variants: {
				OpenCycleMap: 'cycle',
				Transport: {
					options: {
						variant: 'transport'
					}
				},
				TransportDark: {
					options: {
						variant: 'transport-dark'
					}
				},
				SpinalMap: {
					options: {
						variant: 'spinal-map'
					}
				},
				Landscape: 'landscape',
				Outdoors: 'outdoors',
				Pioneer: 'pioneer',
				MobileAtlas: 'mobile-atlas',
				Neighbourhood: 'neighbourhood'
			}
		},
		CyclOSM: {
			url: 'https://dev.{s}.tile.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png',
			options: {
				maxZoom: 20,
				attribution: '<a href="https://github.com/cyclosm/cyclosm-cartocss-style/releases" title="CyclOSM - Open Bicycle render">CyclOSM</a> | Map data: {attribution.OpenStreetMap}'
			}
		},
		OpenMapSurfer: {
			url: 'https://maps.heigit.org/openmapsurfer/tiles/{variant}/webmercator/{z}/{x}/{y}.png',
			options: {
				maxZoom: 19,
				variant: 'roads',
				attribution: 'Imagery from <a href="http://giscience.uni-hd.de/">GIScience Research Group @ University of Heidelberg</a> | Map data '
			},
			variants: {
				Roads: {
					options: {
						variant: 'roads',
						attribution: '{attribution.OpenMapSurfer}{attribution.OpenStreetMap}'
					}
				},
				Hybrid: {
					options: {
						variant: 'hybrid',
						attribution: '{attribution.OpenMapSurfer}{attribution.OpenStreetMap}'
					}
				},
				AdminBounds: {
					options: {
						variant: 'adminb',
						maxZoom: 18,
						attribution: '{attribution.OpenMapSurfer}{attribution.OpenStreetMap}'
					}
				},
				ContourLines: {
					options: {
						variant: 'asterc',
						maxZoom: 18,
						minZoom: 13,
						attribution: '{attribution.OpenMapSurfer} <a href="https://lpdaac.usgs.gov/products/aster_policies">ASTER GDEM</a>'
					}
				},
				Hillshade: {
					options: {
						variant: 'asterh',
						maxZoom: 18,
						attribution: '{attribution.OpenMapSurfer} <a href="https://lpdaac.usgs.gov/products/aster_policies">ASTER GDEM</a>, <a href="http://srtm.csi.cgiar.org/">SRTM</a>'
					}
				},
				ElementsAtRisk: {
					options: {
						variant: 'elements_at_risk',
						attribution: '{attribution.OpenMapSurfer}{attribution.OpenStreetMap}'
					}
				}
			}
		},
		Hydda: {
			url: 'https://{s}.tile.openstreetmap.se/hydda/{variant}/{z}/{x}/{y}.png',
			options: {
				maxZoom: 18,
				variant: 'full',
				attribution: 'Tiles courtesy of <a href="http://openstreetmap.se/" target="_blank">OpenStreetMap Sweden</a> &mdash; Map data {attribution.OpenStreetMap}'
			},
			variants: {
				Full: 'full',
				Base: 'base',
				RoadsAndLabels: 'roads_and_labels'
			}
		},
		MapBox: {
			url: 'https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}{r}.png?access_token={accessToken}',
			options: {
				attribution:
					'<a href="https://www.mapbox.com/about/maps/" target="_blank">&copy; Mapbox</a> ' +
					'{attribution.OpenStreetMap} ' +
					'<a href="https://www.mapbox.com/map-feedback/" target="_blank">Improve this map</a>',
				subdomains: 'abcd',
				id: 'mapbox.streets',
				accessToken: '<insert your access token here>',
			}
		},
		Stamen: {
			url: 'https://stamen-tiles-{s}.a.ssl.fastly.net/{variant}/{z}/{x}/{y}{r}.{ext}',
			options: {
				attribution:
					'Map tiles by <a href="http://stamen.com">Stamen Design</a>, ' +
					'<a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; ' +
					'Map data {attribution.OpenStreetMap}',
				subdomains: 'abcd',
				minZoom: 0,
				maxZoom: 20,
				variant: 'toner',
				ext: 'png'
			},
			variants: {
				Toner: 'toner',
				TonerBackground: 'toner-background',
				TonerHybrid: 'toner-hybrid',
				TonerLines: 'toner-lines',
				TonerLabels: 'toner-labels',
				TonerLite: 'toner-lite',
				Watercolor: {
					url: 'https://stamen-tiles-{s}.a.ssl.fastly.net/{variant}/{z}/{x}/{y}.{ext}',
					options: {
						variant: 'watercolor',
						ext: 'jpg',
						minZoom: 1,
						maxZoom: 16
					}
				},
				Terrain: {
					options: {
						variant: 'terrain',
						minZoom: 0,
						maxZoom: 18
					}
				},
				TerrainBackground: {
					options: {
						variant: 'terrain-background',
						minZoom: 0,
						maxZoom: 18
					}
				},
				TerrainLabels: {
					options: {
						variant: 'terrain-labels',
						minZoom: 0,
						maxZoom: 18
					}
				},
				TopOSMRelief: {
					url: 'https://stamen-tiles-{s}.a.ssl.fastly.net/{variant}/{z}/{x}/{y}.{ext}',
					options: {
						variant: 'toposm-color-relief',
						ext: 'jpg',
						bounds: [[22, -132], [51, -56]]
					}
				},
				TopOSMFeatures: {
					options: {
						variant: 'toposm-features',
						bounds: [[22, -132], [51, -56]],
						opacity: 0.9
					}
				}
			}
		},
		TomTom: {
			url: 'https://{s}.api.tomtom.com/map/1/tile/{variant}/{style}/{z}/{x}/{y}.{ext}?key={apikey}',
			options: {
				variant: 'basic',
				maxZoom: 22,
				attribution:
					'<a href="https://tomtom.com" target="_blank">&copy;  1992 - ' + new Date().getFullYear() + ' TomTom.</a> ',
				subdomains: 'abcd',
				style: 'main',
				ext: 'png',
				apikey: '<insert your API key here>',
			},
			variants: {
				Basic: 'basic',
				Hybrid: 'hybrid',
				Labels: 'labels'
			}
		},
		Esri: {
			url: 'https://server.arcgisonline.com/ArcGIS/rest/services/{variant}/MapServer/tile/{z}/{y}/{x}',
			options: {
				variant: 'World_Street_Map',
				attribution: 'Tiles &copy; Esri'
			},
			variants: {
				WorldStreetMap: {
					options: {
						attribution:
							'{attribution.Esri} &mdash; ' +
							'Source: Esri, DeLorme, NAVTEQ, USGS, Intermap, iPC, NRCAN, Esri Japan, METI, Esri China (Hong Kong), Esri (Thailand), TomTom, 2012'
					}
				},
				DeLorme: {
					options: {
						variant: 'Specialty/DeLorme_World_Base_Map',
						minZoom: 1,
						maxZoom: 11,
						attribution: '{attribution.Esri} &mdash; Copyright: &copy;2012 DeLorme'
					}
				},
				WorldTopoMap: {
					options: {
						variant: 'World_Topo_Map',
						attribution:
							'{attribution.Esri} &mdash; ' +
							'Esri, DeLorme, NAVTEQ, TomTom, Intermap, iPC, USGS, FAO, NPS, NRCAN, GeoBase, Kadaster NL, Ordnance Survey, Esri Japan, METI, Esri China (Hong Kong), and the GIS User Community'
					}
				},
				WorldImagery: {
					options: {
						variant: 'World_Imagery',
						attribution:
							'{attribution.Esri} &mdash; ' +
							'Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community'
					}
				},
				WorldTerrain: {
					options: {
						variant: 'World_Terrain_Base',
						maxZoom: 13,
						attribution:
							'{attribution.Esri} &mdash; ' +
							'Source: USGS, Esri, TANA, DeLorme, and NPS'
					}
				},
				WorldShadedRelief: {
					options: {
						variant: 'World_Shaded_Relief',
						maxZoom: 13,
						attribution: '{attribution.Esri} &mdash; Source: Esri'
					}
				},
				WorldPhysical: {
					options: {
						variant: 'World_Physical_Map',
						maxZoom: 8,
						attribution: '{attribution.Esri} &mdash; Source: US National Park Service'
					}
				},
				OceanBasemap: {
					options: {
						variant: 'Ocean_Basemap',
						maxZoom: 13,
						attribution: '{attribution.Esri} &mdash; Sources: GEBCO, NOAA, CHS, OSU, UNH, CSUMB, National Geographic, DeLorme, NAVTEQ, and Esri'
					}
				},
				NatGeoWorldMap: {
					options: {
						variant: 'NatGeo_World_Map',
						maxZoom: 16,
						attribution: '{attribution.Esri} &mdash; National Geographic, Esri, DeLorme, NAVTEQ, UNEP-WCMC, USGS, NASA, ESA, METI, NRCAN, GEBCO, NOAA, iPC'
					}
				},
				WorldGrayCanvas: {
					options: {
						variant: 'Canvas/World_Light_Gray_Base',
						maxZoom: 16,
						attribution: '{attribution.Esri} &mdash; Esri, DeLorme, NAVTEQ'
					}
				}
			}
		},
		OpenWeatherMap: {
			url: 'http://{s}.tile.openweathermap.org/map/{variant}/{z}/{x}/{y}.png?appid={apiKey}',
			options: {
				maxZoom: 19,
				attribution: 'Map data &copy; <a href="http://openweathermap.org">OpenWeatherMap</a>',
				apiKey:'<insert your api key here>',
				opacity: 0.5
			},
			variants: {
				Clouds: 'clouds',
				CloudsClassic: 'clouds_cls',
				Precipitation: 'precipitation',
				PrecipitationClassic: 'precipitation_cls',
				Rain: 'rain',
				RainClassic: 'rain_cls',
				Pressure: 'pressure',
				PressureContour: 'pressure_cntr',
				Wind: 'wind',
				Temperature: 'temp',
				Snow: 'snow'
			}
		},
		HERE: {
			/*
			 * HERE maps, formerly Nokia maps.
			 * These basemaps are free, but you need an API key. Please sign up at
			 * https://developer.here.com/plans
			 */
			url:
				'https://{s}.{base}.maps.api.here.com/maptile/2.1/' +
				'{type}/{mapID}/{variant}/{z}/{x}/{y}/{size}/{format}?' +
				'app_id={app_id}&app_code={app_code}&lg={language}',
			options: {
				attribution:
					'Map &copy; 1987-' + new Date().getFullYear() + ' <a href="http://developer.here.com">HERE</a>',
				subdomains: '1234',
				mapID: 'newest',
				'app_id': '<insert your app_id here>',
				'app_code': '<insert your app_code here>',
				base: 'base',
				variant: 'normal.day',
				maxZoom: 20,
				type: 'maptile',
				language: 'eng',
				format: 'png8',
				size: '256'
			},
			variants: {
				normalDay: 'normal.day',
				normalDayCustom: 'normal.day.custom',
				normalDayGrey: 'normal.day.grey',
				normalDayMobile: 'normal.day.mobile',
				normalDayGreyMobile: 'normal.day.grey.mobile',
				normalDayTransit: 'normal.day.transit',
				normalDayTransitMobile: 'normal.day.transit.mobile',
				normalDayTraffic: {
					options: {
						variant: 'normal.traffic.day',
						base: 'traffic',
						type: 'traffictile'
					}
				},
				normalNight: 'normal.night',
				normalNightMobile: 'normal.night.mobile',
				normalNightGrey: 'normal.night.grey',
				normalNightGreyMobile: 'normal.night.grey.mobile',
				normalNightTransit: 'normal.night.transit',
				normalNightTransitMobile: 'normal.night.transit.mobile',
				reducedDay: 'reduced.day',
				reducedNight: 'reduced.night',
				basicMap: {
					options: {
						type: 'basetile'
					}
				},
				mapLabels: {
					options: {
						type: 'labeltile',
						format: 'png'
					}
				},
				trafficFlow: {
					options: {
						base: 'traffic',
						type: 'flowtile'
					}
				},
				carnavDayGrey: 'carnav.day.grey',
				hybridDay: {
					options: {
						base: 'aerial',
						variant: 'hybrid.day'
					}
				},
				hybridDayMobile: {
					options: {
						base: 'aerial',
						variant: 'hybrid.day.mobile'
					}
				},
				hybridDayTransit: {
					options: {
						base: 'aerial',
						variant: 'hybrid.day.transit'
					}
				},
				hybridDayGrey: {
					options: {
						base: 'aerial',
						variant: 'hybrid.grey.day'
					}
				},
				hybridDayTraffic: {
					options: {
						variant: 'hybrid.traffic.day',
						base: 'traffic',
						type: 'traffictile'
					}
				},
				pedestrianDay: 'pedestrian.day',
				pedestrianNight: 'pedestrian.night',
				satelliteDay: {
					options: {
						base: 'aerial',
						variant: 'satellite.day'
					}
				},
				terrainDay: {
					options: {
						base: 'aerial',
						variant: 'terrain.day'
					}
				},
				terrainDayMobile: {
					options: {
						base: 'aerial',
						variant: 'terrain.day.mobile'
					}
				}
			}
		},
		FreeMapSK: {
			url: 'http://t{s}.freemap.sk/T/{z}/{x}/{y}.jpeg',
			options: {
				minZoom: 8,
				maxZoom: 16,
				subdomains: '1234',
				bounds: [[47.204642, 15.996093], [49.830896, 22.576904]],
				attribution:
					'{attribution.OpenStreetMap}, vizualization CC-By-SA 2.0 <a href="http://freemap.sk">Freemap.sk</a>'
			}
		},
		MtbMap: {
			url: 'http://tile.mtbmap.cz/mtbmap_tiles/{z}/{x}/{y}.png',
			options: {
				attribution:
					'{attribution.OpenStreetMap} &amp; USGS'
			}
		},
		CartoDB: {
			url: 'https://{s}.basemaps.cartocdn.com/{variant}/{z}/{x}/{y}{r}.png',
			options: {
				attribution: '{attribution.OpenStreetMap} &copy; <a href="https://carto.com/attributions">CARTO</a>',
				subdomains: 'abcd',
				maxZoom: 19,
				variant: 'light_all'
			},
			variants: {
				Positron: 'light_all',
				PositronNoLabels: 'light_nolabels',
				PositronOnlyLabels: 'light_only_labels',
				DarkMatter: 'dark_all',
				DarkMatterNoLabels: 'dark_nolabels',
				DarkMatterOnlyLabels: 'dark_only_labels',
				Voyager: 'rastertiles/voyager',
				VoyagerNoLabels: 'rastertiles/voyager_nolabels',
				VoyagerOnlyLabels: 'rastertiles/voyager_only_labels',
				VoyagerLabelsUnder: 'rastertiles/voyager_labels_under'
			}
		},
		HikeBike: {
			url: 'https://tiles.wmflabs.org/{variant}/{z}/{x}/{y}.png',
			options: {
				maxZoom: 19,
				attribution: '{attribution.OpenStreetMap}',
				variant: 'hikebike'
			},
			variants: {
				HikeBike: {},
				HillShading: {
					options: {
						maxZoom: 15,
						variant: 'hillshading'
					}
				}
			}
		},
		BasemapAT: {
			url: 'https://maps{s}.wien.gv.at/basemap/{variant}/{type}/google3857/{z}/{y}/{x}.{format}',
			options: {
				maxZoom: 19,
				attribution: 'Datenquelle: <a href="https://www.basemap.at">basemap.at</a>',
				subdomains: ['', '1', '2', '3', '4'],
				type: 'normal',
				format: 'png',
				bounds: [[46.358770, 8.782379], [49.037872, 17.189532]],
				variant: 'geolandbasemap'
			},
			variants: {
				basemap: {
					options: {
						maxZoom: 20, // currently only in Vienna
						variant: 'geolandbasemap'
					}
				},
				grau: 'bmapgrau',
				overlay: 'bmapoverlay',
				terrain: {
					options: {
						variant: 'bmapgelaende',
						type: 'grau',
						format: 'jpeg'
					}
				},
				surface: {
					options: {
						variant: 'bmapoberflaeche',
						type: 'grau',
						format: 'jpeg'
					}
				},
				highdpi: {
					options: {
						variant: 'bmaphidpi',
						format: 'jpeg'
					}
				},
				orthofoto: {
					options: {
						maxZoom: 20, // currently only in Vienna
						variant: 'bmaporthofoto30cm',
						format: 'jpeg'
					}
				}
			}
		},
		nlmaps: {
			url: 'https://geodata.nationaalgeoregister.nl/tiles/service/wmts/{variant}/EPSG:3857/{z}/{x}/{y}.png',
			options: {
				minZoom: 6,
				maxZoom: 19,
				bounds: [[50.5, 3.25], [54, 7.6]],
				attribution: 'Kaartgegevens &copy; <a href="kadaster.nl">Kadaster</a>'
			},
			variants: {
				'standaard': 'brtachtergrondkaart',
				'pastel': 'brtachtergrondkaartpastel',
				'grijs': 'brtachtergrondkaartgrijs',
				'luchtfoto': {
					'url': 'https://geodata.nationaalgeoregister.nl/luchtfoto/rgb/wmts/2018_ortho25/EPSG:3857/{z}/{x}/{y}.png',
				}
			}
		},
		NASAGIBS: {
			url: 'https://map1.vis.earthdata.nasa.gov/wmts-webmerc/{variant}/default/{time}/{tilematrixset}{maxZoom}/{z}/{y}/{x}.{format}',
			options: {
				attribution:
					'Imagery provided by services from the Global Imagery Browse Services (GIBS), operated by the NASA/GSFC/Earth Science Data and Information System ' +
					'(<a href="https://earthdata.nasa.gov">ESDIS</a>) with funding provided by NASA/HQ.',
				bounds: [[-85.0511287776, -179.999999975], [85.0511287776, 179.999999975]],
				minZoom: 1,
				maxZoom: 9,
				format: 'jpg',
				time: '',
				tilematrixset: 'GoogleMapsCompatible_Level'
			},
			variants: {
				ModisTerraTrueColorCR: 'MODIS_Terra_CorrectedReflectance_TrueColor',
				ModisTerraBands367CR: 'MODIS_Terra_CorrectedReflectance_Bands367',
				ViirsEarthAtNight2012: {
					options: {
						variant: 'VIIRS_CityLights_2012',
						maxZoom: 8
					}
				},
				ModisTerraLSTDay: {
					options: {
						variant: 'MODIS_Terra_Land_Surface_Temp_Day',
						format: 'png',
						maxZoom: 7,
						opacity: 0.75
					}
				},
				ModisTerraSnowCover: {
					options: {
						variant: 'MODIS_Terra_Snow_Cover',
						format: 'png',
						maxZoom: 8,
						opacity: 0.75
					}
				},
				ModisTerraAOD: {
					options: {
						variant: 'MODIS_Terra_Aerosol',
						format: 'png',
						maxZoom: 6,
						opacity: 0.75
					}
				},
				ModisTerraChlorophyll: {
					options: {
						variant: 'MODIS_Terra_Chlorophyll_A',
						format: 'png',
						maxZoom: 7,
						opacity: 0.75
					}
				}
			}
		},
		NLS: {
			// NLS maps are copyright National library of Scotland.
			// http://maps.nls.uk/projects/api/index.html
			// Please contact NLS for anything other than non-commercial low volume usage
			//
			// Map sources: Ordnance Survey 1:1m to 1:63K, 1920s-1940s
			//   z0-9  - 1:1m
			//  z10-11 - quarter inch (1:253440)
			//  z12-18 - one inch (1:63360)
			url: 'https://nls-{s}.tileserver.com/nls/{z}/{x}/{y}.jpg',
			options: {
				attribution: '<a href="http://geo.nls.uk/maps/">National Library of Scotland Historic Maps</a>',
				bounds: [[49.6, -12], [61.7, 3]],
				minZoom: 1,
				maxZoom: 18,
				subdomains: '0123',
			}
		},
		JusticeMap: {
			// Justice Map (http://www.justicemap.org/)
			// Visualize race and income data for your community, county and country.
			// Includes tools for data journalists, bloggers and community activists.
			url: 'http://www.justicemap.org/tile/{size}/{variant}/{z}/{x}/{y}.png',
			options: {
				attribution: '<a href="http://www.justicemap.org/terms.php">Justice Map</a>',
				// one of 'county', 'tract', 'block'
				size: 'county',
				// Bounds for USA, including Alaska and Hawaii
				bounds: [[14, -180], [72, -56]]
			},
			variants: {
				income: 'income',
				americanIndian: 'indian',
				asian: 'asian',
				black: 'black',
				hispanic: 'hispanic',
				multi: 'multi',
				nonWhite: 'nonwhite',
				white: 'white',
				plurality: 'plural'
			}
		},
		Wikimedia: {
			url: 'https://maps.wikimedia.org/osm-intl/{z}/{x}/{y}{r}.png',
			options: {
				attribution: '<a href="https://wikimediafoundation.org/wiki/Maps_Terms_of_Use">Wikimedia</a>',
				minZoom: 1,
				maxZoom: 19
			}
		},
		GeoportailFrance: {
			url: 'https://wxs.ign.fr/{apikey}/geoportail/wmts?REQUEST=GetTile&SERVICE=WMTS&VERSION=1.0.0&STYLE={style}&TILEMATRIXSET=PM&FORMAT={format}&LAYER={variant}&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}',
			options: {
				attribution: '<a target="_blank" href="https://www.geoportail.gouv.fr/">Geoportail France</a>',
				bounds: [[-75, -180], [81, 180]],
				minZoom: 2,
				maxZoom: 18,
				// Get your own geoportail apikey here : http://professionnels.ign.fr/ign/contrats/
				// NB : 'choisirgeoportail' is a demonstration key that comes with no guarantee
				apikey: 'choisirgeoportail',
				format: 'image/jpeg',
				style : 'normal',
				variant: 'GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD'
			},
			variants: {
				parcels: {
					options : {
						variant: 'CADASTRALPARCELS.PARCELS',
						maxZoom: 20,
						style : 'bdparcellaire',
						format: 'image/png'
					}
				},
				ignMaps: 'GEOGRAPHICALGRIDSYSTEMS.MAPS',
				maps: 'GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD',
				orthos: {
					options: {
						maxZoom: 19,
						variant: 'ORTHOIMAGERY.ORTHOPHOTOS'
					}
				}
			}
		},
		OneMapSG: {
			url: 'https://maps-{s}.onemap.sg/v3/{variant}/{z}/{x}/{y}.png',
			options: {
				variant: 'Default',
				minZoom: 11,
				maxZoom: 18,
				bounds: [[1.56073, 104.11475], [1.16, 103.502]],
				attribution: '<img src="https://docs.onemap.sg/maps/images/oneMap64-01.png" style="height:20px;width:20px;"/> New OneMap | Map data &copy; contributors, <a href="http://SLA.gov.sg">Singapore Land Authority</a>'
			},
			variants: {
				Default: 'Default',
				Night: 'Night',
				Original: 'Original',
				Grey: 'Grey',
				LandLot: 'LandLot'
			}
		}
	};

	L.tileLayer.provider = function (provider, options) {
		return new L.TileLayer.Provider(provider, options);
	};

	return L;
}));
