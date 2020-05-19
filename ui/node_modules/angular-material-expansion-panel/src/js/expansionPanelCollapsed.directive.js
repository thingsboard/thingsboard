angular
  .module('material.components.expansionPanels')
  .directive('mdExpansionPanelCollapsed', expansionPanelCollapsedDirective);



/**
 * @ngdoc directive
 * @name mdExpansionPanelCollapsed
 * @module material.components.expansionPanels
 *
 * @restrict E
 *
 * @description
 * `mdExpansionPanelCollapsed` is used to contain content when the panel is collapsed
 **/
expansionPanelCollapsedDirective.$inject = ['$animateCss', '$timeout'];
function expansionPanelCollapsedDirective($animateCss, $timeout) {
  var directive = {
    restrict: 'E',
    require: '^^mdExpansionPanel',
    link: link
  };
  return directive;


  function link(scope, element, attrs, expansionPanelCtrl) {
    expansionPanelCtrl.registerCollapsed({
      show: show,
      hide: hide
    });


    element.on('click', function () {
      expansionPanelCtrl.expand();
    });


    function hide(options) {
      // set width to maintian demensions when element is set to postion: absolute
      element.css('width', element[0].offsetWidth + 'px');
      // set min height so the expansion panel does not shrink when collapsed element is set to position: absolute
      expansionPanelCtrl.$element.css('min-height', element[0].offsetHeight + 'px');

      var animationParams = {
        addClass: 'md-absolute md-hide',
        from: {opacity: 1},
        to: {opacity: 0}
      };
      if (options.animation === false) { animationParams.duration = 0; }
      $animateCss(element, animationParams)
      .start()
      .then(function () {
        element.removeClass('md-hide');
        element.css('display', 'none');
      });
    }


    function show(options) {
      element.css('display', '');
      // set width to maintian demensions when element is set to postion: absolute
      element.css('width', element[0].parentNode.offsetWidth + 'px');

      var animationParams = {
        addClass: 'md-show',
        from: {opacity: 0},
        to: {opacity: 1}
      };
      if (options.animation === false) { animationParams.duration = 0; }
      $animateCss(element, animationParams)
      .start()
      .then(function () {
        // safari will animate the min-height if transition is not set to 0
        expansionPanelCtrl.$element.css('transition', 'none');
        element.removeClass('md-absolute md-show');

        // remove width when element is no longer position: absolute
        element.css('width', '');


        // remove min height when element is no longer position: absolute
        expansionPanelCtrl.$element.css('min-height', '');
        // remove transition block on next digest
        $timeout(function () {
          expansionPanelCtrl.$element.css('transition', '');
        }, 0);
      });
    }
  }
}
