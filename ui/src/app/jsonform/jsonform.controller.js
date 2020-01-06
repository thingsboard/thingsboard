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
import './jsonform.scss';

/*@ngInject*/
export default function JsonFormController($scope/*, $rootScope, $log*/) {

    var vm = this;

    vm.pretty = pretty;
    vm.resetModel = resetModel;
    vm.itParses     = true;
    vm.itParsesForm = true;

    vm.formJson = "[   \n" +
        "    {\n" +
        "        \"key\": \"name\",\n" +
        "\t\"type\": \"text\"        \n" +
        "    },\n" +
        "    {\n" +
        "\t\"key\": \"name2\",\n" +
        "\t\"type\": \"color\"\n" +
        "    },\n" +
        "    {\n" +
        "\t\"key\": \"name3\",\n" +
        "\t\"type\": \"javascript\"\n" +
        "    },    \n" +
            "\t\"name4\"\n" +
        "]";
    vm.schemaJson = "{\n" +
        "    \"type\": \"object\",\n" +
        "    \"title\": \"Comment\",\n" +
        "    \"properties\": {\n" +
        "        \"name\": {\n" +
        "            \"title\": \"Name 1\",\n" +
        "            \"type\": \"string\"\n" +
        "         },\n" +
        "        \"name2\": {\n" +
        "            \"title\": \"Name 2\",\n" +
        "            \"type\": \"string\"\n" +
        "         },\n" +
        "        \"name3\": {\n" +
        "            \"title\": \"Name 3\",\n" +
        "            \"type\": \"string\"\n" +
        "         },\n" +
        "        \"name4\": {\n" +
        "            \"title\": \"Name 4\",\n" +
        "            \"type\": \"number\"\n" +
        "         }\n" +
        "     },\n" +
        "     \"required\": [\n" +
        "         \"name1\", \"name2\", \"name3\", \"name4\"\n" +
        "     ]\n" +
        "}";
/*        '{\n'+
    '    "type": "object",\n'+
    '    "title": "Comment",\n'+
    '    "properties": {\n'+
    '        "name": {\n'+
    '            "title": "Name",\n'+
    '            "type": "string"\n'+
    '         }\n'+
    '     },\n'+
    '     "required": [\n'+
    '         "name"\n'+
    '     ]\n'+
    '}';*/

    vm.schema = angular.fromJson(vm.schemaJson);
    vm.form = angular.fromJson(vm.formJson);
    vm.model = { name: '#ccc' };

    $scope.$watch('vm.schemaJson',function(val,old){
        if (val && val !== old) {
            try {
                vm.schema = angular.fromJson(vm.schemaJson);
                vm.itParses = true;
            } catch (e){
                vm.itParses = false;
            }
        }
    });


    $scope.$watch('vm.formJson',function(val,old){
        if (val && val !== old) {
            try {
                vm.form = angular.fromJson(vm.formJson);
                vm.itParsesForm = true;
            } catch (e){
                vm.itParsesForm = false;
            }
        }
    });

    function pretty (){
        return angular.isString(vm.model) ? vm.model : angular.toJson(vm.model, true);
    }

    function resetModel () {
        $scope.ngform.$setPristine();
        vm.model = { name: 'New hello world!' };
    }
}
