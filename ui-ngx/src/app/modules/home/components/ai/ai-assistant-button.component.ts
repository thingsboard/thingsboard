// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  HostListener,
  inject,
  Renderer2,
  signal,
  untracked,
  viewChild,
  ViewContainerRef
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { AiChatsService } from '@core/services/ai-chats.service';
import { ChatLevel } from '@shared/models/ai-chat.models';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState, selectAiTourStepSeen } from '@core/auth/auth.selectors';
import { ActionPreferencesUpdateAiTour } from '@core/auth/auth.actions';
import { AiTourStep } from '@shared/models/user-settings.models';
import { Authority } from '@shared/models/authority.enum';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { escapeTourText, TourPopupRef, TourPopupService } from '@shared/components/tour/tour-popup.service';
import { TourService } from '@core/services/tour.service';
import { AiAssistantContextService } from '@home/components/ai/ai-assistant-context.service';
import { AiMarkCue } from '@home/components/ai/ai-mark.component';

const INTRO_DELAY_MS = 900;

const intro: { owner: AiAssistantButtonComponent | null } = {owner: null};

@Component({
  selector: 'tb-ai-assistant-button',
  template: `
    @if (available && !panelService.open() && !panelService.suppressed()) {
      @let status = cue();
      <button #aiButton tb-ai-mark class="tb-ai-assistant-button" [cue]="markCue()"
              [working]="status?.level === 'running'" [glow]="introOpen()" [pressed]="panelService.open()"
              tb-popover [tbAiTip]="buttonTip" [tbPopoverTrigger]="introOpen() ? null : 'hover'"
              [attr.aria-label]="status ? ('ai-assistant.ai-assistant-cue' | translate: {cue: status.tip}) : ('ai-assistant.ai-assistant' | translate)"
              aria-keyshortcuts="/"
              (click)="toggle($event)">
      </button>
      <ng-template #buttonTip>
        <div class="tb-ai-tip-body tip-mini">
          <div class="tip-keyrow">
            <span>{{ status ? ('ai-assistant.ai-assistant-cue' | translate: {cue: status.tip}) : ('ai-assistant.ai-assistant' | translate) }}</span>
            <span class="tip-caps"><kbd>/</kbd></span>
          </div>
        </div>
      </ng-template>
    }
  `,
  styles: [`
    :host {
      display: contents;
    }
  `],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiAssistantButtonComponent {

  panelService = inject(AiAssistantPanelService);

  private store = inject(Store<AppState>);

  private readonly authState = getCurrentAuthState(this.store);

  readonly available = this.authState.aiEnabled
    && this.authState.authUser?.authority === Authority.TENANT_ADMIN
    && inject(UserPermissionsService).hasGenericPermission(Resource.AI, Operation.ALL);

  private dialog = inject(MatDialog);

  private chats = inject(AiChatsService);

  private translate = inject(TranslateService);

  private context = inject(AiAssistantContextService);

  private tourPopup = inject(TourPopupService);

  private tourService = inject(TourService);

  private renderer = inject(Renderer2);

  private viewContainerRef = inject(ViewContainerRef);

  private buttonRef = viewChild('aiButton', {read: ElementRef<HTMLElement>});

  private introSeen = toSignal(this.store.select(selectAiTourStepSeen(AiTourStep.AI_BUTTON)), {initialValue: true});

  private welcomeSeen = toSignal(this.store.select(selectAiTourStepSeen(AiTourStep.WELCOME)), {initialValue: false});

  readonly introOpen = signal(false);

  private introPopup: TourPopupRef | null = null;

  private introTimer: ReturnType<typeof setTimeout> | null = null;

  readonly cue = computed<{level: ChatLevel; tip: string} | null>(() => {
    const chats = this.chats.statusChats();
    const titleOf = (title: string) => title || this.translate.instant('ai-assistant.new-chat');
    const attention = chats.filter(chat => this.chats.needsAttention(chat));
    if (attention.length) {
      const level: AiMarkCue = attention.some(chat => this.chats.levelOf(chat) === 'needs') ? 'needs' : 'error';
      return {
        level,
        tip: attention.length === 1
          ? this.translate.instant('ai-assistant.waiting-one-tip', {title: titleOf(attention[0].title), status: this.chats.tipOf(attention[0])})
          : this.translate.instant('ai-assistant.waiting-many', {count: attention.length})
      };
    }
    const running = chats.filter(chat => this.chats.levelOf(chat) === 'running');
    if (running.length) {
      return {
        level: 'running',
        tip: running.length === 1
          ? this.translate.instant('ai-assistant.running-one-tip', {title: titleOf(running[0].title)})
          : this.translate.instant('ai-assistant.running-many', {count: running.length})
      };
    }
    return null;
  });

  readonly markCue = computed<AiMarkCue | null>(() => {
    const level = this.cue()?.level;
    return level === 'needs' || level === 'error' ? level : null;
  });

  private introWanted = computed(() => this.available && this.welcomeSeen() && !this.tourService.active() && !this.introSeen() && this.cue()?.level !== 'running' && !this.panelService.open()
    && !this.panelService.suppressed() && !!this.buttonRef());

  private introEffect = effect(() => {
    const wanted = this.introWanted();
    untracked(() => {
      this.clearIntroTimer();
      if (wanted && !this.introOpen()) {
        this.introTimer = setTimeout(() => this.showIntro(), INTRO_DELAY_MS);
      } else if (!wanted && this.introOpen()) {
        this.hideIntro();
      }
    });
  });

  constructor() {
    if (this.available) {
      this.chats.watchStatus();
    }
    inject(Router).events.pipe(
      filter(event => event instanceof NavigationStart || event instanceof NavigationEnd),
      takeUntilDestroyed()
    ).subscribe(event => {
      this.clearIntroTimer();
      if (event instanceof NavigationStart) {
        this.hideIntro();
      } else if (this.introWanted()) {
        this.introTimer = setTimeout(() => this.showIntro(), INTRO_DELAY_MS);
      }
    });
    inject(DestroyRef).onDestroy(() => {
      this.clearIntroTimer();
      this.hideIntro();
    });
  }

  @HostListener('document:keydown', ['$event'])
  onSlash(event: KeyboardEvent): void {
    if (event.key !== '/' || event.metaKey || event.ctrlKey || event.altKey || event.defaultPrevented || !this.available
      || this.panelService.open() || this.panelService.suppressed() || this.dialog.openDialogs.length
      || (event.target as HTMLElement)?.closest?.('input, textarea, select, [contenteditable]')
      || Array.from(document.querySelectorAll<HTMLElement>('.tb-ai-composer textarea')).some(field => field.offsetParent && !field.closest('tb-ai-assistant-panel:not(.open)'))) {
      return;
    }
    event.preventDefault();
    this.openChat();
  }

  toggle($event: Event): void {
    $event.stopPropagation();
    this.openChat();
  }

  private openChat(): void {
    if (this.introOpen()) {
      this.closeIntro();
    }
    this.panelService.openPanel();
  }

  private showIntro(): void {
    this.introTimer = null;
    const trigger: HTMLElement | undefined = this.buttonRef()?.nativeElement;
    if (!this.introWanted() || !trigger || intro.owner || this.dialog.openDialogs.length || !this.visible(trigger)) {
      return;
    }
    intro.owner = this;
    const welcome = this.context.welcome();
    const example = welcome.examples[0]?.label?.trim();
    const lead = this.translate.instant(`ai-assistant.intro.lead-${welcome.place}${example ? '-example' : ''}`, {example});
    const keys = matchMedia('(hover: hover) and (pointer: fine)').matches ? ' ' + this.translate.instant('ai-assistant.intro.keys') : '';
    this.introOpen.set(true);
    this.introPopup = this.tourPopup.show({
      target: trigger,
      hostView: this.viewContainerRef,
      renderer: this.renderer,
      placement: ['bottomRight', 'bottom', 'bottomLeft'],
      title: welcome.title || this.translate.instant('ai-assistant.panel-title'),
      text: escapeTourText(lead) + keys,
      skipLabel: this.translate.instant('ai-assistant.intro.got-it'),
      nextLabel: this.translate.instant('ai-assistant.intro.open-chat'),
      onSkip: () => this.closeIntro(),
      onClose: () => this.closeIntro(),
      onNext: () => this.openChat(),
      onHidden: () => {
        if (this.introOpen()) {
          this.introPopup = null;
          this.hideIntro();
        }
      }
    });
  }

  private closeIntro(): void {
    this.hideIntro();
    if (!this.introSeen()) {
      this.store.dispatch(new ActionPreferencesUpdateAiTour({step: AiTourStep.AI_BUTTON, seen: true}));
    }
  }

  private hideIntro(): void {
    if (intro.owner === this) {
      intro.owner = null;
    }
    this.introOpen.set(false);
    const popup = this.introPopup;
    this.introPopup = null;
    popup?.close();
  }

  private clearIntroTimer(): void {
    if (this.introTimer) {
      clearTimeout(this.introTimer);
      this.introTimer = null;
    }
  }

  private visible(element: HTMLElement): boolean {
    const rect = element.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0 && (element.checkVisibility?.({visibilityProperty: true}) ?? element.offsetParent !== null);
  }
}
