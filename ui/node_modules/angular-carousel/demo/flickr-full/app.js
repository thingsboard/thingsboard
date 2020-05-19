
document.ontouchmove = function(event) {
    // provent body move (ipad)
    var sourceElement = event.target || event.srcElement;
    if(!angular.element(sourceElement).hasClass('enable_touchmove')) {
      e.preventDefault();
    }
};

function partition(items, size) {
  var p = [];
  for (var i=Math.floor(items.length/size); i-->0; ) {
      p[i]=items.slice(i*size, (i+1)*size);
  }
  return p;
}

angular.module('myApp', ['angular-carousel', 'snap', 'truncate', 'angular-lazy'])
  .controller('demoController', ['$scope', '$http', '$timeout', function($scope, $http, $timeout) {

    var modalOpened = null;
    function simpleModal() {
      // modals helper
      this.visible = false;
      this.cls = '';
      this.open = function(force) {
        if (modalOpened) {
          modalOpened.close();
          if (!force) return;
        }
        this.visible = true;
        $scope.blurred = true;
        modalOpened = this;
        var me = this;
        $timeout(function() {
          me.cls = 'open';
        }, 100);
      };
      this.close = function() {
        this.cls = '';
        $scope.blurred = false;
        modalOpened = null;
        var me = this;
        $timeout(function() {
          me.visible = false;
        }, 300);
      };
    };

    $scope.aboutModal = new simpleModal();
    $scope.previewModal = new simpleModal();

    var page = 1,
        maxPages = 1,
        query = {},
        tmpPages = [];

    function fetch() {
      // fetch a single result page
      var params = {
        method: 'flickr.groups.pools.getPhotos',
        api_key: '98a83da50faeef3886249aef8ee3903e',
        per_page: 200,
        page: page,
        format: 'json',
        media: 'photos',
        extras: ['url_o']
      };
      angular.extend(params, query);
      $http.jsonp('http://api.flickr.com/services/rest/', {params: params});
    }

    window.jsonFlickrApi = function(result) {
      // flickr callback
      if (!result.photos) {
        $scope.pages = tmpPages;
        $scope.loading = false;
        return;
      }
      var newPics = [];
      angular.forEach(result.photos.photo, function(data) {
        newPics.push({
          image: "http://farm" + data.farm + ".staticflickr.com/" + data.server + "/" + data.id + "_" + data.secret,
          title: data.title,
          cls: ''
        });
      });

      // add to existing data
      $scope.pics = $scope.pics.concat(newPics);

      // distribute pages with 2, 3, 4, 5 items and choose tp accordingly
      var newPages = partition(newPics, 5);
      var tplPages = [];
      angular.forEach(newPages, function(page) {
        tplPages.push({
          images: page,
          tpl: 'page-' + 5 + '-' + parseInt(Math.floor(Math.random() * 4) + 1, 0)
        });
      });

      tmpPages = tmpPages.concat(tplPages);

      if (page < maxPages) {
        // enable display then fetch some more pages
        page += 1;
        fetch();
      } else {
        $scope.pages = tmpPages;
        $scope.loading = false;
      }
    };

    $scope.toggle = function(item) {
      if ($scope.previewModal.visible) {
        $scope.previewModal.close();
        return;
      }
      $scope.current = item;
      $scope.previewModal.open();
    }

    function load(args) {
      if ($scope.previewModal.visible) {
        $scope.previewModal.close();
      }
      if ($scope.aboutModal.visible) {
        $scope.aboutModal.close();
      }
      tmpPages = [];
      $scope.loading = true;
      query = args;
      page = 1;
      $scope.pics = [];
      $scope.pages = [];
      fetch();
    }

    $scope.loadGroup = function(grp) {
      load({
        group_id: grp
      });
    };

    // default search
    $scope.loadGroup('1580643@N23');

  }]);

