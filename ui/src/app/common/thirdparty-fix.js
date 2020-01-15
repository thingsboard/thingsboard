/*
 * Copyright © 2016-2020 The Thingsboard Authors
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
import moment from 'moment';

export default angular.module('thingsboard.thirdpartyFix', [])
    .factory('Fullscreen', Fullscreen)
    .factory('$mdColorPicker', mdColorPicker)
    .provider('$mdpDatePicker', mdpDatePicker)
    .provider('$mdpTimePicker', mdpTimePicker)
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

function DatePickerCtrl($scope, $mdDialog, $mdMedia, $timeout, currentDate, options) {
    var self = this;

    this.date = moment(currentDate);
    this.minDate = options.minDate && moment(options.minDate).isValid() ? moment(options.minDate) : null;
    this.maxDate = options.maxDate && moment(options.maxDate).isValid() ? moment(options.maxDate) : null;
    this.displayFormat = options.displayFormat || "ddd, MMM DD";
    this.dateFilter = angular.isFunction(options.dateFilter) ? options.dateFilter : null;
    this.selectingYear = false;

    // validate min and max date
    if (this.minDate && this.maxDate) {
        if (this.maxDate.isBefore(this.minDate)) {
            this.maxDate = moment(this.minDate).add(1, 'days');
        }
    }

    if (this.date) {
        // check min date
        if (this.minDate && this.date.isBefore(this.minDate)) {
            this.date = moment(this.minDate);
        }

        // check max date
        if (this.maxDate && this.date.isAfter(this.maxDate)) {
            this.date = moment(this.maxDate);
        }
    }

    this.yearItems = {
        currentIndex_: 0,
        PAGE_SIZE: 5,
        START: (self.minDate ? self.minDate.year() : 1900),
        END: (self.maxDate ? self.maxDate.year() : 0),
        getItemAtIndex: function(index) {
            if(this.currentIndex_ < index)
                this.currentIndex_ = index;

            return this.START + index;
        },
        getLength: function() {
            return Math.min(
                this.currentIndex_ + Math.floor(this.PAGE_SIZE / 2),
                Math.abs(this.START - this.END) + 1
            );
        }
    };

    $scope.$mdMedia = $mdMedia;
    $scope.year = this.date.year();

    this.selectYear = function(year) {
        self.date.year(year);
        $scope.year = year;
        self.selectingYear = false;
        self.animate();
    };

    this.showYear = function() {
        self.yearTopIndex = (self.date.year() - self.yearItems.START) + Math.floor(self.yearItems.PAGE_SIZE / 2);
        self.yearItems.currentIndex_ = (self.date.year() - self.yearItems.START) + 1;
        self.selectingYear = true;
    };

    this.showCalendar = function() {
        self.selectingYear = false;
    };

    this.cancel = function() {
        $mdDialog.cancel();
    };

    this.confirm = function() {
        var date = this.date;

        if (this.minDate && this.date.isBefore(this.minDate)) {
            date = moment(this.minDate);
        }

        if (this.maxDate && this.date.isAfter(this.maxDate)) {
            date = moment(this.maxDate);
        }

        $mdDialog.hide(date.toDate());
    };

    this.animate = function() {
        self.animating = true;
        $timeout(angular.noop).then(function() {
            self.animating = false;
        })
    };
}

/*@ngInject*/
function mdpDatePicker() {
    var LABEL_OK = "OK",
        LABEL_CANCEL = "Cancel",
        DISPLAY_FORMAT = "ddd, MMM DD";

    this.setDisplayFormat = function(format) {
        DISPLAY_FORMAT = format;
    };

    this.setOKButtonLabel = function(label) {
        LABEL_OK = label;
    };

    this.setCancelButtonLabel = function(label) {
        LABEL_CANCEL = label;
    };

    /*@ngInject*/
    this.$get = function($mdDialog) {
        var datePicker = function(currentDate, options) {
            if (!angular.isDate(currentDate)) currentDate = Date.now();
            if (!angular.isObject(options)) options = {};

            options.displayFormat = DISPLAY_FORMAT;

            return $mdDialog.show({
                controller:  ['$scope', '$mdDialog', '$mdMedia', '$timeout', 'currentDate', 'options', DatePickerCtrl],
                controllerAs: 'datepicker',
                clickOutsideToClose: true,
                template: '<md-dialog aria-label="" class="mdp-datepicker" ng-class="{ \'portrait\': !$mdMedia(\'gt-xs\') }">' +
                '<md-dialog-content layout="row" layout-wrap>' +
                '<div layout="column" layout-align="start center">' +
                '<md-toolbar layout-align="start start" flex class="mdp-datepicker-date-wrapper md-hue-1 md-primary" layout="column">' +
                '<span class="mdp-datepicker-year" ng-click="datepicker.showYear()" ng-class="{ \'active\': datepicker.selectingYear }">{{ datepicker.date.format(\'YYYY\') }}</span>' +
                '<span class="mdp-datepicker-date" ng-click="datepicker.showCalendar()" ng-class="{ \'active\': !datepicker.selectingYear }">{{ datepicker.date.format(datepicker.displayFormat) }}</span> ' +
                '</md-toolbar>' +
                '</div>' +
                '<div>' +
                '<div class="mdp-datepicker-select-year mdp-animation-zoom" layout="column" layout-align="center start" ng-if="datepicker.selectingYear">' +
                '<md-virtual-repeat-container md-auto-shrink md-top-index="datepicker.yearTopIndex">' +
                '<div flex md-virtual-repeat="item in datepicker.yearItems" md-on-demand class="repeated-year">' +
                '<span class="md-button" ng-click="datepicker.selectYear(item)" md-ink-ripple ng-class="{ \'md-primary current\': item == year }">{{ item }}</span>' +
                '</div>' +
                '</md-virtual-repeat-container>' +
                '</div>' +
                '<mdp-calendar ng-if="!datepicker.selectingYear" class="mdp-animation-zoom" date="datepicker.date" min-date="datepicker.minDate" date-filter="datepicker.dateFilter" max-date="datepicker.maxDate"></mdp-calendar>' +
                '<md-dialog-actions layout="row">' +
                '<span flex></span>' +
                '<md-button ng-click="datepicker.cancel()" aria-label="' + LABEL_CANCEL + '">' + LABEL_CANCEL + '</md-button>' +
                '<md-button ng-click="datepicker.confirm()" class="md-primary" aria-label="' + LABEL_OK + '">' + LABEL_OK + '</md-button>' +
                '</md-dialog-actions>' +
                '</div>' +
                '</md-dialog-content>' +
                '</md-dialog>',
                targetEvent: options.targetEvent,
                locals: {
                    currentDate: currentDate,
                    options: options
                },
                multiple: true
            });
        };

        return datePicker;
    };

}

function TimePickerCtrl($scope, $mdDialog, time, autoSwitch, $mdMedia) {
    var self = this;
    this.VIEW_HOURS = 1;
    this.VIEW_MINUTES = 2;
    this.currentView = this.VIEW_HOURS;
    this.time = moment(time);
    this.autoSwitch = !!autoSwitch;

    this.clockHours = parseInt(this.time.format("h"));
    this.clockMinutes = parseInt(this.time.minutes());

    $scope.$mdMedia = $mdMedia;

    this.switchView = function() {
        self.currentView = self.currentView == self.VIEW_HOURS ? self.VIEW_MINUTES : self.VIEW_HOURS;
    };

    this.setAM = function() {
        if(self.time.hours() >= 12)
            self.time.hour(self.time.hour() - 12);
    };

    this.setPM = function() {
        if(self.time.hours() < 12)
            self.time.hour(self.time.hour() + 12);
    };

    this.cancel = function() {
        $mdDialog.cancel();
    };

    this.confirm = function() {
        $mdDialog.hide(this.time.toDate());
    };
}

/*@ngInject*/
function mdpTimePicker() {
    var LABEL_OK = "OK",
        LABEL_CANCEL = "Cancel";

    this.setOKButtonLabel = function(label) {
        LABEL_OK = label;
    };

    this.setCancelButtonLabel = function(label) {
        LABEL_CANCEL = label;
    };

    /*@ngInject*/
    this.$get = function($mdDialog) {
        var timePicker = function(time, options) {
            if(!angular.isDate(time)) time = Date.now();
            if (!angular.isObject(options)) options = {};

            return $mdDialog.show({
                controller:  ['$scope', '$mdDialog', 'time', 'autoSwitch', '$mdMedia', TimePickerCtrl],
                controllerAs: 'timepicker',
                clickOutsideToClose: true,
                template: '<md-dialog aria-label="" class="mdp-timepicker" ng-class="{ \'portrait\': !$mdMedia(\'gt-xs\') }">' +
                '<md-dialog-content layout-gt-xs="row" layout-wrap>' +
                '<md-toolbar layout-gt-xs="column" layout-xs="row" layout-align="center center" flex class="mdp-timepicker-time md-hue-1 md-primary">' +
                '<div class="mdp-timepicker-selected-time">' +
                '<span ng-class="{ \'active\': timepicker.currentView == timepicker.VIEW_HOURS }" ng-click="timepicker.currentView = timepicker.VIEW_HOURS">{{ timepicker.time.format("h") }}</span>:' +
                '<span ng-class="{ \'active\': timepicker.currentView == timepicker.VIEW_MINUTES }" ng-click="timepicker.currentView = timepicker.VIEW_MINUTES">{{ timepicker.time.format("mm") }}</span>' +
                '</div>' +
                '<div layout="column" class="mdp-timepicker-selected-ampm">' +
                '<span ng-click="timepicker.setAM()" ng-class="{ \'active\': timepicker.time.hours() < 12 }">AM</span>' +
                '<span ng-click="timepicker.setPM()" ng-class="{ \'active\': timepicker.time.hours() >= 12 }">PM</span>' +
                '</div>' +
                '</md-toolbar>' +
                '<div>' +
                '<div class="mdp-clock-switch-container" ng-switch="timepicker.currentView" layout layout-align="center center">' +
                '<mdp-clock class="mdp-animation-zoom" auto-switch="timepicker.autoSwitch" time="timepicker.time" type="hours" ng-switch-when="1"></mdp-clock>' +
                '<mdp-clock class="mdp-animation-zoom" auto-switch="timepicker.autoSwitch" time="timepicker.time" type="minutes" ng-switch-when="2"></mdp-clock>' +
                '</div>' +

                '<md-dialog-actions layout="row">' +
                '<span flex></span>' +
                '<md-button ng-click="timepicker.cancel()" aria-label="' + LABEL_CANCEL + '">' + LABEL_CANCEL + '</md-button>' +
                '<md-button ng-click="timepicker.confirm()" class="md-primary" aria-label="' + LABEL_OK + '">' + LABEL_OK + '</md-button>' +
                '</md-dialog-actions>' +
                '</div>' +
                '</md-dialog-content>' +
                '</md-dialog>',
                targetEvent: options.targetEvent,
                locals: {
                    time: time,
                    autoSwitch: options.autoSwitch
                },
                multiple: true
            });
        };

        return timePicker;
    };
}
