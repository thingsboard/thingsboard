(function() {

  'use strict';

  angular
    .module('flowchart')
    .provider('NodeTemplatePath', NodeTemplatePath);

  function NodeTemplatePath() {
    var templatePath = "flowchart/node.html";

    this.setTemplatePath = setTemplatePath;
    this.$get = NodeTemplatePath;

    function setTemplatePath(path) {
      templatePath = path;
    }

    function NodeTemplatePath() {
      return templatePath;
    }
  }

}());
