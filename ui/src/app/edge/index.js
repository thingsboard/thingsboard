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
import thingsboardApiEdge from '../api/edge.service';
import thingsboardApiCustomer from '../api/customer.service';

import EdgeRoutes from './edge.routes';
import {EdgeController, EdgeCardController} from './edge.controller';
import AssignEdgeToCustomerController from './assign-to-customer.controller';
import AddEdgesToCustomerController from './add-edges-to-customer.controller';
import SetRootRuleChainToEdgesController from './set-root-rule-chain-to-edges.controller';
import EdgeDirective from './edge.directive';
import EdgeDownlinksContentDialogController from './downlinks/edge-downlinks-content-dialog.controller';
import EdgeDownlinksHeaderDirective from './downlinks/edge-downlinks-header.directive';
import EdgeDownlinksRowDirective from './downlinks/edge-downlinks-row.directive';
import EdgeDownlinksDirective from "./downlinks/edge-downlinks-table.directive";

export default angular.module('thingsboard.edge', [
    uiRouter,
    thingsboardGrid,
    thingsboardApiUser,
    thingsboardApiEdge,
    thingsboardApiCustomer
])
    .config(EdgeRoutes)
    .controller('EdgeController', EdgeController)
    .controller('EdgeCardController', EdgeCardController)
    .controller('AssignEdgeToCustomerController', AssignEdgeToCustomerController)
    .controller('AddEdgesToCustomerController', AddEdgesToCustomerController)
    .controller('SetRootRuleChainToEdgesController', SetRootRuleChainToEdgesController)
    .controller('EdgeDownlinksContentDialogController', EdgeDownlinksContentDialogController)
    .directive('tbEdge', EdgeDirective)
    .directive('tbEdgeDownlinksTable', EdgeDownlinksDirective)
    .directive('tbEdgeDownlinksHeader', EdgeDownlinksHeaderDirective)
    .directive('tbEdgeDownlinksRow', EdgeDownlinksRowDirective)
    .name;
