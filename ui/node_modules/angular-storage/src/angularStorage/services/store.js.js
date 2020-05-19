angular.module('angular-storage.store', ['angular-storage.internalStore'])
  .provider('store', function() {

    // the default storage
    var _storage = 'localStorage';

    //caching is on by default
    var _caching = true;

    /**
     * Sets the storage.
     *
     * @param {String} storage The storage name
     */
    this.setStore = function(storage) {
      if (storage && angular.isString(storage)) {
        _storage = storage;
      }
    };

    /**
     * Sets the internal cache usage
     *
     * @param {boolean} useCache Whether to use internal cache
     */
    this.setCaching = function(useCache) {
      _caching = !!useCache;
    };

    this.$get = function(InternalStore) {
      var store = new InternalStore(null, _storage, null, _caching);

      /**
       * Returns a namespaced store
       *
       * @param {String} namespace The namespace
       * @param {String} storage The name of the storage service
       * @param {String} delimiter The key delimiter
       * @param {boolean} useCache whether to use the internal caching
       * @returns {InternalStore}
       */
      store.getNamespacedStore = function(namespace, storage, delimiter, useCache) {
        return new InternalStore(namespace, storage, delimiter, useCache);
      };

      return store;
    };
  });

