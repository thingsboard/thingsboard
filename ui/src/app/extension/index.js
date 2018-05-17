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
import ExtensionTableDirective from './extension-table.directive';
import ExtensionFormHttpDirective from './extensions-forms/extension-form-http.directive';
import ExtensionFormMqttDirective from './extensions-forms/extension-form-mqtt.directive'
import ExtensionFormOpcDirective from './extensions-forms/extension-form-opc.directive';
import ExtensionFormModbusDirective from './extensions-forms/extension-form-modbus.directive';

import {ParseToNull} from './extension-dialog.controller';

export default angular.module('thingsboard.extension', [])
    .directive('tbExtensionTable', ExtensionTableDirective)
    .directive('tbExtensionFormHttp', ExtensionFormHttpDirective)
    .directive('tbExtensionFormMqtt', ExtensionFormMqttDirective)
    .directive('tbExtensionFormOpc', ExtensionFormOpcDirective)
    .directive('tbExtensionFormModbus', ExtensionFormModbusDirective)
    .directive('parseToNull', ParseToNull)
    .name;