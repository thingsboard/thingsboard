angular
  .module('material.components.expansionPanels')
  .directive('mdExpansionPanelExpanded', expansionPanelExpandedDirective);



/**
 * @ngdoc directive
 * @name mdExpansionPanelExpanded
 * @module material.components.expansionPanels
 *
 * @restrict E
 *
 * @description
 * `mdExpansionPanelExpanded` is used to contain content when the panel is expanded
 *
 * @param {number=} height - add this aatribute set the max height of the expanded content. The container will be set to scroll
 **/
expansionPanelExpandedDirective.$inject = ['$animateCss', '$timeout'];
function expansionPanelExpandedDirective($animateCss, $timeout) {
  var directive = {
    restrict: 'E',
    require: '^^mdExpansionPanel',
    link: link
  };
  return directive;


  function link(scope, element, attrs, expansionPanelCtrl) {
    var setHeight = attrs.height || undefined;
    if (setHeight !== undefined) { setHeight = setHeight.replace('px', '') + 'px'; }

    expansionPanelCtrl.registerExpanded({
      show: show,
      hide: hide,
      setHeight: setHeight !== undefined,
      $element: element
    });




    function hide(options) {
      var height = setHeight ? setHeight : element[0].scrollHeight + 'px';
      element.addClass('md-hide md-overflow');
      element.removeClass('md-show md-scroll-y');

      var animationParams = {
        from: {'max-height': height, opacity: 1},
        to: {'max-height': '48px', opacity: 0}
      };
      if (options.animation === false) { animationParams.duration = 0; }
      $animateCss(element, animationParams)
      .start()
      .then(function () {
        element.css('display', 'none');
        element.removeClass('md-hide');
      });
    }


    function show(options) {
      element.css('display', '');
      element.addClass('md-show md-overflow');
      // use passed in height or the contents height
      var height = setHeight ? setHeight : element[0].scrollHeight + 'px';

      var animationParams = {
        from: {'max-height': '48px', opacity: 0},
        to: {'max-height': height, opacity: 1}
      };
      if (options.animation === false) { animationParams.duration = 0; }
      $animateCss(element, animationParams)
      .start()
      .then(function () {

        // if height was passed in then set div to scroll
        if (setHeight !== undefined) {
          element.addClass('md-scroll-y');
        } else {
          // safari will animate the max-height if transition is not set to 0
          element.css('transition', 'none');
          element.css('max-height', 'none');
          // remove transition block on next digest
          $timeout(function () {
            element.css('transition', '');
          }, 0);
        }

        element.removeClass('md-overflow');
      });
    }
  }
}
