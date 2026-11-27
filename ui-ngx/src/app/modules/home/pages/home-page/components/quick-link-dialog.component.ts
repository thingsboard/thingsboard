// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  Injector,
  signal,
  viewChild,
  ViewEncapsulation
} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ResourcesService } from '@core/services/resources.service';
import { getMaterialIcons, splitIconName } from '@shared/models/icon.models';
import { Subscription } from 'rxjs';
import { DocumentationLink } from '@shared/models/user-settings.models';
import { HomeToggleOption } from '@home/pages/home-page/components/home-toggle.component';
import {
  parseQuickWebLink,
  QUICK_LINK_ICONS,
  QuickLinkDialogResult,
  quickLinkIconFromUrl,
  quickLinkNameFromUrl,
  QuickLinkPage
} from '@home/pages/home-page/quick-links.models';

export interface QuickLinkDialogData {
  pages: QuickLinkPage[];
  used: string[];
  editing?: DocumentationLink;
}

type QuickLinkKind = 'page' | 'web';

const CRUMB_CAP = 8;
const ICON_RESULTS = 96;
const ICON_SIZE = 40;

@Component({
  selector: 'tb-quick-link-dialog',
  templateUrl: './quick-link-dialog.component.html',
  styleUrls: ['./quick-link-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuickLinkDialogComponent {

  readonly dialogRef = inject(MatDialogRef<QuickLinkDialogComponent, QuickLinkDialogResult>);
  readonly data = inject<QuickLinkDialogData>(MAT_DIALOG_DATA);

  private resources = inject(ResourcesService);
  private injector = inject(Injector);

  private pageInput = viewChild<ElementRef<HTMLInputElement>>('pageInput');
  private urlInput = viewChild<ElementRef<HTMLInputElement>>('urlInput');
  private pageList = viewChild<ElementRef<HTMLElement>>('pageList');
  private iconGrid = viewChild<ElementRef<HTMLElement>>('iconGrid');

  readonly editing = !!this.data.editing;

  readonly kindOptions: HomeToggleOption<QuickLinkKind>[] = [
    {value: 'page', label: 'home.quick-links.page'},
    {value: 'web', label: 'home.quick-links.web-link'}
  ];

  readonly kind = signal<QuickLinkKind>(this.editing ? 'web' : 'page');

  readonly query = signal('');
  readonly active = signal(0);

  private readonly used = new Set(this.data.used);

  readonly pageGroups = computed(() => {
    const query = this.query().trim().toLowerCase();
    if (!query) {
      return {groups: [this.data.pages], more: 0};
    }
    const byName = this.data.pages.filter(page => page.name.toLowerCase().includes(query));
    let byCrumb = this.data.pages.filter(page => !page.name.toLowerCase().includes(query) && page.crumb.toLowerCase().includes(query));
    let more = 0;
    if (byCrumb.length > CRUMB_CAP) {
      more = byCrumb.length - CRUMB_CAP;
      byCrumb = byCrumb.slice(0, CRUMB_CAP);
    }
    return {groups: [byName, byCrumb].filter(group => group.length), more};
  });

  readonly freePages = computed(() => this.pageGroups().groups.flat().filter(page => !this.used.has(page.id)));

  readonly activePage = computed(() => {
    const free = this.freePages();
    return free[Math.min(this.active(), free.length - 1)] ?? null;
  });

  readonly url = signal(this.data.editing?.link ?? '');
  readonly name = signal(this.data.editing?.name ?? '');
  readonly icon = signal(this.data.editing?.icon || 'link');
  private nameTouched = this.editing;
  private iconTouched = this.editing;

  readonly parsed = computed(() => parseQuickWebLink(this.url()));

  readonly insecure = computed(() => this.parsed().url?.protocol === 'http:');

  readonly canSubmit = computed(() => !!this.parsed().url && !!this.name().trim());

  readonly iconQuery = signal('');
  private readonly iconResults = signal<{query: string; icons: string[]} | null>(null);
  private iconSubscription: Subscription | null = null;

  readonly iconSearch = computed(() => {
    const query = this.iconQuery().trim();
    if (!query) {
      const chosen = this.icon();
      return {icons: QUICK_LINK_ICONS.includes(chosen) ? QUICK_LINK_ICONS : [chosen, ...QUICK_LINK_ICONS], total: -1};
    }
    const results = this.iconResults();
    if (results?.query !== query) {
      return {icons: [] as string[], total: null};
    }
    return {icons: results.icons.slice(0, ICON_RESULTS), total: results.icons.length};
  });

  constructor() {
    this.focusPane();
    inject(DestroyRef).onDestroy(() => this.iconSubscription?.unsubscribe());
  }

  setKind(kind: QuickLinkKind): void {
    this.kind.set(kind);
    this.focusPane();
  }

  onQuery(value: string): void {
    this.query.set(value);
    this.active.set(0);
  }

  clearQuery(): void {
    this.onQuery('');
    this.pageInput()?.nativeElement.focus();
  }

  freeIndex(page: QuickLinkPage): number {
    return this.freePages().indexOf(page);
  }

  onPageKeydown(event: KeyboardEvent): void {
    const count = this.freePages().length;
    if (event.key === 'ArrowDown' && count) {
      event.preventDefault();
      this.active.set((Math.min(this.active(), count - 1) + 1) % count);
      this.scrollActive();
    } else if (event.key === 'ArrowUp' && count) {
      event.preventDefault();
      this.active.set((Math.min(this.active(), count - 1) - 1 + count) % count);
      this.scrollActive();
    } else if (event.key === 'Enter' && this.activePage()) {
      event.preventDefault();
      this.addPage(this.activePage());
    }
  }

  addPage(page: QuickLinkPage): void {
    if (!this.used.has(page.id)) {
      this.dialogRef.close({page});
    }
  }

  onUrl(value: string): void {
    this.url.set(value);
    const url = this.parsed().url;
    if (url && !this.nameTouched) {
      this.name.set(quickLinkNameFromUrl(url));
    }
    if (url && !this.iconTouched) {
      this.icon.set(quickLinkIconFromUrl(url));
    }
  }

  onName(value: string): void {
    this.name.set(value);
    this.nameTouched = !!value.trim();
  }

  onIconQuery(value: string): void {
    this.iconQuery.set(value);
    this.searchIcons(value.trim());
  }

  clearIconQuery(input: HTMLInputElement): void {
    this.iconQuery.set('');
    input.focus();
  }

  private searchIcons(query: string): void {
    this.iconSubscription?.unsubscribe();
    if (!query) {
      return;
    }
    this.iconSubscription = getMaterialIcons(this.resources, ICON_RESULTS, true, query).subscribe(
      chunks => this.iconResults.set({query, icons: chunks.flat().map(icon => icon.name)})
    );
  }

  pick(icon: string): void {
    this.icon.set(icon);
    this.iconTouched = true;
  }

  onIconQueryKeydown(event: KeyboardEvent): void {
    const icons = this.iconSearch().icons;
    if (event.key === 'Enter') {
      event.preventDefault();
      if (icons[0]) {
        this.pick(icons[0]);
      }
    } else if (event.key === 'ArrowDown' && icons.length) {
      event.preventDefault();
      this.focusChosenIcon();
    }
  }

  onIconGridKeydown(event: KeyboardEvent, queryInput: HTMLInputElement): void {
    const icons = this.iconSearch().icons;
    const grid = this.iconGrid()?.nativeElement;
    if (!icons.length || !grid) {
      return;
    }
    const columns = Math.max(1, Math.floor((grid.clientWidth + 4) / ICON_SIZE));
    const steps: {[key: string]: number} = {ArrowRight: 1, ArrowLeft: -1, ArrowDown: columns, ArrowUp: -columns};
    const step = steps[event.key];
    if (step === undefined) {
      return;
    }
    event.preventDefault();
    const at = Math.max(0, icons.indexOf(this.icon()));
    if (event.key === 'ArrowUp' && at - columns < 0) {
      queryInput.focus();
      return;
    }
    this.pick(icons[Math.min(icons.length - 1, Math.max(0, at + step))]);
    this.focusChosenIcon();
  }

  iconLabel(icon: string): string {
    return splitIconName(icon)[1].replace(/[_-]/g, ' ');
  }

  iconTabIndex(icon: string, index: number): number {
    const icons = this.iconSearch().icons;
    return icon === this.icon() || (!icons.includes(this.icon()) && index === 0) ? 0 : -1;
  }

  submit(): void {
    const url = this.parsed().url;
    const name = this.name().trim();
    if (url && name) {
      this.dialogRef.close({web: {name, icon: this.icon(), link: url.href}});
    }
  }

  onFieldKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.submit();
    }
  }

  private focusPane(): void {
    afterNextRender(() => (this.kind() === 'page' ? this.pageInput() : this.urlInput())?.nativeElement.focus(), {injector: this.injector});
  }

  private scrollActive(): void {
    afterNextRender(() => this.pageList()?.nativeElement.querySelector('.tb-quick-link-page.active')?.scrollIntoView({block: 'nearest'}),
      {injector: this.injector});
  }

  private focusChosenIcon(): void {
    afterNextRender(() => this.iconGrid()?.nativeElement.querySelector<HTMLElement>('[tabindex="0"]')?.focus(), {injector: this.injector});
  }
}
