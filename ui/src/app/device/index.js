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
import uiRouter from 'angular-ui-router';
import thingsboardGrid from '../components/grid.directive';
import thingsboardApiUser from '../api/user.service';
import thingsboardApiDevice from '../api/device.service';
import thingsboardApiCustomer from '../api/customer.service';

import DeviceRoutes from './device.routes';
import {DeviceController, DeviceCardController} from './device.controller';
import AssignDeviceToCustomerController from './assign-to-customer.controller';
import AddDevicesToCustomerController from './add-devices-to-customer.controller';
import ManageDeviceCredentialsController from './device-credentials.controller';
import DeviceDirective from './device.directive';

export default angular.module('thingsboard.device', [
    uiRouter,
    thingsboardGrid,
    thingsboardApiUser,
    thingsboardApiDevice,
    thingsboardApiCustomer
])
    .config(DeviceRoutes)
    .controller('DeviceController', DeviceController)
    .controller('DeviceCardController', DeviceCardController)
    .controller('AssignDeviceToCustomerController', AssignDeviceToCustomerController)
    .controller('AddDevicesToCustomerController', AddDevicesToCustomerController)
    .controller('ManageDeviceCredentialsController', ManageDeviceCredentialsController)
    .directive('tbDevice', DeviceDirective)
    .name;
