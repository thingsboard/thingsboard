// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, HostBinding } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableFooterComponent } from '@home/components/entity/entity-table-footer.component';
import { Extension, ExtensionQuota, ExtensionSizeAvailability } from '@shared/models/extension.models';
import { ExtensionQuotaService } from '@home/pages/extension/extension-quota.service';
import { map, Observable } from 'rxjs';

interface ExtensionQuotaView {
  quota: ExtensionQuota;
  // only the tiers that still fit; empty when the tenant has no room left at all
  deployable: Array<ExtensionSizeAvailability>;
}

/**
 * Panel under the extensions table: what the tenant already uses and how much is still free,
 * including how many more extensions of each resource tier fit.
 */
@Component({
  selector: 'tb-extension-quota-panel',
  templateUrl: './extension-quota-panel.component.html',
  styleUrls: ['./extension-quota-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class ExtensionQuotaPanelComponent extends EntityTableFooterComponent<Extension> {

  @HostBinding('style.width') width = '100%';

  // shown instead of a number when the tenant profile sets no limit for that dimension
  readonly unlimited = '∞';

  // the API counts CPU in millicores; the panel shows CPU cores, like the tenant profile form
  readonly millicoresInCpu = 1000;

  readonly vm$: Observable<ExtensionQuotaView>;

  constructor(protected store: Store<AppState>,
              private quotaService: ExtensionQuotaService) {
    super(store);
    // a tier the tenant cannot fit even once carries no information — drop it and keep the row short;
    // a null count means that tier is unlimited, so it always stays
    this.vm$ = this.quotaService.quota$.pipe(
      map(quota => quota ? {
        quota,
        deployable: quota.availableBySize.filter(item => item.count === null || item.count > 0)
      } : null)
    );
  }

}
