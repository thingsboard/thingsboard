/*
 * Copyright © 2016-2017 The Thingsboard Authors
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
import './import-dialog.scss';

/*@ngInject*/
export default function ImportDialogController($scope, $mdDialog, toast, importTitle, importFileLabel) {

    var vm = this;

    vm.cancel = cancel;
    vm.importFromJson = importFromJson;
    vm.fileAdded = fileAdded;
    vm.clearFile = clearFile;

    vm.importTitle = importTitle;
    vm.importFileLabel = importFileLabel;


    function cancel() {
        $mdDialog.cancel();
    }

    function fileAdded($file) {
        if ($file.getExtension() === 'json') {
            var reader = new FileReader();
            reader.onload = function(event) {
                $scope.$apply(function() {
                    if (event.target.result) {
                        $scope.theForm.$setDirty();
                        var importJson = event.target.result;
                        if (importJson && importJson.length > 0) {
                            try {
                                vm.importData = angular.fromJson(importJson);
                                vm.fileName = $file.name;
                            } catch (err) {
                                vm.fileName = null;
                                toast.showError(err.message);
                            }
                        }
                    }
                });
            };
            reader.readAsText($file.file);
        }
    }

    function clearFile() {
        $scope.theForm.$setDirty();
        vm.fileName = null;
        vm.importData = null;
    }

    function importFromJson() {
        $scope.theForm.$setPristine();
        $mdDialog.hide(vm.importData);
    }
}
