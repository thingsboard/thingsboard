//test without adf specific annotations
angular.module('myMod', ['adf.provider'])
  .config(["dashboardProvider", function(dashboardProvider){
    dashboardProvider
      .widget('myWidget', {
        title: 'My Widget'
      });
    }]);

//test adf controller, without dependency
angular.module('myMod', ['adf.provider'])
  .config(["dashboardProvider", function(dashboardProvider){
    dashboardProvider
      .widget('myWidget', {
        title: 'My Widget',
        controller: function(){

        }
      });
    }]);

//test adf controller
angular.module('myMod', ['adf.provider'])
  .config(["dashboardProvider", function(dashboardProvider){
    dashboardProvider
      .widget('myWidget', {
        title: 'My Widget',
        controller: ["$http", function($http){

        }]
      });
    }]);

//test mixed resolve
angular.module('myMod', ['adf.provider'])
  .config(["dashboardProvider", function(dashboardProvider){
    dashboardProvider
      .widget('myWidget', {
        title: 'My Widget',
        resolve: {
          one: function(){

          },
          two: ["$http", function($http){

          }]
        }
      });
    }]);

//test adf edit controller
angular.module('myMod', ['adf.provider'])
  .config(["dashboardProvider", function(dashboardProvider){
    dashboardProvider
      .widget('myWidget', {
        title: 'My Widget',
        edit: {
          controller: ["$http", function($http){

          }]
        }
      });
    }]);

//test full adf components
angular.module('myMod', ['adf.provider'])
  .config(["dashboardProvider", function(dashboardProvider){
    dashboardProvider
      .widget('myWidget', {
        title: 'My Widget',
        controller: ["$http", function($http){
           // control something
        }],
        resolve: {
          one: ["$http", "config", function($http, config){
            // resolve something
          }],
          two: ["$http", "config", function($http, config){
            // resolve something
          }]
        },
        edit: {
          controller: ["$http", function($http){
            // control something
          }],
          apply: ["$http", "config", function($http, config){
            // apply configuration
          }],
          resolve: {
            editone: ["$http", "config", function($http, config){
              // resolve something
            }],
            edittwo: ["$http", "config", function($http, config){
              // resolve something
            }]
          }
        }
      });
  }]);
