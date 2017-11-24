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

import './extension-form.scss';

/* eslint-disable angular/log */

import extensionFormMqttTemplate from './extension-form-mqtt.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ExtensionFormHttpDirective($compile, $templateCache, $translate, types) {

    var linker = function(scope, element) {

        var template = $templateCache.get(extensionFormMqttTemplate);
        element.html(template);

        scope.types = types;
        scope.theForm = scope.$parent.theForm;

        scope.nameExpressions = {
            deviceNameJsonExpression: "extension.converter-json",
            deviceNameTopicExpression: "extension.topic"
        };
        scope.typeExpressions = {
            deviceTypeJsonExpression: "extension.converter-json",
            deviceTypeTopicExpression: "extension.topic"
        };

        scope.extensionCustomConverterOptions = {
            useWrapMode: false,
            mode: 'json',
            showGutter: true,
            showPrintMargin: true,
            theme: 'github',
            advanced: {
                enableSnippets: true,
                enableBasicAutocompletion: true,
                enableLiveAutocompletion: true
            },
            onLoad: function(_ace) {
                _ace.$blockScrolling = 1;
            }
        };


        if(scope.isAdd) {
            scope.brokers = [];
            scope.config.brokers = scope.brokers;
        } else {
            scope.brokers = scope.config.brokers;
        }

        scope.updateValidity = function () {
            var valid = scope.brokers && scope.brokers.length > 0;
            scope.theForm.$setValidity('brokers', valid);
            if(scope.brokers.length) {
                for(let i=0;i<scope.brokers.length;i++) {
                    if(scope.brokers[i].credentials.type == scope.types.mqttCredentialTypes.pem.value) {
                        if(!(scope.brokers[i].credentials.caCert && scope.brokers[i].credentials.privateKey && scope.brokers[i].credentials.cert)) {
                            scope.theForm.$setValidity('cert.PEM', false);
                            break;
                        } else {
                            scope.theForm.$setValidity('cert.PEM', true);
                        }
                    }
                }
            }
        }

        scope.$watch('brokers', function() {
            scope.updateValidity();
        }, true);

        scope.addBroker = function() {
            var newBroker = {host:"localhost", port:1882, ssl:false, retryInterval:3000, credentials:{type:"anonymous"}, mapping:[]};
            scope.brokers.push(newBroker);
        }

        scope.removeBroker = function(broker) {
            var index = scope.brokers.indexOf(broker);
            if (index > -1) {
                scope.brokers.splice(index, 1);
            }
            scope.theForm.$setDirty();
        }

        scope.addMap = function(mapping) {
            var newMap = {topicFilter:"sensors", converter:{attributes:[],timeseries:[]}};

            mapping.push(newMap);
        }

        scope.removeMap = function(map, mapping) {
            var index = mapping.indexOf(map);
            if (index > -1) {
                mapping.splice(index, 1);
            }
            scope.theForm.$setDirty();
        }

        scope.addAttribute = function(attributes) {
            var newAttribute = {type:"", key:"", value:""};
            attributes.push(newAttribute);
        }

        scope.removeAttribute = function(attribute, attributes) {
            var index = attributes.indexOf(attribute);
            if (index > -1) {
                attributes.splice(index, 1);
            }
            scope.theForm.$setDirty();
        }

        scope.changeCredentials = function(broker) {
            var type = broker.credentials.type;
            broker.credentials = {};
            broker.credentials.type = type;
        }

        scope.changeConverterType = function(map) {
            if(map.converterType == "custom"){
                map.converter = "";
            }
            if(map.converterType == "json") {
                map.converter = {attributes:[],timeseries:[]};
            }
        }

        scope.changeNameExpression = function(converter) {
            if(converter.nameExp == "deviceNameJsonExpression") {
                if(converter.deviceNameTopicExpression) {
                    delete converter.deviceNameTopicExpression;
                }
            }
            if(converter.nameExp == "deviceNameTopicExpression") {
                if(converter.deviceNameJsonExpression) {
                    delete converter.deviceNameJsonExpression;
                }
            }
        }

        scope.changeTypeExpression = function(converter) {
            if(converter.typeExp == "deviceTypeJsonExpression") {
                if(converter.deviceTypeTopicExpression) {
                    delete converter.deviceTypeTopicExpression;
                }
            }
            if(converter.typeExp == "deviceTypeTopicExpression") {
                if(converter.deviceTypeJsonExpression) {
                    delete converter.deviceTypeJsonExpression;
                }
            }
        }

        scope.validateCustomConverter = function(model, editorName) {
            if(model && model.length) {
                try {
                    angular.fromJson(model);
                    scope.theForm[editorName].$setValidity('converterJSON', true);
                } catch(e) {
                    scope.theForm[editorName].$setValidity('converterJSON', false);
                }
            }
        }

        scope.fileAdded = function($file, broker, fileType) {
            var reader = new FileReader();
            reader.onload = function(event) {
                scope.$apply(function() {
                    if(event.target.result) {
                        scope.theForm.$setDirty();
                        var addedFile = event.target.result;
                        if (addedFile && addedFile.length > 0) {
                            if(fileType == "caCert") {
                                broker.credentials.caCertFileName = $file.name;
                                broker.credentials.caCert = addedFile.replace(/^data.*base64,/, "");
                            }
                            if(fileType == "privateKey") {
                                broker.credentials.privateKeyFileName = $file.name;
                                broker.credentials.privateKey = addedFile.replace(/^data.*base64,/, "");
                            }
                            if(fileType == "Cert") {
                                broker.credentials.certFileName = $file.name;
                                broker.credentials.cert = addedFile.replace(/^data.*base64,/, "");
                            }
                        }
                    }
                });
            };
            reader.readAsDataURL($file.file);
        }

        scope.clearFile = function(broker, fileType) {
            scope.theForm.$setDirty();
            if(fileType == "caCert") {
                broker.credentials.caCertFileName = null;
                broker.credentials.caCert = null;
            }
            if(fileType == "privateKey") {
                broker.credentials.privateKeyFileName = null;
                broker.credentials.privateKey = null;
            }
            if(fileType == "Cert") {
                broker.credentials.certFileName = null;
                broker.credentials.cert = null;
            }
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "A",
        link: linker,
        scope: {
            config: "=",
            isAdd: "="
        }
    }
}