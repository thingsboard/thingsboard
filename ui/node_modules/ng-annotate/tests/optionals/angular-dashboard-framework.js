//test without adf specific annotations
angular.module('myMod', ['adf.provider'])
  .config(function(dashboardProvider){
    dashboardProvider
      .widget('myWidget', {
        title: 'My Widget'
      });
    });

//test adf controller, without dependency
angular.module('myMod', ['adf.provider'])
  .config(function(dashboardProvider){
    dashboardProvider
      .widget('myWidget', {
        title: 'My Widget',
        controller: function(){

        }
      });
    });

//test adf controller
angular.module('myMod', ['adf.provider'])
  .config(function(dashboardProvider){
    dashboardProvider
      .widget('myWidget', {
        title: 'My Widget',
        controller: function($http){

        }
      });
    });

//test mixed resolve
angular.module('myMod', ['adf.provider'])
  .config(function(dashboardProvider){
    dashboardProvider
      .widget('myWidget', {
        title: 'My Widget',
        resolve: {
          one: function(){

          },
          two: function($http){

          }
        }
      });
    });

//test adf edit controller
angular.module('myMod', ['adf.provider'])
  .config(function(dashboardProvider){
    dashboardProvider
      .widget('myWidget', {
        title: 'My Widget',
        edit: {
          controller: function($http){

          }
        }
      });
    });

//test full adf components
angular.module('myMod', ['adf.provider'])
  .config(function(dashboardProvider){
    dashboardProvider
      .widget('myWidget', {
        title: 'My Widget',
        controller: function($http){
           // control something
        },
        resolve: {
          one: function($http, config){
            // resolve something
          },
          two: function($http, config){
            // resolve something
          }
        },
        edit: {
          controller: function($http){
            // control something
          },
          apply: function($http, config){
            // apply configuration
          },
          resolve: {
            editone: function($http, config){
              // resolve something
            },
            edittwo: function($http, config){
              // resolve something
            }
          }
        }
      });
  });
