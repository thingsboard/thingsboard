/* global moment, angular */

var module = angular.module("mdPickers", [
	"ngMaterial",
	"ngAnimate",
	"ngAria"
]);

module.config(["$mdIconProvider", "mdpIconsRegistry", function($mdIconProvider, mdpIconsRegistry) {
	angular.forEach(mdpIconsRegistry, function(icon, index) {
		$mdIconProvider.icon(icon.id, icon.url);
	});
}]);

module.run(["$templateCache", "mdpIconsRegistry", function($templateCache, mdpIconsRegistry) {
	angular.forEach(mdpIconsRegistry, function(icon, index) {
		$templateCache.put(icon.url, icon.svg);
	});
}]);