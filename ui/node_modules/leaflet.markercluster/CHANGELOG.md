Leaflet.markercluster
=====================

(all changes without author notice are by [@danzel](https://github.com/danzel))

## 1.4.1 (2018-09-14)

### Bugfixes

 * Better support stopping dragged markers from being clustered (by [@DerGuteWolf](https://github.com/DerGuteWolf))  [#909](https://github.com/Leaflet/Leaflet.markercluster/pull/909)

## 1.4.0 (2018-08-22)

Required leaflet version bumped to 1.3.1

### Improvements

 * Tests run against leaflet 1.1.0+ [#863](https://github.com/Leaflet/Leaflet.markercluster/issues/863)

### Bugfixes

 * Fix clearLayers not removing removed markers [#860](https://github.com/Leaflet/Leaflet.markercluster/issues/860)
 * Remember opacity 0 correctly (by [@r-yanyo](https://github.com/r-yanyo)) [#890](https://github.com/Leaflet/Leaflet.markercluster/pull/890)
 * Fix chunkedLoading LatLngBounds.intersects() (by [@boldtrn](https://github.com/boldtrn))  [#743](https://github.com/Leaflet/Leaflet.markercluster/issues/743) [#891](https://github.com/Leaflet/Leaflet.markercluster/pull/891)
 
## 1.3.0 (2018-01-19)

### Improvements

 * Use Rollup for builds (by [@IvanSanchez](https://github.com/IvanSanchez)) [#769](https://github.com/Leaflet/Leaflet.markercluster/pull/769)

### Bugfixes

 * Fix Spiderfier positioning for small markers (by [@ghybs](https://github.com/ghybs)) [#846](https://github.com/Leaflet/Leaflet.markercluster/pull/846)
 * Fix anchor usage with latest leaflet version [#861](https://github.com/Leaflet/Leaflet.markercluster/issues/861)

## 1.2.0 (2017-11-06)

### Improvements

 * Move `clusterPane` option in to `options` field (by [@ghybs](https://github.com/ghybs)) [#832](https://github.com/Leaflet/Leaflet.markercluster/pull/832)

### Bugfixes

 * Fix very small `maxClusterRadius` hanging the browser (by [@lucaswerkmeister](https://github.com/lucaswerkmeister)) [#838](https://github.com/Leaflet/Leaflet.markercluster/pull/838)

## 1.1.0 (2017-08-27)

### Improvements

 * Add `clusterPane` option to allow putting clusters in a different pane (by [@ckrahe](https://github.com/ckrahe)) [#819](https://github.com/Leaflet/Leaflet.markercluster/issues/819)
 
## 1.0.6 (2017-06-19)

### Bugfixes

 * Fix some issues when used with non-integer zoom [#789](https://github.com/Leaflet/Leaflet.markercluster/issues/789)
 * Change examples to use https (by [@ghybs](https://github.com/ghybs)) [#794](https://github.com/Leaflet/Leaflet.markercluster/pull/794)

## 1.0.5 (2017-04-26)

### Improvements

 * Allow passing fitBounds options to zoomToBounds (by [@timkelty](https://github.com/timkelty)) [#779](https://github.com/Leaflet/Leaflet.markercluster/pull/779)

### Bugfixes

 * Fixed bug where disableClusteringAtZoom being 0 is treated the same as null (by [@MrCheeze](https://github.com/MrCheeze)) [#773](https://github.com/Leaflet/Leaflet.markercluster/pull/773)

## 1.0.4 (2017-03-14)

### Bugfixes

 * Fix errors removing a MarkerClusterGroup from the map during an animation [#758](https://github.com/Leaflet/Leaflet.markercluster/issues/758)

## 1.0.3 (2017-02-02)

### Bugfixes

 * Fix moving markers while the MarkerClusterGroup is not on the map [#753](https://github.com/Leaflet/Leaflet.markercluster/issues/753)

## 1.0.2 (2017-01-27)

### Improvements

 * Support `layeradd` and `layerremove` events [#647](https://github.com/Leaflet/Leaflet.markercluster/issues/647)

### Bugfixes

 * Add support for maps with negative minZoom [#704](https://github.com/Leaflet/Leaflet.markercluster/issues/704)
 * Fixed zoomToShowLayer() markers disappearing bug (by [@z3ut](https://github.com/z3ut)) [#739](https://github.com/Leaflet/Leaflet.markercluster/issues/739)
 * Fix an issue when opening a popup inside of zoomToShowLayer
 * If a marker is moved with an open popup on it, re-open the popup after moving it. [#651](https://github.com/Leaflet/Leaflet.markercluster/issues/651)


## 1.0.1 (2017-01-25)

### Improvements

 * Add install and build steps with jake (by [@kazes](https://github.com/kazes)) [#733](https://github.com/Leaflet/Leaflet.markercluster/pull/733)
 * Readme improvements (by [@ghybs](https://github.com/ghybs), [@bertyhell](https://github.com/bertyhell)) [#734](https://github.com/Leaflet/Leaflet.markercluster/pull/738), [#734](https://github.com/Leaflet/Leaflet.markercluster/pull/738
 * Bump all examples to leaflet 1.0.3

### Bugfixes

 * Fixed leaflet 1.0.2 bug where clearLayers would throw an exception (by [@marcianoviereck92](https://github.com/marcianoviereck92)) [#746](https://github.com/Leaflet/Leaflet.markercluster/pull/746)


## 1.0.0 (2016-10-03)

### Improvements

 * Compatibility with Leaflet 1.0.0 (by [@danzel](https://githum.com/danzel), [@Eschon](https://github.com/Eschon), [@ghybs](https://github.com/ghybs), [@IvanSanchez](https://github.com/IvanSanchez))
 * Support moving markers [#57](https://github.com/Leaflet/Leaflet.markercluster/issues/57)
 * chunkedLoading option to keep browser more responsive during larging a load data set [#292](https://github.com/Leaflet/Leaflet.markercluster/issues/292)
 * maxClusterRadius can be a function (by [@Schwanksta](https://github.com/Schwanksta)) [#298](https://github.com/Leaflet/Leaflet.markercluster/issues/298)
 * Spiderfy without zooming when all markers at same location (by [@rdenniston](https://github.com/rdenniston), [@ghybs](https://github.com/ghybs)) [#415](https://github.com/Leaflet/Leaflet.markercluster/issues/415), [#606](https://github.com/Leaflet/Leaflet.markercluster/issues/606)
 * On becoming visible, markers retain their original opacity. (by [@IvanSanchez](https://github.com/IvanSanchez)) [#444](https://github.com/Leaflet/Leaflet.markercluster/issues/444)
 * Spiderleg Polyline options (by [@mikeatlas](https://github.com/mikeatlas)) [#466](https://github.com/Leaflet/Leaflet.markercluster/issues/466)
 * Extra methods to allow refreshing cluster icons (by [@ghybs](https://github.com/ghybs)) [#564](https://github.com/Leaflet/Leaflet.markercluster/issues/564)
 * Ability to disable animations (by [@ghybs](https://github.com/ghybs)) [#578](https://github.com/Leaflet/Leaflet.markercluster/issues/578)
 * Optimized performance of bulk addLayers and removeLayers (by [@ghybs](https://github.com/ghybs)) [#584](https://github.com/Leaflet/Leaflet.markercluster/issues/584)
 * Replaced spiderfy legs animation from SMIL to CSS transition (by [@ghybs](https://github.com/ghybs)) [#585](https://github.com/Leaflet/Leaflet.markercluster/issues/585)
 * Provide more detailed context information on the spiderfied event (by [@evanvosberg](https://github.com/evanvosberg)) [#421](https://github.com/Leaflet/Leaflet.markercluster/issues/421)
 * Add unspiderfied event
 * Readme updates (by [@ghybs](https://github.com/ghybs), [@tomchadwin](https://github.com/tomchadwin) [@Cyrille37](https://github.com/Cyrille37) [@franckl](https://github.com/franckl) [@mikeatlas](https://github.com/mikeatlas) 
 [@rdenniston](https://github.com/rdenniston) [@maackle](https://github.com/maackle) [@fureigh](https://github.com/fureigh) [@Wildhoney](https://github.com/Wildhoney) [@Schwanksta](https://github.com/Schwanksta) [@frankrowe](https://github.com/frankrowe))
 * Improve adding and removing nested LayerGroups (by [@ghybs](https://github.com/ghybs)) [#624](https://github.com/Leaflet/Leaflet.markercluster/pull/624)
 * Add public unspiderfy method (by [@zverev](https://github.com/zverev)) [#617](https://github.com/Leaflet/Leaflet.markercluster/pull/617)
 * Optimized performance of bulk add with complex icon create function (by [@mlazowik](https://github.com/mlazowik)) [#697](https://github.com/Leaflet/Leaflet.markercluster/pull/697)
 * Remove leaflet from peerDependencies (by [@tyleralves](https://github.com/tyleralves)) [#703](https://github.com/Leaflet/Leaflet.markercluster/pull/703)
 * Simplified _recursively (by [@ghybs](https://github.com/ghybs)) [#656](https://github.com/Leaflet/Leaflet.markercluster/pull/656)

### Bugfixes

 * Fix getBounds when removeOutsideVisibleBounds: false is set. [#321](https://github.com/Leaflet/Leaflet.markercluster/issues/321)
 * Fix zoomToShowLayer fails after initial spiderfy [#286](https://github.com/Leaflet/Leaflet.markercluster/issues/286)
 * Fix cluster not disappearing on Android [#344](https://github.com/Leaflet/Leaflet.markercluster/issues/344)
 * Fix RemoveLayers() when spiderified (by [@Grsmto](https://github.com/Grsmto)) [#358](https://github.com/Leaflet/Leaflet.markercluster/issues/358)
 * Remove lines from map when removing cluster (by [@olive380](https://github.com/olive380)) [#532](https://github.com/Leaflet/Leaflet.markercluster/issues/532)
 * Fix getConvexHull when all markers are located at same latitude (by [@olive380](https://github.com/olive380)) [#533](https://github.com/Leaflet/Leaflet.markercluster/issues/533)
 * Fix removeLayers when cluster is not on the map (by [@eschon](https://github.com/eschon)) [#556](https://github.com/Leaflet/Leaflet.markercluster/issues/556)
 * Improved zoomToShowLayer with callback check (by [@ghybs](https://github.com/ghybs)) [#572](https://github.com/Leaflet/Leaflet.markercluster/issues/572)
 * Improved reliability of RefreshSpec test suite for PhantomJS (by [@ghybs](https://github.com/ghybs)) [#577](https://github.com/Leaflet/Leaflet.markercluster/issues/577)
 * Corrected effect of removeOutsideVisibleBounds option (by [@ghybs](https://github.com/ghybs)) [#575](https://github.com/Leaflet/Leaflet.markercluster/issues/575)
 * Fix getLayer when provided a string [#531](https://github.com/Leaflet/Leaflet.markercluster/issues/531)
 * Documentation improvements (by [@ghybs](https://github.com/ghybs)) [#579](https://github.com/Leaflet/Leaflet.markercluster/issues/579)
 * Correct _getExpandedVisibleBounds for Max Latitude (by [@ghybs](https://github.com/ghybs)) [#587](https://github.com/Leaflet/Leaflet.markercluster/issues/587)
 * Correct unspiderfy vector (by [@ghybs](https://github.com/ghybs)) [#604](https://github.com/Leaflet/Leaflet.markercluster/issues/604)
 * Remove "leaflet-cluster-anim" class on map remove while spiderfied (by [@ghybs](https://github.com/ghybs)) [#607](https://github.com/Leaflet/Leaflet.markercluster/issues/607)
 * Fix disableClusteringAtZoom maxZoom troubles (by [@OriginalSin](https://github.com/OriginalSin)) [#609](https://github.com/Leaflet/Leaflet.markercluster/issues/609)
 * Fix clusters not disappearing when they were near the edge on mobile (by [@ghybs](https://github.com/ghybs)) [#529](https://github.com/Leaflet/Leaflet.markercluster/issues/529)
 * Remove leaflet from dependencies (by [@ghybs](https://github.com/ghybs)) [#639](https://github.com/Leaflet/Leaflet.markercluster/issues/639)
 * Fix interaction between zoomOrSpiderfy and disableClusteringAtZoom (by [@ghybs](https://github.com/ghybs)) [#633](https://github.com/Leaflet/Leaflet.markercluster/issues/633) [#648](https://github.com/Leaflet/Leaflet.markercluster/issues/648)


## 0.4 (2013-12-19)

### Improvements

 * Fix Quick Zoom in/out causing everything to disappear in Firefox (Reported by [@paulovieira](https://github.com/paulovieira)) [#140](https://github.com/Leaflet/Leaflet.markercluster/issues/140)
 * Slow the expand/contract animation down from 200ms to 300ms

### Bugfixes

 * Fix some cases zoomToShowLayer wouldn't work  (Reported by [@absemetov](https://github.com/absemetov)) [#203](https://github.com/Leaflet/Leaflet.markercluster/issues/203) [#228](https://github.com/Leaflet/Leaflet.markercluster/issues/228) [#286](https://github.com/Leaflet/Leaflet.markercluster/issues/286)


## 0.3 (2013-12-18)

### Improvements

 * Work better with custom projections (by [@andersarstrand](https://github.com/andersarstrand)) [#74](https://github.com/Leaflet/Leaflet.markercluster/issues/74)
 * Add custom getBounds that works (Reported by [@2803media](https://github.com/2803media))
 * Allow spacing spiderfied icons further apart (Reported by [@stevevance](https://github.com/stevevance)) [#100](https://github.com/Leaflet/Leaflet.markercluster/issues/100)
 * Add custom eachLayer that works (Reported by [@cilogi](https://github.com/cilogi)) [#102](https://github.com/Leaflet/Leaflet.markercluster/issues/102)
 * Add an option (removeOutsideVisibleBounds) to prevent removing clusters that are outside of the visible bounds (by [@wildhoney](https://github.com/wildhoney)) [#103](https://github.com/Leaflet/Leaflet.markercluster/issues/103)
 * Add getBounds method to cluster (Reported by [@nderambure](https://github.com/nderambure)) [#88](https://github.com/Leaflet/Leaflet.markercluster/issues/88)
 * Lots of unit tests
 * Support having Circle / CircleMarker as child markers
 * Add factory methods (Reported by [@mourner](https://github.com/mourner)) [#21](https://github.com/Leaflet/Leaflet.markercluster/issues/21)
 * Add getVisibleParent method to allow getting the visible parent cluster or the marker if it is visible. (By [@littleiffel](https://github.com/littleiffel)) [#102](https://github.com/Leaflet/Leaflet.markercluster/issues/102)
 * Allow adding non-clusterable things to a MarkerClusterGroup, we don't cluster them. (Reported by [@benbalter](https://github.com/benbalter)) [#195](https://github.com/Leaflet/Leaflet.markercluster/issues/195)
 * removeLayer supports taking a FeatureGroup (Reported by [@pabloalcaraz](https://github.com/pabloalcaraz)) [#236](https://github.com/Leaflet/Leaflet.markercluster/issues/236)
 * DistanceGrid tests, QuickHull tests and improvements (By [@tmcw](https://github.com/tmcw)) [#247](https://github.com/Leaflet/Leaflet.markercluster/issues/247) [#248](https://github.com/Leaflet/Leaflet.markercluster/issues/248) [#249](https://github.com/Leaflet/Leaflet.markercluster/issues/249)
 * Implemented getLayers (Reported by [@metajungle](https://github.com/metajungle)) [#222](https://github.com/Leaflet/Leaflet.markercluster/issues/222)
 * zoomToBounds now only zooms in as far as it needs to to get all of the markers on screen if this is less zoom than zooming to the actual bounds would be (Reported by [@adamyonk](https://github.com/adamyonk)) [#185](https://github.com/Leaflet/Leaflet.markercluster/issues/185)
 * Keyboard accessibility improvements (By [@Zombienaute](https://github.com/Zombienaute)) [#273](https://github.com/Leaflet/Leaflet.markercluster/issues/273)
 * IE Specific css in the default styles is no longer a separate file (By [@frankrowe](https://github.com/frankrowe)) [#280](https://github.com/Leaflet/Leaflet.markercluster/issues/280)
 * Improve usability with small maps (Reported by [@JSCSJSCS](https://github.com/JSCSJSCS)) [#144](https://github.com/Leaflet/Leaflet.markercluster/issues/144)
 * Implement FeatureGroup.getLayer (Reported by [@newmanw](https://github.com/newmanw)) [#244](https://github.com/Leaflet/Leaflet.markercluster/issues/244)

### Bugfixes

 * Fix singleMarkerMode when you aren't on the map (by [@duncanparkes](https://github.com/duncanparkes)) [#77](https://github.com/Leaflet/Leaflet.markercluster/issues/77)
 * Fix clearLayers when you aren't on the map (by [@duncanparkes](https://github.com/duncanparkes)) [#79](https://github.com/Leaflet/Leaflet.markercluster/issues/79)
 * IE10 Bug fix (Reported by [@theLundquist](https://github.com/theLundquist)) [#86](https://github.com/Leaflet/Leaflet.markercluster/issues/86)
 * Fixes for hasLayer after removing a layer (Reported by [@cvisto](https://github.com/cvisto)) [#44](https://github.com/Leaflet/Leaflet.markercluster/issues/44)
 * Fix clearLayers not unsetting __parent of the markers, preventing them from being re-added. (Reported by [@apuntovanini](https://github.com/apuntovanini)) [#99](https://github.com/Leaflet/Leaflet.markercluster/issues/99)
 * Fix map.removeLayer(markerClusterGroup) not working (Reported by [@Driklyn](https://github.com/Driklyn)) [#108](https://github.com/Leaflet/Leaflet.markercluster/issues/108)
 * Fix map.addLayers not updating cluster icons (Reported by [@Driklyn](https://github.com/Driklyn)) [#114](https://github.com/Leaflet/Leaflet.markercluster/issues/114)
 * Fix spiderfied clusters breaking if a marker is added to them (Reported by [@Driklyn](https://github.com/Driklyn)) [#114](https://github.com/Leaflet/Leaflet.markercluster/issues/114)
 * Don't show coverage for spiderfied clusters as it will be wrong. (Reported by [@ajbeaven](https://github.com/ajbeaven)) [#95](https://github.com/Leaflet/Leaflet.markercluster/issues/95)
 * Improve zoom in/out immediately making all everything disappear, still issues in Firefox [#140](https://github.com/Leaflet/Leaflet.markercluster/issues/140)
 * Fix animation not stopping with only one marker. (Reported by [@Driklyn](https://github.com/Driklyn)) [#146](https://github.com/Leaflet/Leaflet.markercluster/issues/146)
 * Various fixes for new leaflet (Reported by [@PeterAronZentai](https://github.com/PeterAronZentai)) [#159](https://github.com/Leaflet/Leaflet.markercluster/issues/159)
 * Fix clearLayers when we are spiderfying (Reported by [@skullbooks](https://github.com/skullbooks)) [#162](https://github.com/Leaflet/Leaflet.markercluster/issues/162)
 * Fix removing layers in certain situations (Reported by [@bpavot](https://github.com/bpavot)) [#160](https://github.com/Leaflet/Leaflet.markercluster/issues/160)
 * Support calling hasLayer with null (by [@l0c0luke](https://github.com/l0c0luke)) [#170](https://github.com/Leaflet/Leaflet.markercluster/issues/170)
 * Lots of fixes for removing a MarkerClusterGroup from the map (Reported by [@annetdeboer](https://github.com/annetdeboer)) [#200](https://github.com/Leaflet/Leaflet.markercluster/issues/200)
 * Throw error when being added to a map with no maxZoom.
 * Fixes for markers not appearing after a big zoom (Reported by [@arnoldbird](https://github.com/annetdeboer)) [#216](https://github.com/Leaflet/Leaflet.markercluster/issues/216) (Reported by [@mathilde-pellerin](https://github.com/mathilde-pellerin)) [#260](https://github.com/Leaflet/Leaflet.markercluster/issues/260)
 * Fix coverage polygon not being removed when a MarkerClusterGroup is removed (Reported by [@ZeusTheTrueGod](https://github.com/ZeusTheTrueGod)) [#245](https://github.com/Leaflet/Leaflet.markercluster/issues/245)
 * Fix getVisibleParent when no parent is visible (Reported by [@ajbeaven](https://github.com/ajbeaven)) [#265](https://github.com/Leaflet/Leaflet.markercluster/issues/265)
 * Fix spiderfied markers not hiding on a big zoom (Reported by [@Vaesive](https://github.com/Vaesive)) [#268](https://github.com/Leaflet/Leaflet.markercluster/issues/268)
 * Fix clusters not hiding on a big zoom (Reported by [@versusvoid](https://github.com/versusvoid)) [#281](https://github.com/Leaflet/Leaflet.markercluster/issues/281)
 * Don't fire multiple clustermouseover/off events due to child divs in the cluster marker (Reported by [@heidemn](https://github.com/heidemn)) [#252](https://github.com/Leaflet/Leaflet.markercluster/issues/252)

## 0.2 (2012-10-11)

### Improvements

 * Add addLayers/removeLayers bulk add and remove functions that perform better than the individual methods
 * Allow customising the polygon generated for showing the area a cluster covers (by [@yohanboniface](https://github.com/yohanboniface)) [#68](https://github.com/Leaflet/Leaflet.markercluster/issues/68)
 * Add zoomToShowLayer method to zoom down to a marker then call a callback once it is visible
 * Add animateAddingMarkers to allow disabling animations caused when adding/removing markers
 * Add hasLayer
 * Pass the L.MarkerCluster to iconCreateFunction to give more flexibility deciding the icon
 * Make addLayers support geojson layers
 * Allow disabling clustering at a given zoom level
 * Allow styling markers that are added like they were clusters of size 1


### Bugfixes

 * Support when leaflet is configured to use canvas rather than SVG
 * Fix some potential crashes in zoom handlers
 * Tidy up when we are removed from the map

## 0.1 (2012-08-16)

Initial Release!
