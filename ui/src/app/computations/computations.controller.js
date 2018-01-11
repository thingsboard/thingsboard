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

/*@ngInject*/
export default function ComputationsController($scope, $q, $rootScope, $log, computationService, toast){

    var vm = this;

    vm.upload = upload;

    function upload() {
        var file = $scope.fileName;
        $log.log("Checking file.", file);
        if(file){
            $log.log("file got.");
            var deferred = $q.defer();
            computationService.upload(file).then(
                function success(fileInfo) {
                    //$rootScope.$broadcast('deviceSaved');
                    $log.log("Got file info", fileInfo);
                    toast.showSuccess(file.name + ' uploaded successfully!');
                },
                function fail() {
                    $log.log("Failed to upload file");
                    deferred.reject();
                }
            );
            return deferred.promise;
        }
    }
}