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
/* eslint-disable import/no-unresolved, import/default */

import socialsharePanelTemplate from './socialshare-panel.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.socialsharePanel', [])
    .directive('tbSocialSharePanel', SocialsharePanel)
    .name;

/*@ngInject*/
function SocialsharePanel() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            shareTitle: '@',
            shareText: '@',
            shareLink: '@',
            shareHashTags: '@'
        },
        controller: SocialsharePanelController,
        controllerAs: 'vm',
        templateUrl: socialsharePanelTemplate
    };
}

/*@ngInject*/
function SocialsharePanelController(utils) {

    let vm = this;

    vm.isShareLinkLocal = function() {
        if (vm.shareLink && vm.shareLink.length > 0) {
            return utils.isLocalUrl(vm.shareLink);
        } else {
            return true;
        }
    }

}