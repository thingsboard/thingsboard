// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { createNgModule, inject, Injectable, Injector, signal, untracked } from '@angular/core';
import { Store } from '@ngrx/store';
import { MatDialog } from '@angular/material/dialog';
import { AppState } from '@core/core.state';
import { getCurrentAuthState, getCurrentUserSettings } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { AiTourStep } from '@shared/models/user-settings.models';
import { AiChatsService } from '@core/services/ai-chats.service';

export interface TourLauncherApi {
  welcome(): void;
  assistantTour(): Promise<void>;
}

const OFFER_DELAY_MS = 1200;

@Injectable({providedIn: 'root'})
export class TourService {

  readonly active = signal(false);

  private store = inject(Store<AppState>);
  private injector = inject(Injector);
  private userPermissions = inject(UserPermissionsService);
  private chats = inject(AiChatsService);
  private dialog = inject(MatDialog);

  private launcher: Promise<TourLauncherApi> | null = null;

  private offered = false;

  aiAvailable(): boolean {
    const authState = getCurrentAuthState(this.store);
    return !!authState?.aiEnabled && authState.authUser?.authority === Authority.TENANT_ADMIN
      && this.userPermissions.hasGenericPermission(Resource.AI, Operation.ALL);
  }

  offerWelcome(): void {
    if (this.offered || !this.aiAvailable() || this.seen(AiTourStep.WELCOME)) {
      return;
    }
    this.offered = true;
    this.chats.watchStatus();
    setTimeout(() => {
      if (this.active() || this.seen(AiTourStep.WELCOME) || this.dialog.openDialogs.length
        || untracked(this.chats.statusChats).some(chat => this.chats.levelOf(chat) === 'running')) {
        this.offered = false;
        return;
      }
      this.load().then(launcher => launcher.welcome());
    }, OFFER_DELAY_MS);
  }

  playAssistantTour(): void {
    if (this.aiAvailable() && !this.active()) {
      this.load().then(launcher => launcher.assistantTour());
    }
  }

  private seen(step: AiTourStep): boolean {
    return !!getCurrentUserSettings(this.store)?.aiTour?.[step];
  }

  private load(): Promise<TourLauncherApi> {
    if (!this.launcher) {
      this.launcher = import('@modules/tour/tour.module').then(m => createNgModule(m.TourModule, this.injector).injector.get(m.TourLauncher));
      this.launcher.catch(() => this.launcher = null);
    }
    return this.launcher;
  }
}
