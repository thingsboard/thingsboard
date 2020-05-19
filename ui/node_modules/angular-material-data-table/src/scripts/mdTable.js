'use strict';

angular.module('md.data.table').directive('mdTable', mdTable);

function Hash() {
  var keys = {};
  
  this.equals = function (key, item) {
    return keys[key] === item;
  };

  this.get = function (key) {
    return keys[key];
  };
  
  this.has = function (key) {
    return keys.hasOwnProperty(key);
  };

  this.purge = function (key) {
    delete keys[key];
  };
  
  this.update = function (key, item) {
    keys[key] = item;
  };
}

function mdTable() {
  
  function compile(tElement, tAttrs) {
    tElement.addClass('md-table');
    
    if(tAttrs.hasOwnProperty('mdProgress')) {
      var body = tElement.find('tbody')[0];
      var progress = angular.element('<thead class="md-table-progress" md-table-progress>');
      
      if(body) {
        tElement[0].insertBefore(progress[0], body);
      }
    }
  }
  
  function Controller($attrs, $element, $q, $scope) {
    var self = this;
    var queue = [];
    var watchListener;
    var modelChangeListeners = [];
    
    self.$$hash = new Hash();
    self.$$columns = {};
    
    function enableRowSelection() {
      self.$$rowSelect = true;
      
      watchListener = $scope.$watchCollection('$mdTable.selected', function (selected) {
        modelChangeListeners.forEach(function (listener) {
          listener(selected);
        });
      });
      
      $element.addClass('md-row-select');
    }
    
    function disableRowSelection() {
      self.$$rowSelect = false;
      
      if(angular.isFunction(watchListener)) {
        watchListener();
      }
      
      $element.removeClass('md-row-select');
    }
    
    function resolvePromises() {
      if(!queue.length) {
        return $scope.$applyAsync();
      }
      
      queue[0]['finally'](function () {
        queue.shift();
        resolvePromises();
      });
    }
    
    function rowSelect() {
      return $attrs.mdRowSelect === '' || self.rowSelect;
    }
    
    function validateModel() {
      if(!self.selected) {
        return console.error('Row selection: ngModel is not defined.');
      }
      
      if(!angular.isArray(self.selected)) {
        return console.error('Row selection: Expected an array. Recived ' + typeof self.selected + '.');
      }
      
      return true;
    }
    
    self.columnCount = function () {
      return self.getRows($element[0]).reduce(function (count, row) {
        return row.cells.length > count ? row.cells.length : count;
      }, 0);
    };
    
    self.getRows = function (element) {
      return Array.prototype.filter.call(element.rows, function (row) {
        return !row.classList.contains('ng-leave');
      });
    };
    
    self.getBodyRows = function () {
      return Array.prototype.reduce.call($element.prop('tBodies'), function (result, tbody) {
        return result.concat(self.getRows(tbody));
      }, []);
    };
    
    self.getElement = function () {
      return $element;
    };
    
    self.getHeaderRows = function () {
      return self.getRows($element.prop('tHead'));
    };
    
    self.enableMultiSelect = function () {
      return $attrs.multiple === '' || $scope.$eval($attrs.multiple);
    };
    
    self.waitingOnPromise = function () {
      return !!queue.length;
    };
    
    self.queuePromise = function (promise) {
      if(!promise) {
        return;
      }
      
      if(queue.push(angular.isArray(promise) ? $q.all(promise) : $q.when(promise)) === 1) {
        resolvePromises();
      }
    };
    
    self.registerModelChangeListener = function (listener) {
      modelChangeListeners.push(listener);
    };
    
    self.removeModelChangeListener = function (listener) {
      var index = modelChangeListeners.indexOf(listener);
      
      if(index !== -1) {
        modelChangeListeners.splice(index, 1);
      }
    };
    
    if($attrs.hasOwnProperty('mdProgress')) {
      $scope.$watch('$mdTable.progress', self.queuePromise);
    }
    
    $scope.$watch(rowSelect, function (enable) {
      if(enable && !!validateModel()) {
        enableRowSelection();
      } else {
        disableRowSelection();
      }
    });
  }
  
  Controller.$inject = ['$attrs', '$element', '$q', '$scope'];
  
  return {
    bindToController: true,
    compile: compile,
    controller: Controller,
    controllerAs: '$mdTable',
    restrict: 'A',
    scope: {
      progress: '=?mdProgress',
      selected: '=ngModel',
      rowSelect: '=mdRowSelect'
    }
  };
}