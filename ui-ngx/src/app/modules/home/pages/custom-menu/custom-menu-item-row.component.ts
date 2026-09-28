// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  Renderer2,
  SimpleChanges,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import {
  CMItemLinkType,
  CMItemType,
  cmLinkTypeTranslations,
  CMScope,
  CustomMenuItem,
  HomeMenuItem,
  HomeMenuItemType,
  homeMenuItemTypeTranslations,
  isCustomMenuItem,
  isDefaultMenuItem,
  isHomeMenuItem,
  MenuItem
} from '@shared/models/custom-menu.models';
import { TbPopoverService } from '@shared/components/popover.service';
import { TranslateService } from '@ngx-translate/core';
import { MenuSection, menuSectionMap } from '@core/services/menu.models';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { coerceBoolean } from '@shared/decorators/coercion';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { MatButton, MatIconButton } from '@angular/material/button';
import { deepClone } from '@core/utils';
import { DefaultMenuItemPanelComponent } from '@home/pages/custom-menu/default-menu-item-panel.component';
import {
  AddCustomMenuItemDialogComponent,
  AddCustomMenuItemDialogData
} from '@home/pages/custom-menu/add-custom-menu-item.dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { CustomMenuItemPanelComponent } from '@home/pages/custom-menu/custom-menu-item-panel.component';
import { Observable, Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DisplayPopoverConfig } from '@shared/components/popover.models';

@Component({
    selector: 'tb-custom-menu-item-row',
    templateUrl: './custom-menu-item-row.component.html',
    styleUrls: ['./custom-menu-item-row.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CustomMenuItemRowComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => CustomMenuItemRowComponent),
            multi: true
        }
    ],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class CustomMenuItemRowComponent implements ControlValueAccessor, OnInit, OnDestroy, Validator, OnChanges {

  homeMenuItemTypeTranslations = homeMenuItemTypeTranslations;

  @Input()
  disabled: boolean;

  @Input()
  scope: CMScope;

  @Input()
  @coerceBoolean()
  showHidden = true;

  @Input()
  @coerceBoolean()
  childDrag = false;

  @Input()
  level = 0;

  @Input()
  maxIconNameBlockWidth = 256;

  @Input()
  hideItems: Observable<void>;

  @Output()
  menuItemRemoved = new EventEmitter();

  menuItemRowFormGroup: UntypedFormGroup;

  modelValue: MenuItem;

  isDefaultMenuItem = false;

  isHomeMenuItem = false;

  isCustomMenuItem = false;

  isCustomSection = false;

  isCleanupEnabled = false;

  iconNameBlockWidth = '256px';

  itemInfo: string;

  private defaultItemName: string;

  private defaultMenuSection: MenuSection;

  get itemName(): string {
    return this.isDefaultMenuItem ? this.defaultItemName : this.customTranslate.transform(this.menuItemRowFormGroup.get('name').value);
  }

  get itemNamePlaceholder(): string {
    return this.isDefaultMenuItem ? this.defaultItemName : this.translate.instant('custom-menu.menu-item-name');
  }

  get pagesDragEnabled(): boolean {
    return !this.disabled && this.visiblePagesControls().length > 1;
  }

  private propagateChange = (_val: any) => {};

  private hideItemsSubscription: Subscription;

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private translate: TranslateService,
              private customTranslate: CustomTranslatePipe,
              private dialog: MatDialog,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.menuItemRowFormGroup = this.fb.group({
      visible: [true, []],
      icon: [null, []],
      name: [null, []],
      pages: this.fb.array([])
    });
    this.menuItemRowFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
    this.updateIconNameBlockWidth();
    if (this.hideItems) {
      this.hideItemsSubscription = this.hideItems.subscribe(() => {
        if (!this.isHomeMenuItem && !this.disabled && this.modelValue.visible) {
          this.menuItemRowFormGroup.patchValue(
            {visible: false}, {emitEvent: true}
          );
        }
      });
    }
  }

  ngOnDestroy() {
    if (this.hideItemsSubscription) {
      this.hideItemsSubscription.unsubscribe();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['level', 'childDrag', 'maxIconNameBlockWidth'].includes(propName)) {
          this.updateIconNameBlockWidth();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.menuItemRowFormGroup.disable({emitEvent: false});
    } else {
      this.menuItemRowFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: MenuItem): void {
    this.modelValue = value;
    this.menuItemRowFormGroup.patchValue(
      {
        visible: value.visible,
        icon: value.icon,
        name: value.name
      }, {emitEvent: false}
    );
    if (value.pages?.length) {
      this.menuItemRowFormGroup.setControl('pages', this.preparePagesFormArray(value.pages), {emitEvent: false});
    }
    if (isDefaultMenuItem(this.modelValue)) {
      this.defaultMenuSection = menuSectionMap.get(this.modelValue.id);
      this.isDefaultMenuItem = true;
      this.defaultItemName = this.translate.instant(this.defaultMenuSection.name);
      if (!value.icon) {
        this.menuItemRowFormGroup.patchValue({
          icon: this.defaultMenuSection.icon
        }, {emitEvent: false});
      }
      if (isHomeMenuItem(this.modelValue)) {
        this.isHomeMenuItem = true;
      }
    } else if (isCustomMenuItem(this.modelValue)) {
      this.isCustomMenuItem = true;
      this.menuItemRowFormGroup.get('name').setValidators([Validators.required]);
      if (this.modelValue.menuItemType === CMItemType.SECTION) {
        this.isCustomSection = true;
      }
    }
    this.updateCleanupState();
    this.updateItemInfo();
    this.cd.markForCheck();
  }

  public validate(_c: UntypedFormControl) {
    if (!this.menuItemRowFormGroup.valid) {
      return {
        invalidMenuItem: true
      };
    }
    return null;
  }

  edit($event: Event, matButton: MatIconButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const config: DisplayPopoverConfig<any> = {
        trigger,
        renderer: this.renderer,
        componentType: undefined,
        hostView: this.viewContainerRef,
        preferredPlacement: ['right', 'bottom', 'top'],
        context: {
          disabled: this.disabled,
          scope: this.scope,
          menuItem: deepClone(this.modelValue)
        },
        showCloseButton: false,
        popoverContentStyle: {padding: '16px 24px'},
        isModal: true
      };
      if (this.isDefaultMenuItem) {
        config.componentType = DefaultMenuItemPanelComponent;
        const defaultMenuItemPanelPopover = this.popoverService.displayPopover(config);
        defaultMenuItemPanelPopover.tbComponentRef.instance.popover = defaultMenuItemPanelPopover;
        defaultMenuItemPanelPopover.tbComponentRef.instance.defaultMenuItemApplied.subscribe((menuItem) => {
          defaultMenuItemPanelPopover.hide();
          this.afterMenuItemEdit(menuItem);
        });
      } else {
        config.componentType = CustomMenuItemPanelComponent;
        config.context.subItem = this.level > 0;
        const customMenuItemPanelPopover = this.popoverService.displayPopover(config);
        customMenuItemPanelPopover.tbComponentRef.instance.popover = customMenuItemPanelPopover;
        customMenuItemPanelPopover.tbComponentRef.instance.customMenuItemApplied.subscribe((menuItem) => {
          customMenuItemPanelPopover.hide();
          this.afterMenuItemEdit(menuItem);
        });
      }
    }
  }

  cleanup() {
    this.menuItemRowFormGroup.patchValue(
      {
        visible: true,
        icon: this.defaultMenuSection.icon,
        name: null
      }, {emitEvent: false}
    );
    this.modelValue.visible = true;
    delete this.modelValue.icon;
    delete this.modelValue.name;
    if (this.isHomeMenuItem) {
      (this.modelValue as HomeMenuItem).homeType = HomeMenuItemType.DEFAULT;
      delete (this.modelValue as HomeMenuItem).dashboardId;
      delete (this.modelValue as HomeMenuItem).hideDashboardToolbar;
    }
    this.updateModel();
  }

  delete() {
    this.menuItemRemoved.emit();
  }

  pageDrop(event: CdkDragDrop<string[]>) {
    const pagesArray = this.pagesFormArray();
    const page = this.visiblePagesControls()[event.previousIndex];
    const previousIndex = this.actualPageIndex(event.previousIndex);
    const currentIndex = this.actualPageIndex(event.currentIndex);
    pagesArray.removeAt(previousIndex);
    pagesArray.insert(currentIndex, page);
  }

  visiblePagesControls(): Array<AbstractControl> {
    return this.pagesFormArray().controls.filter(c => this.showHidden || c.value.visible);
  }

  removeCustomPage(index: number) {
    this.pagesFormArray().removeAt(this.actualPageIndex(index));
  }

  addCustomPage(button: MatButton) {
    this.dialog.open<AddCustomMenuItemDialogComponent, AddCustomMenuItemDialogData,
      CustomMenuItem>(AddCustomMenuItemDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        subItem: true,
        scope: this.scope
      }
    }).afterClosed().subscribe((page) => {
      if (page) {
        const pagesArray = this.pagesFormArray();
        const pageControl = this.fb.control(page, []);
        pagesArray.push(pageControl);
        setTimeout(() => {
          button._elementRef.nativeElement.scrollIntoView({block: 'nearest'});
        }, 0);
      }
    });
  }

  private afterMenuItemEdit(menuItem: MenuItem) {
    this.menuItemRowFormGroup.patchValue(
      {
        visible: menuItem.visible,
        icon: menuItem.icon || (this.isDefaultMenuItem ? this.defaultMenuSection.icon : null),
        name: menuItem.name
      }, {emitEvent: false}
    );
    if (this.isHomeMenuItem) {
      const homeMenuItem = menuItem as HomeMenuItem;
      (this.modelValue as HomeMenuItem).homeType = homeMenuItem.homeType;
      if (homeMenuItem.homeType === HomeMenuItemType.DEFAULT) {
        delete (this.modelValue as HomeMenuItem).dashboardId;
        delete (this.modelValue as HomeMenuItem).hideDashboardToolbar;
      } else {
        (this.modelValue as HomeMenuItem).dashboardId = homeMenuItem.dashboardId;
        (this.modelValue as HomeMenuItem).hideDashboardToolbar = homeMenuItem.hideDashboardToolbar;
      }
    } else if (this.isCustomMenuItem) {
      const customMenuItem = menuItem as CustomMenuItem;
      if (customMenuItem.menuItemType === CMItemType.LINK) {
        (this.modelValue as CustomMenuItem).linkType = customMenuItem.linkType;
        if (customMenuItem.linkType === CMItemLinkType.URL) {
          (this.modelValue as CustomMenuItem).url = customMenuItem.url;
          (this.modelValue as CustomMenuItem).setAccessToken = customMenuItem.setAccessToken;
          delete (this.modelValue as CustomMenuItem).dashboardId;
          delete (this.modelValue as CustomMenuItem).hideDashboardToolbar;
        } else {
          (this.modelValue as CustomMenuItem).dashboardId = customMenuItem.dashboardId;
          (this.modelValue as CustomMenuItem).hideDashboardToolbar = customMenuItem.hideDashboardToolbar;
          delete (this.modelValue as CustomMenuItem).url;
          delete (this.modelValue as CustomMenuItem).setAccessToken;
        }
      }
    }
    this.updateModel();
  }

  private updateIconNameBlockWidth() {
    if (this.maxIconNameBlockWidth) {
      let width = this.maxIconNameBlockWidth;
      if (this.childDrag) {
        width -= 24;
      }
      width -= this.level * 16;
      this.iconNameBlockWidth = width + 'px';
    } else {
      this.iconNameBlockWidth = '100%';
    }
  }

  private updateCleanupState() {
    if (this.isDefaultMenuItem) {
      this.isCleanupEnabled = !this.modelValue.visible ||
        !!this.modelValue.name ||
        !!this.modelValue.icon ||
        (this.isHomeMenuItem && (this.modelValue as HomeMenuItem).homeType !== HomeMenuItemType.DEFAULT);
    }
  }

  private updateItemInfo() {
    if (this.isHomeMenuItem) {
      this.itemInfo = this.translate.instant(this.homeMenuItemTypeTranslations.get((this.modelValue as HomeMenuItem).homeType));
    } else if (this.isCustomMenuItem) {
      if ((this.modelValue as CustomMenuItem).menuItemType === CMItemType.SECTION) {
        this.itemInfo = this.translate.instant('custom-menu.item-type-section');
      } else {
        this.itemInfo = this.translate.instant('custom-menu.item-type-link') + ': ' +
          this.translate.instant(cmLinkTypeTranslations.get((this.modelValue as CustomMenuItem).linkType));
      }
    } else {
      this.itemInfo = '';
    }
  }

  private updateModel() {
    this.modelValue.visible = this.menuItemRowFormGroup.get('visible').value;
    const name = this.menuItemRowFormGroup.get('name').value;
    if (name) {
      this.modelValue.name = name;
    } else {
      delete this.modelValue.name;
    }
    let icon = this.menuItemRowFormGroup.get('icon').value;
    if (this.isDefaultMenuItem) {
      if (this.defaultMenuSection.icon === icon) {
        icon = null;
      }
    }
    if (icon) {
      this.modelValue.icon = icon;
    } else {
      delete this.modelValue.icon;
    }
    const pages: MenuItem[] = this.menuItemRowFormGroup.get('pages').value;
    if (pages?.length) {
      this.modelValue.pages = pages;
    } else {
      delete this.modelValue.pages;
    }
    this.updateCleanupState();
    this.updateItemInfo();
    this.propagateChange(this.modelValue);
  }

  private pagesFormArray(): UntypedFormArray {
    return this.menuItemRowFormGroup.get('pages') as UntypedFormArray;
  }

  private actualPageIndex(index: number): number {
    const page = this.visiblePagesControls()[index];
    return this.pagesFormArray().controls.indexOf(page);
  }

  private preparePagesFormArray(pages: MenuItem[]): UntypedFormArray {
    const pagesControls: Array<AbstractControl> = [];
    pages.forEach((page) => {
      pagesControls.push(this.fb.control(page, []));
    });
    return this.fb.array(pagesControls);
  }

}
