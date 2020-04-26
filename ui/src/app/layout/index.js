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
import './home.scss';

import uiRouter from 'angular-ui-router';
import ngSanitize from 'angular-sanitize';
import 'angular-breadcrumb';

import thingsboardMenu from '../services/menu.service';
import thingsboardApiDevice from '../api/device.service';
import thingsboardApiLogin from '../api/login.service';
import thingsboardApiUser from '../api/user.service';

import thingsboardNoAnimate from '../components/no-animate.directive';
import thingsboardOnFinishRender from '../components/finish-render.directive';
import thingsboardSideMenu from '../components/side-menu.directive';
import thingsboardNavTree from '../components/nav-tree.directive';
import thingsboardDashboardAutocomplete from '../components/dashboard-autocomplete.directive';
import thingsboardKvMap from '../components/kv-map.directive';
import thingsboardGatewayConfig from '../components/gateway/gateway-config.directive';
import thingsboardGatewayConfigSelect from '../components/gateway/gateway-config-select.directive';
import thingsboardGatewayForm from '../components/gateway/gateway-form.directive';
import thingsboardJsonObjectEdit from '../components/json-object-edit.directive';
import thingsboardJsonContent from '../components/json-content.directive';

import thingsboardUserMenu from './user-menu.directive';

import thingsboardEntity from '../entity';
import thingsboardEvent from '../event';
import thingsboardAlarm from '../alarm';
import thingsboardAuditLog from '../audit';
import thingsboardExtension from '../extension';
import thingsboardTenant from '../tenant';
import thingsboardCustomer from '../customer';
import thingsboardUser from '../user';
import thingsboardHomeLinks from '../home';
import thingsboardAdmin from '../admin';
import thingsboardProfile from '../profile';
import thingsboardAsset from '../asset';
import thingsboardDevice from '../device';
import thingsboardEntityView from '../entity-view';
import thingsboardWidgetLibrary from '../widget';
import thingsboardDashboard from '../dashboard';
import thingsboardRuleChain from '../rulechain';
import thingsboardQueue from '../components/queue';

import thingsboardJsonForm from '../jsonform';

import HomeRoutes from './home.routes';
import HomeController from './home.controller';
import BreadcrumbLabel from './breadcrumb-label.filter';
import BreadcrumbIcon from './breadcrumb-icon.filter';

export default angular.module('thingsboard.home', [
    uiRouter,
    ngSanitize,
    'ncy-angular-breadcrumb',
    thingsboardMenu,
    thingsboardHomeLinks,
    thingsboardUserMenu,
    thingsboardEntity,
    thingsboardEvent,
    thingsboardAlarm,
    thingsboardAuditLog,
    thingsboardExtension,
    thingsboardTenant,
    thingsboardCustomer,
    thingsboardUser,
    thingsboardAdmin,
    thingsboardProfile,
    thingsboardAsset,
    thingsboardDevice,
    thingsboardEntityView,
    thingsboardWidgetLibrary,
    thingsboardDashboard,
    thingsboardRuleChain,
    thingsboardQueue,
    thingsboardJsonForm,
    thingsboardApiDevice,
    thingsboardApiLogin,
    thingsboardApiUser,
    thingsboardNoAnimate,
    thingsboardOnFinishRender,
    thingsboardSideMenu,
    thingsboardNavTree,
    thingsboardDashboardAutocomplete,
    thingsboardKvMap,
    thingsboardGatewayConfig,
    thingsboardGatewayConfigSelect,
    thingsboardGatewayForm,
    thingsboardJsonObjectEdit,
    thingsboardJsonContent
])
    .config(HomeRoutes)
    .controller('HomeController', HomeController)
    .filter('breadcrumbLabel', BreadcrumbLabel)
    .filter('breadcrumbIcon', BreadcrumbIcon)
    .name;
