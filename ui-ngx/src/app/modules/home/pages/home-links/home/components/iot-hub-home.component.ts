// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { HomeCardId } from '@shared/models/user-settings.models';
import { HOME_CARDS } from '@home/pages/home-links/home/home-cards.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { ItemType } from '@shared/models/iot-hub/iot-hub-item.models';
import { MpItemVersionQuery, MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { PageLink } from '@shared/models/page/page-link';

const SHOWN = 8;

const TEMPLATE_TINTS = ['#e0f2f1,#80cbc4', '#e3f2fd,#90caf9', '#fff3e0,#ffcc80', '#f3e5f5,#ce93d8', '#e8f5e9,#a5d6a7',
  '#e0f7fa,#80deea', '#fbe9e7,#ffab91', '#ede7f6,#b39ddb'];

const DEVICE_TINTS = ['#eceff1,#b0bec5', '#fbe9e7,#ffab91', '#e0f7fa,#80deea', '#fffde7,#fff59d', '#efebe9,#bcaaa4',
  '#ede7f6,#b39ddb', '#e8f5e9,#a5d6a7', '#e3f2fd,#90caf9'];

export interface IotHubKind {
  type: ItemType;
  name: string;
  icon: string;
  route: string;
}

const KINDS: IotHubKind[] = [
  {type: ItemType.WIDGET, name: 'home.iot-hub.widgets', icon: 'widgets', route: 'widgets'},
  {type: ItemType.CALCULATED_FIELD, name: 'home.iot-hub.calculated-fields', icon: 'functions', route: 'calculated-fields'},
  {type: ItemType.ALARM_RULE, name: 'home.iot-hub.alarm-rules', icon: 'notification_important', route: 'alarm-rules'},
  {type: ItemType.RULE_CHAIN, name: 'home.iot-hub.rule-chains', icon: 'account_tree', route: 'rule-chains'}
];

interface HubTile {
  item: MpItemVersionView;
  tint: string;
  image: string | null;
  meta: string;
}

@Component({
  selector: 'tb-iot-hub-home',
  templateUrl: './iot-hub-home.component.html',
  styleUrls: ['./iot-hub-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class IoTHubHomeComponent {

  readonly card = HOME_CARDS[HomeCardId.IOT_HUB];

  readonly kinds = KINDS;

  readonly templates = signal<HubTile[]>([]);
  readonly devices = signal<HubTile[]>([]);
  readonly deviceTotal = signal(0);
  readonly loaded = signal(false);
  readonly failed = signal(false);
  readonly searching = signal(false);
  readonly query = signal('');

  @ViewChild('searchInput') searchInput: ElementRef<HTMLInputElement>;
  @ViewChild('searchOpener', {read: ElementRef}) searchOpener: ElementRef<HTMLElement>;

  private router = inject(Router);
  private api = inject(IotHubApiService);
  private actions = inject(IotHubActionsService);

  private installedTemplates: IotHubInstalledItem[] = [];
  private installedDeviceCounts: Record<string, number> = {};

  constructor() {
    this.load();
  }

  isInstalled(item: MpItemVersionView): boolean {
    return this.installedTemplates.some(installed => installed.itemId === item.itemId);
  }

  open(item: MpItemVersionView): void {
    const installed = item.type === ItemType.SOLUTION_TEMPLATE ? this.installedTemplates.find(i => i.itemId === item.itemId) : undefined;
    const count = item.type === ItemType.DEVICE ? this.installedDeviceCounts[item.itemId] || 0 : 0;
    this.actions.openItemDetail(item, installed, count).subscribe(result => {
      if (result === 'installed' || result === 'deleted') {
        this.loadInstalled();
      }
    });
  }

  browse(route: string): void {
    this.router.navigate(['/iot-hub', route]);
  }

  openSearch(): void {
    this.searching.set(true);
    setTimeout(() => this.searchInput?.nativeElement.focus());
  }

  closeSearch(focusOpener = true): void {
    this.query.set('');
    this.searching.set(false);
    if (focusOpener) {
      setTimeout(() => this.searchOpener?.nativeElement.focus());
    }
  }

  clearSearch(): void {
    this.query.set('');
    this.searchInput?.nativeElement.focus();
  }

  search(): void {
    const search = this.query().trim();
    if (search) {
      this.router.navigate(['/iot-hub/search'], {queryParams: {search}});
    } else {
      this.searchInput?.nativeElement.focus();
    }
  }

  onSearchKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.search();
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.closeSearch();
    }
  }

  onSearchFocusOut(event: FocusEvent): void {
    const field = (event.currentTarget as HTMLElement);
    if (!field.contains(event.relatedTarget as Node) && !this.query().trim()) {
      this.closeSearch(false);
    }
  }

  private load(): void {
    const config = {ignoreLoading: true, ignoreErrors: true};
    const sortOrder: SortOrder = {property: 'totalInstallCount', direction: Direction.DESC};
    const query = (type: ItemType) => new MpItemVersionQuery(new PageLink(SHOWN, 0, null, sortOrder), {type});
    forkJoin({
      templates: this.api.getPublishedVersions(query(ItemType.SOLUTION_TEMPLATE), config),
      devices: this.api.getPublishedVersions(query(ItemType.DEVICE), config)
    }).subscribe({
      next: ({templates, devices}) => {
        this.templates.set(templates.data.map((item, i) => this.tile(item, TEMPLATE_TINTS[i % TEMPLATE_TINTS.length], item.useCases?.[0] ?? '')));
        this.devices.set(devices.data.map((item, i) => this.tile(item, DEVICE_TINTS[i % DEVICE_TINTS.length], this.deviceMeta(item))));
        this.deviceTotal.set(devices.totalElements);
        this.loaded.set(true);
      },
      error: () => {
        this.failed.set(true);
        this.loaded.set(true);
      }
    });
    this.loadInstalled();
  }

  private loadInstalled(): void {
    const config = {ignoreLoading: true, ignoreErrors: true};
    forkJoin({
      templates: this.api.getInstalledItems(new PageLink(10000, 0), ItemType.SOLUTION_TEMPLATE, undefined, config),
      deviceCounts: this.api.getInstalledItemCounts(ItemType.DEVICE, config)
    }).subscribe({
      next: ({templates, deviceCounts}) => {
        this.installedTemplates = templates.data;
        this.installedDeviceCounts = deviceCounts;
        this.templates.update(tiles => [...tiles]);
      },
      error: () => {}
    });
  }

  private tile(item: MpItemVersionView, tint: string, meta: string): HubTile {
    return {item, tint: `linear-gradient(135deg, ${tint})`, image: this.preview(item), meta};
  }

  private deviceMeta(item: MpItemVersionView): string {
    const vendor = item.dataDescriptor?.vendor;
    const connectivity: string[] = item.dataDescriptor?.connectivity ?? [];
    const via = item.dataDescriptor?.hardwareType === 'Gateway' ? 'Gateway' : connectivity[connectivity.length - 1];
    return [vendor, via].filter(Boolean).join(' · ');
  }

  private preview(item: MpItemVersionView): string | null {
    if (!item.image) {
      return null;
    }
    const [path, params] = item.image.split('?');
    const withPreview = path.endsWith('/preview') ? path : `${path}/preview`;
    return this.api.resolveResourceUrl(params ? `${withPreview}?${params}` : withPreview);
  }
}
