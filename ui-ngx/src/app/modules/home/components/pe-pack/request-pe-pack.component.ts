// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import {
  AddonType,
  PlatformFeature,
  PlatformFeatureOfferInfo,
  platformFeatureOffers
} from '@shared/models/subscription.models';
import { ActivatedRoute, Router } from '@angular/router';
import {
  getIntegrationHelpLinkByType,
  IntegrationType,
  integrationTypeInfoMap
} from '@shared/models/integration.models';
import { HelpLinks } from '@shared/models/constants';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { resolveSendErrorMessage } from '@core/utils';
import { NotificationService } from '@core/http/notification.service';
import { TranslateService } from '@ngx-translate/core';

const PE_PACK_REQUESTED_STORAGE_KEY = 'pePackRequested';

@Component({
  selector: 'tb-request-pe-pack',
  templateUrl: './request-pe-pack.component.html',
  styleUrls: ['./request-pe-pack.component.scss'],
  standalone: false
})
export class RequestPePackComponent implements OnInit {

  PlatformFeature = PlatformFeature;

  feature: PlatformFeature;

  offerInfo: PlatformFeatureOfferInfo;

  integrationTypes: IntegrationType[] = [
    IntegrationType.CHIRPSTACK, IntegrationType.TTI, IntegrationType.LORIOT, IntegrationType.THINGPARK, IntegrationType.MQTT,
    IntegrationType.HTTP, IntegrationType.COAP, IntegrationType.TCP, IntegrationType.UDP, IntegrationType.OPC_UA,
    IntegrationType.KAFKA, IntegrationType.RABBITMQ, IntegrationType.APACHE_PULSAR, IntegrationType.AWS_IOT, IntegrationType.AWS_KINESIS,
    IntegrationType.AWS_SQS, IntegrationType.AZURE_IOT_HUB, IntegrationType.AZURE_EVENT_HUB, IntegrationType.AZURE_SERVICE_BUS,
    IntegrationType.PUB_SUB, IntegrationType.PARTICLE, IntegrationType.TUYA, IntegrationType.SIGFOX, IntegrationType.CUSTOM
  ];

  pePackRequested = false;

  sendError = false;
  sendNetworkError = false;
  sendErrorMessage: string | null = null;

  get customerUser(): boolean {
    return getCurrentAuthUser(this.store)?.authority === Authority.CUSTOMER_USER;
  }

  constructor(private route: ActivatedRoute,
              private router: Router,
              private store: Store<AppState>,
              private wl: WhiteLabelingService,
              private translate: TranslateService,
              private notificationService: NotificationService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.feature = this.route.snapshot.data.platformFeature;
    this.offerInfo = platformFeatureOffers.get(this.feature);
    this.pePackRequested = this.restorePePackRequested();
  }

  integrationName(type: IntegrationType): string {
    if (type === IntegrationType.TTI) {
      return 'integration.type-things-stack';
    }
    return integrationTypeInfoMap.get(type).name;
  }

  openIntegrationGuide(type: IntegrationType) {
    const linkId = getIntegrationHelpLinkByType(type);
    this.gotoHelpPage(linkId);
  }

  requestAccess() {
    this.clearSendErrors();
    this.notificationService.sendAddonAccessRequest(AddonType.PROFESSIONAL_UPGRADE, {ignoreLoading: true, ignoreErrors: true}).subscribe(
      {
        next: () => {
          this.pePackRequested = true;
          this.storePePackRequested();
          this.cd.markForCheck();
        },
        error: err => {
          void this.onSendError(err);
          this.cd.markForCheck();
        }
      }
    );
  }

  gotoPage(page: string) {
    void this.router.navigateByUrl(page);
  }

  private gotoHelpPage(helpLinkId: string): void {
    let helpUrl = HelpLinks.linksMap[helpLinkId];
    if (helpUrl) {
      const baseUrl =  this.wl.getHelpLinkBaseUrl();
      if (baseUrl) {
        helpUrl = helpUrl.replace('https://thingsboard.io', baseUrl);
      }
      window.open(helpUrl, '_blank');
    }
  }

  private storePePackRequested(): void {
    try {
      localStorage.setItem(PE_PACK_REQUESTED_STORAGE_KEY, JSON.stringify(true));
    } catch {}
  }

  private restorePePackRequested(): boolean {
    try {
      const stored = localStorage.getItem(PE_PACK_REQUESTED_STORAGE_KEY);
      return stored ? (JSON.parse(stored) === true) : false;
    } catch {
      return false;
    }
  }

  private clearSendErrors(): void {
    this.sendError = false;
    this.sendNetworkError = false;
    this.sendErrorMessage = null;
  }

  private async onSendError(err: any): Promise<void> {
    this.sendError = true;
    // Asked of the failure itself, not of whether a message could be got out of it: status 0 means the browser
    // never reached a server, which is what all five readers of this flag are actually asking.
    this.sendNetworkError = err?.status === 0;
    this.sendErrorMessage = this.sendNetworkError ? null : await resolveSendErrorMessage(err, this.translate);
  }

}
