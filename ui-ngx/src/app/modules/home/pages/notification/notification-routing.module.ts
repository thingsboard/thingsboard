///
/// Copyright © 2016-2023 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { ActivatedRoute, RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { Component, NgModule, OnInit } from '@angular/core';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-notification-temp-component',
  template: '<div>{{text}}</div>',
  styleUrls: []
})
class NotificationTempComponent implements OnInit {

  text: string;

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    if (isDefinedAndNotNull(this.route.snapshot.data.text)) {
      this.text = this.route.snapshot.data.text;
    }
  }
}

const routes: Routes = [
  {
    path: 'notification',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        label: 'notification.notification-center',
        icon: 'mdi:message-badge'
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: '/notification/inbox'
        }
      },
      {
        path: 'inbox',
        component: NotificationTempComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'notification.inbox',
          breadcrumb: {
            label: 'notification.inbox',
            icon: 'inbox'
          },
          text: 'TODO: Implement inbox'
        }
      },
      {
        path: 'sent',
        component: NotificationTempComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'notification.sent',
          breadcrumb: {
            label: 'notification.sent',
            icon: 'outbox'
          },
          text: 'TODO: Implement sent'
        }
      },
      {
        path: 'templates',
        component: NotificationTempComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'notification.templates',
          breadcrumb: {
            label: 'notification.templates',
            icon: 'mdi:message-draw'
          },
          text: 'TODO: Implement templates'
        }
      },
      {
        path: 'recipients',
        component: NotificationTempComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'notification.recipients',
          breadcrumb: {
            label: 'notification.recipients',
            icon: 'contacts'
          },
          text: 'TODO: Implement recipients'
        }
      },
      {
        path: 'rules',
        component: NotificationTempComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'notification.rules',
          breadcrumb: {
            label: 'notification.rules',
            icon: 'mdi:message-cog'
          },
          text: 'TODO: Implement rules'
        }
      }
    ]
  }
];

@NgModule({
  declarations: [NotificationTempComponent],
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class NotificationRoutingModule { }
