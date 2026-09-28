// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectorRef,
  Component,
  EventEmitter,
  HostBinding,
  inject,
  Input,
  OnChanges,
  Output,
  SimpleChanges
} from '@angular/core';
import {
  ApprovalResult,
  ToolApprovalResult,
  ToolExecutionRequested,
  ToolExecutionResult
} from '@shared/models/ai-chat.models';

export type ApprovalCardState = 'pending' | 'executing' | 'approved' | 'denied' | 'error';

@Component({
  selector: 'tb-ai-approval-card',
  templateUrl: './ai-approval-card.component.html',
  styleUrls: ['./ai-approval-card.component.scss'],
  standalone: false
})
export class AiApprovalCardComponent implements OnChanges {

  @Input() data: ToolExecutionRequested;

  @Input() completedExecution: ToolExecutionResult | null = null;

  @Input() autoApprove = false;

  @Output() approvalAction = new EventEmitter<ApprovalResult>();

  @HostBinding('class') state: ApprovalCardState = 'pending';

  private readonly cd = inject(ChangeDetectorRef);

  ngOnChanges(changes: SimpleChanges): void {
    if (this.autoApprove && this.state === 'pending' && !this.completedExecution) {
      this.approve();
    }
    if (changes.completedExecution && this.completedExecution?.executionId === this.data.executionId) {
      const result = this.completedExecution;
      if (result.approvalStatus === ToolApprovalResult.DENIED || result.approvalStatus === ToolApprovalResult.TIMEOUT) {
        this.state = 'denied';
      } else if (result.approvalStatus === ToolApprovalResult.APPROVED && result.executionStatus === 'SUCCESS') {
        this.state = 'approved';
      } else {
        this.state = 'error';
      }
      this.cd.detectChanges();
    }
  }

  get isDestructive(): boolean {
    return this.data.destructive;
  }

  get approveIcon(): string {
    return this.data.icon || (this.isDestructive ? 'delete' : 'check');
  }

  get resultIcon(): string {
    if (this.completedExecution?.icon) {
      return this.completedExecution.icon;
    }
    switch (this.state) {
      case 'approved':
        return this.approveIcon;
      case 'denied':
        return 'block';
      default:
        return 'error_outline';
    }
  }

  approve(): void {
    this.emitApproval();
  }

  approveAll(): void {
    this.emitApproval(true);
  }

  deny(): void {
    this.state = 'executing';
    this.approvalAction.emit({executionId: this.data.executionId, approved: false});
  }

  private emitApproval(autoApprove = false): void {
    this.state = 'executing';
    this.approvalAction.emit({executionId: this.data.executionId, approved: true, autoApprove});
  }
}
