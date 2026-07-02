///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { BreakpointObserver } from '@angular/cdk/layout';
import { forkJoin, Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { MediaBreakpoints } from '@shared/models/constants';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MpItemVersionQuery, MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { getItemTypeIcon, ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubInstalledItem } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';
import { resolveIotHubItemImageUrl } from '@home/components/iot-hub/iot-hub-utils';

interface CategoryCard {
  type: ItemType;
  titleKey: string;
  icon: string;
  cssClass: string;
  image: string;
}

interface HeroTypeConfig {
  type: ItemType;
  labelKey: string;
  color: string;
  icon: string;
}

interface SearchResultGroup {
  type: ItemType;
  items: MpItemVersionView[];
}

const SEARCH_GROUP_ORDER: ItemType[] = [
  ItemType.DEVICE, ItemType.SOLUTION_TEMPLATE, ItemType.WIDGET,
  ItemType.CALCULATED_FIELD, ItemType.ALARM_RULE, ItemType.RULE_CHAIN
];

@Component({
  selector: 'tb-iot-hub-home',
  standalone: false,
  templateUrl: './iot-hub-home.component.html',
  styleUrls: ['./iot-hub-home.component.scss']
})
export class TbIotHubHomeComponent implements OnInit, OnDestroy {

  readonly ItemType = ItemType;

  searchText = '';
  searchResults: MpItemVersionView[] = [];
  searchResultGroups: SearchResultGroup[] = [];
  searchLoaded = false;
  searchLoading = false;
  @ViewChild(MatAutocompleteTrigger) searchAutoTrigger: MatAutocompleteTrigger;
  @ViewChild('searchInput', {read: ElementRef}) searchInputRef: ElementRef;
  private searchSubject = new Subject<string>();
  private searchSubscription: Subscription;


  heroTypes: HeroTypeConfig[] = [
    {
      type: ItemType.DEVICE, labelKey: 'item.type-device-plural', color: '#4b63cc',
      icon: 'assets/iot-hub/hero-device-cluster.svg'
    },
    {
      type: ItemType.SOLUTION_TEMPLATE, labelKey: 'item.type-solution-template-plural', color: '#2b6bb4',
      icon: 'assets/iot-hub/hero-solution-template-cluster.svg'
    },
    {
      type: ItemType.WIDGET, labelKey: 'item.type-widget-plural', color: '#2c9755',
      icon: 'assets/iot-hub/hero-widget-cluster.svg'
    },
    {
      type: ItemType.CALCULATED_FIELD, labelKey: 'item.type-calculated-field-plural', color: '#3cb4e0',
      icon: 'assets/iot-hub/hero-calculated-field-cluster.svg'
    },
    {
      type: ItemType.ALARM_RULE, labelKey: 'item.type-alarm-rule-plural', color: '#d66f2e',
      icon: 'assets/iot-hub/hero-alarm-rule-cluster.svg'
    },
    {
      type: ItemType.RULE_CHAIN, labelKey: 'item.type-rule-chain-plural', color: '#a95ae2',
      icon: 'assets/iot-hub/hero-rule-chain-cluster.svg'
    }
  ];

  activeHeroType: HeroTypeConfig = this.heroTypes[0];
  heroIconsReady = false;
  private heroInterval: any;

  categoryCards: CategoryCard[] = [
    { type: ItemType.DEVICE, titleKey: 'iot-hub.device-library', icon: 'memory', cssClass: 'category-devices', image: 'assets/iot-hub/category-device-library-img.png' },
    { type: ItemType.SOLUTION_TEMPLATE, titleKey: 'item.type-solution-template-plural', icon: 'integration_instructions', cssClass: 'category-solutions', image: 'assets/iot-hub/category-solution-templates-img.png' },
    { type: ItemType.WIDGET, titleKey: 'item.type-widget-plural', icon: 'widgets', cssClass: 'category-widgets', image: 'assets/iot-hub/category-widgets-img.png' },
    { type: ItemType.CALCULATED_FIELD, titleKey: 'item.type-calculated-field-plural', icon: 'functions', cssClass: 'category-calc-fields', image: 'assets/iot-hub/category-calculated-fields-img.png' },
    { type: ItemType.ALARM_RULE, titleKey: 'item.type-alarm-rule-plural', icon: 'notification_important', cssClass: 'category-alarm-rules', image: 'assets/iot-hub/category-alarm-rules-img.png' },
    { type: ItemType.RULE_CHAIN, titleKey: 'item.type-rule-chain-plural', icon: 'account_tree', cssClass: 'category-rule-chains', image: 'assets/iot-hub/category-rule-chains-img.png' }
  ];

  popularWidgets: MpItemVersionView[] = [];
  popularSolutionTemplates: MpItemVersionView[] = [];
  popularCalcFields: MpItemVersionView[] = [];
  popularAlarmRules: MpItemVersionView[] = [];
  popularRuleChains: MpItemVersionView[] = [];
  popularDevices: MpItemVersionView[] = [];

  installedWidgets: IotHubInstalledItem[] = [];
  installedSolutionTemplates: IotHubInstalledItem[] = [];
  installedDeviceCounts: Record<string, number> = {};
  installedCalcFieldCounts: Record<string, number> = {};
  installedAlarmRuleCounts: Record<string, number> = {};
  installedRuleChainCounts: Record<string, number> = {};
  installedItemsCount = 0;

  isLoading = true;
  hasError = false;
  private retryTimer: any = null;

  bigCardCount = 5;
  compactCardCount = 6;
  private breakpointSubscription: Subscription;

  constructor(
    private router: Router,
    private iotHubApiService: IotHubApiService,
    private iotHubActions: IotHubActionsService,
    private translate: TranslateService,
    private breakpointObserver: BreakpointObserver
  ) {}

  ngOnInit(): void {
    this.updateCardCounts();
    this.loadPopularItems();
    this.breakpointSubscription = this.breakpointObserver.observe([
      MediaBreakpoints['lt-sm'],
      MediaBreakpoints['lt-md'],
      MediaBreakpoints['lt-lg'],
      MediaBreakpoints['lt-xmd'],
      MediaBreakpoints['lt-xl'],
      MediaBreakpoints['gt-xxl']
    ]).subscribe(() => {
      const prev = { big: this.bigCardCount, compact: this.compactCardCount };
      this.updateCardCounts();
      if (this.bigCardCount !== prev.big || this.compactCardCount !== prev.compact) {
        this.loadPopularItems();
      }
    });
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(text => {
        this.searchLoading = true;
        const sortOrder: SortOrder = { property: 'totalInstallCount', direction: Direction.DESC };
        const pageLink = new PageLink(10, 0, text.trim() || null, sortOrder);
        const query = new MpItemVersionQuery(pageLink);
        return this.iotHubApiService.getPublishedVersions(query, { ignoreLoading: true });
      })
    ).subscribe(result => {
      this.searchResults = result.data;
      this.searchResultGroups = this.groupSearchResults(result.data);
      this.searchLoaded = true;
      this.searchLoading = false;
    });
    // One-tick delay so Angular renders icons in hidden state first, then triggers transition
    requestAnimationFrame(() => {
      this.heroIconsReady = true;
      this.startHeroCycle();
    });
  }

  ngOnDestroy(): void {
    this.stopHeroCycle();
    this.searchSubscription?.unsubscribe();
    this.breakpointSubscription?.unsubscribe();
  }

  onHeroTypeHover(config: HeroTypeConfig): void {
    this.stopHeroCycle();
    this.activeHeroType = config;
  }

  onHeroTypeLeave(): void {
    this.startHeroCycle();
  }

  private startHeroCycle(): void {
    this.stopHeroCycle();
    this.heroInterval = setInterval(() => {
      const idx = this.heroTypes.indexOf(this.activeHeroType);
      this.activeHeroType = this.heroTypes[(idx + 1) % this.heroTypes.length];
    }, 3000);
  }

  private stopHeroCycle(): void {
    if (this.heroInterval) {
      clearInterval(this.heroInterval);
      this.heroInterval = null;
    }
  }

  onSearchInput(): void {
    this.searchLoading = true;
    this.searchSubject.next(this.searchText || '');
  }

  onSearchFocus(): void {
    if (!this.searchLoaded) {
      this.searchLoading = true;
      this.searchSubject.next(this.searchText || '');
    }
  }

  searchDisplayFn = (value: any): string => {
    return typeof value === 'string' ? value : this.searchText || '';
  };

  clearSearch(): void {
    this.searchText = '';
    this.searchSubject.next('');
    this.searchInputRef?.nativeElement?.focus();
    setTimeout(() => this.searchAutoTrigger?.openPanel());
  }

  onSearch(): void {
    this.seeAllResults();
  }

  seeAllResults(): void {
    this.searchAutoTrigger?.closePanel();
    const search = this.searchText?.trim() || undefined;
    void this.router.navigate(['/iot-hub/search'], { queryParams: { search } });
  }

  isCompactType(type: ItemType): boolean {
    return type === ItemType.CALCULATED_FIELD
        || type === ItemType.ALARM_RULE
        || type === ItemType.RULE_CHAIN;
  }

  getCompactIcon(item: MpItemVersionView): string {
    return item.icon || getItemTypeIcon(item.type);
  }

  getItemImage(item: MpItemVersionView): string | null {
    return resolveIotHubItemImageUrl(item, this.iotHubApiService);
  }

  getItemTypeIcon(type: ItemType): string {
    return getItemTypeIcon(type);
  }

  getSearchGroupLabel(type: ItemType): string {
    const key = itemTypeTranslations.get(type);
    return key ? this.translate.instant(key + '-plural') : type;
  }

  navigateToBrowse(type: ItemType): void {
    void this.router.navigate(['/iot-hub', this.getTypeRoute(type)]);
  }

  hasAnyPopularItems(): boolean {
    return this.popularDevices.length > 0
      || this.popularSolutionTemplates.length > 0
      || this.popularWidgets.length > 0
      || this.popularCalcFields.length > 0
      || this.popularAlarmRules.length > 0
      || this.popularRuleChains.length > 0;
  }

  private getTypeRoute(type: ItemType): string {
    switch (type) {
      case ItemType.WIDGET: return 'widgets';
      case ItemType.DASHBOARD: return 'dashboards';
      case ItemType.SOLUTION_TEMPLATE: return 'solution-templates';
      case ItemType.CALCULATED_FIELD: return 'calculated-fields';
      case ItemType.ALARM_RULE: return 'alarm-rules';
      case ItemType.RULE_CHAIN: return 'rule-chains';
      case ItemType.DEVICE: return 'devices';
      default: return 'widgets';
    }
  }

  navigateToInstalledItems(): void {
    void this.router.navigate(['/iot-hub/installed']);
  }

  openItemDetail(item: MpItemVersionView): void {
    this.iotHubActions.openItemDetail(item, this.findInstalledItem(item), this.findInstalledItemsCount(item)).subscribe(result => {
      if (result === 'installed' || result === 'deleted') {
        this.reloadInstalledItems(item.type);
      }
    });
  }

  private reloadInstalledItems(type: ItemType): void {
    const config = { ignoreLoading: true };
    const pageLink = new PageLink(10000, 0);
    this.iotHubApiService.getInstalledItemsCount(null, config).subscribe(count => {
      this.installedItemsCount = count;
    });
    if (type === ItemType.WIDGET) {
      this.iotHubApiService.getInstalledItems(pageLink, ItemType.WIDGET, undefined, config).subscribe(data => {
        this.installedWidgets = data.data;
      });
    } else if (type === ItemType.SOLUTION_TEMPLATE) {
      this.iotHubApiService.getInstalledItems(pageLink, ItemType.SOLUTION_TEMPLATE, undefined, config).subscribe(data => {
        this.installedSolutionTemplates = data.data;
      });
    } else if (type === ItemType.DEVICE) {
      this.iotHubApiService.getInstalledItemCounts(ItemType.DEVICE, config).subscribe(counts => {
        this.installedDeviceCounts = counts;
      });
    } else if (type === ItemType.CALCULATED_FIELD) {
      this.iotHubApiService.getInstalledItemCounts(ItemType.CALCULATED_FIELD, config).subscribe(counts => {
        this.installedCalcFieldCounts = counts;
      });
    } else if (type === ItemType.ALARM_RULE) {
      this.iotHubApiService.getInstalledItemCounts(ItemType.ALARM_RULE, config).subscribe(counts => {
        this.installedAlarmRuleCounts = counts;
      });
    } else if (type === ItemType.RULE_CHAIN) {
      this.iotHubApiService.getInstalledItemCounts(ItemType.RULE_CHAIN, config).subscribe(counts => {
        this.installedRuleChainCounts = counts;
      });
    }
  }

  private findInstalledItem(item: MpItemVersionView): IotHubInstalledItem | undefined {
    switch (item.type) {
      case ItemType.WIDGET:
        return this.installedWidgets.find(i => i.itemId === item.itemId);
      case ItemType.SOLUTION_TEMPLATE:
        return this.installedSolutionTemplates.find(i => i.itemId === item.itemId);
      default:
        return undefined;
    }
  }

  findInstalledItemsCount(item: MpItemVersionView): number {
    switch (item.type) {
      case ItemType.DEVICE:
        return this.installedDeviceCounts[item.itemId] || 0;
      case ItemType.CALCULATED_FIELD:
        return this.installedCalcFieldCounts[item.itemId] || 0;
      case ItemType.ALARM_RULE:
        return this.installedAlarmRuleCounts[item.itemId] || 0;
      case ItemType.RULE_CHAIN:
        return this.installedRuleChainCounts[item.itemId] || 0;
      default:
        return 0;
    }
  }

  installItem(item: MpItemVersionView): void {
    this.iotHubActions.installItem(item).subscribe(result => {
      if (result === 'installed') {
        this.reloadInstalledItems(item.type);
      }
    });
  }

  updateItem(item: MpItemVersionView): void {
    const installedItem = this.findInstalledItem(item);
    this.iotHubActions.updateItem(installedItem, item.version, item.id as string).subscribe(result => {
      if (result === 'updated') {
        this.reloadInstalledItems(item.type);
      }
    });
  }

  navigateToCreator(creatorId: string): void {
    void this.router.navigate(['/iot-hub/creator', creatorId]);
  }

  getInstalledWidget(item: MpItemVersionView): IotHubInstalledItem | undefined {
    return this.installedWidgets.find(i => i.itemId === item.itemId);
  }

  getInstalledSolutionTemplate(item: MpItemVersionView): IotHubInstalledItem | undefined {
    return this.installedSolutionTemplates.find(i => i.itemId === item.itemId);
  }

  deleteInstalledItem(item: MpItemVersionView): void {
    const installedItem = this.findInstalledItem(item);
    if (!installedItem) { return; }
    this.iotHubActions.deleteItem(installedItem).subscribe((deleted) => {
      if (deleted) {
        this.installedItemsCount = Math.max(0, this.installedItemsCount - 1);
        if (item.type === ItemType.WIDGET) {
          this.installedWidgets = this.installedWidgets.filter(i => i.id.id !== installedItem.id.id);
        } else if (item.type === ItemType.SOLUTION_TEMPLATE) {
          this.installedSolutionTemplates = this.installedSolutionTemplates.filter(i => i.id.id !== installedItem.id.id);
        } else if (item.type === ItemType.DEVICE && this.installedDeviceCounts[item.itemId]) {
          this.installedDeviceCounts[item.itemId] = Math.max(0, this.installedDeviceCounts[item.itemId] - 1);
        } else if (item.type === ItemType.CALCULATED_FIELD && this.installedCalcFieldCounts[item.itemId]) {
          this.installedCalcFieldCounts[item.itemId] = Math.max(0, this.installedCalcFieldCounts[item.itemId] - 1);
        } else if (item.type === ItemType.ALARM_RULE && this.installedAlarmRuleCounts[item.itemId]) {
          this.installedAlarmRuleCounts[item.itemId] = Math.max(0, this.installedAlarmRuleCounts[item.itemId] - 1);
        } else if (item.type === ItemType.RULE_CHAIN && this.installedRuleChainCounts[item.itemId]) {
          this.installedRuleChainCounts[item.itemId] = Math.max(0, this.installedRuleChainCounts[item.itemId] - 1);
        }
      }
    });
  }

  openSignup(): void {
    window.open(this.iotHubApiService.baseUrl + '/signup', '_blank');
  }

  private updateCardCounts(): void {
    if (this.breakpointObserver.isMatched(MediaBreakpoints['lt-sm'])) {
      // ≤599px: 1-col big cards, 1-col compact
      this.bigCardCount = 2;
      this.compactCardCount = 4;
    } else if (this.breakpointObserver.isMatched(MediaBreakpoints['lt-md'])) {
      // ≤959px: 2-col big cards, 1-col compact
      this.bigCardCount = 4;
      this.compactCardCount = 4;
    } else if (this.breakpointObserver.isMatched(MediaBreakpoints['lt-lg'])) {
      // ≤1279px: 2-col big cards, 1-col compact
      this.bigCardCount = 4;
      this.compactCardCount = 4;
    } else if (this.breakpointObserver.isMatched(MediaBreakpoints['lt-xmd'])) {
      // ≤1599px: 3-col big cards, 2-col compact
      this.bigCardCount = 3;
      this.compactCardCount = 4;
    } else if (this.breakpointObserver.isMatched(MediaBreakpoints['lt-xl'])) {
      // ≤1919px: 4-col big cards, 3-col compact
      this.bigCardCount = 4;
      this.compactCardCount = 6;
    } else if (this.breakpointObserver.isMatched(MediaBreakpoints['gt-xxl'])) {
      // ≥2448px: 6-col big cards, 4-col compact
      this.bigCardCount = 6;
      this.compactCardCount = 8;
    } else {
      // ≥1920px: 5-col big cards, 3-col compact
      this.bigCardCount = 5;
      this.compactCardCount = 6;
    }
  }

  retryLoadPopularItems(): void {
    // Debounce frequent retry clicks: show the loading spinner
    // immediately and defer the actual loadPopularItems() call by
    // 350ms so rapid-fire clicks coalesce into a single request set.
    // hasError stays as-is until the next request actually succeeds.
    if (this.retryTimer != null) {
      clearTimeout(this.retryTimer);
    }
    this.isLoading = true;
    this.retryTimer = setTimeout(() => {
      this.retryTimer = null;
      this.loadPopularItems();
    }, 350);
  }

  private loadPopularItems(): void {
    if (this.retryTimer != null) {
      clearTimeout(this.retryTimer);
      this.retryTimer = null;
    }
    this.isLoading = true;
    // hasError is intentionally NOT reset here — the error UI stays
    // visible until the next forkJoin actually succeeds (cleared in
    // the `next` callback below).
    const sortOrder: SortOrder = { property: 'totalInstallCount', direction: Direction.DESC };
    const config = { ignoreLoading: true, ignoreErrors: true };
    const installedPageLink = new PageLink(10000, 0);

    const buildQuery = (type: ItemType, size: number): MpItemVersionQuery => {
      const pageLink = new PageLink(size, 0, null, sortOrder);
      return new MpItemVersionQuery(pageLink, { type });
    };

    forkJoin({
      widgets: this.iotHubApiService.getPublishedVersions(buildQuery(ItemType.WIDGET, this.bigCardCount), config),
      solutionTemplates: this.iotHubApiService.getPublishedVersions(buildQuery(ItemType.SOLUTION_TEMPLATE, this.bigCardCount), config),
      calcFields: this.iotHubApiService.getPublishedVersions(buildQuery(ItemType.CALCULATED_FIELD, this.compactCardCount), config),
      alarmRules: this.iotHubApiService.getPublishedVersions(buildQuery(ItemType.ALARM_RULE, this.compactCardCount), config),
      ruleChains: this.iotHubApiService.getPublishedVersions(buildQuery(ItemType.RULE_CHAIN, this.compactCardCount), config),
      devices: this.iotHubApiService.getPublishedVersions(buildQuery(ItemType.DEVICE, this.bigCardCount), config),
      installedWidgets: this.iotHubApiService.getInstalledItems(installedPageLink, ItemType.WIDGET, undefined, config),
      installedSolutionTemplates: this.iotHubApiService.getInstalledItems(installedPageLink, ItemType.SOLUTION_TEMPLATE, undefined, config),
      installedDeviceCounts: this.iotHubApiService.getInstalledItemCounts(ItemType.DEVICE, config),
      installedCalcFieldCounts: this.iotHubApiService.getInstalledItemCounts(ItemType.CALCULATED_FIELD, config),
      installedAlarmRuleCounts: this.iotHubApiService.getInstalledItemCounts(ItemType.ALARM_RULE, config),
      installedRuleChainCounts: this.iotHubApiService.getInstalledItemCounts(ItemType.RULE_CHAIN, config),
      installedCount: this.iotHubApiService.getInstalledItemsCount(null, config)
    }).subscribe({
      next: (results) => {
        this.popularWidgets = results.widgets.data;
        this.popularSolutionTemplates = results.solutionTemplates.data;
        this.popularCalcFields = results.calcFields.data;
        this.popularAlarmRules = results.alarmRules.data;
        this.popularRuleChains = results.ruleChains.data;
        this.popularDevices = results.devices.data;
        this.installedWidgets = results.installedWidgets.data;
        this.installedSolutionTemplates = results.installedSolutionTemplates.data;
        this.installedDeviceCounts = results.installedDeviceCounts;
        this.installedCalcFieldCounts = results.installedCalcFieldCounts;
        this.installedAlarmRuleCounts = results.installedAlarmRuleCounts;
        this.installedRuleChainCounts = results.installedRuleChainCounts;
        this.installedItemsCount = results.installedCount;
        this.isLoading = false;
        this.hasError = false;
      },
      error: () => {
        this.isLoading = false;
        this.hasError = true;
        this.popularWidgets = [];
        this.popularSolutionTemplates = [];
        this.popularCalcFields = [];
        this.popularAlarmRules = [];
        this.popularRuleChains = [];
        this.popularDevices = [];
      }
    });
  }

  private groupSearchResults(items: MpItemVersionView[]): SearchResultGroup[] {
    const groupMap = new Map<ItemType, MpItemVersionView[]>();
    for (const item of items) {
      if (!SEARCH_GROUP_ORDER.includes(item.type)) {
        continue;
      }
      let list = groupMap.get(item.type);
      if (!list) {
        list = [];
        groupMap.set(item.type, list);
      }
      list.push(item);
    }
    return SEARCH_GROUP_ORDER
      .filter(type => groupMap.has(type))
      .map(type => ({ type, items: groupMap.get(type) }));
  }
}
