// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { Observable, ReplaySubject, Subject, Subscription } from 'rxjs';
import { MenuSection } from '@core/services/menu.models';
import { MenuService } from '@core/services/menu.service';
import { TranslateService } from '@ngx-translate/core';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { distinctUntilChanged, map, share, switchMap } from 'rxjs/operators';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { Router } from '@angular/router';

interface GotoMenuLink {
  name: string;
  icon: string;
  breadcrumb: string;
  path: string;
  queryParams?: {[k: string]: any};
}

const CRUMB_CAP = 8;

interface GotoMenuResult {
  hasMatches: boolean;
  nameMatches: GotoMenuLink[];
  crumbMatches: GotoMenuLink[];
  crumbMore: number;
}

@Component({
  selector: 'tb-goto-menu',
  templateUrl: './goto-menu.component.html',
  styleUrls: ['./goto-menu.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class GotoMenuComponent implements OnInit, OnDestroy {

  @ViewChild('searchLinkInput', {static: false}) searchLinkInput: ElementRef;

  @Input()
  collapsed = false;

  @Output()
  searchBtnClicked = new EventEmitter<void>();

  @Output()
  linkClicked = new EventEmitter<void>();

  gotoMenuResult: GotoMenuResult;

  private allLinksObservable$: Observable<Array<GotoMenuLink>> = null;

  private searchSubject = new Subject<string>();
  private searchSubscription: Subscription;

  searchText = '';

  constructor(private menuService: MenuService,
              private translate: TranslateService,
              private customTranslate: CustomTranslatePipe,
              private router: Router,
              public wl: WhiteLabelingService) {
  }

  ngOnInit(): void {
    this.searchSubscription = this.searchSubject.pipe(
      distinctUntilChanged(),
      switchMap(text => {
        return this.fetchLinks(text);
      })
    ).subscribe(result => {
      this.gotoMenuResult = result;
    });
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  onSearchInput(): void {
    this.searchSubject.next(this.searchText || '');
  }

  clear() {
    this.searchText = '';
    this.searchSubject.next('');
  }

  searchBtnClick() {
    this.searchBtnClicked.emit();
    setTimeout(() => {
      this.searchLinkInput.nativeElement.blur();
      this.searchLinkInput.nativeElement.focus();
    });
  }

  searchDisplayFn = (value: any): string => {
    return typeof value === 'string' ? value : this.searchText || '';
  };

  gotoLink(link: GotoMenuLink): void {
    this.searchText = '';
    this.searchSubject.next('');
    this.linkClicked.emit();
    this.router.navigate([link.path], { queryParams: link.queryParams }).then();
  }

  private fetchLinks(searchText?: string): Observable<GotoMenuResult> {
    this.searchText = searchText;
    return this.allLinks().pipe(
      map((links) => this.filterLinks(links))
    );
  }

  private allLinks(): Observable<Array<GotoMenuLink>> {
    if (this.allLinksObservable$ === null) {
      this.allLinksObservable$ = this.menuService.menuSections().pipe(
        map((links) => {
          return this.toGotoMenus(links);
        }),
        share({
          connector: () => new ReplaySubject(1),
          resetOnError: false,
          resetOnComplete: false,
          resetOnRefCountZero: false
        })
      );
    }
    return this.allLinksObservable$;
  }

  private filterLinks(links: GotoMenuLink[]): GotoMenuResult {
    const query = this.searchText.trim().toLowerCase();
    const nameMatches: GotoMenuLink[] = [];
    let crumbMatches: GotoMenuLink[] = [];
    for (const link of links) {
      if (link.name.toLowerCase().includes(query)) {
        nameMatches.push(link);
      } else if (link.breadcrumb.toLowerCase().includes(query)) {
        crumbMatches.push(link);
      }
    }
    let crumbMore = 0;
    if (crumbMatches.length > CRUMB_CAP) {
      crumbMore = crumbMatches.length - CRUMB_CAP;
      crumbMatches = crumbMatches.slice(0, CRUMB_CAP);
    }
    return {
      hasMatches: !!nameMatches.length || !!crumbMatches.length,
      nameMatches,
      crumbMatches,
      crumbMore
    };
  }

  private toGotoMenus(sections: MenuSection[], result: GotoMenuLink[] = [], parentNames: string[] = []): GotoMenuLink[] {
    for (const section of sections) {
      const name = section.customTranslate ? this.customTranslate.transform(section.name)
        : this.translate.instant(section.name);
      if (section.type === 'link') {
        const breadcrumb = parentNames.join(' • ');
        const gotoLink: GotoMenuLink = {
          name,
          icon: section.outlinedIcon,
          path: section.path,
          queryParams: section.queryParams,
          breadcrumb
        };
        result.push(gotoLink);
      }
      if (section.pages?.length) {
        const parentLabels = [...parentNames, name];
        this.toGotoMenus(section.pages, result, parentLabels);
      }
    }
    return result;
  }
}
