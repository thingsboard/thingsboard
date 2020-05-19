angular.module('angular-storage.internalStore', ['angular-storage.localStorage', 'angular-storage.sessionStorage'])
  .factory('InternalStore', function($log, $injector) {

    function InternalStore(namespace, storage, delimiter, useCache) {
      this.namespace = namespace || null;
      if (angular.isUndefined(useCache) || useCache == null) {
        useCache = true;
      }
      this.useCache = useCache;
      this.delimiter = delimiter || '.';
      this.inMemoryCache = {};
      this.storage = $injector.get(storage || 'localStorage');
    }

    InternalStore.prototype.getNamespacedKey = function(key) {
      if (!this.namespace) {
        return key;
      } else {
        return [this.namespace, key].join(this.delimiter);
      }
    };

    InternalStore.prototype.set = function(name, elem) {
      if (this.useCache) {
        this.inMemoryCache[name] = elem;
      }
      this.storage.set(this.getNamespacedKey(name), JSON.stringify(elem));
    };

    InternalStore.prototype.get = function(name) {
      var obj = null;
      if (this.useCache && name in this.inMemoryCache) {
        return this.inMemoryCache[name];
      }
      var saved = this.storage.get(this.getNamespacedKey(name));
      try {

        if (typeof saved === 'undefined' || saved === 'undefined') {
          obj = undefined;
        } else {
          obj = JSON.parse(saved);
        }

        if (this.useCache) {
          this.inMemoryCache[name] = obj;
        }
      } catch(e) {
        $log.error('Error parsing saved value', e);
        this.remove(name);
      }
      return obj;
    };

    InternalStore.prototype.remove = function(name) {
      if (this.useCache) {
        this.inMemoryCache[name] = null;
      }
      this.storage.remove(this.getNamespacedKey(name));
    };

    return InternalStore;
  });

