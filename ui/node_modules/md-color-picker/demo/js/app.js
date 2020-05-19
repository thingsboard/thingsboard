var app = angular.module('plunker', ['ngMaterial','ngCookies', 'mdColorPicker']);

app.controller('MainCtrl', function($scope) {
  $scope.textConfig = {};
  $scope.textConfig.fonts = [
			'Arial',
			'Arial Black',
			'Comic Sans MS',
			'Courier New',
			'Georgia',
			'Impact',
			'Times New Roman',
			'Trebuchet MS',
			'Verdana'
		];

	$scope.textConfig.font;
	$scope.textConfig.textColor;
	$scope.textConfig.textBackground;

    $scope.textConfig.backgroundOptions = {
        label: "Text Background",
        icon: "font_download",

        hasBackdrop: true,
        clickOutsideToClose: true,
        random: true,
        openOnInput: true,

        alphaChannel: false,
        history: false,
        defaultTab: 1
    };
    $scope.textConfig.showPreview = true;

});
