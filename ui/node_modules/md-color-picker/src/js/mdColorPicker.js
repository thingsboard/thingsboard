
(function( window, angular, undefined ) {
'use strict';
var dateClick;

var GradientCanvas = function( type, restrictX ) {

	this.type = type;
	this.restrictX = restrictX;
	this.offset = {
		x: null,
		y: null
	};
	this.height = 255;

	this.$scope = null;
	this.$element = null;

	this.get = angular.bind(this, function( $temp_scope, $temp_element, $temp_attrs ) {
		////////////////////////////
		// Variables
		////////////////////////////

		this.$scope = $temp_scope;
		this.$element = $temp_element;


		this.canvas = this.$element.children()[0];
		this.marker = this.$element.children()[1];
		this.context = this.canvas.getContext('2d');
		this.currentColor = this.$scope.color.toRgb();
		this.currentHue = this.$scope.color.toHsv().h;
		////////////////////////////
		// Watchers, Observes, Events
		////////////////////////////

		//$scope.$watch( function() { return color.getRgb(); }, hslObserver, true );



		this.$element.on( 'mousedown', angular.bind( this, this.onMouseDown ) );
		this.$scope.$on('mdColorPicker:colorSet', angular.bind( this, this.onColorSet ) );
		if ( this.extra ) {
			this.extra();
		};
		////////////////////////////
		// init
		////////////////////////////

		this.draw();
	});

	//return angular.bind( this, this.get );

};
GradientCanvas.prototype.$window = angular.element( window );

GradientCanvas.prototype.getColorByMouse = function( e ) {
	var x = e.pageX - this.offset.x;
	var y = e.pageY - this.offset.y;

	return this.getColorByPoint( x, y );
};

GradientCanvas.prototype.setMarkerCenter = function( x, y ) {
	var xOffset = -1 * this.marker.offsetWidth / 2;
	var yOffset = -1 * this.marker.offsetHeight / 2;
	if ( y === undefined ) {
		y = x + yOffset;
		y = Math.max( Math.min( this.height-1 + yOffset, y ), Math.ceil( yOffset ) );

		x = 0;
	} else {
		x = x + xOffset;
		y = y + yOffset;

		x = Math.max( Math.min( this.height + xOffset, x ), Math.ceil( xOffset ) );
		y = Math.max( Math.min( this.height + yOffset, y ), Math.ceil( yOffset ) );
	}



	angular.element(this.marker).css({'left': x + 'px' });
	angular.element(this.marker).css({'top': y + 'px'});
};

GradientCanvas.prototype.getMarkerCenter = function() {
	var returnObj = {
		x: this.marker.offsetLeft + ( Math.floor( this.marker.offsetWidth / 2 ) ),
		y: this.marker.offsetTop + ( Math.floor( this.marker.offsetHeight / 2 ) )
	};
	return returnObj;
};

GradientCanvas.prototype.getImageData = function( x, y ) {
	x = Math.max( 0, Math.min( x, this.canvas.width-1 ) );
	y = Math.max( 0, Math.min( y, this.canvas.height-1 ) );

	var imageData = this.context.getImageData( x, y, 1, 1 ).data;
	return imageData;
};

GradientCanvas.prototype.onMouseDown = function( e ) {
	// Prevent highlighting
	e.preventDefault();
	e.stopImmediatePropagation();

	this.$scope.previewUnfocus();

	this.$element.css({ 'cursor': 'none' });

	this.offset.x = this.canvas.getBoundingClientRect().left+1;
	this.offset.y = this.canvas.getBoundingClientRect().top;

	var fn = angular.bind( this, function( e ) {
		switch( this.type ) {
			case 'hue':
				var hue = this.getColorByMouse( e );
				this.$scope.$broadcast( 'mdColorPicker:spectrumHueChange', {hue: hue});
				break;
			case 'alpha':
				var alpha = this.getColorByMouse( e );
				this.$scope.color.setAlpha( alpha );
				this.$scope.alpha = alpha;
				this.$scope.$apply();
				break;
			case 'spectrum':
				var color = this.getColorByMouse( e );
				this.setColor( color );
				break;
		}
	});

	this.$window.on( 'mousemove', fn );
	this.$window.one( 'mouseup', angular.bind(this, function( e ) {
		this.$window.off( 'mousemove', fn );
		this.$element.css({ 'cursor': 'crosshair' });
	}));

	// Set the color
	fn( e );
};

GradientCanvas.prototype.setColor = function( color ) {

		this.$scope.color._r = color.r;
		this.$scope.color._g = color.g;
		this.$scope.color._b = color.b;
		this.$scope.$apply();
		this.$scope.$broadcast('mdColorPicker:spectrumColorChange', { color: color });
};

GradientCanvas.prototype.onColorSet = function( e, args ) {
	switch( this.type ) {
		case 'hue':
			var hsv = this.$scope.color.toHsv();
			this.setMarkerCenter( this.canvas.height - ( this.canvas.height * ( hsv.h / 360 ) ) );
			break;
		case 'alpha':
			this.currentColor = args.color.toRgb();
			this.draw();

			var alpha = args.color.getAlpha();
			var pos = this.canvas.height - ( this.canvas.height * alpha );

			this.setMarkerCenter( pos );
			break;
		case 'spectrum':
			var hsv = args.color.toHsv();
			this.currentHue = hsv.h;
			this.draw();

			var posX = this.canvas.width * hsv.s;
			var posY = this.canvas.height - ( this.canvas.height * hsv.v );

			this.setMarkerCenter( posX, posY );
			break;
	}

};

var hueLinkFn = new GradientCanvas( 'hue', true );
hueLinkFn.getColorByPoint = function( x, y ) {
	var imageData = this.getImageData( x, y );
	this.setMarkerCenter( y );

	var hsl = new tinycolor( {r: imageData[0], g: imageData[1], b: imageData[2] } );
	return hsl.toHsl().h;

};
hueLinkFn.draw = function()  {


	this.$element.css({'height': this.height + 'px'});

	this.canvas.height = this.height;
	this.canvas.width = this.height;



	// Create gradient
	var hueGrd = this.context.createLinearGradient(90, 0.000, 90, this.height);

	// Add colors
	hueGrd.addColorStop(0.01,	'rgba(255, 0, 0, 1.000)');
	hueGrd.addColorStop(0.167, 	'rgba(255, 0, 255, 1.000)');
	hueGrd.addColorStop(0.333, 	'rgba(0, 0, 255, 1.000)');
	hueGrd.addColorStop(0.500, 	'rgba(0, 255, 255, 1.000)');
	hueGrd.addColorStop(0.666, 	'rgba(0, 255, 0, 1.000)');
	hueGrd.addColorStop(0.828, 	'rgba(255, 255, 0, 1.000)');
	hueGrd.addColorStop(0.999, 	'rgba(255, 0, 0, 1.000)');

	// Fill with gradient
	this.context.fillStyle = hueGrd;
	this.context.fillRect( 0, 0, this.canvas.width, this.height );
};


var alphaLinkFn = new GradientCanvas( 'alpha', true );
alphaLinkFn.getColorByPoint = function( x, y ) {
	var imageData = this.getImageData( x, y );
	this.setMarkerCenter( y );

	return imageData[3] / 255;
};
alphaLinkFn.draw = function ()  {
	this.$element.css({'height': this.height + 'px'});

	this.canvas.height = this.height;
	this.canvas.width = this.height;

	// Create gradient
	var hueGrd = this.context.createLinearGradient(90, 0.000, 90, this.height);

	// Add colors
	hueGrd.addColorStop(0.01,	'rgba(' + this.currentColor.r + ',' + this.currentColor.g + ',' + this.currentColor.b + ', 1.000)');
	hueGrd.addColorStop(0.999,	'rgba(' + this.currentColor.r + ',' + this.currentColor.g + ',' + this.currentColor.b + ', 0.000)');

	// Fill with gradient
	this.context.fillStyle = hueGrd;
	this.context.fillRect( 0, 0, this.canvas.width, this.height );
};
alphaLinkFn.extra = function() {
	this.$scope.$on('mdColorPicker:spectrumColorChange', angular.bind( this, function( e, args ) {
		this.currentColor = args.color;
		this.draw();
	}));
};


var spectrumLinkFn = new GradientCanvas( 'spectrum', false );
spectrumLinkFn.getColorByPoint = function( x, y ) {
	var imageData = this.getImageData( x, y );
 	this.setMarkerCenter(x,y);

	return {
		r: imageData[0],
		g: imageData[1],
		b: imageData[2]
	};
};
spectrumLinkFn.draw = function() {
	this.canvas.height = this.height;
	this.canvas.width = this.height;
	this.context.clearRect(0, 0, this.canvas.width, this.canvas.height);
	// White gradient

	var whiteGrd = this.context.createLinearGradient(0, 0, this.canvas.width, 0);

	whiteGrd.addColorStop(0, 'rgba(255, 255, 255, 1.000)');
	whiteGrd.addColorStop(1, 'rgba(255, 255, 255, 0.000)');

	// Black Gradient
	var blackGrd = this.context.createLinearGradient(0, 0, 0, this.canvas.height);

	blackGrd.addColorStop(0, 'rgba(0, 0, 0, 0.000)');
	blackGrd.addColorStop(1, 'rgba(0, 0, 0, 1.000)');

	// Fill with solid
	this.context.fillStyle = 'hsl( ' + this.currentHue + ', 100%, 50%)';
	this.context.fillRect( 0, 0, this.canvas.width, this.canvas.height );

	// Fill with white
	this.context.fillStyle = whiteGrd;
	this.context.fillRect( 0, 0, this.canvas.width, this.canvas.height );

	// Fill with black
	this.context.fillStyle = blackGrd;
	this.context.fillRect( 0, 0, this.canvas.width, this.canvas.height );
};
spectrumLinkFn.extra = function() {
	this.$scope.$on('mdColorPicker:spectrumHueChange', angular.bind( this, function( e, args ) {
		this.currentHue = args.hue;
		this.draw();
		var markerPos = this.getMarkerCenter();
		var color = this.getColorByPoint( markerPos.x, markerPos.y );
		this.setColor( color );

	}));
};


angular.module('mdColorPicker', [])
	.run(['$templateCache', function ($templateCache) {
		//icon resource should not be dependent
		//credit to materialdesignicons.com
		var shapes = {
			'clear': '<path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>',
			'gradient': '<path d="M11 9h2v2h-2zm-2 2h2v2H9zm4 0h2v2h-2zm2-2h2v2h-2zM7 9h2v2H7zm12-6H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 18H7v-2h2v2zm4 0h-2v-2h2v2zm4 0h-2v-2h2v2zm2-7h-2v2h2v2h-2v-2h-2v2h-2v-2h-2v2H9v-2H7v2H5v-2h2v-2H5V5h14v6z"/>',
			'tune': '<path d="M13 21v-2h8v-2h-8v-2h-2v6h2zM3 17v2h6v-2H3z"/><path d="M21 13v-2H11v2h10zM7 9v2H3v2h4v2h2V9H7z"/><path d="M15 9h2V7h4V5h-4V3h-2v6zM3 5v2h10V5H3z"/>',
			'view_module': '<path d="M4 11h5V5H4v6z"/><path d="M4 18h5v-6H4v6z"/><path d="M10 18h5v-6h-5v6z"/><path d="M16 18h5v-6h-5v6z"/><path d="M10 11h5V5h-5v6z"/><path d="M16 5v6h5V5h-5z"/>',
			'view_headline': '<path d="M4 15h17v-2H4v2z"/><path d="M4 19h17v-2H4v2z"/><path d="M4 11h17V9H4v2z"/><path d="M4 5v2h17V5H4z"/>',
			'history': '<path d="M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9z"/><path d="M12 8v5l4.28 2.54.72-1.21-3.5-2.08V8H12z"/>',
			'clear_all': '<path d="M5 13h14v-2H5v2zm-2 4h14v-2H3v2zM7 7v2h14V7H7z"/>'
		};
		for (var i in shapes) {
			if (shapes.hasOwnProperty(i)) {
				$templateCache.put([i, 'svg'].join('.'),
					['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">', shapes[i], '</svg>'].join(''));
			}
		}
	}])
	.factory('mdColorPickerHistory', ['$injector', function( $injector ) {

		var history = [];
		var strHistory = [];

		var $cookies = false;
		try {
			$cookies = $injector.get('$cookies');
		} catch(e) {

		}

		if ( $cookies ) {
			var tmpHistory = $cookies.getObject( 'mdColorPickerHistory' ) || [];
			for ( var i = 0; i < tmpHistory.length; i++ ) {
				history.push( tinycolor( tmpHistory[i] ) );
				strHistory.push( tmpHistory[i] );
			}
		}

		var length = 40;

		return {
			length: function() {
				if ( arguments[0] ) {
					length = arguments[0];
				} else {
					return history.length;
				}
			},
			add: function( color ) {
				for( var x = 0; x < history.length; x++ ) {
					if ( history[x].toRgbString() === color.toRgbString() ) {
						history.splice(x, 1);
						strHistory.splice(x, 1);
					}
				}

				history.unshift( color );
				strHistory.unshift( color.toRgbString() );

				if ( history.length > length ) {
					history.pop();
					strHistory.pop();
				}
				if ( $cookies ) {
					$cookies.putObject('mdColorPickerHistory', strHistory );
				}
			},
			get: function() {
				return history;
			},
			reset: function() {
				history = [];
				strHistory = [];
				if ( $cookies ) {
					$cookies.putObject('mdColorPickerHistory', strHistory );
				}
			}
		};
	}])
	.directive('mdColorPicker', [ '$timeout', 'mdColorPickerHistory', function( $timeout, colorHistory ) {

		return {
			templateUrl: "mdColorPicker.tpl.html",

			// Added required controller ngModel
			require: '^ngModel',
			scope: {
				options: '=mdColorPicker',

				// Input options
				type: '@',
				label: '@',
				icon: '@',
				random: '@',
				default: '@',

				// Dialog Options
				openOnInput: '@',
				hasBackdrop: '@',
				clickOutsideToClose: '@',
				skipHide: '@',
				preserveScope: '@',

				// Advanced options
				mdColorClearButton: '=?',
				mdColorPreview: '=?',

				mdColorAlphaChannel: '=?',
				mdColorSpectrum: '=?',
				mdColorSliders: '=?',
				mdColorGenericPalette: '=?',
				mdColorMaterialPalette: '=?',
				mdColorHistory: '=?',
				mdColorDefaultTab: '=?'
			},
			controller: ['$scope', '$element', '$attrs', '$mdDialog', '$mdColorPicker', function( $scope, $element, $attrs, $mdDialog, $mdColorPicker ) {
				var didJustClose = false;

				// Merge Options Object with scope.  Scope will take precedence much like css vs style attribute.
				if ( $scope.options !== undefined ) {
					for ( var opt in $scope.options ) {
						if ( $scope.options.hasOwnProperty( opt ) ) {
							var scopeVal = undefined;
							var scopeKey = undefined;
							if ( $scope.hasOwnProperty( opt ) ) {
								scopeKey = opt;
							} else if ( $scope.hasOwnProperty( 'mdColor' + opt.slice(0,1).toUpperCase() + opt.slice(1) ) ) {
								scopeKey = 'mdColor' + opt.slice(0,1).toUpperCase() + opt.slice(1);
							}

							if ( scopeKey && $scope[scopeKey] === undefined ) {
								$scope[scopeKey] = $scope.options[opt];
							}
						}
					}
				}

				// Get ngModelController from the current element
				var ngModel = $element.controller('ngModel');

				// Quick function for updating the local 'value' on scope
				var updateValue = function(val) {
					$scope.value = val || ngModel.$viewValue || '';
				};

				// Defaults
				// Everything is enabled by default.
				$scope.mdColorClearButton = $scope.mdColorClearButton === undefined ? true : $scope.mdColorClearButton;
				$scope.mdColorPreview = $scope.mdColorPreview === undefined ? true : $scope.mdColorPreview;

				$scope.mdColorAlphaChannel = $scope.mdColorAlphaChannel === undefined ? true : $scope.mdColorAlphaChannel;
				$scope.mdColorSpectrum = $scope.mdColorSpectrum === undefined ? true : $scope.mdColorSpectrum;
				$scope.mdColorSliders = $scope.mdColorSliders === undefined ? true : $scope.mdColorSliders;
				$scope.mdColorGenericPalette = $scope.mdColorGenericPalette === undefined ? true : $scope.mdColorGenericPalette;
				$scope.mdColorMaterialPalette = $scope.mdColorMaterialPalette === undefined ? true : $scope.mdColorMaterialPalette;
				$scope.mdColorHistory = $scope.mdColorHistory === undefined ? true : $scope.mdColorHistory;


				// Set the starting value
				updateValue();

				// Keep an eye on changes
				$scope.$watch(function() {
					return ngModel.$modelValue;
				},function(newVal) {
					updateValue(newVal);
				});

				// Watch for updates to value and set them on the model
				$scope.$watch('value',function(newVal,oldVal) {
					if (newVal !== '' && typeof newVal !== 'undefined' && newVal && newVal !== oldVal) {
						ngModel.$setViewValue(newVal);
					}
				});

				// The only other ngModel changes

				$scope.clearValue = function clearValue() {
					$scope.value = '';
				};
				$scope.showColorPicker = function showColorPicker($event) {
					if ( didJustClose ) {
						return;
					}
				//	dateClick = Date.now();
				//	console.log( "CLICK OPEN", dateClick, $scope );

					$mdColorPicker.show({
						value: $scope.value,
						defaultValue: $scope.default,
						random: $scope.random,
						clickOutsideToClose: $scope.clickOutsideToClose,
						hasBackdrop: $scope.hasBackdrop,
						skipHide: $scope.skipHide,
						preserveScope: $scope.preserveScope,

						mdColorAlphaChannel: $scope.mdColorAlphaChannel,
						mdColorSpectrum: $scope.mdColorSpectrum,
						mdColorSliders: $scope.mdColorSliders,
						mdColorGenericPalette: $scope.mdColorGenericPalette,
						mdColorMaterialPalette: $scope.mdColorMaterialPalette,
						mdColorHistory: $scope.mdColorHistory,
						mdColorDefaultTab: $scope.mdColorDefaultTab,

						$event: $event,

					}).then(function( color ) {
						$scope.value = color;
					});
				};
			}],
			compile: function( element, attrs ) {

				//attrs.value = attrs.value || "#ff0000";
				attrs.type = attrs.type !== undefined ? attrs.type : 0;

			}
		};
	}])
	.directive( 'mdColorPickerContainer', ['$compile','$timeout','$mdColorPalette','mdColorPickerHistory', function( $compile, $timeout, $mdColorPalette, colorHistory ) {
		return {
			templateUrl: 'mdColorPickerContainer.tpl.html',
			scope: {
				value: '=?',
				default: '@',
				random: '@',
				ok: '=?',
				mdColorAlphaChannel: '=',
				mdColorSpectrum: '=',
				mdColorSliders: '=',
				mdColorGenericPalette: '=',
				mdColorMaterialPalette: '=',
				mdColorHistory: '=',
				mdColorDefaultTab: '='
			},
			controller: function( $scope, $element, $attrs ) {
			//	console.log( "mdColorPickerContainer Controller", Date.now() - dateClick, $scope );

				function getTabIndex( tab ) {
					var index = 0;
					if ( tab && typeof( tab ) === 'string' ) {
/* DOM isn't fast enough for this

						var tabs = $element[0].querySelector('.md-color-picker-colors').getElementsByTagName( 'md-tab' );
						console.log( tabs.length );
*/
						var tabName = 'mdColor' + tab.slice(0,1).toUpperCase() + tab.slice(1);
						var tabs = ['mdColorSpectrum', 'mdColorSliders', 'mdColorGenericPalette', 'mdColorMaterialPalette', 'mdColorHistory'];
						for ( var x = 0; x < tabs.length; x++ ) {
							//console.log(  tabs[x]('ng-if') );
							//if ( tabs[x].getAttribute('ng-if') == tabName ) {
							if ( tabs[x] == tabName ) {
								if ( $scope[tabName] ) {
									index = x;
									break;
								}
							}
						}
					} else if ( tab && typeof ( tab ) === 'number') {
						index = tab;
					}

					return index;
				}

				///////////////////////////////////
				// Variables
				///////////////////////////////////
				var container = angular.element( $element[0].querySelector('.md-color-picker-container') );
				var resultSpan = angular.element( container[0].querySelector('.md-color-picker-result') );
				var previewInput = angular.element( $element[0].querySelector('.md-color-picker-preview-input') );

				var outputFn = [
					'toHexString',
					'toRgbString',
					'toHslString'
				];



				$scope.default = $scope.default ? $scope.default : $scope.random ? tinycolor.random() : 'rgb(255,255,255)';
				if ( $scope.value.search('#') >= 0 ) {
					$scope.type = 0;
				} else if ( $scope.value.search('rgb') >= 0 ) {
					$scope.type = 1;
				} else if ( $scope.value.search('hsl') >= 0 ) {
					$scope.type = 2;
				}
				$scope.color = new tinycolor($scope.value || $scope.default); // Set initial color
				$scope.alpha = $scope.color.getAlpha();
				$scope.history =  colorHistory;
				$scope.materialFamily = [];

				$scope.whichPane = getTabIndex( $scope.mdColorDefaultTab );
				$scope.inputFocus = false;

				// Colors for the palette screen
				///////////////////////////////////
				var steps = 9;
				var freq = 2*Math.PI/steps;

				$scope.palette = [
					["rgb(255, 204, 204)","rgb(255, 230, 204)","rgb(255, 255, 204)","rgb(204, 255, 204)","rgb(204, 255, 230)","rgb(204, 255, 255)","rgb(204, 230, 255)","rgb(204, 204, 255)","rgb(230, 204, 255)","rgb(255, 204, 255)"],
					["rgb(255, 153, 153)","rgb(255, 204, 153)","rgb(255, 255, 153)","rgb(153, 255, 153)","rgb(153, 255, 204)","rgb(153, 255, 255)","rgb(153, 204, 255)","rgb(153, 153, 255)","rgb(204, 153, 255)","rgb(255, 153, 255)"],
					["rgb(255, 102, 102)","rgb(255, 179, 102)","rgb(255, 255, 102)","rgb(102, 255, 102)","rgb(102, 255, 179)","rgb(102, 255, 255)","rgb(102, 179, 255)","rgb(102, 102, 255)","rgb(179, 102, 255)","rgb(255, 102, 255)"],
					["rgb(255, 51, 51)","rgb(255, 153, 51)","rgb(255, 255, 51)","rgb(51, 255, 51)","rgb(51, 255, 153)","rgb(51, 255, 255)","rgb(51, 153, 255)","rgb(51, 51, 255)","rgb(153, 51, 255)","rgb(255, 51, 255)"],
					["rgb(255, 0, 0)","rgb(255, 128, 0)","rgb(255, 255, 0)","rgb(0, 255, 0)","rgb(0, 255, 128)","rgb(0, 255, 255)","rgb(0, 128, 255)","rgb(0, 0, 255)","rgb(128, 0, 255)","rgb(255, 0, 255)"],
					["rgb(245, 0, 0)","rgb(245, 123, 0)","rgb(245, 245, 0)","rgb(0, 245, 0)","rgb(0, 245, 123)","rgb(0, 245, 245)","rgb(0, 123, 245)","rgb(0, 0, 245)","rgb(123, 0, 245)","rgb(245, 0, 245)"],
					["rgb(214, 0, 0)","rgb(214, 108, 0)","rgb(214, 214, 0)","rgb(0, 214, 0)","rgb(0, 214, 108)","rgb(0, 214, 214)","rgb(0, 108, 214)","rgb(0, 0, 214)","rgb(108, 0, 214)","rgb(214, 0, 214)"],
					["rgb(163, 0, 0)","rgb(163, 82, 0)","rgb(163, 163, 0)","rgb(0, 163, 0)","rgb(0, 163, 82)","rgb(0, 163, 163)","rgb(0, 82, 163)","rgb(0, 0, 163)","rgb(82, 0, 163)","rgb(163, 0, 163)"],
					["rgb(92, 0, 0)","rgb(92, 46, 0)","rgb(92, 92, 0)","rgb(0, 92, 0)","rgb(0, 92, 46)","rgb(0, 92, 92)","rgb(0, 46, 92)","rgb(0, 0, 92)","rgb(46, 0, 92)","rgb(92, 0, 92)"],
					["rgb(255, 255, 255)","rgb(205, 205, 205)","rgb(178, 178, 178)","rgb(153, 153, 153)","rgb(127, 127, 127)","rgb(102, 102, 102)","rgb(76, 76, 76)","rgb(51, 51, 51)","rgb(25, 25, 25)","rgb(0, 0, 0)"]
				];

				$scope.materialPalette = $mdColorPalette;

				///////////////////////////////////
				// Functions
				///////////////////////////////////
				$scope.isDark = function isDark( color ) {
					if ( angular.isArray( color ) ) {
						return tinycolor( {r: color[0], g: color[1], b: color[2] }).isDark();
					} else {
						return tinycolor( color ).isDark();
					}

				};
				$scope.previewFocus = function() {
					$scope.inputFocus = true;
					$timeout( function() {
						previewInput[0].setSelectionRange(0, previewInput[0].value.length);
					});
				};
				$scope.previewUnfocus = function() {
					$scope.inputFocus = false;
					previewInput[0].blur();
				};
				$scope.previewBlur = function() {
					$scope.inputFocus = false;
					$scope.setValue();
				};
				$scope.previewKeyDown = function( $event ) {

					if ( $event.keyCode == 13 ) {
						$scope.ok && $scope.ok();
					}
				};
				$scope.setPaletteColor = function( event ) {
					$timeout( function() {
						$scope.color = tinycolor( event.target.style.backgroundColor );
					});
				};

				$scope.setValue = function setValue() {
					// Set the value if available
					if ( $scope.color && $scope.color && outputFn[$scope.type] && $scope.color.toRgbString() !== 'rgba(0, 0, 0, 0)' ) {
						$scope.value = $scope.color[outputFn[$scope.type]]();
					}
				};

				$scope.changeValue = function changeValue() {
					$scope.color = tinycolor( $scope.value );
					$scope.$broadcast('mdColorPicker:colorSet', { color: $scope.color });
				};


				///////////////////////////////////
				// Watches and Events
				///////////////////////////////////
				$scope.$watch( 'color._a', function( newValue ) {
					$scope.color.setAlpha( newValue );
				}, true);

				$scope.$watch( 'whichPane', function( newValue ) {
					// 0 - spectrum selector
					// 1 - sliders
					// 2 - palette
					$scope.$broadcast('mdColorPicker:colorSet', {color: $scope.color });

				});

				$scope.$watch( 'type', function() {
					previewInput.removeClass('switch');
					$timeout(function() {
						previewInput.addClass('switch');
					});
				});

				$scope.$watchGroup(['color.toRgbString()', 'type'], function( newValue ) {
					if ( !$scope.inputFocus ) {
						$scope.setValue();
					}
				});


				///////////////////////////////////
				// INIT
				// Let all the other directives initialize
				///////////////////////////////////
			//	console.log( "mdColorPickerContainer Controller PRE Timeout", Date.now() - dateClick );
				$timeout( function() {
			//		console.log( "mdColorPickerContainer Controller Timeout", Date.now() - dateClick );
					$scope.$broadcast('mdColorPicker:colorSet', { color: $scope.color });
					previewInput.focus();
					$scope.previewFocus();
				});
			},
			link: function( scope, element, attrs ) {

				var tabs = element[0].getElementsByTagName( 'md-tab' );
				/*
				Replicating these structure without ng-repeats

				<div ng-repeat="row in palette track by $index" flex="15"  layout-align="space-between" layout="row"  layout-fill>
					<div ng-repeat="col in row track by $index" flex="10" style="height: 25.5px;" ng-style="{'background': col};" ng-click="setPaletteColor($event)"></div>
				</div>

				<div ng-repeat="(key, value) in materialColors">
					<div ng-style="{'background': 'rgb('+value['500'].value[0]+','+value['500'].value[1]+','+value['500'].value[2]+')', height: '75px'}" class="md-color-picker-material-title" ng-class="{'dark': isDark( value['500'].value )}" ng-click="setPaletteColor($event)">
						<span>{{key}}</span>
					</div>
					<div ng-repeat="(label, color) in value track by $index" ng-style="{'background': 'rgb('+color.value[0]+','+color.value[1]+','+color.value[2]+')', height: '33px'}" class="md-color-picker-with-label" ng-class="{'dark': isDark( color.value )}" ng-click="setPaletteColor($event)">
						<span>{{label}}</span>
					</div>
				</div>
				*/


				$timeout(function() {
					createDOM();
				});

				function createDOM() {
					var paletteContainer = angular.element( element[0].querySelector('.md-color-picker-palette') );
					var materialContainer = angular.element( element[0].querySelector('.md-color-picker-material-palette') );
					var paletteRow = angular.element('<div class="flex-15 layout-fill layout-row layout-align-space-between" layout-align="space-between" layout="row" layout-fill"></div>');
					var paletteCell = angular.element('<div class="flex-10"></div>');

					var materialTitle = angular.element('<div class="md-color-picker-material-title"></div>');
					var materialRow = angular.element('<div class="md-color-picker-with-label"></div>');



					angular.forEach(scope.palette, function( value, key ) {
						var row = paletteRow.clone();
						angular.forEach( value, function( color ) {
							var cell = paletteCell.clone();
							cell.css({
								height: '25.5px',
								backgroundColor: color
							});
							cell.bind('click', scope.setPaletteColor );
							row.append( cell );
						});

						paletteContainer.append( row );
					});

					angular.forEach(scope.materialPalette, function( value, key ) {
						var title = materialTitle.clone();
						title.html('<span>'+key.replace('-',' ')+'</span>');
						title.css({
							height: '75px',
							backgroundColor: 'rgb('+value['500'].value[0]+','+value['500'].value[1]+','+value['500'].value[2]+')'
						});
						if ( scope.isDark(value['500'].value) ) {
							title.addClass('dark');
						}

						materialContainer.append( title );

						angular.forEach( value, function( color, label ) {

							var row = materialRow.clone();
							row.css({
								height: '33px',
								backgroundColor: 'rgb('+color.value[0]+','+color.value[1]+','+color.value[2]+')'
							});
							if ( scope.isDark(color.value) ) {
								row.addClass('dark');
							}

							row.html('<span>'+label+'</span>');
							row.bind('click', scope.setPaletteColor );
							materialContainer.append( row );
						});


					});
				}
			}
		};
	}])

	.directive( 'mdColorPickerHue', [function() {
		return {
			template: '<canvas width="100%" height="100%"></canvas><div class="md-color-picker-marker"></div>',
			link: hueLinkFn.get,
			controller: function() {
			//	console.log( "mdColorPickerHue Controller", Date.now() - dateClick );
			}
		};
	}])

	.directive( 'mdColorPickerAlpha', [function() {
		return {
			template: '<canvas width="100%" height="100%"></canvas><div class="md-color-picker-marker"></div>',
			link: alphaLinkFn.get,
			controller: function() {
			//	console.log( "mdColorPickerAlpha Controller", Date.now() - dateClick );
			}
		};
	}])

	.directive( 'mdColorPickerSpectrum', [function() {
		return {
			template: '<canvas width="100%" height="100%"></canvas><div class="md-color-picker-marker"></div>{{hue}}',
			link: spectrumLinkFn.get,
			controller: function() {
			//	console.log( "mdColorPickerSpectrum Controller", Date.now() - dateClick );
			}
		};
	}])

    .factory('$mdColorPicker', ['$q', '$mdDialog', 'mdColorPickerHistory', function ($q, $mdDialog, colorHistory)
    {
		var dialog;

        return {
            show: function (options)
            {
                if ( options === undefined ) {
                    options = {};
                }
				//console.log( 'DIALOG OPTIONS', options );
				// Defaults
				// Dialog Properties
                options.hasBackdrop = options.hasBackdrop === undefined ? true : options.hasBackdrop;
				options.clickOutsideToClose = options.clickOutsideToClose === undefined ? true : options.clickOutsideToClose;
				options.defaultValue = options.defaultValue === undefined ? '#FFFFFF' : options.defaultValue;
				options.focusOnOpen = options.focusOnOpen === undefined ? false : options.focusOnOpen;
				options.preserveScope = options.preserveScope === undefined ? true : options.preserveScope;
				options.skipHide = options.skipHide === undefined ? true : options.skipHide;

				// mdColorPicker Properties
				options.mdColorAlphaChannel = options.mdColorAlphaChannel === undefined ? false : options.mdColorAlphaChannel;
				options.mdColorSpectrum = options.mdColorSpectrum === undefined ? true : options.mdColorSpectrum;
				options.mdColorSliders = options.mdColorSliders === undefined ? true : options.mdColorSliders;
				options.mdColorGenericPalette = options.mdColorGenericPalette === undefined ? true : options.mdColorGenericPalette;
				options.mdColorMaterialPalette = options.mdColorMaterialPalette === undefined ? true : options.mdColorMaterialPalette;
				options.mdColorHistory = options.mdColorHistory === undefined ? true : options.mdColorHistory;





                dialog = $mdDialog.show({
					templateUrl: 'mdColorPickerDialog.tpl.html',
					hasBackdrop: options.hasBackdrop,
					clickOutsideToClose: options.clickOutsideToClose,

					controller: ['$scope', 'options', function( $scope, options ) {
							//console.log( "DIALOG CONTROLLER OPEN", Date.now() - dateClick );
							$scope.close = function close()
                            {
								$mdDialog.cancel();
							};
							$scope.ok = function ok()
							{
								$mdDialog.hide( $scope.value );
							};
							$scope.hide = $scope.ok;



							$scope.value = options.value;
							$scope.default = options.defaultValue;
							$scope.random = options.random;

							$scope.mdColorAlphaChannel = options.mdColorAlphaChannel;
							$scope.mdColorSpectrum = options.mdColorSpectrum;
							$scope.mdColorSliders = options.mdColorSliders;
							$scope.mdColorGenericPalette = options.mdColorGenericPalette;
							$scope.mdColorMaterialPalette = options.mdColorMaterialPalette;
							$scope.mdColorHistory = options.mdColorHistory;
							$scope.mdColorDefaultTab = options.mdColorDefaultTab;

					}],

					locals: {
						options: options,
					},
					preserveScope: options.preserveScope,
  					skipHide: options.skipHide,

					targetEvent: options.$event,
					focusOnOpen: options.focusOnOpen,
					autoWrap: false,
					onShowing: function() {
				//		console.log( "DIALOG OPEN START", Date.now() - dateClick );
					},
					onComplete: function() {
				//		console.log( "DIALOG OPEN COMPLETE", Date.now() - dateClick );
					}
                });

				dialog.then(function (value) {
                    colorHistory.add(new tinycolor(value));
                }, function () { });

                return dialog;
            },
			hide: function() {
				return dialog.hide();
			},
			cancel: function() {
				return dialog.cancel();
			}
		};
	}]);
})( window, window.angular );
