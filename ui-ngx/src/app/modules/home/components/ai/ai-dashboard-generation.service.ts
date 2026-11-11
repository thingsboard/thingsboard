// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { DestroyRef, Injectable } from '@angular/core';
import { createDefaultEntityDataPageLink, singleEntityFilterFromDeviceId } from '@shared/models/query/query.models';
import { AiLoadingModalComponent, AiLoadingModalData } from '@home/components/ai/ai-loading-modal.component';
import { dashboardGenerateImage, generateDashboardSteps } from '@home/components/ai/ai.models';
import { finalize } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isNotEmptyStr, parseHttpErrorMessage } from '@core/utils';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import {
  AiNoTelemetryModalComponent,
  AiNoTelemetryModalData
} from '@home/components/ai/ai-no-telemetry-modal.component';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { Operation, Resource } from '@shared/models/security.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { MatDialog } from '@angular/material/dialog';
import { AiChatService } from '@core/http/ai-chat.service';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DomSanitizer } from '@angular/platform-browser';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupInfo } from '@shared/models/entity-group.models';

interface GenerateDashboardOptions {
  deviceId: string;
  destroyRef?: DestroyRef;
  noTelemetry: AiNoTelemetryModalData;
  beforeOpen?: () => void;
}

interface AllowDashboardGenerateOptions {
  entityGroup?: EntityGroupInfo;
  requireTelemetry?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AiDashboardGenerationService {

  constructor(private entityService: EntityService,
              private dialog: MatDialog,
              private aiChatService: AiChatService,
              private router: Router,
              private translate: TranslateService,
              private sanitizer: DomSanitizer,
              private store: Store<AppState>,
              private userPermissionsService: UserPermissionsService) {
  }

  generateWithTelemetryCheck(opts: GenerateDashboardOptions): void {
    this.entityService.findEntityKeysByQueryV2({
      entityFilter: singleEntityFilterFromDeviceId(opts.deviceId),
      pageLink: createDefaultEntityDataPageLink(1),
    }, true, false).subscribe((res) => {
      opts.beforeOpen?.();
      if (res.timeseries.length) {
        this.generate(opts.deviceId, opts.destroyRef);
      } else {
        this.openNoTelemetryModal(opts.noTelemetry);
      }
    });
  }


  generate(deviceId: string, destroyRef?: DestroyRef, timeseriesKeys: string[] = []): void {
    const dialogRef = this.dialog.open(AiLoadingModalComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        messages: generateDashboardSteps,
        estimateWaitTime: 60000,
        image: dashboardGenerateImage
      } as AiLoadingModalData
    });
    let request$ = this.aiChatService.generateDashboard(deviceId, timeseriesKeys).pipe(
      finalize(() => dialogRef.close())
    );
    if (destroyRef) {
      request$ = request$.pipe(takeUntilDestroyed(destroyRef));
    }
    request$.subscribe({
      next: (response) => this.router.navigate(['/dashboards', response])
    });
  }

  openNoTelemetryModal(data: AiNoTelemetryModalData): void {
    this.dialog.open(AiNoTelemetryModalComponent, {
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data,
    });
  }

  isAllowedDashboardGenerate(opts: AllowDashboardGenerateOptions = {}): boolean {
    const allowed = getCurrentAuthState(this.store).aiEnabled
      && getCurrentAuthUser(this.store).authority === Authority.TENANT_ADMIN
      && this.userPermissionsService.hasGenericPermission(Resource.AI, Operation.WRITE)
      && this.userPermissionsService.hasEntityTypeOperationPermission(EntityType.DASHBOARD, Operation.WRITE)
      && this.userPermissionsService.hasEntityTypeOperationPermission(EntityType.DEVICE, Operation.READ_ATTRIBUTES, opts.entityGroup);
    if (!opts.requireTelemetry) {
      return allowed;
    }
    return allowed
      && this.userPermissionsService.hasEntityTypeOperationPermission(EntityType.DEVICE, Operation.READ_TELEMETRY, opts.entityGroup);
  }
}
