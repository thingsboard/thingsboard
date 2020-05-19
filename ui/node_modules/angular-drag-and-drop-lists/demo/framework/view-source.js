angular.module("demo").directive('viewSource', function($http, $timeout) {
    return {
        scope: {
            demoName: "@viewSource",
            highlightLines: "="
        },
        templateUrl: 'framework/view-source.html',
        link: function (scope, element, attr) {

            scope.models = {
                types: [
                    {extension: "html", language: "markup", label: "Markup"},
                    {extension: "css", language: "css", label: "CSS"},
                    {extension: "js", language: "javascript", label: "Javascript"},
                ],
                activeTab: "markup"
            };

            angular.forEach(scope.models.types, function(type) {
                $http.get(scope.demoName + '/' + scope.demoName + '.' + type.extension)
                    .success(function (data) {
                        type.source = data;
                        $timeout(Prism.highlightAll, 0);
                    });
            });

        }
    };
});
