// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  HostBinding,
  inject,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild
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

  @Input() answered = false;

  @ViewChild('denyInput') denyInput: ElementRef<HTMLInputElement>;

  denying = false;

  @Output() approvalAction = new EventEmitter<ApprovalResult>();

  @HostBinding('class') state: ApprovalCardState = 'pending';

  private readonly cd = inject(ChangeDetectorRef);

  ngOnChanges(changes: SimpleChanges): void {
    if (this.autoApprove && this.state === 'pending' && !this.completedExecution) {
      this.approve();
    }
    if (this.answered && this.state === 'pending' && !this.completedExecution) {
      this.denying = false;
      this.state = 'executing';
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
    if (this.denying) {
      this.sendDeny(this.denyInput?.nativeElement.value ?? '');
      return;
    }
    this.denying = true;
    setTimeout(() => this.denyInput?.nativeElement.focus());
  }

  sendDeny(words: string): void {
    const reason = words.trim();
    this.denying = false;
    this.state = 'executing';
    this.approvalAction.emit(reason
      ? {executionId: this.data.executionId, approved: false, reason}
      : {executionId: this.data.executionId, approved: false});
  }

  closeDeny(): void {
    this.denying = false;
  }

  private emitApproval(autoApprove = false): void {
    this.state = 'executing';
    this.approvalAction.emit({executionId: this.data.executionId, approved: true, autoApprove});
  }
}
