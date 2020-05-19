(function(){"use strict";angular
  .module('material.components.expansionPanels')
  .directive('mdExpansionPanelGroup', expansionPanelGroupDirective);

/**
 * @ngdoc directive
 * @name mdExpansionPanelGroup
 * @module material.components.expansionPanels
 *
 * @restrict E
 *
 * @description
 * `mdExpansionPanelGroup` is a container used to manage multiple expansion panels
 *
 * @param {string=} md-component-id - add an id if you want to acces the panel via the `$mdExpansionPanelGroup` service
 * @param {string=} auto-expand - panels expand when added to `<md-expansion-panel-group>`
 * @param {string=} multiple - allows for more than one panel to be expanded at a time
 **/
function expansionPanelGroupDirective() {
  var directive = {
    restrict: 'E',
    controller: ['$scope', '$attrs', '$element', '$mdComponentRegistry', controller]
  };
  return directive;


  function controller($scope, $attrs, $element, $mdComponentRegistry) {
    /* jshint validthis: true */
    var vm = this;

    var deregister;
    var registered = {};
    var panels = {};
    var onChangeFuncs = [];
    var multipleExpand = $attrs.mdMultiple !== undefined || $attrs.multiple !== undefined;
    var autoExpand = $attrs.mdAutoExpand !== undefined || $attrs.autoExpand !== undefined;


    deregister = $mdComponentRegistry.register({
      $element: $element,
      register: register,
      getRegistered: getRegistered,
      getAll: getAll,
      getOpen: getOpen,
      remove: remove,
      removeAll: removeAll,
      collapseAll: collapseAll,
      onChange: onChange,
      count: panelCount
    }, $attrs.mdComponentId);

    vm.addPanel = addPanel;
    vm.expandPanel = expandPanel;
    vm.removePanel = removePanel;


    $scope.$on('$destroy', function () {
      if (typeof deregister === 'function') {
        deregister();
        deregister = undefined;
      }

      // destroy all panels
      // for some reason the child panels scopes are not getting destroyed
      Object.keys(panels).forEach(function (key) {
        panels[key].destroy();
      });
    });



    function onChange(callback) {
      onChangeFuncs.push(callback);

      return function () {
        onChangeFuncs.splice(onChangeFuncs.indexOf(callback), 1);
      };
    }

    function callOnChange() {
      var count = panelCount();
      onChangeFuncs.forEach(function (func) {
        func(count);
      });
    }


    function addPanel(componentId, panelCtrl) {
      panels[componentId] = panelCtrl;
      if (autoExpand === true) {
        panelCtrl.expand();
        closeOthers(componentId);
      }
      callOnChange();
    }

    function expandPanel(componentId) {
      closeOthers(componentId);
    }

    function remove(componentId, options) {
      return panels[componentId].remove(options);
    }

    function removeAll(options) {
      Object.keys(panels).forEach(function (panelId) {
        panels[panelId].remove(options);
      });
    }

    function removePanel(componentId) {
      delete panels[componentId];
      callOnChange();
    }

    function panelCount() {
      return Object.keys(panels).length;
    }

    function closeOthers(id) {
      if (multipleExpand === false) {
        Object.keys(panels).forEach(function (panelId) {
          if (panelId !== id) { panels[panelId].collapse(); }
        });
      }
    }


    function register(name, options) {
      if (registered[name] !== undefined) {
        throw Error('$mdExpansionPanelGroup.register() The name "' + name + '" has already been registered');
      }
      registered[name] = options;
    }


    function getRegistered(name) {
      if (registered[name] === undefined) {
        throw Error('$mdExpansionPanelGroup.addPanel() Cannot find Panel with name of "' + name + '"');
      }
      return registered[name];
    }


    function getAll() {
      return Object.keys(panels).map(function (panelId) {
        return panels[panelId];
      });
    }

    function getOpen() {
      return Object.keys(panels).map(function (panelId) {
        return panels[panelId];
      }).filter(function (instance) {
        return instance.isOpen();
      });
    }

    function collapseAll(noAnimation) {
      var animation = noAnimation === true ? false : true;
      Object.keys(panels).forEach(function (panelId) {
        panels[panelId].collapse({animation: animation});
      });
    }
  }
}
}());