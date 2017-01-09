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
import thingsboardApiCustomer from '../api/customer.service';
import thingsboardGrid from '../components/grid.directive';
import thingsboardContact from '../components/contact.directive';
import thingsboardContactShort from '../components/contact-short.filter';

import CustomerRoutes from './customer.routes';
import CustomerController from './customer.controller';
import CustomerDirective from './customer.directive';

export default angular.module('thingsboard.customer', [
    uiRouter,
    thingsboardApiCustomer,
    thingsboardGrid,
    thingsboardContact,
    thingsboardContactShort
])
    .config(CustomerRoutes)
    .controller('CustomerController', CustomerController)
    .directive('tbCustomer', CustomerDirective)
    .name;
