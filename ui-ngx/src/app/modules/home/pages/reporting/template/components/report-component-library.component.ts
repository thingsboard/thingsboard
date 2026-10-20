// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  Component,
  ElementRef, HostBinding,
  Input,
  OnChanges, OnDestroy,
  OnInit,
  SimpleChanges, ViewChild,
  viewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  csvReportComponentTypes, ReportComponentContext,
  reportComponentGroups,
  ReportComponentLibraryGroup,
  ReportComponentLibraryItem,
  reportComponentsLibrary,
  reportComponentTypes
} from '@home/pages/reporting/template/components/report-component.models';
import { CdkDragMove, CdkDragRelease, CdkDragStart, CdkDropList } from '@angular/cdk/drag-drop';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ReportComponentType } from '@shared/models/report-component.models';
import { TbReportFormat } from '@shared/models/report.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'tb-report-component-library',
    templateUrl: './report-component-library.component.html',
    styleUrls: ['./report-component-library.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ReportComponentLibraryComponent implements OnInit, OnChanges, AfterViewInit, OnDestroy {

  @ViewChild(CdkDropList) dropList?: CdkDropList;

  @HostBinding('style.display')
  get display() {
    return this.reportComponentIds?.length ? 'block' : 'none';
  }

  @Input()
  @coerceBoolean()
  subReport = false;

  @Input()
  @coerceBoolean()
  nestedLibrary = false;

  @Input()
  format: TbReportFormat = TbReportFormat.PDF;

  @Input()
  group: ReportComponentLibraryGroup;

  @Input()
  filter: string;

  @Input()
  context: ReportComponentContext;

  libraryDragOriginList = viewChild('libraryDragOriginList', {
    read: ElementRef<HTMLElement>,
  });

  libraryDragActiveList = viewChild('libraryDragActiveList', {
    read: ElementRef<HTMLElement>,
  });

  reportComponentIds: string[];
  reportComponentsLibrary: Map<string, ReportComponentLibraryItem>;

  private reportComponentsTitleMap = new Map<string, string>();

  private itemDragEntered = false;

  constructor(private translate: TranslateService) {}

  ngOnInit() {
    if (this.group) {
      const ids = reportComponentGroups.get(this.group);
      this.reportComponentsLibrary = new Map<string, ReportComponentLibraryItem>();
      for (const id of ids) {
        this.reportComponentsLibrary.set(id, reportComponentsLibrary.get(id));
      }
    } else {
      this.reportComponentsLibrary = reportComponentsLibrary;
    }
    this.reportComponentsLibrary.forEach((item, id) => {
      this.reportComponentsTitleMap.set(id, (this.translate.instant(item.title) as string).toUpperCase());
    });
    this.updateReportComponentIds();
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['subReport', 'format', 'filter'].includes(propName)) {
          this.updateReportComponentIds();
        }
      }
    }
  }

  ngAfterViewInit() {
    if (this.dropList) {
      this.context.dragDropCtx.register(this.dropList);
    }
  }

  ngOnDestroy() {
    if (this.dropList) {
      this.context.dragDropCtx.deregister(this.dropList);
    }
  }

  dropListEnterPredicate(): boolean {
    return false;
  }

  dragStarted(event: CdkDragStart) {
    //event.source.getPlaceholderElement().style.height = Math.max(60, event.source.element.nativeElement.offsetHeight) + 'px';
    event.source.getPlaceholderElement().style.height = event.source.element.nativeElement.offsetHeight + 'px';
    document.body.style.cursor = 'grabbing';
    this.copyExistingLibItemsToActiveList();
    this.setActiveListVisibility(true);
  }

  dragEnded() {
    document.body.style.cursor = 'auto';
    this.setActiveListVisibility(false);
  }

  dragEntered() {
    this.itemDragEntered = true;
  }

  dragMoved(event: CdkDragMove) {
    this.context.dragDropCtx.dragMoved(event);
  }

  dragReleased(event: CdkDragRelease) {
    if (!this.itemDragEntered) {
      const origin = this.libraryDragOriginList();
      $('.tb-report-component-placeholder', origin.nativeElement).hide();
      $('.tb-report-component-library-drag-item', origin.nativeElement).show();
      this.setActiveListVisibility(false);
    }
    this.itemDragEntered = false;
    this.context.dragDropCtx.dragReleased(event);
  }

  private updateReportComponentIds() {
    let componentTypes = this.format === TbReportFormat.CSV ? csvReportComponentTypes : reportComponentTypes;
    this.reportComponentIds = [];
    if (this.subReport) {
      componentTypes = componentTypes.filter((type) => type !== ReportComponentType.SUB_REPORT );
    }
    const search = this.filter ? this.filter.trim().toUpperCase() : '';
    this.reportComponentsLibrary.forEach((item, id) => {
      if (componentTypes.includes(item.type) && this.reportComponentsTitleMap.get(id).includes(search)) {
        this.reportComponentIds.push(id);
      }
    });
  }

  private copyExistingLibItemsToActiveList() {
    const overlay = this.libraryDragActiveList();
    const origin = this.libraryDragOriginList();
    if (!overlay || !origin) {
      return;
    }
    overlay.nativeElement.innerHTML = origin.nativeElement.innerHTML;

    $('.tb-report-component-placeholder', overlay.nativeElement).hide();
    $('.tb-report-component-library-drag-item', overlay.nativeElement).show();
  }

  private setActiveListVisibility(visible: boolean) {
    const overlay = this.libraryDragActiveList();
    const origin = this.libraryDragOriginList();
    if (!overlay || !origin) {
      return;
    }
    const scrollTop = origin.nativeElement.scrollTop;
    overlay.nativeElement.style.display = visible ? 'flex' : 'none';
    origin.nativeElement.style.display = !visible ? 'flex' : 'none';
    if (visible) {
      overlay.nativeElement.scrollTop = scrollTop;
    }
  }
}
