///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Injectable } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { select, Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { selectAuth, selectAuthUser, selectIsAuthenticated } from '../auth/auth.selectors';
import { take } from 'rxjs/operators';
import { HomeSection, MenuSection } from '@core/services/menu.models';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { guid } from '@core/utils';
import { AuthState } from "@core/auth/auth.models";

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  menuSections$: Subject<Array<MenuSection>> = new BehaviorSubject<Array<MenuSection>>([]);
  homeSections$: Subject<Array<HomeSection>> = new BehaviorSubject<Array<HomeSection>>([]);

  constructor(private store: Store<AppState>, private authService: AuthService) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe(
      (authenticated: boolean) => {
        if (authenticated) {
          this.buildMenu();
        }
      }
    );
  }

  private buildMenu() {
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        if (authState.authUser) {
          let menuSections: Array<MenuSection>;
          let homeSections: Array<HomeSection>;
          switch (authState.authUser.authority) {
            case Authority.SYS_ADMIN:
              menuSections = this.buildSysAdminMenu(authState);
              homeSections = this.buildSysAdminHome(authState);
              break;
            case Authority.TENANT_ADMIN:
              menuSections = this.buildTenantAdminMenu(authState);
              homeSections = this.buildTenantAdminHome(authState);
              break;
            case Authority.CUSTOMER_USER:
              menuSections = this.buildCustomerUserMenu(authState);
              homeSections = this.buildCustomerUserHome(authState);
              break;
          }
          this.menuSections$.next(menuSections);
          this.homeSections$.next(homeSections);
        }
      }
    );
  }

  private buildSysAdminMenu(authState: AuthState): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push(
      {
        id: guid(),
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home'
      },
      {
        id: guid(),
        name: 'tenant.tenants',
        type: 'link',
        path: '/tenants',
        icon: 'supervisor_account'
      },
      {
        id: guid(),
        name: 'tenant-profile.tenant-profiles',
        type: 'link',
        path: '/tenantProfiles',
        icon: 'mdi:alpha-t-box',
        isMdiIcon: true
      },
      {
        id: guid(),
        name: 'widget.widget-library',
        type: 'link',
        path: '/widgets-bundles',
        icon: 'now_widgets'
      },
      {
        id: guid(),
        name: 'admin.system-settings',
        type: 'toggle',
        path: '/settings',
        height: '200px',
        icon: 'settings',
        pages: [
          {
            id: guid(),
            name: 'admin.general',
            type: 'link',
            path: '/settings/general',
            icon: 'settings_applications'
          },
          {
            id: guid(),
            name: 'admin.outgoing-mail',
            type: 'link',
            path: '/settings/outgoing-mail',
            icon: 'mail'
          },
          {
            id: guid(),
            name: 'admin.sms-provider',
            type: 'link',
            path: '/settings/sms-provider',
            icon: 'sms'
          },
          {
            id: guid(),
            name: 'admin.security-settings',
            type: 'link',
            path: '/settings/security-settings',
            icon: 'security'
          },
          {
            id: guid(),
            name: 'admin.oauth2.oauth2',
            type: 'link',
            path: '/settings/oauth2',
            icon: 'security'
          }
        ]
      }
    );
    return sections;
  }

  private buildSysAdminHome(authState: AuthState): Array<HomeSection> {
    const homeSections: Array<HomeSection> = [];
    homeSections.push(
      {
        name: 'tenant.management',
        places: [
          {
            name: 'tenant.tenants',
            icon: 'supervisor_account',
            path: '/tenants'
          },
          {
            name: 'tenant-profile.tenant-profiles',
            icon: 'mdi:alpha-t-box',
            isMdiIcon: true,
            path: '/tenantProfiles'
          },
        ]
      },
      {
        name: 'widget.management',
        places: [
          {
            name: 'widget.widget-library',
            icon: 'now_widgets',
            path: '/widgets-bundles'
          }
        ]
      },
      {
        name: 'admin.system-settings',
        places: [
          {
            name: 'admin.general',
            icon: 'settings_applications',
            path: '/settings/general'
          },
          {
            name: 'admin.outgoing-mail',
            icon: 'mail',
            path: '/settings/outgoing-mail'
          },
          {
            name: 'admin.sms-provider',
            icon: 'sms',
            path: '/settings/sms-provider'
          },
          {
            name: 'admin.security-settings',
            icon: 'security',
            path: '/settings/security-settings'
          },
          {
            name: 'admin.oauth2.oauth2',
            icon: 'security',
            path: '/settings/oauth2'
          }
        ]
      }
    );
    return homeSections;
  }

  private buildTenantAdminMenu(authState: AuthState): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push(
      {
        id: guid(),
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home'
      },
      {
        id: guid(),
        name: 'rulechain.rulechains',
        type: 'link',
        path: '/ruleChains',
        icon: 'settings_ethernet'
      },
      {
        id: guid(),
        name: 'customer.customers',
        type: 'link',
        path: '/customers',
        icon: 'supervisor_account'
      },
      {
        id: guid(),
        name: 'asset.assets',
        type: 'link',
        path: '/assets',
        icon: 'domain'
      },
      {
        id: guid(),
        name: 'device.devices',
        type: 'link',
        path: '/devices',
        icon: 'devices_other'
      },
      {
        id: guid(),
        name: 'device-profile.device-profiles',
        type: 'link',
        path: '/deviceProfiles',
        icon: 'mdi:alpha-d-box',
        isMdiIcon: true
      },
      {
        id: guid(),
        name: 'entity-view.entity-views',
        type: 'link',
        path: '/entityViews',
        icon: 'view_quilt'
      }
    );
    if (authState.edgesSupportEnabled) {
      sections.push(
        {
          id: guid(),
          name: 'edge.management',
          type: 'toggle',
          path: '/edges',
          height: '80px',
          icon: 'router',
          pages: [
            {
              id: guid(),
              name: 'edge.edge-instances',
              type: 'link',
              path: '/edges',
              icon: 'router'
            },
            {
              id: guid(),
              name: 'edge.rulechain-templates',
              type: 'link',
              path: '/edges/ruleChains',
              icon: 'settings_ethernet'
            }
          ]
        }
      );
    }
    sections.push(
      {
        id: guid(),
        name: 'widget.widget-library',
        type: 'link',
        path: '/widgets-bundles',
        icon: 'now_widgets'
      },
      {
        id: guid(),
        name: 'dashboard.dashboards',
        type: 'link',
        path: '/dashboards',
        icon: 'dashboards'
      },
      {
        id: guid(),
        name: 'audit-log.audit-logs',
        type: 'link',
        path: '/auditLogs',
        icon: 'track_changes'
      },
      {
        id: guid(),
        name: 'api-usage.api-usage',
        type: 'link',
        path: '/usage',
        icon: 'insert_chart',
        notExact: true
      }
    );
    return sections;
  }

  private buildTenantAdminHome(authState: AuthState): Array<HomeSection> {
    const homeSections: Array<HomeSection> = [];
    homeSections.push(
      {
        name: 'rulechain.management',
        places: [
          {
            name: 'rulechain.rulechains',
            icon: 'settings_ethernet',
            path: '/ruleChains'
          }
        ]
      },
      {
        name: 'customer.management',
        places: [
          {
            name: 'customer.customers',
            icon: 'supervisor_account',
            path: '/customers'
          }
        ]
      },
      {
        name: 'asset.management',
        places: [
          {
            name: 'asset.assets',
            icon: 'domain',
            path: '/assets'
          }
        ]
      },
      {
        name: 'device.management',
        places: [
          {
            name: 'device.devices',
            icon: 'devices_other',
            path: '/devices'
          },
          {
            name: 'device-profile.device-profiles',
            icon: 'mdi:alpha-d-box',
            isMdiIcon: true,
            path: '/deviceProfiles'
          }
        ]
      },
      {
        name: 'entity-view.management',
        places: [
          {
            name: 'entity-view.entity-views',
            icon: 'view_quilt',
            path: '/entityViews'
          }
        ]
      }
    );
    if (authState.edgesSupportEnabled) {
      homeSections.push(
        {
          name: 'edge.management',
          places: [
            {
              name: 'edge.edge-instances',
              icon: 'router',
              path: '/edges'
            },
            {
              name: 'edge.rulechain-templates',
              icon: 'settings_ethernet',
              path: '/edges/ruleChains'
            }
          ]
        }
      );
    }
    homeSections.push(
      {
        name: 'dashboard.management',
        places: [
          {
            name: 'widget.widget-library',
            icon: 'now_widgets',
            path: '/widgets-bundles'
          },
          {
            name: 'dashboard.dashboards',
            icon: 'dashboard',
            path: '/dashboards'
          }
        ]
      },
      {
        name: 'audit-log.audit',
        places: [
          {
            name: 'audit-log.audit-logs',
            icon: 'track_changes',
            path: '/auditLogs'
          },
          {
            name: 'api-usage.api-usage',
            icon: 'insert_chart',
            path: '/usage'
          }
        ]
      }
    );
    return homeSections;
  }

  private buildCustomerUserMenu(authState: AuthState): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push(
      {
        id: guid(),
        name: 'home.home',
        type: 'link',
        path: '/home',
        icon: 'home'
      },
      {
        id: guid(),
        name: 'asset.assets',
        type: 'link',
        path: '/assets',
        icon: 'domain'
      },
      {
        id: guid(),
        name: 'device.devices',
        type: 'link',
        path: '/devices',
        icon: 'devices_other'
      },
      {
        id: guid(),
        name: 'entity-view.entity-views',
        type: 'link',
        path: '/entityViews',
        icon: 'view_quilt'
      }
    );
    if (authState.edgesSupportEnabled) {
      sections.push(
        {
          id: guid(),
          name: 'edge.edge-instances',
          type: 'link',
          path: '/edges',
          icon: 'router'
        }
      );
    }
    sections.push(
      {
        id: guid(),
        name: 'dashboard.dashboards',
        type: 'link',
        path: '/dashboards',
        icon: 'dashboard'
      }
    );
    return sections;
  }

  private buildCustomerUserHome(authState: AuthState): Array<HomeSection> {
    const homeSections: Array<HomeSection> = [];
    homeSections.push(
      {
        name: 'asset.view-assets',
        places: [
          {
            name: 'asset.assets',
            icon: 'domain',
            path: '/assets'
          }
        ]
      },
      {
        name: 'device.view-devices',
        places: [
          {
            name: 'device.devices',
            icon: 'devices_other',
            path: '/devices'
          }
        ]
      },
      {
        name: 'entity-view.management',
        places: [
          {
            name: 'entity-view.entity-views',
            icon: 'view_quilt',
            path: '/entityViews'
          }
        ]
      }
    );
    if (authState.edgesSupportEnabled) {
      homeSections.push(
        {
          name: 'edge.management',
          places: [
            {
              name: 'edge.edge-instances',
              icon: 'router',
              path: '/edges'
            }
          ]
        }
      );
    }
    homeSections.push(
      {
        name: 'dashboard.view-dashboards',
        places: [
          {
            name: 'dashboard.dashboards',
            icon: 'dashboard',
            path: '/dashboards'
          }
        ]
      }
    );
    return homeSections;
  }

  public menuSections(): Observable<Array<MenuSection>> {
    return this.menuSections$;
  }

  public homeSections(): Observable<Array<HomeSection>> {
    return this.homeSections$;
  }

}

