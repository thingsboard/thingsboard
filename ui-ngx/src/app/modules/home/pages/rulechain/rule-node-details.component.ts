///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, Input, OnChanges, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FcRuleNode, RuleNodeType } from '@shared/models/rule-node.models';
import { EntityType } from '@shared/models/entity-type.models';
import { Subscription } from 'rxjs';
import { RuleChainService } from '@core/http/rule-chain.service';
import { RuleNodeConfigComponent } from './rule-node-config.component';
import { Router } from '@angular/router';
import { RuleChainType } from '@app/shared/models/rule-chain.models';

@Component({
  selector: 'tb-rule-node',
  templateUrl: './rule-node-details.component.html',
  styleUrls: ['./rule-node-details.component.scss']
})
export class RuleNodeDetailsComponent extends PageComponent implements OnInit, OnChanges {

  @ViewChild('ruleNodeConfigComponent') ruleNodeConfigComponent: RuleNodeConfigComponent;

  @Input()
  ruleNode: FcRuleNode;

  @Input()
  ruleChainId: string;

  @Input()
  ruleChainType: RuleChainType;

  @Input()
  isEdit: boolean;

  @Input()
  isReadOnly: boolean;

  @Input()
  isAdd = false;

  ruleNodeType = RuleNodeType;
  entityType = EntityType;

  ruleNodeFormGroup: FormGroup;

  private ruleNodeFormSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder,
              private ruleChainService: RuleChainService,
              private router: Router) {
    super(store);
    this.ruleNodeFormGroup = this.fb.group({});
  }

  private buildForm() {
    if (this.ruleNodeFormSubscription) {
      this.ruleNodeFormSubscription.unsubscribe();
      this.ruleNodeFormSubscription = null;
    }
    if (this.ruleNode) {
      if (this.ruleNode.component.type !== RuleNodeType.RULE_CHAIN) {

        this.ruleNodeFormGroup = this.fb.group({
          name: [this.ruleNode.name, [Validators.required, Validators.pattern('(.|\\s)*\\S(.|\\s)*')]],
          debugMode: [this.ruleNode.debugMode, []],
          configuration: [this.ruleNode.configuration, [Validators.required]],
          additionalInfo: this.fb.group(
            {
              description: [this.ruleNode.additionalInfo ? this.ruleNode.additionalInfo.description : ''],
            }
          )
        });
      } else {
        this.ruleNodeFormGroup = this.fb.group({
          targetRuleChainId: [this.ruleNode.targetRuleChainId, [Validators.required]],
          additionalInfo: this.fb.group(
            {
              description: [this.ruleNode.additionalInfo ? this.ruleNode.additionalInfo.description : ''],
            }
          )
        });
      }
      this.ruleNodeFormSubscription = this.ruleNodeFormGroup.valueChanges.subscribe(() => {
        this.updateRuleNode();
      });
    } else {
      this.ruleNodeFormGroup = this.fb.group({});
    }
  }

  private updateRuleNode() {
    const formValue = this.ruleNodeFormGroup.value || {};

    if (this.ruleNode.component.type === RuleNodeType.RULE_CHAIN) {
      const targetRuleChainId: string = formValue.targetRuleChainId;
      if (this.ruleNode.targetRuleChainId !== targetRuleChainId && targetRuleChainId) {
        this.ruleChainService.getRuleChain(targetRuleChainId).subscribe(
          (ruleChain) => {
            this.ruleNode.name = ruleChain.name;
            Object.assign(this.ruleNode, formValue);
          }
        );
      } else {
        Object.assign(this.ruleNode, formValue);
      }
    } else {
      formValue.name = formValue.name.trim();
      Object.assign(this.ruleNode, formValue);
    }
  }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (change.currentValue !== change.previousValue) {
        if (propName === 'ruleNode') {
          this.buildForm();
        }
      }
    }
  }

  validate() {
    if (this.ruleNode.component.type !== RuleNodeType.RULE_CHAIN) {
      this.ruleNodeConfigComponent.validate();
    }
  }

  openRuleChain($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.ruleNode.targetRuleChainId) {
      if (this.ruleChainType === RuleChainType.EDGE) {
        this.router.navigateByUrl(`/edges/ruleChains/${this.ruleNode.targetRuleChainId}`);
      } else {
        this.router.navigateByUrl(`/ruleChains/${this.ruleNode.targetRuleChainId}`);
      }
    }
  }
}
