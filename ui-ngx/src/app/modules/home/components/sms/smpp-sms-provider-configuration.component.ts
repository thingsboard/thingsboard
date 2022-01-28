import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import {
  AwsSnsSmsProviderConfiguration, SmppSmsProviderConfiguration,
  SmsProviderConfiguration,
  SmsProviderType
} from '@shared/models/settings.models';
import { isDefinedAndNotNull } from '@core/utils';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-smpp-sms-provider-configuration',
  templateUrl: './smpp-sms-provider-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SmppSmsProviderConfigurationComponent),
    multi: true
  }]
})
export class SmppSmsProviderConfigurationComponent  implements ControlValueAccessor, OnInit{
  constructor(private fb: FormBuilder) {
  }
  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }
  @Input()
  disabled: boolean;

  smppSmsProviderConfigurationFormGroup: FormGroup;
  bindTypes = [
    {value: 'TX', name: 'Transmitter'},
    {value: 'RX', name: 'Receiver'},
    {value: 'TRX', name: 'Transciever'},
  ]

  sourcesTon = [
    {value: 0, name: 'Unknown'},
    {value: 1, name: 'International'},
    {value: 2, name: 'National'},
    {value: 3, name: 'Network Specific'},
    {value: 4, name: 'Subscriber Number'},
    {value: 5, name: 'Alphanumeric'},
    {value: 6, name: 'Abbreviated'}
  ]

  sourcesNpi = [
    {value: 0, name: 'Unknown'},
    {value: 1, name: 'ISDN/telephone numbering plan (E163/E164)'},
    {value: 3, name: 'Data numbering plan (X.121)'},
    {value: 4, name: 'Telex numbering plan (F.69)'},
    {value: 5, name: 'Land Mobile (E.212)'},
    {value: 8, name: 'National numbering plan'},
    {value: 9, name: 'Private numbering plan'},
    {value: 10, name: 'ERMES numbering plan (ETSI DE/PS 3 01-3)'},
    {value: 13, name: 'Internet (IP)'},
    {value: 18, name: 'WAP Client Id (to be defined by WAP Forum)'},
  ]

  destinationsTon = [
    {value: 0, name: 'Unknown'},
    {value: 1, name: 'International'},
    {value: 2, name: 'National'},
    {value: 3, name: 'Network Specific'},
    {value: 4, name: 'Subscriber Number'},
    {value: 5, name: 'Alphanumeric'},
    {value: 6, name: 'Abbreviated'},
  ]

  destinationsNpi = [
    {value: 0, name: 'Unknown'},
    {value: 1, name: 'ISDN/telephone numbering plan (E163/E164)'},
    {value: 3, name: 'Data numbering plan (X.121)'},
    {value: 4, name: 'Telex numbering plan (F.69)'},
    {value: 6, name: 'Land Mobile (E.212)'},
    {value: 8, name: 'National numbering plan'},
    {value: 9, name: 'Private numbering plan'},
    {value: 10, name: 'ERMES numbering plan (ETSI DE/PS 3 01-3)'},
    {value: 13, name: 'Internet (IP)'},
    {value: 18, name: 'WAP Client Id (to be defined by WAP Forum)'},
  ]

  codingSchemes = [
    {value: 0, name: 'SMSC Default Alphabet (ASCII for short and long code and to GSM for toll-free)'},
    {value: 1, name: 'IA5 (ASCII for short and long code, Latin 9 for toll-free (ISO-8859-9))'},
    {value: 2, name: 'Octet Unspecified (8-bit binary)'},
    {value: 3, name: 'Latin 1 (ISO-8859-1)'},
    {value: 4, name: 'Octet Unspecified (8-bit binary)'},
    {value: 5, name: 'JIS (X 0208-1990)'},
    {value: 6, name: 'Cyrillic (ISO-8859-5)'},
    {value: 7, name: 'Latin/Hebrew (ISO-8859-8)'},
    {value: 8, name: 'UCS2/UTF-16 (ISO/IEC-10646)'},
    {value: 9, name: 'Pictogram Encoding'},
    {value: 10, name: 'Music Codes (ISO-2022-JP)'},
    {value: 13, name: 'Extended Kanji JIS (X 0212-1990)'},
    {value: 14, name: 'Korean Graphic Character Set (KS C 5601/KS X 1001)'},
  ]

  private propagateChange = (v: any) => { };

  ngOnInit(): void {
    this.smppSmsProviderConfigurationFormGroup = this.fb.group({
      protocolVersion: [null, [Validators.required]],
      host: [null, [Validators.required]],
      port: [null, [Validators.required]],
      systemId: [null, [Validators.required]],
      password: [null, [Validators.required]],
      systemType: [null],
      bindType: [null, []],
      serviceType: [null, []],
      sourceAddress: [null, []],
      sourceTon: [null, []],
      sourceNpi: [null, []],
      destinationTon: [null, []],
      destinationNpi: [null, []],
      addressRange: [null, []],
      codingScheme: [null, []],
    });

    this.smppSmsProviderConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateValue();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.smppSmsProviderConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.smppSmsProviderConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AwsSnsSmsProviderConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.smppSmsProviderConfigurationFormGroup.patchValue(value, {emitEvent: false});
    }
  }

  private updateValue() {
    let configuration: SmppSmsProviderConfiguration = null;
    if (this.smppSmsProviderConfigurationFormGroup.valid) {
      configuration = this.smppSmsProviderConfigurationFormGroup.value;
      (configuration as SmsProviderConfiguration).type = SmsProviderType.SMPP;
    }
    this.propagateChange(configuration);
  }

}
