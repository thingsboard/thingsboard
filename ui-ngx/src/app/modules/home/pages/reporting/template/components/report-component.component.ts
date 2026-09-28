// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ComponentRef,
  DestroyRef,
  Directive,
  ElementRef,
  EventEmitter,
  HostBinding,
  HostListener,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  Renderer2,
  SimpleChanges,
  viewChild,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { isLayoutReportComponentConfig, ReportComponentConfig } from '@shared/models/report-component.models';
import {
  pointsToPixels,
  ReportComponentContext,
  ReportComponentTypeData,
  reportComponentTypesData
} from '@home/pages/reporting/template/components/report-component.models';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { from } from 'rxjs';
import { ReportComponentsComponent } from '@home/pages/reporting/template/components/report-components.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TbReportFormat } from '@shared/models/report.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { isFunction } from '@core/utils';
import ITooltipsterInstance = JQueryTooltipster.ITooltipsterInstance;
import ITooltipsterGeoHelper = JQueryTooltipster.ITooltipsterGeoHelper;

export interface IReportComponent {
  selected: boolean;
  componentUpdated(): void;
}

@Component({
    selector: 'tb-report-component',
    templateUrl: './report-component.component.html',
    styleUrls: ['./report-component.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ReportComponentComponent implements IReportComponent, OnInit, AfterViewInit, OnChanges, OnDestroy {

  reportComponentElement = viewChild('reportComponentElement', {
    read: ElementRef<HTMLElement>,
  });

  @HostBinding('class')
  class = 'tb-report-component-host';

  @HostBinding('style.background')
  background: string;

  @HostBinding('style.border-width.pt')
  borderWidth: number;

  @HostBinding('style.border-radius.pt')
  borderRadius: number;

  @HostBinding('style.border-color')
  borderColor: string;

  @HostBinding('style.border-style')
  borderStyle = 'solid';

  @HostBinding('style.margin-left.pt')
  marginLeft: number;

  @HostBinding('style.margin-right.pt')
  marginRight: number;

  @HostBinding('style.margin-top.pt')
  marginTop: number;

  @HostBinding('style.margin-bottom.pt')
  marginBottom: number;

  @HostBinding('style.padding-left.pt')
  paddingLeft: number;

  @HostBinding('style.padding-right.pt')
  paddingRight: number;

  @HostBinding('style.padding-top.pt')
  paddingTop: number;

  @HostBinding('style.padding-bottom.pt')
  paddingBottom: number;

  @HostBinding('style.display')
  display = 'block';

  @HostBinding('style.position')
  position = 'relative';

  @Input()
  reportComponent: ReportComponentConfig;

  @Input()
  format: TbReportFormat;

  @Input()
  @coerceBoolean()
  innerComponent = false;

  @Input()
  dragging = false;

  @Input()
  scale = 1;

  @Input()
  parentScale = 1;

  @Input()
  width: number;

  @Input()
  pageMarginLeft: number;

  @Input()
  pageMarginRight: number;

  @Input()
  @coerceBoolean()
  last = false;

  @Input()
  readonly = false;

  @Input()
  context: ReportComponentContext;

  @Output()
  edit = new EventEmitter<ReportComponentConfig>();

  @Output()
  childEdit = new EventEmitter<ReportComponentConfig>();

  @Output()
  makeCopy = new EventEmitter();

  @Output()
  remove = new EventEmitter();

  @Output()
  childRemove = new EventEmitter<ReportComponentConfig>();

  @Output()
  childrenChanged = new EventEmitter();

  @ViewChild('reportPreviewContainer', {static: true}) reportPreviewContainer: TbAnchorComponent;

  typeData: ReportComponentTypeData;

  @HostBinding('class.tb-hover')
  hovered = false;

  @HostBinding('class.tb-selected')
  @Input()
  selected = false;

  @HostBinding('class.tb-child-selected')
  @Input()
  childSelected = false;

  @HostBinding('class.tb-report-component-container')
  reportComponentsContainer = false;

  public get isPlainFormat(): boolean {
    return this.format === TbReportFormat.CSV;
  }

  private editReportComponentTooltip: ITooltipsterInstance;

  private reportComponentPreview: AbstractReportComponentPreview;

  private reportComponentHeight = 0;

  private hostResize$: ResizeObserver;

  constructor(private reportComponents: ReportComponentsComponent,
              public elementRef: ElementRef<HTMLElement>,
              private container: ViewContainerRef,
              private renderer: Renderer2,
              private destroyRef: DestroyRef,
              private cd: ChangeDetectorRef) {}

  ngOnInit() {
    this.typeData = reportComponentTypesData.getReportComponentTypeData(this.reportComponent.type,
          this.reportComponent.subType);
    if (this.typeData) {
      const compRef = this.reportPreviewContainer.viewContainerRef.createComponent(this.typeData.previewComponent);
      this.reportComponentPreview = compRef.instance;
      this.reportComponentPreview.context = this.context;
      this.reportComponentPreview.scale = this.scale;
      this.reportComponentPreview.reportComponent = this.reportComponent;
      this.reportComponentPreview.format = this.format;
      if (this.typeData.previewContext) {
        for (const key of Object.keys(this.typeData.previewContext)) {
          this.reportComponentPreview[key] = this.typeData.previewContext[key];
        }
      }
      this.reportComponentPreview.contentResized.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateComponentLayout();
      });
    }
    if (isReportComponentContainer(this.reportComponentPreview)) {
      this.reportComponentsContainer = true;
      this.reportComponentPreview.componentEdit.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((component) => {
        this.childEdit.emit(component);
      });
      this.reportComponentPreview.componentRemoved.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((component) => {
        this.childRemove.emit(component);
      });
      this.reportComponentPreview.componentsChanged.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.childrenChanged.emit();
      });
    }
    this.initEditReportComponentTooltip();
    this.updateComponentLayout();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['scale', 'parentScale', 'width'].includes(propName)) {
          if (propName === 'scale') {
            this.reportComponentPreview.scale = this.scale;
          }
          this.updateComponentLayout();
        }
        if (['pageMarginLeft', 'pageMarginRight'].includes(propName)) {
          if (this.typeData.pageBreak) {
            this.updateComponentLayout();
          }
        }
      }
    }
  }

  ngAfterViewInit() {
    this.updateComponentLayout();
    this.hostResize$ = new ResizeObserver(() => {
      this.updateComponentSize();
    });
    this.hostResize$.observe(this.elementRef.nativeElement);
  }

  ngOnDestroy(): void {
    if (this.editReportComponentTooltip && !this.editReportComponentTooltip.status().destroyed) {
      this.editReportComponentTooltip.destroy();
    }
    if (this.hostResize$) {
      this.hostResize$.disconnect();
    }
  }

  @HostListener('click', ['$event'])
  onEdit(event: MouseEvent) {
    if (event) {
      event.stopPropagation();
    }
    this.edit.emit(this.reportComponent);
  }

  onCopy(event: MouseEvent) {
    if (event) {
      event.stopPropagation();
    }
    if (this.readonly) { return; }
    this.makeCopy.emit();
  }

  onRemove(event: MouseEvent) {
    if (event) {
      event.stopPropagation();
    }
    if (this.readonly) { return; }
    if (this.reportComponentsContainer) {
      if (isReportComponentContainer(this.reportComponentPreview)) {
        const children = this.reportComponentPreview.getAllChildReportComponentConfigs();
        for (const child of children) {
          this.childRemove.emit(child);
        }
      }
    }
    this.remove.emit();
  }

  @HostListener('mouseenter', ['$event'])
  mouseEnter(event: MouseEvent) {
    if (event.buttons === 0) {
      this.hovered = true;
    }
  }

  @HostListener('mouseleave', ['$event'])
  mouseLeave(_event: MouseEvent) {
    this.hovered = false;
  }

  private updateComponentSize() {
    const parentWidth = this.elementRef.nativeElement.getBoundingClientRect().width / this.parentScale;
    if (parentWidth > 0) {
      const border = pointsToPixels(this.borderWidth) * 2;
      const leftRightPaddings = pointsToPixels(this.paddingLeft) + pointsToPixels(this.paddingRight);
      this.renderer.setStyle(this.reportComponentElement().nativeElement, 'width', ((parentWidth - leftRightPaddings - border) / this.scale) + 'px');
      if (!this.innerComponent) {
        this.renderer.setStyle(this.reportComponentElement().nativeElement, 'transform', `scale(${this.scale})`);
      }
      const rect = this.reportComponentElement().nativeElement.getBoundingClientRect();
      const targetHeight = rect.height > 0 ? rect.height / this.parentScale : this.reportComponentHeight;
      this.reportComponentHeight = targetHeight;
      const topBottomPaddings = pointsToPixels(this.paddingTop) + pointsToPixels(this.paddingBottom);
      this.renderer.setStyle(this.elementRef.nativeElement, 'height', (targetHeight + topBottomPaddings + border) + 'px');
    }
  }

  private updateComponentLayout() {
    if (isLayoutReportComponentConfig(this.reportComponent) && !this.isPlainFormat) {
      this.background = this.reportComponent.background;
      this.borderWidth = (this.reportComponent.borderWidth || 0) * this.scale;
      this.borderRadius = (this.reportComponent.borderRadius || 0) * this.scale;
      this.borderColor = this.reportComponent.borderColor || 'transparent';
      this.marginLeft = (this.reportComponent.margins?.left || 0) * this.scale;
      this.marginRight = (this.reportComponent.margins?.right || 0) * this.scale;
      this.marginTop = (this.reportComponent.margins?.top || 0) * this.scale;
      this.marginBottom = (this.reportComponent.margins?.bottom || 0) * this.scale;
      this.paddingLeft = (this.reportComponent.paddings?.left || 0) * this.scale;
      this.paddingRight = (this.reportComponent.paddings?.right || 0) * this.scale;
      this.paddingTop = (this.reportComponent.paddings?.top || 0) * this.scale;
      this.paddingBottom = (this.reportComponent.paddings?.bottom || 0) * this.scale;
    } else {
      this.borderWidth = 0;
      this.paddingLeft = this.paddingRight = this.paddingTop = this.paddingBottom =
        this.marginLeft = this.marginRight = this.marginTop = this.marginBottom = 0;
      if (this.isPlainFormat && !this.last) {
        this.marginBottom = 20 * this.scale;
      }
      if (this.typeData.pageBreak) {
        this.marginLeft = -this.pageMarginLeft / this.scale;
        this.marginRight = -this.pageMarginRight / this.scale;
      }
    }
    this.updateComponentSize();
  }

  public componentUpdated() {
    if (this.reportComponentPreview) {
      this.reportComponentPreview.componentUpdated();
    }
    this.updateComponentLayout();
  }

  public childComponentUpdated(component: ReportComponentConfig): boolean {
    if (isReportComponentContainer(this.reportComponentPreview)) {
      return this.reportComponentPreview.childComponentUpdated(component);
    }
    return false;
  }

  public childComponentSelected(component: ReportComponentConfig): boolean {
    if (isReportComponentContainer(this.reportComponentPreview)) {
      this.childSelected = this.reportComponentPreview.childComponentSelected(component);
      return this.childSelected;
    }
    return false;
  }

  public deselect(): void {
    this.selected = false;
    this.childSelected = false;
    if (isReportComponentContainer(this.reportComponentPreview)) {
      this.reportComponentPreview.deselectChildren();
    }
  }

  private initEditReportComponentTooltip() {
    let componentRef: ComponentRef<EditReportComponentTooltipComponent>;
    const parent = this.reportComponents.element.nativeElement;
    from(import('tooltipster')).subscribe(() => {
      $(this.elementRef.nativeElement).tooltipster({
        parent: $(parent),
        delay: [0, 50],
        distance: 0,
        zIndex: 151,
        arrow: false,
        theme: ['tb-report-component-edit-tooltip'],
        interactive: true,
        trigger: 'hover',
        ignoreCloseOnScroll: true,
        side: ['top'],
        trackOrigin: true,
        trackerInterval: 25,
        content: '',
        checkOverflowY: (geo: ITooltipsterGeoHelper, bcr: DOMRect) => {
          return geo.origin.windowOffset.top < bcr.top || geo.origin.windowOffset.bottom < bcr.bottom;
        },
        functionPosition: (instance, helper, position) => {
          const clientRect = helper.origin.getBoundingClientRect();
          const container = parent.getBoundingClientRect();

          position.coord.left = Math.max(0, clientRect.right - position.size.width - container.left);
          position.target = clientRect.right;
          position.coord.top = position.coord.top - container.top;
          if (this.innerComponent) {
            position.coord.left -= (clientRect.width / 2 - position.size.width / 2);
            position.coord.top += position.size.height;
          }
          return position;
        },
        functionReady: (_instance, helper) => {
          this.editReportComponentTooltip.__scrollHandler({});
          const tooltipEl = $(helper.tooltip);
          tooltipEl.on('mouseenter', () => {
            this.hovered = true;
            this.cd.markForCheck();
          });
          tooltipEl.on('mouseleave', () => {
            this.hovered = false;
            this.cd.markForCheck();
          });
        },
        functionBefore: (_instance, helper) => {
          return (helper.event as any).buttons === 0;
        },
        functionAfter: () => {
          this.hovered = false;
          this.cd.markForCheck();
        }
      });
      this.editReportComponentTooltip = $(this.elementRef.nativeElement).tooltipster('instance');
      componentRef = this.container.createComponent(EditReportComponentTooltipComponent);
      componentRef.instance.container = this;
      componentRef.instance.viewInited.subscribe(() => {
        if (this.editReportComponentTooltip.status().open) {
          this.editReportComponentTooltip.reposition();
        }
      });
      this.editReportComponentTooltip.on('destroyed', () => {
        componentRef.destroy();
      });
      const parentElement = componentRef.instance.element.nativeElement;
      const content = parentElement.firstChild;
      parentElement.removeChild(content);
      parentElement.style.display = 'none';
      this.editReportComponentTooltip.content(content);
    });
  }
}

@Component({
    template: `
    <div class="tb-report-component-action-container">
      <div class="tb-report-component-actions-panel">
        <button mat-icon-button class="tb-mat-20"
                (click)="container.onEdit($event)"
                matTooltip="{{ (container.readonly ? 'action.view' : 'action.edit') | translate }}"
                matTooltipPosition="above">
          <tb-icon>{{ container.readonly ? 'visibility' : 'edit' }}</tb-icon>
        </button>
        @if (!container.readonly && !container.innerComponent) {
          <button mat-icon-button class="tb-mat-20"
                  (click)="container.onCopy($event)"
                  matTooltip="{{ 'action.duplicate' | translate }}"
                  matTooltipPosition="above">
            <tb-icon>content_copy</tb-icon>
          </button>
        }
        @if (!container.readonly) {
          <button mat-icon-button class="tb-mat-20"
                  (click)="container.onRemove($event);"
                  matTooltip="{{ 'action.remove' | translate }}"
                  matTooltipPosition="above">
            <tb-icon>close</tb-icon>
          </button>
        }
      </div>
    </div>`,
    styles: [],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class EditReportComponentTooltipComponent implements AfterViewInit {

  @Input()
  container: ReportComponentComponent;

  @Output()
  viewInited = new EventEmitter();

  constructor(public element: ElementRef<HTMLElement>,
              public cd: ChangeDetectorRef) {
  }

  ngAfterViewInit() {
    this.viewInited.emit();
  }
}

@Directive()
export abstract class AbstractReportComponentPreview<C extends ReportComponentConfig = ReportComponentConfig> implements OnInit {

  @HostBinding('style.width')
  width = '100%';

  @Input()
  context: ReportComponentContext;

  @Input()
  reportComponent: C;

  @Input()
  format: TbReportFormat;

  @Input()
  scale = 1;

  @Output()
  contentResized = new EventEmitter();

  public get isPlainFormat(): boolean {
    return this.format === TbReportFormat.CSV;
  }

  protected cd = inject(ChangeDetectorRef);

  ngOnInit() {
    this.componentUpdated();
  }

  componentUpdated() {
    this.onComponentUpdated();
    this.cd.detectChanges();
  }

  protected onComponentUpdated() {}

}

@Directive()
export abstract class AbstractReportComponentPreviewContainer<C extends ReportComponentConfig = ReportComponentConfig>
  extends AbstractReportComponentPreview<C> {

  @Output()
  componentEdit = new EventEmitter<ReportComponentConfig>();

  @Output()
  componentsChanged = new EventEmitter();

  @Output()
  componentRemoved = new EventEmitter<ReportComponentConfig>();

  childComponentUpdated(reportComponent: ReportComponentConfig): boolean {
    const comp = this.findChildReportComponent(reportComponent);
    if (comp) {
      comp.componentUpdated();
      return true;
    }
    return false;
  }

  deselectChildren(): void {
    const reportComponents = this.getAllChildReportComponents();
    for (const comp of reportComponents) {
      comp.selected = false;
    }
  }

  childComponentSelected(reportComponent: ReportComponentConfig): boolean {
    const comp = this.findChildReportComponent(reportComponent);
    if (comp) {
      comp.selected = true;
      return true;
    }
    return false;
  }

  public abstract getAllChildReportComponentConfigs(): ReportComponentConfig[];

  protected abstract getAllChildReportComponents(): IReportComponent[];

  protected abstract findChildReportComponent(reportComponent: ReportComponentConfig): IReportComponent | undefined;

}

export const isReportComponentContainer = (component: AbstractReportComponentPreview): component is AbstractReportComponentPreviewContainer => {
  const previewComponent = (component as any);
  return previewComponent &&
    previewComponent.childComponentUpdated && isFunction(previewComponent.childComponentUpdated);
};
