/*
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import tinycolor from 'tinycolor2';

export default angular.module('thingsboard.thirdpartyFix', [])
    .factory('Fullscreen', Fullscreen)
    .factory('$mdColorPicker', mdColorPicker)
    .name;

/*@ngInject*/
function Fullscreen($document, $rootScope) {

    /* eslint-disable */

    var document = $document[0];

    // ensure ALLOW_KEYBOARD_INPUT is available and enabled
    var isKeyboardAvailbleOnFullScreen = (typeof Element !== 'undefined' && 'ALLOW_KEYBOARD_INPUT' in Element) && Element.ALLOW_KEYBOARD_INPUT;

    var emitter = $rootScope.$new();

    // listen event on document instead of element to avoid firefox limitation
    // see https://developer.mozilla.org/en-US/docs/Web/Guide/API/DOM/Using_full_screen_mode
    $document.on('fullscreenchange webkitfullscreenchange mozfullscreenchange MSFullscreenChange', function(){
        emitter.$emit('FBFullscreen.change', serviceInstance.isEnabled());
    });

    var serviceInstance = {
        $on: angular.bind(emitter, emitter.$on),
        all: function() {
            serviceInstance.enable( document.documentElement );
        },
        enable: function(element) {
            if(element.requestFullScreen) {
                element.requestFullScreen();
            } else if(element.mozRequestFullScreen) {
                element.mozRequestFullScreen();
            } else if(element.webkitRequestFullscreen) {
                // Safari temporary fix
                //if (/Version\/[\d]{1,2}(\.[\d]{1,2}){1}(\.(\d){1,2}){0,1} Safari/.test(navigator.userAgent)) {
                if (/Safari/.test(navigator.userAgent)) {
                    element.webkitRequestFullscreen();
                } else {
                    element.webkitRequestFullscreen(isKeyboardAvailbleOnFullScreen);
                }
            } else if (element.msRequestFullscreen) {
                element.msRequestFullscreen();
            }
        },
        cancel: function() {
            if(document.cancelFullScreen) {
                document.cancelFullScreen();
            } else if(document.mozCancelFullScreen) {
                document.mozCancelFullScreen();
            } else if(document.webkitExitFullscreen) {
                document.webkitExitFullscreen();
            } else if (document.msExitFullscreen) {
                document.msExitFullscreen();
            }
        },
        isEnabled: function(){
            var fullscreenElement = document.fullscreenElement || document.mozFullScreenElement || document.webkitFullscreenElement || document.msFullscreenElement;
            return fullscreenElement ? true : false;
        },
        toggleAll: function(){
            serviceInstance.isEnabled() ? serviceInstance.cancel() : serviceInstance.all();
        },
        isSupported: function(){
            var docElm = document.documentElement;
            var requestFullscreen = docElm.requestFullScreen || docElm.mozRequestFullScreen || docElm.webkitRequestFullscreen || docElm.msRequestFullscreen;
            return requestFullscreen ? true : false;
        }
    };

    /* eslint-enable */

    return serviceInstance;
}

/*@ngInject*/
function mdColorPicker($q, $mdDialog, mdColorPickerHistory) {
    var dialog;

    /* eslint-disable angular/definedundefined */

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
            if (options.skipHide !== undefined) {
                options.multiple = options.skipHide;
            }
            if (options.multiple === undefined) {
                options.multiple = true;
            }

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
                multiple: options.multiple,

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
                mdColorPickerHistory.add(new tinycolor(value));
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

    /* eslint-enable angular/definedundefined */
}
