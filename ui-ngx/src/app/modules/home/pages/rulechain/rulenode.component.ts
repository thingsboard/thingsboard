///
/// Copyright © 2016-2023 The Thingsboard Authors
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
import { ActivatedRoute, Router } from '@angular/router';
import { RuleChainType } from '@app/shared/models/rule-chain.models';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from "rxjs/internal/Observable";
import { RuleChainService } from "@core/http/rule-chain.service";
import { MatDialog } from "@angular/material/dialog";
import { map, mergeMap } from "rxjs/operators";
import {
  ClearRuleNodeErrorsDialogComponent,
  ClearRuleNodeErrorsDialogData
} from "@home/pages/rulechain/clear-rule-node-errors-dialog.component";
import { of } from "rxjs";

@Component({
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: 'rule-node',
  templateUrl: './rulenode.component.html',
  styleUrls: ['./rulenode.component.scss']
})
export class RuleNodeComponent extends FcNodeComponent implements OnInit {

  iconUrl: SafeResourceUrl;
  RuleNodeType = RuleNodeType;
  showErrorsStatus: Observable<boolean>;

  constructor(private sanitizer: DomSanitizer,
              private translate: TranslateService,
              private router: Router,
              private ruleChainService: RuleChainService,
              private dialog: MatDialog,
              private route: ActivatedRoute) {
    super();
  }

  ngOnInit(): void {
    super.ngOnInit();
    if (this.node.iconUrl) {
      this.iconUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.node.iconUrl);
    }
    this.showErrorsStatus = this.route.data.pipe(map((data) => data.user?.additionalInfo?.showErrorsStatus || true));
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

  openClearErrorStatsDialog($event: Event, node: FcRuleNode) {
    if ($event) {
      $event.stopPropagation();
    }
    if (!this.modelservice.isEditable() || this.node.readonly) {
      return;
    }
    this.dialog.open<ClearRuleNodeErrorsDialogComponent, ClearRuleNodeErrorsDialogData,
      boolean>(ClearRuleNodeErrorsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog'],
      data: {
        node: node
      }
    }).afterClosed().pipe(
      mergeMap((confirmResult) => {
        return confirmResult ? this.ruleChainService.clearRuleNodeStats(node.ruleNodeId.id) : of(confirmResult);
      })
    ).subscribe((result) => {
      if (result !== false) {
        node.stats = {
          errorsCount: 0,
          lastErrorMsg: null,
          msgData: null,
          msgMetadata: null
        };
      }
    });
  }

  isRuleNodeHasErrors(node: FcRuleNode) {
    return node?.stats ? !!node.stats.errorsCount : false;
  }

  nodeSelected(node: FcRuleNode) {
    return !this.modelservice.selectedObjects.find(el => el.id === node.id);
  }
}
