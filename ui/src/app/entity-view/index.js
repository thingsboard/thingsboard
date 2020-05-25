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
import thingsboardApiEntityView from '../api/entity-view.service';
import thingsboardApiCustomer from '../api/customer.service';

import EntityViewRoutes from './entity-view.routes';
import {EntityViewController, EntityViewCardController} from './entity-view.controller';
import AssignEntityViewToCustomerController from './assign-to-customer.controller';
import AddEntityViewsToCustomerController from './add-entity-views-to-customer.controller';
import EntityViewDirective from './entity-view.directive';
import AddEntityViewsToEdgeController from './add-entity-views-to-edge.controller';

export default angular.module('thingsboard.entityView', [
    uiRouter,
    thingsboardGrid,
    thingsboardApiUser,
    thingsboardApiEntityView,
    thingsboardApiCustomer
])
    .config(EntityViewRoutes)
    .controller('EntityViewController', EntityViewController)
    .controller('EntityViewCardController', EntityViewCardController)
    .controller('AssignEntityViewToCustomerController', AssignEntityViewToCustomerController)
    .controller('AddEntityViewsToCustomerController', AddEntityViewsToCustomerController)
    .controller('AddEntityViewsToEdgeController', AddEntityViewsToEdgeController)
    .directive('tbEntityView', EntityViewDirective)
    .name;
