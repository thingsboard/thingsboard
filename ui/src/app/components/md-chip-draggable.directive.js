/*
 * Copyright © 2016-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
export default angular.module('thingsboard.directives.mdChipDraggable', [])
    .directive('tbChipDraggable', function () {
    return {
      restrict: 'A',
      scope: {},
      bindToController: true,
      controllerAs: 'vm',
      controller: ['$document', '$scope', '$element', '$timeout',
        function ($document, $scope, $element, $timeout) {
          var options = {
            axis: 'horizontal',
          };
          var handle = $element[0];
          var draggingClassName = 'dragging';
          var droppingClassName = 'dropping';
          var droppingBeforeClassName = 'dropping--before';
          var droppingAfterClassName = 'dropping--after';
          var dragging = false;
          var preventDrag = false;
          var dropPosition;
          var dropTimeout;

          var move = function (from, to) {
            this.splice(to, 0, this.splice(from, 1)[0]);
          };

          $element = angular.element($element[0].closest('md-chip'));

          $element.attr('draggable', true);

          $element.on('mousedown', function (event) {
            if (event.target !== handle) {
              preventDrag = true;
            }
          });

          $document.on('mouseup', function () {
            preventDrag = false;
          });

          $element.on('dragstart', function (event) {
            if (preventDrag) {
              event.preventDefault();

            } else {
              dragging = true;

              $element.addClass(draggingClassName);

              var dataTransfer = event.dataTransfer || event.originalEvent.dataTransfer;

              dataTransfer.effectAllowed = 'copyMove';
              dataTransfer.dropEffect = 'move';
              dataTransfer.setData('text/plain', $scope.$parent.$mdChipsCtrl.items.indexOf($scope.$parent.$chip));
            }
          });

          $element.on('dragend', function () {
            dragging = false;

            $element.removeClass(draggingClassName);
          });

          var dragOverHandler = function (event) {
            if (dragging) {
              return;
            }

            event.preventDefault();
            
            var bounds = $element[0].getBoundingClientRect();

            var props = {
              width: bounds.right - bounds.left,
              height: bounds.bottom - bounds.top,
              x: (event.originalEvent || event).clientX - bounds.left,
              y: (event.originalEvent || event).clientY - bounds.top,
            };

            var offset = options.axis === 'vertical' ? props.y : props.x;
            var midPoint = (options.axis === 'vertical' ? props.height : props.width) / 2;

            $element.addClass(droppingClassName);
            
            if (offset < midPoint) {
              dropPosition = 'before';
              $element.removeClass(droppingAfterClassName);
              $element.addClass(droppingBeforeClassName);

            } else {
              dropPosition = 'after';
              $element.removeClass(droppingBeforeClassName);
              $element.addClass(droppingAfterClassName);
            }
          };

          var dropHandler = function (event) {
            event.preventDefault();

            var droppedItemIndex = parseInt((event.dataTransfer || event.originalEvent.dataTransfer).getData('text/plain'), 10);
            var currentIndex = $scope.$parent.$mdChipsCtrl.items.indexOf($scope.$parent.$chip);
            var newIndex = null;

            if (dropPosition === 'before') {
              if (droppedItemIndex < currentIndex) {
                newIndex = currentIndex - 1;
              } else {
                newIndex = currentIndex;
              }
            } else {
              if (droppedItemIndex < currentIndex) {
                newIndex = currentIndex;
              } else {
                newIndex = currentIndex + 1;
              }
            }

            // prevent event firing multiple times in firefox
            $timeout.cancel(dropTimeout);
            dropTimeout = $timeout(function () {
              dropPosition = null;

              move.apply($scope.$parent.$mdChipsCtrl.items, [droppedItemIndex, newIndex]);

              $scope.$apply(function () {
                $scope.$emit('mdChipDraggable:change', {
                  collection: $scope.$parent.$mdChipsCtrl.items,
                  item: $scope.$parent.$mdChipsCtrl.items[droppedItemIndex],
                  from: droppedItemIndex,
                  to: newIndex,
                });
              });

              $element.removeClass(droppingClassName);
              $element.removeClass(droppingBeforeClassName);
              $element.removeClass(droppingAfterClassName);

              $element.off('drop', dropHandler);
            }, 1000 / 16);
          };

          $element.on('dragenter', function () {
            if (dragging) {
              return;
            }

            $element.off('dragover', dragOverHandler);
            $element.off('drop', dropHandler);

            $element.on('dragover', dragOverHandler);
            $element.on('drop', dropHandler);
          });

          $element.on('dragleave', function () {
            $element.removeClass(droppingClassName);
            $element.removeClass(droppingBeforeClassName);
            $element.removeClass(droppingAfterClassName);
          });

        }],
    };
  })
  .name;

/* eslint-enable angular/angularelement */
