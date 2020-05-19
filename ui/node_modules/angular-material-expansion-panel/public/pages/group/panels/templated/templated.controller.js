(function(){"use strict";angular
  .module('angularMaterialExpansionPanel')
  .controller('TemplatedPanelController', TemplatedPanelController);



function TemplatedPanelController(title, summary, content) {
  var vm = this;

  vm.title = title;
  vm.summary = summary;
  vm.content = content;
}
}());