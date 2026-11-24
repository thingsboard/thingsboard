// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  Injector,
  signal,
  viewChild
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { map } from 'rxjs/operators';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { UserSettingsService } from '@core/http/user-settings.service';
import { MenuService } from '@core/services/menu.service';
import { MenuSection } from '@core/services/menu.models';
import { UtilsService } from '@core/services/utils.service';
import { ToastNotificationService } from '@core/services/toast-notification.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { DocumentationLink, HomeCardId, QuickLinks } from '@shared/models/user-settings.models';
import { HOME_CARDS } from '@home/pages/home-links/home/home-cards.models';
import {
  defaultQuickLinks,
  isQuickWebLinkId,
  QUICK_LINKS_MAX,
  QuickLinkDialogResult,
  quickLinkPages,
  QuickLinkView,
  quickWebLinkId
} from '@home/pages/home-links/home/quick-links.models';
import {
  QuickLinkDialogComponent,
  QuickLinkDialogData
} from '@home/pages/home-links/home/components/quick-link-dialog.component';

@Component({
  selector: 'tb-quick-links-home',
  templateUrl: './quick-links-home.component.html',
  styleUrls: ['./quick-links-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuickLinksHomeComponent {

  readonly card = HOME_CARDS[HomeCardId.QUICK_LINKS];
  readonly max = QUICK_LINKS_MAX;

  private store = inject(Store<AppState>);
  private userSettingsService = inject(UserSettingsService);
  private menuService = inject(MenuService);
  private utils = inject(UtilsService);
  private translate = inject(TranslateService);
  private toast = inject(ToastNotificationService);
  private dialog = inject(MatDialog);
  private docsUrl = inject(WhiteLabelingService).getDocsUrl();
  private host = inject<ElementRef<HTMLElement>>(ElementRef);
  private injector = inject(Injector);

  private columns = viewChild<ElementRef<HTMLElement>>('columns');

  private readonly settings = signal<QuickLinks | null>(null);

  readonly editing = signal(false);

  readonly pages = toSignal(this.menuService.menuSections().pipe(
    map(sections => quickLinkPages(sections, section => this.sectionName(section)))
  ), {initialValue: []});

  readonly loaded = computed(() => this.settings() !== null);

  readonly links = computed<QuickLinkView[]>(() => {
    const settings = this.settings();
    const pages = this.pages();
    const views: QuickLinkView[] = [];
    for (const id of settings?.links ?? []) {
      if (isQuickWebLinkId(id)) {
        const web = settings.webLinks?.[id];
        if (web) {
          views.push({id, name: web.name, icon: web.icon || 'link', web});
        }
      } else {
        const page = pages.find(p => p.id === id);
        if (page) {
          views.push({id, name: page.name, icon: page.icon, page});
        }
      }
    }
    return views;
  });

  readonly full = computed(() => this.links().length >= QUICK_LINKS_MAX);

  constructor() {
    this.userSettingsService.getQuickLinks().subscribe(quickLinks => this.settings.set(this.withDefaults(quickLinks)));
    effect(() => {
      this.links();
      this.editing();
      afterNextRender(() => this.fitColumns(), {injector: this.injector});
    });
    const observer = new ResizeObserver(() => this.fitColumns());
    observer.observe(this.host.nativeElement);
    inject(DestroyRef).onDestroy(() => observer.disconnect());
  }

  toggleEdit(): void {
    this.editing.set(!this.editing());
  }

  addLink(): void {
    this.openDialog().subscribe(result => {
      if (!result) {
        return;
      }
      const settings = this.current();
      if ('page' in result) {
        this.save({...settings, links: [...settings.links, result.page.id]});
        this.notify('home.quick-links.added', result.page.name);
      } else {
        const id = quickWebLinkId();
        this.save({links: [...settings.links, id], webLinks: {...settings.webLinks, [id]: result.web}});
        this.notify('home.quick-links.added', result.web.name);
      }
    });
  }

  editLink(link: QuickLinkView): void {
    this.openDialog(link.web).subscribe(result => {
      if (result && 'web' in result) {
        const settings = this.current();
        this.save({...settings, webLinks: {...settings.webLinks, [link.id]: result.web}});
        this.notify('home.quick-links.saved', result.web.name);
      }
    });
  }

  remove(link: QuickLinkView): void {
    const settings = this.current();
    const webLinks = {...settings.webLinks};
    delete webLinks[link.id];
    this.save({links: settings.links.filter(id => id !== link.id), webLinks});
    this.notify('home.quick-links.removed', link.name);
  }

  drop(event: CdkDragDrop<QuickLinkView[]>): void {
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    const visible = this.links().map(link => link.id);
    const [moved] = visible.splice(event.previousIndex, 1);
    visible.splice(event.currentIndex, 0, moved);
    const settings = this.current();
    this.save({...settings, links: [...visible, ...settings.links.filter(id => !visible.includes(id))]});
  }

  reset(): void {
    this.settings.set(this.withDefaults({}));
    this.userSettingsService.updateQuickLinks({links: null, webLinks: null}).subscribe();
    this.toast.dispatchNotification(this.toastConfig(this.translate.instant('home.quick-links.reset-done')));
  }

  private fitColumns(): void {
    const list = this.columns()?.nativeElement;
    if (!list) {
      return;
    }
    list.classList.remove('fit-one');
    const cut = Array.from(list.querySelectorAll<HTMLElement>('.tb-quick-link-text')).some(text => text.scrollWidth > text.clientWidth + 1);
    list.classList.toggle('fit-one', cut);
  }

  private openDialog(editing?: DocumentationLink) {
    return this.dialog.open<QuickLinkDialogComponent, QuickLinkDialogData, QuickLinkDialogResult>(QuickLinkDialogComponent, {
      panelClass: 'tb-quick-link-panel',
      autoFocus: false,
      restoreFocus: true,
      data: {
        pages: this.pages(),
        used: this.links().filter(link => !!link.page).map(link => link.id),
        editing
      }
    }).afterClosed();
  }

  private current(): Required<QuickLinks> {
    const settings = this.settings();
    return {links: [...(settings?.links ?? [])], webLinks: {...(settings?.webLinks ?? {})}};
  }

  private save(quickLinks: QuickLinks): void {
    this.settings.set(quickLinks);
    this.userSettingsService.updateQuickLinks(quickLinks).subscribe();
  }

  private withDefaults(quickLinks: QuickLinks | null): QuickLinks {
    if (quickLinks?.links) {
      return quickLinks;
    }
    return defaultQuickLinks(getCurrentAuthUser(this.store)?.authority, this.docsUrl, key => this.translate.instant(key));
  }

  private sectionName(section: MenuSection): string {
    return section.customTranslate ? this.utils.customTranslation(section.name, section.name) : this.translate.instant(section.name);
  }

  private notify(key: string, name: string): void {
    this.toast.dispatchNotification(this.toastConfig(this.translate.instant(key, {name})));
  }

  private toastConfig(message: string) {
    return {
      message,
      type: 'info' as const,
      duration: 4000,
      horizontalPosition: 'right' as const,
      verticalPosition: 'bottom' as const,
      panelClass: 'tb-home-toast'
    };
  }
}
