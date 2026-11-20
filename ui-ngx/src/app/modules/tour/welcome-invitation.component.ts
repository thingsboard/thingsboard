// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  HostListener,
  inject,
  signal,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { buildScene, CoverWords, SceneContext, WELCOME_CARDS } from '@modules/tour/welcome-scenes';

export type TourPerson = 'before' | 'new';

export interface WelcomeInvitationData {
  person: TourPerson;
}

export type WelcomeInvitationResult = 'tour' | 'later';

@Component({
  selector: 'tb-welcome-invitation',
  templateUrl: './welcome-invitation.component.html',
  styleUrls: ['./welcome-invitation.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WelcomeInvitationComponent implements AfterViewInit {

  @ViewChild('scenes', {static: true}) scenesRef: ElementRef<HTMLElement>;

  @ViewChild('go', {read: ElementRef}) goRef: ElementRef<HTMLElement>;

  private data = inject<WelcomeInvitationData>(MAT_DIALOG_DATA);

  private dialogRef = inject<MatDialogRef<WelcomeInvitationComponent, WelcomeInvitationResult>>(MatDialogRef);

  private translate = inject(TranslateService);

  readonly cards = WELCOME_CARDS;

  readonly cardNumbers = Array.from({length: WELCOME_CARDS}, (_, i) => i + 1);

  readonly person = this.data.person;

  readonly step = signal(0);

  readonly steps = computed(() => [this.step()]);

  private frames: HTMLElement[] = [];

  private readonly context: SceneContext = {
    t: (key, params) => this.translate.instant(key, params),
    locale: (this.translate.currentLang || 'en-US').replace('_', '-'),
    still: typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches
  };

  private readonly coverWords: CoverWords = {
    pill: this.translate.instant(`tour.invite.pill-${this.person}`),
    news: this.translate.instant('tour.invite.news'),
    title: this.translate.instant('tour.invite.cover-title')
  };

  ngAfterViewInit(): void {
    const host = this.scenesRef.nativeElement;
    for (let n = 0; n <= WELCOME_CARDS; n++) {
      const frame = document.createElement('div');
      frame.className = 'iv-canvas iv-scene';
      frame.setAttribute('aria-hidden', 'true');
      host.append(frame);
      this.frames.push(frame);
    }
    this.show(0);
  }

  show(k: number): void {
    if (k < 0 || k > WELCOME_CARDS) {
      return;
    }
    this.step.set(k);
    this.frames.forEach((frame, n) => {
      if (n === k && !frame.classList.contains('on')) {
        const fresh = buildScene(n, this.context, this.coverWords);
        frame.replaceWith(fresh);
        this.frames[n] = fresh;
        fresh.classList.add('on');
      } else if (n !== k) {
        frame.classList.remove('on');
      }
    });
    setTimeout(() => this.goRef?.nativeElement.focus({preventScroll: true}), 30);
  }

  next(): void {
    if (this.step() === WELCOME_CARDS) {
      this.dialogRef.close('tour');
    } else {
      this.show(this.step() + 1);
    }
  }

  back(): void {
    this.show(this.step() - 1);
  }

  later(): void {
    this.dialogRef.close('later');
  }

  @HostListener('document:keydown', ['$event'])
  onKey(event: KeyboardEvent): void {
    if (event.key === 'ArrowRight') {
      this.show(this.step() + 1);
    } else if (event.key === 'ArrowLeft') {
      this.show(this.step() - 1);
    }
  }
}
