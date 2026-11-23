// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ApplicationRef, ComponentRef, createComponent, EnvironmentInjector, inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { ActionPreferencesUpdateAiTour } from '@core/auth/auth.actions';
import { AiTourStep } from '@shared/models/user-settings.models';
import { TourLauncherApi, TourService } from '@core/services/tour.service';
import { PopoverPreferredPlacement } from '@shared/components/popover.models';
import { escapeTourText, TourPopupRef, TourPopupService } from '@shared/components/tour/tour-popup.service';
import { TourHostComponent } from '@modules/tour/tour-host.component';
import {
  TourPerson,
  WelcomeInvitationComponent,
  WelcomeInvitationData,
  WelcomeInvitationResult
} from '@modules/tour/welcome-invitation.component';

type TourSide = 'right' | 'above';

interface TourStop {
  target: () => HTMLElement | null;
  side: TourSide;
  title: string;
  text: () => string;
  pad?: number;
  radius?: number;
  interactive?: boolean;
  enter?: () => Promise<void>;
}

const AI_PAGE = '/ai-assistant';
const NEW_USER_MS = 24 * 60 * 60 * 1000;
const PLACEMENTS: Record<TourSide, PopoverPreferredPlacement> = {
  right: ['right', 'rightTop', 'rightBottom', 'bottom'],
  above: ['top', 'topLeft', 'topRight']
};

const delay = (ms: number) => new Promise<void>(resolve => setTimeout(resolve, ms));

const visible = (el: HTMLElement | null, minWidth = 1): HTMLElement | null => {
  const rect = el?.getBoundingClientRect();
  return rect && rect.width >= minWidth && rect.height > 0 && rect.right > 0 ? el : null;
};

@Injectable()
export class TourLauncher implements TourLauncherApi {

  private dialog = inject(MatDialog);
  private store = inject(Store<AppState>);
  private router = inject(Router);
  private translate = inject(TranslateService);
  private tourService = inject(TourService);
  private tourPopup = inject(TourPopupService);
  private appRef = inject(ApplicationRef);
  private environmentInjector = inject(EnvironmentInjector);

  welcome(): void {
    if (this.tourService.active()) {
      return;
    }
    this.tourService.active.set(true);
    this.dialog.open<WelcomeInvitationComponent, WelcomeInvitationData, WelcomeInvitationResult>(WelcomeInvitationComponent, {
      panelClass: 'tb-invite-panel',
      backdropClass: 'tb-invite-backdrop',
      autoFocus: false,
      restoreFocus: false,
      maxWidth: '100vw',
      data: {person: this.person()}
    }).afterClosed().subscribe(result => {
      this.markSeen(AiTourStep.WELCOME);
      this.tourService.active.set(false);
      if (result === 'tour') {
        this.assistantTour();
      }
    });
  }

  async assistantTour(): Promise<void> {
    if (this.tourService.active()) {
      return;
    }
    this.tourService.active.set(true);
    this.markSeen(AiTourStep.ASSISTANT_TOUR);
    const tree = this.router.parseUrl(this.router.url);
    const path = this.router.url.split(/[?#]/)[0];
    if (path !== AI_PAGE || tree.queryParamMap.has('chatId') || tree.queryParamMap.has('solutionId')) {
      await this.router.navigateByUrl(AI_PAGE);
    }
    await this.until(() => this.composer(), 6000);
    const before = this.person() === 'before';
    const t = (key: string) => this.translate.instant('tour.assistant.' + key);
    this.play([
      {
        target: () => visible(document.querySelector<HTMLElement>(`tb-side-menu a[href="${AI_PAGE}"]`)),
        side: 'right', pad: 2, radius: 8,
        title: t('menu-title'),
        text: () => t('menu-text')
      },
      {
        target: () => this.chatsList() ?? this.chatsButton(),
        side: 'right', radius: 10,
        title: t('chats-title'),
        text: () => t(this.chatsList() ? 'chats-text' : 'chats-folded-text')
      },
      {
        target: () => this.newSolution(),
        side: 'right', pad: 2, radius: 8,
        title: t(before ? 'solutions-title-before' : 'solutions-title-new'),
        text: () => t(before ? 'solutions-text-before' : 'solutions-text-new')
      },
      {
        target: () => this.composer(),
        side: 'above', radius: 20, interactive: true,
        title: t('start-title'),
        text: () => t('start-text'),
        enter: async () => this.openGetStarted()
      }
    ]);
  }

  private play(stops: TourStop[]): void {
    const host = createComponent(TourHostComponent, {environmentInjector: this.environmentInjector});
    this.appRef.attachView(host.hostView);
    document.body.append(host.location.nativeElement);
    let index = -1;
    let over = false;
    let popup: TourPopupRef | null = null;
    let cleanup: (() => void) | null = null;
    const clear = () => {
      cleanup?.();
      cleanup = null;
      popup?.close();
      popup = null;
      host.instance.spot.set(null);
    };
    const end = (done: boolean) => {
      if (over) {
        return;
      }
      over = true;
      clear();
      document.removeEventListener('keydown', onKey, true);
      this.destroy(host);
      this.tourService.active.set(false);
      if (done) {
        this.composer()?.querySelector('textarea')?.focus();
      }
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        event.stopPropagation();
        end(false);
      }
    };
    document.addEventListener('keydown', onKey, true);
    const go = async (to: number) => {
      clear();
      let n = to;
      while (n < stops.length && !stops[n].target()) {
        n++;
      }
      if (n >= stops.length) {
        end(true);
        return;
      }
      index = n;
      const stop = stops[index];
      await stop.enter?.();
      const target = stop.target();
      if (over || !target) {
        return;
      }
      const live = stops.filter(s => !!s.target());
      const at = Math.max(0, live.indexOf(stop));
      const last = at === live.length - 1;
      cleanup = this.spotlight(host, target, stop, () => end(true));
      popup = this.tourPopup.show({
        target,
        hostView: host.instance.viewContainerRef,
        renderer: host.instance.renderer,
        placement: PLACEMENTS[stop.side],
        title: stop.title,
        text: escapeTourText(stop.text()),
        count: live.length > 1 ? this.translate.instant('tour.assistant.count', {step: at + 1, count: live.length}) : null,
        skipLabel: last ? null : this.translate.instant('tour.assistant.skip'),
        nextLabel: this.translate.instant(last ? 'tour.assistant.done' : 'tour.assistant.next'),
        onNext: () => go(index + 1),
        onSkip: () => end(false),
        onClose: () => end(false)
      });
      setTimeout(() => document.querySelector<HTMLElement>('.tb-tour-popover .tb-tour-popup-go')?.focus({preventScroll: true}), 60);
    };
    go(0);
  }

  private spotlight(host: ComponentRef<TourHostComponent>, target: HTMLElement, stop: TourStop, onUse: () => void): () => void {
    const pad = stop.pad ?? 6;
    let frame = 0;
    const place = () => {
      if (!target.isConnected) {
        return;
      }
      const rect = target.getBoundingClientRect();
      const own = parseFloat(getComputedStyle(target).borderTopLeftRadius) || 0;
      host.instance.spot.set({
        left: rect.left - pad,
        top: rect.top - pad,
        width: rect.width + pad * 2,
        height: rect.height + pad * 2,
        radius: own ? own + pad : stop.radius ?? 12,
        through: !!stop.interactive
      });
      this.appRef.tick();
    };
    const again = () => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(place);
    };
    place();
    window.addEventListener('resize', again);
    window.addEventListener('scroll', again, true);
    const observer = new ResizeObserver(again);
    observer.observe(target);
    const field = stop.interactive ? target.querySelector('textarea') : null;
    const onClick = () => onUse();
    const onSend = (event: KeyboardEvent) => {
      if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
        setTimeout(onUse, 0);
      }
    };
    if (stop.interactive) {
      target.addEventListener('click', onClick, true);
      field?.addEventListener('keydown', onSend);
    }
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener('resize', again);
      window.removeEventListener('scroll', again, true);
      observer.disconnect();
      target.removeEventListener('click', onClick, true);
      field?.removeEventListener('keydown', onSend);
    };
  }

  private destroy(host: ComponentRef<TourHostComponent>): void {
    this.appRef.detachView(host.hostView);
    host.destroy();
  }

  private chatsList(): HTMLElement | null {
    return visible(document.querySelector<HTMLElement>('.ai-rail .ai-list'), 120);
  }

  private chatsButton(): HTMLElement | null {
    return Array.from(document.querySelectorAll<HTMLElement>('.ai-bar-chats, .ai-strip .ai-ib')).map(el => visible(el)).find(el => !!el) ?? null;
  }

  private newSolution(): HTMLElement | null {
    const label = this.translate.instant('ai-assistant.new-solution');
    const nav = Array.from(document.querySelectorAll<HTMLElement>('.ai-rail .ai-nav')).find(el => el.textContent.includes(label));
    return visible(nav ?? null, 40)
      ?? visible(document.querySelector<HTMLElement>(`.ai-strip .ai-ib[aria-label="${label}"]`));
  }

  private composer(): HTMLElement | null {
    return visible(document.querySelector<HTMLElement>('tb-ai-chat.ai-canvas .tb-ai-composer'));
  }

  private async openGetStarted(): Promise<void> {
    if (this.router.parseUrl(this.router.url).queryParamMap.get('getStarted') !== 'true') {
      await this.router.navigateByUrl(AI_PAGE + '?getStarted=true');
    }
    await this.until(() => this.composer() && document.querySelector('tb-ai-chat.ai-canvas .k-pcards'), 4000);
  }

  private async until(found: () => unknown, timeoutMs: number): Promise<void> {
    for (let waited = 0; !found() && waited < timeoutMs; waited += 100) {
      await delay(100);
    }
  }

  private person(): TourPerson {
    const created = getCurrentAuthState(this.store)?.userDetails?.createdTime;
    return created && Date.now() - created < NEW_USER_MS ? 'new' : 'before';
  }

  private markSeen(step: AiTourStep): void {
    this.store.dispatch(new ActionPreferencesUpdateAiTour({step, seen: true}));
  }
}
