// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
