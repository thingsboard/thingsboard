import { ChangeDetectionStrategy, Component, forwardRef, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import {
  noLeadTrailSpacesRegex,
  PortLimits, SocketConfig,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-socket-config',
  templateUrl: './socket-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SocketConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SocketConfigComponent),
      multi: true
    }
  ]
})
export class SocketConfigComponent implements ControlValueAccessor, Validator, OnDestroy {
  socketConfigFormGroup: UntypedFormGroup;
  portLimits = PortLimits;

  private onChange: (value: string) => void;
  private onTouched: () => void;

  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    this.socketConfigFormGroup = this.fb.group({
      address: ['127.0.0.1', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]],
      port: ['50000', [Validators.required, Validators.min(PortLimits.MIN), Validators.max(PortLimits.MAX)]],
      bufferSize: ['1024', [Validators.required, Validators.pattern(noLeadTrailSpacesRegex)]]
    });

    this.socketConfigFormGroup.valueChanges.subscribe(value => {
      this.onChange(value);
      this.onTouched();
    });
  }

  get portErrorTooltip(): string {
    if (this.socketConfigFormGroup.get('port').hasError('required')) {
      return this.translate.instant('gateway.port-required');
    } else if (
      this.socketConfigFormGroup.get('port').hasError('min') ||
      this.socketConfigFormGroup.get('port').hasError('max')
    ) {
      return this.translate.instant('gateway.port-limits-error',
        {min: PortLimits.MIN, max: PortLimits.MAX});
    }
    return '';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(socketConfig: SocketConfig): void {
    this.socketConfigFormGroup.patchValue(socketConfig, {emitEvent: false});
  }

  validate(): ValidationErrors | null {
    return this.socketConfigFormGroup.valid ? null : {
      socketConfigFormGroup: {valid: false}
    };
  }
}
