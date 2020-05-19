'use strict';

angular.module('md.data.table').directive('mdSelect', mdSelect);

function mdSelect($compile, $parse) {

  // empty controller to bind scope properties to
  function Controller() {

  }

  function postLink(scope, element, attrs, ctrls) {
    var self = ctrls.shift();
    var tableCtrl = ctrls.shift();
    var getId = $parse(attrs.mdSelectId);

    self.id = getId(self.model);

    if(tableCtrl.$$rowSelect && self.id) {
      if(tableCtrl.$$hash.has(self.id)) {
        var index = tableCtrl.selected.indexOf(tableCtrl.$$hash.get(self.id));

        // if the item is no longer selected remove it
        if(index === -1) {
          tableCtrl.$$hash.purge(self.id);
        }

        // if the item is not a reference to the current model update the reference
        else if(!tableCtrl.$$hash.equals(self.id, self.model)) {
          tableCtrl.$$hash.update(self.id, self.model);
          tableCtrl.selected.splice(index, 1, self.model);
        }

      } else {

        // check if the item has been selected
        tableCtrl.selected.some(function (item, index) {
          if(getId(item) === self.id) {
            tableCtrl.$$hash.update(self.id, self.model);
            tableCtrl.selected.splice(index, 1, self.model);

            return true;
          }
        });
      }
    }

    self.isSelected = function () {
      if(!tableCtrl.$$rowSelect) {
        return false;
      }

      if(self.id) {
        return tableCtrl.$$hash.has(self.id);
      }

      return tableCtrl.selected.indexOf(self.model) !== -1;
    };

    self.select = function () {
      if(self.disabled) {
        return;
      }

      if(tableCtrl.enableMultiSelect()) {
        tableCtrl.selected.push(self.model);
      } else {
        tableCtrl.selected.splice(0, tableCtrl.selected.length, self.model);
      }

      if(angular.isFunction(self.onSelect)) {
        self.onSelect(self.model);
      }
    };

    self.deselect = function () {
      if(self.disabled) {
        return;
      }

      tableCtrl.selected.splice(tableCtrl.selected.indexOf(self.model), 1);

      if(angular.isFunction(self.onDeselect)) {
        self.onDeselect(self.model);
      }
    };

    self.toggle = function (event) {
      if(event && event.stopPropagation) {
        event.stopPropagation();
      }

      return self.isSelected() ? self.deselect() : self.select();
    };

    function autoSelect() {
      return attrs.mdAutoSelect === '' || self.autoSelect;
    }

    function createCheckbox() {
      var checkbox = angular.element('<md-checkbox>').attr({
        'aria-label': 'Select Row',
        'ng-click': '$mdSelect.toggle($event)',
        'ng-checked': '$mdSelect.isSelected()',
        'ng-disabled': '$mdSelect.disabled'
      });

      return angular.element('<td class="md-cell md-checkbox-cell">').append($compile(checkbox)(scope));
    }

    function disableSelection() {
      Array.prototype.some.call(element.children(), function (child) {
        return child.classList.contains('md-checkbox-cell') && element[0].removeChild(child);
      });

      if(autoSelect()) {
        element.off('click', toggle);
      }
    }

    function enableSelection() {
      element.prepend(createCheckbox());

      if(autoSelect()) {
        element.on('click', toggle);
      }
    }

    function enableRowSelection() {
      return tableCtrl.$$rowSelect;
    }

    function onSelectChange(selected) {
      if(!self.id) {
        return;
      }

      if(tableCtrl.$$hash.has(self.id)) {
        // check if the item has been deselected
        if(selected.indexOf(tableCtrl.$$hash.get(self.id)) === -1) {
          tableCtrl.$$hash.purge(self.id);
        }

        return;
      }

      // check if the item has been selected
      if(selected.indexOf(self.model) !== -1) {
        tableCtrl.$$hash.update(self.id, self.model);
      }
    }

    function toggle(event) {
      scope.$applyAsync(function () {
        self.toggle(event);
      });
    }

    scope.$watch(enableRowSelection, function (enable) {
      if(enable) {
        enableSelection();
      } else {
        disableSelection();
      }
    });

    scope.$watch(autoSelect, function (newValue, oldValue) {
      if(newValue === oldValue) {
        return;
      }

      if(tableCtrl.$$rowSelect && newValue) {
        element.on('click', toggle);
      } else {
        element.off('click', toggle);
      }
    });

    scope.$watch(self.isSelected, function (isSelected) {
      return isSelected ? element.addClass('md-selected') : element.removeClass('md-selected');
    });

    scope.$watch(tableCtrl.enableMultiSelect, function (multiple) {
      if(tableCtrl.$$rowSelect && !multiple) {
        // remove all but the first selected item
        tableCtrl.selected.splice(1);
      }
    });

    tableCtrl.registerModelChangeListener(onSelectChange);

    element.on('$destroy', function () {
      tableCtrl.removeModelChangeListener(onSelectChange);
    });
  }

  return {
    bindToController: true,
    controller: Controller,
    controllerAs: '$mdSelect',
    link: postLink,
    require: ['mdSelect', '^^mdTable'],
    restrict: 'A',
    scope: {
      model: '=mdSelect',
      disabled: '=ngDisabled',
      onSelect: '=?mdOnSelect',
      onDeselect: '=?mdOnDeselect',
      autoSelect: '=mdAutoSelect'
    }
  };
}

mdSelect.$inject = ['$compile', '$parse'];