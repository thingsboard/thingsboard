// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, computed, effect, ElementRef, EventEmitter, inject, input, Output, signal } from '@angular/core';
import { decorateReferences } from '@home/components/ai/ai-references';
import { AiPlanProgress, AiPlanStep } from '@home/components/ai/ai-action.models';

export interface AiPlanRun {
  auto: boolean;
  skipped: number[];
}

type AiPlanMark = 'todo' | 'now' | 'done' | 'off';

@Component({
  selector: 'tb-ai-plan',
  templateUrl: './ai-plan.component.html',
  standalone: false
})
export class AiPlanComponent {

  steps = input.required<AiPlanStep[]>();
  progress = input<AiPlanProgress | null>(null);
  gate = input(false);
  autoApprove = input(false);

  @Output() run = new EventEmitter<AiPlanRun>();
  @Output() change = new EventEmitter<void>();

  private cleared = signal<ReadonlySet<number>>(new Set());

  private host = inject(ElementRef<HTMLElement>);

  private chips = effect(() => {
    const steps = this.steps();
    setTimeout(() => {
      const whats = (this.host.nativeElement as HTMLElement).querySelectorAll<HTMLElement>('.k-plan li .k-what');
      steps.forEach((step, index) => {
        if (step.makes?.length && whats[index]) {
          decorateReferences(whats[index], step.makes.map(made => ({name: made.name, entityType: made.type, id: ''})));
        }
      });
    });
  });

  marks = computed<AiPlanMark[]>(() => {
    const progress = this.progress();
    return this.steps().map((_, index) => {
      const number = index + 1;
      if (progress?.done.includes(number)) {
        return 'done';
      }
      if (progress?.skipped.includes(number)) {
        return 'off';
      }
      return progress?.now === number ? 'now' : 'todo';
    });
  });

  counted = computed(() => this.gate()
    ? this.steps().filter((step, index) => !step.optional || !this.cleared().has(index)).length
    : this.marks().filter(mark => mark !== 'off').length);

  done = computed(() => this.marks().filter(mark => mark === 'done').length);

  parts(text: string): {text: string; code: boolean}[] {
    return text.split('`').map((part, index) => ({text: part, code: index % 2 === 1})).filter(part => part.text);
  }

  checked(index: number): boolean {
    return !this.cleared().has(index);
  }

  toggle(index: number): void {
    this.cleared.update(set => {
      const next = new Set(set);
      if (next.has(index)) {
        next.delete(index);
      } else {
        next.add(index);
      }
      return next;
    });
  }

  start(auto: boolean): void {
    const skipped = this.steps().map((step, index) => step.optional && this.cleared().has(index) ? index + 1 : 0).filter(Boolean);
    this.run.emit({auto, skipped});
  }

}
