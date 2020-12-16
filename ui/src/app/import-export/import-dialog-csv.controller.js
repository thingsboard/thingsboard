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
import './import-dialog.scss';

/*@ngInject*/
export default function ImportDialogCsvController($scope, $mdDialog, toast, importTitle, importFileLabel, entityType, importExport, types, $mdStepper, $timeout) {

    var vm = this;

    vm.cancel = cancel;
    vm.fileAdded = fileAdded;
    vm.clearFile = clearFile;
    vm.nextStep = nextStep;
    vm.previousStep = previousStep;

    vm.importParameters = {
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
    vm.isImportData = false;

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

    vm.progressCreate = 0;

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
            delim: vm.importParameters.delim,
            header: vm.importParameters.isHeader
        };
        return importExport.convertCSVToJson(importData, config);
    }

    function createColumnsData(parseData) {
        vm.columnsParam = [];
        var columnParam = {};
        for (var i = 0; i < parseData.headers.length; i++) {
            if (vm.importParameters.isHeader && parseData.headers[i].search(/^(name|type|label)$/im) === 0) {
                columnParam = {
                    type: types.importEntityColumnType[parseData.headers[i].toLowerCase()].value,
                    key: parseData.headers[i].toLowerCase(),
                    sampleData: parseData.rows[0][i]
                };
            } else {
                columnParam = {
                    type: types.importEntityColumnType.serverAttribute.value,
                    key: vm.importParameters.isHeader ? parseData.headers[i] : "",
                    sampleData: parseData.rows[0][i]
                };
            }
            vm.columnsParam.push(columnParam);
        }
    }

    function addEntities (importData, parameterColumns) {
        var entitiesData = [];
        var sentDataLength = 0;
        var config = {
            ignoreErrors: true,
            resendRequest: true
        };

        for (var i = 0; i < importData.rows.length; i++) {
            var entityData = {
                name: "",
                type: "",
                description: "",
                gateway: null,
                label: "",
                accessToken: "",
                attributes: {
                    server: [],
                    shared: []
                },
                timeseries: [],
                edgeLicenseKey: "",
                cloudEndpoint: "",
                routingKey: "",
                secret: ""
            };
            for (var j = 0; j < parameterColumns.length; j++) {
                switch (parameterColumns[j].type) {
                    case types.importEntityColumnType.serverAttribute.value:
                        entityData.attributes.server.push({
                            key: parameterColumns[j].key,
                            value: importData.rows[i][j]
                        });
                        break;
                    case types.importEntityColumnType.timeseries.value:
                        entityData.timeseries.push({
                            key: parameterColumns[j].key,
                            value: importData.rows[i][j]
                        });
                        break;
                    case types.importEntityColumnType.sharedAttribute.value:
                        entityData.attributes.shared.push({
                            key: parameterColumns[j].key,
                            value: importData.rows[i][j]
                        });
                        break;
                    case types.importEntityColumnType.accessToken.value:
                        entityData.accessToken = importData.rows[i][j];
                        break;
                    case types.importEntityColumnType.name.value:
                        entityData.name = importData.rows[i][j];
                        break;
                    case types.importEntityColumnType.type.value:
                        entityData.type = importData.rows[i][j];
                        break;
                    case types.importEntityColumnType.label.value:
                        entityData.label = importData.rows[i][j];
                        break;
                    case types.importEntityColumnType.isGateway.value:
                        entityData.gateway = importData.rows[i][j];
                        break;
                    case types.importEntityColumnType.description.value:
                        entityData.description = importData.rows[i][j];
                        break;
                    case types.importEntityColumnType.edgeLicenseKey.value:
                        entityData.edgeLicenseKey = importData.rows[i][j];
                        break;
                    case types.importEntityColumnType.cloudEndpoint.value:
                        entityData.cloudEndpoint = importData.rows[i][j];
                        break;
                    case types.importEntityColumnType.routingKey.value:
                        entityData.routingKey = importData.rows[i][j];
                        break;
                    case types.importEntityColumnType.secret.value:
                        entityData.secret = importData.rows[i][j];
                        break;
                }
            }
            entitiesData.push(entityData);
        }
        $scope.$on('createImportEntityCompleted', function () {
            sentDataLength++;
            vm.progressCreate = Math.round((sentDataLength / importData.rows.length) * 100);
        });
        importExport.createMultiEntity(entitiesData, vm.entityType, vm.importParameters.isUpdate, config).then(function (response) {
            vm.statistical = response;
            vm.isImportData = false;
            $mdStepper('import-stepper').next();
        });
    }

    function clearFile() {
        vm.theFormStep1.$setDirty();
        vm.fileName = null;
        vm.importData = null;
    }

    function previousStep(step) {
        let steppers = $mdStepper('import-stepper');
        switch (step) {
            case 1:
                steppers.back();
                $timeout(function () {
                    vm.theFormStep1.$setDirty();
                });
                break;
            default:
                steppers.back();
                break;
        }
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
                    steppers.back();
                    $timeout(function () {
                        clearFile();
                    });
                } else {
                    createColumnsData(parseData);
                    steppers.next();
                }
                break;
            case 4:
                steppers.next();
                vm.isImportData = true;
                addEntities(parseData, vm.columnsParam);
                break;
            case 6:
                $mdDialog.hide();
                break;
        }

    }

    function cancel() {
        if($mdStepper('import-stepper').currentStep > 2){
            $mdDialog.hide();
        } else {
            $mdDialog.cancel();
        }
    }
}
