import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output, SimpleChanges
} from '@angular/core';
import { Color, ColorPickerControl } from '@iplab/ngx-color-picker';
import { Subscription } from 'rxjs';
export enum ColorType {
  hex = 'hex',
  hexa = 'hexa',
  rgba = 'rgba',
  rgb = 'rgb',
  hsla = 'hsla',
  hsl = 'hsl',
  cmyk = 'cmyk'
}

@Component({
  selector: `tb-color-picker`,
  templateUrl: `./color-picker.component.html`,
  styleUrls: [`./color-picker.component.scss`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ColorPickerComponent implements OnInit, OnChanges, OnDestroy {

  public selectedPresentation = 0;
  public presentations = [ColorType.hex, ColorType.hexa, ColorType.rgb, ColorType.rgba, ColorType.hsl, ColorType.hsla];

  @Input()
  public color: string;

  @Input()
  public control: ColorPickerControl;

  @Output()
  public colorChange: EventEmitter<string> = new EventEmitter(false);

  private subscriptions: Array<Subscription> = [];

  constructor(private readonly cdr: ChangeDetectorRef) {
  }

  public ngOnInit(): void {
    if (!this.control) {
      this.control = new ColorPickerControl();
    }

    if (this.color) {
      this.control.setValueFrom(this.color);
    }

    this.subscriptions.push(
      this.control.valueChanges.subscribe((value) => {
        this.cdr.markForCheck();
        this.colorChange.emit(this.getValueByType(value, this.control.initType));
      })
    );
  }

  changeColorFormat(event: Event) {
    this.colorChange.emit(this.getValueByType(this.control.value, this.control.initType));
  }

  public ngOnDestroy(): void {
    this.cdr.detach();
    this.subscriptions.forEach((subscription) => subscription.unsubscribe());
    this.subscriptions.length = 0;
  }

  public ngOnChanges(changes: SimpleChanges): void {
    if (this.color && this.control && this.getValueByType(this.control.value, this.control.initType) !== this.color) {
      this.control.setValueFrom(this.color);
    }
  }

  public changePresentation(): void {
    this.selectedPresentation =
      this.selectedPresentation === this.presentations.length - 1 ? 0 : this.selectedPresentation + 1;
  }

  getValueByType(color: Color, type: ColorType): string {
    switch (type) {
      case ColorType.hex:
        return color.toHexString();
      case ColorType.hexa:
        return color.toHexString(true);
      case ColorType.rgb:
        return color.toRgbString();
      case ColorType.rgba:
        return color.toRgbaString();
      case ColorType.hsl:
        return color.toHslString();
      case ColorType.hsla:
        return color.toHslaString();
      default:
        return color.toRgbaString();
    }
  }

}
