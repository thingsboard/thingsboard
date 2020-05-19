(function(){"use strict";angular
  .module('angularMaterialExpansionPanel')
  .controller('HomeController', HomeController);


function HomeController($scope, $mdExpansionPanel) {
  $mdExpansionPanel().waitFor('expansionPanelOne').then(function (instance) {
    instance.expand();
  });


  $scope.collapseOne = function () {
    $mdExpansionPanel('expansionPanelOne').collapse();
  };
}
}());