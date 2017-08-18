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
import uiRouter from 'angular-ui-router';
import thingsboardGrid from '../components/grid.directive';
import thingsboardApiUser from '../api/user.service';
import thingsboardToast from '../services/toast';

import UserRoutes from './user.routes';
import UserController from './user.controller';
import AddUserController from './add-user.controller';
import ActivationLinkDialogController from './activation-link.controller';
import UserDirective from './user.directive';

export default angular.module('thingsboard.user', [
    uiRouter,
    thingsboardGrid,
    thingsboardApiUser,
    thingsboardToast
])
    .config(UserRoutes)
    .controller('UserController', UserController)
    .controller('AddUserController', AddUserController)
    .controller('ActivationLinkDialogController', ActivationLinkDialogController)
    .directive('tbUser', UserDirective)
    .name;
