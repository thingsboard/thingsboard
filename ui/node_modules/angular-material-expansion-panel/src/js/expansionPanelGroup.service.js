angular
  .module('material.components.expansionPanels')
  .factory('$mdExpansionPanelGroup', expansionPanelGroupService);


/**
 * @ngdoc service
 * @name $mdExpansionPanelGroup
 * @module material.components.expansionPanels
 *
 * @description
 * Expand and collapse Expansion Panel using its `md-component-id`
 *
 * @example
 * $mdExpansionPanelGroup('comonentId').then(function (instance) {
 *  instance.register({
 *    componentId: 'cardComponentId',
 *    templateUrl: 'template.html',
 *    controller: 'Controller'
 *  });
 *  instance.add('cardComponentId', {local: localData});
 *  instance.remove('cardComponentId', {animation: false});
 *  instance.removeAll({animation: false});
 * });
 */
expansionPanelGroupService.$inject = ['$mdComponentRegistry', '$mdUtil', '$mdExpansionPanel', '$templateRequest', '$rootScope', '$compile', '$controller', '$q', '$log'];
function expansionPanelGroupService($mdComponentRegistry, $mdUtil, $mdExpansionPanel, $templateRequest, $rootScope, $compile, $controller, $q, $log) {
  var errorMsg = "ExpansionPanelGroup '{0}' is not available! Did you use md-component-id='{0}'?";
  var service = {
    find: findInstance,
    waitFor: waitForInstance
  };

  return function (handle) {
    if (handle === undefined) { return service; }
    return findInstance(handle);
  };



  function findInstance(handle) {
    var instance = $mdComponentRegistry.get(handle);

    if (!instance) {
      // Report missing instance
      $log.error( $mdUtil.supplant(errorMsg, [handle || ""]) );
      return undefined;
    }

    return createGroupInstance(instance);
  }

  function waitForInstance(handle) {
    var deffered = $q.defer();

    $mdComponentRegistry.when(handle).then(function (instance) {
      deffered.resolve(createGroupInstance(instance));
    }).catch(function (error) {
      deffered.reject();
      $log.error(error);
    });

    return deffered.promise;
  }





  // --- returned service for group instance ---

  function createGroupInstance(instance) {
    var service = {
      add: add,
      register: register,
      getAll: getAll,
      getOpen: getOpen,
      remove: remove,
      removeAll: removeAll,
      collapseAll: collapseAll,
      onChange: onChange,
      count: count
    };

    return service;


    function register(name, options) {
      if (typeof name !== 'string') {
        throw Error('$mdExpansionPanelGroup.register() Expects name to be a string');
      }

      validateOptions(options);
      instance.register(name, options);
    }

    function remove(componentId, options) {
      return instance.remove(componentId, options);
    }

    function removeAll(options) {
      instance.removeAll(options);
    }

    function onChange(callback) {
      return instance.onChange(callback);
    }

    function count() {
      return instance.count();
    }

    function getAll() {
      return instance.getAll();
    }

    function getOpen() {
      return instance.getOpen();
    }

    function collapseAll(noAnimation) {
      instance.collapseAll(noAnimation);
    }


    function add(options, locals) {
      locals = locals || {};
      // assume if options is a string then they are calling a registered card by its component id
      if (typeof options === 'string') {
        // call add panel with the stored options
        return add(instance.getRegistered(options), locals);
      }

      validateOptions(options);
      if (options.componentId && instance.isPanelActive(options.componentId)) {
        return $q.reject('panel with componentId "' + options.componentId + '" is currently active');
      }


      var deffered = $q.defer();
      var scope = $rootScope.$new();
      angular.extend(scope, options.scope);

      getTemplate(options, function (template) {
        var element = angular.element(template);
        var componentId = options.componentId || element.attr('md-component-id') || '_panelComponentId_' + $mdUtil.nextUid();
        var panelPromise = $mdExpansionPanel().waitFor(componentId);
        element.attr('md-component-id', componentId);

        var linkFunc = $compile(element);
        if (options.controller) {
          angular.extend(locals, options.locals || {});
          locals.$scope = scope;
          locals.$panel = panelPromise;
          var invokeCtrl = $controller(options.controller, locals, true);
          var ctrl = invokeCtrl();
          element.data('$ngControllerController', ctrl);
          element.children().data('$ngControllerController', ctrl);
          if (options.controllerAs) {
            scope[options.controllerAs] = ctrl;
          }
        }

        // link after the element is added so we can find card manager directive
        instance.$element.append(element);
        linkFunc(scope);

        panelPromise.then(function (instance) {
          deffered.resolve(instance);
        });
      });

      return deffered.promise;
    }


    function validateOptions(options) {
      if (typeof options !== 'object' || options === null) {
        throw Error('$mdExapnsionPanelGroup.add()/.register() : Requires an options object to be passed in');
      }

      // if none of these exist then a dialog box cannot be created
      if (!options.template && !options.templateUrl) {
        throw Error('$mdExapnsionPanelGroup.add()/.register() : Is missing required paramters to create. Required One of the following: template, templateUrl');
      }
    }



    function getTemplate(options, callback) {
      var template;

      if (options.templateUrl !== undefined) {
        $templateRequest(options.templateUrl)
          .then(function(response) {
            callback(response);
          });
      } else {
        callback(options.template);
      }
    }
  }
}
