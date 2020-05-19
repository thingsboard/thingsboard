angular.module('angularMaterialExpansionPanel', [
  'ngRoute',
  'ngAnimate',
  'ngMaterial',
  'material.components.expansionPanels'
])
  .config(configApp);


configApp.$inject = ['$routeProvider'];
function configApp($routeProvider) {
  $routeProvider
    .when('/', {
      templateUrl: 'pages/home/home.html',
      controller: 'HomeController',
      controllerAs: 'vm'
    })
    .when('/group', {
      templateUrl: 'pages/group/group.html',
      controller: 'GroupController',
      controllerAs: 'vm'
    })
    .when('/autoexpand', {
      templateUrl: 'pages/autoExpand/autoExpand.html',
      controller: 'AutoExpandController',
      controllerAs: 'vm'
    })
    .when('/multiple', {
      templateUrl: 'pages/multiple/multiple.html',
      controller: 'MultipleController',
      controllerAs: 'vm'
    })
    .otherwise('/');
}
