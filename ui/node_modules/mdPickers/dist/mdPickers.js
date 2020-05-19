(function() {
"use strict";
/* global moment, angular */

var module = angular.module("mdPickers", [
	"ngMaterial",
	"ngAnimate",
	"ngAria"
]);

module.config(["$mdIconProvider", "mdpIconsRegistry", function($mdIconProvider, mdpIconsRegistry) {
	angular.forEach(mdpIconsRegistry, function(icon, index) {
		$mdIconProvider.icon(icon.id, icon.url);
	});
}]);

module.run(["$templateCache", "mdpIconsRegistry", function($templateCache, mdpIconsRegistry) {
	angular.forEach(mdpIconsRegistry, function(icon, index) {
		$templateCache.put(icon.url, icon.svg);
	});
}]);
module.constant("mdpIconsRegistry", [
    {
        id: 'mdp-chevron-left',
        url: 'mdp-chevron-left.svg',
        svg: '<svg height="24" viewBox="0 0 24 24" width="24" xmlns="http://www.w3.org/2000/svg"><path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/><path d="M0 0h24v24H0z" fill="none"/></svg>'
    },
    {
        id: 'mdp-chevron-right',
        url: 'mdp-chevron-right.svg',
        svg: '<svg height="24" viewBox="0 0 24 24" width="24" xmlns="http://www.w3.org/2000/svg"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/><path d="M0 0h24v24H0z" fill="none"/></svg>'
    },
    {
        id: 'mdp-access-time',
        url: 'mdp-access-time.svg',
        svg: '<svg height="24" viewBox="0 0 24 24" width="24" xmlns="http://www.w3.org/2000/svg"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z"/><path d="M0 0h24v24H0z" fill="none"/><path d="M12.5 7H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>'
    },
    {
        id: 'mdp-event',
        url: 'mdp-event.svg',
        svg: '<svg height="24" viewBox="0 0 24 24" width="24" xmlns="http://www.w3.org/2000/svg"><path d="M17 12h-5v5h5v-5zM16 1v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-1V1h-2zm3 18H5V8h14v11z"/><path d="M0 0h24v24H0z" fill="none"/></svg>'
    }
]);
module.directive("ngMessage", ["$mdUtil", function($mdUtil) {
   return {
        restrict: "EA",
        priority: 101,
        compile: function(element) {
            var inputContainer = $mdUtil.getClosest(element, "mdp-time-picker", true) ||
                                 $mdUtil.getClosest(element, "mdp-date-picker", true);
            
            // If we are not a child of an input container, don't do anything
            if (!inputContainer) return;
            
            // Add our animation class
            element.toggleClass('md-input-message-animation', true);
            
            return {};
        }
    } 
}]);
/* global moment, angular */

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

module.provider("$mdpDatePicker", function() {
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
    
    this.$get = ["$mdDialog", function($mdDialog) {
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
                skipHide: true
            });
        };
    
        return datePicker;
    }];
});

function CalendarCtrl($scope) {
	var self = this;
	this.dow = moment.localeData().firstDayOfWeek();
	
    this.weekDays = [].concat(
        moment.weekdaysMin().slice(
            this.dow
        ),
        moment.weekdaysMin().slice(
            0, 
            this.dow
        )
    );
    
    this.daysInMonth = [];
    
    this.getDaysInMonth = function() {
        var days = self.date.daysInMonth(),
            firstDay = moment(self.date).date(1).day() - this.dow;
            
        if(firstDay < 0) firstDay = this.weekDays.length - 1;
            

        var arr = [];
        for(var i = 1; i <= (firstDay + days); i++) {
            var day = null;
            if(i > firstDay) {
                day =  {
                    value: (i - firstDay),
                    enabled: self.isDayEnabled(moment(self.date).date(i - firstDay).toDate())
                };
            }
            arr.push(day);
        }
 
        return arr;
    };
    
    this.isDayEnabled = function(day) {
        return (!this.minDate || this.minDate <= day) && 
            (!this.maxDate || this.maxDate >= day) && 
            (!self.dateFilter || !self.dateFilter(day));
    };
    
    this.selectDate = function(dom) {
        self.date.date(dom);
    };

    this.nextMonth = function() {
        self.date.add(1, 'months');
    };

    this.prevMonth = function() {
        self.date.subtract(1, 'months');
    };
    
    this.updateDaysInMonth = function() {
        self.daysInMonth = self.getDaysInMonth();
    };
    
    $scope.$watch(function() { return  self.date.unix() }, function(newValue, oldValue) {
        if(newValue && newValue !== oldValue)
            self.updateDaysInMonth();
    })
    
    self.updateDaysInMonth();
}

module.directive("mdpCalendar", ["$animate", function($animate) {
    return {
        restrict: 'E',
        bindToController: {
            "date": "=",
            "minDate": "=",
            "maxDate": "=",
            "dateFilter": "="
        },
        template: '<div class="mdp-calendar">' +
                    '<div layout="row" layout-align="space-between center">' +
                        '<md-button aria-label="previous month" class="md-icon-button" ng-click="calendar.prevMonth()"><md-icon md-svg-icon="mdp-chevron-left"></md-icon></md-button>' +
                        '<div class="mdp-calendar-monthyear" ng-show="!calendar.animating">{{ calendar.date.format("MMMM YYYY") }}</div>' +
                        '<md-button aria-label="next month" class="md-icon-button" ng-click="calendar.nextMonth()"><md-icon md-svg-icon="mdp-chevron-right"></md-icon></md-button>' +
                    '</div>' +
                    '<div layout="row" layout-align="space-around center" class="mdp-calendar-week-days" ng-show="!calendar.animating">' +
                        '<div layout layout-align="center center" ng-repeat="d in calendar.weekDays track by $index">{{ d }}</div>' +
                    '</div>' +
                    '<div layout="row" layout-align="start center" layout-wrap class="mdp-calendar-days" ng-class="{ \'mdp-animate-next\': calendar.animating }" ng-show="!calendar.animating" md-swipe-left="calendar.nextMonth()" md-swipe-right="calendar.prevMonth()">' +
                        '<div layout layout-align="center center" ng-repeat-start="day in calendar.daysInMonth track by $index" ng-class="{ \'mdp-day-placeholder\': !day }">' +
                            '<md-button class="md-icon-button md-raised" aria-label="Select day" ng-if="day" ng-class="{ \'md-accent\': calendar.date.date() == day.value }" ng-click="calendar.selectDate(day.value)" ng-disabled="!day.enabled">{{ day.value }}</md-button>' +
                        '</div>' +
                        '<div flex="100" ng-if="($index + 1) % 7 == 0" ng-repeat-end></div>' +
                    '</div>' +
                '</div>',
        controller: ["$scope", CalendarCtrl],
        controllerAs: "calendar",
        link: function(scope, element, attrs, ctrl) {
            var animElements = [
                element[0].querySelector(".mdp-calendar-week-days"),
                element[0].querySelector('.mdp-calendar-days'),
                element[0].querySelector('.mdp-calendar-monthyear')
            ].map(function(a) {
               return angular.element(a); 
            });
                
            scope.$watch(function() { return  ctrl.date.format("YYYYMM") }, function(newValue, oldValue) {
                var direction = null;
                
                if(newValue > oldValue)
                    direction = "mdp-animate-next";
                else if(newValue < oldValue)
                    direction = "mdp-animate-prev";
                
                if(direction) {
                    for(var i in animElements) {
                        animElements[i].addClass(direction);
                        $animate.removeClass(animElements[i], direction);
                    }
                }
            });
        }
    }
}]);

function formatValidator(value, format) {
    return !value || angular.isDate(value) || moment(value, format, true).isValid();
}

function minDateValidator(value, format, minDate) {
    var minDate = moment(minDate, "YYYY-MM-DD", true);
    var date = angular.isDate(value) ? moment(value) :  moment(value, format, true);
    
    return !value || 
            angular.isDate(value) || 
            !minDate.isValid() || 
            date.isSameOrAfter(minDate);
}

function maxDateValidator(value, format, maxDate) {
    var maxDate = moment(maxDate, "YYYY-MM-DD", true);
    var date = angular.isDate(value) ? moment(value) :  moment(value, format, true);
    
    return !value || 
            angular.isDate(value) || 
            !maxDate.isValid() || 
            date.isSameOrBefore(maxDate);
}

function filterValidator(value, format, filter) {
    var date = angular.isDate(value) ? moment(value) :  moment(value, format, true);
                
    return !value || 
            angular.isDate(value) || 
            !angular.isFunction(filter) || 
            !filter(date);
}

function requiredValidator(value, ngModel) {
    return value
}

module.directive("mdpDatePicker", ["$mdpDatePicker", "$timeout", function($mdpDatePicker, $timeout) {
    return  {
        restrict: 'E',
        require: 'ngModel',
        transclude: true,
        template: function(element, attrs) {
            var noFloat = angular.isDefined(attrs.mdpNoFloat),
                placeholder = angular.isDefined(attrs.mdpPlaceholder) ? attrs.mdpPlaceholder : "",
                openOnClick = angular.isDefined(attrs.mdpOpenOnClick) ? true : false;
            
            return '<div layout layout-align="start start">' +
                    '<md-button' + (angular.isDefined(attrs.mdpDisabled) ? ' ng-disabled="disabled"' : '') + ' class="md-icon-button" ng-click="showPicker($event)">' +
                        '<md-icon md-svg-icon="mdp-event"></md-icon>' +
                    '</md-button>' +
                    '<md-input-container' + (noFloat ? ' md-no-float' : '') + ' md-is-error="isError()">' +
                        '<input type="{{ ::type }}"' + (angular.isDefined(attrs.mdpDisabled) ? ' ng-disabled="disabled"' : '') + ' aria-label="' + placeholder + '" placeholder="' + placeholder + '"' + (openOnClick ? ' ng-click="showPicker($event)" ' : '') + ' />' +
                    '</md-input-container>' +
                '</div>';
        },
        scope: {
            "minDate": "=mdpMinDate",
            "maxDate": "=mdpMaxDate",
            "dateFilter": "=mdpDateFilter",
            "dateFormat": "@mdpFormat",
            "placeholder": "@mdpPlaceholder",
            "noFloat": "=mdpNoFloat",
            "openOnClick": "=mdpOpenOnClick",
            "disabled": "=?mdpDisabled"
        },
        link: {
            pre: function(scope, element, attrs, ngModel, $transclude) {
                
            },
            post: function(scope, element, attrs, ngModel, $transclude) {
                var inputElement = angular.element(element[0].querySelector('input')),
                    inputContainer = angular.element(element[0].querySelector('md-input-container')),
                    inputContainerCtrl = inputContainer.controller("mdInputContainer");
                    
                $transclude(function(clone) {
                   inputContainer.append(clone); 
                });  
                
                var messages = angular.element(inputContainer[0].querySelector("[ng-messages]"));
                
                scope.type = scope.dateFormat ? "text" : "date"
                scope.dateFormat = scope.dateFormat || "YYYY-MM-DD";
                scope.model = ngModel;
                
                scope.isError = function() {
                    return !ngModel.$pristine && !!ngModel.$invalid;
                };
                
                // update input element if model has changed
                ngModel.$formatters.unshift(function(value) {
                    var date = angular.isDate(value) && moment(value);
                    if(date && date.isValid()) 
                        updateInputElement(date.format(scope.dateFormat));
                    else
                        updateInputElement(null);
                });
                
                ngModel.$validators.format = function(modelValue, viewValue) {
                    return formatValidator(viewValue, scope.dateFormat);
                };
                
                ngModel.$validators.minDate = function(modelValue, viewValue) {
                    return minDateValidator(viewValue, scope.dateFormat, scope.minDate);
                };
                
                ngModel.$validators.maxDate = function(modelValue, viewValue) {
                    return maxDateValidator(viewValue, scope.dateFormat, scope.maxDate);
                };
                
                ngModel.$validators.filter = function(modelValue, viewValue) {
                    return filterValidator(viewValue, scope.dateFormat, scope.dateFilter);
                };
                
                ngModel.$validators.required = function(modelValue, viewValue) {
                    return angular.isUndefined(attrs.required) || !ngModel.$isEmpty(modelValue) || !ngModel.$isEmpty(viewValue);
                };
                
                ngModel.$parsers.unshift(function(value) {
                    var parsed = moment(value, scope.dateFormat, true);
                    if(parsed.isValid()) {
                        if(angular.isDate(ngModel.$modelValue)) {
                            var originalModel = moment(ngModel.$modelValue);
                            originalModel.year(parsed.year());
                            originalModel.month(parsed.month());
                            originalModel.date(parsed.date());
                            
                            parsed = originalModel;
                        }
                        return parsed.toDate(); 
                    } else
                        return null;
                });
                
                // update input element value
                function updateInputElement(value) {
                    inputElement[0].value = value;
                    inputContainerCtrl.setHasValue(!ngModel.$isEmpty(value));
                }
                
                function updateDate(date) {
                    var value = moment(date, angular.isDate(date) ? null : scope.dateFormat, true),
                        strValue = value.format(scope.dateFormat);
    
                    if(value.isValid()) {
                        updateInputElement(strValue);
                        ngModel.$setViewValue(strValue);
                    } else {
                        updateInputElement(date);
                        ngModel.$setViewValue(date);
                    }
                    
                    if(!ngModel.$pristine && 
                        messages.hasClass("md-auto-hide") && 
                        inputContainer.hasClass("md-input-invalid")) messages.removeClass("md-auto-hide");
    
                    ngModel.$render();
                }
                    
                scope.showPicker = function(ev) {
                    $mdpDatePicker(ngModel.$modelValue, {
                	    minDate: scope.minDate, 
                	    maxDate: scope.maxDate,
                	    dateFilter: scope.dateFilter,
                	    targetEvent: ev
            	    }).then(updateDate);
                };
                
                function onInputElementEvents(event) {
                    if(event.target.value !== ngModel.$viewVaue)
                        updateDate(event.target.value);
                }
                
                inputElement.on("reset input blur", onInputElementEvents);
                
                scope.$on("$destroy", function() {
                    inputElement.off("reset input blur", onInputElementEvents);
                });
            }
        }
    };
}]);


module.directive("mdpDatePicker", ["$mdpDatePicker", "$timeout", function($mdpDatePicker, $timeout) {
    return  {
        restrict: 'A',
        require: 'ngModel',
        scope: {
            "minDate": "@min",
            "maxDate": "@max",
            "dateFilter": "=mdpDateFilter",
            "dateFormat": "@mdpFormat",
        },
        link: function(scope, element, attrs, ngModel, $transclude) {
            scope.dateFormat = scope.dateFormat || "YYYY-MM-DD";
            
            ngModel.$validators.format = function(modelValue, viewValue) {
                return formatValidator(viewValue, scope.format);
            };
            
            ngModel.$validators.minDate = function(modelValue, viewValue) {
                return minDateValidator(viewValue, scope.format, scope.minDate);
            };
            
            ngModel.$validators.maxDate = function(modelValue, viewValue) {
                return maxDateValidator(viewValue, scope.format, scope.maxDate);
            };
            
            ngModel.$validators.filter = function(modelValue, viewValue) {
                return filterValidator(viewValue, scope.format, scope.dateFilter);
            };
            
            function showPicker(ev) {
                $mdpDatePicker(ngModel.$modelValue, {
            	    minDate: scope.minDate, 
            	    maxDate: scope.maxDate,
            	    dateFilter: scope.dateFilter,
            	    targetEvent: ev
        	    }).then(function(time) {
                    ngModel.$setViewValue(moment(time).format(scope.format));
                    ngModel.$render();
                });
            };
            
            element.on("click", showPicker);
            
            scope.$on("$destroy", function() {
                element.off("click", showPicker);
            });
        }
    }
}]);
/* global moment, angular */

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

function ClockCtrl($scope) {
    var TYPE_HOURS = "hours";
    var TYPE_MINUTES = "minutes";
    var self = this;
    
    this.STEP_DEG = 360 / 12;
    this.steps = [];
    
    this.CLOCK_TYPES = {
        "hours": {
            range: 12,
        },
        "minutes": {
            range: 60,
        }
    }
    
    this.getPointerStyle = function() {
        var divider = 1;
        switch(self.type) {
            case TYPE_HOURS:
                divider = 12;
                break;
            case TYPE_MINUTES:
                divider = 60;
                break;
        }  
        var degrees = Math.round(self.selected * (360 / divider)) - 180;
        return { 
            "-webkit-transform": "rotate(" + degrees + "deg)",
            "-ms-transform": "rotate(" + degrees + "deg)",
            "transform": "rotate(" + degrees + "deg)"
        }
    };
    
    this.setTimeByDeg = function(deg) {
        deg = deg >= 360 ? 0 : deg;
        var divider = 0;
        switch(self.type) {
            case TYPE_HOURS:
                divider = 12;
                break;
            case TYPE_MINUTES:
                divider = 60;
                break;
        }  
        
        self.setTime(
            Math.round(divider / 360 * deg)
        );
    };
    
    this.setTime = function(time, type) {
        this.selected = time;
        
        switch(self.type) {
            case TYPE_HOURS:
                if(self.time.format("A") == "PM") time += 12;
                this.time.hours(time);
                break;
            case TYPE_MINUTES:
                if(time > 59) time -= 60;
                this.time.minutes(time);
                break;
        }
        
    };
    
    this.init = function() {
        self.type = self.type || "hours";
        switch(self.type) {
            case TYPE_HOURS:
                for(var i = 1; i <= 12; i++)
                    self.steps.push(i);
                self.selected = self.time.hours() || 0;
                if(self.selected > 12) self.selected -= 12;
                    
                break;
            case TYPE_MINUTES:
                for(var i = 5; i <= 55; i+=5)
                    self.steps.push(i);
                self.steps.push(0);
                self.selected = self.time.minutes() || 0;
                
                break;
        }
    };
    
    this.init();
}

module.directive("mdpClock", ["$animate", "$timeout", function($animate, $timeout) {
    return {
        restrict: 'E',
        bindToController: {
            'type': '@?',
            'time': '=',
            'autoSwitch': '=?'
        },
        replace: true,
        template: '<div class="mdp-clock">' +
                        '<div class="mdp-clock-container">' +
                            '<md-toolbar class="mdp-clock-center md-primary"></md-toolbar>' +
                            '<md-toolbar ng-style="clock.getPointerStyle()" class="mdp-pointer md-primary">' +
                                '<span class="mdp-clock-selected md-button md-raised md-primary"></span>' +
                            '</md-toolbar>' +
                            '<md-button ng-class="{ \'md-primary\': clock.selected == step }" class="md-icon-button md-raised mdp-clock-deg{{ ::(clock.STEP_DEG * ($index + 1)) }}" ng-repeat="step in clock.steps" ng-click="clock.setTime(step)">{{ step }}</md-button>' +
                        '</div>' +
                    '</div>',
        controller: ["$scope", ClockCtrl],
        controllerAs: "clock",
        link: function(scope, element, attrs, ctrl) {
            var pointer = angular.element(element[0].querySelector(".mdp-pointer")),
                timepickerCtrl = scope.$parent.timepicker;
            
            var onEvent = function(event) {
                var containerCoords = event.currentTarget.getClientRects()[0];
                var x = ((event.currentTarget.offsetWidth / 2) - (event.pageX - containerCoords.left)),
                    y = ((event.pageY - containerCoords.top) - (event.currentTarget.offsetHeight / 2));

                var deg = Math.round((Math.atan2(x, y) * (180 / Math.PI)));
                $timeout(function() {
                    ctrl.setTimeByDeg(deg + 180);
                    if(ctrl.autoSwitch && ["mouseup", "click"].indexOf(event.type) !== -1 && timepickerCtrl) timepickerCtrl.switchView();
                });
            }; 
            
            element.on("mousedown", function() {
               element.on("mousemove", onEvent);
            });
            
            element.on("mouseup", function(e) {
                element.off("mousemove");
            });
            
            element.on("click", onEvent);
            scope.$on("$destroy", function() {
                element.off("click", onEvent);
                element.off("mousemove", onEvent); 
            });
        }
    }
}]);

module.provider("$mdpTimePicker", function() {
    var LABEL_OK = "OK",
        LABEL_CANCEL = "Cancel";
        
    this.setOKButtonLabel = function(label) {
        LABEL_OK = label;
    };
    
    this.setCancelButtonLabel = function(label) {
        LABEL_CANCEL = label;
    };
    
    this.$get = ["$mdDialog", function($mdDialog) {
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
                skipHide: true
            });
        };
    
        return timePicker;
    }];
});

module.directive("mdpTimePicker", ["$mdpTimePicker", "$timeout", function($mdpTimePicker, $timeout) {
    return  {
        restrict: 'E',
        require: 'ngModel',
        transclude: true,
        template: function(element, attrs) {
            var noFloat = angular.isDefined(attrs.mdpNoFloat),
                placeholder = angular.isDefined(attrs.mdpPlaceholder) ? attrs.mdpPlaceholder : "",
                openOnClick = angular.isDefined(attrs.mdpOpenOnClick) ? true : false;
            
            return '<div layout layout-align="start start">' +
                    '<md-button class="md-icon-button" ng-click="showPicker($event)"' + (angular.isDefined(attrs.mdpDisabled) ? ' ng-disabled="disabled"' : '') + '>' +
                        '<md-icon md-svg-icon="mdp-access-time"></md-icon>' +
                    '</md-button>' +
                    '<md-input-container' + (noFloat ? ' md-no-float' : '') + ' md-is-error="isError()">' +
                        '<input type="{{ ::type }}"' + (angular.isDefined(attrs.mdpDisabled) ? ' ng-disabled="disabled"' : '') + ' aria-label="' + placeholder + '" placeholder="' + placeholder + '"' + (openOnClick ? ' ng-click="showPicker($event)" ' : '') + ' />' +
                    '</md-input-container>' +
                '</div>';
        },
        scope: {
            "timeFormat": "@mdpFormat",
            "placeholder": "@mdpPlaceholder",
            "autoSwitch": "=?mdpAutoSwitch",
            "disabled": "=?mdpDisabled"
        },
        link: function(scope, element, attrs, ngModel, $transclude) {
            var inputElement = angular.element(element[0].querySelector('input')),
                inputContainer = angular.element(element[0].querySelector('md-input-container')),
                inputContainerCtrl = inputContainer.controller("mdInputContainer");
                
            $transclude(function(clone) {
               inputContainer.append(clone); 
            });
            
            var messages = angular.element(inputContainer[0].querySelector("[ng-messages]"));
            
            scope.type = scope.timeFormat ? "text" : "time"
            scope.timeFormat = scope.timeFormat || "HH:mm";
            scope.autoSwitch = scope.autoSwitch || false;
            
            scope.$watch(function() { return ngModel.$error }, function(newValue, oldValue) {
                inputContainerCtrl.setInvalid(!ngModel.$pristine && !!Object.keys(ngModel.$error).length);
            }, true);
            
            // update input element if model has changed
            ngModel.$formatters.unshift(function(value) {
                var time = angular.isDate(value) && moment(value);
                if(time && time.isValid()) 
                    updateInputElement(time.format(scope.timeFormat));
                else
                    updateInputElement(null);
            });
            
            ngModel.$validators.format = function(modelValue, viewValue) {
                return !viewValue || angular.isDate(viewValue) || moment(viewValue, scope.timeFormat, true).isValid();
            };
            
            ngModel.$validators.required = function(modelValue, viewValue) {
                return angular.isUndefined(attrs.required) || !ngModel.$isEmpty(modelValue) || !ngModel.$isEmpty(viewValue);
            };
            
            ngModel.$parsers.unshift(function(value) {
                var parsed = moment(value, scope.timeFormat, true);
                if(parsed.isValid()) {
                    if(angular.isDate(ngModel.$modelValue)) {
                        var originalModel = moment(ngModel.$modelValue);
                        originalModel.minutes(parsed.minutes());
                        originalModel.hours(parsed.hours());
                        originalModel.seconds(parsed.seconds());
                        
                        parsed = originalModel;
                    }
                    return parsed.toDate(); 
                } else
                    return null;
            });
            
            // update input element value
            function updateInputElement(value) {
                inputElement[0].value = value;
                inputContainerCtrl.setHasValue(!ngModel.$isEmpty(value));
            }
            
            function updateTime(time) {
                var value = moment(time, angular.isDate(time) ? null : scope.timeFormat, true),
                    strValue = value.format(scope.timeFormat);

                if(value.isValid()) {
                    updateInputElement(strValue);
                    ngModel.$setViewValue(strValue);
                } else {
                    updateInputElement(time);
                    ngModel.$setViewValue(time);
                }
                
                if(!ngModel.$pristine && 
                    messages.hasClass("md-auto-hide") && 
                    inputContainer.hasClass("md-input-invalid")) messages.removeClass("md-auto-hide");
                
                ngModel.$render();
            }
                
            scope.showPicker = function(ev) {
                $mdpTimePicker(ngModel.$modelValue, {
                    targetEvent: ev,
                    autoSwitch: scope.autoSwitch
                }).then(function(time) {
                    updateTime(time, true);
                });
            };
            
            function onInputElementEvents(event) {
                if(event.target.value !== ngModel.$viewVaue)
                    updateTime(event.target.value);
            }
            
            inputElement.on("reset input blur", onInputElementEvents);
            
            scope.$on("$destroy", function() {
                inputElement.off("reset input blur", onInputElementEvents);
            })
        }
    };
}]);

module.directive("mdpTimePicker", ["$mdpTimePicker", "$timeout", function($mdpTimePicker, $timeout) {
    return  {
        restrict: 'A',
        require: 'ngModel',
        scope: {
            "timeFormat": "@mdpFormat",
            "autoSwitch": "=?mdpAutoSwitch",
        },
        link: function(scope, element, attrs, ngModel, $transclude) {
            scope.format = scope.format || "HH:mm";
            function showPicker(ev) {
                $mdpTimePicker(ngModel.$modelValue, {
                    targetEvent: ev,
                    autoSwitch: scope.autoSwitch
                }).then(function(time) {
                    ngModel.$setViewValue(moment(time).format(scope.format));
                    ngModel.$render();
                });
            };
            
            element.on("click", showPicker);
            
            scope.$on("$destroy", function() {
                element.off("click", showPicker);
            });
        }
    }
}]);

})();