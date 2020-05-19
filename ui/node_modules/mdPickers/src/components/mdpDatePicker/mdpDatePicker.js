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