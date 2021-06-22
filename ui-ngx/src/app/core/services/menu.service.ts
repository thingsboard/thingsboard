///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import {Injectable} from '@angular/core';
import {AuthService} from '../auth/auth.service';
import {select, Store} from '@ngrx/store';
import {AppState} from '../core.state';
import {selectAuth, selectIsAuthenticated} from '../auth/auth.selectors';
import {take} from 'rxjs/operators';
import {HomeSection, MenuSection} from '@core/services/menu.models';
import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {Authority} from '@shared/models/authority.enum';
import {guid} from '@core/utils';
import {AuthState} from '@core/auth/auth.models';

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
              menuSections = this.encapsulateThingsboardMenu(this.buildSysAdminMenu(authState));
              homeSections = this.encapsulateThingsboardHomeMenu(this.buildSysAdminHome(authState));
              break;
            case Authority.TENANT_ADMIN:
              menuSections = this.encapsulateThingsboardMenu(this.buildTenantAdminMenu(authState));
              homeSections = this.encapsulateThingsboardHomeMenu(this.buildTenantAdminHome(authState));
              break;
            case Authority.CUSTOMER_USER:
              menuSections = this.encapsulateThingsboardMenu(this.buildCustomerUserMenu(authState));
              homeSections = this.encapsulateThingsboardHomeMenu(this.buildCustomerUserHome(authState));
              break;
          }
          this.menuSections$.next(menuSections);
          this.homeSections$.next(homeSections);
        }
      }
    );
  }

  private encapsulateThingsboardMenu(menuItems: Array<MenuSection>): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push({
      id: guid(),
      name: 'Thingsboard',
      type: 'toggle',
      path: '/',
      notExact: false,
      isMdiIcon: true,
      icon: 'thingsboard_selected',
      pages: menuItems
    });
    return sections;
  }

  private encapsulateThingsboardHomeMenu(menuItems: Array<HomeSection>): Array<HomeSection> {
    return menuItems;
  }

  private buildSysAdminMenu(authState: AuthState): Array<MenuSection> {
    const sections: Array<MenuSection> = [];
    sections.push(
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
        height: '240px',
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
          },
          {
            id: guid(),
            name: 'resource.resources-library',
            type: 'link',
            path: '/settings/resources-library',
            icon: 'folder'
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
            name: 'resource.resources-library',
            icon: 'folder',
            path: '/resources-library'
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
        name: 'rulechain.rulechains',
        type: 'link',
        path: '/ruleChains',
        isMdiIcon: true,
        icon: 'rule_chains'
      },
      {
        id: guid(),
        name: 'customer.customers',
        type: 'link',
        path: '/customers',
        isMdiIcon: true,
        icon: 'customer'
      },
      {
        id: guid(),
        name: 'asset.assets',
        type: 'link',
        path: '/assets',
        isMdiIcon: true,
        icon: 'assets'
      },
      {
        id: guid(),
        name: 'device.devices',
        type: 'link',
        path: '/devices',
        isMdiIcon: true,
        icon: 'device'
      },
      {
        id: guid(),
        name: 'device-profile.device-profiles',
        type: 'link',
        path: '/deviceProfiles',
        isMdiIcon: true,
        icon: 'device_profiles'
      },
      {
        id: guid(),
        name: 'ota-update.ota-updates',
        type: 'link',
        isMdiIcon: true,
        icon: 'firmware'
        path: '/otaUpdates',
      },
      {
        id: guid(),
        name: 'entity-view.entity-views',
        type: 'link',
        path: '/entityViews',
        isMdiIcon: true,
        icon: 'entity_views'
      }
    );
    if (authState.edgesSupportEnabled) {
      sections.push(
        {
          id: guid(),
          name: 'edge.edge-instances',
          type: 'link',
          path: '/edgeInstances',
          icon: 'router'
        },
        {
          id: guid(),
          name: 'edge.management',
          type: 'toggle',
          path: '/edgeManagement',
          height: '40px',
          icon: 'settings_input_antenna',
          pages: [
            {
              id: guid(),
              name: 'edge.rulechain-templates',
              type: 'link',
              isMdiIcon: true,
              path: '/edgeManagement/ruleChains',
              icon: 'rule_chains'
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
        isMdiIcon: true,
        icon: 'widgets_library'
      },
      {
        id: guid(),
        name: 'dashboard.dashboards',
        type: 'link',
        path: '/dashboards',
        isMdiIcon: true,
        icon: 'dashboard'
      },
      {
        id: guid(),
        name: 'audit-log.audit-logs',
        type: 'link',
        path: '/auditLogs',
        isMdiIcon: true,
        icon: 'adit_usage'
      },
      {
        id: guid(),
        name: 'api-usage.api-usage',
        type: 'link',
        path: '/usage',
        notExact: true,
        isMdiIcon: true,
        icon: 'api_usage'
      },
      {
        id: guid(),
        name: 'admin.system-settings',
        type: 'toggle',
        path: '/settings',
        height: '80px',
        isMdiIcon: true,
        icon: 'home_settings',
        pages: [
          {
            id: guid(),
            name: 'admin.home-settings',
            type: 'link',
            path: '/settings/home',
            icon: 'settings_applications'
          },
          {
            id: guid(),
            name: 'resource.resources-library',
            type: 'link',
            path: '/settings/resources-library',
            isMdiIcon: true,
            icon: 'resources_library'
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
            path: '/ruleChains',
            isMdiIcon: true,
            icon: 'rule_chains'
          }
        ]
      },
      {
        name: 'customer.management',
        places: [
          {
            name: 'customer.customers',
            path: '/customers',
            isMdiIcon: true,
            icon: 'customer'
          }
        ]
      },
      {
        name: 'asset.management',
        places: [
          {
            name: 'asset.assets',
            path: '/assets',
            isMdiIcon: true,
            icon: 'assets'
          }
        ]
      },
      {
        name: 'device.management',
        places: [
          {
            name: 'device.devices',
            path: '/devices',
            isMdiIcon: true,
            icon: 'device'
          },
          {
            name: 'device-profile.device-profiles',
            path: '/deviceProfiles',
            isMdiIcon: true,
            icon: 'device_profiles'
          },
          {
            name: 'ota-update.ota-updates',
            isMdiIcon: true,
            icon: 'firmware',
            path: '/otaUpdates'
          }
        ]
      },
      {
        name: 'entity-view.management',
        places: [
          {
            name: 'entity-view.entity-views',
            path: '/entityViews',
            isMdiIcon: true,
            icon: 'entity_views'
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
              path: '/edgeManagement/ruleChains',
              isMdiIcon: true,
              icon: 'rule_chains'
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
            path: '/widgets-bundles',
            isMdiIcon: true,
            icon: 'widgets_library'
          },
          {
            name: 'dashboard.dashboards',
            path: '/dashboards',
            isMdiIcon: true,
            icon: 'dashboard'
          }
        ]
      },
      {
        name: 'audit-log.audit',
        places: [
          {
            name: 'audit-log.audit-logs',
            path: '/auditLogs',
            isMdiIcon: true,
            icon: 'adit_usage'
          },
          {
            name: 'api-usage.api-usage',
            path: '/usage',
            isMdiIcon: true,
            icon: 'api_usage'
          }
        ]
      },
      {
        name: 'admin.system-settings',
        places: [
          {
            name: 'admin.home-settings',
            path: '/settings/home',
            isMdiIcon: true,
            icon: 'home_settings'
          },
          {
            name: 'resource.resources-library',
            path: '/settings/resources-library',
            isMdiIcon: true,
            icon: 'resources_library'
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
        name: 'asset.assets',
        type: 'link',
        path: '/assets',
        isMdiIcon: true,
        icon: 'assets'
      },
      {
        id: guid(),
        name: 'device.devices',
        type: 'link',
        path: '/devices',
        isMdiIcon: true,
        icon: 'device'
      },
      {
        id: guid(),
        name: 'entity-view.entity-views',
        type: 'link',
        path: '/entityViews',
        isMdiIcon: true,
        icon: 'entity_views'
      }
    );
    if (authState.edgesSupportEnabled) {
      sections.push(
        {
          id: guid(),
          name: 'edge.edge-instances',
          type: 'link',
          path: '/edgeInstances',
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
        isMdiIcon: true,
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
            path: '/assets',
            isMdiIcon: true,
            icon: 'assets'
          }
        ]
      },
      {
        name: 'device.view-devices',
        places: [
          {
            name: 'device.devices',
            path: '/devices',
            isMdiIcon: true,
            icon: 'device'
          }
        ]
      },
      {
        name: 'entity-view.management',
        places: [
          {
            name: 'entity-view.entity-views',
            path: '/entityViews',
            isMdiIcon: true,
            icon: 'entity_views'
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
            path: '/dashboards',
            isMdiIcon: true,
            icon: 'dashboard'
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

