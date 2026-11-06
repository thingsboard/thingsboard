// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, ElementRef, inject, Input, OnDestroy, signal } from '@angular/core';
import { DashboardsOverview } from '@shared/models/solution-creator.models';

@Component({
  selector: 'tb-solution-dashboards',
  templateUrl: './solution-dashboards.component.html',
  styleUrls: ['./solution-pane.scss', './solution-dashboards.component.scss'],
  standalone: false
})
export class SolutionDashboardsComponent implements AfterViewInit, OnDestroy {

  @Input({required: true}) dashboards: DashboardsOverview[] = [];

  readonly markdownStyles = [
    '.tb-markdown-view h1, .tb-markdown-view h2, .tb-markdown-view h3 { display: none; }',
    '.tb-markdown-view h4, .tb-markdown-view h5 { font-size: 14px; line-height: 20px; font-weight: 500; margin: 16px 0 6px; padding: 0; }',
    '.tb-markdown-view p, .tb-markdown-view li { font-size: 14px; line-height: 20px; padding: 0; }',
    '.tb-markdown-view ul, .tb-markdown-view ol { padding-left: 20px; margin: 0; }'
  ];

  active = signal(0);

  wide = signal(false);

  private host = inject<ElementRef<HTMLElement>>(ElementRef);
  private resize: ResizeObserver;
  private scroller: HTMLElement | null = null;
  private scrollListener = () => this.onScroll();

  ngAfterViewInit(): void {
    this.resize = new ResizeObserver(() => this.wide.set(this.host.nativeElement.clientWidth >= 660 && this.dashboards?.length > 1));
    this.resize.observe(this.host.nativeElement);
    this.scroller = this.host.nativeElement.closest('.sol-pane-body') as HTMLElement;
    this.scroller?.addEventListener('scroll', this.scrollListener, {passive: true});
  }

  ngOnDestroy(): void {
    this.resize?.disconnect();
    this.scroller?.removeEventListener('scroll', this.scrollListener);
  }

  goTo(index: number): void {
    this.active.set(index);
    const target = this.host.nativeElement.querySelector(`[data-dash="${index}"]`) as HTMLElement;
    if (target && this.scroller) {
      const tabs = this.wide() ? 0 : (this.host.nativeElement.querySelector('.sol-tabs') as HTMLElement)?.offsetHeight ?? 0;
      this.scroller.scrollTo({
        top: this.scroller.scrollTop + target.getBoundingClientRect().top - this.scroller.getBoundingClientRect().top - tabs,
        behavior: 'smooth'
      });
    }
  }

  private onScroll(): void {
    if (!this.scroller) {
      return;
    }
    const top = this.scroller.getBoundingClientRect().top + (this.wide() ? 8 : 60);
    let current = 0;
    this.dashboards.forEach((_, index) => {
      const el = this.host.nativeElement.querySelector(`[data-dash="${index}"]`) as HTMLElement;
      if (el && el.getBoundingClientRect().top <= top) {
        current = index;
      }
    });
    if (current !== this.active()) {
      this.active.set(current);
    }
  }
}
