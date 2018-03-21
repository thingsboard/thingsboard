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

import RuleChainRoutes from './rulechain.routes';
import RuleChainsController from './rulechains.controller';
import {RuleChainController, AddRuleNodeController, AddRuleNodeLinkController} from './rulechain.controller';
import RuleChainDirective from './rulechain.directive';
import RuleNodeDirective from './rulenode.directive';
import LinkDirective from './link.directive';

export default angular.module('thingsboard.ruleChain', [])
    .config(RuleChainRoutes)
    .controller('RuleChainsController', RuleChainsController)
    .controller('RuleChainController', RuleChainController)
    .controller('AddRuleNodeController', AddRuleNodeController)
    .controller('AddRuleNodeLinkController', AddRuleNodeLinkController)
    .directive('tbRuleChain', RuleChainDirective)
    .directive('tbRuleNode', RuleNodeDirective)
    .directive('tbRuleNodeLink', LinkDirective)
    .name;
