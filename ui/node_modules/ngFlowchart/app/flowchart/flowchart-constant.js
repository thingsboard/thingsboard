(function() {

  'use strict';

  var constants = {
    htmlPrefix: 'fc',
    leftConnectorType: 'leftConnector',
    rightConnectorType: 'rightConnector',
    curvedStyle: 'curved',
    lineStyle: 'line',
    dragAnimationRepaint: 'repaint',
    dragAnimationShadow: 'shadow'
  };
  constants.canvasClass = constants.htmlPrefix + '-canvas';
  constants.selectedClass = constants.htmlPrefix + '-selected';
  constants.editClass = constants.htmlPrefix + '-edit';
  constants.activeClass = constants.htmlPrefix + '-active';
  constants.hoverClass = constants.htmlPrefix + '-hover';
  constants.draggingClass = constants.htmlPrefix + '-dragging';
  constants.edgeClass = constants.htmlPrefix + '-edge';
  constants.edgeLabelClass = constants.htmlPrefix + '-edge-label';
  constants.connectorClass = constants.htmlPrefix + '-connector';
  constants.magnetClass = constants.htmlPrefix + '-magnet';
  constants.nodeClass = constants.htmlPrefix + '-node';
  constants.nodeOverlayClass = constants.htmlPrefix + '-node-overlay';
  constants.leftConnectorClass = constants.htmlPrefix + '-' + constants.leftConnectorType + 's';
  constants.rightConnectorClass = constants.htmlPrefix + '-' + constants.rightConnectorType + 's';
  constants.canvasResizeThreshold = 200;
  constants.canvasResizeStep = 200;

  angular
    .module('flowchart')
    .constant('flowchartConstants', constants);

}());
