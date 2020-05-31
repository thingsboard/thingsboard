/*
 * Copyright © 2016-2020 The Thingsboard Authors
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
import './md-chip-draggable.scss';

var globalDraggingChipsWrapId = null;

export default angular.module('thingsboard.directives.mdChipDraggable', [])
    .directive('tbChipDraggable', function () {
    return {
      restrict: 'A',
      scope: {},
      bindToController: true,
      controllerAs: 'vm',
      controller: ['$document', '$scope', '$element', '$timeout',
        function ($document, $scope, $element, $timeout) {
          var handle = $element[0];
          var draggingClassName = 'dragging';
          var droppingClassName = 'dropping';
          var droppingBeforeClassName = 'dropping-before';
          var droppingAfterClassName = 'dropping-after';
          var dragging = false;
          var preventDrag = false;
          var dropPosition;
          var dropTimeout;
          var counter = 0;

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

              globalDraggingChipsWrapId = angular.element($element[0].closest('md-chips-wrap')).attr('id');

              $element.addClass(draggingClassName);

              var dataTransfer = event.dataTransfer || event.originalEvent.dataTransfer;

              dataTransfer.effectAllowed = 'copyMove';
              dataTransfer.dropEffect = 'move';

              dataTransfer.setData('text/plain', $scope.$parent.$mdChipsCtrl.items.indexOf($scope.$parent.$chip));
            }
          });

          $element.on('dragend', function () {
            dragging = false;
            globalDraggingChipsWrapId = null;

            $element.removeClass(draggingClassName);
          });

          var dragOverHandler = function (event) {
            if (dragging) {
              return;
            }

            var targetChipsWrapId = angular.element($element[0].closest('md-chips-wrap')).attr('id');

            event.preventDefault();

            if (globalDraggingChipsWrapId !== targetChipsWrapId) {
                return;
            }

            var bounds = $element[0].getBoundingClientRect();

            var props = {
              width: bounds.right - bounds.left,
              height: bounds.bottom - bounds.top,
              x: (event.originalEvent || event).clientX - bounds.left,
              y: (event.originalEvent || event).clientY - bounds.top,
            };

            var horizontalOffset = props.x;
            var horizontalMidPoint = props.width / 2;

            var verticalOffset = props.y;
            var verticalMidPoint = props.height / 2;

            $element.addClass(droppingClassName);

            $element.removeClass(droppingAfterClassName);
            $element.removeClass(droppingBeforeClassName);

            if (horizontalOffset >= horizontalMidPoint || verticalOffset >= verticalMidPoint) {
                dropPosition = 'after';
                $element.addClass(droppingAfterClassName);
            } else {
                dropPosition = 'before';
                $element.addClass(droppingBeforeClassName);
            }

          };

          var dropHandler = function (event) {
            counter = 0;
            event.preventDefault();

            var targetChipsWrapId = angular.element($element[0].closest('md-chips-wrap')).attr('id');
            if (globalDraggingChipsWrapId !== targetChipsWrapId) {
                return;
            }

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
              $element.removeClass(droppingAfterClassName);
              $element.removeClass(droppingBeforeClassName);

              $element.off('drop', dropHandler);
            }, 1000 / 16);
          };

          $element.on('dragenter', function () {
              counter++;
            if (dragging) {
              return;
            }

            $element.off('dragover', dragOverHandler);
            $element.off('drop', dropHandler);

            $element.on('dragover', dragOverHandler);
            $element.on('drop', dropHandler);
          });

          $element.on('dragleave', function () {
              counter--;
            if (counter <=0) {
                counter = 0;
                $element.removeClass(droppingClassName);
                $element.removeClass(droppingAfterClassName);
                $element.removeClass(droppingBeforeClassName);
            }
          });

        }],
    };
  })
  .name;

/* eslint-enable angular/angularelement */
