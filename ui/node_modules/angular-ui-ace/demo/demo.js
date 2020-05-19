'use strict';

angular.module('doc.ui-ace', ['ui.ace', 'prettifyDirective', 'ui.bootstrap', 'plunker'])
  .controller('AceCtrl', ['$scope', function ($scope) {

    // The modes
    $scope.modes = ['Scheme', 'XML', 'Javascript'];
    $scope.mode = $scope.modes[0];


    // The ui-ace option
    $scope.aceOption = {
      mode: $scope.mode.toLowerCase(),
      onLoad: function (_ace) {

        // HACK to have the ace instance in the scope...
        $scope.modeChanged = function () {
          _ace.getSession().setMode('ace/mode/' + $scope.mode.toLowerCase());
        };

      }
    };

    // Initial code content...
    $scope.aceModel = ';; Scheme code in here.\n' +
      '(define (double x)\n\t(* x x))\n\n\n' +
      '<!-- XML code in here. -->\n' +
      '<root>\n\t<foo>\n\t</foo>\n\t<bar/>\n</root>\n\n\n' +
      '// Javascript code in here.\n' +
      'function foo(msg) {\n\tvar r = Math.random();\n\treturn "" + r + " : " + msg;\n}';

  }])
;
