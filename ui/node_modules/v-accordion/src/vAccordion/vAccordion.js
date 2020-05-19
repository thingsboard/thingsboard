
// Config
angular.module('vAccordion.config', [])
  .constant('accordionConfig', {
    states: {
      expanded: 'is-expanded'
    },
    expandAnimationDuration: 0.5
  })
  .animation('.is-expanded', [ '$animateCss', 'accordionConfig', function ($animateCss, accordionConfig) {
    return {
      addClass: function (element, className, done) {
        var paneContent = angular.element(element[0].querySelector('v-pane-content')),
            paneInner = angular.element(paneContent[0].querySelector('div'));

        var height = paneInner[0].offsetHeight;

        var expandAnimation = $animateCss(paneContent, {
          easing: 'ease',
          from: { maxHeight: '0px' },
          to: { maxHeight: height + 'px' },
          duration: accordionConfig.expandAnimationDuration
        });

        expandAnimation.start().done(function () {
          paneContent.css('max-height', 'none');
          done();
        });

        return function (isCancelled) {
          if (isCancelled) {
            paneContent.css('max-height', 'none');
          }
        };
      },
      removeClass: function (element, className, done) {
        var paneContent = angular.element(element[0].querySelector('v-pane-content')),
            paneInner = angular.element(paneContent[0].querySelector('div'));

        var height = paneInner[0].offsetHeight;

        var collapseAnimation = $animateCss(paneContent, {
          easing: 'ease',
          from: { maxHeight: height + 'px' },
          to: { maxHeight: '0px' },
          duration: accordionConfig.expandAnimationDuration
        });

        collapseAnimation.start().done(done);

        return function (isCancelled) {
          if (isCancelled) {
            paneContent.css('max-height', '0px');
          }
        };
      }
    };
  } ]);


// Modules
angular.module('vAccordion.directives', []);
angular.module('vAccordion',
  [
    'vAccordion.config',
    'vAccordion.directives'
  ]);
