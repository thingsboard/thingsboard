(function (angular, window) {
    'use strict';

    var regModules = ['ng', 'oc.lazyLoad'],
        regInvokes = {},
        regConfigs = [],
        modulesToLoad = [],
        // modules to load from angular.module or other sources
    realModules = [],
        // real modules called from angular.module
    recordDeclarations = [],
        broadcast = angular.noop,
        runBlocks = {},
        justLoaded = [];

    var ocLazyLoad = angular.module('oc.lazyLoad', ['ng']);

    ocLazyLoad.provider('$ocLazyLoad', ["$controllerProvider", "$provide", "$compileProvider", "$filterProvider", "$injector", "$animateProvider", function ($controllerProvider, $provide, $compileProvider, $filterProvider, $injector, $animateProvider) {
        var modules = {},
            providers = {
            $controllerProvider: $controllerProvider,
            $compileProvider: $compileProvider,
            $filterProvider: $filterProvider,
            $provide: $provide, // other things (constant, decorator, provider, factory, service)
            $injector: $injector,
            $animateProvider: $animateProvider
        },
            debug = false,
            events = false,
            moduleCache = [],
            modulePromises = {};

        moduleCache.push = function (value) {
            if (this.indexOf(value) === -1) {
                Array.prototype.push.apply(this, arguments);
            }
        };

        this.config = function (config) {
            // If we want to define modules configs
            if (angular.isDefined(config.modules)) {
                if (angular.isArray(config.modules)) {
                    angular.forEach(config.modules, function (moduleConfig) {
                        modules[moduleConfig.name] = moduleConfig;
                    });
                } else {
                    modules[config.modules.name] = config.modules;
                }
            }

            if (angular.isDefined(config.debug)) {
                debug = config.debug;
            }

            if (angular.isDefined(config.events)) {
                events = config.events;
            }
        };

        /**
         * Get the list of existing registered modules
         * @param element
         */
        this._init = function _init(element) {
            // this is probably useless now because we override angular.bootstrap
            if (modulesToLoad.length === 0) {
                var elements = [element],
                    names = ['ng:app', 'ng-app', 'x-ng-app', 'data-ng-app'],
                    NG_APP_CLASS_REGEXP = /\sng[:\-]app(:\s*([\w\d_]+);?)?\s/,
                    append = function append(elm) {
                    return elm && elements.push(elm);
                };

                angular.forEach(names, function (name) {
                    names[name] = true;
                    append(document.getElementById(name));
                    name = name.replace(':', '\\:');
                    if (typeof element[0] !== 'undefined' && element[0].querySelectorAll) {
                        angular.forEach(element[0].querySelectorAll('.' + name), append);
                        angular.forEach(element[0].querySelectorAll('.' + name + '\\:'), append);
                        angular.forEach(element[0].querySelectorAll('[' + name + ']'), append);
                    }
                });

                angular.forEach(elements, function (elm) {
                    if (modulesToLoad.length === 0) {
                        var className = ' ' + element.className + ' ';
                        var match = NG_APP_CLASS_REGEXP.exec(className);
                        if (match) {
                            modulesToLoad.push((match[2] || '').replace(/\s+/g, ','));
                        } else {
                            angular.forEach(elm.attributes, function (attr) {
                                if (modulesToLoad.length === 0 && names[attr.name]) {
                                    modulesToLoad.push(attr.value);
                                }
                            });
                        }
                    }
                });
            }

            if (modulesToLoad.length === 0 && !((window.jasmine || window.mocha) && angular.isDefined(angular.mock))) {
                console.error('No module found during bootstrap, unable to init ocLazyLoad. You should always use the ng-app directive or angular.boostrap when you use ocLazyLoad.');
            }

            var addReg = function addReg(moduleName) {
                if (regModules.indexOf(moduleName) === -1) {
                    // register existing modules
                    regModules.push(moduleName);
                    var mainModule = angular.module(moduleName);

                    // register existing components (directives, services, ...)
                    _invokeQueue(null, mainModule._invokeQueue, moduleName);
                    _invokeQueue(null, mainModule._configBlocks, moduleName); // angular 1.3+

                    angular.forEach(mainModule.requires, addReg);
                }
            };

            angular.forEach(modulesToLoad, function (moduleName) {
                addReg(moduleName);
            });

            modulesToLoad = []; // reset for next bootstrap
            recordDeclarations.pop(); // wait for the next lazy load
        };

        /**
         * Like JSON.stringify but that doesn't throw on circular references
         * @param obj
         */
        var stringify = function stringify(obj) {
            try {
                return JSON.stringify(obj);
            } catch (e) {
                var cache = [];
                return JSON.stringify(obj, function (key, value) {
                    if (angular.isObject(value) && value !== null) {
                        if (cache.indexOf(value) !== -1) {
                            // Circular reference found, discard key
                            return;
                        }
                        // Store value in our collection
                        cache.push(value);
                    }
                    return value;
                });
            }
        };

        var hashCode = function hashCode(str) {
            var hash = 0,
                i,
                chr,
                len;
            if (str.length == 0) {
                return hash;
            }
            for (i = 0, len = str.length; i < len; i++) {
                chr = str.charCodeAt(i);
                hash = (hash << 5) - hash + chr;
                hash |= 0; // Convert to 32bit integer
            }
            return hash;
        };

        function _register(providers, registerModules, params) {
            if (registerModules) {
                var k,
                    moduleName,
                    moduleFn,
                    tempRunBlocks = [];
                for (k = registerModules.length - 1; k >= 0; k--) {
                    moduleName = registerModules[k];
                    if (!angular.isString(moduleName)) {
                        moduleName = getModuleName(moduleName);
                    }
                    if (!moduleName || justLoaded.indexOf(moduleName) !== -1 || modules[moduleName] && realModules.indexOf(moduleName) === -1) {
                        continue;
                    }
                    // new if not registered
                    var newModule = regModules.indexOf(moduleName) === -1;
                    moduleFn = ngModuleFct(moduleName);
                    if (newModule) {
                        regModules.push(moduleName);
                        _register(providers, moduleFn.requires, params);
                    }
                    if (moduleFn._runBlocks.length > 0) {
                        // new run blocks detected! Replace the old ones (if existing)
                        runBlocks[moduleName] = [];
                        while (moduleFn._runBlocks.length > 0) {
                            runBlocks[moduleName].push(moduleFn._runBlocks.shift());
                        }
                    }
                    if (angular.isDefined(runBlocks[moduleName]) && (newModule || params.rerun)) {
                        tempRunBlocks = tempRunBlocks.concat(runBlocks[moduleName]);
                    }
                    _invokeQueue(providers, moduleFn._invokeQueue, moduleName, params.reconfig);
                    _invokeQueue(providers, moduleFn._configBlocks, moduleName, params.reconfig); // angular 1.3+
                    broadcast(newModule ? 'ocLazyLoad.moduleLoaded' : 'ocLazyLoad.moduleReloaded', moduleName);
                    registerModules.pop();
                    justLoaded.push(moduleName);
                }
                // execute the run blocks at the end
                var instanceInjector = providers.getInstanceInjector();
                angular.forEach(tempRunBlocks, function (fn) {
                    instanceInjector.invoke(fn);
                });
            }
        }

        function _registerInvokeList(args, moduleName) {
            var invokeList = args[2][0],
                type = args[1],
                newInvoke = false;
            if (angular.isUndefined(regInvokes[moduleName])) {
                regInvokes[moduleName] = {};
            }
            if (angular.isUndefined(regInvokes[moduleName][type])) {
                regInvokes[moduleName][type] = {};
            }
            var onInvoke = function onInvoke(invokeName, invoke) {
                if (!regInvokes[moduleName][type].hasOwnProperty(invokeName)) {
                    regInvokes[moduleName][type][invokeName] = [];
                }
                if (checkHashes(invoke, regInvokes[moduleName][type][invokeName])) {
                    newInvoke = true;
                    regInvokes[moduleName][type][invokeName].push(invoke);
                    broadcast('ocLazyLoad.componentLoaded', [moduleName, type, invokeName]);
                }
            };

            function checkHashes(potentialNew, invokes) {
                var isNew = true,
                    newHash;
                if (invokes.length) {
                    newHash = signature(potentialNew);
                    angular.forEach(invokes, function (invoke) {
                        isNew = isNew && signature(invoke) !== newHash;
                    });
                }
                return isNew;
            }

            function signature(data) {
                if (angular.isArray(data)) {
                    // arrays are objects, we need to test for it first
                    return hashCode(data.toString());
                } else if (angular.isObject(data)) {
                    // constants & values for example
                    return hashCode(stringify(data));
                } else {
                    if (angular.isDefined(data) && data !== null) {
                        return hashCode(data.toString());
                    } else {
                        // null & undefined constants
                        return data;
                    }
                }
            }

            if (angular.isString(invokeList)) {
                onInvoke(invokeList, args[2][1]);
            } else if (angular.isObject(invokeList)) {
                angular.forEach(invokeList, function (invoke, key) {
                    if (angular.isString(invoke)) {
                        // decorators for example
                        onInvoke(invoke, invokeList[1]);
                    } else {
                        // components registered as object lists {"componentName": function() {}}
                        onInvoke(key, invoke);
                    }
                });
            } else {
                return false;
            }
            return newInvoke;
        }

        function _invokeQueue(providers, queue, moduleName, reconfig) {
            if (!queue) {
                return;
            }

            var i, len, args, provider;
            for (i = 0, len = queue.length; i < len; i++) {
                args = queue[i];
                if (angular.isArray(args)) {
                    if (providers !== null) {
                        if (providers.hasOwnProperty(args[0])) {
                            provider = providers[args[0]];
                        } else {
                            throw new Error('unsupported provider ' + args[0]);
                        }
                    }
                    var isNew = _registerInvokeList(args, moduleName);
                    if (args[1] !== 'invoke') {
                        if (isNew && angular.isDefined(provider)) {
                            provider[args[1]].apply(provider, args[2]);
                        }
                    } else {
                        // config block
                        var callInvoke = function callInvoke(fct) {
                            var invoked = regConfigs.indexOf(moduleName + '-' + fct);
                            if (invoked === -1 || reconfig) {
                                if (invoked === -1) {
                                    regConfigs.push(moduleName + '-' + fct);
                                }
                                if (angular.isDefined(provider)) {
                                    provider[args[1]].apply(provider, args[2]);
                                }
                            }
                        };
                        if (angular.isFunction(args[2][0])) {
                            callInvoke(args[2][0]);
                        } else if (angular.isArray(args[2][0])) {
                            for (var j = 0, jlen = args[2][0].length; j < jlen; j++) {
                                if (angular.isFunction(args[2][0][j])) {
                                    callInvoke(args[2][0][j]);
                                }
                            }
                        }
                    }
                }
            }
        }

        function getModuleName(module) {
            var moduleName = null;
            if (angular.isString(module)) {
                moduleName = module;
            } else if (angular.isObject(module) && module.hasOwnProperty('name') && angular.isString(module.name)) {
                moduleName = module.name;
            }
            return moduleName;
        }

        function moduleExists(moduleName) {
            if (!angular.isString(moduleName)) {
                return false;
            }
            try {
                return ngModuleFct(moduleName);
            } catch (e) {
                if (/No module/.test(e) || e.message.indexOf('$injector:nomod') > -1) {
                    return false;
                }
            }
        }

        this.$get = ["$log", "$rootElement", "$rootScope", "$cacheFactory", "$q", function ($log, $rootElement, $rootScope, $cacheFactory, $q) {
            var instanceInjector,
                filesCache = $cacheFactory('ocLazyLoad');

            if (!debug) {
                $log = {};
                $log['error'] = angular.noop;
                $log['warn'] = angular.noop;
                $log['info'] = angular.noop;
            }

            // Make this lazy because when $get() is called the instance injector hasn't been assigned to the rootElement yet
            providers.getInstanceInjector = function () {
                return instanceInjector ? instanceInjector : instanceInjector = $rootElement.data('$injector') || angular.injector();
            };

            broadcast = function broadcast(eventName, params) {
                if (events) {
                    $rootScope.$broadcast(eventName, params);
                }
                if (debug) {
                    $log.info(eventName, params);
                }
            };

            function reject(e) {
                var deferred = $q.defer();
                $log.error(e.message);
                deferred.reject(e);
                return deferred.promise;
            }

            return {
                _broadcast: broadcast,

                _$log: $log,

                /**
                 * Returns the files cache used by the loaders to store the files currently loading
                 * @returns {*}
                 */
                _getFilesCache: function getFilesCache() {
                    return filesCache;
                },

                /**
                 * Let the service know that it should monitor angular.module because files are loading
                 * @param watch boolean
                 */
                toggleWatch: function toggleWatch(watch) {
                    if (watch) {
                        recordDeclarations.push(true);
                    } else {
                        recordDeclarations.pop();
                    }
                },

                /**
                 * Let you get a module config object
                 * @param moduleName String the name of the module
                 * @returns {*}
                 */
                getModuleConfig: function getModuleConfig(moduleName) {
                    if (!angular.isString(moduleName)) {
                        throw new Error('You need to give the name of the module to get');
                    }
                    if (!modules[moduleName]) {
                        return null;
                    }
                    return angular.copy(modules[moduleName]);
                },

                /**
                 * Let you define a module config object
                 * @param moduleConfig Object the module config object
                 * @returns {*}
                 */
                setModuleConfig: function setModuleConfig(moduleConfig) {
                    if (!angular.isObject(moduleConfig)) {
                        throw new Error('You need to give the module config object to set');
                    }
                    modules[moduleConfig.name] = moduleConfig;
                    return moduleConfig;
                },

                /**
                 * Returns the list of loaded modules
                 * @returns {string[]}
                 */
                getModules: function getModules() {
                    return regModules;
                },

                /**
                 * Let you check if a module has been loaded into Angular or not
                 * @param modulesNames String/Object a module name, or a list of module names
                 * @returns {boolean}
                 */
                isLoaded: function isLoaded(modulesNames) {
                    var moduleLoaded = function moduleLoaded(module) {
                        var isLoaded = regModules.indexOf(module) > -1;
                        if (!isLoaded) {
                            isLoaded = !!moduleExists(module);
                        }
                        return isLoaded;
                    };
                    if (angular.isString(modulesNames)) {
                        modulesNames = [modulesNames];
                    }
                    if (angular.isArray(modulesNames)) {
                        var i, len;
                        for (i = 0, len = modulesNames.length; i < len; i++) {
                            if (!moduleLoaded(modulesNames[i])) {
                                return false;
                            }
                        }
                        return true;
                    } else {
                        throw new Error('You need to define the module(s) name(s)');
                    }
                },

                /**
                 * Given a module, return its name
                 * @param module
                 * @returns {String}
                 */
                _getModuleName: getModuleName,

                /**
                 * Returns a module if it exists
                 * @param moduleName
                 * @returns {module}
                 */
                _getModule: function getModule(moduleName) {
                    try {
                        return ngModuleFct(moduleName);
                    } catch (e) {
                        // this error message really suxx
                        if (/No module/.test(e) || e.message.indexOf('$injector:nomod') > -1) {
                            e.message = 'The module "' + stringify(moduleName) + '" that you are trying to load does not exist. ' + e.message;
                        }
                        throw e;
                    }
                },

                /**
                 * Check if a module exists and returns it if it does
                 * @param moduleName
                 * @returns {boolean}
                 */
                moduleExists: moduleExists,

                /**
                 * Load the dependencies, and might try to load new files depending on the config
                 * @param moduleName (String or Array of Strings)
                 * @param localParams
                 * @returns {*}
                 * @private
                 */
                _loadDependencies: function _loadDependencies(moduleName, localParams) {
                    var loadedModule,
                        requires,
                        diff,
                        promisesList = [],
                        self = this;

                    moduleName = self._getModuleName(moduleName);

                    if (moduleName === null) {
                        return $q.when();
                    } else {
                        try {
                            loadedModule = self._getModule(moduleName);
                        } catch (e) {
                            return reject(e);
                        }
                        // get unloaded requires
                        requires = self.getRequires(loadedModule);
                    }

                    angular.forEach(requires, function (requireEntry) {
                        // If no configuration is provided, try and find one from a previous load.
                        // If there isn't one, bail and let the normal flow run
                        if (angular.isString(requireEntry)) {
                            var config = self.getModuleConfig(requireEntry);
                            if (config === null) {
                                moduleCache.push(requireEntry); // We don't know about this module, but something else might, so push it anyway.
                                return;
                            }
                            requireEntry = config;
                            // ignore the name because it's probably not a real module name
                            config.name = undefined;
                        }

                        // Check if this dependency has been loaded previously
                        if (self.moduleExists(requireEntry.name)) {
                            // compare against the already loaded module to see if the new definition adds any new files
                            diff = requireEntry.files.filter(function (n) {
                                return self.getModuleConfig(requireEntry.name).files.indexOf(n) < 0;
                            });

                            // If the module was redefined, advise via the console
                            if (diff.length !== 0) {
                                self._$log.warn('Module "', moduleName, '" attempted to redefine configuration for dependency. "', requireEntry.name, '"\n Additional Files Loaded:', diff);
                            }

                            // Push everything to the file loader, it will weed out the duplicates.
                            if (angular.isDefined(self.filesLoader)) {
                                // if a files loader is defined
                                promisesList.push(self.filesLoader(requireEntry, localParams).then(function () {
                                    return self._loadDependencies(requireEntry);
                                }));
                            } else {
                                return reject(new Error('Error: New dependencies need to be loaded from external files (' + requireEntry.files + '), but no loader has been defined.'));
                            }
                            return;
                        } else if (angular.isArray(requireEntry)) {
                            var files = [];
                            angular.forEach(requireEntry, function (entry) {
                                // let's check if the entry is a file name or a config name
                                var config = self.getModuleConfig(entry);
                                if (config === null) {
                                    files.push(entry);
                                } else if (config.files) {
                                    files = files.concat(config.files);
                                }
                            });
                            if (files.length > 0) {
                                requireEntry = {
                                    files: files
                                };
                            }
                        } else if (angular.isObject(requireEntry)) {
                            if (requireEntry.hasOwnProperty('name') && requireEntry['name']) {
                                // The dependency doesn't exist in the module cache and is a new configuration, so store and push it.
                                self.setModuleConfig(requireEntry);
                                moduleCache.push(requireEntry['name']);
                            }
                        }

                        // Check if the dependency has any files that need to be loaded. If there are, push a new promise to the promise list.
                        if (angular.isDefined(requireEntry.files) && requireEntry.files.length !== 0) {
                            if (angular.isDefined(self.filesLoader)) {
                                // if a files loader is defined
                                promisesList.push(self.filesLoader(requireEntry, localParams).then(function () {
                                    return self._loadDependencies(requireEntry);
                                }));
                            } else {
                                return reject(new Error('Error: the module "' + requireEntry.name + '" is defined in external files (' + requireEntry.files + '), but no loader has been defined.'));
                            }
                        }
                    });

                    // Create a wrapper promise to watch the promise list and resolve it once everything is done.
                    return $q.all(promisesList);
                },

                /**
                 * Inject new modules into Angular
                 * @param moduleName
                 * @param localParams
                 * @param real
                 */
                inject: function inject(moduleName) {
                    var localParams = arguments.length <= 1 || arguments[1] === undefined ? {} : arguments[1];
                    var real = arguments.length <= 2 || arguments[2] === undefined ? false : arguments[2];

                    var self = this,
                        deferred = $q.defer();
                    if (angular.isDefined(moduleName) && moduleName !== null) {
                        if (angular.isArray(moduleName)) {
                            var promisesList = [];
                            angular.forEach(moduleName, function (module) {
                                promisesList.push(self.inject(module, localParams, real));
                            });
                            return $q.all(promisesList);
                        } else {
                            self._addToLoadList(self._getModuleName(moduleName), true, real);
                        }
                    }
                    if (modulesToLoad.length > 0) {
                        var res = modulesToLoad.slice(); // clean copy
                        var loadNext = function loadNext(moduleName) {
                            moduleCache.push(moduleName);
                            modulePromises[moduleName] = deferred.promise;
                            self._loadDependencies(moduleName, localParams).then(function success() {
                                try {
                                    justLoaded = [];
                                    _register(providers, moduleCache, localParams);
                                } catch (e) {
                                    self._$log.error(e.message);
                                    deferred.reject(e);
                                    return;
                                }

                                if (modulesToLoad.length > 0) {
                                    loadNext(modulesToLoad.shift()); // load the next in list
                                } else {
                                        deferred.resolve(res); // everything has been loaded, resolve
                                    }
                            }, function error(err) {
                                deferred.reject(err);
                            });
                        };

                        // load the first in list
                        loadNext(modulesToLoad.shift());
                    } else if (localParams && localParams.name && modulePromises[localParams.name]) {
                        return modulePromises[localParams.name];
                    } else {
                        deferred.resolve();
                    }
                    return deferred.promise;
                },

                /**
                 * Get the list of required modules/services/... for this module
                 * @param module
                 * @returns {Array}
                 */
                getRequires: function getRequires(module) {
                    var requires = [];
                    angular.forEach(module.requires, function (requireModule) {
                        if (regModules.indexOf(requireModule) === -1) {
                            requires.push(requireModule);
                        }
                    });
                    return requires;
                },

                /**
                 * Invoke the new modules & component by their providers
                 * @param providers
                 * @param queue
                 * @param moduleName
                 * @param reconfig
                 * @private
                 */
                _invokeQueue: _invokeQueue,

                /**
                 * Check if a module has been invoked and registers it if not
                 * @param args
                 * @param moduleName
                 * @returns {boolean} is new
                 */
                _registerInvokeList: _registerInvokeList,

                /**
                 * Register a new module and loads it, executing the run/config blocks if needed
                 * @param providers
                 * @param registerModules
                 * @param params
                 * @private
                 */
                _register: _register,

                /**
                 * Add a module name to the list of modules that will be loaded in the next inject
                 * @param name
                 * @param force
                 * @private
                 */
                _addToLoadList: _addToLoadList,

                /**
                 * Unregister modules (you shouldn't have to use this)
                 * @param modules
                 */
                _unregister: function _unregister(modules) {
                    if (angular.isDefined(modules)) {
                        if (angular.isArray(modules)) {
                            angular.forEach(modules, function (module) {
                                regInvokes[module] = undefined;
                            });
                        }
                    }
                }
            };
        }];

        // Let's get the list of loaded modules & components
        this._init(angular.element(window.document));
    }]);

    var bootstrapFct = angular.bootstrap;
    angular.bootstrap = function (element, modules, config) {
        // Clean state from previous bootstrap
        regModules = ['ng', 'oc.lazyLoad'];
        regInvokes = {};
        regConfigs = [];
        modulesToLoad = [];
        realModules = [];
        recordDeclarations = [];
        broadcast = angular.noop;
        runBlocks = {};
        justLoaded = [];
        // we use slice to make a clean copy
        angular.forEach(modules.slice(), function (module) {
            _addToLoadList(module, true, true);
        });
        return bootstrapFct(element, modules, config);
    };

    var _addToLoadList = function _addToLoadList(name, force, real) {
        if ((recordDeclarations.length > 0 || force) && angular.isString(name) && modulesToLoad.indexOf(name) === -1) {
            modulesToLoad.push(name);
            if (real) {
                realModules.push(name);
            }
        }
    };

    var ngModuleFct = angular.module;
    angular.module = function (name, requires, configFn) {
        _addToLoadList(name, false, true);
        return ngModuleFct(name, requires, configFn);
    };

    // CommonJS package manager support:
    if (typeof module !== 'undefined' && typeof exports !== 'undefined' && module.exports === exports) {
        module.exports = 'oc.lazyLoad';
    }
})(angular, window);