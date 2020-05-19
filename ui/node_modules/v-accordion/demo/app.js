(function (angular) {
  'use strict';

  angular
    .module('myApp', [ 'ngAnimate', 'vAccordion' ])

    .controller('MainController', function ($scope) {

      $scope.panesA = [
        {
          id: 'pane-1a',
          header: 'Pane 1',
          content: 'Curabitur et ligula. Ut molestie a, ultricies porta urna. Vestibulum commodo volutpat a, convallis ac, laoreet enim. Phasellus fermentum in, dolor. Pellentesque facilisis. Nulla imperdiet sit amet magna. Vestibulum dapibus, mauris nec malesuada fames ac turpis velit, rhoncus eu, luctus et interdum adipiscing wisi.',
          isExpanded: true
        },
        {
          id: 'pane-2a',
          header: 'Pane 2',
          content: 'Lorem ipsum dolor sit amet enim. Etiam ullamcorper. Suspendisse a pellentesque dui, non felis. Maecenas malesuada elit lectus felis, malesuada ultricies.'
        },
        {
          id: 'pane-3a',
          header: 'Pane 3',
          content: 'Aliquam erat ac ipsum. Integer aliquam purus. Quisque lorem tortor fringilla sed, vestibulum id, eleifend justo vel bibendum sapien massa ac turpis faucibus orci luctus non.',

          subpanes: [
            {
              id: 'subpane-1a',
              header: 'Subpane 1',
              content: 'Quisque lorem tortor fringilla sed, vestibulum id, eleifend justo vel bibendum sapien massa ac turpis faucibus orci luctus non.'
            },
            {
              id: 'subpane-2a',
              header: 'Subpane 2 (disabled)',
              content: 'Curabitur et ligula. Ut molestie a, ultricies porta urna. Quisque lorem tortor fringilla sed, vestibulum id.',
              isDisabled: true
            }
          ]
        }
      ];

      $scope.panesB = [
        {
          id: 'pane-1b',
          header: 'Pane 1',
          content: 'Curabitur et ligula. Ut molestie a, ultricies porta urna. Vestibulum commodo volutpat a, convallis ac, laoreet enim. Phasellus fermentum in, dolor. Pellentesque facilisis. Nulla imperdiet sit amet magna. Vestibulum dapibus, mauris nec malesuada fames ac turpis velit, rhoncus eu, luctus et interdum adipiscing wisi.',
          isExpanded: true
        },
        {
          id: 'pane-2b',
          header: 'Pane 2',
          content: 'Lorem ipsum dolor sit amet enim. Etiam ullamcorper. Suspendisse a pellentesque dui, non felis. Maecenas malesuada elit lectus felis, malesuada ultricies.'
        },
        {
          id: 'pane-3b',
          header: 'Pane 3',
          content: 'Aliquam erat ac ipsum. Integer aliquam purus. Quisque lorem tortor fringilla sed, vestibulum id, eleifend justo vel bibendum sapien massa ac turpis faucibus orci luctus non.',

          subpanes: [
            {
              id: 'subpane-1b',
              header: 'Subpane 1',
              content: 'Quisque lorem tortor fringilla sed, vestibulum id, eleifend justo vel bibendum sapien massa ac turpis faucibus orci luctus non.'
            },
            {
              id: 'subpane-2b',
              header: 'Subpane 2 (disabled)',
              content: 'Curabitur et ligula. Ut molestie a, ultricies porta urna. Quisque lorem tortor fringilla sed, vestibulum id.',
              isDisabled: true
            }
          ]
        }
      ];

      $scope.expandCallback = function (index, id) {
        console.log('expand:', index, id);
      };

      $scope.collapseCallback = function (index, id) {
        console.log('collapse:', index, id);
      };

      $scope.$on('accordionA:onReady', function () {
        console.log('accordionA is ready!');
      });

    });

})(angular);
