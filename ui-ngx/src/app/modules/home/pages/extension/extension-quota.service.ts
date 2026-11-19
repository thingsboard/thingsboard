// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectIsAuthenticated } from '@core/auth/auth.selectors';
import { ExtensionQuota } from '@shared/models/extension.models';
import { ExtensionService } from '@core/http/extension.service';

/**
 * Holds the last loaded extension quota. The extensions table reloads it on every fetch, so the
 * panel under the table follows create/delete/start/stop without knowing about the table itself.
 * The value is null while it is unknown (not loaded yet, or the request failed).
 */
@Injectable({
  providedIn: 'root'
})
export class ExtensionQuotaService {

  private readonly quotaSubject = new BehaviorSubject<ExtensionQuota>(null);

  readonly quota$: Observable<ExtensionQuota> = this.quotaSubject.asObservable();

  constructor(private extensionService: ExtensionService,
              private store: Store<AppState>) {
    // This service is a root singleton and logout does not reload the SPA, so without this the cached
    // value outlives the session that loaded it. The panel subscribes through the async pipe, and a
    // BehaviorSubject replays its last value synchronously — so the next user to open the Extensions
    // page would see the previous tenant's usage and deployable tiers painted before their own
    // request returns.
    this.store.select(selectIsAuthenticated).pipe(
      filter((authenticated) => !authenticated),
      takeUntilDestroyed()
    ).subscribe(() => this.quotaSubject.next(null));
  }

  reload() {
    this.extensionService.getExtensionQuota({ignoreErrors: true}).subscribe({
      next: (quota) => this.quotaSubject.next(quota),
      error: () => this.quotaSubject.next(null)
    });
  }

}
