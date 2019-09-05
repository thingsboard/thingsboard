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

function WebCameraWidgetController($element, $scope, $window) { //eslint-disable-line
    let vm = this;

    vm.videoInput = [];
    // vm.audioInput = [];
    vm.videoDevice = "";
    // vm.audioDevice = "";
    vm.screenShot = "";
    vm.isShowCamera = false;
    vm.isPreviewPhoto = false;

    let streamDevice = null;
    let indexWebCamera = 0;

    const videoElement = $element[0].querySelector('#videoStream');
    const canvas = $element[0].querySelector('canvas');

    vm.getStream = getStream;
    vm.takeScreenshot = takeScreenshot;
    vm.takePhoto = takePhoto;
    vm.switchWebCamera = switchWebCamera;
    vm.cancelPhoto = cancelPhoto;

    $scope.$watch('vm.ctx', function() {
        if (vm.ctx) {
            if (!hasGetUserMedia()) {
                alert('getUserMedia() is not supported by your browser');//eslint-disable-line
            } else {
                getDevices().then(gotDevices).then(getStream);
            }
        }
    });

    function hasGetUserMedia() {
        return !!($window.navigator.mediaDevices && $window.navigator.mediaDevices.getUserMedia);
    }

    function takePhoto(){
        vm.isShowCamera = true;
        getDevices().then(gotDevices).then(getStream);
    }

    function cancelPhoto() {
        vm.isPreviewPhoto = false;
        vm.screenShot = "";
    }

    function switchWebCamera() {
        indexWebCamera = (indexWebCamera+1)%vm.videoInput.length;
        vm.videoDevice = vm.videoInput[indexWebCamera].deviceId;
        getStream();
    }

    function getDevices() {
        // AFAICT in Safari this only gets default devices until gUM is called :/
        return $window.navigator.mediaDevices.enumerateDevices();
    }

    function gotDevices(deviceInfos) {
        console.log('Available input and output devices:', deviceInfos);//eslint-disable-line
        for (const deviceInfo of deviceInfos) {
            let device = {
                deviceId: deviceInfo.deviceId,
                label: ""
            };
            // if (deviceInfo.kind === 'audioinput') {
            //     device.label = deviceInfo.label || `Microphone ${vm.audioInput.length + 1}`;
            //     vm.audioInput.push(device);
            // } else
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
            // audio: {deviceId: vm.audioDevice !== "" ? {exact: vm.audioDevice} : undefined},
            video: {deviceId: vm.videoDevice !== "" ? {exact: vm.videoDevice} : undefined}
        };
        return $window.navigator.mediaDevices.getUserMedia(constraints).then(gotStream).catch(handleError);
    }

    function gotStream(stream) {
        streamDevice = stream; // make stream available to console
        // if(vm.audioDevice === ""){
        //     vm.audioDevice = vm.audioInput[vm.audioInput.findIndex(option => option.label === stream.getAudioTracks()[0].label)].deviceId;
        // }
        if(vm.videoDevice === ""){
            indexWebCamera = vm.videoInput.findIndex(option => option.label === stream.getVideoTracks()[0].label);
            indexWebCamera = indexWebCamera === -1 ? 0 : indexWebCamera;
            vm.videoDevice = vm.videoInput[indexWebCamera].deviceId;
        }
        videoElement.srcObject = stream;
    }

    function takeScreenshot() {
        canvas.width = videoElement.videoWidth;
        canvas.height = videoElement.videoHeight;
        canvas.getContext('2d').drawImage(videoElement, 0, 0);
        vm.screenShot = canvas.toDataURL('image/webp');
        vm.isPreviewPhoto = true;
        console.log(vm.screenShot);//eslint-disable-line
    }

    function handleError(error) {
        console.error('Error: ', error);//eslint-disable-line
    }
}
