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
import './alarm-comments-dialog.scss';
/*@ngInject*/
export default function AlarmCommentsDialogController($mdDialog, $filter, $scope, $mdMedia, $log, alarmService, userService, alarm) {

    var vm = this;
    vm.alarm = alarm;
    vm.comments = alarm.details.comments;
    vm.entitiesCount = vm.comments ? vm.comments.length : 0;
    vm.defaultPageSize = 5;

    vm.query = {
        order: '-ts',
        limit: vm.defaultPageSize,
        page: 1,
        search: null
    };

    vm.columns = [{'key':'ts', 'text': 'alarm.created-time'}, {'key':'user.name', 'text':'alarm.comment.author'}, {'key':'note', 'text':'alarm.comment.comment'}];

    updateEntities();

    vm.addComment = addComment;
    vm.cancel = cancel;
    vm.onReorder = onReorder;
    vm.onPaginate = onPaginate;

    function addComment($event) {
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.newCommentText) {

            if (angular.isUndefined(alarm.details)) {
                alarm.details = {}
            }
            var details = alarm.details;
            if (angular.isUndefined(details.comments)) {
                details.comments = [];
            }
            var comments = details.comments;
            var currentUser = userService.getCurrentUser();

            var newComment = {
                user : {
                    name: currentUser.firstName + " " + currentUser.lastName,
                    id: currentUser.userId
                },
                ts: new Date().getTime(),
                comment: vm.newCommentText
            }
            comments.push(newComment)
            alarmService.saveAlarm(vm.alarm, true).then(function success() {
                $mdDialog.hide();
            },function error(e) {
                $log.error(e);
                $mdDialog.cancel();
            });
        }
    }

    function cancel () {
        $mdDialog.cancel();
    }

    function onReorder() {
        updateEntities();
    }

    function onPaginate() {
        updateEntities();
    }

    function updateEntities() {
        if (vm.comments) {
            var result = $filter('orderBy')(vm.comments, vm.query.order);

            var startIndex = vm.query.limit * (vm.query.page - 1);
            vm.entities = result.slice(startIndex, startIndex + vm.query.limit);
        }
    }

    $scope.$watch(function () {
        return $mdMedia('gt-xs');
    }, function (isGtXs) {
        vm.isGtXs = isGtXs;
    });

    $scope.$watch(function () {
        return $mdMedia('gt-md');
    }, function (isGtMd) {
        vm.isGtMd = isGtMd;
        if (vm.isGtMd) {
            vm.limitOptions = [vm.defaultPageSize, vm.defaultPageSize * 2, vm.defaultPageSize * 3];
        } else {
            //vm.limitOptions = null;
        }
    });
}