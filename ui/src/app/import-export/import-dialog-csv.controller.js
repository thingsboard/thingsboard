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
export default function ImportDialogCsvController($scope, $mdDialog, toast, importTitle, importFileLabel, entityType, importExport, types, $mdStepper) {

    var vm = this;

    vm.cancel = cancel;
    vm.finishExport = finishExport;
    vm.fileAdded = fileAdded;
    vm.clearFile = clearFile;
    vm.nextStep = nextStep;
    vm.previousStep = previousStep;

    vm.addDevices = addDevices;
    vm.importParams = {
        delim: ',',
        isUpdate: true,
        isHeader: true
    };

    vm.importTitle = importTitle;
    vm.importFileLabel = importFileLabel;
    vm.entityType = entityType;

    vm.isVertical = true;
    vm.isLinear = true;
    vm.isAlternative = false;
    vm.isMobileStepText = true;

    vm.columnsParam = [];
    vm.parseData = [];

    vm.delimiters = [{
        key: ',',
        value: ','
    }, {
        key: ';',
        value: ';'
    }, {
        key: '|',
        value: '|'
    }, {
        key: '\t',
        value: 'Tab'
    }];

    var parseData = {};

    function fileAdded($file) {
        if ($file.getExtension() === 'csv') {
            var reader = new FileReader();
            reader.onload = function (event) {
                $scope.$apply(function () {
                    if (event.target.result) {
                        vm.theFormStep1.$setDirty();
                        var importCSV = event.target.result;
                        if (importCSV && importCSV.length > 0) {
                            try {
                                vm.importData = importCSV;
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

    function parseCSV(importData) {
        var config = {
            delim: vm.importParams.delim,
            header: vm.importParams.isHeader
        };
        return importExport.convertCSVToJson(importData, config);
    }

    function createColumnsData(parseData) {
        var columnParam = {};
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

    function addDevices(importData, parameterColumns) {
        var entitysData = [];
        var config = {
            ignoreErrors: true,
            resendRequest: true
        };
        for (var i = 0; i < importData.rows.length; i++) {
            var entityData = {
                name: "",
                type: "",
                attributes: {
                    server: [],
                    shared: []
                },
                timeseries: []
            };
            for (var j = 0; j < parameterColumns.length; j++) {
                switch (parameterColumns[j].type) {
                    case types.entityGroup.columnType.serverAttribute.value:
                        entityData.attributes.server.push({
                            key: parameterColumns[j].key,
                            value: importData.rows[i][j]
                        });
                        break;
                    case types.entityGroup.columnType.sharedAttribute.value:
                        entityData.attributes.shared.push({
                            key: parameterColumns[j].key,
                            value: importData.rows[i][j]
                        });
                        break;
                    case types.entityGroup.columnType.timeseries.value:
                        entityData.timeseries.push({
                            key: parameterColumns[j].key,
                            value: importData.rows[i][j]
                        });
                        break;
                    case types.entityGroup.columnType.entityField.value:
                        switch (parameterColumns[j].key) {
                            case types.entityGroup.entityField.name.value:
                                entityData.name = importData.rows[i][j];
                                break;
                            case types.entityGroup.entityField.type.value:
                                entityData.type = importData.rows[i][j];
                                break;
                        }
                        break;
                }
            }
            entitysData.push(entityData);
        }
        importExport.createMultiEntity(entitysData, vm.entityType, vm.importParams.isUpdate, config).then(function (response) {
            vm.statistical = response;
            $mdStepper('import-stepper').next();
        });
    }

    function clearFile() {
        vm.theFormStep1.$setDirty();
        vm.fileName = null;
        vm.importData = null;
    }

    function previousStep() {
        let steppers = $mdStepper('import-stepper');
        steppers.back();
    }

    function nextStep(step) {
        let steppers = $mdStepper('import-stepper');
        switch (step) {
            case 2:
                steppers.next();
                break;
            case 3:
                parseData = parseCSV(vm.importData);
                if (parseData === -1) {
                    clearFile();
                    steppers.back();
                } else {
                    createColumnsData(parseData);
                    steppers.next();
                }
                break;
            case 4:
                steppers.next();
                addDevices(parseData, vm.columnsParam)
                break;
        }

    }

    function cancel() {
        $mdDialog.cancel();
    }

    function finishExport() {
        $mdDialog.hide();
    }
}
