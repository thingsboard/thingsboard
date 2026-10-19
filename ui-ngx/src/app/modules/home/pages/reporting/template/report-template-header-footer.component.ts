// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  Output,
  Renderer2,
  viewChild,
  viewChildren,
  ViewEncapsulation
} from '@angular/core';
import { HeaderFooter, TbReportFormat } from '@shared/models/report.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ReportComponentConfig } from '@shared/models/report-component.models';
import { ReportComponentsComponent } from '@home/pages/reporting/template/components/report-components.component';
import { ReportComponentContext } from '@home/pages/reporting/template/components/report-component.models';

@Component({
    selector: 'tb-report-template-header-footer',
    templateUrl: './report-template-header-footer.component.html',
    styleUrls: ['./report-template-header-footer.component.scss', './report-components-container.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ReportTemplateHeaderFooterComponent {

  get currentHeader(): HeaderFooter {
    return this.headerToggleValue === 'header' ? this.headerFooter : this.headerFooter.firstPage;
  }

  @Input()
  @coerceBoolean()
  header = true;

  @Input()
  headerFooter: HeaderFooter;

  @Input()
  headerToggleValue: 'header' | 'firstPageHeader' = 'header';

  @Input()
  format: TbReportFormat;

  @Input()
  background: string;

  @Input()
  width: number;

  @Input()
  scale = 1;

  @Input()
  marginTop: number;

  @Input()
  marginLeft: number;

  @Input()
  marginRight: number;

  @Input()
  marginBottom: number;

  @Input()
  readonly = false;

  @Input()
  context: ReportComponentContext;

  @Output()
  componentsChanged = new EventEmitter();

  @Output()
  componentEdit = new EventEmitter<ReportComponentConfig>();

  @Output()
  componentRemoved = new EventEmitter<ReportComponentConfig>();

  @Output()
  currentHeaderChanged = new EventEmitter<'header' | 'firstPageHeader'>();

  @Output()
  enabledHeaderChanged = new EventEmitter();

  @Output()
  expandAnimationStart = new EventEmitter();

  @Output()
  expandAnimationFinish = new EventEmitter();

  reportComponentsComponents = viewChildren(ReportComponentsComponent);

  reportHeaderToolbarButtonsEl = viewChild('reportHeaderToolbarButtons', {
    read: ElementRef<HTMLElement>,
  });

  disableHeaderButtonEl = viewChild('disableHeaderButton', {
    read: ElementRef<HTMLElement>,
  });

  reportHeaderComponentsEl = viewChild('reportHeaderComponents', {
    read: ElementRef<HTMLElement>,
  });

  expanded = true;

  private expandAnimation = false;

  constructor(private renderer: Renderer2) {

  }

  public toggleCurrentHeader() {
    this.currentHeaderChanged.emit(this.headerToggleValue);
  }

  public disableHeader(): void {
    this.currentHeader.enabled = false;
    this.enabledHeaderChanged.emit();
  }

  public enableHeader(): void {
    this.currentHeader.enabled = true;
    this.enabledHeaderChanged.emit();
  }

  public reportComponentsChanged() {
    this.componentsChanged.emit();
  }

  public editReportComponent(reportComponent: ReportComponentConfig): void {
    this.componentEdit.emit(reportComponent);
  }

  public reportComponentRemoved(reportComponent: ReportComponentConfig) {
    this.componentRemoved.emit(reportComponent);
  }

  public toggleExpanded() {
    if (this.expandAnimation) {
      return;
    }
    this.expandAnimation = true;
    const expand = !this.expanded;
    this.expandAnimationStart.emit();
    if (expand) {
      const reportHeaderComponents = this.reportHeaderComponentsEl();
      const children: HTMLCollection = reportHeaderComponents.nativeElement.children;
      let height = 0;
      for (let i = 0; i < children.length; i++) {
        height += (children.item(i) as HTMLElement).offsetHeight;
      }
      const reportHeaderToolbarButtons = this.reportHeaderToolbarButtonsEl();
      this.renderer.setStyle(reportHeaderToolbarButtons.nativeElement, 'right', '0px');
      this.renderer.setStyle(reportHeaderComponents.nativeElement, 'maxHeight', height + 'px');
      setTimeout(() => {
        this.renderer.setStyle(reportHeaderComponents.nativeElement, 'maxHeight', null);
        this.renderer.setStyle(reportHeaderComponents.nativeElement, 'overflow', null);
        this.expandAnimation = false;
        this.expandAnimationFinish.emit();
      }, 400);
      this.expanded = expand;
    } else {
      const reportHeaderComponents = this.reportHeaderComponentsEl();
      const height = reportHeaderComponents.nativeElement.offsetHeight;
      this.renderer.setStyle(reportHeaderComponents.nativeElement, 'maxHeight', height + 'px');
      setTimeout(() => {
        const disableHeaderButton = this.disableHeaderButtonEl();
        const disabledButtonWidth: number = disableHeaderButton ? disableHeaderButton.nativeElement.offsetWidth : 0;
        const toolbarButtonsRight = -(disabledButtonWidth + 8);
        const reportHeaderToolbarButtons = this.reportHeaderToolbarButtonsEl();
        this.renderer.setStyle(reportHeaderToolbarButtons.nativeElement, 'right', toolbarButtonsRight + 'px');
        this.renderer.setStyle(reportHeaderComponents.nativeElement, 'overflow', 'hidden');
        this.renderer.setStyle(reportHeaderComponents.nativeElement, 'maxHeight', '0');
        setTimeout(() => {
          this.expandAnimation = false;
          this.expandAnimationFinish.emit();
        }, 400);
        this.expanded = expand;
      });
    }
  }

}
