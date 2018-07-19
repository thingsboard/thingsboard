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
import NodeScriptTestController from './script/node-script-test.controller';
import RuleChainDirective from './rulechain.directive';
import RuleNodeDefinedConfigDirective from './rulenode-defined-config.directive';
import RuleNodeConfigDirective from './rulenode-config.directive';
import RuleNodeDirective from './rulenode.directive';
import LinkDirective from './link.directive';
import MessageTypeAutocompleteDirective from './message-type-autocomplete.directive';
import NodeScriptTest from './script/node-script-test.service';

export default angular.module('thingsboard.ruleChain', [])
    .config(RuleChainRoutes)
    .controller('RuleChainsController', RuleChainsController)
    .controller('RuleChainController', RuleChainController)
    .controller('AddRuleNodeController', AddRuleNodeController)
    .controller('AddRuleNodeLinkController', AddRuleNodeLinkController)
    .controller('NodeScriptTestController', NodeScriptTestController)
    .directive('tbRuleChain', RuleChainDirective)
    .directive('tbRuleNodeDefinedConfig', RuleNodeDefinedConfigDirective)
    .directive('tbRuleNodeConfig', RuleNodeConfigDirective)
    .directive('tbRuleNode', RuleNodeDirective)
    .directive('tbRuleNodeLink', LinkDirective)
    .directive('tbMessageTypeAutocomplete', MessageTypeAutocompleteDirective)
    .factory('ruleNodeScriptTest', NodeScriptTest)
    .name;
