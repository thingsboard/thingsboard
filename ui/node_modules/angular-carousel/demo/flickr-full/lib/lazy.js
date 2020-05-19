angular.module('angular-lazy', []).
directive('lazyBackground', ['$document', '$parse', function($document, $parse) {
    return {
        restrict: 'A',
        link: function(scope, iElement, iAttrs) {
            function setLoading(elm) {
                if (loader) {
                    elm.html('');
                    elm.append(loader);
                    elm.css({
                        'background-image': null
                    });
                }
            }
            var loader = null;
            if (angular.isDefined(iAttrs.lazyLoader)) {
                loader = angular.element($document[0].querySelector(iAttrs.lazyLoader)).clone();
            }
            var bgModel = $parse(iAttrs.lazyBackground);
            scope.$watch(bgModel, function(newValue, oldValue) {
                setLoading(iElement);
                var src = bgModel(scope);
                var img = document.createElement('img');
                img.onload = function() {
                    if (loader) loader.remove();
                    if (angular.isDefined(iAttrs.lazyLoadingClass)) {
                        iElement.removeClass(iAttrs.lazyLoadingClass);
                    }
                    if (angular.isDefined(iAttrs.lazyLoadedClass)) {
                        iElement.addClass(iAttrs.lazyLoadedClass);
                    }
                    iElement.css({
                        'background-image': 'url(' + this.src + ')'
                    });
                };
                img.onerror= function() {
                    //console.log('error');
                };
                img.src = src;
            });
        }
    };
}]);
