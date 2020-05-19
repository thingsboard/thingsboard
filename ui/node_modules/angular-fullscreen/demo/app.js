var app = angular.module('DemoApp', ['FBAngular']);

function MainCtrl($scope, Fullscreen) {

   $scope.goFullscreen = function () {

      // Fullscreen
      if (Fullscreen.isEnabled())
         Fullscreen.cancel();
      else
         Fullscreen.all();

      // Set Fullscreen to a specific element (bad practice)
      // Fullscreen.enable( document.getElementById('img') )

   };

   $scope.isFullScreen = false;

   $scope.goFullScreenViaWatcher = function() {
      $scope.isFullScreen = !$scope.isFullScreen;
   };

}
