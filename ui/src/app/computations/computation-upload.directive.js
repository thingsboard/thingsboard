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
/* eslint-disable import/no-unresolved, import/default */

//import computationsUploadTemplate from './computation-upload.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ComputationUploadDirective($parse, $log) {
  var linker = function(scope, element, attrs){
      //var template = $templateCache.get(computationsUploadTemplate);
      //element.html(template);

      var model = $parse(attrs.fileModel);
      var modelSetter = model.assign;

      $log.log(element);

      element.bind('change', function(){
          scope.$apply(function(){
              $log.log(element[0].files);
              modelSetter(scope, element[0].files[0]);
          });
      });

      //$compile(element.contents())(scope);
  }

  return {
         restrict: 'A',
         link: linker/*,
         scope: {
            onUpload: '&'
         }*/
       };
}