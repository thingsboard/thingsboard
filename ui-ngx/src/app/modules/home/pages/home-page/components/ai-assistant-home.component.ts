// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  HostListener,
  inject,
  Renderer2,
  signal,
  untracked,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentUserSettings } from '@core/auth/auth.selectors';
import { ActionPreferencesUpdateAiTour } from '@core/auth/auth.actions';
import { AiTourStep } from '@shared/models/user-settings.models';
import { TourService } from '@core/services/tour.service';
import { AiChatsService } from '@core/services/ai-chats.service';
import { AiSolutionsService } from '@core/services/ai-solutions.service';
import { escapeTourText, TourPopupRef, TourPopupService } from '@shared/components/tour/tour-popup.service';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { AiPromptMessage } from '@shared/models/ai-chat.models';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { AiPromptInputComponent } from '@home/components/ai/ai-prompt-input.component';

const OFFER_DELAY_MS = 900;

@Component({
  selector: 'tb-ai-assistant-home',
  template: `
    <div class="tb-ai-compose-row" [class.open]="composerFocused() || !!composerRef?.hasText()">
      <tb-ai-prompt-input #composerRef class="tb-ai-home-composer" [slim]="true"
                          (focusin)="composerFocused.set(true)" (focusout)="composerFocused.set(false)"
                          [placeholder]="'home.ai.placeholder' | translate"
                          (messageSent)="send($event)"></tb-ai-prompt-input>
      @if (hasChats()) {
        <a class="tb-ai-chats-btn" routerLink="/ai-assistant">
          <mat-icon aria-hidden="true">forum</mat-icon>
          <span class="tb-ai-chats-t">{{ 'home.ai.chats' | translate }}</span>
          <mat-icon class="tb-ai-chats-go" aria-hidden="true">chevron_right</mat-icon>
        </a>
      }
    </div>
  `,
  styleUrls: ['./ai-assistant-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiAssistantHomeComponent implements AfterViewInit {

  @ViewChild(AiPromptInputComponent) composer: AiPromptInputComponent;

  @ViewChild(AiPromptInputComponent, {read: ElementRef}) composerHost: ElementRef<HTMLElement>;

  readonly composerFocused = signal(false);

  private router = inject(Router);
  private panelService = inject(AiAssistantPanelService);
  private dialog = inject(MatDialog);
  private translate = inject(TranslateService);
  private store = inject(Store<AppState>);
  private tourService = inject(TourService);
  private chats = inject(AiChatsService);
  private solutions = inject(AiSolutionsService);

  readonly hasChats = computed(() => !this.chats.loaded() || !this.solutions.loaded()
    || this.chats.chats().length > 0 || this.solutions.solutions().length > 0);
  private tourPopup = inject(TourPopupService);
  private renderer = inject(Renderer2);
  private viewContainerRef = inject(ViewContainerRef);

  private offer: TourPopupRef | null = null;
  private offerTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.chats.ensureLoaded();
    this.solutions.ensureLoaded();
    inject(DestroyRef).onDestroy(() => {
      clearTimeout(this.offerTimer);
      this.offer?.close();
    });
  }

  ngAfterViewInit(): void {
    if (this.tourService.aiAvailable() && this.seen(AiTourStep.WELCOME) && !this.seen(AiTourStep.HOME)) {
      this.chats.watchStatus();
      this.offerTimer = setTimeout(() => this.showOffer(), OFFER_DELAY_MS);
    }
  }

  private showOffer(): void {
    this.offerTimer = null;
    const host = this.composerHost?.nativeElement;
    const target = host?.querySelector('.tb-ai-composer') ?? host;
    if (!target || this.seen(AiTourStep.HOME) || this.tourService.active() || this.dialog.openDialogs.length
      || untracked(this.chats.statusChats).some(chat => this.chats.levelOf(chat) === 'running')) {
      return;
    }
    const keyboard = matchMedia('(hover: hover) and (pointer: fine)').matches;
    this.offer = this.tourPopup.show({
      target,
      hostView: this.viewContainerRef,
      renderer: this.renderer,
      placement: ['bottom', 'top'],
      hint: true,
      title: this.translate.instant('home.ai.offer-title'),
      text: escapeTourText(this.translate.instant('home.ai.offer-text')),
      foot: keyboard ? `<kbd>/</kbd> ${escapeTourText(this.translate.instant('home.ai.offer-key'))}` : null,
      nextLabel: this.translate.instant('home.ai.offer-action'),
      onNext: () => this.closeOffer(true),
      onClose: () => this.closeOffer(false),
      onEscape: () => this.closeOffer(false),
      onHidden: () => this.offer = null
    });
  }

  private closeOffer(ask: boolean): void {
    const offer = this.offer;
    this.offer = null;
    offer?.close();
    if (!this.seen(AiTourStep.HOME)) {
      this.store.dispatch(new ActionPreferencesUpdateAiTour({step: AiTourStep.HOME, seen: true}));
    }
    if (ask) {
      this.composer?.focus();
    }
  }

  private seen(step: AiTourStep): boolean {
    return !!getCurrentUserSettings(this.store)?.aiTour?.[step];
  }

  send(prompt: AiPromptMessage): void {
    this.panelService.handOff(prompt);
    this.router.navigateByUrl('/ai-assistant');
  }

  @HostListener('document:keydown', ['$event'])
  onSlash(event: KeyboardEvent): void {
    if (event.key !== '/' || event.metaKey || event.ctrlKey || event.altKey || event.defaultPrevented || this.dialog.openDialogs.length
      || (event.target as HTMLElement)?.closest?.('input, textarea, select, [contenteditable]')) {
      return;
    }
    event.preventDefault();
    this.composer?.focus();
  }
}
