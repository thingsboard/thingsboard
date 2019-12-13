import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { ISearchableComponent } from '@home/models/searchable-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, NgForm } from '@angular/forms';
import { FcRuleNode } from './rulechain-page.models';
import { RuleNodeType } from '@shared/models/rule-node.models';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-rule-node',
  templateUrl: './rule-node-details.component.html',
  styleUrls: []
})
export class RuleNodeDetailsComponent extends PageComponent implements OnInit {

  @ViewChild('ruleNodeForm', {static: true}) ruleNodeForm: NgForm;

  ruleNodeValue: FcRuleNode;

  @Input()
  set ruleNode(ruleNode: FcRuleNode) {
    this.ruleNodeValue = ruleNode;
  }

  @Input()
  ruleChainId: string;

  @Input()
  isEdit: boolean;

  @Input()
  isReadOnly: boolean;

  @Output()
  deleteRuleNode = new EventEmitter();

  ruleNodeType = RuleNodeType;
  entityType = EntityType;

  ruleNodeFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
    this.ruleNodeFormGroup = this.fb.group({});
  }

  private buildForm() {
  }

  ngOnInit(): void {
  }

  onDeleteRuleNode(event) {
    this.deleteRuleNode.emit();
  }

}
