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

import beautify from 'js-beautify';

const js_beautify =  beautify.js;

/*@ngInject*/
export default function ExtensionDialogController($scope, $mdDialog, $translate, isAdd, allExtensions, entityId, entityType, extension, types, attributeService) {

    var vm = this;

    vm.types = types;
    vm.isAdd = isAdd;
    vm.entityType = entityType;
    vm.entityId = entityId;
    vm.allExtensions = allExtensions;

    vm.configuration = {};
    vm.newExtension = {id:"",type:"",configuration:vm.configuration};

    if(!vm.isAdd) {
        vm.newExtension = angular.copy(extension);
        vm.configuration = vm.newExtension.configuration;
        editTransformers(vm.newExtension);
    }

    vm.cancel = cancel;
    vm.save = save;

    function cancel() {
        $mdDialog.cancel();
    }
    function save() {
        $mdDialog.hide();
        saveTransformers();
        if(vm.isAdd) {
            vm.allExtensions.push(vm.newExtension);
        } else {
            var index = vm.allExtensions.indexOf(extension);
            if(index > -1) {
                vm.allExtensions[index] = vm.newExtension;
            }
        }

        var editedValue = angular.toJson(vm.allExtensions);

        attributeService.saveEntityAttributes(vm.entityType, vm.entityId, types.attributesScope.shared.value, [{key:"configuration", value:editedValue}]).then(
            function success() {
                $scope.theForm.$setPristine();
            }
        );
    }
    
    vm.validateId = function() {
        var coincidenceArray = vm.allExtensions.filter(function(ext) {
            return ext.id == vm.newExtension.id;
        });
        if(coincidenceArray.length) {
            if(!vm.isAdd) {
                if(coincidenceArray[0].id == extension.id) {
                    $scope.theForm.extensionId.$setValidity('uniqueIdValidation', true);
                } else {
                    $scope.theForm.extensionId.$setValidity('uniqueIdValidation', false);
                }
            } else {
                $scope.theForm.extensionId.$setValidity('uniqueIdValidation', false);
            }
        } else {
            $scope.theForm.extensionId.$setValidity('uniqueIdValidation', true);
        }
    }

    function saveTransformers() {
        if(vm.newExtension.type == types.extensionType.http) {
            var config = vm.newExtension.configuration.converterConfigurations;
            if(config && config.length > 0) {
                for(let i=0;i<config.length;i++) {
                    for(let j=0;j<config[i].converters.length;j++){
                        for(let k=0;k<config[i].converters[j].attributes.length;k++){
                            if(config[i].converters[j].attributes[k].transformerType == "toDouble"){
                                config[i].converters[j].attributes[k].transformer = {type: "intToDouble"};
                            }
                            delete config[i].converters[j].attributes[k].transformerType;
                        }
                        for(let l=0;l<config[i].converters[j].timeseries.length;l++) {
                            if(config[i].converters[j].timeseries[l].transformerType == "toDouble"){
                                config[i].converters[j].timeseries[l].transformer = {type: "intToDouble"};
                            }
                            delete config[i].converters[j].timeseries[l].transformerType;
                        }
                    }
                }
            }
        }
        if(vm.newExtension.type == types.extensionType.mqtt) {
            var brokers = vm.newExtension.configuration.brokers;
            if(brokers && brokers.length > 0) {
                for(let i=0;i<brokers.length;i++) {
                    if(brokers[i].mapping && brokers[i].mapping.length > 0) {
                        for(let j=0;j<brokers[i].mapping.length;j++) {
                            if(brokers[i].mapping[j].converterType == "json") {
                                delete brokers[i].mapping[j].converter.nameExp;
                                delete brokers[i].mapping[j].converter.typeExp;
                            }
                            delete brokers[i].mapping[j].converterType;
                        }
                    }
                }
            }
        }
    }

    function editTransformers(extension) {
        if(extension.type == types.extensionType.http) {
            var config = extension.configuration.converterConfigurations;
            for(let i=0;i<config.length;i++) {
                for(let j=0;j<config[i].converters.length;j++){
                    for(let k=0;k<config[i].converters[j].attributes.length;k++){
                        if(config[i].converters[j].attributes[k].transformer){
                            if(config[i].converters[j].attributes[k].transformer.type == "intToDouble"){
                                config[i].converters[j].attributes[k].transformerType = "toDouble";
                            } else {
                                config[i].converters[j].attributes[k].transformerType = "custom";
                                config[i].converters[j].attributes[k].transformer = js_beautify(config[i].converters[j].attributes[k].transformer, {indent_size: 4});
                            }
                        }
                    }
                    for(let l=0;l<config[i].converters[j].timeseries.length;l++) {
                        if(config[i].converters[j].timeseries[l].transformer){
                            if(config[i].converters[j].timeseries[l].transformer.type == "intToDouble"){
                                config[i].converters[j].timeseries[l].transformerType = "toDouble";
                            } else {
                                config[i].converters[j].timeseries[l].transformerType = "custom";
                                config[i].converters[j].timeseries[l].transformer = js_beautify(config[i].converters[j].timeseries[l].transformer, {indent_size: 4});
                            }
                        }
                    }
                }
            }
        }
        if(extension.type == types.extensionType.mqtt) {
            var brokers = extension.configuration.brokers;
            for(let i=0;i<brokers.length;i++) {
                if(brokers[i].mapping && brokers[i].mapping.length > 0) {
                    for(let j=0;j<brokers[i].mapping.length;j++) {
                        if(brokers[i].mapping[j].converter.type == "json") {
                            if(brokers[i].mapping[j].converter.deviceNameTopicExpression) {
                                brokers[i].mapping[j].converter.nameExp = "deviceNameTopicExpression";
                            } else {
                                brokers[i].mapping[j].converter.nameExp = "deviceNameJsonExpression";
                            }
                            if(brokers[i].mapping[j].converter.deviceTypeTopicExpression) {
                                brokers[i].mapping[j].converter.typeExp = "deviceTypeTopicExpression";
                            } else {
                                brokers[i].mapping[j].converter.typeExp = "deviceTypeJsonExpression";
                            }
                            brokers[i].mapping[j].converterType = "json";
                        } else {
                            brokers[i].mapping[j].converterType = "custom";
                        }
                    }
                }
            }
        }
    }
}

/*@ngInject*/
export function ParseToNull() {
    var linker = function (scope, elem, attrs, ngModel) {
        ngModel.$parsers.push(function(value) {
            if(value === "") {
                return null;
            }
            return value;
        })
    };
    return {
        restrict: "A",
        link: linker,
        require: "ngModel"
    }
}