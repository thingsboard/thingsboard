(function() {
    "use strict";

    angular.module('angular-carousel')

    .filter('carouselSlice', function() {
        return function(collection, start, size) {
            if (angular.isArray(collection)) {
                return collection.slice(start, start + size);
            } else if (angular.isObject(collection)) {
                // dont try to slice collections :)
                return collection;
            }
        };
    });

})();
