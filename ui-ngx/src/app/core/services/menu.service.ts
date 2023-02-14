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

import { Injectable } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { select, Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { getCurrentOpenedMenuSections, selectAuth, selectIsAuthenticated } from '../auth/auth.selectors';
import { filter, take } from 'rxjs/operators';
import { HomeSection, MenuSection } from '@core/services/menu.models';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { guid } from '@core/utils';
import { AuthState } from '@core/auth/auth.models';
import { NavigationEnd, Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  currentMenuSections: Array<MenuSection>;
  menuSections$: Subject<Array<MenuSection>> = new BehaviorSubject<Array<MenuSection>>([]);
  homeSections$: Subject<Array<HomeSection>> = new BehaviorSubject<Array<HomeSection>>([]);

  constructor(private store: Store<AppState>,
              private authService: AuthService,
              private router: Router) {
    this.store.pipe(select(selectIsAuthenticated)).subscribe(
      (authenticated: boolean) => {
        if (authenticated) {
          this.buildMenu();
        }
      }
    );
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(
      () => {
        this.updateOpenedMenuSections();
      }
    );
  }

  private buildMenu() {
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        if (authState.authUser) {
          let homeSections: Array<HomeSection>;
          switch (authState.authUser.authority) {
            case Authority.SYS_ADMIN:
              this.currentMenuSections = this.buildSysAdminMenu(authState);
              homeSections = this.buildSysAdminHome(authState);
              break;
            case Authority.TENANT_ADMIN:
              this.currentMenuSections = this.buildTenantAdminMenu(authState);
              homeSections = this.buildTenantAdminHome(authState);
              break;
            case Authority.CUSTOMER_USER:
              this.currentMenuSections = this.buildCustomerUserMenu(authState);
              homeSections = this.buildCustomerUserHome(authState);
              break;
          }
          this.updateOpenedMenuSections();
          this.menuSections$.next(this.currentMenuSections);
          this.homeSections$.next(homeSections);
        }
      }
    );
  }

  private updateOpenedMenuSections() {
    const url = this.router.url;
    const openedMenuSections = getCurrentOpenedMenuSections(this.store);
    this.currentMenuSections.filter(section => section.type === 'toggle' &&
      (url.startsWith(section.path) || openedMenuSections.includes(section.path))).forEach(
      section => section.opened = true
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
        name: 'admin.resources',
        type: 'toggle',
        path: '/resources',
        height: '80px',
        icon: 'folder',
        pages: [
          {
            id: guid(),
            name: 'widget.widget-library',
            type: 'link',
            path: '/resources/widgets-bundles',
            icon: 'now_widgets'
          },
          {
            id: guid(),
            name: 'resource.resources-library',
            type: 'link',
            path: '/resources/resources-library',
            icon: 'mdi:rhombus-split',
            isMdiIcon: true
          }
        ]
      },
      {
        id: guid(),
        name: 'admin.settings',
        type: 'link',
        path: '/settings',
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
            name: 'admin.notifications',
            type: 'link',
            path: '/settings/notifications',
            icon: 'mdi:message-badge',
            isMdiIcon: true
          },
          {
            id: guid(),
            name: 'admin.queues',
            type: 'link',
            path: '/settings/queues',
            icon: 'swap_calls'
          },
        ]
      },
      {
        id: guid(),
        name: 'security.security',
        type: 'link',
        path: '/security-settings',
        icon: 'security',
        pages: [
          {
            id: guid(),
            name: 'admin.general',
            type: 'link',
            path: '/security-settings/general',
            icon: 'settings_applications'
          },
          {
            id: guid(),
            name: 'admin.oauth2.oauth2',
            type: 'link',
            path: '/security-settings/oauth2',
            icon: 'mdi:shield-account',
            isMdiIcon: true
          },
          {
            id: guid(),
            name: 'admin.2fa.2fa',
            type: 'link',
            path: '/security-settings/2fa',
            icon: 'mdi:two-factor-authentication',
            isMdiIcon: true
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
          },
          {
            name: 'admin.2fa.2fa',
            icon: 'mdi:two-factor-authentication',
            isMdiIcon: true,
            path: '/settings/2fa'
          },
          {
            name: 'resource.resources-library',
            icon: 'folder',
            path: '/settings/resources-library'
          },
          {
            name: 'admin.queues',
            icon: 'swap_calls',
            path: '/settings/queues'
          },
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
        name: 'alarm.alarms',
        type: 'link',
        path: '/alarm',
        icon: 'notifications',
        pages: [
          {
            id: guid(),
            name: 'alarm.all-alarms',
            type: 'link',
            path: '/alarm/alarms',
            icon: 'notifications'
          },
          {
            id: guid(),
            name: 'alarm-rule.rules',
            type: 'link',
            path: '/alarm/rules',
            icon: 'edit_notifications'
          }
        ]
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
        name: 'entity.entities',
        type: 'toggle',
        path: '/entities',
        height: '120px',
        icon: 'category',
        pages: [
          {
            id: guid(),
            name: 'device.devices',
            type: 'link',
            path: '/entities/devices',
            icon: 'devices_other'
          },
          {
            id: guid(),
            name: 'asset.assets',
            type: 'link',
            path: '/entities/assets',
            icon: 'domain'
          },
          {
            id: guid(),
            name: 'entity-view.entity-views',
            type: 'link',
            path: '/entities/entityViews',
            icon: 'view_quilt'
          }
        ]
      },
      {
        id: guid(),
        name: 'profiles.profiles',
        type: 'toggle',
        path: '/profiles',
        height: '80px',
        icon: 'badge',
        pages: [
          {
            id: guid(),
            name: 'device-profile.device-profiles',
            type: 'link',
            path: '/profiles/deviceProfiles',
            icon: 'mdi:alpha-d-box',
            isMdiIcon: true
          },
          {
            id: guid(),
            name: 'asset-profile.asset-profiles',
            type: 'link',
            path: '/profiles/assetProfiles',
            icon: 'mdi:alpha-a-box',
            isMdiIcon: true
          }
        ]
      },
      {
        id: guid(),
        name: 'customer.customers',
        type: 'link',
        path: '/customers',
        icon: 'supervisor_account'
      }
    );
    if (authState.edgesSupportEnabled) {
      sections.push(
        {
          id: guid(),
          name: 'edge.management',
          type: 'toggle',
          path: '/edgeManagement',
          height: '80px',
          icon: 'settings_input_antenna',
          pages: [
            {
              id: guid(),
              name: 'edge.instances',
              type: 'link',
              path: '/edgeManagement/instances',
              icon: 'router'
            },
            {
              id: guid(),
              name: 'edge.rulechain-templates',
              type: 'link',
              path: '/edgeManagement/ruleChains',
              icon: 'settings_ethernet'
            }
          ]
        }
      );
    }
    sections.push(
      {
        id: guid(),
        name: 'feature.advanced-features',
        type: 'toggle',
        path: '/features',
        height: '120px',
        icon: 'construction',
        pages: [
          {
            id: guid(),
            name: 'rulechain.rulechains',
            type: 'link',
            path: '/features/ruleChains',
            icon: 'settings_ethernet'
          },
          {
            id: guid(),
            name: 'ota-update.ota-updates',
            type: 'link',
            path: '/features/otaUpdates',
            icon: 'memory'
          },
          {
            id: guid(),
            name: 'version-control.version-control',
            type: 'link',
            path: '/features/vc',
            icon: 'history'
          }
        ]
      },
      {
        id: guid(),
        name: 'admin.resources',
        type: 'toggle',
        path: '/resources',
        height: '80px',
        icon: 'folder',
        pages: [
          {
            id: guid(),
            name: 'widget.widget-library',
            type: 'link',
            path: '/resources/widgets-bundles',
            icon: 'now_widgets'
          },
          {
            id: guid(),
            name: 'resource.resources-library',
            type: 'link',
            path: '/resources/resources-library',
            icon: 'mdi:rhombus-split',
            isMdiIcon: true
          }
        ]
      },
      {
        id: guid(),
        name: 'notification.notification-center',
        type: 'link',
        path: '/notification',
        icon: 'mdi:message-badge',
        isMdiIcon: true,
        pages: [
          {
            id: guid(),
            name: 'notification.inbox',
            type: 'link',
            path: '/notification/inbox',
            icon: 'inbox'
          },
          {
            id: guid(),
            name: 'notification.sent',
            type: 'link',
            path: '/notification/sent',
            icon: 'outbox'
          },
          {
            id: guid(),
            name: 'notification.templates',
            type: 'link',
            path: '/notification/templates',
            icon: 'mdi:message-draw',
            isMdiIcon: true
          },
          {
            id: guid(),
            name: 'notification.recipients',
            type: 'link',
            path: '/notification/recipients',
            icon: 'contacts'
          },
          {
            id: guid(),
            name: 'notification.rules',
            type: 'link',
            path: '/notification/rules',
            icon: 'mdi:message-cog',
            isMdiIcon: true
          }
        ]
      },
      {
        id: guid(),
        name: 'api-usage.api-usage',
        type: 'link',
        path: '/usage',
        icon: 'insert_chart'
      },
      {
        id: guid(),
        name: 'admin.settings',
        type: 'link',
        path: '/settings',
        icon: 'settings',
        pages: [
          {
            id: guid(),
            name: 'admin.home',
            type: 'link',
            path: '/settings/home',
            icon: 'settings_applications'
          },
          {
            id: guid(),
            name: 'admin.repository',
            type: 'link',
            path: '/settings/repository',
            icon: 'manage_history'
          },
          {
            id: guid(),
            name: 'admin.auto-commit',
            type: 'link',
            path: '/settings/auto-commit',
            icon: 'settings_backup_restore'
          }
        ]
      },
      {
        id: guid(),
        name: 'security.security',
        type: 'link',
        path: '/security-settings',
        icon: 'security',
        pages: [
          {
            id: guid(),
            name: 'audit-log.audit-logs',
            type: 'link',
            path: '/security-settings/auditLogs',
            icon: 'track_changes'
          }
        ]
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
          },
          {
            name: 'asset-profile.asset-profiles',
            icon: 'mdi:alpha-a-box',
            isMdiIcon: true,
            path: '/profiles/assetProfiles'
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
            path: '/profiles/deviceProfiles'
          },
          {
            name: 'ota-update.ota-updates',
            icon: 'memory',
            path: '/otaUpdates'
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
              path: '/edgeInstances'
            },
            {
              name: 'edge.rulechain-templates',
              icon: 'settings_ethernet',
              path: '/edgeManagement/ruleChains'
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
        name: 'version-control.management',
        places: [
          {
            name: 'version-control.version-control',
            icon: 'history',
            path: '/vc'
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
      },
      {
        name: 'admin.system-settings',
        places: [
          {
            name: 'admin.home-settings',
            icon: 'settings_applications',
            path: '/settings/home'
          },
          {
            name: 'resource.resources-library',
            icon: 'folder',
            path: '/settings/resources-library'
          },
          {
            name: 'admin.repository-settings',
            icon: 'manage_history',
            path: '/settings/repository',
          },
          {
            name: 'admin.auto-commit-settings',
            icon: 'settings_backup_restore',
            path: '/settings/auto-commit'
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
        name: 'alarm.alarms',
        type: 'link',
        path: '/alarms',
        icon: 'notifications'
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
        name: 'entity.entities',
        type: 'toggle',
        path: '/entities',
        height: '120px',
        icon: 'category',
        pages: [
          {
            id: guid(),
            name: 'device.devices',
            type: 'link',
            path: '/entities/devices',
            icon: 'devices_other'
          },
          {
            id: guid(),
            name: 'asset.assets',
            type: 'link',
            path: '/entities/assets',
            icon: 'domain'
          },
          {
            id: guid(),
            name: 'entity-view.entity-views',
            type: 'link',
            path: '/entities/entityViews',
            icon: 'view_quilt'
          }
        ]
      }
    );
    if (authState.edgesSupportEnabled) {
      sections.push(
        {
          id: guid(),
          name: 'edge.edge-instances',
          type: 'link',
          path: '/edgeManagement/instances',
          icon: 'router'
        }
      );
    }
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
              icon: 'settings_input_antenna',
              path: '/edgeInstances'
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

