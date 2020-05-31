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
import './web-camera-input-widget.scss';

/* eslint-disable import/no-unresolved, import/default */
import webCameraWidgetTemplate from './web-camera-input-widget.tpl.html';
/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.webCameraWidget', [])
    .directive('tbWebCameraWidget', webCameraWidget)
    .name;

/*@ngInject*/
function webCameraWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            ctx: '='
        },
        controller: WebCameraWidgetController,
        controllerAs: 'vm',
        templateUrl: webCameraWidgetTemplate
    };
}

function WebCameraWidgetController($element, $scope, $window, types, utils, attributeService, Fullscreen) {
    let vm = this;

    vm.videoInput = [];
    vm.videoDevice = "";
    vm.previewPhoto = "";
    vm.isShowCamera = false;
    vm.isPreviewPhoto = false;

    let streamDevice = null;
    let indexWebCamera = 0;
    let videoElement = null;
    let canvas = null;
    let photoCamera = null;
    let dataKeyType = "";
    let width = 640;
    let height = 480;

    const DEFAULT_IMAGE_TYPE = 'image/jpeg';
    const DEFAULT_IMAGE_QUALITY = 0.92;

    vm.getStream = getStream;
    vm.createPhoto = createPhoto;
    vm.takePhoto = takePhoto;
    vm.switchWebCamera = switchWebCamera;
    vm.cancelPhoto = cancelPhoto;
    vm.closeCamera = closeCamera;
    vm.savePhoto = savePhoto;

    vm.isEntityDetected = false;
    vm.dataKeyDetected = false;
    vm.isCameraSupport = false;
    vm.isDeviceDetect = false;

    $scope.$watch('vm.ctx', function () {
        if (vm.ctx && vm.ctx.datasources && vm.ctx.datasources.length) {
            let datasource = vm.ctx.datasources[0];
            if (datasource.type === types.datasourceType.entity) {
                if (datasource.entityType && datasource.entityId) {
                    if (vm.ctx.settings.widgetTitle && vm.ctx.settings.widgetTitle.length) {
                        $scope.titleTemplate = utils.customTranslation(vm.ctx.settings.widgetTitle, vm.ctx.settings.widgetTitle);
                    } else {
                        $scope.titleTemplate = vm.ctx.widgetConfig.title;
                    }
                    vm.isEntityDetected = true;
                }
            }
            width = vm.ctx.settings.maxWidth ? vm.ctx.settings.maxWidth : 640;
            height = vm.ctx.settings.maxHeight ? vm.ctx.settings.maxWidth : 480;
            if (datasource.dataKeys.length) {
                $scope.currentKey = datasource.dataKeys[0].name;
                dataKeyType = datasource.dataKeys[0].type;
                vm.dataKeyDetected = true;
            }
            if (hasGetUserMedia()) {
                vm.isCameraSupport = true;
                getDevices().then(gotDevices).then(() => {
                    vm.isDeviceDetect = !!vm.videoInput.length;
                });
            }
        }
    });

    function getVideoAspectRatio() {
        if (videoElement.videoWidth && videoElement.videoWidth > 0 &&
            videoElement.videoHeight && videoElement.videoHeight > 0) {
            return videoElement.videoWidth / videoElement.videoHeight;
        }
        return width / height;
    }

    vm.videoWidth = function() {
        const videoRatio = getVideoAspectRatio();
        return Math.min(width, height * videoRatio);
    }

    vm.videoHeight = function() {
        const videoRatio = getVideoAspectRatio();
        return Math.min(height, width / videoRatio);
    }

    function hasGetUserMedia() {
        return !!($window.navigator.mediaDevices && $window.navigator.mediaDevices.getUserMedia);
    }

    function takePhoto(){
        vm.isShowCamera = true;
        videoElement = $element[0].querySelector('#videoStream');
        photoCamera = $element[0].querySelector('#photoCamera');
        canvas = $element[0].querySelector('canvas');
        Fullscreen.enable(photoCamera);
        getStream();
    }

    function cancelPhoto() {
        vm.isPreviewPhoto = false;
        vm.previewPhoto = "";
    }

    function switchWebCamera() {
        indexWebCamera = (indexWebCamera+1)%vm.videoInput.length;
        vm.videoDevice = vm.videoInput[indexWebCamera].deviceId;
        getStream();
    }

    function getDevices() {
        return $window.navigator.mediaDevices.enumerateDevices();
    }

    function gotDevices(deviceInfos) {
        for (const deviceInfo of deviceInfos) {
            let device = {
                deviceId: deviceInfo.deviceId,
                label: ""
            };
            if (deviceInfo.kind === 'videoinput') {
                device.label = deviceInfo.label || `Camera ${vm.videoInput.length + 1}`;
                vm.videoInput.push(device);
            }
        }
    }

    function getStream() {
        if (streamDevice !== null) {
            streamDevice.getTracks().forEach(track => {
                track.stop();
            });
        }
        const constraints = {
            video: {deviceId: vm.videoDevice !== "" ? {exact: vm.videoDevice} : undefined}
        };
        return $window.navigator.mediaDevices.getUserMedia(constraints).then(gotStream);
    }

    function gotStream(stream) {
        streamDevice = stream;
        if(vm.videoDevice === ""){
            indexWebCamera = vm.videoInput.findIndex(option => option.label === stream.getVideoTracks()[0].label);
            indexWebCamera = indexWebCamera === -1 ? 0 : indexWebCamera;
            vm.videoDevice = vm.videoInput[indexWebCamera].deviceId;
        }
        videoElement.srcObject = stream;
    }

    function createPhoto() {
        canvas.width = vm.videoWidth();
        canvas.height = vm.videoHeight();
        canvas.getContext('2d').drawImage(videoElement, 0, 0, vm.videoWidth(), vm.videoHeight());
        const mimeType = vm.ctx.settings.imageFormat ? vm.ctx.settings.imageFormat : DEFAULT_IMAGE_TYPE;
        const quality = vm.ctx.settings.imageQuality ? vm.ctx.settings.imageQuality : DEFAULT_IMAGE_QUALITY;
        vm.previewPhoto = canvas.toDataURL(mimeType, quality);
        vm.isPreviewPhoto = true;
    }

    function closeCamera(){
        Fullscreen.cancel(photoCamera);
        vm.isShowCamera = false;
        if (streamDevice !== null) {
            streamDevice.getTracks().forEach(track => {
                track.stop();
            });
        }
        streamDevice = null;
        videoElement.srcObject = null;
    }

    function savePhoto(){
        let promiseData = null;
        let datasource = vm.ctx.datasources[0];
        let saveData = [{
            key: datasource.dataKeys[0].name,
            value: vm.previewPhoto
        }];
        if(dataKeyType === types.dataKeyType.attribute){
            promiseData = attributeService.saveEntityAttributes(datasource.entityType, datasource.entityId, types.attributesScope.server.value, saveData);
        } else if(dataKeyType === types.dataKeyType.timeseries){
            promiseData = attributeService.saveEntityTimeseries(datasource.entityType, datasource.entityId, "scope", saveData);
        }
        promiseData.then(()=>{
            vm.isPreviewPhoto = false;
            closeCamera();
        })
    }
}
