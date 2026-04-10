///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Component, OnInit } from '@angular/core';
import { FcNodeComponent } from 'ngx-flowchart';
import { FcRuleNode, RuleNodeType } from '@shared/models/rule-node.models';
import { Router } from '@angular/router';
import { RuleChainType } from '@app/shared/models/rule-chain.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'rule-node',
    templateUrl: './rulenode.component.html',
    styleUrls: ['./rulenode.component.scss'],
    standalone: false
})
export class RuleNodeComponent extends FcNodeComponent implements OnInit {

  iconUrl: SafeResourceUrl;
  RuleNodeType = RuleNodeType;

  constructor(private sanitizer: DomSanitizer,
              private translate: TranslateService,
              private router: Router) {
    super();
  }

  ngOnInit(): void {
    super.ngOnInit();
    if (this.node.iconUrl) {
      this.iconUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.node.iconUrl);
    }
  }

  openRuleChain($event: Event, node: FcRuleNode) {
    if ($event) {
      $event.stopPropagation();
    }
    if (node.configuration?.ruleChainId) {
      if (node.ruleChainType === RuleChainType.EDGE) {
        this.router.navigateByUrl(`/edgeManagement/ruleChains/${node.configuration?.ruleChainId}`);
      } else {
        this.router.navigateByUrl(`/ruleChains/${node.configuration?.ruleChainId}`);
      }

    }
  }

  displayOpenRuleChainTooltip($event: MouseEvent, node: FcRuleNode) {
    if ($event) {
      $event.stopPropagation();
    }
    this.userNodeCallbacks.mouseLeave($event, node);
    const tooltipContent = '<div class="tb-rule-node-tooltip">' +
      '<div id="tb-node-content">' +
      '<div class="tb-node-description">' + this.translate.instant('rulechain.open-rulechain') + '</div></div></div>';
    const element = $($event.target);
    element.tooltipster(
      {
        theme: 'tooltipster-shadow',
        delay: 100,
        trigger: 'custom',
        triggerOpen: {
          click: false,
          tap: false
        },
        triggerClose: {
          click: true,
          tap: true,
          scroll: true,
          mouseleave: true
        },
        side: 'top',
        distance: 12,
        trackOrigin: true
      }
    );
    const tooltip = element.tooltipster('instance');
    const contentElement = $(tooltipContent);
    tooltip.content(contentElement);
    tooltip.open();
  }
}
