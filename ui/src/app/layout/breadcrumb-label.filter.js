/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
export default function BreadcrumbLabel($translate) {
    var labels = {};

    var breadcrumbLabel = function (bLabel) {

        var labelObj;
        labelObj = angular.fromJson(bLabel);
        if (labelObj) {
            var translate = !(labelObj.translate && labelObj.translate === 'false');
            var key = translate ? $translate.use() : 'orig';
            if (!labels[labelObj.label]) {
                labels[labelObj.label] = {};
            }
            if (!labels[labelObj.label][key]) {
                labels[labelObj.label][key] = labelObj.label;
                if (translate) {
                    $translate([labelObj.label]).then(
                        function (translations) {
                            labels[labelObj.label][key] = translations[labelObj.label];
                        }
                    )
                }
            }
            return labels[labelObj.label][key];
        } else {
            return '';
        }
    };

    breadcrumbLabel.$stateful = true;

    return breadcrumbLabel;
}
