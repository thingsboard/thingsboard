/*
 * Copyright © 2016-2019 The Thingsboard Authors
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
import EdgeRoutes from './edge.routes';
import {EdgeController, EdgeCardController} from './edge.controller';
import EdgeDirective from './edge.directive';

export default angular.module('thingsboard.edge', [])
    .config(EdgeRoutes)
    .controller('EdgeController', EdgeController)
    .controller('EdgeCardController', EdgeCardController)
    .directive('tbEdge', EdgeDirective)
    .name;
