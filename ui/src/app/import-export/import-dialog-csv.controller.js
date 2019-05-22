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
import './import-dialog.scss';

/*@ngInject*/
export default function ImportDialogCsvController($scope, $mdDialog, toast, importTitle, importFileLabel, importExport, types, $timeout) {

    var vm = this;

    vm.cancel = cancel;
    vm.importFromJson = importFromJson;
    vm.fileAdded = fileAdded;
    vm.clearFile = clearFile;

    vm.addDevices = addDevices;
    vm.importParams = {
        isUpdate: true,
        isHeader: true
    };

    vm.importTitle = importTitle;
    vm.importFileLabel = importFileLabel;

    vm.columnsParam = [];
    vm.parseData = [];

    vm.entityType = types.entityType.device;
    vm.columnTypes = {};
    vm.entityField = {};

    var parseData = {};

    switch (vm.entityType) {
        case types.entityType.device:
            vm.columnTypes = types.entityGroup.columnType;
            break;
    }

    vm.entityField.name = types.entityGroup.entityField.name;

    switch (vm.entityType) {
        case types.entityType.device:
            vm.entityField.type = types.entityGroup.entityField.type;
            // vm.entityField.assigned_customer = types.entityGroup.entityField.assigned_customer;
            break;
    }

    $scope.$watch('vm.columnsParam', function(newVal, prevVal){
        if (newVal && !angular.equals(newVal, prevVal)) {
            var isSelectName = false;
            var isSelectType = false;
            for (var i = 0; i < newVal.length; i++) {
                if (newVal[i].type === types.entityGroup.columnType.entityField.value &&
                    newVal[i].key === types.entityGroup.entityField.name.value) {
                    isSelectName = true;
                }
                if (newVal[i].type === types.entityGroup.columnType.entityField.value &&
                    newVal[i].key === types.entityGroup.entityField.type.value) {
                    isSelectType = true;
                }
            }
            $timeout(function () {
                vm.entityField.name.disable = isSelectName;
                vm.entityField.type.disable = isSelectType;
            });
        }
    }, true);


    function cancel() {
        $mdDialog.cancel();
    }

    function fileAdded($file) {
        if ($file.getExtension() === 'csv') {
            var reader = new FileReader();
            reader.onload = function (event) {
                $scope.$apply(function () {
                    if (event.target.result) {
                        $scope.theForm.$setDirty();
                        var importCSV = event.target.result;
                        if (importCSV && importCSV.length > 0) {
                            try {
                                vm.importData = importCSV;
                                vm.fileName = $file.name;
                                parseCSVData(vm.importData);
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

    function parseCSVData(importData) {
        var columnParam = {};
        var config = {
            delim: vm.importParams.delim,
            header: vm.importParams.isHeader
        };
        parseData = importExport.convertCSVToJson(importData, config);
        for (var i = 0; i < parseData.headers.length; i++) {
            if (vm.importParams.isHeader && parseData.headers[i].search(/^(name|type)$/im) === 0) {
                columnParam = {
                    type: types.entityGroup.columnType.entityField.value,
                    key: parseData.headers[i].toLowerCase(),
                    sampleData: parseData.rows[0][i]
                };
            } else {
                columnParam = {
                    type: types.entityGroup.columnType.serverAttribute.value,
                    key: vm.importParams.isHeader ? parseData.headers[i] : "",
                    sampleData: parseData.rows[0][i]
                };
            }
            vm.columnsParam.push(columnParam);
        }
    }

    function addDevices () {
        var arrayParam = [{type: "ENTITY_FIELD", key: "name", sampleData: "Device 1"}, {type: "ENTITY_FIELD", key: "type", sampleData: "test"}, {type: "SERVER_ATTRIBUTE", key: "test", sampleData: "test"}, {type: "TIMESERIES", key: "testBoolean", sampleData: false}, {type: "SHARED_ATTRIBUTE", key: "testNumber", sampleData: 123}]; // eslint-disable-line
        var data =  {headers: ["Device 1", "test", "test", "FALSE", "123"],
        rows:[["Device 1", "test", "test", false, 123.5]]};
        // rows:[["Device 1", "test", "test", false, 123],
        //         ["Device 2", "test", "test", false, 124],
        //         ["Device 3", "test", "test", false, 125],
        //         ["Device 4", "test", "test", false, 126],
        //         ["Device 5", "test", "test", false, 127]]};
        arrayParam = vm.columnsParam;
        data = parseData;
        var arrayData = [];
        var config = {
            ignoreErrors: true,
            resendRequest: true
        };
        for (var i = 0; i < data.rows.length; i ++) {
            var obj = {
                name: "",
                type: "",
                attributes: {
                    server: [],
                    shared: []
                },
                timeseries: []
            };
            for(var j = 0; j < arrayParam.length; j++){
                switch (arrayParam[j].type) {
                    case types.entityGroup.columnType.serverAttribute.value:
                        obj.attributes.server.push({
                            key: arrayParam[j].key,
                            value: data.rows[i][j]
                        });
                        break;
                    case types.entityGroup.columnType.sharedAttribute.value:
                        obj.attributes.shared.push({
                            key: arrayParam[j].key,
                            value: data.rows[i][j]
                        });
                        break;
                    case types.entityGroup.columnType.timeseries.value:
                        obj.timeseries.push({
                            key: arrayParam[j].key,
                            value: data.rows[i][j]
                        });
                        break;
                    case types.entityGroup.columnType.entityField.value:
                        switch (arrayParam[j].key) {
                            case types.entityGroup.entityField.name.value:
                                obj.name = data.rows[i][j];
                                break;
                            case types.entityGroup.entityField.type.value:
                                obj.type = data.rows[i][j];
                                break;
                        }
                        break;
                }
            }
            arrayData.push(obj);
        }
        importExport.createMultiEntity(arrayData, vm.entityType, vm.importParams.isUpdate, config).then(function () {
            $mdDialog.hide();
        });
    }

    function clearFile() {
        $scope.theForm.$setDirty();
        vm.fileName = null;
        vm.importData = null;
        vm.columnsParam = [];
    }

    function importFromJson() {
        $scope.theForm.$setPristine();
        $mdDialog.hide(vm.importData);
    }
}
