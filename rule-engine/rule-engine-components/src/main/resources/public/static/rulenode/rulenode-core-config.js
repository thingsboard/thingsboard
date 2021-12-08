import * as i0 from '@angular/core';
import { Component, Pipe, ViewChild, forwardRef, Input, NgModule } from '@angular/core';
import * as i3$4 from '@shared/public-api';
import { RuleNodeConfigurationComponent, AttributeScope, telemetryTypeTranslations, ServiceType, AlarmSeverity, alarmSeverityTranslations, EntitySearchDirection, entitySearchDirectionTranslations, EntityType, PageComponent, MessageType, messageTypeNames, SharedModule, AggregationType, aggregationTranslations, alarmStatusTranslations, AlarmStatus } from '@shared/public-api';
import * as i1 from '@ngrx/store';
import * as i2 from '@angular/forms';
import { Validators, NgControl, NG_VALUE_ACCESSOR, NG_VALIDATORS, FormControl } from '@angular/forms';
import * as i3 from '@angular/material/form-field';
import * as i3$1 from '@angular/material/checkbox';
import * as i8 from '@angular/flex-layout/flex';
import * as i4 from '@ngx-translate/core';
import * as i11 from '@angular/material/input';
import * as i10 from '@angular/common';
import { CommonModule } from '@angular/common';
import * as i1$1 from '@angular/platform-browser';
import * as i4$1 from '@angular/material/select';
import * as i5 from '@angular/material/core';
import * as i4$2 from '@angular/material/expansion';
import * as i7 from '@shared/components/button/toggle-password.component';
import * as i8$1 from '@shared/components/file-input.component';
import * as i3$2 from '@shared/components/queue/queue-type-list.component';
import * as i3$3 from '@core/public-api';
import { isDefinedAndNotNull } from '@core/public-api';
import * as i5$1 from '@shared/components/js-func.component';
import * as i6 from '@angular/material/button';
import { ENTER, COMMA, SEMICOLON } from '@angular/cdk/keycodes';
import * as i5$2 from '@angular/material/chips';
import * as i6$1 from '@angular/material/icon';
import * as i7$1 from '@shared/components/entity/entity-type-select.component';
import * as i6$2 from '@shared/components/entity/entity-select.component';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import * as i9 from '@shared/components/tb-error.component';
import * as i9$1 from '@angular/flex-layout/extended';
import * as i12 from '@angular/material/tooltip';
import { distinctUntilChanged, startWith, map, mergeMap, share } from 'rxjs/operators';
import * as i7$2 from '@shared/components/tb-checkbox.component';
import * as i5$3 from '@home/components/sms/sms-provider-configuration.component';
import { HomeComponentsModule } from '@home/components/public-api';
import * as i7$3 from '@shared/components/relation/relation-type-autocomplete.component';
import * as i8$2 from '@shared/components/entity/entity-subtype-list.component';
import * as i7$4 from '@home/components/relation/relation-filters.component';
import { of } from 'rxjs';
import * as i7$5 from '@angular/material/autocomplete';
import * as i12$1 from '@shared/pipe/highlight.pipe';
import * as i8$3 from '@shared/components/entity/entity-autocomplete.component';
import * as i3$5 from '@shared/components/entity/entity-type-list.component';

class EmptyConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.emptyConfigForm;
    }
    onConfigurationSet(configuration) {
        this.emptyConfigForm = this.fb.group({});
    }
}
EmptyConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: EmptyConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
EmptyConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: EmptyConfigComponent, selector: "tb-node-empty-config", usesInheritance: true, ngImport: i0, template: '<div></div>', isInline: true });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: EmptyConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-node-empty-config',
                    template: '<div></div>',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class SafeHtmlPipe {
    constructor(sanitizer) {
        this.sanitizer = sanitizer;
    }
    transform(html) {
        return this.sanitizer.bypassSecurityTrustHtml(html);
    }
}
SafeHtmlPipe.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SafeHtmlPipe, deps: [{ token: i1$1.DomSanitizer }], target: i0.ɵɵFactoryTarget.Pipe });
SafeHtmlPipe.ɵpipe = i0.ɵɵngDeclarePipe({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SafeHtmlPipe, name: "safeHtml" });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SafeHtmlPipe, decorators: [{
            type: Pipe,
            args: [{
                    name: 'safeHtml',
                }]
        }], ctorParameters: function () { return [{ type: i1$1.DomSanitizer }]; } });

class AssignCustomerConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.assignCustomerConfigForm;
    }
    onConfigurationSet(configuration) {
        this.assignCustomerConfigForm = this.fb.group({
            customerNamePattern: [configuration ? configuration.customerNamePattern : null, [Validators.required, Validators.pattern(/.*\S.*/)]],
            createCustomerIfNotExists: [configuration ? configuration.createCustomerIfNotExists : false, []],
            customerCacheExpiration: [configuration ? configuration.customerCacheExpiration : null, [Validators.required, Validators.min(0)]]
        });
    }
    prepareOutputConfig(configuration) {
        configuration.customerNamePattern = configuration.customerNamePattern.trim();
        return configuration;
    }
}
AssignCustomerConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: AssignCustomerConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
AssignCustomerConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: AssignCustomerConfigComponent, selector: "tb-action-node-assign-to-customer-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"assignCustomerConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.customer-name-pattern</mat-label>\n    <input required matInput formControlName=\"customerNamePattern\">\n    <mat-error *ngIf=\"assignCustomerConfigForm.get('customerNamePattern').hasError('required') ||\n                      assignCustomerConfigForm.get('customerNamePattern').hasError('pattern')\">\n      {{ 'tb.rulenode.customer-name-pattern-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-checkbox fxFlex formControlName=\"createCustomerIfNotExists\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.create-customer-if-not-exists' | translate }}\n  </mat-checkbox>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.customer-cache-expiration</mat-label>\n    <input required type=\"number\" min=\"0\" step=\"1\" matInput formControlName=\"customerCacheExpiration\">\n    <mat-error *ngIf=\"assignCustomerConfigForm.get('customerCacheExpiration').hasError('required')\">\n      {{ 'tb.rulenode.customer-cache-expiration-required' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"assignCustomerConfigForm.get('customerCacheExpiration').hasError('min')\">\n      {{ 'tb.rulenode.customer-cache-expiration-range' | translate }}\n    </mat-error>\n    <mat-hint translate>tb.rulenode.customer-cache-expiration-hint</mat-hint>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: AssignCustomerConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-assign-to-customer-config',
                    templateUrl: './assign-customer-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class AttributesConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.attributeScopes = Object.keys(AttributeScope);
        this.telemetryTypeTranslationsMap = telemetryTypeTranslations;
    }
    configForm() {
        return this.attributesConfigForm;
    }
    onConfigurationSet(configuration) {
        this.attributesConfigForm = this.fb.group({
            scope: [configuration ? configuration.scope : null, [Validators.required]],
            notifyDevice: [configuration ? configuration.scope : true, []]
        });
    }
}
AttributesConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: AttributesConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
AttributesConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: AttributesConfigComponent, selector: "tb-action-node-attributes-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"attributesConfigForm\" fxLayout=\"column\">\n  <mat-form-field fxFlex class=\"mat-block\">\n    <mat-label translate>attribute.attributes-scope</mat-label>\n    <mat-select formControlName=\"scope\" required>\n      <mat-option *ngFor=\"let scope of attributeScopes\" [value]=\"scope\">\n        {{ telemetryTypeTranslationsMap.get(scope) | translate }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n  <div *ngIf=\"attributesConfigForm.get('scope').value === 'SHARED_SCOPE'\">\n    <mat-checkbox formControlName=\"notifyDevice\">\n      {{ 'tb.rulenode.notify-device' | translate }}\n    </mat-checkbox>\n    <div class=\"tb-hint\" style=\"padding-bottom: 16px;\" translate>tb.rulenode.notify-device-hint</div>\n  </div>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: AttributesConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-attributes-config',
                    templateUrl: './attributes-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

var OriginatorSource;
(function (OriginatorSource) {
    OriginatorSource["CUSTOMER"] = "CUSTOMER";
    OriginatorSource["TENANT"] = "TENANT";
    OriginatorSource["RELATED"] = "RELATED";
    OriginatorSource["ALARM_ORIGINATOR"] = "ALARM_ORIGINATOR";
})(OriginatorSource || (OriginatorSource = {}));
const originatorSourceTranslations = new Map([
    [OriginatorSource.CUSTOMER, 'tb.rulenode.originator-customer'],
    [OriginatorSource.TENANT, 'tb.rulenode.originator-tenant'],
    [OriginatorSource.RELATED, 'tb.rulenode.originator-related'],
    [OriginatorSource.ALARM_ORIGINATOR, 'tb.rulenode.originator-alarm-originator'],
]);
var PerimeterType;
(function (PerimeterType) {
    PerimeterType["CIRCLE"] = "CIRCLE";
    PerimeterType["POLYGON"] = "POLYGON";
})(PerimeterType || (PerimeterType = {}));
const perimeterTypeTranslations = new Map([
    [PerimeterType.CIRCLE, 'tb.rulenode.perimeter-circle'],
    [PerimeterType.POLYGON, 'tb.rulenode.perimeter-polygon'],
]);
var TimeUnit;
(function (TimeUnit) {
    TimeUnit["MILLISECONDS"] = "MILLISECONDS";
    TimeUnit["SECONDS"] = "SECONDS";
    TimeUnit["MINUTES"] = "MINUTES";
    TimeUnit["HOURS"] = "HOURS";
    TimeUnit["DAYS"] = "DAYS";
})(TimeUnit || (TimeUnit = {}));
const timeUnitTranslations = new Map([
    [TimeUnit.MILLISECONDS, 'tb.rulenode.time-unit-milliseconds'],
    [TimeUnit.SECONDS, 'tb.rulenode.time-unit-seconds'],
    [TimeUnit.MINUTES, 'tb.rulenode.time-unit-minutes'],
    [TimeUnit.HOURS, 'tb.rulenode.time-unit-hours'],
    [TimeUnit.DAYS, 'tb.rulenode.time-unit-days']
]);
var RangeUnit;
(function (RangeUnit) {
    RangeUnit["METER"] = "METER";
    RangeUnit["KILOMETER"] = "KILOMETER";
    RangeUnit["FOOT"] = "FOOT";
    RangeUnit["MILE"] = "MILE";
    RangeUnit["NAUTICAL_MILE"] = "NAUTICAL_MILE";
})(RangeUnit || (RangeUnit = {}));
const rangeUnitTranslations = new Map([
    [RangeUnit.METER, 'tb.rulenode.range-unit-meter'],
    [RangeUnit.KILOMETER, 'tb.rulenode.range-unit-kilometer'],
    [RangeUnit.FOOT, 'tb.rulenode.range-unit-foot'],
    [RangeUnit.MILE, 'tb.rulenode.range-unit-mile'],
    [RangeUnit.NAUTICAL_MILE, 'tb.rulenode.range-unit-nautical-mile']
]);
var EntityDetailsField;
(function (EntityDetailsField) {
    EntityDetailsField["TITLE"] = "TITLE";
    EntityDetailsField["COUNTRY"] = "COUNTRY";
    EntityDetailsField["STATE"] = "STATE";
    EntityDetailsField["ZIP"] = "ZIP";
    EntityDetailsField["ADDRESS"] = "ADDRESS";
    EntityDetailsField["ADDRESS2"] = "ADDRESS2";
    EntityDetailsField["PHONE"] = "PHONE";
    EntityDetailsField["EMAIL"] = "EMAIL";
    EntityDetailsField["ADDITIONAL_INFO"] = "ADDITIONAL_INFO";
})(EntityDetailsField || (EntityDetailsField = {}));
const entityDetailsTranslations = new Map([
    [EntityDetailsField.TITLE, 'tb.rulenode.entity-details-title'],
    [EntityDetailsField.COUNTRY, 'tb.rulenode.entity-details-country'],
    [EntityDetailsField.STATE, 'tb.rulenode.entity-details-state'],
    [EntityDetailsField.ZIP, 'tb.rulenode.entity-details-zip'],
    [EntityDetailsField.ADDRESS, 'tb.rulenode.entity-details-address'],
    [EntityDetailsField.ADDRESS2, 'tb.rulenode.entity-details-address2'],
    [EntityDetailsField.PHONE, 'tb.rulenode.entity-details-phone'],
    [EntityDetailsField.EMAIL, 'tb.rulenode.entity-details-email'],
    [EntityDetailsField.ADDITIONAL_INFO, 'tb.rulenode.entity-details-additional_info']
]);
var FetchMode;
(function (FetchMode) {
    FetchMode["FIRST"] = "FIRST";
    FetchMode["LAST"] = "LAST";
    FetchMode["ALL"] = "ALL";
})(FetchMode || (FetchMode = {}));
var SamplingOrder;
(function (SamplingOrder) {
    SamplingOrder["ASC"] = "ASC";
    SamplingOrder["DESC"] = "DESC";
})(SamplingOrder || (SamplingOrder = {}));
var SqsQueueType;
(function (SqsQueueType) {
    SqsQueueType["STANDARD"] = "STANDARD";
    SqsQueueType["FIFO"] = "FIFO";
})(SqsQueueType || (SqsQueueType = {}));
const sqsQueueTypeTranslations = new Map([
    [SqsQueueType.STANDARD, 'tb.rulenode.sqs-queue-standard'],
    [SqsQueueType.FIFO, 'tb.rulenode.sqs-queue-fifo'],
]);
const credentialsTypes = ['anonymous', 'basic', 'cert.PEM'];
const credentialsTypeTranslations = new Map([
    ['anonymous', 'tb.rulenode.credentials-anonymous'],
    ['basic', 'tb.rulenode.credentials-basic'],
    ['cert.PEM', 'tb.rulenode.credentials-pem']
]);
const azureIotHubCredentialsTypes = ['sas', 'cert.PEM'];
const azureIotHubCredentialsTypeTranslations = new Map([
    ['sas', 'tb.rulenode.credentials-sas'],
    ['cert.PEM', 'tb.rulenode.credentials-pem']
]);
var HttpRequestType;
(function (HttpRequestType) {
    HttpRequestType["GET"] = "GET";
    HttpRequestType["POST"] = "POST";
    HttpRequestType["PUT"] = "PUT";
    HttpRequestType["DELETE"] = "DELETE";
})(HttpRequestType || (HttpRequestType = {}));
const ToByteStandartCharsetTypes = [
    'US-ASCII',
    'ISO-8859-1',
    'UTF-8',
    'UTF-16BE',
    'UTF-16LE',
    'UTF-16'
];
const ToByteStandartCharsetTypeTranslations = new Map([
    ['US-ASCII', 'tb.rulenode.charset-us-ascii'],
    ['ISO-8859-1', 'tb.rulenode.charset-iso-8859-1'],
    ['UTF-8', 'tb.rulenode.charset-utf-8'],
    ['UTF-16BE', 'tb.rulenode.charset-utf-16be'],
    ['UTF-16LE', 'tb.rulenode.charset-utf-16le'],
    ['UTF-16', 'tb.rulenode.charset-utf-16'],
]);

class AzureIotHubConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.allAzureIotHubCredentialsTypes = azureIotHubCredentialsTypes;
        this.azureIotHubCredentialsTypeTranslationsMap = azureIotHubCredentialsTypeTranslations;
    }
    configForm() {
        return this.azureIotHubConfigForm;
    }
    onConfigurationSet(configuration) {
        this.azureIotHubConfigForm = this.fb.group({
            topicPattern: [configuration ? configuration.topicPattern : null, [Validators.required]],
            host: [configuration ? configuration.host : null, [Validators.required]],
            port: [configuration ? configuration.port : null, [Validators.required, Validators.min(1), Validators.max(65535)]],
            connectTimeoutSec: [configuration ? configuration.connectTimeoutSec : null,
                [Validators.required, Validators.min(1), Validators.max(200)]],
            clientId: [configuration ? configuration.clientId : null, [Validators.required]],
            cleanSession: [configuration ? configuration.cleanSession : false, []],
            ssl: [configuration ? configuration.ssl : false, []],
            credentials: this.fb.group({
                type: [configuration && configuration.credentials ? configuration.credentials.type : null, [Validators.required]],
                sasKey: [configuration && configuration.credentials ? configuration.credentials.sasKey : null, []],
                caCert: [configuration && configuration.credentials ? configuration.credentials.caCert : null, []],
                caCertFileName: [configuration && configuration.credentials ? configuration.credentials.caCertFileName : null, []],
                privateKey: [configuration && configuration.credentials ? configuration.credentials.privateKey : null, []],
                privateKeyFileName: [configuration && configuration.credentials ? configuration.credentials.privateKeyFileName : null, []],
                cert: [configuration && configuration.credentials ? configuration.credentials.cert : null, []],
                certFileName: [configuration && configuration.credentials ? configuration.credentials.certFileName : null, []],
                password: [configuration && configuration.credentials ? configuration.credentials.password : null, []],
            })
        });
    }
    prepareOutputConfig(configuration) {
        const credentialsType = configuration.credentials.type;
        if (credentialsType === 'sas') {
            configuration.credentials = {
                type: credentialsType,
                sasKey: configuration.credentials.sasKey,
                caCert: configuration.credentials.caCert,
                caCertFileName: configuration.credentials.caCertFileName
            };
        }
        return configuration;
    }
    validatorTriggers() {
        return ['credentials.type'];
    }
    updateValidators(emitEvent) {
        const credentialsControl = this.azureIotHubConfigForm.get('credentials');
        const credentialsType = credentialsControl.get('type').value;
        if (emitEvent) {
            credentialsControl.reset({ type: credentialsType }, { emitEvent: false });
        }
        credentialsControl.get('sasKey').setValidators([]);
        credentialsControl.get('privateKey').setValidators([]);
        credentialsControl.get('privateKeyFileName').setValidators([]);
        credentialsControl.get('cert').setValidators([]);
        credentialsControl.get('certFileName').setValidators([]);
        switch (credentialsType) {
            case 'sas':
                credentialsControl.get('sasKey').setValidators([Validators.required]);
                break;
            case 'cert.PEM':
                credentialsControl.get('privateKey').setValidators([Validators.required]);
                credentialsControl.get('privateKeyFileName').setValidators([Validators.required]);
                credentialsControl.get('cert').setValidators([Validators.required]);
                credentialsControl.get('certFileName').setValidators([Validators.required]);
                break;
        }
        credentialsControl.get('sasKey').updateValueAndValidity({ emitEvent });
        credentialsControl.get('privateKey').updateValueAndValidity({ emitEvent });
        credentialsControl.get('privateKeyFileName').updateValueAndValidity({ emitEvent });
        credentialsControl.get('cert').updateValueAndValidity({ emitEvent });
        credentialsControl.get('certFileName').updateValueAndValidity({ emitEvent });
    }
}
AzureIotHubConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: AzureIotHubConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
AzureIotHubConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: AzureIotHubConfigComponent, selector: "tb-action-node-azure-iot-hub-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"azureIotHubConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.topic</mat-label>\n    <input required matInput formControlName=\"topicPattern\">\n    <mat-error *ngIf=\"azureIotHubConfigForm.get('topicPattern').hasError('required')\">\n      {{ 'tb.rulenode.topic-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.hostname</mat-label>\n    <input required matInput formControlName=\"host\">\n    <mat-error *ngIf=\"azureIotHubConfigForm.get('host').hasError('required')\">\n      {{ 'tb.rulenode.hostname-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.device-id</mat-label>\n    <input required matInput formControlName=\"clientId\" autocomplete=\"new-clientId\">\n    <mat-error *ngIf=\"azureIotHubConfigForm.get('clientId').hasError('required')\">\n      {{ 'tb.rulenode.device-id-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-accordion multi>\n    <mat-expansion-panel class=\"tb-mqtt-credentials-panel-group\">\n      <mat-expansion-panel-header>\n        <mat-panel-title translate>tb.rulenode.credentials</mat-panel-title>\n        <mat-panel-description>\n          {{ azureIotHubCredentialsTypeTranslationsMap.get(azureIotHubConfigForm.get('credentials.type').value) | translate }}\n        </mat-panel-description>\n      </mat-expansion-panel-header>\n      <section formGroupName=\"credentials\" fxLayout=\"column\">\n        <mat-form-field class=\"mat-block\">\n          <mat-label translate>tb.rulenode.credentials-type</mat-label>\n          <mat-select formControlName=\"type\" required>\n            <mat-option *ngFor=\"let credentialsType of allAzureIotHubCredentialsTypes\" [value]=\"credentialsType\">\n              {{ azureIotHubCredentialsTypeTranslationsMap.get(credentialsType) | translate }}\n            </mat-option>\n          </mat-select>\n          <mat-error *ngIf=\"azureIotHubConfigForm.get('credentials.type').hasError('required')\">\n            {{ 'tb.rulenode.credentials-type-required' | translate }}\n          </mat-error>\n        </mat-form-field>\n        <section fxLayout=\"column\" [ngSwitch]=\"azureIotHubConfigForm.get('credentials.type').value\">\n          <ng-template ngSwitchCase=\"anonymous\">\n          </ng-template>\n          <ng-template ngSwitchCase=\"sas\">\n            <mat-form-field class=\"mat-block\">\n              <mat-label translate>tb.rulenode.sas-key</mat-label>\n              <input type=\"password\" required matInput formControlName=\"sasKey\" autocomplete=\"new-password\">\n              <tb-toggle-password matSuffix></tb-toggle-password>\n              <mat-error *ngIf=\"azureIotHubConfigForm.get('credentials.sasKey').hasError('required')\">\n                {{ 'tb.rulenode.sas-key-required' | translate }}\n              </mat-error>\n            </mat-form-field>\n            <tb-file-input formControlName=\"caCert\"\n                           inputId=\"caCertSelect\"\n                           [existingFileName]=\"azureIotHubConfigForm.get('credentials.caCertFileName').value\"\n                           (fileNameChanged)=\"azureIotHubConfigForm.get('credentials.caCertFileName').setValue($event)\"\n                           label=\"{{'tb.rulenode.azure-ca-cert' | translate}}\"\n                           noFileText=\"tb.rulenode.no-file\"\n                           dropLabel=\"{{'tb.rulenode.drop-file' | translate}}\">\n            </tb-file-input>\n          </ng-template>\n          <ng-template ngSwitchCase=\"cert.PEM\">\n            <tb-file-input formControlName=\"caCert\"\n                           inputId=\"caCertSelect\"\n                           [existingFileName]=\"azureIotHubConfigForm.get('credentials.caCertFileName').value\"\n                           (fileNameChanged)=\"azureIotHubConfigForm.get('credentials.caCertFileName').setValue($event)\"\n                           label=\"{{'tb.rulenode.azure-ca-cert' | translate}}\"\n                           noFileText=\"tb.rulenode.no-file\"\n                           dropLabel=\"{{'tb.rulenode.drop-file' | translate}}\">\n            </tb-file-input>\n            <tb-file-input formControlName=\"cert\"\n                           inputId=\"CertSelect\"\n                           [existingFileName]=\"azureIotHubConfigForm.get('credentials.certFileName').value\"\n                           (fileNameChanged)=\"azureIotHubConfigForm.get('credentials.certFileName').setValue($event)\"\n                           required\n                           requiredAsError\n                           label=\"{{'tb.rulenode.cert' | translate}}\"\n                           noFileText=\"tb.rulenode.no-file\"\n                           dropLabel=\"{{'tb.rulenode.drop-file' | translate}}\">\n            </tb-file-input>\n            <tb-file-input style=\"padding-bottom: 8px;\"\n                           formControlName=\"privateKey\"\n                           inputId=\"privateKeySelect\"\n                           [existingFileName]=\"azureIotHubConfigForm.get('credentials.privateKeyFileName').value\"\n                           (fileNameChanged)=\"azureIotHubConfigForm.get('credentials.privateKeyFileName').setValue($event)\"\n                           required\n                           requiredAsError\n                           label=\"{{'tb.rulenode.private-key' | translate}}\"\n                           noFileText=\"tb.rulenode.no-file\"\n                           dropLabel=\"{{'tb.rulenode.drop-file' | translate}}\">\n            </tb-file-input>\n            <mat-form-field class=\"mat-block\">\n              <mat-label translate>tb.rulenode.private-key-password</mat-label>\n              <input type=\"password\" matInput formControlName=\"password\" autocomplete=\"new-password\">\n              <tb-toggle-password matSuffix></tb-toggle-password>\n            </mat-form-field>\n          </ng-template>\n        </section>\n      </section>\n    </mat-expansion-panel>\n    </mat-accordion>\n</section>\n", styles: [":host .tb-mqtt-credentials-panel-group{margin:0 6px}:host .tb-hint.client-id{margin-top:-1.25em;max-width:-moz-fit-content;max-width:fit-content}\n"], components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$2.MatExpansionPanel, selector: "mat-expansion-panel", inputs: ["disabled", "expanded", "hideToggle", "togglePosition"], outputs: ["opened", "closed", "expandedChange", "afterExpand", "afterCollapse"], exportAs: ["matExpansionPanel"] }, { type: i4$2.MatExpansionPanelHeader, selector: "mat-expansion-panel-header", inputs: ["tabIndex", "expandedHeight", "collapsedHeight"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i7.TogglePasswordComponent, selector: "tb-toggle-password" }, { type: i8$1.FileInputComponent, selector: "tb-file-input", inputs: ["label", "accept", "noFileText", "inputId", "allowedExtensions", "dropLabel", "contentConvertFunction", "required", "requiredAsError", "disabled", "existingFileName", "readAsBinary", "workFromFileObj", "multipleFile"], outputs: ["fileNameChanged"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i4$2.MatAccordion, selector: "mat-accordion", inputs: ["multi", "displayMode", "togglePosition", "hideToggle"], exportAs: ["matAccordion"] }, { type: i4$2.MatExpansionPanelTitle, selector: "mat-panel-title" }, { type: i4$2.MatExpansionPanelDescription, selector: "mat-panel-description" }, { type: i2.FormGroupName, selector: "[formGroupName]", inputs: ["formGroupName"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i10.NgSwitch, selector: "[ngSwitch]", inputs: ["ngSwitch"] }, { type: i10.NgSwitchCase, selector: "[ngSwitchCase]", inputs: ["ngSwitchCase"] }, { type: i3.MatSuffix, selector: "[matSuffix]" }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: AzureIotHubConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-azure-iot-hub-config',
                    templateUrl: './azure-iot-hub-config.component.html',
                    styleUrls: ['./mqtt-config.component.scss']
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class CheckPointConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.serviceType = ServiceType.TB_RULE_ENGINE;
    }
    configForm() {
        return this.checkPointConfigForm;
    }
    onConfigurationSet(configuration) {
        this.checkPointConfigForm = this.fb.group({
            queueName: [configuration ? configuration.queueName : null, [Validators.required]]
        });
    }
}
CheckPointConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CheckPointConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
CheckPointConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: CheckPointConfigComponent, selector: "tb-action-node-check-point-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"checkPointConfigForm\" fxLayout=\"column\">\n  <tb-queue-type-list\n    required\n    [queueType]=\"serviceType\"\n    formControlName=\"queueName\">\n  </tb-queue-type-list>\n</section>\n", components: [{ type: i3$2.QueueTypeListComponent, selector: "tb-queue-type-list", inputs: ["required", "disabled", "queueType"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CheckPointConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-check-point-config',
                    templateUrl: './check-point-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class ClearAlarmConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb, nodeScriptTestService, translate) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.nodeScriptTestService = nodeScriptTestService;
        this.translate = translate;
    }
    configForm() {
        return this.clearAlarmConfigForm;
    }
    onConfigurationSet(configuration) {
        this.clearAlarmConfigForm = this.fb.group({
            alarmDetailsBuildJs: [configuration ? configuration.alarmDetailsBuildJs : null, [Validators.required]],
            alarmType: [configuration ? configuration.alarmType : null, [Validators.required]]
        });
    }
    testScript() {
        const script = this.clearAlarmConfigForm.get('alarmDetailsBuildJs').value;
        this.nodeScriptTestService.testNodeScript(script, 'json', this.translate.instant('tb.rulenode.details'), 'Details', ['msg', 'metadata', 'msgType'], this.ruleNodeId, 'rulenode/clear_alarm_node_script_fn').subscribe((theScript) => {
            if (theScript) {
                this.clearAlarmConfigForm.get('alarmDetailsBuildJs').setValue(theScript);
            }
        });
    }
    onValidate() {
        this.jsFuncComponent.validateOnSubmit();
    }
}
ClearAlarmConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: ClearAlarmConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }, { token: i3$3.NodeScriptTestService }, { token: i4.TranslateService }], target: i0.ɵɵFactoryTarget.Component });
ClearAlarmConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: ClearAlarmConfigComponent, selector: "tb-action-node-clear-alarm-config", viewQueries: [{ propertyName: "jsFuncComponent", first: true, predicate: ["jsFuncComponent"], descendants: true, static: true }], usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"clearAlarmConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title no-padding\">tb.rulenode.alarm-details-builder</label>\n  <tb-js-func #jsFuncComponent\n              formControlName=\"alarmDetailsBuildJs\"\n              functionName=\"Details\"\n              [functionArgs]=\"['msg', 'metadata', 'msgType']\"\n              helpId=\"rulenode/clear_alarm_node_script_fn\"\n              noValidate=\"true\">\n  </tb-js-func>\n  <div fxLayout=\"row\" style=\"padding-bottom: 16px;\">\n    <button mat-button mat-raised-button color=\"primary\" (click)=\"testScript()\">\n      {{ 'tb.rulenode.test-details-function' | translate }}\n    </button>\n  </div>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.alarm-type</mat-label>\n    <input required matInput formControlName=\"alarmType\">\n    <mat-error *ngIf=\"clearAlarmConfigForm.get('alarmType').hasError('required')\">\n      {{ 'tb.rulenode.alarm-type-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n</section>\n", components: [{ type: i5$1.JsFuncComponent, selector: "tb-js-func", inputs: ["functionName", "functionArgs", "validationArgs", "resultType", "disabled", "fillHeight", "editorCompleter", "globalVariables", "disableUndefinedCheck", "helpId", "noValidate", "required"] }, { type: i6.MatButton, selector: "button[mat-button], button[mat-raised-button], button[mat-icon-button],             button[mat-fab], button[mat-mini-fab], button[mat-stroked-button],             button[mat-flat-button]", inputs: ["disabled", "disableRipple", "color"], exportAs: ["matButton"] }, { type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: ClearAlarmConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-clear-alarm-config',
                    templateUrl: './clear-alarm-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }, { type: i3$3.NodeScriptTestService }, { type: i4.TranslateService }]; }, propDecorators: { jsFuncComponent: [{
                type: ViewChild,
                args: ['jsFuncComponent', { static: true }]
            }] } });

class CreateAlarmConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb, nodeScriptTestService, translate) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.nodeScriptTestService = nodeScriptTestService;
        this.translate = translate;
        this.alarmSeverities = Object.keys(AlarmSeverity);
        this.alarmSeverityTranslationMap = alarmSeverityTranslations;
        this.separatorKeysCodes = [ENTER, COMMA, SEMICOLON];
    }
    configForm() {
        return this.createAlarmConfigForm;
    }
    onConfigurationSet(configuration) {
        this.createAlarmConfigForm = this.fb.group({
            alarmDetailsBuildJs: [configuration ? configuration.alarmDetailsBuildJs : null, [Validators.required]],
            useMessageAlarmData: [configuration ? configuration.useMessageAlarmData : false, []],
            alarmType: [configuration ? configuration.alarmType : null, []],
            severity: [configuration ? configuration.severity : null, []],
            propagate: [configuration ? configuration.propagate : false, []],
            relationTypes: [configuration ? configuration.relationTypes : null, []],
            dynamicSeverity: false
        });
        this.createAlarmConfigForm.get('dynamicSeverity').valueChanges.subscribe((dynamicSeverity) => {
            if (dynamicSeverity) {
                this.createAlarmConfigForm.get('severity').patchValue('', { emitEvent: false });
            }
            else {
                this.createAlarmConfigForm.get('severity').patchValue(this.alarmSeverities[0], { emitEvent: false });
            }
        });
    }
    validatorTriggers() {
        return ['useMessageAlarmData'];
    }
    updateValidators(emitEvent) {
        const useMessageAlarmData = this.createAlarmConfigForm.get('useMessageAlarmData').value;
        if (useMessageAlarmData) {
            this.createAlarmConfigForm.get('alarmType').setValidators([]);
            this.createAlarmConfigForm.get('severity').setValidators([]);
        }
        else {
            this.createAlarmConfigForm.get('alarmType').setValidators([Validators.required]);
            this.createAlarmConfigForm.get('severity').setValidators([Validators.required]);
        }
        this.createAlarmConfigForm.get('alarmType').updateValueAndValidity({ emitEvent });
        this.createAlarmConfigForm.get('severity').updateValueAndValidity({ emitEvent });
    }
    testScript() {
        const script = this.createAlarmConfigForm.get('alarmDetailsBuildJs').value;
        this.nodeScriptTestService.testNodeScript(script, 'json', this.translate.instant('tb.rulenode.details'), 'Details', ['msg', 'metadata', 'msgType'], this.ruleNodeId, 'rulenode/create_alarm_node_script_fn').subscribe((theScript) => {
            if (theScript) {
                this.createAlarmConfigForm.get('alarmDetailsBuildJs').setValue(theScript);
            }
        });
    }
    removeKey(key, keysField) {
        const keys = this.createAlarmConfigForm.get(keysField).value;
        const index = keys.indexOf(key);
        if (index >= 0) {
            keys.splice(index, 1);
            this.createAlarmConfigForm.get(keysField).setValue(keys, { emitEvent: true });
        }
    }
    addKey(event, keysField) {
        const input = event.input;
        let value = event.value;
        if ((value || '').trim()) {
            value = value.trim();
            let keys = this.createAlarmConfigForm.get(keysField).value;
            if (!keys || keys.indexOf(value) === -1) {
                if (!keys) {
                    keys = [];
                }
                keys.push(value);
                this.createAlarmConfigForm.get(keysField).setValue(keys, { emitEvent: true });
            }
        }
        if (input) {
            input.value = '';
        }
    }
    onValidate() {
        this.jsFuncComponent.validateOnSubmit();
    }
}
CreateAlarmConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CreateAlarmConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }, { token: i3$3.NodeScriptTestService }, { token: i4.TranslateService }], target: i0.ɵɵFactoryTarget.Component });
CreateAlarmConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: CreateAlarmConfigComponent, selector: "tb-action-node-create-alarm-config", viewQueries: [{ propertyName: "jsFuncComponent", first: true, predicate: ["jsFuncComponent"], descendants: true, static: true }], usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"createAlarmConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title no-padding\">tb.rulenode.alarm-details-builder</label>\n  <tb-js-func #jsFuncComponent\n              formControlName=\"alarmDetailsBuildJs\"\n              functionName=\"Details\"\n              [functionArgs]=\"['msg', 'metadata', 'msgType']\"\n              helpId=\"rulenode/create_alarm_node_script_fn\"\n              noValidate=\"true\">\n  </tb-js-func>\n  <div fxLayout=\"row\" style=\"padding-bottom: 16px;\">\n    <button mat-button mat-raised-button color=\"primary\" (click)=\"testScript()\">\n      {{ 'tb.rulenode.test-details-function' | translate }}\n    </button>\n  </div>\n  <div fxLayout=\"column\" fxLayout.gt-sm=\"row\">\n    <mat-checkbox formControlName=\"useMessageAlarmData\" style=\"padding-bottom: 16px; flex: 1;\">\n      {{ 'tb.rulenode.use-message-alarm-data' | translate }}\n    </mat-checkbox>\n    <mat-checkbox  *ngIf=\"createAlarmConfigForm.get('useMessageAlarmData').value === false\" formControlName=\"dynamicSeverity\" style=\"padding-bottom: 16px; flex: 1;\">\n      {{ 'tb.rulenode.use-dynamically-change-the-severity-of-alar' | translate }}\n    </mat-checkbox>\n  </div>\n  <section fxLayout=\"column\" *ngIf=\"createAlarmConfigForm.get('useMessageAlarmData').value === false\">\n    <section fxLayout=\"column\" fxLayout.gt-sm=\"row\" fxLayoutGap.gt-sm=\"8px\">\n      <mat-form-field fxFlex style=\"padding-bottom: 32px;\">\n        <mat-label translate>tb.rulenode.alarm-type</mat-label>\n        <input required matInput formControlName=\"alarmType\">\n        <mat-error *ngIf=\"createAlarmConfigForm.get('alarmType').hasError('required')\">\n          {{ 'tb.rulenode.alarm-type-required' | translate }}\n        </mat-error>\n        <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n      </mat-form-field>\n        <mat-form-field fxFlex *ngIf = \"!createAlarmConfigForm.get('dynamicSeverity').value\">\n          <mat-label translate>tb.rulenode.alarm-severity</mat-label>\n          <mat-select formControlName=\"severity\" required>\n            <mat-option *ngFor=\"let severity of alarmSeverities\" [value]=\"severity\">\n              {{ alarmSeverityTranslationMap.get(severity) | translate }}\n            </mat-option>\n          </mat-select>\n          <mat-error *ngIf=\"createAlarmConfigForm.get('severity').hasError('required')\">\n            {{ 'tb.rulenode.alarm-severity-required' | translate }}\n          </mat-error>\n        </mat-form-field>\n        <mat-form-field fxFlex *ngIf = \"createAlarmConfigForm.get('dynamicSeverity').value\" style=\"padding-bottom: 32px;\">\n          <mat-label>    {{ 'tb.rulenode.alarm-severity' | translate }}</mat-label>\n          <input matInput formControlName=\"severity\" required>\n          <mat-error *ngIf=\"createAlarmConfigForm.get('severity').hasError('required')\">\n            {{ 'tb.rulenode.alarm-severity-required' | translate }}\n          </mat-error>\n          <mat-hint [innerHTML]=\"'tb.rulenode.alarm-severity-pattern-hint' | translate | safeHtml\"></mat-hint>\n        </mat-form-field>\n    </section>\n    <mat-checkbox formControlName=\"propagate\" style=\"padding-bottom: 16px;\">\n      {{ 'tb.rulenode.propagate' | translate }}\n    </mat-checkbox>\n    <section *ngIf=\"createAlarmConfigForm.get('propagate').value === true\">\n      <mat-form-field floatLabel=\"always\" class=\"mat-block\" style=\"padding-bottom: 16px;\">\n        <mat-label translate>tb.rulenode.relation-types-list</mat-label>\n        <mat-chip-list #relationTypesChipList>\n          <mat-chip\n            *ngFor=\"let key of createAlarmConfigForm.get('relationTypes').value;\"\n            (removed)=\"removeKey(key, 'relationTypes')\">\n            {{key}}\n            <mat-icon matChipRemove>close</mat-icon>\n          </mat-chip>\n          <input matInput type=\"text\" placeholder=\"{{'tb.rulenode.relation-types-list' | translate}}\"\n                 style=\"max-width: 200px;\"\n                 [matChipInputFor]=\"relationTypesChipList\"\n                 [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n                 (matChipInputTokenEnd)=\"addKey($event, 'relationTypes')\"\n                 [matChipInputAddOnBlur]=\"true\">\n        </mat-chip-list>\n        <mat-hint translate>tb.rulenode.relation-types-list-hint</mat-hint>\n      </mat-form-field>\n    </section>\n  </section>\n</section>\n", components: [{ type: i5$1.JsFuncComponent, selector: "tb-js-func", inputs: ["functionName", "functionArgs", "validationArgs", "resultType", "disabled", "fillHeight", "editorCompleter", "globalVariables", "disableUndefinedCheck", "helpId", "noValidate", "required"] }, { type: i6.MatButton, selector: "button[mat-button], button[mat-raised-button], button[mat-icon-button],             button[mat-fab], button[mat-mini-fab], button[mat-stroked-button],             button[mat-flat-button]", inputs: ["disabled", "disableRipple", "color"], exportAs: ["matButton"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i5$2.MatChipList, selector: "mat-chip-list", inputs: ["aria-orientation", "multiple", "compareWith", "value", "required", "placeholder", "disabled", "selectable", "tabIndex", "errorStateMatcher"], outputs: ["change", "valueChange"], exportAs: ["matChipList"] }, { type: i6$1.MatIcon, selector: "mat-icon", inputs: ["color", "inline", "svgIcon", "fontSet", "fontIcon"], exportAs: ["matIcon"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i5$2.MatChip, selector: "mat-basic-chip, [mat-basic-chip], mat-chip, [mat-chip]", inputs: ["color", "disableRipple", "tabIndex", "selected", "value", "selectable", "disabled", "removable"], outputs: ["selectionChange", "destroyed", "removed"], exportAs: ["matChip"] }, { type: i5$2.MatChipRemove, selector: "[matChipRemove]" }, { type: i5$2.MatChipInput, selector: "input[matChipInputFor]", inputs: ["matChipInputSeparatorKeyCodes", "placeholder", "id", "matChipInputFor", "matChipInputAddOnBlur", "disabled"], outputs: ["matChipInputTokenEnd"], exportAs: ["matChipInput", "matChipInputFor"] }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CreateAlarmConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-create-alarm-config',
                    templateUrl: './create-alarm-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }, { type: i3$3.NodeScriptTestService }, { type: i4.TranslateService }]; }, propDecorators: { jsFuncComponent: [{
                type: ViewChild,
                args: ['jsFuncComponent', { static: true }]
            }] } });

class CreateRelationConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.directionTypes = Object.keys(EntitySearchDirection);
        this.directionTypeTranslations = entitySearchDirectionTranslations;
        this.entityType = EntityType;
    }
    configForm() {
        return this.createRelationConfigForm;
    }
    onConfigurationSet(configuration) {
        this.createRelationConfigForm = this.fb.group({
            direction: [configuration ? configuration.direction : null, [Validators.required]],
            entityType: [configuration ? configuration.entityType : null, [Validators.required]],
            entityNamePattern: [configuration ? configuration.entityNamePattern : null, []],
            entityTypePattern: [configuration ? configuration.entityTypePattern : null, []],
            relationType: [configuration ? configuration.relationType : null, [Validators.required]],
            createEntityIfNotExists: [configuration ? configuration.createEntityIfNotExists : false, []],
            removeCurrentRelations: [configuration ? configuration.removeCurrentRelations : false, []],
            changeOriginatorToRelatedEntity: [configuration ? configuration.changeOriginatorToRelatedEntity : false, []],
            entityCacheExpiration: [configuration ? configuration.entityCacheExpiration : null, [Validators.required, Validators.min(0)]],
        });
    }
    validatorTriggers() {
        return ['entityType'];
    }
    updateValidators(emitEvent) {
        const entityType = this.createRelationConfigForm.get('entityType').value;
        if (entityType) {
            this.createRelationConfigForm.get('entityNamePattern').setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
        }
        else {
            this.createRelationConfigForm.get('entityNamePattern').setValidators([]);
        }
        if (entityType && (entityType === EntityType.DEVICE || entityType === EntityType.ASSET)) {
            this.createRelationConfigForm.get('entityTypePattern').setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
        }
        else {
            this.createRelationConfigForm.get('entityTypePattern').setValidators([]);
        }
        this.createRelationConfigForm.get('entityNamePattern').updateValueAndValidity({ emitEvent });
        this.createRelationConfigForm.get('entityTypePattern').updateValueAndValidity({ emitEvent });
    }
    prepareOutputConfig(configuration) {
        configuration.entityNamePattern = configuration.entityNamePattern ? configuration.entityNamePattern.trim() : null;
        configuration.entityTypePattern = configuration.entityTypePattern ? configuration.entityTypePattern.trim() : null;
        return configuration;
    }
}
CreateRelationConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CreateRelationConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
CreateRelationConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: CreateRelationConfigComponent, selector: "tb-action-node-create-relation-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"createRelationConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" style=\"min-width: 100px;\">\n    <mat-label translate>relation.direction</mat-label>\n    <mat-select required matInput formControlName=\"direction\">\n      <mat-option *ngFor=\"let type of directionTypes\" [value]=\"type\">\n        {{ directionTypeTranslations.get(type) | translate }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n  <div fxLayout=\"row\" fxLayoutGap=\"8px\">\n    <tb-entity-type-select\n      showLabel\n      style=\"min-width: 100px;\"\n      required\n      formControlName=\"entityType\">\n    </tb-entity-type-select>\n    <mat-form-field *ngIf=\"createRelationConfigForm.get('entityType').value\" fxFlex class=\"mat-block\" style=\"padding-bottom: 40px;\">\n      <mat-label translate>tb.rulenode.entity-name-pattern</mat-label>\n      <input required matInput formControlName=\"entityNamePattern\">\n      <mat-error *ngIf=\"createRelationConfigForm.get('entityNamePattern').hasError('required') ||\n                        createRelationConfigForm.get('entityNamePattern').hasError('pattern')\">\n        {{ 'tb.rulenode.entity-name-pattern-required' | translate }}\n      </mat-error>\n      <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n    </mat-form-field>\n    <mat-form-field *ngIf=\"createRelationConfigForm.get('entityType').value === entityType.DEVICE ||\n                           createRelationConfigForm.get('entityType').value === entityType.ASSET\"\n                    fxFlex class=\"mat-block\" style=\"padding-bottom: 40px;\">\n      <mat-label translate>tb.rulenode.entity-type-pattern</mat-label>\n      <input required matInput formControlName=\"entityTypePattern\">\n      <mat-error *ngIf=\"createRelationConfigForm.get('entityTypePattern').hasError('required') ||\n                        createRelationConfigForm.get('entityTypePattern').hasError('pattern')\">\n        {{ 'tb.rulenode.entity-type-pattern-required' | translate }}\n      </mat-error>\n      <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n    </mat-form-field>\n  </div>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.relation-type-pattern</mat-label>\n    <input required matInput formControlName=\"relationType\">\n    <mat-error *ngIf=\"createRelationConfigForm.get('relationType').hasError('required')\">\n      {{ 'tb.rulenode.relation-type-pattern-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <section *ngIf=\"createRelationConfigForm.get('entityType').value === entityType.CUSTOMER ||\n                  createRelationConfigForm.get('entityType').value === entityType.DEVICE ||\n                  createRelationConfigForm.get('entityType').value === entityType.ASSET\">\n    <mat-checkbox formControlName=\"createEntityIfNotExists\">\n      {{ 'tb.rulenode.create-entity-if-not-exists' | translate }}\n    </mat-checkbox>\n    <div class=\"tb-hint\" style=\"padding-bottom: 16px;\" translate>tb.rulenode.create-entity-if-not-exists-hint</div>\n  </section>\n  <mat-checkbox formControlName=\"removeCurrentRelations\">\n    {{ 'tb.rulenode.remove-current-relations' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" style=\"padding-bottom: 16px;\" translate>tb.rulenode.remove-current-relations-hint</div>\n  <mat-checkbox formControlName=\"changeOriginatorToRelatedEntity\">\n    {{ 'tb.rulenode.change-originator-to-related-entity' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" style=\"padding-bottom: 16px;\" translate>tb.rulenode.change-originator-to-related-entity-hint</div>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.entity-cache-expiration</mat-label>\n    <input required type=\"number\" min=\"0\" step=\"1\" matInput formControlName=\"entityCacheExpiration\">\n    <mat-error *ngIf=\"createRelationConfigForm.get('entityCacheExpiration').hasError('required')\">\n      {{ 'tb.rulenode.entity-cache-expiration-required' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"createRelationConfigForm.get('entityCacheExpiration').hasError('min')\">\n      {{ 'tb.rulenode.entity-cache-expiration-range' | translate }}\n    </mat-error>\n    <mat-hint translate>tb.rulenode.entity-cache-expiration-hint</mat-hint>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i7$1.EntityTypeSelectComponent, selector: "tb-entity-type-select", inputs: ["allowedEntityTypes", "useAliasEntityTypes", "showLabel", "required", "disabled"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CreateRelationConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-create-relation-config',
                    templateUrl: './create-relation-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class DeleteRelationConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.directionTypes = Object.keys(EntitySearchDirection);
        this.directionTypeTranslations = entitySearchDirectionTranslations;
        this.entityType = EntityType;
    }
    configForm() {
        return this.deleteRelationConfigForm;
    }
    onConfigurationSet(configuration) {
        this.deleteRelationConfigForm = this.fb.group({
            deleteForSingleEntity: [configuration ? configuration.deleteForSingleEntity : false, []],
            direction: [configuration ? configuration.direction : null, [Validators.required]],
            entityType: [configuration ? configuration.entityType : null, []],
            entityNamePattern: [configuration ? configuration.entityNamePattern : null, []],
            relationType: [configuration ? configuration.relationType : null, [Validators.required]],
            entityCacheExpiration: [configuration ? configuration.entityCacheExpiration : null, [Validators.required, Validators.min(0)]],
        });
    }
    validatorTriggers() {
        return ['deleteForSingleEntity', 'entityType'];
    }
    updateValidators(emitEvent) {
        const deleteForSingleEntity = this.deleteRelationConfigForm.get('deleteForSingleEntity').value;
        const entityType = this.deleteRelationConfigForm.get('entityType').value;
        if (deleteForSingleEntity) {
            this.deleteRelationConfigForm.get('entityType').setValidators([Validators.required]);
        }
        else {
            this.deleteRelationConfigForm.get('entityType').setValidators([]);
        }
        if (deleteForSingleEntity && entityType) {
            this.deleteRelationConfigForm.get('entityNamePattern').setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
        }
        else {
            this.deleteRelationConfigForm.get('entityNamePattern').setValidators([]);
        }
        this.deleteRelationConfigForm.get('entityType').updateValueAndValidity({ emitEvent: false });
        this.deleteRelationConfigForm.get('entityNamePattern').updateValueAndValidity({ emitEvent });
    }
    prepareOutputConfig(configuration) {
        configuration.entityNamePattern = configuration.entityNamePattern ? configuration.entityNamePattern.trim() : null;
        return configuration;
    }
}
DeleteRelationConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: DeleteRelationConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
DeleteRelationConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: DeleteRelationConfigComponent, selector: "tb-action-node-delete-relation-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"deleteRelationConfigForm\" fxLayout=\"column\">\n  <mat-checkbox formControlName=\"deleteForSingleEntity\">\n    {{ 'tb.rulenode.delete-relation-to-specific-entity' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" style=\"padding-bottom: 16px;\" translate>tb.rulenode.delete-relation-hint</div>\n  <mat-form-field class=\"mat-block\" style=\"min-width: 100px;\">\n    <mat-label translate>relation.direction</mat-label>\n    <mat-select required matInput formControlName=\"direction\">\n      <mat-option *ngFor=\"let type of directionTypes\" [value]=\"type\">\n        {{ directionTypeTranslations.get(type) | translate }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n  <div *ngIf=\"deleteRelationConfigForm.get('deleteForSingleEntity').value\" fxLayout=\"row\" fxLayoutGap=\"8px\">\n    <tb-entity-type-select\n      showLabel\n      style=\"min-width: 100px;\"\n      required\n      formControlName=\"entityType\">\n    </tb-entity-type-select>\n    <mat-form-field *ngIf=\"deleteRelationConfigForm.get('entityType').value\" fxFlex class=\"mat-block\" style=\"padding-bottom: 32px;\">\n      <mat-label translate>tb.rulenode.entity-name-pattern</mat-label>\n      <input required matInput formControlName=\"entityNamePattern\">\n      <mat-error *ngIf=\"deleteRelationConfigForm.get('entityNamePattern').hasError('required') ||\n                        deleteRelationConfigForm.get('entityNamePattern').hasError('pattern')\">\n        {{ 'tb.rulenode.entity-name-pattern-required' | translate }}\n      </mat-error>\n      <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n    </mat-form-field>\n  </div>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.relation-type-pattern</mat-label>\n    <input required matInput formControlName=\"relationType\">\n    <mat-error *ngIf=\"deleteRelationConfigForm.get('relationType').hasError('required')\">\n      {{ 'tb.rulenode.relation-type-pattern-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.entity-cache-expiration</mat-label>\n    <input required type=\"number\" min=\"0\" step=\"1\" matInput formControlName=\"entityCacheExpiration\">\n    <mat-error *ngIf=\"deleteRelationConfigForm.get('entityCacheExpiration').hasError('required')\">\n      {{ 'tb.rulenode.entity-cache-expiration-required' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"deleteRelationConfigForm.get('entityCacheExpiration').hasError('min')\">\n      {{ 'tb.rulenode.entity-cache-expiration-range' | translate }}\n    </mat-error>\n    <mat-hint translate>tb.rulenode.entity-cache-expiration-hint</mat-hint>\n  </mat-form-field>\n</section>\n", components: [{ type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i7$1.EntityTypeSelectComponent, selector: "tb-entity-type-select", inputs: ["allowedEntityTypes", "useAliasEntityTypes", "showLabel", "required", "disabled"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: DeleteRelationConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-delete-relation-config',
                    templateUrl: './delete-relation-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class DeviceProfileConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.deviceProfile;
    }
    onConfigurationSet(configuration) {
        this.deviceProfile = this.fb.group({
            persistAlarmRulesState: [configuration ? configuration.persistAlarmRulesState : false, Validators.required],
            fetchAlarmRulesStateOnStart: [configuration ? configuration.fetchAlarmRulesStateOnStart : false, Validators.required]
        });
    }
}
DeviceProfileConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: DeviceProfileConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
DeviceProfileConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: DeviceProfileConfigComponent, selector: "tb-device-profile-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"deviceProfile\" fxLayout=\"column\">\n  <mat-checkbox fxFlex formControlName=\"persistAlarmRulesState\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.persist-alarm-rules' | translate }}\n  </mat-checkbox>\n  <mat-checkbox fxFlex formControlName=\"fetchAlarmRulesStateOnStart\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.fetch-alarm-rules' | translate }}\n  </mat-checkbox>\n</section>\n", components: [{ type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: DeviceProfileConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-device-profile-config',
                    templateUrl: './device-profile-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class GeneratorConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb, nodeScriptTestService, translate) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.nodeScriptTestService = nodeScriptTestService;
        this.translate = translate;
    }
    configForm() {
        return this.generatorConfigForm;
    }
    onConfigurationSet(configuration) {
        this.generatorConfigForm = this.fb.group({
            msgCount: [configuration ? configuration.msgCount : null, [Validators.required, Validators.min(0)]],
            periodInSeconds: [configuration ? configuration.periodInSeconds : null, [Validators.required, Validators.min(1)]],
            originator: [configuration ? configuration.originator : null, []],
            jsScript: [configuration ? configuration.jsScript : null, [Validators.required]]
        });
    }
    prepareInputConfig(configuration) {
        if (configuration) {
            if (configuration.originatorId && configuration.originatorType) {
                configuration.originator = {
                    id: configuration.originatorId, entityType: configuration.originatorType
                };
            }
            else {
                configuration.originator = null;
            }
            delete configuration.originatorId;
            delete configuration.originatorType;
        }
        return configuration;
    }
    prepareOutputConfig(configuration) {
        if (configuration.originator) {
            configuration.originatorId = configuration.originator.id;
            configuration.originatorType = configuration.originator.entityType;
        }
        else {
            configuration.originatorId = null;
            configuration.originatorType = null;
        }
        delete configuration.originator;
        return configuration;
    }
    testScript() {
        const script = this.generatorConfigForm.get('jsScript').value;
        this.nodeScriptTestService.testNodeScript(script, 'generate', this.translate.instant('tb.rulenode.generator'), 'Generate', ['prevMsg', 'prevMetadata', 'prevMsgType'], this.ruleNodeId, 'rulenode/generator_node_script_fn').subscribe((theScript) => {
            if (theScript) {
                this.generatorConfigForm.get('jsScript').setValue(theScript);
            }
        });
    }
    onValidate() {
        this.jsFuncComponent.validateOnSubmit();
    }
}
GeneratorConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: GeneratorConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }, { token: i3$3.NodeScriptTestService }, { token: i4.TranslateService }], target: i0.ɵɵFactoryTarget.Component });
GeneratorConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: GeneratorConfigComponent, selector: "tb-action-node-generator-config", viewQueries: [{ propertyName: "jsFuncComponent", first: true, predicate: ["jsFuncComponent"], descendants: true, static: true }], usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"generatorConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.message-count</mat-label>\n    <input required type=\"number\" min=\"0\" step=\"1\" matInput formControlName=\"msgCount\">\n    <mat-error *ngIf=\"generatorConfigForm.get('msgCount').hasError('required')\">\n      {{ 'tb.rulenode.message-count-required' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"generatorConfigForm.get('msgCount').hasError('min')\">\n      {{ 'tb.rulenode.min-message-count-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.period-seconds</mat-label>\n    <input required type=\"number\" min=\"1\" step=\"1\" matInput formControlName=\"periodInSeconds\">\n    <mat-error *ngIf=\"generatorConfigForm.get('periodInSeconds').hasError('required')\">\n      {{ 'tb.rulenode.period-seconds-required' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"generatorConfigForm.get('periodInSeconds').hasError('min')\">\n      {{ 'tb.rulenode.min-period-seconds-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <div fxLayout=\"column\">\n    <label class=\"tb-small\">{{ 'tb.rulenode.originator' | translate }}</label>\n    <tb-entity-select\n      required=\"false\"\n      formControlName=\"originator\">\n    </tb-entity-select>\n  </div>\n  <label translate class=\"tb-title no-padding\">tb.rulenode.generate</label>\n  <tb-js-func #jsFuncComponent\n              formControlName=\"jsScript\"\n              functionName=\"Generate\"\n              [functionArgs]=\"['prevMsg', 'prevMetadata', 'prevMsgType']\"\n              helpId=\"rulenode/generator_node_script_fn\"\n              noValidate=\"true\">\n  </tb-js-func>\n  <div fxLayout=\"row\" style=\"padding-bottom: 16px;\">\n    <button mat-button mat-raised-button color=\"primary\" (click)=\"testScript()\">\n      {{ 'tb.rulenode.test-generator-function' | translate }}\n    </button>\n  </div>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i6$2.EntitySelectComponent, selector: "tb-entity-select", inputs: ["allowedEntityTypes", "useAliasEntityTypes", "required", "disabled"] }, { type: i5$1.JsFuncComponent, selector: "tb-js-func", inputs: ["functionName", "functionArgs", "validationArgs", "resultType", "disabled", "fillHeight", "editorCompleter", "globalVariables", "disableUndefinedCheck", "helpId", "noValidate", "required"] }, { type: i6.MatButton, selector: "button[mat-button], button[mat-raised-button], button[mat-icon-button],             button[mat-fab], button[mat-mini-fab], button[mat-stroked-button],             button[mat-flat-button]", inputs: ["disabled", "disableRipple", "color"], exportAs: ["matButton"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: GeneratorConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-generator-config',
                    templateUrl: './generator-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }, { type: i3$3.NodeScriptTestService }, { type: i4.TranslateService }]; }, propDecorators: { jsFuncComponent: [{
                type: ViewChild,
                args: ['jsFuncComponent', { static: true }]
            }] } });

class GpsGeoActionConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.perimeterType = PerimeterType;
        this.perimeterTypes = Object.keys(PerimeterType);
        this.perimeterTypeTranslationMap = perimeterTypeTranslations;
        this.rangeUnits = Object.keys(RangeUnit);
        this.rangeUnitTranslationMap = rangeUnitTranslations;
        this.timeUnits = Object.keys(TimeUnit);
        this.timeUnitsTranslationMap = timeUnitTranslations;
    }
    configForm() {
        return this.geoActionConfigForm;
    }
    onConfigurationSet(configuration) {
        this.geoActionConfigForm = this.fb.group({
            latitudeKeyName: [configuration ? configuration.latitudeKeyName : null, [Validators.required]],
            longitudeKeyName: [configuration ? configuration.longitudeKeyName : null, [Validators.required]],
            fetchPerimeterInfoFromMessageMetadata: [configuration ? configuration.fetchPerimeterInfoFromMessageMetadata : false, []],
            perimeterType: [configuration ? configuration.perimeterType : null, []],
            centerLatitude: [configuration ? configuration.centerLatitude : null, []],
            centerLongitude: [configuration ? configuration.centerLatitude : null, []],
            range: [configuration ? configuration.range : null, []],
            rangeUnit: [configuration ? configuration.rangeUnit : null, []],
            polygonsDefinition: [configuration ? configuration.polygonsDefinition : null, []],
            minInsideDuration: [configuration ? configuration.minInsideDuration : null,
                [Validators.required, Validators.min(1), Validators.max(2147483647)]],
            minInsideDurationTimeUnit: [configuration ? configuration.minInsideDurationTimeUnit : null, [Validators.required]],
            minOutsideDuration: [configuration ? configuration.minOutsideDuration : null,
                [Validators.required, Validators.min(1), Validators.max(2147483647)]],
            minOutsideDurationTimeUnit: [configuration ? configuration.minOutsideDurationTimeUnit : null, [Validators.required]],
        });
    }
    validatorTriggers() {
        return ['fetchPerimeterInfoFromMessageMetadata', 'perimeterType'];
    }
    updateValidators(emitEvent) {
        const fetchPerimeterInfoFromMessageMetadata = this.geoActionConfigForm.get('fetchPerimeterInfoFromMessageMetadata').value;
        const perimeterType = this.geoActionConfigForm.get('perimeterType').value;
        if (fetchPerimeterInfoFromMessageMetadata) {
            this.geoActionConfigForm.get('perimeterType').setValidators([]);
        }
        else {
            this.geoActionConfigForm.get('perimeterType').setValidators([Validators.required]);
        }
        if (!fetchPerimeterInfoFromMessageMetadata && perimeterType === PerimeterType.CIRCLE) {
            this.geoActionConfigForm.get('centerLatitude').setValidators([Validators.required,
                Validators.min(-90), Validators.max(90)]);
            this.geoActionConfigForm.get('centerLongitude').setValidators([Validators.required,
                Validators.min(-180), Validators.max(180)]);
            this.geoActionConfigForm.get('range').setValidators([Validators.required, Validators.min(0)]);
            this.geoActionConfigForm.get('rangeUnit').setValidators([Validators.required]);
        }
        else {
            this.geoActionConfigForm.get('centerLatitude').setValidators([]);
            this.geoActionConfigForm.get('centerLongitude').setValidators([]);
            this.geoActionConfigForm.get('range').setValidators([]);
            this.geoActionConfigForm.get('rangeUnit').setValidators([]);
        }
        if (!fetchPerimeterInfoFromMessageMetadata && perimeterType === PerimeterType.POLYGON) {
            this.geoActionConfigForm.get('polygonsDefinition').setValidators([Validators.required]);
        }
        else {
            this.geoActionConfigForm.get('polygonsDefinition').setValidators([]);
        }
        this.geoActionConfigForm.get('perimeterType').updateValueAndValidity({ emitEvent: false });
        this.geoActionConfigForm.get('centerLatitude').updateValueAndValidity({ emitEvent });
        this.geoActionConfigForm.get('centerLongitude').updateValueAndValidity({ emitEvent });
        this.geoActionConfigForm.get('range').updateValueAndValidity({ emitEvent });
        this.geoActionConfigForm.get('rangeUnit').updateValueAndValidity({ emitEvent });
        this.geoActionConfigForm.get('polygonsDefinition').updateValueAndValidity({ emitEvent });
    }
}
GpsGeoActionConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: GpsGeoActionConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
GpsGeoActionConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: GpsGeoActionConfigComponent, selector: "tb-action-node-gps-geofencing-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"geoActionConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.latitude-key-name</mat-label>\n    <input matInput formControlName=\"latitudeKeyName\" required>\n    <mat-error *ngIf=\"geoActionConfigForm.get('latitudeKeyName').hasError('required')\">\n      {{ 'tb.rulenode.latitude-key-name-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.longitude-key-name</mat-label>\n    <input matInput formControlName=\"longitudeKeyName\" required>\n    <mat-error *ngIf=\"geoActionConfigForm.get('longitudeKeyName').hasError('required')\">\n      {{ 'tb.rulenode.longitude-key-name-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-checkbox fxFlex formControlName=\"fetchPerimeterInfoFromMessageMetadata\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.fetch-perimeter-info-from-message-metadata' | translate }}\n  </mat-checkbox>\n  <div fxLayout=\"row\" *ngIf=\"!geoActionConfigForm.get('fetchPerimeterInfoFromMessageMetadata').value\">\n    <mat-form-field fxFlex class=\"mat-block\">\n      <mat-label translate>tb.rulenode.perimeter-type</mat-label>\n      <mat-select formControlName=\"perimeterType\" required>\n        <mat-option *ngFor=\"let type of perimeterTypes\" [value]=\"type\">\n          {{ perimeterTypeTranslationMap.get(type) | translate }}\n        </mat-option>\n      </mat-select>\n    </mat-form-field>\n  </div>\n  <div fxLayout=\"column\"\n       *ngIf=\"geoActionConfigForm.get('perimeterType').value === perimeterType.CIRCLE &&\n       !geoActionConfigForm.get('fetchPerimeterInfoFromMessageMetadata').value\">\n    <div fxLayout=\"row\" fxLayoutGap=\"8px\">\n      <mat-form-field fxFlex>\n        <mat-label translate>tb.rulenode.circle-center-latitude</mat-label>\n        <input type=\"number\" min=\"-90\" max=\"90\" step=\"0.1\" matInput formControlName=\"centerLatitude\" required>\n        <mat-error *ngIf=\"geoActionConfigForm.get('centerLatitude').hasError('required')\">\n          {{ 'tb.rulenode.circle-center-latitude-required' | translate }}\n        </mat-error>\n      </mat-form-field>\n      <mat-form-field fxFlex>\n        <mat-label translate>tb.rulenode.circle-center-longitude</mat-label>\n        <input type=\"number\" min=\"-180\" max=\"180\" step=\"0.1\" matInput formControlName=\"centerLongitude\" required>\n        <mat-error *ngIf=\"geoActionConfigForm.get('centerLongitude').hasError('required')\">\n          {{ 'tb.rulenode.circle-center-longitude-required' | translate }}\n        </mat-error>\n      </mat-form-field>\n    </div>\n    <div fxLayout=\"row\" fxLayoutGap=\"8px\">\n      <mat-form-field fxFlex>\n        <mat-label translate>tb.rulenode.range</mat-label>\n        <input type=\"number\" min=\"0\" step=\"0.1\" matInput formControlName=\"range\" required>\n        <mat-error *ngIf=\"geoActionConfigForm.get('range').hasError('required')\">\n          {{ 'tb.rulenode.range-required' | translate }}\n        </mat-error>\n      </mat-form-field>\n      <mat-form-field fxFlex>\n        <mat-label translate>tb.rulenode.range-units</mat-label>\n        <mat-select formControlName=\"rangeUnit\" required>\n          <mat-option *ngFor=\"let type of rangeUnits\" [value]=\"type\">\n            {{ rangeUnitTranslationMap.get(type) | translate }}\n          </mat-option>\n        </mat-select>\n      </mat-form-field>\n    </div>\n  </div>\n  <div fxLayout=\"column\" *ngIf=\"geoActionConfigForm.get('perimeterType').value === perimeterType.POLYGON &&\n                             !geoActionConfigForm.get('fetchPerimeterInfoFromMessageMetadata').value\">\n    <mat-form-field class=\"mat-block\" hintLabel=\"{{'tb.rulenode.polygon-definition-hint' | translate}}\" style=\"padding-bottom: 16px;\">\n      <mat-label translate>tb.rulenode.polygon-definition</mat-label>\n      <input matInput formControlName=\"polygonsDefinition\" required>\n      <mat-error *ngIf=\"geoActionConfigForm.get('polygonsDefinition').hasError('required')\">\n        {{ 'tb.rulenode.polygon-definition-required' | translate }}\n      </mat-error>\n    </mat-form-field>\n  </div>\n  <div fxLayout=\"column\" fxLayout.gt-sm=\"row\" fxLayoutGap.gt-sm=\"8px\">\n    <mat-form-field fxFlex class=\"mat-block\">\n      <mat-label translate>tb.rulenode.min-inside-duration</mat-label>\n      <input type=\"number\" step=\"1\" min=\"1\" max=\"2147483647\" matInput formControlName=\"minInsideDuration\" required>\n      <mat-error *ngIf=\"geoActionConfigForm.get('minInsideDuration').hasError('required')\">\n        {{ 'tb.rulenode.min-inside-duration-value-required' | translate }}\n      </mat-error>\n      <mat-error *ngIf=\"geoActionConfigForm.get('minInsideDuration').hasError('min')\">\n        {{ 'tb.rulenode.time-value-range' | translate }}\n      </mat-error>\n      <mat-error *ngIf=\"geoActionConfigForm.get('minInsideDuration').hasError('max')\">\n        {{ 'tb.rulenode.time-value-range' | translate }}\n      </mat-error>\n    </mat-form-field>\n    <mat-form-field fxFlex class=\"mat-block\">\n      <mat-label translate>tb.rulenode.min-inside-duration-time-unit</mat-label>\n      <mat-select formControlName=\"minInsideDurationTimeUnit\" required>\n        <mat-option *ngFor=\"let timeUnit of timeUnits\" [value]=\"timeUnit\">\n          {{ timeUnitsTranslationMap.get(timeUnit) | translate }}\n        </mat-option>\n      </mat-select>\n    </mat-form-field>\n  </div>\n  <div fxLayout=\"column\" fxLayout.gt-sm=\"row\" fxLayoutGap.gt-sm=\"8px\">\n    <mat-form-field fxFlex class=\"mat-block\">\n      <mat-label translate>tb.rulenode.min-outside-duration</mat-label>\n      <input type=\"number\" step=\"1\" min=\"1\" max=\"2147483647\" matInput formControlName=\"minOutsideDuration\" required>\n      <mat-error *ngIf=\"geoActionConfigForm.get('minOutsideDuration').hasError('required')\">\n        {{ 'tb.rulenode.min-outside-duration-value-required' | translate }}\n      </mat-error>\n      <mat-error *ngIf=\"geoActionConfigForm.get('minOutsideDuration').hasError('min')\">\n        {{ 'tb.rulenode.time-value-range' | translate }}\n      </mat-error>\n      <mat-error *ngIf=\"geoActionConfigForm.get('minOutsideDuration').hasError('max')\">\n        {{ 'tb.rulenode.time-value-range' | translate }}\n      </mat-error>\n    </mat-form-field>\n    <mat-form-field fxFlex class=\"mat-block\">\n      <mat-label translate>tb.rulenode.min-outside-duration-time-unit</mat-label>\n      <mat-select formControlName=\"minOutsideDurationTimeUnit\" required>\n        <mat-option *ngFor=\"let timeUnit of timeUnits\" [value]=\"timeUnit\">\n          {{ timeUnitsTranslationMap.get(timeUnit) | translate }}\n        </mat-option>\n      </mat-select>\n    </mat-form-field>\n  </div>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.MaxValidator, selector: "input[type=number][max][formControlName],input[type=number][max][formControl],input[type=number][max][ngModel]", inputs: ["max"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: GpsGeoActionConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-gps-geofencing-config',
                    templateUrl: './gps-geo-action-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class KvMapConfigComponent extends PageComponent {
    constructor(store, translate, injector, fb) {
        super(store);
        this.store = store;
        this.translate = translate;
        this.injector = injector;
        this.fb = fb;
        this.propagateChange = null;
        this.valueChangeSubscription = null;
    }
    get required() {
        return this.requiredValue;
    }
    set required(value) {
        this.requiredValue = coerceBooleanProperty(value);
    }
    ngOnInit() {
        this.ngControl = this.injector.get(NgControl);
        if (this.ngControl != null) {
            this.ngControl.valueAccessor = this;
        }
        this.kvListFormGroup = this.fb.group({});
        this.kvListFormGroup.addControl('keyVals', this.fb.array([]));
    }
    keyValsFormArray() {
        return this.kvListFormGroup.get('keyVals');
    }
    registerOnChange(fn) {
        this.propagateChange = fn;
    }
    registerOnTouched(fn) {
    }
    setDisabledState(isDisabled) {
        this.disabled = isDisabled;
        if (this.disabled) {
            this.kvListFormGroup.disable({ emitEvent: false });
        }
        else {
            this.kvListFormGroup.enable({ emitEvent: false });
        }
    }
    writeValue(keyValMap) {
        if (this.valueChangeSubscription) {
            this.valueChangeSubscription.unsubscribe();
        }
        const keyValsControls = [];
        if (keyValMap) {
            for (const property of Object.keys(keyValMap)) {
                if (Object.prototype.hasOwnProperty.call(keyValMap, property)) {
                    keyValsControls.push(this.fb.group({
                        key: [property, [Validators.required]],
                        value: [keyValMap[property], [Validators.required]]
                    }));
                }
            }
        }
        this.kvListFormGroup.setControl('keyVals', this.fb.array(keyValsControls));
        this.valueChangeSubscription = this.kvListFormGroup.valueChanges.subscribe(() => {
            this.updateModel();
        });
    }
    removeKeyVal(index) {
        this.kvListFormGroup.get('keyVals').removeAt(index);
    }
    addKeyVal() {
        const keyValsFormArray = this.kvListFormGroup.get('keyVals');
        keyValsFormArray.push(this.fb.group({
            key: ['', [Validators.required]],
            value: ['', [Validators.required]]
        }));
    }
    validate(c) {
        const kvList = this.kvListFormGroup.get('keyVals').value;
        if (!kvList.length && this.required) {
            return {
                kvMapRequired: true
            };
        }
        if (!this.kvListFormGroup.valid) {
            return {
                kvFieldsRequired: true
            };
        }
        return null;
    }
    updateModel() {
        const kvList = this.kvListFormGroup.get('keyVals').value;
        if (this.required && !kvList.length || !this.kvListFormGroup.valid) {
            this.propagateChange(null);
        }
        else {
            const keyValMap = {};
            kvList.forEach((entry) => {
                keyValMap[entry.key] = entry.value;
            });
            this.propagateChange(keyValMap);
        }
    }
}
KvMapConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: KvMapConfigComponent, deps: [{ token: i1.Store }, { token: i4.TranslateService }, { token: i0.Injector }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
KvMapConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: KvMapConfigComponent, selector: "tb-kv-map-config", inputs: { disabled: "disabled", requiredText: "requiredText", keyText: "keyText", keyRequiredText: "keyRequiredText", valText: "valText", valRequiredText: "valRequiredText", required: "required" }, providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => KvMapConfigComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => KvMapConfigComponent),
            multi: true,
        }
    ], usesInheritance: true, ngImport: i0, template: "<section fxLayout=\"column\" class=\"tb-kv-map-config\" [formGroup]=\"kvListFormGroup\">\n  <div class=\"header\" fxFlex fxLayout=\"row\" fxLayoutGap=\"8px\">\n    <span class=\"cell\" fxFlex>{{ keyText | translate }}</span>\n    <span class=\"cell\" fxFlex>{{ valText | translate }}</span>\n    <span [fxShow]=\"!disabled\" style=\"width: 52px;\" innerHTML=\"&nbsp\"></span>\n  </div>\n  <div class=\"body\">\n    <div class=\"row\" fxLayout=\"row\" fxLayoutAlign=\"start center\" fxLayoutGap=\"8px\"\n         formArrayName=\"keyVals\"\n         *ngFor=\"let keyValControl of keyValsFormArray().controls; let $index = index\">\n      <mat-form-field fxFlex floatLabel=\"always\" hideRequiredMarker class=\"cell mat-block\">\n        <mat-label></mat-label>\n        <input [formControl]=\"keyValControl.get('key')\" matInput required\n               placeholder=\"{{ keyText | translate }}\"/>\n        <mat-error *ngIf=\"keyValControl.get('key').hasError('required')\">\n          {{ keyRequiredText | translate }}\n        </mat-error>\n      </mat-form-field>\n      <mat-form-field fxFlex floatLabel=\"always\" hideRequiredMarker class=\"cell mat-block\">\n        <mat-label></mat-label>\n        <input [formControl]=\"keyValControl.get('value')\" matInput required\n               placeholder=\"{{ valText | translate }}\"/>\n        <mat-error *ngIf=\"keyValControl.get('value').hasError('required')\">\n          {{ valRequiredText | translate }}\n        </mat-error>\n      </mat-form-field>\n      <button mat-button mat-icon-button color=\"primary\"\n              [fxShow]=\"!disabled\"\n              type=\"button\"\n              (click)=\"removeKeyVal($index)\"\n              [disabled]=\"isLoading$ | async\"\n              matTooltip=\"{{ 'tb.key-val.remove-entry' | translate }}\"\n              matTooltipPosition=\"above\">\n        <mat-icon>close</mat-icon>\n      </button>\n    </div>\n  </div>\n  <tb-error [error]=\"ngControl.hasError('kvMapRequired')\n                  ? translate.instant(requiredText) : ''\"></tb-error>\n  <div style=\"margin-top: 8px;\">\n    <button mat-button mat-raised-button color=\"primary\"\n            [fxShow]=\"!disabled\"\n            [disabled]=\"isLoading$ | async\"\n            (click)=\"addKeyVal()\"\n            type=\"button\"\n            matTooltip=\"{{ 'tb.key-val.add-entry' | translate }}\"\n            matTooltipPosition=\"above\">\n      <mat-icon>add</mat-icon>\n      {{ 'action.add' | translate }}\n    </button>\n  </div>\n</section>\n", styles: [":host .tb-kv-map-config{margin-bottom:16px}:host .tb-kv-map-config .header{padding-left:5px;padding-right:5px;padding-bottom:5px}:host .tb-kv-map-config .header .cell{padding-left:5px;padding-right:5px;color:#0000008a;font-size:12px;font-weight:700;white-space:nowrap}:host .tb-kv-map-config .body{padding-left:5px;padding-right:5px;padding-bottom:20px;max-height:300px;overflow:auto}:host .tb-kv-map-config .body .row{padding-top:5px;max-height:40px}:host .tb-kv-map-config .body .cell{padding-left:5px;padding-right:5px}:host ::ng-deep .tb-kv-map-config .body mat-form-field.cell{margin:0;max-height:40px}:host ::ng-deep .tb-kv-map-config .body mat-form-field.cell .mat-form-field-infix{border-top:0}:host ::ng-deep .tb-kv-map-config .body button.mat-button{margin:0}\n"], components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i6.MatButton, selector: "button[mat-button], button[mat-raised-button], button[mat-icon-button],             button[mat-fab], button[mat-mini-fab], button[mat-stroked-button],             button[mat-flat-button]", inputs: ["disabled", "disableRipple", "color"], exportAs: ["matButton"] }, { type: i6$1.MatIcon, selector: "mat-icon", inputs: ["color", "inline", "svgIcon", "fontSet", "fontIcon"], exportAs: ["matIcon"] }, { type: i9.TbErrorComponent, selector: "tb-error", inputs: ["error"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i9$1.DefaultShowHideDirective, selector: "  [fxShow], [fxShow.print],  [fxShow.xs], [fxShow.sm], [fxShow.md], [fxShow.lg], [fxShow.xl],  [fxShow.lt-sm], [fxShow.lt-md], [fxShow.lt-lg], [fxShow.lt-xl],  [fxShow.gt-xs], [fxShow.gt-sm], [fxShow.gt-md], [fxShow.gt-lg],  [fxHide], [fxHide.print],  [fxHide.xs], [fxHide.sm], [fxHide.md], [fxHide.lg], [fxHide.xl],  [fxHide.lt-sm], [fxHide.lt-md], [fxHide.lt-lg], [fxHide.lt-xl],  [fxHide.gt-xs], [fxHide.gt-sm], [fxHide.gt-md], [fxHide.gt-lg]", inputs: ["fxShow", "fxShow.print", "fxShow.xs", "fxShow.sm", "fxShow.md", "fxShow.lg", "fxShow.xl", "fxShow.lt-sm", "fxShow.lt-md", "fxShow.lt-lg", "fxShow.lt-xl", "fxShow.gt-xs", "fxShow.gt-sm", "fxShow.gt-md", "fxShow.gt-lg", "fxHide", "fxHide.print", "fxHide.xs", "fxHide.sm", "fxHide.md", "fxHide.lg", "fxHide.xl", "fxHide.lt-sm", "fxHide.lt-md", "fxHide.lt-lg", "fxHide.lt-xl", "fxHide.gt-xs", "fxHide.gt-sm", "fxHide.gt-md", "fxHide.gt-lg"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i8.DefaultLayoutAlignDirective, selector: "  [fxLayoutAlign], [fxLayoutAlign.xs], [fxLayoutAlign.sm], [fxLayoutAlign.md],  [fxLayoutAlign.lg], [fxLayoutAlign.xl], [fxLayoutAlign.lt-sm], [fxLayoutAlign.lt-md],  [fxLayoutAlign.lt-lg], [fxLayoutAlign.lt-xl], [fxLayoutAlign.gt-xs], [fxLayoutAlign.gt-sm],  [fxLayoutAlign.gt-md], [fxLayoutAlign.gt-lg]", inputs: ["fxLayoutAlign", "fxLayoutAlign.xs", "fxLayoutAlign.sm", "fxLayoutAlign.md", "fxLayoutAlign.lg", "fxLayoutAlign.xl", "fxLayoutAlign.lt-sm", "fxLayoutAlign.lt-md", "fxLayoutAlign.lt-lg", "fxLayoutAlign.lt-xl", "fxLayoutAlign.gt-xs", "fxLayoutAlign.gt-sm", "fxLayoutAlign.gt-md", "fxLayoutAlign.gt-lg"] }, { type: i2.FormArrayName, selector: "[formArrayName]", inputs: ["formArrayName"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlDirective, selector: "[formControl]", inputs: ["disabled", "formControl", "ngModel"], outputs: ["ngModelChange"], exportAs: ["ngForm"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i12.MatTooltip, selector: "[matTooltip]", exportAs: ["matTooltip"] }], pipes: { "translate": i4.TranslatePipe, "async": i10.AsyncPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: KvMapConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-kv-map-config',
                    templateUrl: './kv-map-config.component.html',
                    styleUrls: ['./kv-map-config.component.scss'],
                    providers: [
                        {
                            provide: NG_VALUE_ACCESSOR,
                            useExisting: forwardRef(() => KvMapConfigComponent),
                            multi: true
                        },
                        {
                            provide: NG_VALIDATORS,
                            useExisting: forwardRef(() => KvMapConfigComponent),
                            multi: true,
                        }
                    ]
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i4.TranslateService }, { type: i0.Injector }, { type: i2.FormBuilder }]; }, propDecorators: { disabled: [{
                type: Input
            }], requiredText: [{
                type: Input
            }], keyText: [{
                type: Input
            }], keyRequiredText: [{
                type: Input
            }], valText: [{
                type: Input
            }], valRequiredText: [{
                type: Input
            }], required: [{
                type: Input
            }] } });

class KafkaConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.ackValues = ['all', '-1', '0', '1'];
        this.ToByteStandartCharsetTypesValues = ToByteStandartCharsetTypes;
        this.ToByteStandartCharsetTypeTranslationMap = ToByteStandartCharsetTypeTranslations;
    }
    configForm() {
        return this.kafkaConfigForm;
    }
    onConfigurationSet(configuration) {
        this.kafkaConfigForm = this.fb.group({
            topicPattern: [configuration ? configuration.topicPattern : null, [Validators.required]],
            bootstrapServers: [configuration ? configuration.bootstrapServers : null, [Validators.required]],
            retries: [configuration ? configuration.retries : null, [Validators.min(0)]],
            batchSize: [configuration ? configuration.batchSize : null, [Validators.min(0)]],
            linger: [configuration ? configuration.linger : null, [Validators.min(0)]],
            bufferMemory: [configuration ? configuration.bufferMemory : null, [Validators.min(0)]],
            acks: [configuration ? configuration.acks : null, [Validators.required]],
            keySerializer: [configuration ? configuration.keySerializer : null, [Validators.required]],
            valueSerializer: [configuration ? configuration.valueSerializer : null, [Validators.required]],
            otherProperties: [configuration ? configuration.otherProperties : null, []],
            addMetadataKeyValuesAsKafkaHeaders: [configuration ? configuration.addMetadataKeyValuesAsKafkaHeaders : false, []],
            kafkaHeadersCharset: [configuration ? configuration.kafkaHeadersCharset : null, []]
        });
    }
    validatorTriggers() {
        return ['addMetadataKeyValuesAsKafkaHeaders'];
    }
    updateValidators(emitEvent) {
        const addMetadataKeyValuesAsKafkaHeaders = this.kafkaConfigForm.get('addMetadataKeyValuesAsKafkaHeaders').value;
        if (addMetadataKeyValuesAsKafkaHeaders) {
            this.kafkaConfigForm.get('kafkaHeadersCharset').setValidators([Validators.required]);
        }
        else {
            this.kafkaConfigForm.get('kafkaHeadersCharset').setValidators([]);
        }
        this.kafkaConfigForm.get('kafkaHeadersCharset').updateValueAndValidity({ emitEvent });
    }
}
KafkaConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: KafkaConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
KafkaConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: KafkaConfigComponent, selector: "tb-action-node-kafka-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"kafkaConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 8px;\">\n    <mat-label translate>tb.rulenode.topic-pattern</mat-label>\n    <input required matInput formControlName=\"topicPattern\">\n    <mat-error *ngIf=\"kafkaConfigForm.get('topicPattern').hasError('required')\">\n      {{ 'tb.rulenode.topic-pattern-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.bootstrap-servers</mat-label>\n    <input required matInput formControlName=\"bootstrapServers\">\n    <mat-error *ngIf=\"kafkaConfigForm.get('bootstrapServers').hasError('required')\">\n      {{ 'tb.rulenode.bootstrap-servers-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.retries</mat-label>\n    <input type=\"number\" step=\"1\" min=\"0\" matInput formControlName=\"retries\">\n    <mat-error *ngIf=\"kafkaConfigForm.get('retries').hasError('min')\">\n      {{ 'tb.rulenode.min-retries-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.batch-size-bytes</mat-label>\n    <input type=\"number\" step=\"1\" min=\"0\" matInput formControlName=\"batchSize\">\n    <mat-error *ngIf=\"kafkaConfigForm.get('batchSize').hasError('min')\">\n      {{ 'tb.rulenode.min-batch-size-bytes-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.linger-ms</mat-label>\n    <input type=\"number\" step=\"1\" min=\"0\" matInput formControlName=\"linger\">\n    <mat-error *ngIf=\"kafkaConfigForm.get('linger').hasError('min')\">\n      {{ 'tb.rulenode.min-linger-ms-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.buffer-memory-bytes</mat-label>\n    <input type=\"number\" step=\"1\" min=\"0\" matInput formControlName=\"bufferMemory\">\n    <mat-error *ngIf=\"kafkaConfigForm.get('bufferMemory').hasError('min')\">\n      {{ 'tb.rulenode.min-buffer-memory-bytes-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.acks</mat-label>\n    <mat-select formControlName=\"acks\" required>\n      <mat-option *ngFor=\"let ackValue of ackValues\" [value]=\"ackValue\">\n        {{ ackValue }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.key-serializer</mat-label>\n    <input required matInput formControlName=\"keySerializer\">\n    <mat-error *ngIf=\"kafkaConfigForm.get('keySerializer').hasError('required')\">\n      {{ 'tb.rulenode.key-serializer-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.value-serializer</mat-label>\n    <input required matInput formControlName=\"valueSerializer\">\n    <mat-error *ngIf=\"kafkaConfigForm.get('valueSerializer').hasError('required')\">\n      {{ 'tb.rulenode.value-serializer-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <label translate class=\"tb-title\">tb.rulenode.other-properties</label>\n  <tb-kv-map-config\n    required=\"false\"\n    formControlName=\"otherProperties\"\n    keyText=\"tb.rulenode.key\"\n    keyRequiredText=\"tb.rulenode.key-required\"\n    valText=\"tb.rulenode.value\"\n    valRequiredText=\"tb.rulenode.value-required\">\n  </tb-kv-map-config>\n  <mat-checkbox fxFlex formControlName=\"addMetadataKeyValuesAsKafkaHeaders\" style=\"padding-top: 16px;\">\n    {{ 'tb.rulenode.add-metadata-key-values-as-kafka-headers' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" translate>tb.rulenode.add-metadata-key-values-as-kafka-headers-hint</div>\n  <mat-form-field fxFlex class=\"mat-block\" *ngIf=\"kafkaConfigForm.get('addMetadataKeyValuesAsKafkaHeaders').value\">\n    <mat-label translate>tb.rulenode.charset-encoding</mat-label>\n    <mat-select formControlName=\"kafkaHeadersCharset\" required>\n      <mat-option *ngFor=\"let charset of ToByteStandartCharsetTypesValues\" [value]=\"charset\">\n        {{ ToByteStandartCharsetTypeTranslationMap.get(charset) | translate }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: KvMapConfigComponent, selector: "tb-kv-map-config", inputs: ["disabled", "requiredText", "keyText", "keyRequiredText", "valText", "valRequiredText", "required"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: KafkaConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-kafka-config',
                    templateUrl: './kafka-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class LogConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb, nodeScriptTestService, translate) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.nodeScriptTestService = nodeScriptTestService;
        this.translate = translate;
    }
    configForm() {
        return this.logConfigForm;
    }
    onConfigurationSet(configuration) {
        this.logConfigForm = this.fb.group({
            jsScript: [configuration ? configuration.jsScript : null, [Validators.required]]
        });
    }
    testScript() {
        const script = this.logConfigForm.get('jsScript').value;
        this.nodeScriptTestService.testNodeScript(script, 'string', this.translate.instant('tb.rulenode.to-string'), 'ToString', ['msg', 'metadata', 'msgType'], this.ruleNodeId, 'rulenode/log_node_script_fn').subscribe((theScript) => {
            if (theScript) {
                this.logConfigForm.get('jsScript').setValue(theScript);
            }
        });
    }
    onValidate() {
        this.jsFuncComponent.validateOnSubmit();
    }
}
LogConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: LogConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }, { token: i3$3.NodeScriptTestService }, { token: i4.TranslateService }], target: i0.ɵɵFactoryTarget.Component });
LogConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: LogConfigComponent, selector: "tb-action-node-log-config", viewQueries: [{ propertyName: "jsFuncComponent", first: true, predicate: ["jsFuncComponent"], descendants: true, static: true }], usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"logConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title no-padding\">tb.rulenode.to-string</label>\n  <tb-js-func #jsFuncComponent\n              formControlName=\"jsScript\"\n              functionName=\"ToString\"\n              [functionArgs]=\"['msg', 'metadata', 'msgType']\"\n              helpId=\"rulenode/log_node_script_fn\"\n              noValidate=\"true\">\n  </tb-js-func>\n  <div fxLayout=\"row\">\n    <button mat-button mat-raised-button color=\"primary\" (click)=\"testScript()\">\n      {{ 'tb.rulenode.test-to-string-function' | translate }}\n    </button>\n  </div>\n</section>\n", components: [{ type: i5$1.JsFuncComponent, selector: "tb-js-func", inputs: ["functionName", "functionArgs", "validationArgs", "resultType", "disabled", "fillHeight", "editorCompleter", "globalVariables", "disableUndefinedCheck", "helpId", "noValidate", "required"] }, { type: i6.MatButton, selector: "button[mat-button], button[mat-raised-button], button[mat-icon-button],             button[mat-fab], button[mat-mini-fab], button[mat-stroked-button],             button[mat-flat-button]", inputs: ["disabled", "disableRipple", "color"], exportAs: ["matButton"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: LogConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-log-config',
                    templateUrl: './log-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }, { type: i3$3.NodeScriptTestService }, { type: i4.TranslateService }]; }, propDecorators: { jsFuncComponent: [{
                type: ViewChild,
                args: ['jsFuncComponent', { static: true }]
            }] } });

class CredentialsConfigComponent extends PageComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.subscriptions = [];
        this.disableCertPemCredentials = false;
        this.passwordFieldRquired = true;
        this.allCredentialsTypes = credentialsTypes;
        this.credentialsTypeTranslationsMap = credentialsTypeTranslations;
        this.propagateChange = null;
    }
    get required() {
        return this.requiredValue;
    }
    set required(value) {
        this.requiredValue = coerceBooleanProperty(value);
    }
    ngOnInit() {
        this.credentialsConfigFormGroup = this.fb.group({
            type: [null, [Validators.required]],
            username: [null, []],
            password: [null, []],
            caCert: [null, []],
            caCertFileName: [null, []],
            privateKey: [null, []],
            privateKeyFileName: [null, []],
            cert: [null, []],
            certFileName: [null, []]
        });
        this.subscriptions.push(this.credentialsConfigFormGroup.valueChanges.pipe(distinctUntilChanged()).subscribe(() => {
            this.updateView();
        }));
        this.subscriptions.push(this.credentialsConfigFormGroup.get('type').valueChanges.subscribe(() => {
            this.credentialsTypeChanged();
        }));
    }
    ngOnChanges(changes) {
        for (const propName of Object.keys(changes)) {
            const change = changes[propName];
            if (!change.firstChange && change.currentValue !== change.previousValue) {
                if (change.currentValue && propName === 'disableCertPemCredentials') {
                    const credentialsTypeValue = this.credentialsConfigFormGroup.get('type').value;
                    if (credentialsTypeValue === 'cert.PEM') {
                        setTimeout(() => {
                            this.credentialsConfigFormGroup.get('type').patchValue('anonymous', { emitEvent: true });
                        });
                    }
                }
            }
        }
    }
    ngOnDestroy() {
        this.subscriptions.forEach(s => s.unsubscribe());
    }
    writeValue(credentials) {
        if (isDefinedAndNotNull(credentials)) {
            this.credentialsConfigFormGroup.reset(credentials, { emitEvent: false });
            this.updateValidators(false);
        }
    }
    setDisabledState(isDisabled) {
        if (isDisabled) {
            this.credentialsConfigFormGroup.disable();
        }
        else {
            this.credentialsConfigFormGroup.enable();
            this.updateValidators();
        }
    }
    updateView() {
        let credentialsConfigValue = this.credentialsConfigFormGroup.value;
        const credentialsTypeValue = credentialsConfigValue.type;
        switch (credentialsTypeValue) {
            case 'anonymous':
                credentialsConfigValue = {
                    type: credentialsTypeValue
                };
                break;
            case 'basic':
                credentialsConfigValue = {
                    type: credentialsTypeValue,
                    username: credentialsConfigValue.username,
                    password: credentialsConfigValue.password,
                };
                break;
            case 'cert.PEM':
                delete credentialsConfigValue.username;
                break;
        }
        this.propagateChange(credentialsConfigValue);
    }
    registerOnChange(fn) {
        this.propagateChange = fn;
    }
    registerOnTouched(fn) {
    }
    validate(c) {
        return this.credentialsConfigFormGroup.valid ? null : {
            credentialsConfig: {
                valid: false,
            },
        };
    }
    credentialsTypeChanged() {
        this.credentialsConfigFormGroup.patchValue({
            username: null,
            password: null,
            caCert: null,
            caCertFileName: null,
            privateKey: null,
            privateKeyFileName: null,
            cert: null,
            certFileName: null,
        });
        this.updateValidators();
    }
    updateValidators(emitEvent = false) {
        const credentialsTypeValue = this.credentialsConfigFormGroup.get('type').value;
        if (emitEvent) {
            this.credentialsConfigFormGroup.reset({ type: credentialsTypeValue }, { emitEvent: false });
        }
        this.credentialsConfigFormGroup.setValidators([]);
        this.credentialsConfigFormGroup.get('username').setValidators([]);
        this.credentialsConfigFormGroup.get('password').setValidators([]);
        switch (credentialsTypeValue) {
            case 'anonymous':
                break;
            case 'basic':
                this.credentialsConfigFormGroup.get('username').setValidators([Validators.required]);
                this.credentialsConfigFormGroup.get('password').setValidators(this.passwordFieldRquired ? [Validators.required] : []);
                break;
            case 'cert.PEM':
                this.credentialsConfigFormGroup.setValidators([this.requiredFilesSelected(Validators.required, [['caCert', 'caCertFileName'], ['privateKey', 'privateKeyFileName', 'cert', 'certFileName']])]);
                break;
        }
        this.credentialsConfigFormGroup.get('username').updateValueAndValidity({ emitEvent });
        this.credentialsConfigFormGroup.get('password').updateValueAndValidity({ emitEvent });
        this.credentialsConfigFormGroup.updateValueAndValidity({ emitEvent });
    }
    requiredFilesSelected(validator, requiredFieldsSet = null) {
        return (group) => {
            if (!requiredFieldsSet) {
                requiredFieldsSet = [Object.keys(group.controls)];
            }
            const allRequiredFilesSelected = (group === null || group === void 0 ? void 0 : group.controls) &&
                requiredFieldsSet.some(arrFields => arrFields.every(k => !validator(group.controls[k])));
            return allRequiredFilesSelected ? null : { notAllRequiredFilesSelected: true };
        };
    }
}
CredentialsConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CredentialsConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
CredentialsConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: CredentialsConfigComponent, selector: "tb-credentials-config", inputs: { required: "required", disableCertPemCredentials: "disableCertPemCredentials", passwordFieldRquired: "passwordFieldRquired" }, providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => CredentialsConfigComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => CredentialsConfigComponent),
            multi: true,
        }
    ], usesInheritance: true, usesOnChanges: true, ngImport: i0, template: "<section [formGroup]=\"credentialsConfigFormGroup\" fxLayout=\"column\">\n  <mat-expansion-panel class=\"tb-credentials-config-panel-group\">\n    <mat-expansion-panel-header>\n      <mat-panel-title translate>tb.rulenode.credentials</mat-panel-title>\n      <mat-panel-description>\n        {{ credentialsTypeTranslationsMap.get(credentialsConfigFormGroup.get('type').value) | translate }}\n      </mat-panel-description>\n    </mat-expansion-panel-header>\n    <ng-template matExpansionPanelContent>\n      <mat-form-field class=\"mat-block\">\n        <mat-label translate>tb.rulenode.credentials-type</mat-label>\n        <mat-select formControlName=\"type\" required>\n          <mat-option *ngFor=\"let credentialsType of allCredentialsTypes\" [value]=\"credentialsType\" [disabled]=\"credentialsType === 'cert.PEM' && disableCertPemCredentials\">\n            {{ credentialsTypeTranslationsMap.get(credentialsType) | translate }}\n          </mat-option>\n        </mat-select>\n        <mat-error *ngIf=\"credentialsConfigFormGroup.get('type').hasError('required')\">\n          {{ 'tb.rulenode.credentials-type-required' | translate }}\n        </mat-error>\n      </mat-form-field>\n      <section fxLayout=\"column\" [ngSwitch]=\"credentialsConfigFormGroup.get('type').value\">\n        <ng-template ngSwitchCase=\"anonymous\">\n        </ng-template>\n        <ng-template ngSwitchCase=\"basic\">\n          <mat-form-field class=\"mat-block\">\n            <mat-label translate>tb.rulenode.username</mat-label>\n            <input required matInput formControlName=\"username\">\n            <mat-error *ngIf=\"credentialsConfigFormGroup.get('username').hasError('required')\">\n              {{ 'tb.rulenode.username-required' | translate }}\n            </mat-error>\n          </mat-form-field>\n          <mat-form-field class=\"mat-block\">\n            <mat-label translate>tb.rulenode.password</mat-label>\n            <input type=\"password\" [required]=\"passwordFieldRquired\" matInput formControlName=\"password\">\n            <tb-toggle-password matSuffix></tb-toggle-password>\n            <mat-error *ngIf=\"credentialsConfigFormGroup.get('password').hasError('required') && passwordFieldRquired\">\n              {{ 'tb.rulenode.password-required' | translate }}\n            </mat-error>\n          </mat-form-field>\n        </ng-template>\n        <ng-template ngSwitchCase=\"cert.PEM\">\n          <div class=\"tb-hint\">{{ 'tb.rulenode.credentials-pem-hint' | translate }}</div>\n          <tb-file-input formControlName=\"caCert\"\n                         inputId=\"caCertSelect\"\n                         [existingFileName]=\"credentialsConfigFormGroup.get('caCertFileName').value\"\n                         (fileNameChanged)=\"credentialsConfigFormGroup.get('caCertFileName').setValue($event)\"\n                         label=\"{{'tb.rulenode.ca-cert' | translate}}\"\n                         noFileText=\"tb.rulenode.no-file\"\n                         dropLabel=\"{{'tb.rulenode.drop-file' | translate}}\">\n          </tb-file-input>\n          <tb-file-input formControlName=\"cert\"\n                         inputId=\"CertSelect\"\n                         [existingFileName]=\"credentialsConfigFormGroup.get('certFileName').value\"\n                         (fileNameChanged)=\"credentialsConfigFormGroup.get('certFileName').setValue($event)\"\n                         label=\"{{'tb.rulenode.cert' | translate}}\"\n                         noFileText=\"tb.rulenode.no-file\"\n                         dropLabel=\"{{'tb.rulenode.drop-file' | translate}}\">\n          </tb-file-input>\n          <tb-file-input style=\"padding-bottom: 8px;\"\n                         formControlName=\"privateKey\"\n                         inputId=\"privateKeySelect\"\n                         [existingFileName]=\"credentialsConfigFormGroup.get('privateKeyFileName').value\"\n                         (fileNameChanged)=\"credentialsConfigFormGroup.get('privateKeyFileName').setValue($event)\"\n                         label=\"{{'tb.rulenode.private-key' | translate}}\"\n                         noFileText=\"tb.rulenode.no-file\"\n                         dropLabel=\"{{'tb.rulenode.drop-file' | translate}}\">\n          </tb-file-input>\n          <mat-form-field class=\"mat-block\">\n            <mat-label translate>tb.rulenode.private-key-password</mat-label>\n            <input type=\"password\" matInput formControlName=\"password\">\n            <tb-toggle-password matSuffix></tb-toggle-password>\n          </mat-form-field>\n        </ng-template>\n      </section>\n    </ng-template>\n  </mat-expansion-panel>\n</section>\n", components: [{ type: i4$2.MatExpansionPanel, selector: "mat-expansion-panel", inputs: ["disabled", "expanded", "hideToggle", "togglePosition"], outputs: ["opened", "closed", "expandedChange", "afterExpand", "afterCollapse"], exportAs: ["matExpansionPanel"] }, { type: i4$2.MatExpansionPanelHeader, selector: "mat-expansion-panel-header", inputs: ["tabIndex", "expandedHeight", "collapsedHeight"] }, { type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i7.TogglePasswordComponent, selector: "tb-toggle-password" }, { type: i8$1.FileInputComponent, selector: "tb-file-input", inputs: ["label", "accept", "noFileText", "inputId", "allowedExtensions", "dropLabel", "contentConvertFunction", "required", "requiredAsError", "disabled", "existingFileName", "readAsBinary", "workFromFileObj", "multipleFile"], outputs: ["fileNameChanged"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4$2.MatExpansionPanelTitle, selector: "mat-panel-title" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i4$2.MatExpansionPanelDescription, selector: "mat-panel-description" }, { type: i4$2.MatExpansionPanelContent, selector: "ng-template[matExpansionPanelContent]" }, { type: i3.MatLabel, selector: "mat-label" }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i10.NgSwitch, selector: "[ngSwitch]", inputs: ["ngSwitch"] }, { type: i10.NgSwitchCase, selector: "[ngSwitchCase]", inputs: ["ngSwitchCase"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i3.MatSuffix, selector: "[matSuffix]" }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CredentialsConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-credentials-config',
                    templateUrl: './credentials-config.component.html',
                    styleUrls: [],
                    providers: [
                        {
                            provide: NG_VALUE_ACCESSOR,
                            useExisting: forwardRef(() => CredentialsConfigComponent),
                            multi: true
                        },
                        {
                            provide: NG_VALIDATORS,
                            useExisting: forwardRef(() => CredentialsConfigComponent),
                            multi: true,
                        }
                    ]
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; }, propDecorators: { required: [{
                type: Input
            }], disableCertPemCredentials: [{
                type: Input
            }], passwordFieldRquired: [{
                type: Input
            }] } });

class MqttConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.mqttConfigForm;
    }
    onConfigurationSet(configuration) {
        this.mqttConfigForm = this.fb.group({
            topicPattern: [configuration ? configuration.topicPattern : null, [Validators.required]],
            host: [configuration ? configuration.host : null, [Validators.required]],
            port: [configuration ? configuration.port : null, [Validators.required, Validators.min(1), Validators.max(65535)]],
            connectTimeoutSec: [configuration ? configuration.connectTimeoutSec : null,
                [Validators.required, Validators.min(1), Validators.max(200)]],
            clientId: [configuration ? configuration.clientId : null, []],
            cleanSession: [configuration ? configuration.cleanSession : false, []],
            ssl: [configuration ? configuration.ssl : false, []],
            credentials: [configuration ? configuration.credentials : null, []]
        });
    }
}
MqttConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: MqttConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
MqttConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: MqttConfigComponent, selector: "tb-action-node-mqtt-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"mqttConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.topic-pattern</mat-label>\n    <input required matInput formControlName=\"topicPattern\">\n    <mat-error *ngIf=\"mqttConfigForm.get('topicPattern').hasError('required')\">\n      {{ 'tb.rulenode.topic-pattern-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <div fxFlex fxLayout=\"column\" fxLayout.gt-sm=\"row\" fxLayoutGap.gt-sm=\"8px\">\n    <mat-form-field fxFlex=\"60\" class=\"mat-block\">\n      <mat-label translate>tb.rulenode.host</mat-label>\n      <input required matInput formControlName=\"host\">\n      <mat-error *ngIf=\"mqttConfigForm.get('host').hasError('required')\">\n        {{ 'tb.rulenode.host-required' | translate }}\n      </mat-error>\n    </mat-form-field>\n    <mat-form-field fxFlex=\"40\" class=\"mat-block\">\n      <mat-label translate>tb.rulenode.port</mat-label>\n      <input required type=\"number\" step=\"1\" min=\"1\" max=\"65535\" matInput formControlName=\"port\">\n      <mat-error *ngIf=\"mqttConfigForm.get('port').hasError('required')\">\n        {{ 'tb.rulenode.port-required' | translate }}\n      </mat-error>\n      <mat-error *ngIf=\"mqttConfigForm.get('port').hasError('min')\">\n        {{ 'tb.rulenode.port-range' | translate }}\n      </mat-error>\n      <mat-error *ngIf=\"mqttConfigForm.get('port').hasError('max')\">\n        {{ 'tb.rulenode.port-range' | translate }}\n      </mat-error>\n    </mat-form-field>\n    <mat-form-field fxFlex=\"40\" class=\"mat-block\">\n      <mat-label translate>tb.rulenode.connect-timeout</mat-label>\n      <input required type=\"number\" step=\"1\" min=\"1\" max=\"200\" matInput formControlName=\"connectTimeoutSec\">\n      <mat-error *ngIf=\"mqttConfigForm.get('connectTimeoutSec').hasError('required')\">\n        {{ 'tb.rulenode.connect-timeout-required' | translate }}\n      </mat-error>\n      <mat-error *ngIf=\"mqttConfigForm.get('connectTimeoutSec').hasError('min')\">\n        {{ 'tb.rulenode.connect-timeout-range' | translate }}\n      </mat-error>\n      <mat-error *ngIf=\"mqttConfigForm.get('connectTimeoutSec').hasError('max')\">\n        {{ 'tb.rulenode.connect-timeout-range' | translate }}\n      </mat-error>\n    </mat-form-field>\n  </div>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.client-id</mat-label>\n    <input matInput formControlName=\"clientId\">\n  </mat-form-field>\n  <div class=\"tb-hint client-id\" translate>tb.rulenode.client-id-hint</div>\n  <mat-checkbox formControlName=\"cleanSession\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.clean-session' | translate }}\n  </mat-checkbox>\n  <mat-checkbox formControlName=\"ssl\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.enable-ssl' | translate }}\n  </mat-checkbox>\n  <tb-credentials-config formControlName=\"credentials\" [passwordFieldRquired]=\"false\"></tb-credentials-config>\n</section>\n", styles: [":host .tb-mqtt-credentials-panel-group{margin:0 6px}:host .tb-hint.client-id{margin-top:-1.25em;max-width:-moz-fit-content;max-width:fit-content}\n"], components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: CredentialsConfigComponent, selector: "tb-credentials-config", inputs: ["required", "disableCertPemCredentials", "passwordFieldRquired"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.MaxValidator, selector: "input[type=number][max][formControlName],input[type=number][max][formControl],input[type=number][max][ngModel]", inputs: ["max"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: MqttConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-mqtt-config',
                    templateUrl: './mqtt-config.component.html',
                    styleUrls: ['./mqtt-config.component.scss']
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class MsgCountConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.msgCountConfigForm;
    }
    onConfigurationSet(configuration) {
        this.msgCountConfigForm = this.fb.group({
            interval: [configuration ? configuration.interval : null, [Validators.required, Validators.min(1)]],
            telemetryPrefix: [configuration ? configuration.telemetryPrefix : null, [Validators.required]]
        });
    }
}
MsgCountConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: MsgCountConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
MsgCountConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: MsgCountConfigComponent, selector: "tb-action-node-msg-count-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"msgCountConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.interval-seconds</mat-label>\n    <input required type=\"number\" min=\"1\" step=\"1\" matInput formControlName=\"interval\">\n    <mat-error *ngIf=\"msgCountConfigForm.get('interval').hasError('required')\">\n      {{ 'tb.rulenode.interval-seconds-required' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"msgCountConfigForm.get('interval').hasError('min')\">\n      {{ 'tb.rulenode.min-interval-seconds-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.output-timeseries-key-prefix</mat-label>\n    <input required matInput formControlName=\"telemetryPrefix\">\n    <mat-error *ngIf=\"msgCountConfigForm.get('telemetryPrefix').hasError('required')\">\n      {{ 'tb.rulenode.output-timeseries-key-prefix-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: MsgCountConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-msg-count-config',
                    templateUrl: './msg-count-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class MsgDelayConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.msgDelayConfigForm;
    }
    onConfigurationSet(configuration) {
        this.msgDelayConfigForm = this.fb.group({
            useMetadataPeriodInSecondsPatterns: [configuration ? configuration.useMetadataPeriodInSecondsPatterns : false, []],
            periodInSeconds: [configuration ? configuration.periodInSeconds : null, []],
            periodInSecondsPattern: [configuration ? configuration.periodInSecondsPattern : null, []],
            maxPendingMsgs: [configuration ? configuration.maxPendingMsgs : null,
                [Validators.required, Validators.min(1), Validators.max(100000)]],
        });
    }
    validatorTriggers() {
        return ['useMetadataPeriodInSecondsPatterns'];
    }
    updateValidators(emitEvent) {
        const useMetadataPeriodInSecondsPatterns = this.msgDelayConfigForm.get('useMetadataPeriodInSecondsPatterns').value;
        if (useMetadataPeriodInSecondsPatterns) {
            this.msgDelayConfigForm.get('periodInSecondsPattern').setValidators([Validators.required]);
            this.msgDelayConfigForm.get('periodInSeconds').setValidators([]);
        }
        else {
            this.msgDelayConfigForm.get('periodInSecondsPattern').setValidators([]);
            this.msgDelayConfigForm.get('periodInSeconds').setValidators([Validators.required, Validators.min(0)]);
        }
        this.msgDelayConfigForm.get('periodInSecondsPattern').updateValueAndValidity({ emitEvent });
        this.msgDelayConfigForm.get('periodInSeconds').updateValueAndValidity({ emitEvent });
    }
}
MsgDelayConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: MsgDelayConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
MsgDelayConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: MsgDelayConfigComponent, selector: "tb-action-node-msg-delay-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"msgDelayConfigForm\" fxLayout=\"column\">\n  <mat-checkbox formControlName=\"useMetadataPeriodInSecondsPatterns\">\n    {{ 'tb.rulenode.use-metadata-period-in-seconds-patterns' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" style=\"padding-bottom: 16px;\" translate>tb.rulenode.use-metadata-period-in-seconds-patterns-hint</div>\n  <mat-form-field *ngIf=\"msgDelayConfigForm.get('useMetadataPeriodInSecondsPatterns').value !== true; else periodInSecondsPattern\"\n                  class=\"mat-block\">\n    <mat-label translate>tb.rulenode.period-seconds</mat-label>\n    <input required type=\"number\" min=\"0\" step=\"1\" matInput formControlName=\"periodInSeconds\">\n    <mat-error *ngIf=\"msgDelayConfigForm.get('periodInSeconds').hasError('required')\">\n      {{ 'tb.rulenode.period-seconds-required' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"msgDelayConfigForm.get('periodInSeconds').hasError('min')\">\n      {{ 'tb.rulenode.min-period-0-seconds-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <ng-template #periodInSecondsPattern>\n    <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n      <mat-label translate>tb.rulenode.period-in-seconds-pattern</mat-label>\n      <input required matInput formControlName=\"periodInSecondsPattern\">\n      <mat-error *ngIf=\"msgDelayConfigForm.get('periodInSecondsPattern').hasError('required')\">\n        {{ 'tb.rulenode.period-in-seconds-pattern-required' | translate }}\n      </mat-error>\n      <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n    </mat-form-field>\n  </ng-template>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.max-pending-messages</mat-label>\n    <input required type=\"number\" min=\"1\" max=\"100000\" step=\"1\" matInput formControlName=\"maxPendingMsgs\">\n    <mat-error *ngIf=\"msgDelayConfigForm.get('maxPendingMsgs').hasError('required')\">\n      {{ 'tb.rulenode.max-pending-messages-required' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"msgDelayConfigForm.get('maxPendingMsgs').hasError('min')\">\n      {{ 'tb.rulenode.max-pending-messages-range' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"msgDelayConfigForm.get('maxPendingMsgs').hasError('max')\">\n      {{ 'tb.rulenode.max-pending-messages-range' | translate }}\n    </mat-error>\n  </mat-form-field>\n</section>\n", components: [{ type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i2.MaxValidator, selector: "input[type=number][max][formControlName],input[type=number][max][formControl],input[type=number][max][ngModel]", inputs: ["max"] }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: MsgDelayConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-msg-delay-config',
                    templateUrl: './msg-delay-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class PubSubConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.pubSubConfigForm;
    }
    onConfigurationSet(configuration) {
        this.pubSubConfigForm = this.fb.group({
            projectId: [configuration ? configuration.projectId : null, [Validators.required]],
            topicName: [configuration ? configuration.topicName : null, [Validators.required]],
            serviceAccountKey: [configuration ? configuration.serviceAccountKey : null, [Validators.required]],
            serviceAccountKeyFileName: [configuration ? configuration.serviceAccountKeyFileName : null, [Validators.required]],
            messageAttributes: [configuration ? configuration.messageAttributes : null, []]
        });
    }
}
PubSubConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: PubSubConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
PubSubConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: PubSubConfigComponent, selector: "tb-action-node-pub-sub-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"pubSubConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.gcp-project-id</mat-label>\n    <input required matInput formControlName=\"projectId\">\n    <mat-error *ngIf=\"pubSubConfigForm.get('projectId').hasError('required')\">\n      {{ 'tb.rulenode.gcp-project-id-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.pubsub-topic-name</mat-label>\n    <input required matInput formControlName=\"topicName\">\n    <mat-error *ngIf=\"pubSubConfigForm.get('topicName').hasError('required')\">\n      {{ 'tb.rulenode.pubsub-topic-name-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <tb-file-input style=\"padding-bottom: 24px;\"\n                 formControlName=\"serviceAccountKey\"\n                 [existingFileName]=\"pubSubConfigForm.get('serviceAccountKeyFileName').value\"\n                 (fileNameChanged)=\"pubSubConfigForm.get('serviceAccountKeyFileName').setValue($event)\"\n                 required\n                 requiredAsError\n                 label=\"{{'tb.rulenode.gcp-service-account-key' | translate}}\"\n                 noFileText=\"tb.rulenode.no-file\"\n                 dropLabel=\"{{'tb.rulenode.drop-file' | translate}}\">\n  </tb-file-input>\n  <label translate class=\"tb-title\">tb.rulenode.message-attributes</label>\n  <div class=\"tb-hint\" [innerHTML]=\"'tb.rulenode.message-attributes-hint' | translate | safeHtml\"></div>\n  <tb-kv-map-config\n    required=\"false\"\n    formControlName=\"messageAttributes\"\n    keyText=\"tb.rulenode.name\"\n    keyRequiredText=\"tb.rulenode.name-required\"\n    valText=\"tb.rulenode.value\"\n    valRequiredText=\"tb.rulenode.value-required\">\n  </tb-kv-map-config>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i8$1.FileInputComponent, selector: "tb-file-input", inputs: ["label", "accept", "noFileText", "inputId", "allowedExtensions", "dropLabel", "contentConvertFunction", "required", "requiredAsError", "disabled", "existingFileName", "readAsBinary", "workFromFileObj", "multipleFile"], outputs: ["fileNameChanged"] }, { type: KvMapConfigComponent, selector: "tb-kv-map-config", inputs: ["disabled", "requiredText", "keyText", "keyRequiredText", "valText", "valRequiredText", "required"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: PubSubConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-pub-sub-config',
                    templateUrl: './pubsub-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class PushToCloudConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.attributeScopes = Object.keys(AttributeScope);
        this.telemetryTypeTranslationsMap = telemetryTypeTranslations;
    }
    configForm() {
        return this.pushToCloudConfigForm;
    }
    onConfigurationSet(configuration) {
        this.pushToCloudConfigForm = this.fb.group({
            scope: [configuration ? configuration.scope : null, [Validators.required]]
        });
    }
}
PushToCloudConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: PushToCloudConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
PushToCloudConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: PushToCloudConfigComponent, selector: "tb-action-node-push-to-cloud-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"pushToCloudConfigForm\" fxLayout=\"column\">\n  <mat-form-field fxFlex class=\"mat-block\">\n    <mat-label translate>attribute.attributes-scope</mat-label>\n    <mat-select formControlName=\"scope\" required>\n      <mat-option *ngFor=\"let scope of attributeScopes\" [value]=\"scope\">\n        {{ telemetryTypeTranslationsMap.get(scope) | translate }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: PushToCloudConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-push-to-cloud-config',
                    templateUrl: './push-to-cloud-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class PushToEdgeConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.attributeScopes = Object.keys(AttributeScope);
        this.telemetryTypeTranslationsMap = telemetryTypeTranslations;
    }
    configForm() {
        return this.pushToEdgeConfigForm;
    }
    onConfigurationSet(configuration) {
        this.pushToEdgeConfigForm = this.fb.group({
            scope: [configuration ? configuration.scope : null, [Validators.required]]
        });
    }
}
PushToEdgeConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: PushToEdgeConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
PushToEdgeConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: PushToEdgeConfigComponent, selector: "tb-action-node-push-to-edge-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"pushToEdgeConfigForm\" fxLayout=\"column\">\n  <mat-form-field fxFlex class=\"mat-block\">\n    <mat-label translate>attribute.attributes-scope</mat-label>\n    <mat-select formControlName=\"scope\" required>\n      <mat-option *ngFor=\"let scope of attributeScopes\" [value]=\"scope\">\n        {{ telemetryTypeTranslationsMap.get(scope) | translate }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: PushToEdgeConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-push-to-edge-config',
                    templateUrl: './push-to-edge-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class RabbitMqConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.messageProperties = [
            null,
            'BASIC',
            'TEXT_PLAIN',
            'MINIMAL_BASIC',
            'MINIMAL_PERSISTENT_BASIC',
            'PERSISTENT_BASIC',
            'PERSISTENT_TEXT_PLAIN'
        ];
    }
    configForm() {
        return this.rabbitMqConfigForm;
    }
    onConfigurationSet(configuration) {
        this.rabbitMqConfigForm = this.fb.group({
            exchangeNamePattern: [configuration ? configuration.exchangeNamePattern : null, []],
            routingKeyPattern: [configuration ? configuration.routingKeyPattern : null, []],
            messageProperties: [configuration ? configuration.messageProperties : null, []],
            host: [configuration ? configuration.host : null, [Validators.required]],
            port: [configuration ? configuration.port : null, [Validators.required, Validators.min(1), Validators.max(65535)]],
            virtualHost: [configuration ? configuration.virtualHost : null, []],
            username: [configuration ? configuration.username : null, []],
            password: [configuration ? configuration.password : null, []],
            automaticRecoveryEnabled: [configuration ? configuration.automaticRecoveryEnabled : false, []],
            connectionTimeout: [configuration ? configuration.connectionTimeout : null, [Validators.min(0)]],
            handshakeTimeout: [configuration ? configuration.handshakeTimeout : null, [Validators.min(0)]],
            clientProperties: [configuration ? configuration.clientProperties : null, []]
        });
    }
}
RabbitMqConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RabbitMqConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
RabbitMqConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: RabbitMqConfigComponent, selector: "tb-action-node-rabbit-mq-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"rabbitMqConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.exchange-name-pattern</mat-label>\n    <input matInput formControlName=\"exchangeNamePattern\">\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.routing-key-pattern</mat-label>\n    <input matInput formControlName=\"routingKeyPattern\">\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.message-properties</mat-label>\n    <mat-select formControlName=\"messageProperties\">\n      <mat-option *ngFor=\"let property of messageProperties\" [value]=\"property\">\n        {{ property }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n  <div fxLayout.gt-sm=\"row\" fxLayoutGap.gt-sm=\"8px\">\n    <mat-form-field fxFlex=\"100\" fxFlex.gt-sm=\"60\" class=\"mat-block\">\n      <mat-label translate>tb.rulenode.host</mat-label>\n      <input required matInput formControlName=\"host\">\n      <mat-error *ngIf=\"rabbitMqConfigForm.get('host').hasError('required')\">\n        {{ 'tb.rulenode.host-required' | translate }}\n      </mat-error>\n    </mat-form-field>\n    <mat-form-field fxFlex=\"100\" fxFlex.gt-sm=\"40\" class=\"mat-block\">\n      <mat-label translate>tb.rulenode.port</mat-label>\n      <input required type=\"number\" step=\"1\" min=\"1\" max=\"65535\" matInput formControlName=\"port\">\n      <mat-error *ngIf=\"rabbitMqConfigForm.get('port').hasError('required')\">\n        {{ 'tb.rulenode.port-required' | translate }}\n      </mat-error>\n      <mat-error *ngIf=\"rabbitMqConfigForm.get('port').hasError('min')\">\n        {{ 'tb.rulenode.port-range' | translate }}\n      </mat-error>\n      <mat-error *ngIf=\"rabbitMqConfigForm.get('port').hasError('max')\">\n        {{ 'tb.rulenode.port-range' | translate }}\n      </mat-error>\n    </mat-form-field>\n  </div>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.virtual-host</mat-label>\n    <input matInput formControlName=\"virtualHost\">\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.username</mat-label>\n    <input matInput formControlName=\"username\">\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.password</mat-label>\n    <input type=\"password\" matInput formControlName=\"password\">\n    <tb-toggle-password matSuffix></tb-toggle-password>\n  </mat-form-field>\n  <mat-checkbox formControlName=\"automaticRecoveryEnabled\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.automatic-recovery' | translate }}\n  </mat-checkbox>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.connection-timeout-ms</mat-label>\n    <input type=\"number\" step=\"1\" min=\"0\" matInput formControlName=\"connectionTimeout\">\n    <mat-error *ngIf=\"rabbitMqConfigForm.get('connectionTimeout').hasError('min')\">\n      {{ 'tb.rulenode.min-connection-timeout-ms-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.handshake-timeout-ms</mat-label>\n    <input type=\"number\" step=\"1\" min=\"0\" matInput formControlName=\"handshakeTimeout\">\n    <mat-error *ngIf=\"rabbitMqConfigForm.get('handshakeTimeout').hasError('min')\">\n      {{ 'tb.rulenode.min-handshake-timeout-ms-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <label translate class=\"tb-title\">tb.rulenode.client-properties</label>\n  <tb-kv-map-config\n    required=\"false\"\n    formControlName=\"clientProperties\"\n    keyText=\"tb.rulenode.key\"\n    keyRequiredText=\"tb.rulenode.key-required\"\n    valText=\"tb.rulenode.value\"\n    valRequiredText=\"tb.rulenode.value-required\">\n  </tb-kv-map-config>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i7.TogglePasswordComponent, selector: "tb-toggle-password" }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: KvMapConfigComponent, selector: "tb-kv-map-config", inputs: ["disabled", "requiredText", "keyText", "keyRequiredText", "valText", "valRequiredText", "required"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.MaxValidator, selector: "input[type=number][max][formControlName],input[type=number][max][formControl],input[type=number][max][ngModel]", inputs: ["max"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i3.MatSuffix, selector: "[matSuffix]" }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RabbitMqConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-rabbit-mq-config',
                    templateUrl: './rabbit-mq-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class RestApiCallConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.proxySchemes = ['http', 'https'];
        this.httpRequestTypes = Object.keys(HttpRequestType);
    }
    configForm() {
        return this.restApiCallConfigForm;
    }
    onConfigurationSet(configuration) {
        this.restApiCallConfigForm = this.fb.group({
            restEndpointUrlPattern: [configuration ? configuration.restEndpointUrlPattern : null, [Validators.required]],
            requestMethod: [configuration ? configuration.requestMethod : null, [Validators.required]],
            useSimpleClientHttpFactory: [configuration ? configuration.useSimpleClientHttpFactory : false, []],
            ignoreRequestBody: [configuration ? configuration.ignoreRequestBody : false, []],
            enableProxy: [configuration ? configuration.enableProxy : false, []],
            useSystemProxyProperties: [configuration ? configuration.enableProxy : false, []],
            proxyScheme: [configuration ? configuration.proxyHost : null, []],
            proxyHost: [configuration ? configuration.proxyHost : null, []],
            proxyPort: [configuration ? configuration.proxyPort : null, []],
            proxyUser: [configuration ? configuration.proxyUser : null, []],
            proxyPassword: [configuration ? configuration.proxyPassword : null, []],
            readTimeoutMs: [configuration ? configuration.readTimeoutMs : null, []],
            maxParallelRequestsCount: [configuration ? configuration.maxParallelRequestsCount : null, [Validators.min(0)]],
            headers: [configuration ? configuration.headers : null, []],
            useRedisQueueForMsgPersistence: [configuration ? configuration.useRedisQueueForMsgPersistence : false, []],
            trimQueue: [configuration ? configuration.trimQueue : false, []],
            maxQueueSize: [configuration ? configuration.maxQueueSize : null, []],
            credentials: [configuration ? configuration.credentials : null, []]
        });
    }
    validatorTriggers() {
        return ['useSimpleClientHttpFactory', 'useRedisQueueForMsgPersistence', 'enableProxy', 'useSystemProxyProperties'];
    }
    updateValidators(emitEvent) {
        const useSimpleClientHttpFactory = this.restApiCallConfigForm.get('useSimpleClientHttpFactory').value;
        const useRedisQueueForMsgPersistence = this.restApiCallConfigForm.get('useRedisQueueForMsgPersistence').value;
        const enableProxy = this.restApiCallConfigForm.get('enableProxy').value;
        const useSystemProxyProperties = this.restApiCallConfigForm.get('useSystemProxyProperties').value;
        if (enableProxy && !useSystemProxyProperties) {
            this.restApiCallConfigForm.get('proxyHost').setValidators(enableProxy ? [Validators.required] : []);
            this.restApiCallConfigForm.get('proxyPort').setValidators(enableProxy ?
                [Validators.required, Validators.min(1), Validators.max(65535)] : []);
        }
        else {
            this.restApiCallConfigForm.get('proxyHost').setValidators([]);
            this.restApiCallConfigForm.get('proxyPort').setValidators([]);
            if (useSimpleClientHttpFactory) {
                this.restApiCallConfigForm.get('readTimeoutMs').setValidators([]);
            }
            else {
                this.restApiCallConfigForm.get('readTimeoutMs').setValidators([Validators.min(0)]);
            }
        }
        if (useRedisQueueForMsgPersistence) {
            this.restApiCallConfigForm.get('maxQueueSize').setValidators([Validators.min(0)]);
        }
        else {
            this.restApiCallConfigForm.get('maxQueueSize').setValidators([]);
        }
        this.restApiCallConfigForm.get('readTimeoutMs').updateValueAndValidity({ emitEvent });
        this.restApiCallConfigForm.get('maxQueueSize').updateValueAndValidity({ emitEvent });
        this.restApiCallConfigForm.get('proxyHost').updateValueAndValidity({ emitEvent });
        this.restApiCallConfigForm.get('proxyPort').updateValueAndValidity({ emitEvent });
        this.restApiCallConfigForm.get('credentials').updateValueAndValidity({ emitEvent });
    }
}
RestApiCallConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RestApiCallConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
RestApiCallConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: RestApiCallConfigComponent, selector: "tb-action-node-rest-api-call-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"restApiCallConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.endpoint-url-pattern</mat-label>\n    <input required matInput formControlName=\"restEndpointUrlPattern\">\n    <mat-error *ngIf=\"restApiCallConfigForm.get('restEndpointUrlPattern').hasError('required')\">\n      {{ 'tb.rulenode.endpoint-url-pattern-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.request-method</mat-label>\n    <mat-select formControlName=\"requestMethod\">\n      <mat-option *ngFor=\"let requestType of httpRequestTypes\" [value]=\"requestType\">\n        {{ requestType }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n  <mat-checkbox formControlName=\"enableProxy\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.enable-proxy' | translate }}\n  </mat-checkbox>\n  <mat-checkbox *ngIf=\"!restApiCallConfigForm.get('enableProxy').value\" formControlName=\"useSimpleClientHttpFactory\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.use-simple-client-http-factory' | translate }}\n  </mat-checkbox>\n  <mat-checkbox formControlName=\"ignoreRequestBody\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.ignore-request-body' | translate }}\n  </mat-checkbox>\n  <div *ngIf=\"restApiCallConfigForm.get('enableProxy').value\">\n    <mat-checkbox formControlName=\"useSystemProxyProperties\" style=\"padding-bottom: 16px;\">\n      {{ 'tb.rulenode.use-system-proxy-properties' | translate }}\n    </mat-checkbox>\n    <div *ngIf=\"!restApiCallConfigForm.get('useSystemProxyProperties').value\">\n      <div fxLayout.gt-sm=\"row\" fxLayoutGap.gt-sm=\"8px\">\n        <mat-form-field class=\"mat-block\" fxFlex=\"100\" fxFlex.gt-sm=\"10\">\n          <mat-label translate>tb.rulenode.proxy-scheme</mat-label>\n          <mat-select formControlName=\"proxyScheme\">\n            <mat-option *ngFor=\"let proxyScheme of proxySchemes\" [value]=\"proxyScheme\">\n              {{ proxyScheme }}\n            </mat-option>\n          </mat-select>\n        </mat-form-field>\n        <mat-form-field class=\"md-block\" fxFlex=\"100\" fxFlex.gt-sm=\"50\">\n          <mat-label translate>tb.rulenode.proxy-host</mat-label>\n          <input matInput required formControlName=\"proxyHost\">\n          <mat-error *ngIf=\"restApiCallConfigForm.get('proxyHost').hasError('required')\">\n            {{ 'tb.rulenode.proxy-host-required' | translate }}\n          </mat-error>\n        </mat-form-field>\n        <mat-form-field class=\"mat-block\" fxFlex=\"100\" fxFlex.gt-sm=\"40\">\n          <mat-label translate>tb.rulenode.proxy-port</mat-label>\n          <input matInput required formControlName=\"proxyPort\" type=\"number\" step=\"1\">\n          <mat-error *ngIf=\"restApiCallConfigForm.get('proxyPort').hasError('required')\">\n            {{ 'tb.rulenode.proxy-port-required' | translate }}\n          </mat-error>\n          <mat-error\n            *ngIf=\"restApiCallConfigForm.get('proxyPort').hasError('min') || restApiCallConfigForm.get('proxyPort').hasError('max')\">\n            {{ 'tb.rulenode.proxy-port-range' | translate }}\n          </mat-error>\n        </mat-form-field>\n      </div>\n      <mat-form-field class=\"mat-block\">\n        <mat-label translate>tb.rulenode.proxy-user</mat-label>\n        <input matInput formControlName=\"proxyUser\">\n      </mat-form-field>\n      <mat-form-field class=\"mat-block\">\n        <mat-label translate>tb.rulenode.proxy-password</mat-label>\n        <input matInput formControlName=\"proxyPassword\">\n      </mat-form-field>\n    </div>\n  </div>\n  <mat-form-field *ngIf=\"!restApiCallConfigForm.get('useSimpleClientHttpFactory').value || restApiCallConfigForm.get('enableProxy').value\" class=\"mat-block\"\n                  style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.read-timeout</mat-label>\n    <input type=\"number\" step=\"1\" min=\"0\" matInput formControlName=\"readTimeoutMs\">\n    <mat-hint translate>tb.rulenode.read-timeout-hint</mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.max-parallel-requests-count</mat-label>\n    <input type=\"number\" step=\"1\" min=\"0\" matInput formControlName=\"maxParallelRequestsCount\">\n    <mat-hint translate>tb.rulenode.max-parallel-requests-count-hint</mat-hint>\n  </mat-form-field>\n  <label translate class=\"tb-title\">tb.rulenode.headers</label>\n  <div class=\"tb-hint\" [innerHTML]=\"'tb.rulenode.headers-hint' | translate | safeHtml\"></div>\n  <tb-kv-map-config\n    required=\"false\"\n    formControlName=\"headers\"\n    keyText=\"tb.rulenode.header\"\n    keyRequiredText=\"tb.rulenode.header-required\"\n    valText=\"tb.rulenode.value\"\n    valRequiredText=\"tb.rulenode.value-required\">\n  </tb-kv-map-config>\n  <mat-checkbox formControlName=\"useRedisQueueForMsgPersistence\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.use-redis-queue' | translate }}\n  </mat-checkbox>\n  <div fxLayout=\"column\" *ngIf=\"restApiCallConfigForm.get('useRedisQueueForMsgPersistence').value === true\">\n    <mat-checkbox formControlName=\"trimQueue\" style=\"padding-bottom: 16px;\">\n      {{ 'tb.rulenode.trim-redis-queue' | translate }}\n    </mat-checkbox>\n    <mat-form-field class=\"mat-block\">\n      <mat-label translate>tb.rulenode.redis-queue-max-size</mat-label>\n      <input type=\"number\" step=\"1\" min=\"0\" matInput formControlName=\"maxQueueSize\">\n    </mat-form-field>\n  </div>\n  <tb-credentials-config formControlName=\"credentials\" [disableCertPemCredentials]=\"restApiCallConfigForm.get('useSimpleClientHttpFactory').value\"></tb-credentials-config>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: KvMapConfigComponent, selector: "tb-kv-map-config", inputs: ["disabled", "requiredText", "keyText", "keyRequiredText", "valText", "valRequiredText", "required"] }, { type: CredentialsConfigComponent, selector: "tb-credentials-config", inputs: ["required", "disableCertPemCredentials", "passwordFieldRquired"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RestApiCallConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-rest-api-call-config',
                    templateUrl: './rest-api-call-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class RpcReplyConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.rpcReplyConfigForm;
    }
    onConfigurationSet(configuration) {
        this.rpcReplyConfigForm = this.fb.group({
            requestIdMetaDataAttribute: [configuration ? configuration.requestIdMetaDataAttribute : null, []]
        });
    }
}
RpcReplyConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RpcReplyConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
RpcReplyConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: RpcReplyConfigComponent, selector: "tb-action-node-rpc-reply-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"rpcReplyConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.request-id-metadata-attribute</mat-label>\n    <input matInput formControlName=\"requestIdMetaDataAttribute\">\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RpcReplyConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-rpc-reply-config',
                    templateUrl: './rpc-reply-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class RpcRequestConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.rpcRequestConfigForm;
    }
    onConfigurationSet(configuration) {
        this.rpcRequestConfigForm = this.fb.group({
            timeoutInSeconds: [configuration ? configuration.timeoutInSeconds : null, [Validators.required, Validators.min(0)]]
        });
    }
}
RpcRequestConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RpcRequestConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
RpcRequestConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: RpcRequestConfigComponent, selector: "tb-action-node-rpc-request-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"rpcRequestConfigForm\" fxLayout=\"column\">\n  <mat-form-field fxFlex class=\"mat-block\">\n    <mat-label translate>tb.rulenode.timeout-sec</mat-label>\n    <input type=\"number\" min=\"0\" step=\"1\" matInput formControlName=\"timeoutInSeconds\" required>\n    <mat-error *ngIf=\"rpcRequestConfigForm.get('timeoutInSeconds').hasError('required')\">\n      {{ 'tb.rulenode.timeout-required' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"rpcRequestConfigForm.get('timeoutInSeconds').hasError('min')\">\n      {{ 'tb.rulenode.min-timeout-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RpcRequestConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-rpc-request-config',
                    templateUrl: './rpc-request-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class SaveToCustomTableConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.saveToCustomTableConfigForm;
    }
    onConfigurationSet(configuration) {
        this.saveToCustomTableConfigForm = this.fb.group({
            tableName: [configuration ? configuration.tableName : null, [Validators.required, Validators.pattern(/.*\S.*/)]],
            fieldsMapping: [configuration ? configuration.fieldsMapping : null, [Validators.required]]
        });
    }
    prepareOutputConfig(configuration) {
        configuration.tableName = configuration.tableName.trim();
        return configuration;
    }
}
SaveToCustomTableConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SaveToCustomTableConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
SaveToCustomTableConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: SaveToCustomTableConfigComponent, selector: "tb-action-node-custom-table-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"saveToCustomTableConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.custom-table-name</mat-label>\n    <input required matInput formControlName=\"tableName\">\n    <mat-error *ngIf=\"saveToCustomTableConfigForm.get('tableName').hasError('required') ||\n                      saveToCustomTableConfigForm.get('tableName').hasError('pattern')\">\n      {{ 'tb.rulenode.custom-table-name-required' | translate }}\n    </mat-error>\n    <mat-hint translate>tb.rulenode.custom-table-hint</mat-hint>\n  </mat-form-field>\n  <label translate class=\"tb-title tb-required\">tb.rulenode.fields-mapping</label>\n  <tb-kv-map-config\n    required\n    formControlName=\"fieldsMapping\"\n    requiredText=\"tb.rulenode.fields-mapping-required\"\n    keyText=\"tb.rulenode.message-field\"\n    keyRequiredText=\"tb.rulenode.message-field-required\"\n    valText=\"tb.rulenode.table-col\"\n    valRequiredText=\"tb.rulenode.table-col-required\">\n  </tb-kv-map-config>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: KvMapConfigComponent, selector: "tb-kv-map-config", inputs: ["disabled", "requiredText", "keyText", "keyRequiredText", "valText", "valRequiredText", "required"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SaveToCustomTableConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-custom-table-config',
                    templateUrl: './save-to-custom-table-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class SendEmailConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.smtpProtocols = [
            'smtp',
            'smtps'
        ];
        this.tlsVersions = ['TLSv1', 'TLSv1.1', 'TLSv1.2', 'TLSv1.3'];
    }
    configForm() {
        return this.sendEmailConfigForm;
    }
    onConfigurationSet(configuration) {
        this.sendEmailConfigForm = this.fb.group({
            useSystemSmtpSettings: [configuration ? configuration.useSystemSmtpSettings : false, []],
            smtpProtocol: [configuration ? configuration.smtpProtocol : null, []],
            smtpHost: [configuration ? configuration.smtpHost : null, []],
            smtpPort: [configuration ? configuration.smtpPort : null, []],
            timeout: [configuration ? configuration.timeout : null, []],
            enableTls: [configuration ? configuration.enableTls : false, []],
            tlsVersion: [configuration ? configuration.tlsVersion : null, []],
            enableProxy: [configuration ? configuration.enableProxy : false, []],
            proxyHost: [configuration ? configuration.proxyHost : null, []],
            proxyPort: [configuration ? configuration.proxyPort : null, []],
            proxyUser: [configuration ? configuration.proxyUser : null, []],
            proxyPassword: [configuration ? configuration.proxyPassword : null, []],
            username: [configuration ? configuration.username : null, []],
            password: [configuration ? configuration.password : null, []]
        });
    }
    validatorTriggers() {
        return ['useSystemSmtpSettings', 'enableProxy'];
    }
    updateValidators(emitEvent) {
        const useSystemSmtpSettings = this.sendEmailConfigForm.get('useSystemSmtpSettings').value;
        const enableProxy = this.sendEmailConfigForm.get('enableProxy').value;
        if (useSystemSmtpSettings) {
            this.sendEmailConfigForm.get('smtpProtocol').setValidators([]);
            this.sendEmailConfigForm.get('smtpHost').setValidators([]);
            this.sendEmailConfigForm.get('smtpPort').setValidators([]);
            this.sendEmailConfigForm.get('timeout').setValidators([]);
            this.sendEmailConfigForm.get('proxyHost').setValidators([]);
            this.sendEmailConfigForm.get('proxyPort').setValidators([]);
        }
        else {
            this.sendEmailConfigForm.get('smtpProtocol').setValidators([Validators.required]);
            this.sendEmailConfigForm.get('smtpHost').setValidators([Validators.required]);
            this.sendEmailConfigForm.get('smtpPort').setValidators([Validators.required, Validators.min(1), Validators.max(65535)]);
            this.sendEmailConfigForm.get('timeout').setValidators([Validators.required, Validators.min(0)]);
            this.sendEmailConfigForm.get('proxyHost').setValidators(enableProxy ? [Validators.required] : []);
            this.sendEmailConfigForm.get('proxyPort').setValidators(enableProxy ?
                [Validators.required, Validators.min(1), Validators.max(65535)] : []);
        }
        this.sendEmailConfigForm.get('smtpProtocol').updateValueAndValidity({ emitEvent });
        this.sendEmailConfigForm.get('smtpHost').updateValueAndValidity({ emitEvent });
        this.sendEmailConfigForm.get('smtpPort').updateValueAndValidity({ emitEvent });
        this.sendEmailConfigForm.get('timeout').updateValueAndValidity({ emitEvent });
        this.sendEmailConfigForm.get('proxyHost').updateValueAndValidity({ emitEvent });
        this.sendEmailConfigForm.get('proxyPort').updateValueAndValidity({ emitEvent });
    }
}
SendEmailConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SendEmailConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
SendEmailConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: SendEmailConfigComponent, selector: "tb-action-node-send-email-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"sendEmailConfigForm\" fxLayout=\"column\">\n  <mat-checkbox formControlName=\"useSystemSmtpSettings\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.use-system-smtp-settings' | translate }}\n  </mat-checkbox>\n  <section fxLayout=\"column\" *ngIf=\"sendEmailConfigForm.get('useSystemSmtpSettings').value === false\">\n    <mat-form-field class=\"mat-block\">\n      <mat-label translate>tb.rulenode.smtp-protocol</mat-label>\n      <mat-select formControlName=\"smtpProtocol\">\n        <mat-option *ngFor=\"let smtpProtocol of smtpProtocols\" [value]=\"smtpProtocol\">\n          {{ smtpProtocol.toUpperCase() }}\n        </mat-option>\n      </mat-select>\n    </mat-form-field>\n    <div fxLayout.gt-sm=\"row\" fxLayoutGap.gt-sm=\"8px\">\n      <mat-form-field fxFlex=\"100\" fxFlex.gt-sm=\"60\" class=\"mat-block\">\n        <mat-label translate>tb.rulenode.smtp-host</mat-label>\n        <input required matInput formControlName=\"smtpHost\">\n        <mat-error *ngIf=\"sendEmailConfigForm.get('smtpHost').hasError('required')\">\n          {{ 'tb.rulenode.smtp-host-required' | translate }}\n        </mat-error>\n      </mat-form-field>\n      <mat-form-field fxFlex=\"100\" fxFlex.gt-sm=\"40\" class=\"mat-block\">\n        <mat-label translate>tb.rulenode.smtp-port</mat-label>\n        <input required type=\"number\" step=\"1\" min=\"1\" max=\"65535\" matInput formControlName=\"smtpPort\">\n        <mat-error *ngIf=\"sendEmailConfigForm.get('smtpPort').hasError('required')\">\n          {{ 'tb.rulenode.smtp-port-required' | translate }}\n        </mat-error>\n        <mat-error *ngIf=\"sendEmailConfigForm.get('smtpPort').hasError('min')\">\n          {{ 'tb.rulenode.smtp-port-range' | translate }}\n        </mat-error>\n        <mat-error *ngIf=\"sendEmailConfigForm.get('smtpPort').hasError('max')\">\n          {{ 'tb.rulenode.smtp-port-range' | translate }}\n        </mat-error>\n      </mat-form-field>\n    </div>\n    <mat-form-field class=\"mat-block\">\n      <mat-label translate>tb.rulenode.timeout-msec</mat-label>\n      <input required type=\"number\" step=\"1\" min=\"0\" matInput formControlName=\"timeout\">\n      <mat-error *ngIf=\"sendEmailConfigForm.get('timeout').hasError('required')\">\n        {{ 'tb.rulenode.timeout-required' | translate }}\n      </mat-error>\n      <mat-error *ngIf=\"sendEmailConfigForm.get('timeout').hasError('min')\">\n        {{ 'tb.rulenode.min-timeout-msec-message' | translate }}\n      </mat-error>\n    </mat-form-field>\n    <mat-checkbox formControlName=\"enableTls\" style=\"padding-bottom: 16px;\">\n      {{ 'tb.rulenode.enable-tls' | translate }}\n    </mat-checkbox>\n    <mat-form-field class=\"mat-block\" *ngIf=\"sendEmailConfigForm.get('enableTls').value === true\">\n      <mat-label translate>tb.rulenode.tls-version</mat-label>\n      <mat-select formControlName=\"tlsVersion\">\n        <mat-option *ngFor=\"let tlsVersion of tlsVersions\" [value]=\"tlsVersion\">\n          {{ tlsVersion }}\n        </mat-option>\n      </mat-select>\n    </mat-form-field>\n    <tb-checkbox formControlName=\"enableProxy\" style=\"display: block; padding-bottom: 16px;\">\n      {{ 'tb.rulenode.enable-proxy' | translate }}\n    </tb-checkbox>\n    <div *ngIf=\"sendEmailConfigForm.get('enableProxy').value\">\n      <div fxLayout.gt-sm=\"row\" fxLayoutGap.gt-sm=\"8px\">\n        <mat-form-field class=\"mat-block\" fxFlex=\"100\" fxFlex.gt-sm=\"60\">\n          <mat-label translate>tb.rulenode.proxy-host</mat-label>\n          <input matInput required formControlName=\"proxyHost\">\n          <mat-error *ngIf=\"sendEmailConfigForm.get('proxyHost').hasError('required')\">\n            {{ 'tb.rulenode.proxy-host-required' | translate }}\n          </mat-error>\n        </mat-form-field>\n        <mat-form-field class=\"mat-block\" fxFlex=\"100\" fxFlex.gt-sm=\"40\">\n          <mat-label translate>tb.rulenode.proxy-port</mat-label>\n          <input matInput required formControlName=\"proxyPort\" type=\"number\" step=\"1\" min=\"1\" max=\"65535\">\n          <mat-error *ngIf=\"sendEmailConfigForm.get('proxyPort').hasError('required')\">\n            {{ 'tb.rulenode.proxy-port-required' | translate }}\n          </mat-error>\n          <mat-error *ngIf=\"sendEmailConfigForm.get('proxyPort').hasError('min') || sendEmailConfigForm.get('proxyPort').hasError('max')\">\n            {{ 'tb.rulenode.proxy-port-range' | translate }}\n          </mat-error>\n        </mat-form-field>\n      </div>\n      <mat-form-field class=\"mat-block\">\n        <mat-label translate>tb.rulenode.proxy-user</mat-label>\n        <input matInput formControlName=\"proxyUser\">\n      </mat-form-field>\n      <mat-form-field class=\"mat-block\">\n        <mat-label translate>tb.rulenode.proxy-password</mat-label>\n        <input matInput formControlName=\"proxyPassword\">\n      </mat-form-field>\n    </div>\n    <mat-form-field class=\"mat-block\" floatLabel=\"always\">\n      <mat-label translate>tb.rulenode.username</mat-label>\n      <input matInput placeholder=\"{{ 'tb.rulenode.enter-username' | translate }}\" formControlName=\"username\">\n    </mat-form-field>\n    <mat-form-field class=\"mat-block\" floatLabel=\"always\">\n      <mat-label translate>tb.rulenode.password</mat-label>\n      <input matInput type=\"password\" placeholder=\"{{ 'tb.rulenode.enter-password' | translate }}\" formControlName=\"password\">\n      <tb-toggle-password matSuffix></tb-toggle-password>\n    </mat-form-field>\n  </section>\n</section>\n", components: [{ type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i7$2.TbCheckboxComponent, selector: "tb-checkbox", inputs: ["disabled", "trueValue", "falseValue"], outputs: ["valueChange"] }, { type: i7.TogglePasswordComponent, selector: "tb-toggle-password" }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.MaxValidator, selector: "input[type=number][max][formControlName],input[type=number][max][formControl],input[type=number][max][ngModel]", inputs: ["max"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i3.MatSuffix, selector: "[matSuffix]" }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SendEmailConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-send-email-config',
                    templateUrl: './send-email-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class SendSmsConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.sendSmsConfigForm;
    }
    onConfigurationSet(configuration) {
        this.sendSmsConfigForm = this.fb.group({
            numbersToTemplate: [configuration ? configuration.numbersToTemplate : null, [Validators.required]],
            smsMessageTemplate: [configuration ? configuration.smsMessageTemplate : null, [Validators.required]],
            useSystemSmsSettings: [configuration ? configuration.useSystemSmsSettings : false, []],
            smsProviderConfiguration: [configuration ? configuration.smsProviderConfiguration : null, []],
        });
    }
    validatorTriggers() {
        return ['useSystemSmsSettings'];
    }
    updateValidators(emitEvent) {
        const useSystemSmsSettings = this.sendSmsConfigForm.get('useSystemSmsSettings').value;
        if (useSystemSmsSettings) {
            this.sendSmsConfigForm.get('smsProviderConfiguration').setValidators([]);
        }
        else {
            this.sendSmsConfigForm.get('smsProviderConfiguration').setValidators([Validators.required]);
        }
        this.sendSmsConfigForm.get('smsProviderConfiguration').updateValueAndValidity({ emitEvent });
    }
}
SendSmsConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SendSmsConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
SendSmsConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: SendSmsConfigComponent, selector: "tb-action-node-send-sms-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"sendSmsConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 24px;\">\n    <mat-label translate>tb.rulenode.numbers-to-template</mat-label>\n    <input required matInput formControlName=\"numbersToTemplate\">\n    <mat-error *ngIf=\"sendSmsConfigForm.get('numbersToTemplate').hasError('required')\">\n      {{ 'tb.rulenode.numbers-to-template-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.numbers-to-template-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 8px;\">\n    <mat-label translate>tb.rulenode.sms-message-template</mat-label>\n    <textarea required matInput formControlName=\"smsMessageTemplate\" rows=\"6\"></textarea>\n    <mat-error *ngIf=\"sendSmsConfigForm.get('smsMessageTemplate').hasError('required')\">\n      {{ 'tb.rulenode.sms-message-template-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-checkbox formControlName=\"useSystemSmsSettings\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.use-system-sms-settings' | translate }}\n  </mat-checkbox>\n  <tb-sms-provider-configuration\n    *ngIf=\"sendSmsConfigForm.get('useSystemSmsSettings').value === false\"\n    formControlName=\"smsProviderConfiguration\"\n    required>\n  </tb-sms-provider-configuration>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: i5$3.SmsProviderConfigurationComponent, selector: "tb-sms-provider-configuration", inputs: ["required", "disabled"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SendSmsConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-send-sms-config',
                    templateUrl: './send-sms-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class SnsConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.snsConfigForm;
    }
    onConfigurationSet(configuration) {
        this.snsConfigForm = this.fb.group({
            topicArnPattern: [configuration ? configuration.topicArnPattern : null, [Validators.required]],
            accessKeyId: [configuration ? configuration.accessKeyId : null, [Validators.required]],
            secretAccessKey: [configuration ? configuration.secretAccessKey : null, [Validators.required]],
            region: [configuration ? configuration.region : null, [Validators.required]]
        });
    }
}
SnsConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SnsConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
SnsConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: SnsConfigComponent, selector: "tb-action-node-sns-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"snsConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 8px;\">\n    <mat-label translate>tb.rulenode.topic-arn-pattern</mat-label>\n    <input required matInput formControlName=\"topicArnPattern\">\n    <mat-error *ngIf=\"snsConfigForm.get('topicArnPattern').hasError('required')\">\n      {{ 'tb.rulenode.topic-arn-pattern-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.aws-access-key-id</mat-label>\n    <input required matInput formControlName=\"accessKeyId\">\n    <mat-error *ngIf=\"snsConfigForm.get('accessKeyId').hasError('required')\">\n      {{ 'tb.rulenode.aws-access-key-id-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.aws-secret-access-key</mat-label>\n    <input required matInput formControlName=\"secretAccessKey\">\n    <mat-error *ngIf=\"snsConfigForm.get('secretAccessKey').hasError('required')\">\n      {{ 'tb.rulenode.aws-secret-access-key-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.aws-region</mat-label>\n    <input required matInput formControlName=\"region\">\n    <mat-error *ngIf=\"snsConfigForm.get('region').hasError('required')\">\n      {{ 'tb.rulenode.aws-region-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SnsConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-sns-config',
                    templateUrl: './sns-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class SqsConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.sqsQueueType = SqsQueueType;
        this.sqsQueueTypes = Object.keys(SqsQueueType);
        this.sqsQueueTypeTranslationsMap = sqsQueueTypeTranslations;
    }
    configForm() {
        return this.sqsConfigForm;
    }
    onConfigurationSet(configuration) {
        this.sqsConfigForm = this.fb.group({
            queueType: [configuration ? configuration.queueType : null, [Validators.required]],
            queueUrlPattern: [configuration ? configuration.queueUrlPattern : null, [Validators.required]],
            delaySeconds: [configuration ? configuration.delaySeconds : null, [Validators.min(0), Validators.max(900)]],
            messageAttributes: [configuration ? configuration.messageAttributes : null, []],
            accessKeyId: [configuration ? configuration.accessKeyId : null, [Validators.required]],
            secretAccessKey: [configuration ? configuration.secretAccessKey : null, [Validators.required]],
            region: [configuration ? configuration.region : null, [Validators.required]]
        });
    }
}
SqsConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SqsConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
SqsConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: SqsConfigComponent, selector: "tb-action-node-sqs-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"sqsConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.queue-type</mat-label>\n    <mat-select formControlName=\"queueType\" required>\n      <mat-option *ngFor=\"let type of sqsQueueTypes\" [value]=\"type\">\n        {{ sqsQueueTypeTranslationsMap.get(type) | translate }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 8px;\">\n    <mat-label translate>tb.rulenode.queue-url-pattern</mat-label>\n    <input required matInput formControlName=\"queueUrlPattern\">\n    <mat-error *ngIf=\"sqsConfigForm.get('queueUrlPattern').hasError('required')\">\n      {{ 'tb.rulenode.queue-url-pattern-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field *ngIf=\"sqsConfigForm.get('queueType').value === sqsQueueType.STANDARD\" class=\"mat-block\">\n    <mat-label translate>tb.rulenode.delay-seconds</mat-label>\n    <input required type=\"number\" min=\"0\" max=\"900\" step=\"1\" matInput formControlName=\"delaySeconds\">\n    <mat-error *ngIf=\"sqsConfigForm.get('delaySeconds').hasError('min')\">\n      {{ 'tb.rulenode.min-delay-seconds-message' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"sqsConfigForm.get('delaySeconds').hasError('max')\">\n      {{ 'tb.rulenode.max-delay-seconds-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <label translate class=\"tb-title\">tb.rulenode.message-attributes</label>\n  <div class=\"tb-hint\" [innerHTML]=\"'tb.rulenode.message-attributes-hint' | translate | safeHtml\"></div>\n  <tb-kv-map-config\n    required=\"false\"\n    formControlName=\"messageAttributes\"\n    keyText=\"tb.rulenode.name\"\n    keyRequiredText=\"tb.rulenode.name-required\"\n    valText=\"tb.rulenode.value\"\n    valRequiredText=\"tb.rulenode.value-required\">\n  </tb-kv-map-config>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.aws-access-key-id</mat-label>\n    <input required matInput formControlName=\"accessKeyId\">\n    <mat-error *ngIf=\"sqsConfigForm.get('accessKeyId').hasError('required')\">\n      {{ 'tb.rulenode.aws-access-key-id-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.aws-secret-access-key</mat-label>\n    <input required matInput formControlName=\"secretAccessKey\">\n    <mat-error *ngIf=\"sqsConfigForm.get('secretAccessKey').hasError('required')\">\n      {{ 'tb.rulenode.aws-secret-access-key-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.aws-region</mat-label>\n    <input required matInput formControlName=\"region\">\n    <mat-error *ngIf=\"sqsConfigForm.get('region').hasError('required')\">\n      {{ 'tb.rulenode.aws-region-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: KvMapConfigComponent, selector: "tb-kv-map-config", inputs: ["disabled", "requiredText", "keyText", "keyRequiredText", "valText", "valRequiredText", "required"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.MaxValidator, selector: "input[type=number][max][formControlName],input[type=number][max][formControl],input[type=number][max][ngModel]", inputs: ["max"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SqsConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-sqs-config',
                    templateUrl: './sqs-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class TimeseriesConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.timeseriesConfigForm;
    }
    onConfigurationSet(configuration) {
        this.timeseriesConfigForm = this.fb.group({
            defaultTTL: [configuration ? configuration.defaultTTL : null, [Validators.required, Validators.min(0)]]
        });
    }
}
TimeseriesConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TimeseriesConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
TimeseriesConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: TimeseriesConfigComponent, selector: "tb-action-node-timeseries-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"timeseriesConfigForm\" fxLayout=\"column\">\n  <mat-form-field fxFlex class=\"mat-block\">\n    <mat-label translate>tb.rulenode.default-ttl</mat-label>\n    <input type=\"number\" min=\"0\" step=\"1\" matInput formControlName=\"defaultTTL\" required>\n    <mat-error *ngIf=\"timeseriesConfigForm.get('defaultTTL').hasError('required')\">\n      {{ 'tb.rulenode.default-ttl-required' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"timeseriesConfigForm.get('defaultTTL').hasError('min')\">\n      {{ 'tb.rulenode.min-default-ttl-message' | translate }}\n    </mat-error>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TimeseriesConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-timeseries-config',
                    templateUrl: './timeseries-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class UnassignCustomerConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.unassignCustomerConfigForm;
    }
    onConfigurationSet(configuration) {
        this.unassignCustomerConfigForm = this.fb.group({
            customerNamePattern: [configuration ? configuration.customerNamePattern : null, [Validators.required, Validators.pattern(/.*\S.*/)]],
            customerCacheExpiration: [configuration ? configuration.customerCacheExpiration : null, [Validators.required, Validators.min(0)]]
        });
    }
    prepareOutputConfig(configuration) {
        configuration.customerNamePattern = configuration.customerNamePattern.trim();
        return configuration;
    }
}
UnassignCustomerConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: UnassignCustomerConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
UnassignCustomerConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: UnassignCustomerConfigComponent, selector: "tb-action-node-un-assign-to-customer-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"unassignCustomerConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.customer-name-pattern</mat-label>\n    <input required matInput formControlName=\"customerNamePattern\">\n    <mat-error *ngIf=\"unassignCustomerConfigForm.get('customerNamePattern').hasError('required') ||\n                      unassignCustomerConfigForm.get('customerNamePattern').hasError('pattern')\">\n      {{ 'tb.rulenode.customer-name-pattern-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.customer-cache-expiration</mat-label>\n    <input required type=\"number\" min=\"0\" step=\"1\" matInput formControlName=\"customerCacheExpiration\">\n    <mat-error *ngIf=\"unassignCustomerConfigForm.get('customerCacheExpiration').hasError('required')\">\n      {{ 'tb.rulenode.customer-cache-expiration-required' | translate }}\n    </mat-error>\n    <mat-error *ngIf=\"unassignCustomerConfigForm.get('customerCacheExpiration').hasError('min')\">\n      {{ 'tb.rulenode.customer-cache-expiration-range' | translate }}\n    </mat-error>\n    <mat-hint translate>tb.rulenode.customer-cache-expiration-hint</mat-hint>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: UnassignCustomerConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-action-node-un-assign-to-customer-config',
                    templateUrl: './unassign-customer-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class DeviceRelationsQueryConfigComponent extends PageComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.directionTypes = Object.keys(EntitySearchDirection);
        this.directionTypeTranslations = entitySearchDirectionTranslations;
        this.entityType = EntityType;
        this.propagateChange = null;
    }
    get required() {
        return this.requiredValue;
    }
    set required(value) {
        this.requiredValue = coerceBooleanProperty(value);
    }
    ngOnInit() {
        this.deviceRelationsQueryFormGroup = this.fb.group({
            fetchLastLevelOnly: [false, []],
            direction: [null, [Validators.required]],
            maxLevel: [null, []],
            relationType: [null],
            deviceTypes: [null, [Validators.required]]
        });
        this.deviceRelationsQueryFormGroup.valueChanges.subscribe((query) => {
            if (this.deviceRelationsQueryFormGroup.valid) {
                this.propagateChange(query);
            }
            else {
                this.propagateChange(null);
            }
        });
    }
    registerOnChange(fn) {
        this.propagateChange = fn;
    }
    registerOnTouched(fn) {
    }
    setDisabledState(isDisabled) {
        this.disabled = isDisabled;
        if (this.disabled) {
            this.deviceRelationsQueryFormGroup.disable({ emitEvent: false });
        }
        else {
            this.deviceRelationsQueryFormGroup.enable({ emitEvent: false });
        }
    }
    writeValue(query) {
        this.deviceRelationsQueryFormGroup.reset(query, { emitEvent: false });
    }
}
DeviceRelationsQueryConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: DeviceRelationsQueryConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
DeviceRelationsQueryConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: DeviceRelationsQueryConfigComponent, selector: "tb-device-relations-query-config", inputs: { disabled: "disabled", required: "required" }, providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DeviceRelationsQueryConfigComponent),
            multi: true
        }
    ], usesInheritance: true, ngImport: i0, template: "<section fxLayout=\"column\" [formGroup]=\"deviceRelationsQueryFormGroup\">\n  <mat-checkbox formControlName=\"fetchLastLevelOnly\" style=\"padding-bottom: 16px;\">\n    {{ 'alias.last-level-relation' | translate }}\n  </mat-checkbox>\n  <div fxLayoutGap=\"8px\" fxLayout=\"row\">\n    <mat-form-field class=\"mat-block\" style=\"min-width: 100px;\">\n      <mat-label translate>relation.direction</mat-label>\n      <mat-select required matInput formControlName=\"direction\">\n        <mat-option *ngFor=\"let type of directionTypes\" [value]=\"type\">\n          {{ directionTypeTranslations.get(type) | translate }}\n        </mat-option>\n      </mat-select>\n    </mat-form-field>\n    <mat-form-field fxFlex floatLabel=\"always\" class=\"mat-block\">\n      <mat-label translate>tb.rulenode.max-relation-level</mat-label>\n      <input matInput\n             type=\"number\"\n             min=\"1\"\n             step=\"1\"\n             placeholder=\"{{ 'tb.rulenode.unlimited-level' | translate }}\"\n             formControlName=\"maxLevel\">\n    </mat-form-field>\n  </div>\n  <div class=\"mat-caption\" style=\"color: rgba(0,0,0,0.57);\" translate>relation.relation-type</div>\n  <tb-relation-type-autocomplete\n    fxFlex\n    formControlName=\"relationType\">\n  </tb-relation-type-autocomplete>\n  <div class=\"mat-caption tb-required\" style=\"color: rgba(0,0,0,0.57);\" translate>device.device-types</div>\n  <tb-entity-subtype-list\n    required\n    [entityType]=\"entityType.DEVICE\"\n    formControlName=\"deviceTypes\">\n  </tb-entity-subtype-list>\n</section>\n", components: [{ type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i7$3.RelationTypeAutocompleteComponent, selector: "tb-relation-type-autocomplete", inputs: ["required", "disabled"] }, { type: i8$2.EntitySubTypeListComponent, selector: "tb-entity-subtype-list", inputs: ["required", "disabled", "entityType"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: DeviceRelationsQueryConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-device-relations-query-config',
                    templateUrl: './device-relations-query-config.component.html',
                    styleUrls: [],
                    providers: [
                        {
                            provide: NG_VALUE_ACCESSOR,
                            useExisting: forwardRef(() => DeviceRelationsQueryConfigComponent),
                            multi: true
                        }
                    ]
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; }, propDecorators: { disabled: [{
                type: Input
            }], required: [{
                type: Input
            }] } });

class RelationsQueryConfigComponent extends PageComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.directionTypes = Object.keys(EntitySearchDirection);
        this.directionTypeTranslations = entitySearchDirectionTranslations;
        this.propagateChange = null;
    }
    get required() {
        return this.requiredValue;
    }
    set required(value) {
        this.requiredValue = coerceBooleanProperty(value);
    }
    ngOnInit() {
        this.relationsQueryFormGroup = this.fb.group({
            fetchLastLevelOnly: [false, []],
            direction: [null, [Validators.required]],
            maxLevel: [null, []],
            filters: [null]
        });
        this.relationsQueryFormGroup.valueChanges.subscribe((query) => {
            if (this.relationsQueryFormGroup.valid) {
                this.propagateChange(query);
            }
            else {
                this.propagateChange(null);
            }
        });
    }
    registerOnChange(fn) {
        this.propagateChange = fn;
    }
    registerOnTouched(fn) {
    }
    setDisabledState(isDisabled) {
        this.disabled = isDisabled;
        if (this.disabled) {
            this.relationsQueryFormGroup.disable({ emitEvent: false });
        }
        else {
            this.relationsQueryFormGroup.enable({ emitEvent: false });
        }
    }
    writeValue(query) {
        this.relationsQueryFormGroup.reset(query || {}, { emitEvent: false });
    }
}
RelationsQueryConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RelationsQueryConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
RelationsQueryConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: RelationsQueryConfigComponent, selector: "tb-relations-query-config", inputs: { disabled: "disabled", required: "required" }, providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => RelationsQueryConfigComponent),
            multi: true
        }
    ], usesInheritance: true, ngImport: i0, template: "<section fxLayout=\"column\" [formGroup]=\"relationsQueryFormGroup\">\n  <mat-checkbox formControlName=\"fetchLastLevelOnly\" style=\"padding-bottom: 16px;\">\n    {{ 'alias.last-level-relation' | translate }}\n  </mat-checkbox>\n  <div fxLayoutGap=\"8px\" fxLayout=\"row\">\n    <mat-form-field class=\"mat-block\" style=\"min-width: 100px;\">\n      <mat-label translate>relation.direction</mat-label>\n      <mat-select required matInput formControlName=\"direction\">\n        <mat-option *ngFor=\"let type of directionTypes\" [value]=\"type\">\n          {{ directionTypeTranslations.get(type) | translate }}\n        </mat-option>\n      </mat-select>\n    </mat-form-field>\n    <mat-form-field fxFlex floatLabel=\"always\" class=\"mat-block\">\n      <mat-label translate>tb.rulenode.max-relation-level</mat-label>\n      <input matInput\n             type=\"number\"\n             min=\"1\"\n             step=\"1\"\n             placeholder=\"{{ 'tb.rulenode.unlimited-level' | translate }}\"\n             formControlName=\"maxLevel\">\n    </mat-form-field>\n  </div>\n  <div class=\"mat-caption\" style=\"color: rgba(0,0,0,0.57);\" translate>relation.relation-filters</div>\n  <tb-relation-filters\n    formControlName=\"filters\"\n  ></tb-relation-filters>\n</section>\n", components: [{ type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i7$4.RelationFiltersComponent, selector: "tb-relation-filters", inputs: ["disabled", "allowedEntityTypes"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RelationsQueryConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-relations-query-config',
                    templateUrl: './relations-query-config.component.html',
                    styleUrls: [],
                    providers: [
                        {
                            provide: NG_VALUE_ACCESSOR,
                            useExisting: forwardRef(() => RelationsQueryConfigComponent),
                            multi: true
                        }
                    ]
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; }, propDecorators: { disabled: [{
                type: Input
            }], required: [{
                type: Input
            }] } });

class MessageTypesConfigComponent extends PageComponent {
    constructor(store, translate, truncate, fb) {
        super(store);
        this.store = store;
        this.translate = translate;
        this.truncate = truncate;
        this.fb = fb;
        this.placeholder = 'tb.rulenode.message-type';
        this.separatorKeysCodes = [ENTER, COMMA, SEMICOLON];
        this.messageTypes = [];
        this.messageTypesList = [];
        this.searchText = '';
        this.propagateChange = (v) => { };
        this.messageTypeConfigForm = this.fb.group({
            messageType: [null]
        });
        for (const type of Object.keys(MessageType)) {
            this.messageTypesList.push({
                name: messageTypeNames.get(MessageType[type]),
                value: type
            });
        }
    }
    get required() {
        return this.requiredValue;
    }
    set required(value) {
        this.requiredValue = coerceBooleanProperty(value);
    }
    registerOnChange(fn) {
        this.propagateChange = fn;
    }
    registerOnTouched(fn) {
    }
    ngOnInit() {
        this.filteredMessageTypes = this.messageTypeConfigForm.get('messageType').valueChanges
            .pipe(startWith(''), map((value) => value ? value : ''), mergeMap(name => this.fetchMessageTypes(name)), share());
    }
    ngAfterViewInit() { }
    setDisabledState(isDisabled) {
        this.disabled = isDisabled;
        if (this.disabled) {
            this.messageTypeConfigForm.disable({ emitEvent: false });
        }
        else {
            this.messageTypeConfigForm.enable({ emitEvent: false });
        }
    }
    writeValue(value) {
        this.searchText = '';
        this.messageTypes.length = 0;
        if (value) {
            value.forEach((type) => {
                const found = this.messageTypesList.find((messageType => messageType.value === type));
                if (found) {
                    this.messageTypes.push({
                        name: found.name,
                        value: found.value
                    });
                }
                else {
                    this.messageTypes.push({
                        name: type,
                        value: type
                    });
                }
            });
        }
    }
    displayMessageTypeFn(messageType) {
        return messageType ? messageType.name : undefined;
    }
    textIsNotEmpty(text) {
        return (text && text != null && text.length > 0) ? true : false;
    }
    createMessageType($event, value) {
        $event.preventDefault();
        this.transformMessageType(value);
    }
    add(event) {
        this.transformMessageType(event.value);
    }
    fetchMessageTypes(searchText) {
        this.searchText = searchText;
        if (this.searchText && this.searchText.length) {
            const search = this.searchText.toUpperCase();
            return of(this.messageTypesList.filter(messageType => messageType.name.toUpperCase().includes(search)));
        }
        else {
            return of(this.messageTypesList);
        }
    }
    transformMessageType(value) {
        if ((value || '').trim()) {
            let newMessageType = null;
            const messageTypeName = value.trim();
            const existingMessageType = this.messageTypesList.find(messageType => messageType.name === messageTypeName);
            if (existingMessageType) {
                newMessageType = {
                    name: existingMessageType.name,
                    value: existingMessageType.value
                };
            }
            else {
                newMessageType = {
                    name: messageTypeName,
                    value: messageTypeName
                };
            }
            if (newMessageType) {
                this.addMessageType(newMessageType);
            }
        }
        this.clear('');
    }
    remove(messageType) {
        const index = this.messageTypes.indexOf(messageType);
        if (index >= 0) {
            this.messageTypes.splice(index, 1);
            this.updateModel();
        }
    }
    selected(event) {
        this.addMessageType(event.option.value);
        this.clear('');
    }
    addMessageType(messageType) {
        const index = this.messageTypes.findIndex(existingMessageType => existingMessageType.value === messageType.value);
        if (index === -1) {
            this.messageTypes.push(messageType);
            this.updateModel();
        }
    }
    onFocus() {
        this.messageTypeConfigForm.get('messageType').updateValueAndValidity({ onlySelf: true, emitEvent: true });
    }
    clear(value = '') {
        this.messageTypeInput.nativeElement.value = value;
        this.messageTypeConfigForm.get('messageType').patchValue(null, { emitEvent: true });
        setTimeout(() => {
            this.messageTypeInput.nativeElement.blur();
            this.messageTypeInput.nativeElement.focus();
        }, 0);
    }
    updateModel() {
        const value = this.messageTypes.map((messageType => messageType.value));
        if (this.required) {
            this.chipList.errorState = !value.length;
            this.propagateChange(value.length > 0 ? value : null);
        }
        else {
            this.chipList.errorState = false;
            this.propagateChange(value);
        }
    }
}
MessageTypesConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: MessageTypesConfigComponent, deps: [{ token: i1.Store }, { token: i4.TranslateService }, { token: i3$4.TruncatePipe }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
MessageTypesConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: MessageTypesConfigComponent, selector: "tb-message-types-config", inputs: { required: "required", label: "label", placeholder: "placeholder", disabled: "disabled" }, providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MessageTypesConfigComponent),
            multi: true
        }
    ], viewQueries: [{ propertyName: "chipList", first: true, predicate: ["chipList"], descendants: true }, { propertyName: "matAutocomplete", first: true, predicate: ["messageTypeAutocomplete"], descendants: true }, { propertyName: "messageTypeInput", first: true, predicate: ["messageTypeInput"], descendants: true }], usesInheritance: true, ngImport: i0, template: "<mat-form-field [formGroup]=\"messageTypeConfigForm\" style=\"width: 100%;\">\n  <mat-label *ngIf=\"label\" translate>{{ label }}</mat-label>\n  <mat-chip-list #chipList [required]=\"required\">\n    <mat-chip\n      *ngFor=\"let messageType of messageTypes\"\n      [selectable]=\"true\"\n      [removable]=\"true\"\n      (removed)=\"remove(messageType)\">\n      {{messageType.name}}\n      <mat-icon matChipRemove>close</mat-icon>\n    </mat-chip>\n    <input matInput type=\"text\" placeholder=\"{{ placeholder | translate }}\"\n           style=\"max-width: 200px;\"\n           #messageTypeInput\n           (focusin)=\"onFocus()\"\n           formControlName=\"messageType\"\n           matAutocompleteOrigin\n           #origin=\"matAutocompleteOrigin\"\n           [matAutocompleteConnectedTo]=\"origin\"\n           [matAutocomplete]=\"messageTypeAutocomplete\"\n           [matChipInputFor]=\"chipList\"\n           [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n           (matChipInputTokenEnd)=\"add($event)\">\n  </mat-chip-list>\n  <mat-autocomplete #messageTypeAutocomplete=\"matAutocomplete\"\n                    class=\"tb-autocomplete\"\n                    (optionSelected)=\"selected($event)\"\n                    [displayWith]=\"displayMessageTypeFn\">\n    <mat-option *ngFor=\"let messageType of filteredMessageTypes | async\" [value]=\"messageType\">\n      <span [innerHTML]=\"messageType.name | highlight:searchText\"></span>\n    </mat-option>\n    <mat-option *ngIf=\"(filteredMessageTypes | async)?.length === 0\" [value]=\"null\" class=\"tb-not-found\">\n      <div class=\"tb-not-found-content\" (click)=\"$event.stopPropagation()\">\n        <div *ngIf=\"!textIsNotEmpty(searchText); else searchNotEmpty\">\n          <span translate>tb.rulenode.no-message-types-found</span>\n        </div>\n        <ng-template #searchNotEmpty>\n                <span>\n                  {{ translate.get('tb.rulenode.no-message-type-matching',\n                  {messageType: truncate.transform(searchText, true, 6, &apos;...&apos;)}) | async }}\n                </span>\n        </ng-template>\n        <span>\n          <a translate (click)=\"createMessageType($event, searchText)\">tb.rulenode.create-new-message-type</a>\n        </span>\n      </div>\n    </mat-option>\n  </mat-autocomplete>\n  <mat-error *ngIf=\"chipList.errorState\">\n    {{ 'tb.rulenode.message-types-required' | translate }}\n  </mat-error>\n</mat-form-field>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i5$2.MatChipList, selector: "mat-chip-list", inputs: ["aria-orientation", "multiple", "compareWith", "value", "required", "placeholder", "disabled", "selectable", "tabIndex", "errorStateMatcher"], outputs: ["change", "valueChange"], exportAs: ["matChipList"] }, { type: i6$1.MatIcon, selector: "mat-icon", inputs: ["color", "inline", "svgIcon", "fontSet", "fontIcon"], exportAs: ["matIcon"] }, { type: i7$5.MatAutocomplete, selector: "mat-autocomplete", inputs: ["disableRipple"], exportAs: ["matAutocomplete"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }], directives: [{ type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i5$2.MatChip, selector: "mat-basic-chip, [mat-basic-chip], mat-chip, [mat-chip]", inputs: ["color", "disableRipple", "tabIndex", "selected", "value", "selectable", "disabled", "removable"], outputs: ["selectionChange", "destroyed", "removed"], exportAs: ["matChip"] }, { type: i5$2.MatChipRemove, selector: "[matChipRemove]" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i7$5.MatAutocompleteTrigger, selector: "input[matAutocomplete], textarea[matAutocomplete]", exportAs: ["matAutocompleteTrigger"] }, { type: i5$2.MatChipInput, selector: "input[matChipInputFor]", inputs: ["matChipInputSeparatorKeyCodes", "placeholder", "id", "matChipInputFor", "matChipInputAddOnBlur", "disabled"], outputs: ["matChipInputTokenEnd"], exportAs: ["matChipInput", "matChipInputFor"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i7$5.MatAutocompleteOrigin, selector: "[matAutocompleteOrigin]", exportAs: ["matAutocompleteOrigin"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }], pipes: { "translate": i4.TranslatePipe, "async": i10.AsyncPipe, "highlight": i12$1.HighlightPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: MessageTypesConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-message-types-config',
                    templateUrl: './message-types-config.component.html',
                    styleUrls: [],
                    providers: [
                        {
                            provide: NG_VALUE_ACCESSOR,
                            useExisting: forwardRef(() => MessageTypesConfigComponent),
                            multi: true
                        }
                    ]
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i4.TranslateService }, { type: i3$4.TruncatePipe }, { type: i2.FormBuilder }]; }, propDecorators: { required: [{
                type: Input
            }], label: [{
                type: Input
            }], placeholder: [{
                type: Input
            }], disabled: [{
                type: Input
            }], chipList: [{
                type: ViewChild,
                args: ['chipList', { static: false }]
            }], matAutocomplete: [{
                type: ViewChild,
                args: ['messageTypeAutocomplete', { static: false }]
            }], messageTypeInput: [{
                type: ViewChild,
                args: ['messageTypeInput', { static: false }]
            }] } });

class RulenodeCoreConfigCommonModule {
}
RulenodeCoreConfigCommonModule.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RulenodeCoreConfigCommonModule, deps: [], target: i0.ɵɵFactoryTarget.NgModule });
RulenodeCoreConfigCommonModule.ɵmod = i0.ɵɵngDeclareNgModule({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RulenodeCoreConfigCommonModule, declarations: [KvMapConfigComponent,
        DeviceRelationsQueryConfigComponent,
        RelationsQueryConfigComponent,
        MessageTypesConfigComponent,
        CredentialsConfigComponent,
        SafeHtmlPipe], imports: [CommonModule,
        SharedModule,
        HomeComponentsModule], exports: [KvMapConfigComponent,
        DeviceRelationsQueryConfigComponent,
        RelationsQueryConfigComponent,
        MessageTypesConfigComponent,
        CredentialsConfigComponent,
        SafeHtmlPipe] });
RulenodeCoreConfigCommonModule.ɵinj = i0.ɵɵngDeclareInjector({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RulenodeCoreConfigCommonModule, imports: [[
            CommonModule,
            SharedModule,
            HomeComponentsModule
        ]] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RulenodeCoreConfigCommonModule, decorators: [{
            type: NgModule,
            args: [{
                    declarations: [
                        KvMapConfigComponent,
                        DeviceRelationsQueryConfigComponent,
                        RelationsQueryConfigComponent,
                        MessageTypesConfigComponent,
                        CredentialsConfigComponent,
                        SafeHtmlPipe
                    ],
                    imports: [
                        CommonModule,
                        SharedModule,
                        HomeComponentsModule
                    ],
                    exports: [
                        KvMapConfigComponent,
                        DeviceRelationsQueryConfigComponent,
                        RelationsQueryConfigComponent,
                        MessageTypesConfigComponent,
                        CredentialsConfigComponent,
                        SafeHtmlPipe
                    ]
                }]
        }] });

class RuleNodeCoreConfigActionModule {
}
RuleNodeCoreConfigActionModule.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigActionModule, deps: [], target: i0.ɵɵFactoryTarget.NgModule });
RuleNodeCoreConfigActionModule.ɵmod = i0.ɵɵngDeclareNgModule({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigActionModule, declarations: [AttributesConfigComponent,
        TimeseriesConfigComponent,
        RpcRequestConfigComponent,
        LogConfigComponent,
        AssignCustomerConfigComponent,
        ClearAlarmConfigComponent,
        CreateAlarmConfigComponent,
        CreateRelationConfigComponent,
        MsgDelayConfigComponent,
        DeleteRelationConfigComponent,
        GeneratorConfigComponent,
        GpsGeoActionConfigComponent,
        MsgCountConfigComponent,
        RpcReplyConfigComponent,
        SaveToCustomTableConfigComponent,
        UnassignCustomerConfigComponent,
        SnsConfigComponent,
        SqsConfigComponent,
        PubSubConfigComponent,
        KafkaConfigComponent,
        MqttConfigComponent,
        RabbitMqConfigComponent,
        RestApiCallConfigComponent,
        SendEmailConfigComponent,
        CheckPointConfigComponent,
        AzureIotHubConfigComponent,
        DeviceProfileConfigComponent,
        SendSmsConfigComponent,
        PushToEdgeConfigComponent,
        PushToCloudConfigComponent], imports: [CommonModule,
        SharedModule,
        HomeComponentsModule,
        RulenodeCoreConfigCommonModule], exports: [AttributesConfigComponent,
        TimeseriesConfigComponent,
        RpcRequestConfigComponent,
        LogConfigComponent,
        AssignCustomerConfigComponent,
        ClearAlarmConfigComponent,
        CreateAlarmConfigComponent,
        CreateRelationConfigComponent,
        MsgDelayConfigComponent,
        DeleteRelationConfigComponent,
        GeneratorConfigComponent,
        GpsGeoActionConfigComponent,
        MsgCountConfigComponent,
        RpcReplyConfigComponent,
        SaveToCustomTableConfigComponent,
        UnassignCustomerConfigComponent,
        SnsConfigComponent,
        SqsConfigComponent,
        PubSubConfigComponent,
        KafkaConfigComponent,
        MqttConfigComponent,
        RabbitMqConfigComponent,
        RestApiCallConfigComponent,
        SendEmailConfigComponent,
        CheckPointConfigComponent,
        AzureIotHubConfigComponent,
        DeviceProfileConfigComponent,
        SendSmsConfigComponent,
        PushToEdgeConfigComponent,
        PushToCloudConfigComponent] });
RuleNodeCoreConfigActionModule.ɵinj = i0.ɵɵngDeclareInjector({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigActionModule, imports: [[
            CommonModule,
            SharedModule,
            HomeComponentsModule,
            RulenodeCoreConfigCommonModule
        ]] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigActionModule, decorators: [{
            type: NgModule,
            args: [{
                    declarations: [
                        AttributesConfigComponent,
                        TimeseriesConfigComponent,
                        RpcRequestConfigComponent,
                        LogConfigComponent,
                        AssignCustomerConfigComponent,
                        ClearAlarmConfigComponent,
                        CreateAlarmConfigComponent,
                        CreateRelationConfigComponent,
                        MsgDelayConfigComponent,
                        DeleteRelationConfigComponent,
                        GeneratorConfigComponent,
                        GpsGeoActionConfigComponent,
                        MsgCountConfigComponent,
                        RpcReplyConfigComponent,
                        SaveToCustomTableConfigComponent,
                        UnassignCustomerConfigComponent,
                        SnsConfigComponent,
                        SqsConfigComponent,
                        PubSubConfigComponent,
                        KafkaConfigComponent,
                        MqttConfigComponent,
                        RabbitMqConfigComponent,
                        RestApiCallConfigComponent,
                        SendEmailConfigComponent,
                        CheckPointConfigComponent,
                        AzureIotHubConfigComponent,
                        DeviceProfileConfigComponent,
                        SendSmsConfigComponent,
                        PushToEdgeConfigComponent,
                        PushToCloudConfigComponent
                    ],
                    imports: [
                        CommonModule,
                        SharedModule,
                        HomeComponentsModule,
                        RulenodeCoreConfigCommonModule
                    ],
                    exports: [
                        AttributesConfigComponent,
                        TimeseriesConfigComponent,
                        RpcRequestConfigComponent,
                        LogConfigComponent,
                        AssignCustomerConfigComponent,
                        ClearAlarmConfigComponent,
                        CreateAlarmConfigComponent,
                        CreateRelationConfigComponent,
                        MsgDelayConfigComponent,
                        DeleteRelationConfigComponent,
                        GeneratorConfigComponent,
                        GpsGeoActionConfigComponent,
                        MsgCountConfigComponent,
                        RpcReplyConfigComponent,
                        SaveToCustomTableConfigComponent,
                        UnassignCustomerConfigComponent,
                        SnsConfigComponent,
                        SqsConfigComponent,
                        PubSubConfigComponent,
                        KafkaConfigComponent,
                        MqttConfigComponent,
                        RabbitMqConfigComponent,
                        RestApiCallConfigComponent,
                        SendEmailConfigComponent,
                        CheckPointConfigComponent,
                        AzureIotHubConfigComponent,
                        DeviceProfileConfigComponent,
                        SendSmsConfigComponent,
                        PushToEdgeConfigComponent,
                        PushToCloudConfigComponent
                    ]
                }]
        }] });

class CalculateDeltaConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.separatorKeysCodes = [ENTER, COMMA, SEMICOLON];
    }
    configForm() {
        return this.calculateDeltaConfigForm;
    }
    onConfigurationSet(configuration) {
        this.calculateDeltaConfigForm = this.fb.group({
            inputValueKey: [configuration ? configuration.inputValueKey : null, [Validators.required]],
            outputValueKey: [configuration ? configuration.outputValueKey : null, [Validators.required]],
            useCache: [configuration ? configuration.useCache : null, []],
            addPeriodBetweenMsgs: [configuration ? configuration.addPeriodBetweenMsgs : false, []],
            periodValueKey: [configuration ? configuration.periodValueKey : null, []],
            round: [configuration ? configuration.round : null, [Validators.min(0), Validators.max(15)]],
            tellFailureIfDeltaIsNegative: [configuration ? configuration.tellFailureIfDeltaIsNegative : null, []]
        });
    }
    updateValidators(emitEvent) {
        const addPeriodBetweenMsgs = this.calculateDeltaConfigForm.get('addPeriodBetweenMsgs').value;
        if (addPeriodBetweenMsgs) {
            this.calculateDeltaConfigForm.get('periodValueKey').setValidators([Validators.required]);
        }
        else {
            this.calculateDeltaConfigForm.get('periodValueKey').setValidators([]);
        }
        this.calculateDeltaConfigForm.get('periodValueKey').updateValueAndValidity({ emitEvent });
    }
    validatorTriggers() {
        return ['addPeriodBetweenMsgs'];
    }
}
CalculateDeltaConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CalculateDeltaConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
CalculateDeltaConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: CalculateDeltaConfigComponent, selector: "tb-enrichment-node-calculate-delta-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"calculateDeltaConfigForm\" fxLayout=\"column\">\n  <div fxLayout.gt-sm=\"row\" fxLayoutGap.gt-sm=\"8px\">\n    <mat-form-field class=\"mat-block\" fxFlex=\"100\" fxFlex.gt-sm=\"38\">\n      <mat-label translate>tb.rulenode.input-value-key</mat-label>\n      <input required matInput formControlName=\"inputValueKey\">\n      <mat-error *ngIf=\"calculateDeltaConfigForm.get('inputValueKey').hasError('required')\">\n        {{ 'tb.rulenode.input-value-key-required' | translate }}\n      </mat-error>\n    </mat-form-field>\n    <mat-form-field class=\"mat-block\" fxFlex=\"100\" fxFlex.gt-sm=\"38\">\n      <mat-label translate>tb.rulenode.output-value-key</mat-label>\n      <input required matInput formControlName=\"outputValueKey\">\n      <mat-error *ngIf=\"calculateDeltaConfigForm.get('outputValueKey').hasError('required')\">\n        {{ 'tb.rulenode.output-value-key-required' | translate }}\n      </mat-error>\n    </mat-form-field>\n    <mat-form-field class=\"mat-block\" fxFlex=\"100\" fxFlex.gt-sm=\"24\">\n      <mat-label translate>tb.rulenode.round</mat-label>\n      <input type=\"number\" min=\"0\" max=\"15\" step=\"1\" matInput formControlName=\"round\">\n      <mat-error *ngIf=\"calculateDeltaConfigForm.get('round').hasError('min')\">\n        {{ 'tb.rulenode.round-range' | translate }}\n      </mat-error>\n      <mat-error *ngIf=\"calculateDeltaConfigForm.get('round').hasError('max')\">\n        {{ 'tb.rulenode.round-range' | translate }}\n      </mat-error>\n    </mat-form-field>\n  </div>\n  <mat-checkbox formControlName=\"useCache\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.use-cache' | translate }}\n  </mat-checkbox>\n  <mat-checkbox formControlName=\"tellFailureIfDeltaIsNegative\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.tell-failure-if-delta-is-negative' | translate }}\n  </mat-checkbox>\n  <mat-checkbox formControlName=\"addPeriodBetweenMsgs\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.add-period-between-msgs' | translate }}\n  </mat-checkbox>\n  <mat-form-field class=\"mat-block\" *ngIf=\"calculateDeltaConfigForm.get('addPeriodBetweenMsgs').value\">\n    <mat-label translate>tb.rulenode.period-value-key</mat-label>\n    <input required matInput formControlName=\"periodValueKey\">\n    <mat-error *ngIf=\"calculateDeltaConfigForm.get('periodValueKey').hasError('required')\">\n      {{ 'tb.rulenode.period-value-key-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.MaxValidator, selector: "input[type=number][max][formControlName],input[type=number][max][formControl],input[type=number][max][ngModel]", inputs: ["max"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CalculateDeltaConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-enrichment-node-calculate-delta-config',
                    templateUrl: './calculate-delta-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class CustomerAttributesConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.customerAttributesConfigForm;
    }
    onConfigurationSet(configuration) {
        this.customerAttributesConfigForm = this.fb.group({
            telemetry: [configuration ? configuration.telemetry : false, []],
            attrMapping: [configuration ? configuration.attrMapping : null, [Validators.required]]
        });
    }
}
CustomerAttributesConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CustomerAttributesConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
CustomerAttributesConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: CustomerAttributesConfigComponent, selector: "tb-enrichment-node-customer-attributes-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"customerAttributesConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title tb-required\">tb.rulenode.attr-mapping</label>\n  <mat-checkbox fxFlex formControlName=\"telemetry\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.latest-telemetry' | translate }}\n  </mat-checkbox>\n  <tb-kv-map-config\n    required\n    formControlName=\"attrMapping\"\n    requiredText=\"tb.rulenode.attr-mapping-required\"\n    keyText=\"{{ customerAttributesConfigForm.get('telemetry').value ? 'tb.rulenode.source-telemetry' : 'tb.rulenode.source-attribute' }}\"\n    keyRequiredText=\"{{ customerAttributesConfigForm.get('telemetry').value ? 'tb.rulenode.source-telemetry-required' : 'tb.rulenode.source-attribute-required' }}\"\n    valText=\"tb.rulenode.target-attribute\"\n    valRequiredText=\"tb.rulenode.target-attribute-required\">\n  </tb-kv-map-config>\n</section>\n", components: [{ type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: KvMapConfigComponent, selector: "tb-kv-map-config", inputs: ["disabled", "requiredText", "keyText", "keyRequiredText", "valText", "valRequiredText", "required"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CustomerAttributesConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-enrichment-node-customer-attributes-config',
                    templateUrl: './customer-attributes-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class DeviceAttributesConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.separatorKeysCodes = [ENTER, COMMA, SEMICOLON];
    }
    configForm() {
        return this.deviceAttributesConfigForm;
    }
    onConfigurationSet(configuration) {
        this.deviceAttributesConfigForm = this.fb.group({
            deviceRelationsQuery: [configuration ? configuration.deviceRelationsQuery : null, [Validators.required]],
            tellFailureIfAbsent: [configuration ? configuration.tellFailureIfAbsent : false, []],
            clientAttributeNames: [configuration ? configuration.clientAttributeNames : null, []],
            sharedAttributeNames: [configuration ? configuration.sharedAttributeNames : null, []],
            serverAttributeNames: [configuration ? configuration.serverAttributeNames : null, []],
            latestTsKeyNames: [configuration ? configuration.latestTsKeyNames : null, []],
            getLatestValueWithTs: [configuration ? configuration.getLatestValueWithTs : false, []]
        });
    }
    removeKey(key, keysField) {
        const keys = this.deviceAttributesConfigForm.get(keysField).value;
        const index = keys.indexOf(key);
        if (index >= 0) {
            keys.splice(index, 1);
            this.deviceAttributesConfigForm.get(keysField).setValue(keys, { emitEvent: true });
        }
    }
    addKey(event, keysField) {
        const input = event.input;
        let value = event.value;
        if ((value || '').trim()) {
            value = value.trim();
            let keys = this.deviceAttributesConfigForm.get(keysField).value;
            if (!keys || keys.indexOf(value) === -1) {
                if (!keys) {
                    keys = [];
                }
                keys.push(value);
                this.deviceAttributesConfigForm.get(keysField).setValue(keys, { emitEvent: true });
            }
        }
        if (input) {
            input.value = '';
        }
    }
}
DeviceAttributesConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: DeviceAttributesConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
DeviceAttributesConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: DeviceAttributesConfigComponent, selector: "tb-enrichment-node-device-attributes-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"deviceAttributesConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title tb-required\">tb.rulenode.device-relations-query</label>\n  <tb-device-relations-query-config\n    required\n    formControlName=\"deviceRelationsQuery\"\n    style=\"padding-bottom: 15px;\">\n  </tb-device-relations-query-config>\n  <mat-checkbox fxFlex formControlName=\"tellFailureIfAbsent\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.tell-failure-if-absent' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" translate>tb.rulenode.tell-failure-if-absent-hint</div>\n  <label translate class=\"tb-title no-padding\">tb.rulenode.client-attributes</label>\n  <mat-form-field floatLabel=\"always\" class=\"mat-block\">\n    <mat-label></mat-label>\n    <mat-chip-list #clientAttributesChipList>\n      <mat-chip\n        *ngFor=\"let key of deviceAttributesConfigForm.get('clientAttributeNames').value;\"\n        (removed)=\"removeKey(key, 'clientAttributeNames')\">\n        {{key}}\n        <mat-icon matChipRemove>close</mat-icon>\n      </mat-chip>\n      <input matInput type=\"text\" placeholder=\"{{'tb.rulenode.client-attributes' | translate}}\"\n             style=\"max-width: 200px;\"\n             [matChipInputFor]=\"clientAttributesChipList\"\n             [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n             (matChipInputTokenEnd)=\"addKey($event, 'clientAttributeNames')\"\n             [matChipInputAddOnBlur]=\"true\">\n    </mat-chip-list>\n  </mat-form-field>\n  <label translate class=\"tb-title no-padding\">tb.rulenode.shared-attributes</label>\n  <mat-form-field floatLabel=\"always\" class=\"mat-block\">\n    <mat-label></mat-label>\n    <mat-chip-list #sharedAttributesChipList>\n      <mat-chip\n        *ngFor=\"let key of deviceAttributesConfigForm.get('sharedAttributeNames').value;\"\n        (removed)=\"removeKey(key, 'sharedAttributeNames')\">\n        {{key}}\n        <mat-icon matChipRemove>close</mat-icon>\n      </mat-chip>\n      <input matInput type=\"text\" placeholder=\"{{'tb.rulenode.shared-attributes' | translate}}\"\n             style=\"max-width: 200px;\"\n             [matChipInputFor]=\"sharedAttributesChipList\"\n             [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n             (matChipInputTokenEnd)=\"addKey($event, 'sharedAttributeNames')\"\n             [matChipInputAddOnBlur]=\"true\">\n    </mat-chip-list>\n  </mat-form-field>\n  <label translate class=\"tb-title no-padding\">tb.rulenode.server-attributes</label>\n  <mat-form-field floatLabel=\"always\" class=\"mat-block\">\n    <mat-label></mat-label>\n    <mat-chip-list #serverAttributesChipList>\n      <mat-chip\n        *ngFor=\"let key of deviceAttributesConfigForm.get('serverAttributeNames').value;\"\n        (removed)=\"removeKey(key, 'serverAttributeNames')\">\n        {{key}}\n        <mat-icon matChipRemove>close</mat-icon>\n      </mat-chip>\n      <input matInput type=\"text\" placeholder=\"{{'tb.rulenode.server-attributes' | translate}}\"\n             style=\"max-width: 200px;\"\n             [matChipInputFor]=\"serverAttributesChipList\"\n             [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n             (matChipInputTokenEnd)=\"addKey($event, 'serverAttributeNames')\"\n             [matChipInputAddOnBlur]=\"true\">\n    </mat-chip-list>\n  </mat-form-field>\n  <label translate class=\"tb-title no-padding\">tb.rulenode.latest-timeseries</label>\n  <mat-form-field floatLabel=\"always\" class=\"mat-block\">\n    <mat-label></mat-label>\n    <mat-chip-list #latestTimeseriesChipList>\n      <mat-chip\n        *ngFor=\"let key of deviceAttributesConfigForm.get('latestTsKeyNames').value;\"\n        (removed)=\"removeKey(key, 'latestTsKeyNames')\">\n        {{key}}\n        <mat-icon matChipRemove>close</mat-icon>\n      </mat-chip>\n      <input matInput type=\"text\" placeholder=\"{{'tb.rulenode.latest-timeseries' | translate}}\"\n             style=\"max-width: 200px;\"\n             [matChipInputFor]=\"latestTimeseriesChipList\"\n             [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n             (matChipInputTokenEnd)=\"addKey($event, 'latestTsKeyNames')\"\n             [matChipInputAddOnBlur]=\"true\">\n    </mat-chip-list>\n  </mat-form-field>\n  <mat-checkbox formControlName=\"getLatestValueWithTs\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.get-latest-value-with-ts' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" [innerHTML]=\"'tb.rulenode.get-latest-value-with-ts-hint' | translate | safeHtml\"></div>\n</section>\n", styles: [":host label.tb-title{margin-bottom:-10px}\n"], components: [{ type: DeviceRelationsQueryConfigComponent, selector: "tb-device-relations-query-config", inputs: ["disabled", "required"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i5$2.MatChipList, selector: "mat-chip-list", inputs: ["aria-orientation", "multiple", "compareWith", "value", "required", "placeholder", "disabled", "selectable", "tabIndex", "errorStateMatcher"], outputs: ["change", "valueChange"], exportAs: ["matChipList"] }, { type: i6$1.MatIcon, selector: "mat-icon", inputs: ["color", "inline", "svgIcon", "fontSet", "fontIcon"], exportAs: ["matIcon"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i5$2.MatChip, selector: "mat-basic-chip, [mat-basic-chip], mat-chip, [mat-chip]", inputs: ["color", "disableRipple", "tabIndex", "selected", "value", "selectable", "disabled", "removable"], outputs: ["selectionChange", "destroyed", "removed"], exportAs: ["matChip"] }, { type: i5$2.MatChipRemove, selector: "[matChipRemove]" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i5$2.MatChipInput, selector: "input[matChipInputFor]", inputs: ["matChipInputSeparatorKeyCodes", "placeholder", "id", "matChipInputFor", "matChipInputAddOnBlur", "disabled"], outputs: ["matChipInputTokenEnd"], exportAs: ["matChipInput", "matChipInputFor"] }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: DeviceAttributesConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-enrichment-node-device-attributes-config',
                    templateUrl: './device-attributes-config.component.html',
                    styleUrls: ['./device-attributes-config.component.scss']
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class EntityDetailsConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, translate, fb) {
        super(store);
        this.store = store;
        this.translate = translate;
        this.fb = fb;
        this.entityDetailsTranslationsMap = entityDetailsTranslations;
        this.entityDetailsList = [];
        this.searchText = '';
        this.displayDetailsFn = this.displayDetails.bind(this);
        for (const field of Object.keys(EntityDetailsField)) {
            this.entityDetailsList.push(EntityDetailsField[field]);
        }
        this.detailsFormControl = new FormControl('');
        this.filteredEntityDetails = this.detailsFormControl.valueChanges
            .pipe(startWith(''), map((value) => value ? value : ''), mergeMap(name => this.fetchEntityDetails(name)), share());
    }
    ngOnInit() {
        super.ngOnInit();
    }
    configForm() {
        return this.entityDetailsConfigForm;
    }
    prepareInputConfig(configuration) {
        this.searchText = '';
        this.detailsFormControl.patchValue('', { emitEvent: true });
        return configuration;
    }
    onConfigurationSet(configuration) {
        this.entityDetailsConfigForm = this.fb.group({
            detailsList: [configuration ? configuration.detailsList : null, [Validators.required]],
            addToMetadata: [configuration ? configuration.addToMetadata : false, []]
        });
    }
    displayDetails(details) {
        return details ? this.translate.instant(entityDetailsTranslations.get(details)) : undefined;
    }
    fetchEntityDetails(searchText) {
        this.searchText = searchText;
        if (this.searchText && this.searchText.length) {
            const search = this.searchText.toUpperCase();
            return of(this.entityDetailsList.filter(field => this.translate.instant(entityDetailsTranslations.get(EntityDetailsField[field])).toUpperCase().includes(search)));
        }
        else {
            return of(this.entityDetailsList);
        }
    }
    detailsFieldSelected(event) {
        this.addDetailsField(event.option.value);
        this.clear('');
    }
    removeDetailsField(details) {
        const detailsList = this.entityDetailsConfigForm.get('detailsList').value;
        if (detailsList) {
            const index = detailsList.indexOf(details);
            if (index >= 0) {
                detailsList.splice(index, 1);
                this.entityDetailsConfigForm.get('detailsList').setValue(detailsList);
            }
        }
    }
    addDetailsField(details) {
        let detailsList = this.entityDetailsConfigForm.get('detailsList').value;
        if (!detailsList) {
            detailsList = [];
        }
        const index = detailsList.indexOf(details);
        if (index === -1) {
            detailsList.push(details);
            this.entityDetailsConfigForm.get('detailsList').setValue(detailsList);
        }
    }
    onEntityDetailsInputFocus() {
        this.detailsFormControl.updateValueAndValidity({ onlySelf: true, emitEvent: true });
    }
    clear(value = '') {
        this.detailsInput.nativeElement.value = value;
        this.detailsFormControl.patchValue(null, { emitEvent: true });
        setTimeout(() => {
            this.detailsInput.nativeElement.blur();
            this.detailsInput.nativeElement.focus();
        }, 0);
    }
}
EntityDetailsConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: EntityDetailsConfigComponent, deps: [{ token: i1.Store }, { token: i4.TranslateService }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
EntityDetailsConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: EntityDetailsConfigComponent, selector: "tb-enrichment-node-entity-details-config", viewQueries: [{ propertyName: "detailsInput", first: true, predicate: ["detailsInput"], descendants: true }], usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"entityDetailsConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" class=\"entity-fields-list\">\n    <mat-label translate>tb.rulenode.entity-details</mat-label>\n    <mat-chip-list #detailsChipList required>\n      <mat-chip\n        *ngFor=\"let details of entityDetailsConfigForm.get('detailsList').value;\"\n        (removed)=\"removeDetailsField(details)\">\n        <span>\n          <strong>{{entityDetailsTranslationsMap.get(details) | translate}}</strong>\n        </span>\n        <mat-icon matChipRemove>close</mat-icon>\n      </mat-chip>\n      <input matInput type=\"text\"\n             style=\"max-width: 200px;\"\n             #detailsInput\n             (focusin)=\"onEntityDetailsInputFocus()\"\n             [formControl]=\"detailsFormControl\"\n             matAutocompleteOrigin\n             #origin=\"matAutocompleteOrigin\"\n             [matAutocompleteConnectedTo]=\"origin\"\n             [matAutocomplete]=\"detailsAutocomplete\"\n             [matChipInputFor]=\"detailsChipList\">\n    </mat-chip-list>\n    <mat-autocomplete #detailsAutocomplete=\"matAutocomplete\"\n                      class=\"tb-autocomplete\"\n                      (optionSelected)=\"detailsFieldSelected($event)\"\n                      [displayWith]=\"displayDetailsFn\">\n      <mat-option *ngFor=\"let details of filteredEntityDetails | async\" [value]=\"details\">\n        <span [innerHTML]=\"entityDetailsTranslationsMap.get(details) | translate | highlight:searchText\"></span>\n      </mat-option>\n      <mat-option *ngIf=\"(filteredEntityDetails | async)?.length === 0\" [value]=\"null\" class=\"tb-not-found\">\n        <div class=\"tb-not-found-content\" (click)=\"$event.stopPropagation()\">\n          <div>\n            <span translate>tb.rulenode.no-entity-details-matching</span>\n          </div>\n        </div>\n      </mat-option>\n    </mat-autocomplete>\n  </mat-form-field>\n  <tb-error [error]=\"(detailsFormControl.touched &&\n                     entityDetailsConfigForm.get('detailsList').hasError('required'))\n                  ? translate.instant('tb.rulenode.entity-details-list-empty') : ''\"></tb-error>\n  <mat-checkbox fxFlex formControlName=\"addToMetadata\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.add-to-metadata' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" translate>tb.rulenode.add-to-metadata-hint</div>\n</section>\n", styles: [":host ::ng-deep mat-form-field.entity-fields-list .mat-form-field-wrapper{margin-bottom:-1.25em}\n"], components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i5$2.MatChipList, selector: "mat-chip-list", inputs: ["aria-orientation", "multiple", "compareWith", "value", "required", "placeholder", "disabled", "selectable", "tabIndex", "errorStateMatcher"], outputs: ["change", "valueChange"], exportAs: ["matChipList"] }, { type: i6$1.MatIcon, selector: "mat-icon", inputs: ["color", "inline", "svgIcon", "fontSet", "fontIcon"], exportAs: ["matIcon"] }, { type: i7$5.MatAutocomplete, selector: "mat-autocomplete", inputs: ["disableRipple"], exportAs: ["matAutocomplete"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i9.TbErrorComponent, selector: "tb-error", inputs: ["error"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i5$2.MatChip, selector: "mat-basic-chip, [mat-basic-chip], mat-chip, [mat-chip]", inputs: ["color", "disableRipple", "tabIndex", "selected", "value", "selectable", "disabled", "removable"], outputs: ["selectionChange", "destroyed", "removed"], exportAs: ["matChip"] }, { type: i5$2.MatChipRemove, selector: "[matChipRemove]" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i7$5.MatAutocompleteTrigger, selector: "input[matAutocomplete], textarea[matAutocomplete]", exportAs: ["matAutocompleteTrigger"] }, { type: i5$2.MatChipInput, selector: "input[matChipInputFor]", inputs: ["matChipInputSeparatorKeyCodes", "placeholder", "id", "matChipInputFor", "matChipInputAddOnBlur", "disabled"], outputs: ["matChipInputTokenEnd"], exportAs: ["matChipInput", "matChipInputFor"] }, { type: i7$5.MatAutocompleteOrigin, selector: "[matAutocompleteOrigin]", exportAs: ["matAutocompleteOrigin"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlDirective, selector: "[formControl]", inputs: ["disabled", "formControl", "ngModel"], outputs: ["ngModelChange"], exportAs: ["ngForm"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }], pipes: { "translate": i4.TranslatePipe, "async": i10.AsyncPipe, "highlight": i12$1.HighlightPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: EntityDetailsConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-enrichment-node-entity-details-config',
                    templateUrl: './entity-details-config.component.html',
                    styleUrls: ['./entity-details-config.component.scss']
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i4.TranslateService }, { type: i2.FormBuilder }]; }, propDecorators: { detailsInput: [{
                type: ViewChild,
                args: ['detailsInput', { static: false }]
            }] } });

class GetTelemetryFromDatabaseConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.separatorKeysCodes = [ENTER, COMMA, SEMICOLON];
        this.aggregationTypes = AggregationType;
        this.aggregations = Object.keys(AggregationType);
        this.aggregationTypesTranslations = aggregationTranslations;
        this.fetchMode = FetchMode;
        this.fetchModes = Object.keys(FetchMode);
        this.samplingOrders = Object.keys(SamplingOrder);
        this.timeUnits = Object.values(TimeUnit);
        this.timeUnitsTranslationMap = timeUnitTranslations;
    }
    configForm() {
        return this.getTelemetryFromDatabaseConfigForm;
    }
    onConfigurationSet(configuration) {
        this.getTelemetryFromDatabaseConfigForm = this.fb.group({
            latestTsKeyNames: [configuration ? configuration.latestTsKeyNames : null, []],
            aggregation: [configuration ? configuration.aggregation : null, [Validators.required]],
            fetchMode: [configuration ? configuration.fetchMode : null, [Validators.required]],
            orderBy: [configuration ? configuration.orderBy : null, []],
            limit: [configuration ? configuration.limit : null, []],
            useMetadataIntervalPatterns: [configuration ? configuration.useMetadataIntervalPatterns : false, []],
            startInterval: [configuration ? configuration.startInterval : null, []],
            startIntervalTimeUnit: [configuration ? configuration.startIntervalTimeUnit : null, []],
            endInterval: [configuration ? configuration.endInterval : null, []],
            endIntervalTimeUnit: [configuration ? configuration.endIntervalTimeUnit : null, []],
            startIntervalPattern: [configuration ? configuration.startIntervalPattern : null, []],
            endIntervalPattern: [configuration ? configuration.endIntervalPattern : null, []],
        });
    }
    validatorTriggers() {
        return ['fetchMode', 'useMetadataIntervalPatterns'];
    }
    updateValidators(emitEvent) {
        const fetchMode = this.getTelemetryFromDatabaseConfigForm.get('fetchMode').value;
        const useMetadataIntervalPatterns = this.getTelemetryFromDatabaseConfigForm.get('useMetadataIntervalPatterns').value;
        if (fetchMode && fetchMode === FetchMode.ALL) {
            this.getTelemetryFromDatabaseConfigForm.get('aggregation').setValidators([Validators.required]);
            this.getTelemetryFromDatabaseConfigForm.get('orderBy').setValidators([Validators.required]);
            this.getTelemetryFromDatabaseConfigForm.get('limit').setValidators([Validators.required, Validators.min(2), Validators.max(1000)]);
        }
        else {
            this.getTelemetryFromDatabaseConfigForm.get('aggregation').setValidators([]);
            this.getTelemetryFromDatabaseConfigForm.get('orderBy').setValidators([]);
            this.getTelemetryFromDatabaseConfigForm.get('limit').setValidators([]);
        }
        if (useMetadataIntervalPatterns) {
            this.getTelemetryFromDatabaseConfigForm.get('startInterval').setValidators([]);
            this.getTelemetryFromDatabaseConfigForm.get('startIntervalTimeUnit').setValidators([]);
            this.getTelemetryFromDatabaseConfigForm.get('endInterval').setValidators([]);
            this.getTelemetryFromDatabaseConfigForm.get('endIntervalTimeUnit').setValidators([]);
            this.getTelemetryFromDatabaseConfigForm.get('startIntervalPattern').setValidators([Validators.required]);
            this.getTelemetryFromDatabaseConfigForm.get('endIntervalPattern').setValidators([Validators.required]);
        }
        else {
            this.getTelemetryFromDatabaseConfigForm.get('startInterval').setValidators([Validators.required,
                Validators.min(1), Validators.max(2147483647)]);
            this.getTelemetryFromDatabaseConfigForm.get('startIntervalTimeUnit').setValidators([Validators.required]);
            this.getTelemetryFromDatabaseConfigForm.get('endInterval').setValidators([Validators.required,
                Validators.min(1), Validators.max(2147483647)]);
            this.getTelemetryFromDatabaseConfigForm.get('endIntervalTimeUnit').setValidators([Validators.required]);
            this.getTelemetryFromDatabaseConfigForm.get('startIntervalPattern').setValidators([]);
            this.getTelemetryFromDatabaseConfigForm.get('endIntervalPattern').setValidators([]);
        }
        this.getTelemetryFromDatabaseConfigForm.get('aggregation').updateValueAndValidity({ emitEvent });
        this.getTelemetryFromDatabaseConfigForm.get('orderBy').updateValueAndValidity({ emitEvent });
        this.getTelemetryFromDatabaseConfigForm.get('limit').updateValueAndValidity({ emitEvent });
        this.getTelemetryFromDatabaseConfigForm.get('startInterval').updateValueAndValidity({ emitEvent });
        this.getTelemetryFromDatabaseConfigForm.get('startIntervalTimeUnit').updateValueAndValidity({ emitEvent });
        this.getTelemetryFromDatabaseConfigForm.get('endInterval').updateValueAndValidity({ emitEvent });
        this.getTelemetryFromDatabaseConfigForm.get('endIntervalTimeUnit').updateValueAndValidity({ emitEvent });
        this.getTelemetryFromDatabaseConfigForm.get('startIntervalPattern').updateValueAndValidity({ emitEvent });
        this.getTelemetryFromDatabaseConfigForm.get('endIntervalPattern').updateValueAndValidity({ emitEvent });
    }
    removeKey(key, keysField) {
        const keys = this.getTelemetryFromDatabaseConfigForm.get(keysField).value;
        const index = keys.indexOf(key);
        if (index >= 0) {
            keys.splice(index, 1);
            this.getTelemetryFromDatabaseConfigForm.get(keysField).setValue(keys, { emitEvent: true });
        }
    }
    addKey(event, keysField) {
        const input = event.input;
        let value = event.value;
        if ((value || '').trim()) {
            value = value.trim();
            let keys = this.getTelemetryFromDatabaseConfigForm.get(keysField).value;
            if (!keys || keys.indexOf(value) === -1) {
                if (!keys) {
                    keys = [];
                }
                keys.push(value);
                this.getTelemetryFromDatabaseConfigForm.get(keysField).setValue(keys, { emitEvent: true });
            }
        }
        if (input) {
            input.value = '';
        }
    }
}
GetTelemetryFromDatabaseConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: GetTelemetryFromDatabaseConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
GetTelemetryFromDatabaseConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: GetTelemetryFromDatabaseConfigComponent, selector: "tb-enrichment-node-get-telemetry-from-database", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"getTelemetryFromDatabaseConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title no-padding\">tb.rulenode.timeseries-key</label>\n  <mat-form-field floatLabel=\"always\" class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label></mat-label>\n    <mat-chip-list #latestTimeseriesChipList>\n      <mat-chip\n        *ngFor=\"let key of getTelemetryFromDatabaseConfigForm.get('latestTsKeyNames').value;\"\n        (removed)=\"removeKey(key, 'latestTsKeyNames')\">\n        {{key}}\n        <mat-icon matChipRemove>close</mat-icon>\n      </mat-chip>\n      <input matInput type=\"text\" placeholder=\"{{'tb.rulenode.timeseries-key' | translate}}\"\n             style=\"max-width: 200px;\"\n             [matChipInputFor]=\"latestTimeseriesChipList\"\n             [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n             (matChipInputTokenEnd)=\"addKey($event, 'latestTsKeyNames')\"\n             [matChipInputAddOnBlur]=\"true\">\n    </mat-chip-list>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.fetch-mode</mat-label>\n    <mat-select formControlName=\"fetchMode\" required>\n      <mat-option *ngFor=\"let mode of fetchModes\" [value]=\"mode\">\n        {{ mode }}\n      </mat-option>\n    </mat-select>\n    <mat-hint translate>tb.rulenode.fetch-mode-hint</mat-hint>\n  </mat-form-field>\n  <div fxLayout=\"column\" *ngIf=\"getTelemetryFromDatabaseConfigForm.get('fetchMode').value === fetchMode.ALL\">\n    <mat-form-field>\n      <mat-label translate>aggregation.function</mat-label>\n      <mat-select formControlName=\"aggregation\" required>\n        <mat-option *ngFor=\"let aggregation of aggregations\" [value]=\"aggregation\">\n          {{ aggregationTypesTranslations.get(aggregationTypes[aggregation]) | translate }}\n        </mat-option>\n      </mat-select>\n    </mat-form-field>\n    <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n      <mat-label translate>tb.rulenode.order-by</mat-label>\n      <mat-select formControlName=\"orderBy\" required>\n        <mat-option *ngFor=\"let order of samplingOrders\" [value]=\"order\">\n          {{ order }}\n        </mat-option>\n      </mat-select>\n      <mat-hint translate>tb.rulenode.order-by-hint</mat-hint>\n    </mat-form-field>\n    <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n      <mat-label translate>tb.rulenode.limit</mat-label>\n      <input type=\"number\" min=\"2\" max=\"1000\" step=\"1\" matInput formControlName=\"limit\" required>\n      <mat-hint translate>tb.rulenode.limit-hint</mat-hint>\n    </mat-form-field>\n  </div>\n  <mat-checkbox formControlName=\"useMetadataIntervalPatterns\" style=\"padding-bottom: 8px;\">\n    {{ 'tb.rulenode.use-metadata-interval-patterns' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" style=\"padding-bottom: 16px;\" translate>tb.rulenode.use-metadata-interval-patterns-hint</div>\n  <div fxLayout=\"column\" *ngIf=\"getTelemetryFromDatabaseConfigForm.get('useMetadataIntervalPatterns').value === false; else intervalPattern\">\n    <div fxLayout=\"column\" fxLayout.gt-sm=\"row\" fxLayoutGap.gt-sm=\"8px\">\n      <mat-form-field fxFlex class=\"mat-block\">\n        <mat-label translate>tb.rulenode.start-interval</mat-label>\n        <input type=\"number\" step=\"1\" min=\"1\" max=\"2147483647\" matInput formControlName=\"startInterval\" required>\n        <mat-error *ngIf=\"getTelemetryFromDatabaseConfigForm.get('startInterval').hasError('required')\">\n          {{ 'tb.rulenode.start-interval-value-required' | translate }}\n        </mat-error>\n        <mat-error *ngIf=\"getTelemetryFromDatabaseConfigForm.get('startInterval').hasError('min')\">\n          {{ 'tb.rulenode.time-value-range' | translate }}\n        </mat-error>\n        <mat-error *ngIf=\"getTelemetryFromDatabaseConfigForm.get('startInterval').hasError('max')\">\n          {{ 'tb.rulenode.time-value-range' | translate }}\n        </mat-error>\n      </mat-form-field>\n      <mat-form-field fxFlex class=\"mat-block\">\n        <mat-label translate>tb.rulenode.start-interval-time-unit</mat-label>\n        <mat-select formControlName=\"startIntervalTimeUnit\" required>\n          <mat-option *ngFor=\"let timeUnit of timeUnits\" [value]=\"timeUnit\">\n            {{ timeUnitsTranslationMap.get(timeUnit) | translate }}\n          </mat-option>\n        </mat-select>\n      </mat-form-field>\n    </div>\n    <div fxLayout=\"column\" fxLayout.gt-sm=\"row\" fxLayoutGap.gt-sm=\"8px\">\n      <mat-form-field fxFlex class=\"mat-block\">\n        <mat-label translate>tb.rulenode.end-interval</mat-label>\n        <input type=\"number\" step=\"1\" min=\"1\" max=\"2147483647\" matInput formControlName=\"endInterval\" required>\n        <mat-error *ngIf=\"getTelemetryFromDatabaseConfigForm.get('endInterval').hasError('required')\">\n          {{ 'tb.rulenode.end-interval-value-required' | translate }}\n        </mat-error>\n        <mat-error *ngIf=\"getTelemetryFromDatabaseConfigForm.get('endInterval').hasError('min')\">\n          {{ 'tb.rulenode.time-value-range' | translate }}\n        </mat-error>\n        <mat-error *ngIf=\"getTelemetryFromDatabaseConfigForm.get('endInterval').hasError('max')\">\n          {{ 'tb.rulenode.time-value-range' | translate }}\n        </mat-error>\n      </mat-form-field>\n      <mat-form-field fxFlex class=\"mat-block\">\n        <mat-label translate>tb.rulenode.end-interval-time-unit</mat-label>\n        <mat-select formControlName=\"endIntervalTimeUnit\" required>\n          <mat-option *ngFor=\"let timeUnit of timeUnits\" [value]=\"timeUnit\">\n            {{ timeUnitsTranslationMap.get(timeUnit) | translate }}\n          </mat-option>\n        </mat-select>\n      </mat-form-field>\n    </div>\n  </div>\n  <ng-template #intervalPattern>\n    <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n      <mat-label translate>tb.rulenode.start-interval-pattern</mat-label>\n      <input matInput formControlName=\"startIntervalPattern\" required>\n      <mat-error *ngIf=\"getTelemetryFromDatabaseConfigForm.get('startIntervalPattern').hasError('required')\">\n        {{ 'tb.rulenode.start-interval-pattern-required' | translate }}\n      </mat-error>\n      <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n    </mat-form-field>\n    <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n      <mat-label translate>tb.rulenode.end-interval-pattern</mat-label>\n      <input matInput formControlName=\"endIntervalPattern\" required>\n      <mat-error *ngIf=\"getTelemetryFromDatabaseConfigForm.get('endIntervalPattern').hasError('required')\">\n        {{ 'tb.rulenode.end-interval-pattern-required' | translate }}\n      </mat-error>\n      <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n    </mat-form-field>\n  </ng-template>\n</section>\n", styles: [":host label.tb-title{margin-bottom:-10px}\n"], components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i5$2.MatChipList, selector: "mat-chip-list", inputs: ["aria-orientation", "multiple", "compareWith", "value", "required", "placeholder", "disabled", "selectable", "tabIndex", "errorStateMatcher"], outputs: ["change", "valueChange"], exportAs: ["matChipList"] }, { type: i6$1.MatIcon, selector: "mat-icon", inputs: ["color", "inline", "svgIcon", "fontSet", "fontIcon"], exportAs: ["matIcon"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i5$2.MatChip, selector: "mat-basic-chip, [mat-basic-chip], mat-chip, [mat-chip]", inputs: ["color", "disableRipple", "tabIndex", "selected", "value", "selectable", "disabled", "removable"], outputs: ["selectionChange", "destroyed", "removed"], exportAs: ["matChip"] }, { type: i5$2.MatChipRemove, selector: "[matChipRemove]" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i5$2.MatChipInput, selector: "input[matChipInputFor]", inputs: ["matChipInputSeparatorKeyCodes", "placeholder", "id", "matChipInputFor", "matChipInputAddOnBlur", "disabled"], outputs: ["matChipInputTokenEnd"], exportAs: ["matChipInput", "matChipInputFor"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.MaxValidator, selector: "input[type=number][max][formControlName],input[type=number][max][formControl],input[type=number][max][ngModel]", inputs: ["max"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: GetTelemetryFromDatabaseConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-enrichment-node-get-telemetry-from-database',
                    templateUrl: './get-telemetry-from-database-config.component.html',
                    styleUrls: ['./get-telemetry-from-database-config.component.scss']
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class OriginatorAttributesConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.separatorKeysCodes = [ENTER, COMMA, SEMICOLON];
    }
    configForm() {
        return this.originatorAttributesConfigForm;
    }
    onConfigurationSet(configuration) {
        this.originatorAttributesConfigForm = this.fb.group({
            tellFailureIfAbsent: [configuration ? configuration.tellFailureIfAbsent : false, []],
            clientAttributeNames: [configuration ? configuration.clientAttributeNames : null, []],
            sharedAttributeNames: [configuration ? configuration.sharedAttributeNames : null, []],
            serverAttributeNames: [configuration ? configuration.serverAttributeNames : null, []],
            latestTsKeyNames: [configuration ? configuration.latestTsKeyNames : null, []],
            getLatestValueWithTs: [configuration ? configuration.getLatestValueWithTs : false, []]
        });
    }
    removeKey(key, keysField) {
        const keys = this.originatorAttributesConfigForm.get(keysField).value;
        const index = keys.indexOf(key);
        if (index >= 0) {
            keys.splice(index, 1);
            this.originatorAttributesConfigForm.get(keysField).setValue(keys, { emitEvent: true });
        }
    }
    addKey(event, keysField) {
        const input = event.input;
        let value = event.value;
        if ((value || '').trim()) {
            value = value.trim();
            let keys = this.originatorAttributesConfigForm.get(keysField).value;
            if (!keys || keys.indexOf(value) === -1) {
                if (!keys) {
                    keys = [];
                }
                keys.push(value);
                this.originatorAttributesConfigForm.get(keysField).setValue(keys, { emitEvent: true });
            }
        }
        if (input) {
            input.value = '';
        }
    }
}
OriginatorAttributesConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: OriginatorAttributesConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
OriginatorAttributesConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: OriginatorAttributesConfigComponent, selector: "tb-enrichment-node-originator-attributes-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"originatorAttributesConfigForm\" fxLayout=\"column\">\n  <mat-checkbox fxFlex formControlName=\"tellFailureIfAbsent\" style=\"padding-bottom: 8px;\">\n    {{ 'tb.rulenode.tell-failure-if-absent' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" translate>tb.rulenode.tell-failure-if-absent-hint</div>\n  <label translate class=\"tb-title no-padding\">tb.rulenode.client-attributes</label>\n  <mat-form-field floatLabel=\"always\" class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label></mat-label>\n    <mat-chip-list #clientAttributesChipList>\n      <mat-chip\n        *ngFor=\"let key of originatorAttributesConfigForm.get('clientAttributeNames').value;\"\n        (removed)=\"removeKey(key, 'clientAttributeNames')\">\n        {{key}}\n        <mat-icon matChipRemove>close</mat-icon>\n      </mat-chip>\n      <input matInput type=\"text\" placeholder=\"{{'tb.rulenode.client-attributes' | translate}}\"\n             style=\"max-width: 200px;\"\n             [matChipInputFor]=\"clientAttributesChipList\"\n             [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n             (matChipInputTokenEnd)=\"addKey($event, 'clientAttributeNames')\"\n             [matChipInputAddOnBlur]=\"true\">\n    </mat-chip-list>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <label translate class=\"tb-title no-padding\">tb.rulenode.shared-attributes</label>\n  <mat-form-field floatLabel=\"always\" class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label></mat-label>\n    <mat-chip-list #sharedAttributesChipList>\n      <mat-chip\n        *ngFor=\"let key of originatorAttributesConfigForm.get('sharedAttributeNames').value;\"\n        (removed)=\"removeKey(key, 'sharedAttributeNames')\">\n        {{key}}\n        <mat-icon matChipRemove>close</mat-icon>\n      </mat-chip>\n      <input matInput type=\"text\" placeholder=\"{{'tb.rulenode.shared-attributes' | translate}}\"\n             style=\"max-width: 200px;\"\n             [matChipInputFor]=\"sharedAttributesChipList\"\n             [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n             (matChipInputTokenEnd)=\"addKey($event, 'sharedAttributeNames')\"\n             [matChipInputAddOnBlur]=\"true\">\n    </mat-chip-list>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <label translate class=\"tb-title no-padding\">tb.rulenode.server-attributes</label>\n  <mat-form-field floatLabel=\"always\" class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label></mat-label>\n    <mat-chip-list #serverAttributesChipList>\n      <mat-chip\n        *ngFor=\"let key of originatorAttributesConfigForm.get('serverAttributeNames').value;\"\n        (removed)=\"removeKey(key, 'serverAttributeNames')\">\n        {{key}}\n        <mat-icon matChipRemove>close</mat-icon>\n      </mat-chip>\n      <input matInput type=\"text\" placeholder=\"{{'tb.rulenode.server-attributes' | translate}}\"\n             style=\"max-width: 200px;\"\n             [matChipInputFor]=\"serverAttributesChipList\"\n             [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n             (matChipInputTokenEnd)=\"addKey($event, 'serverAttributeNames')\"\n             [matChipInputAddOnBlur]=\"true\">\n    </mat-chip-list>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <label translate class=\"tb-title no-padding\">tb.rulenode.latest-timeseries</label>\n  <mat-form-field floatLabel=\"always\" class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label></mat-label>\n    <mat-chip-list #latestTimeseriesChipList>\n      <mat-chip\n        *ngFor=\"let key of originatorAttributesConfigForm.get('latestTsKeyNames').value;\"\n        (removed)=\"removeKey(key, 'latestTsKeyNames')\">\n        {{key}}\n        <mat-icon matChipRemove>close</mat-icon>\n      </mat-chip>\n      <input matInput type=\"text\" placeholder=\"{{'tb.rulenode.latest-timeseries' | translate}}\"\n             style=\"max-width: 200px;\"\n             [matChipInputFor]=\"latestTimeseriesChipList\"\n             [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n             (matChipInputTokenEnd)=\"addKey($event, 'latestTsKeyNames')\"\n             [matChipInputAddOnBlur]=\"true\">\n    </mat-chip-list>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-checkbox formControlName=\"getLatestValueWithTs\" style=\"padding-bottom: 8px;\">\n    {{ 'tb.rulenode.get-latest-value-with-ts' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" [innerHTML]=\"'tb.rulenode.get-latest-value-with-ts-hint' | translate | safeHtml\"></div>\n</section>\n", styles: [":host label.tb-title{margin-bottom:-10px}\n"], components: [{ type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i5$2.MatChipList, selector: "mat-chip-list", inputs: ["aria-orientation", "multiple", "compareWith", "value", "required", "placeholder", "disabled", "selectable", "tabIndex", "errorStateMatcher"], outputs: ["change", "valueChange"], exportAs: ["matChipList"] }, { type: i6$1.MatIcon, selector: "mat-icon", inputs: ["color", "inline", "svgIcon", "fontSet", "fontIcon"], exportAs: ["matIcon"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i5$2.MatChip, selector: "mat-basic-chip, [mat-basic-chip], mat-chip, [mat-chip]", inputs: ["color", "disableRipple", "tabIndex", "selected", "value", "selectable", "disabled", "removable"], outputs: ["selectionChange", "destroyed", "removed"], exportAs: ["matChip"] }, { type: i5$2.MatChipRemove, selector: "[matChipRemove]" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i5$2.MatChipInput, selector: "input[matChipInputFor]", inputs: ["matChipInputSeparatorKeyCodes", "placeholder", "id", "matChipInputFor", "matChipInputAddOnBlur", "disabled"], outputs: ["matChipInputTokenEnd"], exportAs: ["matChipInput", "matChipInputFor"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: OriginatorAttributesConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-enrichment-node-originator-attributes-config',
                    templateUrl: './originator-attributes-config.component.html',
                    styleUrls: ['./originator-attributes-config.component.scss']
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class OriginatorFieldsConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.originatorFieldsConfigForm;
    }
    onConfigurationSet(configuration) {
        this.originatorFieldsConfigForm = this.fb.group({
            fieldsMapping: [configuration ? configuration.fieldsMapping : null, [Validators.required]]
        });
    }
}
OriginatorFieldsConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: OriginatorFieldsConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
OriginatorFieldsConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: OriginatorFieldsConfigComponent, selector: "tb-enrichment-node-originator-fields-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"originatorFieldsConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title tb-required\">tb.rulenode.fields-mapping</label>\n  <tb-kv-map-config\n    required\n    formControlName=\"fieldsMapping\"\n    requiredText=\"tb.rulenode.fields-mapping-required\"\n    keyText=\"tb.rulenode.source-field\"\n    keyRequiredText=\"tb.rulenode.source-field-required\"\n    valText=\"tb.rulenode.target-attribute\"\n    valRequiredText=\"tb.rulenode.target-attribute-required\">\n  </tb-kv-map-config>\n</section>\n", components: [{ type: KvMapConfigComponent, selector: "tb-kv-map-config", inputs: ["disabled", "requiredText", "keyText", "keyRequiredText", "valText", "valRequiredText", "required"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: OriginatorFieldsConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-enrichment-node-originator-fields-config',
                    templateUrl: './originator-fields-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class RelatedAttributesConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.relatedAttributesConfigForm;
    }
    onConfigurationSet(configuration) {
        this.relatedAttributesConfigForm = this.fb.group({
            relationsQuery: [configuration ? configuration.relationsQuery : null, [Validators.required]],
            telemetry: [configuration ? configuration.telemetry : false, []],
            attrMapping: [configuration ? configuration.attrMapping : null, [Validators.required]]
        });
    }
}
RelatedAttributesConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RelatedAttributesConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
RelatedAttributesConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: RelatedAttributesConfigComponent, selector: "tb-enrichment-node-related-attributes-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"relatedAttributesConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title tb-required\">tb.rulenode.relations-query</label>\n  <tb-relations-query-config\n    required\n    formControlName=\"relationsQuery\"\n    style=\"padding-bottom: 15px;\">\n  </tb-relations-query-config>\n  <label translate class=\"tb-title tb-required\">tb.rulenode.attr-mapping</label>\n  <mat-checkbox fxFlex formControlName=\"telemetry\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.latest-telemetry' | translate }}\n  </mat-checkbox>\n  <tb-kv-map-config\n    required\n    formControlName=\"attrMapping\"\n    requiredText=\"tb.rulenode.attr-mapping-required\"\n    keyText=\"{{ relatedAttributesConfigForm.get('telemetry').value ? 'tb.rulenode.source-telemetry' : 'tb.rulenode.source-attribute' }}\"\n    keyRequiredText=\"{{ relatedAttributesConfigForm.get('telemetry').value ? 'tb.rulenode.source-telemetry-required' : 'tb.rulenode.source-attribute-required' }}\"\n    valText=\"tb.rulenode.target-attribute\"\n    valRequiredText=\"tb.rulenode.target-attribute-required\">\n  </tb-kv-map-config>\n</section>\n", components: [{ type: RelationsQueryConfigComponent, selector: "tb-relations-query-config", inputs: ["disabled", "required"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: KvMapConfigComponent, selector: "tb-kv-map-config", inputs: ["disabled", "requiredText", "keyText", "keyRequiredText", "valText", "valRequiredText", "required"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RelatedAttributesConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-enrichment-node-related-attributes-config',
                    templateUrl: './related-attributes-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class TenantAttributesConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.tenantAttributesConfigForm;
    }
    onConfigurationSet(configuration) {
        this.tenantAttributesConfigForm = this.fb.group({
            telemetry: [configuration ? configuration.telemetry : false, []],
            attrMapping: [configuration ? configuration.attrMapping : null, [Validators.required]]
        });
    }
}
TenantAttributesConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TenantAttributesConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
TenantAttributesConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: TenantAttributesConfigComponent, selector: "tb-enrichment-node-tenant-attributes-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"tenantAttributesConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title tb-required\">tb.rulenode.attr-mapping</label>\n  <mat-checkbox fxFlex formControlName=\"telemetry\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.latest-telemetry' | translate }}\n  </mat-checkbox>\n  <tb-kv-map-config\n    required\n    formControlName=\"attrMapping\"\n    requiredText=\"tb.rulenode.attr-mapping-required\"\n    keyText=\"{{ tenantAttributesConfigForm.get('telemetry').value ? 'tb.rulenode.source-telemetry' : 'tb.rulenode.source-attribute' }}\"\n    keyRequiredText=\"{{ tenantAttributesConfigForm.get('telemetry').value ? 'tb.rulenode.source-telemetry-required' : 'tb.rulenode.source-attribute-required' }}\"\n    valText=\"tb.rulenode.target-attribute\"\n    valRequiredText=\"tb.rulenode.target-attribute-required\">\n  </tb-kv-map-config>\n</section>\n", components: [{ type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: KvMapConfigComponent, selector: "tb-kv-map-config", inputs: ["disabled", "requiredText", "keyText", "keyRequiredText", "valText", "valRequiredText", "required"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TenantAttributesConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-enrichment-node-tenant-attributes-config',
                    templateUrl: './tenant-attributes-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class RulenodeCoreConfigEnrichmentModule {
}
RulenodeCoreConfigEnrichmentModule.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RulenodeCoreConfigEnrichmentModule, deps: [], target: i0.ɵɵFactoryTarget.NgModule });
RulenodeCoreConfigEnrichmentModule.ɵmod = i0.ɵɵngDeclareNgModule({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RulenodeCoreConfigEnrichmentModule, declarations: [CustomerAttributesConfigComponent,
        EntityDetailsConfigComponent,
        DeviceAttributesConfigComponent,
        OriginatorAttributesConfigComponent,
        OriginatorFieldsConfigComponent,
        GetTelemetryFromDatabaseConfigComponent,
        RelatedAttributesConfigComponent,
        TenantAttributesConfigComponent,
        CalculateDeltaConfigComponent], imports: [CommonModule,
        SharedModule,
        RulenodeCoreConfigCommonModule], exports: [CustomerAttributesConfigComponent,
        EntityDetailsConfigComponent,
        DeviceAttributesConfigComponent,
        OriginatorAttributesConfigComponent,
        OriginatorFieldsConfigComponent,
        GetTelemetryFromDatabaseConfigComponent,
        RelatedAttributesConfigComponent,
        TenantAttributesConfigComponent,
        CalculateDeltaConfigComponent] });
RulenodeCoreConfigEnrichmentModule.ɵinj = i0.ɵɵngDeclareInjector({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RulenodeCoreConfigEnrichmentModule, imports: [[
            CommonModule,
            SharedModule,
            RulenodeCoreConfigCommonModule
        ]] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RulenodeCoreConfigEnrichmentModule, decorators: [{
            type: NgModule,
            args: [{
                    declarations: [
                        CustomerAttributesConfigComponent,
                        EntityDetailsConfigComponent,
                        DeviceAttributesConfigComponent,
                        OriginatorAttributesConfigComponent,
                        OriginatorFieldsConfigComponent,
                        GetTelemetryFromDatabaseConfigComponent,
                        RelatedAttributesConfigComponent,
                        TenantAttributesConfigComponent,
                        CalculateDeltaConfigComponent
                    ],
                    imports: [
                        CommonModule,
                        SharedModule,
                        RulenodeCoreConfigCommonModule
                    ],
                    exports: [
                        CustomerAttributesConfigComponent,
                        EntityDetailsConfigComponent,
                        DeviceAttributesConfigComponent,
                        OriginatorAttributesConfigComponent,
                        OriginatorFieldsConfigComponent,
                        GetTelemetryFromDatabaseConfigComponent,
                        RelatedAttributesConfigComponent,
                        TenantAttributesConfigComponent,
                        CalculateDeltaConfigComponent
                    ]
                }]
        }] });

class CheckAlarmStatusComponent extends RuleNodeConfigurationComponent {
    constructor(store, translate, fb) {
        super(store);
        this.store = store;
        this.translate = translate;
        this.fb = fb;
        this.alarmStatusTranslationsMap = alarmStatusTranslations;
        this.alarmStatusList = [];
        this.searchText = '';
        this.displayStatusFn = this.displayStatus.bind(this);
        for (const field of Object.keys(AlarmStatus)) {
            this.alarmStatusList.push(AlarmStatus[field]);
        }
        this.statusFormControl = new FormControl('');
        this.filteredAlarmStatus = this.statusFormControl.valueChanges
            .pipe(startWith(''), map((value) => value ? value : ''), mergeMap(name => this.fetchAlarmStatus(name)), share());
    }
    ngOnInit() {
        super.ngOnInit();
    }
    configForm() {
        return this.alarmStatusConfigForm;
    }
    prepareInputConfig(configuration) {
        this.searchText = '';
        this.statusFormControl.patchValue('', { emitEvent: true });
        return configuration;
    }
    onConfigurationSet(configuration) {
        this.alarmStatusConfigForm = this.fb.group({
            alarmStatusList: [configuration ? configuration.alarmStatusList : null, [Validators.required]],
        });
    }
    displayStatus(status) {
        return status ? this.translate.instant(alarmStatusTranslations.get(status)) : undefined;
    }
    fetchAlarmStatus(searchText) {
        const alarmStatusList = this.getAlarmStatusList();
        this.searchText = searchText;
        if (this.searchText && this.searchText.length) {
            const search = this.searchText.toUpperCase();
            return of(alarmStatusList.filter(field => this.translate.instant(alarmStatusTranslations.get(AlarmStatus[field])).toUpperCase().includes(search)));
        }
        else {
            return of(alarmStatusList);
        }
    }
    alarmStatusSelected(event) {
        this.addAlarmStatus(event.option.value);
        this.clear('');
    }
    removeAlarmStatus(status) {
        const alarmStatusList = this.alarmStatusConfigForm.get('alarmStatusList').value;
        if (alarmStatusList) {
            const index = alarmStatusList.indexOf(status);
            if (index >= 0) {
                alarmStatusList.splice(index, 1);
                this.alarmStatusConfigForm.get('alarmStatusList').setValue(alarmStatusList);
            }
        }
    }
    addAlarmStatus(status) {
        let alarmStatusList = this.alarmStatusConfigForm.get('alarmStatusList').value;
        if (!alarmStatusList) {
            alarmStatusList = [];
        }
        const index = alarmStatusList.indexOf(status);
        if (index === -1) {
            alarmStatusList.push(status);
            this.alarmStatusConfigForm.get('alarmStatusList').setValue(alarmStatusList);
        }
    }
    getAlarmStatusList() {
        return this.alarmStatusList.filter((listItem) => {
            return this.alarmStatusConfigForm.get('alarmStatusList').value.indexOf(listItem) === -1;
        });
    }
    onAlarmStatusInputFocus() {
        this.statusFormControl.updateValueAndValidity({ onlySelf: true, emitEvent: true });
    }
    clear(value = '') {
        this.alarmStatusInput.nativeElement.value = value;
        this.statusFormControl.patchValue(null, { emitEvent: true });
        setTimeout(() => {
            this.alarmStatusInput.nativeElement.blur();
            this.alarmStatusInput.nativeElement.focus();
        }, 0);
    }
}
CheckAlarmStatusComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CheckAlarmStatusComponent, deps: [{ token: i1.Store }, { token: i4.TranslateService }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
CheckAlarmStatusComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: CheckAlarmStatusComponent, selector: "tb-filter-node-check-alarm-status-config", viewQueries: [{ propertyName: "alarmStatusInput", first: true, predicate: ["alarmStatusInput"], descendants: true }], usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"alarmStatusConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" class=\"alarm-status-list\">\n  <mat-label translate>tb.rulenode.alarm-status-filter</mat-label>\n  <mat-chip-list #alarmStatusChipList required>\n    <mat-chip\n      *ngFor=\"let alarmStatus of alarmStatusConfigForm.get('alarmStatusList').value;\"\n      (removed)=\"removeAlarmStatus(alarmStatus)\">\n        <span>\n          <strong>{{alarmStatusTranslationsMap.get(alarmStatus) | translate}}</strong>\n        </span>\n      <mat-icon matChipRemove>close</mat-icon>\n    </mat-chip>\n    <input matInput type=\"text\"\n           style=\"max-width: 200px;\"\n           #alarmStatusInput\n           (focusin)=\"onAlarmStatusInputFocus()\"\n           [formControl]=\"statusFormControl\"\n           matAutocompleteOrigin\n           #origin=\"matAutocompleteOrigin\"\n           [matAutocompleteConnectedTo]=\"origin\"\n           [matAutocomplete]=\"alarmStatusAutocomplete\"\n           [matChipInputFor]=\"alarmStatusChipList\">\n  </mat-chip-list>\n  <mat-autocomplete #alarmStatusAutocomplete=\"matAutocomplete\"\n                    class=\"tb-autocomplete\"\n                    (optionSelected)=\"alarmStatusSelected($event)\"\n                    [displayWith]=\"displayStatusFn\">\n    <mat-option *ngFor=\"let status of filteredAlarmStatus | async\" [value]=\"status\">\n      <span [innerHTML]=\"alarmStatusTranslationsMap.get(status) | translate | highlight:searchText\"></span>\n    </mat-option>\n    <mat-option *ngIf=\"(filteredAlarmStatus | async)?.length === 0\" [value]=\"null\" class=\"tb-not-found\">\n      <div class=\"tb-not-found-content\" (click)=\"$event.stopPropagation()\">\n        <div>\n          <span translate>tb.rulenode.no-alarm-status-matching</span>\n        </div>\n      </div>\n    </mat-option>\n  </mat-autocomplete>\n  </mat-form-field>\n  <tb-error [error]=\"(statusFormControl.touched &&\n                     alarmStatusConfigForm.get('alarmStatusList').hasError('required'))\n                  ? translate.instant('tb.rulenode.alarm-status-list-empty') : ''\"></tb-error>\n  </section>\n\n\n\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i5$2.MatChipList, selector: "mat-chip-list", inputs: ["aria-orientation", "multiple", "compareWith", "value", "required", "placeholder", "disabled", "selectable", "tabIndex", "errorStateMatcher"], outputs: ["change", "valueChange"], exportAs: ["matChipList"] }, { type: i6$1.MatIcon, selector: "mat-icon", inputs: ["color", "inline", "svgIcon", "fontSet", "fontIcon"], exportAs: ["matIcon"] }, { type: i7$5.MatAutocomplete, selector: "mat-autocomplete", inputs: ["disableRipple"], exportAs: ["matAutocomplete"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i9.TbErrorComponent, selector: "tb-error", inputs: ["error"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i5$2.MatChip, selector: "mat-basic-chip, [mat-basic-chip], mat-chip, [mat-chip]", inputs: ["color", "disableRipple", "tabIndex", "selected", "value", "selectable", "disabled", "removable"], outputs: ["selectionChange", "destroyed", "removed"], exportAs: ["matChip"] }, { type: i5$2.MatChipRemove, selector: "[matChipRemove]" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i7$5.MatAutocompleteTrigger, selector: "input[matAutocomplete], textarea[matAutocomplete]", exportAs: ["matAutocompleteTrigger"] }, { type: i5$2.MatChipInput, selector: "input[matChipInputFor]", inputs: ["matChipInputSeparatorKeyCodes", "placeholder", "id", "matChipInputFor", "matChipInputAddOnBlur", "disabled"], outputs: ["matChipInputTokenEnd"], exportAs: ["matChipInput", "matChipInputFor"] }, { type: i7$5.MatAutocompleteOrigin, selector: "[matAutocompleteOrigin]", exportAs: ["matAutocompleteOrigin"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlDirective, selector: "[formControl]", inputs: ["disabled", "formControl", "ngModel"], outputs: ["ngModelChange"], exportAs: ["ngForm"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }], pipes: { "translate": i4.TranslatePipe, "async": i10.AsyncPipe, "highlight": i12$1.HighlightPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CheckAlarmStatusComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-filter-node-check-alarm-status-config',
                    templateUrl: './check-alarm-status.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i4.TranslateService }, { type: i2.FormBuilder }]; }, propDecorators: { alarmStatusInput: [{
                type: ViewChild,
                args: ['alarmStatusInput', { static: false }]
            }] } });

class CheckMessageConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.separatorKeysCodes = [ENTER, COMMA, SEMICOLON];
    }
    configForm() {
        return this.checkMessageConfigForm;
    }
    onConfigurationSet(configuration) {
        this.checkMessageConfigForm = this.fb.group({
            messageNames: [configuration ? configuration.messageNames : null, []],
            metadataNames: [configuration ? configuration.metadataNames : null, []],
            checkAllKeys: [configuration ? configuration.checkAllKeys : false, []],
        });
    }
    validateConfig() {
        const messageNames = this.checkMessageConfigForm.get('messageNames').value;
        const metadataNames = this.checkMessageConfigForm.get('metadataNames').value;
        return messageNames.length > 0 || metadataNames.length > 0;
    }
    removeMessageName(messageName) {
        const messageNames = this.checkMessageConfigForm.get('messageNames').value;
        const index = messageNames.indexOf(messageName);
        if (index >= 0) {
            messageNames.splice(index, 1);
            this.checkMessageConfigForm.get('messageNames').setValue(messageNames, { emitEvent: true });
        }
    }
    removeMetadataName(metadataName) {
        const metadataNames = this.checkMessageConfigForm.get('metadataNames').value;
        const index = metadataNames.indexOf(metadataName);
        if (index >= 0) {
            metadataNames.splice(index, 1);
            this.checkMessageConfigForm.get('metadataNames').setValue(metadataNames, { emitEvent: true });
        }
    }
    addMessageName(event) {
        const input = event.input;
        let value = event.value;
        if ((value || '').trim()) {
            value = value.trim();
            let messageNames = this.checkMessageConfigForm.get('messageNames').value;
            if (!messageNames || messageNames.indexOf(value) === -1) {
                if (!messageNames) {
                    messageNames = [];
                }
                messageNames.push(value);
                this.checkMessageConfigForm.get('messageNames').setValue(messageNames, { emitEvent: true });
            }
        }
        if (input) {
            input.value = '';
        }
    }
    addMetadataName(event) {
        const input = event.input;
        let value = event.value;
        if ((value || '').trim()) {
            value = value.trim();
            let metadataNames = this.checkMessageConfigForm.get('metadataNames').value;
            if (!metadataNames || metadataNames.indexOf(value) === -1) {
                if (!metadataNames) {
                    metadataNames = [];
                }
                metadataNames.push(value);
                this.checkMessageConfigForm.get('metadataNames').setValue(metadataNames, { emitEvent: true });
            }
        }
        if (input) {
            input.value = '';
        }
    }
}
CheckMessageConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CheckMessageConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
CheckMessageConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: CheckMessageConfigComponent, selector: "tb-filter-node-check-message-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"checkMessageConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title no-padding tb-required\">tb.rulenode.data-keys</label>\n  <mat-form-field floatLabel=\"always\" class=\"mat-block\">\n    <mat-label></mat-label>\n    <mat-chip-list #messageNamesChipList>\n      <mat-chip\n        *ngFor=\"let messageName of checkMessageConfigForm.get('messageNames').value;\"\n        (removed)=\"removeMessageName(messageName)\">\n        {{messageName}}\n        <mat-icon matChipRemove>close</mat-icon>\n      </mat-chip>\n      <input matInput type=\"text\" placeholder=\"{{'tb.rulenode.data-keys' | translate}}\"\n             style=\"max-width: 200px;\"\n             [matChipInputFor]=\"messageNamesChipList\"\n             [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n             (matChipInputTokenEnd)=\"addMessageName($event)\"\n             [matChipInputAddOnBlur]=\"true\">\n    </mat-chip-list>\n  </mat-form-field>\n  <div class=\"tb-hint\" translate>tb.rulenode.separator-hint</div>\n  <label translate class=\"tb-title no-padding tb-required\">tb.rulenode.metadata-keys</label>\n  <mat-form-field floatLabel=\"always\" class=\"mat-block\">\n    <mat-label></mat-label>\n    <mat-chip-list #metadataNamesChipList>\n      <mat-chip\n        *ngFor=\"let metadataName of checkMessageConfigForm.get('metadataNames').value;\"\n        (removed)=\"removeMetadataName(metadataName)\">\n        {{metadataName}}\n        <mat-icon matChipRemove>close</mat-icon>\n      </mat-chip>\n      <input matInput type=\"text\" placeholder=\"{{'tb.rulenode.metadata-keys' | translate}}\"\n             style=\"max-width: 200px;\"\n             [matChipInputFor]=\"metadataNamesChipList\"\n             [matChipInputSeparatorKeyCodes]=\"separatorKeysCodes\"\n             (matChipInputTokenEnd)=\"addMetadataName($event)\"\n             [matChipInputAddOnBlur]=\"true\">\n    </mat-chip-list>\n  </mat-form-field>\n  <div class=\"tb-hint\" translate>tb.rulenode.separator-hint</div>\n  <mat-checkbox fxFlex formControlName=\"checkAllKeys\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.check-all-keys' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" translate>tb.rulenode.check-all-keys-hint</div>\n</section>\n", styles: [":host label.tb-title{margin-bottom:-10px}\n"], components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i5$2.MatChipList, selector: "mat-chip-list", inputs: ["aria-orientation", "multiple", "compareWith", "value", "required", "placeholder", "disabled", "selectable", "tabIndex", "errorStateMatcher"], outputs: ["change", "valueChange"], exportAs: ["matChipList"] }, { type: i6$1.MatIcon, selector: "mat-icon", inputs: ["color", "inline", "svgIcon", "fontSet", "fontIcon"], exportAs: ["matIcon"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i5$2.MatChip, selector: "mat-basic-chip, [mat-basic-chip], mat-chip, [mat-chip]", inputs: ["color", "disableRipple", "tabIndex", "selected", "value", "selectable", "disabled", "removable"], outputs: ["selectionChange", "destroyed", "removed"], exportAs: ["matChip"] }, { type: i5$2.MatChipRemove, selector: "[matChipRemove]" }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i5$2.MatChipInput, selector: "input[matChipInputFor]", inputs: ["matChipInputSeparatorKeyCodes", "placeholder", "id", "matChipInputFor", "matChipInputAddOnBlur", "disabled"], outputs: ["matChipInputTokenEnd"], exportAs: ["matChipInput", "matChipInputFor"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CheckMessageConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-filter-node-check-message-config',
                    templateUrl: './check-message-config.component.html',
                    styleUrls: ['./check-message-config.component.scss']
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class CheckRelationConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.entitySearchDirection = Object.keys(EntitySearchDirection);
        this.entitySearchDirectionTranslationsMap = entitySearchDirectionTranslations;
    }
    configForm() {
        return this.checkRelationConfigForm;
    }
    onConfigurationSet(configuration) {
        this.checkRelationConfigForm = this.fb.group({
            checkForSingleEntity: [configuration ? configuration.checkForSingleEntity : false, []],
            direction: [configuration ? configuration.direction : null, []],
            entityType: [configuration ? configuration.entityType : null,
                configuration && configuration.checkForSingleEntity ? [Validators.required] : []],
            entityId: [configuration ? configuration.entityId : null,
                configuration && configuration.checkForSingleEntity ? [Validators.required] : []],
            relationType: [configuration ? configuration.relationType : null, [Validators.required]]
        });
    }
    validatorTriggers() {
        return ['checkForSingleEntity'];
    }
    updateValidators(emitEvent) {
        const checkForSingleEntity = this.checkRelationConfigForm.get('checkForSingleEntity').value;
        this.checkRelationConfigForm.get('entityType').setValidators(checkForSingleEntity ? [Validators.required] : []);
        this.checkRelationConfigForm.get('entityType').updateValueAndValidity({ emitEvent });
        this.checkRelationConfigForm.get('entityId').setValidators(checkForSingleEntity ? [Validators.required] : []);
        this.checkRelationConfigForm.get('entityId').updateValueAndValidity({ emitEvent });
    }
}
CheckRelationConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CheckRelationConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
CheckRelationConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: CheckRelationConfigComponent, selector: "tb-filter-node-check-relation-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"checkRelationConfigForm\" fxLayout=\"column\">\n  <mat-checkbox fxFlex formControlName=\"checkForSingleEntity\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.check-relation-to-specific-entity' | translate }}\n  </mat-checkbox>\n  <div class=\"tb-hint\" translate>tb.rulenode.check-relation-hint</div>\n  <mat-form-field class=\"mat-block\" style=\"min-width: 100px;\">\n    <mat-label translate>relation.direction</mat-label>\n    <mat-select formControlName=\"direction\" required>\n      <mat-option *ngFor=\"let direction of entitySearchDirection\" [value]=\"direction\">\n        {{ entitySearchDirectionTranslationsMap.get(direction) | translate }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n  <div fxLayout=\"row\" *ngIf=\"checkRelationConfigForm.get('checkForSingleEntity').value\" style=\"padding-top: 20px\">\n    <tb-entity-type-select\n      style=\"min-width: 100px; padding-bottom: 20px; padding-right: 8px;\"\n      showLabel\n      required\n      formControlName=\"entityType\">\n    </tb-entity-type-select>\n    <tb-entity-autocomplete\n      fxFlex\n      required\n      *ngIf=\"checkRelationConfigForm.get('entityType').value\"\n      [entityType]=\"checkRelationConfigForm.get('entityType').value\"\n      formControlName=\"entityId\">\n    </tb-entity-autocomplete>\n  </div>\n  <tb-relation-type-autocomplete\n    required\n    formControlName=\"relationType\">\n  </tb-relation-type-autocomplete>\n</section>\n", components: [{ type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: i7$1.EntityTypeSelectComponent, selector: "tb-entity-type-select", inputs: ["allowedEntityTypes", "useAliasEntityTypes", "showLabel", "required", "disabled"] }, { type: i8$3.EntityAutocompleteComponent, selector: "tb-entity-autocomplete", inputs: ["entityType", "entitySubtype", "excludeEntityIds", "labelText", "requiredText", "required", "disabled"], outputs: ["entityChanged"] }, { type: i7$3.RelationTypeAutocompleteComponent, selector: "tb-relation-type-autocomplete", inputs: ["required", "disabled"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: CheckRelationConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-filter-node-check-relation-config',
                    templateUrl: './check-relation-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class GpsGeoFilterConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.perimeterType = PerimeterType;
        this.perimeterTypes = Object.keys(PerimeterType);
        this.perimeterTypeTranslationMap = perimeterTypeTranslations;
        this.rangeUnits = Object.keys(RangeUnit);
        this.rangeUnitTranslationMap = rangeUnitTranslations;
    }
    configForm() {
        return this.geoFilterConfigForm;
    }
    onConfigurationSet(configuration) {
        this.geoFilterConfigForm = this.fb.group({
            latitudeKeyName: [configuration ? configuration.latitudeKeyName : null, [Validators.required]],
            longitudeKeyName: [configuration ? configuration.longitudeKeyName : null, [Validators.required]],
            fetchPerimeterInfoFromMessageMetadata: [configuration ? configuration.fetchPerimeterInfoFromMessageMetadata : false, []],
            perimeterType: [configuration ? configuration.perimeterType : null, []],
            centerLatitude: [configuration ? configuration.centerLatitude : null, []],
            centerLongitude: [configuration ? configuration.centerLatitude : null, []],
            range: [configuration ? configuration.range : null, []],
            rangeUnit: [configuration ? configuration.rangeUnit : null, []],
            polygonsDefinition: [configuration ? configuration.polygonsDefinition : null, []]
        });
    }
    validatorTriggers() {
        return ['fetchPerimeterInfoFromMessageMetadata', 'perimeterType'];
    }
    updateValidators(emitEvent) {
        const fetchPerimeterInfoFromMessageMetadata = this.geoFilterConfigForm.get('fetchPerimeterInfoFromMessageMetadata').value;
        const perimeterType = this.geoFilterConfigForm.get('perimeterType').value;
        if (fetchPerimeterInfoFromMessageMetadata) {
            this.geoFilterConfigForm.get('perimeterType').setValidators([]);
        }
        else {
            this.geoFilterConfigForm.get('perimeterType').setValidators([Validators.required]);
        }
        if (!fetchPerimeterInfoFromMessageMetadata && perimeterType === PerimeterType.CIRCLE) {
            this.geoFilterConfigForm.get('centerLatitude').setValidators([Validators.required,
                Validators.min(-90), Validators.max(90)]);
            this.geoFilterConfigForm.get('centerLongitude').setValidators([Validators.required,
                Validators.min(-180), Validators.max(180)]);
            this.geoFilterConfigForm.get('range').setValidators([Validators.required, Validators.min(0)]);
            this.geoFilterConfigForm.get('rangeUnit').setValidators([Validators.required]);
        }
        else {
            this.geoFilterConfigForm.get('centerLatitude').setValidators([]);
            this.geoFilterConfigForm.get('centerLongitude').setValidators([]);
            this.geoFilterConfigForm.get('range').setValidators([]);
            this.geoFilterConfigForm.get('rangeUnit').setValidators([]);
        }
        if (!fetchPerimeterInfoFromMessageMetadata && perimeterType === PerimeterType.POLYGON) {
            this.geoFilterConfigForm.get('polygonsDefinition').setValidators([Validators.required]);
        }
        else {
            this.geoFilterConfigForm.get('polygonsDefinition').setValidators([]);
        }
        this.geoFilterConfigForm.get('perimeterType').updateValueAndValidity({ emitEvent: false });
        this.geoFilterConfigForm.get('centerLatitude').updateValueAndValidity({ emitEvent });
        this.geoFilterConfigForm.get('centerLongitude').updateValueAndValidity({ emitEvent });
        this.geoFilterConfigForm.get('range').updateValueAndValidity({ emitEvent });
        this.geoFilterConfigForm.get('rangeUnit').updateValueAndValidity({ emitEvent });
        this.geoFilterConfigForm.get('polygonsDefinition').updateValueAndValidity({ emitEvent });
    }
}
GpsGeoFilterConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: GpsGeoFilterConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
GpsGeoFilterConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: GpsGeoFilterConfigComponent, selector: "tb-filter-node-gps-geofencing-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"geoFilterConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.latitude-key-name</mat-label>\n    <input matInput formControlName=\"latitudeKeyName\" required>\n    <mat-error *ngIf=\"geoFilterConfigForm.get('latitudeKeyName').hasError('required')\">\n      {{ 'tb.rulenode.latitude-key-name-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.longitude-key-name</mat-label>\n    <input matInput formControlName=\"longitudeKeyName\" required>\n    <mat-error *ngIf=\"geoFilterConfigForm.get('longitudeKeyName').hasError('required')\">\n      {{ 'tb.rulenode.longitude-key-name-required' | translate }}\n    </mat-error>\n  </mat-form-field>\n  <mat-checkbox fxFlex formControlName=\"fetchPerimeterInfoFromMessageMetadata\" style=\"padding-bottom: 16px;\">\n    {{ 'tb.rulenode.fetch-perimeter-info-from-message-metadata' | translate }}\n  </mat-checkbox>\n  <div fxLayout=\"row\" *ngIf=\"!geoFilterConfigForm.get('fetchPerimeterInfoFromMessageMetadata').value\">\n    <mat-form-field fxFlex class=\"mat-block\">\n      <mat-label translate>tb.rulenode.perimeter-type</mat-label>\n      <mat-select formControlName=\"perimeterType\" required>\n        <mat-option *ngFor=\"let type of perimeterTypes\" [value]=\"type\">\n          {{ perimeterTypeTranslationMap.get(type) | translate }}\n        </mat-option>\n      </mat-select>\n    </mat-form-field>\n  </div>\n  <div fxLayout=\"column\"\n       *ngIf=\"geoFilterConfigForm.get('perimeterType').value === perimeterType.CIRCLE &&\n       !geoFilterConfigForm.get('fetchPerimeterInfoFromMessageMetadata').value\">\n    <div fxLayout=\"row\" fxLayoutGap=\"8px\">\n      <mat-form-field fxFlex>\n        <mat-label translate>tb.rulenode.circle-center-latitude</mat-label>\n        <input type=\"number\" min=\"-90\" max=\"90\" step=\"0.1\" matInput formControlName=\"centerLatitude\" required>\n        <mat-error *ngIf=\"geoFilterConfigForm.get('centerLatitude').hasError('required')\">\n          {{ 'tb.rulenode.circle-center-latitude-required' | translate }}\n        </mat-error>\n      </mat-form-field>\n      <mat-form-field fxFlex>\n        <mat-label translate>tb.rulenode.circle-center-longitude</mat-label>\n        <input type=\"number\" min=\"-180\" max=\"180\" step=\"0.1\" matInput formControlName=\"centerLongitude\" required>\n        <mat-error *ngIf=\"geoFilterConfigForm.get('centerLongitude').hasError('required')\">\n          {{ 'tb.rulenode.circle-center-longitude-required' | translate }}\n        </mat-error>\n      </mat-form-field>\n    </div>\n    <div fxLayout=\"row\" fxLayoutGap=\"8px\">\n      <mat-form-field fxFlex>\n        <mat-label translate>tb.rulenode.range</mat-label>\n        <input type=\"number\" min=\"0\" step=\"0.1\" matInput formControlName=\"range\" required>\n        <mat-error *ngIf=\"geoFilterConfigForm.get('range').hasError('required')\">\n          {{ 'tb.rulenode.range-required' | translate }}\n        </mat-error>\n      </mat-form-field>\n      <mat-form-field fxFlex>\n        <mat-label translate>tb.rulenode.range-units</mat-label>\n        <mat-select formControlName=\"rangeUnit\" required>\n          <mat-option *ngFor=\"let type of rangeUnits\" [value]=\"type\">\n            {{ rangeUnitTranslationMap.get(type) | translate }}\n          </mat-option>\n        </mat-select>\n      </mat-form-field>\n    </div>\n  </div>\n  <div fxLayout=\"row\" *ngIf=\"geoFilterConfigForm.get('perimeterType').value === perimeterType.POLYGON &&\n                             !geoFilterConfigForm.get('fetchPerimeterInfoFromMessageMetadata').value\">\n    <div fxLayout=\"column\" fxFlex=\"100\">\n      <mat-form-field class=\"mat-block\" hintLabel=\"{{'tb.rulenode.polygon-definition-hint' | translate}}\">\n        <mat-label translate>tb.rulenode.polygon-definition</mat-label>\n        <input matInput formControlName=\"polygonsDefinition\" required>\n        <mat-error *ngIf=\"geoFilterConfigForm.get('polygonsDefinition').hasError('required')\">\n          {{ 'tb.rulenode.polygon-definition-required' | translate }}\n        </mat-error>\n      </mat-form-field>\n    </div>\n  </div>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i3$1.MatCheckbox, selector: "mat-checkbox", inputs: ["disableRipple", "color", "tabIndex", "aria-label", "aria-labelledby", "id", "labelPosition", "name", "required", "checked", "disabled", "indeterminate", "aria-describedby", "value"], outputs: ["change", "indeterminateChange"], exportAs: ["matCheckbox"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i8.DefaultLayoutGapDirective, selector: "  [fxLayoutGap], [fxLayoutGap.xs], [fxLayoutGap.sm], [fxLayoutGap.md],  [fxLayoutGap.lg], [fxLayoutGap.xl], [fxLayoutGap.lt-sm], [fxLayoutGap.lt-md],  [fxLayoutGap.lt-lg], [fxLayoutGap.lt-xl], [fxLayoutGap.gt-xs], [fxLayoutGap.gt-sm],  [fxLayoutGap.gt-md], [fxLayoutGap.gt-lg]", inputs: ["fxLayoutGap", "fxLayoutGap.xs", "fxLayoutGap.sm", "fxLayoutGap.md", "fxLayoutGap.lg", "fxLayoutGap.xl", "fxLayoutGap.lt-sm", "fxLayoutGap.lt-md", "fxLayoutGap.lt-lg", "fxLayoutGap.lt-xl", "fxLayoutGap.gt-xs", "fxLayoutGap.gt-sm", "fxLayoutGap.gt-md", "fxLayoutGap.gt-lg"] }, { type: i2.MinValidator, selector: "input[type=number][min][formControlName],input[type=number][min][formControl],input[type=number][min][ngModel]", inputs: ["min"] }, { type: i2.MaxValidator, selector: "input[type=number][max][formControlName],input[type=number][max][formControl],input[type=number][max][ngModel]", inputs: ["max"] }, { type: i2.NumberValueAccessor, selector: "input[type=number][formControlName],input[type=number][formControl],input[type=number][ngModel]" }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: GpsGeoFilterConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-filter-node-gps-geofencing-config',
                    templateUrl: './gps-geo-filter-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class MessageTypeConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.messageTypeConfigForm;
    }
    onConfigurationSet(configuration) {
        this.messageTypeConfigForm = this.fb.group({
            messageTypes: [configuration ? configuration.messageTypes : null, [Validators.required]]
        });
    }
}
MessageTypeConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: MessageTypeConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
MessageTypeConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: MessageTypeConfigComponent, selector: "tb-filter-node-message-type-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"messageTypeConfigForm\" fxLayout=\"column\">\n  <tb-message-types-config\n    required\n    label=\"tb.rulenode.message-types-filter\"\n    formControlName=\"messageTypes\"\n  ></tb-message-types-config>\n</section>\n", components: [{ type: MessageTypesConfigComponent, selector: "tb-message-types-config", inputs: ["required", "label", "placeholder", "disabled"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: MessageTypeConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-filter-node-message-type-config',
                    templateUrl: './message-type-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class OriginatorTypeConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.allowedEntityTypes = [
            EntityType.DEVICE,
            EntityType.ASSET,
            EntityType.ENTITY_VIEW,
            EntityType.TENANT,
            EntityType.CUSTOMER,
            EntityType.USER,
            EntityType.DASHBOARD,
            EntityType.RULE_CHAIN,
            EntityType.RULE_NODE
        ];
    }
    configForm() {
        return this.originatorTypeConfigForm;
    }
    onConfigurationSet(configuration) {
        this.originatorTypeConfigForm = this.fb.group({
            originatorTypes: [configuration ? configuration.originatorTypes : null, [Validators.required]]
        });
    }
}
OriginatorTypeConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: OriginatorTypeConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
OriginatorTypeConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: OriginatorTypeConfigComponent, selector: "tb-filter-node-originator-type-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"originatorTypeConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title no-padding tb-required\">tb.rulenode.originator-types-filter</label>\n  <tb-entity-type-list fxFlex\n                       formControlName=\"originatorTypes\"\n                       [allowedEntityTypes]=\"allowedEntityTypes\"\n                       [ignoreAuthorityFilter]=\"true\"\n                       required>\n  </tb-entity-type-list>\n</section>\n", styles: [":host ::ng-deep tb-entity-type-list .mat-form-field-flex{padding-top:0}:host ::ng-deep tb-entity-type-list .mat-form-field-infix{border-top:0}\n"], components: [{ type: i3$5.EntityTypeListComponent, selector: "tb-entity-type-list", inputs: ["required", "disabled", "allowedEntityTypes", "ignoreAuthorityFilter"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i8.DefaultFlexDirective, selector: "  [fxFlex], [fxFlex.xs], [fxFlex.sm], [fxFlex.md],  [fxFlex.lg], [fxFlex.xl], [fxFlex.lt-sm], [fxFlex.lt-md],  [fxFlex.lt-lg], [fxFlex.lt-xl], [fxFlex.gt-xs], [fxFlex.gt-sm],  [fxFlex.gt-md], [fxFlex.gt-lg]", inputs: ["fxFlex", "fxFlex.xs", "fxFlex.sm", "fxFlex.md", "fxFlex.lg", "fxFlex.xl", "fxFlex.lt-sm", "fxFlex.lt-md", "fxFlex.lt-lg", "fxFlex.lt-xl", "fxFlex.gt-xs", "fxFlex.gt-sm", "fxFlex.gt-md", "fxFlex.gt-lg"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: OriginatorTypeConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-filter-node-originator-type-config',
                    templateUrl: './originator-type-config.component.html',
                    styleUrls: ['./originator-type-config.component.scss']
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class ScriptConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb, nodeScriptTestService, translate) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.nodeScriptTestService = nodeScriptTestService;
        this.translate = translate;
    }
    configForm() {
        return this.scriptConfigForm;
    }
    onConfigurationSet(configuration) {
        this.scriptConfigForm = this.fb.group({
            jsScript: [configuration ? configuration.jsScript : null, [Validators.required]]
        });
    }
    testScript() {
        const script = this.scriptConfigForm.get('jsScript').value;
        this.nodeScriptTestService.testNodeScript(script, 'filter', this.translate.instant('tb.rulenode.filter'), 'Filter', ['msg', 'metadata', 'msgType'], this.ruleNodeId, 'rulenode/filter_node_script_fn').subscribe((theScript) => {
            if (theScript) {
                this.scriptConfigForm.get('jsScript').setValue(theScript);
            }
        });
    }
    onValidate() {
        this.jsFuncComponent.validateOnSubmit();
    }
}
ScriptConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: ScriptConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }, { token: i3$3.NodeScriptTestService }, { token: i4.TranslateService }], target: i0.ɵɵFactoryTarget.Component });
ScriptConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: ScriptConfigComponent, selector: "tb-filter-node-script-config", viewQueries: [{ propertyName: "jsFuncComponent", first: true, predicate: ["jsFuncComponent"], descendants: true, static: true }], usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"scriptConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title no-padding\">tb.rulenode.filter</label>\n  <tb-js-func #jsFuncComponent\n              formControlName=\"jsScript\"\n              functionName=\"Filter\"\n              [functionArgs]=\"['msg', 'metadata', 'msgType']\"\n              helpId=\"rulenode/filter_node_script_fn\"\n              noValidate=\"true\">\n  </tb-js-func>\n  <div fxLayout=\"row\">\n    <button mat-button mat-raised-button color=\"primary\" (click)=\"testScript()\">\n      {{ 'tb.rulenode.test-filter-function' | translate }}\n    </button>\n  </div>\n</section>\n", components: [{ type: i5$1.JsFuncComponent, selector: "tb-js-func", inputs: ["functionName", "functionArgs", "validationArgs", "resultType", "disabled", "fillHeight", "editorCompleter", "globalVariables", "disableUndefinedCheck", "helpId", "noValidate", "required"] }, { type: i6.MatButton, selector: "button[mat-button], button[mat-raised-button], button[mat-icon-button],             button[mat-fab], button[mat-mini-fab], button[mat-stroked-button],             button[mat-flat-button]", inputs: ["disabled", "disableRipple", "color"], exportAs: ["matButton"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: ScriptConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-filter-node-script-config',
                    templateUrl: './script-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }, { type: i3$3.NodeScriptTestService }, { type: i4.TranslateService }]; }, propDecorators: { jsFuncComponent: [{
                type: ViewChild,
                args: ['jsFuncComponent', { static: true }]
            }] } });

class SwitchConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb, nodeScriptTestService, translate) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.nodeScriptTestService = nodeScriptTestService;
        this.translate = translate;
    }
    configForm() {
        return this.switchConfigForm;
    }
    onConfigurationSet(configuration) {
        this.switchConfigForm = this.fb.group({
            jsScript: [configuration ? configuration.jsScript : null, [Validators.required]]
        });
    }
    testScript() {
        const script = this.switchConfigForm.get('jsScript').value;
        this.nodeScriptTestService.testNodeScript(script, 'switch', this.translate.instant('tb.rulenode.switch'), 'Switch', ['msg', 'metadata', 'msgType'], this.ruleNodeId, 'rulenode/switch_node_script_fn').subscribe((theScript) => {
            if (theScript) {
                this.switchConfigForm.get('jsScript').setValue(theScript);
            }
        });
    }
    onValidate() {
        this.jsFuncComponent.validateOnSubmit();
    }
}
SwitchConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SwitchConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }, { token: i3$3.NodeScriptTestService }, { token: i4.TranslateService }], target: i0.ɵɵFactoryTarget.Component });
SwitchConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: SwitchConfigComponent, selector: "tb-filter-node-switch-config", viewQueries: [{ propertyName: "jsFuncComponent", first: true, predicate: ["jsFuncComponent"], descendants: true, static: true }], usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"switchConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title no-padding\">tb.rulenode.switch</label>\n  <tb-js-func #jsFuncComponent\n              formControlName=\"jsScript\"\n              functionName=\"Switch\"\n              [functionArgs]=\"['msg', 'metadata', 'msgType']\"\n              helpId=\"rulenode/switch_node_script_fn\"\n              noValidate=\"true\">\n  </tb-js-func>\n  <div fxLayout=\"row\">\n    <button mat-button mat-raised-button color=\"primary\" (click)=\"testScript()\">\n      {{ 'tb.rulenode.test-switch-function' | translate }}\n    </button>\n  </div>\n</section>\n", components: [{ type: i5$1.JsFuncComponent, selector: "tb-js-func", inputs: ["functionName", "functionArgs", "validationArgs", "resultType", "disabled", "fillHeight", "editorCompleter", "globalVariables", "disableUndefinedCheck", "helpId", "noValidate", "required"] }, { type: i6.MatButton, selector: "button[mat-button], button[mat-raised-button], button[mat-icon-button],             button[mat-fab], button[mat-mini-fab], button[mat-stroked-button],             button[mat-flat-button]", inputs: ["disabled", "disableRipple", "color"], exportAs: ["matButton"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: SwitchConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-filter-node-switch-config',
                    templateUrl: './switch-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }, { type: i3$3.NodeScriptTestService }, { type: i4.TranslateService }]; }, propDecorators: { jsFuncComponent: [{
                type: ViewChild,
                args: ['jsFuncComponent', { static: true }]
            }] } });

class RuleNodeCoreConfigFilterModule {
}
RuleNodeCoreConfigFilterModule.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigFilterModule, deps: [], target: i0.ɵɵFactoryTarget.NgModule });
RuleNodeCoreConfigFilterModule.ɵmod = i0.ɵɵngDeclareNgModule({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigFilterModule, declarations: [CheckMessageConfigComponent,
        CheckRelationConfigComponent,
        GpsGeoFilterConfigComponent,
        MessageTypeConfigComponent,
        OriginatorTypeConfigComponent,
        ScriptConfigComponent,
        SwitchConfigComponent,
        CheckAlarmStatusComponent], imports: [CommonModule,
        SharedModule,
        RulenodeCoreConfigCommonModule], exports: [CheckMessageConfigComponent,
        CheckRelationConfigComponent,
        GpsGeoFilterConfigComponent,
        MessageTypeConfigComponent,
        OriginatorTypeConfigComponent,
        ScriptConfigComponent,
        SwitchConfigComponent,
        CheckAlarmStatusComponent] });
RuleNodeCoreConfigFilterModule.ɵinj = i0.ɵɵngDeclareInjector({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigFilterModule, imports: [[
            CommonModule,
            SharedModule,
            RulenodeCoreConfigCommonModule
        ]] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigFilterModule, decorators: [{
            type: NgModule,
            args: [{
                    declarations: [
                        CheckMessageConfigComponent,
                        CheckRelationConfigComponent,
                        GpsGeoFilterConfigComponent,
                        MessageTypeConfigComponent,
                        OriginatorTypeConfigComponent,
                        ScriptConfigComponent,
                        SwitchConfigComponent,
                        CheckAlarmStatusComponent
                    ],
                    imports: [
                        CommonModule,
                        SharedModule,
                        RulenodeCoreConfigCommonModule
                    ],
                    exports: [
                        CheckMessageConfigComponent,
                        CheckRelationConfigComponent,
                        GpsGeoFilterConfigComponent,
                        MessageTypeConfigComponent,
                        OriginatorTypeConfigComponent,
                        ScriptConfigComponent,
                        SwitchConfigComponent,
                        CheckAlarmStatusComponent
                    ]
                }]
        }] });

class ChangeOriginatorConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.originatorSource = OriginatorSource;
        this.originatorSources = Object.keys(OriginatorSource);
        this.originatorSourceTranslationMap = originatorSourceTranslations;
    }
    configForm() {
        return this.changeOriginatorConfigForm;
    }
    onConfigurationSet(configuration) {
        this.changeOriginatorConfigForm = this.fb.group({
            originatorSource: [configuration ? configuration.originatorSource : null, [Validators.required]],
            relationsQuery: [configuration ? configuration.relationsQuery : null, []]
        });
    }
    validatorTriggers() {
        return ['originatorSource'];
    }
    updateValidators(emitEvent) {
        const originatorSource = this.changeOriginatorConfigForm.get('originatorSource').value;
        if (originatorSource && originatorSource === OriginatorSource.RELATED) {
            this.changeOriginatorConfigForm.get('relationsQuery').setValidators([Validators.required]);
        }
        else {
            this.changeOriginatorConfigForm.get('relationsQuery').setValidators([]);
        }
        this.changeOriginatorConfigForm.get('relationsQuery').updateValueAndValidity({ emitEvent });
    }
}
ChangeOriginatorConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: ChangeOriginatorConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
ChangeOriginatorConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: ChangeOriginatorConfigComponent, selector: "tb-transformation-node-change-originator-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"changeOriginatorConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.originator-source</mat-label>\n    <mat-select formControlName=\"originatorSource\" required>\n      <mat-option *ngFor=\"let source of originatorSources\" [value]=\"source\">\n        {{ originatorSourceTranslationMap.get(source) | translate }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n  <section fxLayout=\"column\" *ngIf=\"changeOriginatorConfigForm.get('originatorSource').value === originatorSource.RELATED\">\n    <label translate class=\"tb-title tb-required\">tb.rulenode.relations-query</label>\n    <tb-relations-query-config\n      required\n      formControlName=\"relationsQuery\"\n      style=\"padding-bottom: 15px;\">\n    </tb-relations-query-config>\n  </section>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }, { type: RelationsQueryConfigComponent, selector: "tb-relations-query-config", inputs: ["disabled", "required"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: ChangeOriginatorConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-transformation-node-change-originator-config',
                    templateUrl: './change-originator-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class TransformScriptConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb, nodeScriptTestService, translate) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.nodeScriptTestService = nodeScriptTestService;
        this.translate = translate;
    }
    configForm() {
        return this.scriptConfigForm;
    }
    onConfigurationSet(configuration) {
        this.scriptConfigForm = this.fb.group({
            jsScript: [configuration ? configuration.jsScript : null, [Validators.required]]
        });
    }
    testScript() {
        const script = this.scriptConfigForm.get('jsScript').value;
        this.nodeScriptTestService.testNodeScript(script, 'update', this.translate.instant('tb.rulenode.transformer'), 'Transform', ['msg', 'metadata', 'msgType'], this.ruleNodeId, 'rulenode/transformation_node_script_fn').subscribe((theScript) => {
            if (theScript) {
                this.scriptConfigForm.get('jsScript').setValue(theScript);
            }
        });
    }
    onValidate() {
        this.jsFuncComponent.validateOnSubmit();
    }
}
TransformScriptConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TransformScriptConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }, { token: i3$3.NodeScriptTestService }, { token: i4.TranslateService }], target: i0.ɵɵFactoryTarget.Component });
TransformScriptConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: TransformScriptConfigComponent, selector: "tb-transformation-node-script-config", viewQueries: [{ propertyName: "jsFuncComponent", first: true, predicate: ["jsFuncComponent"], descendants: true, static: true }], usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"scriptConfigForm\" fxLayout=\"column\">\n  <label translate class=\"tb-title no-padding\">tb.rulenode.transform</label>\n  <tb-js-func #jsFuncComponent\n              formControlName=\"jsScript\"\n              functionName=\"Transform\"\n              helpId=\"rulenode/transformation_node_script_fn\"\n              [functionArgs]=\"['msg', 'metadata', 'msgType']\"\n              noValidate=\"true\">\n  </tb-js-func>\n  <div fxLayout=\"row\">\n    <button mat-button mat-raised-button color=\"primary\" (click)=\"testScript()\">\n      {{ 'tb.rulenode.test-transformer-function' | translate }}\n    </button>\n  </div>\n</section>\n", components: [{ type: i5$1.JsFuncComponent, selector: "tb-js-func", inputs: ["functionName", "functionArgs", "validationArgs", "resultType", "disabled", "fillHeight", "editorCompleter", "globalVariables", "disableUndefinedCheck", "helpId", "noValidate", "required"] }, { type: i6.MatButton, selector: "button[mat-button], button[mat-raised-button], button[mat-icon-button],             button[mat-fab], button[mat-mini-fab], button[mat-stroked-button],             button[mat-flat-button]", inputs: ["disabled", "disableRipple", "color"], exportAs: ["matButton"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: TransformScriptConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-transformation-node-script-config',
                    templateUrl: './script-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }, { type: i3$3.NodeScriptTestService }, { type: i4.TranslateService }]; }, propDecorators: { jsFuncComponent: [{
                type: ViewChild,
                args: ['jsFuncComponent', { static: true }]
            }] } });

class ToEmailConfigComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.mailBodyTypes = [
            { name: 'tb.mail-body-type.plain-text', value: 'false' },
            { name: 'tb.mail-body-type.html', value: 'true' },
            { name: 'tb.mail-body-type.dynamic', value: 'dynamic' }
        ];
    }
    configForm() {
        return this.toEmailConfigForm;
    }
    onConfigurationSet(configuration) {
        this.toEmailConfigForm = this.fb.group({
            fromTemplate: [configuration ? configuration.fromTemplate : null, [Validators.required]],
            toTemplate: [configuration ? configuration.toTemplate : null, [Validators.required]],
            ccTemplate: [configuration ? configuration.ccTemplate : null, []],
            bccTemplate: [configuration ? configuration.bccTemplate : null, []],
            subjectTemplate: [configuration ? configuration.subjectTemplate : null, [Validators.required]],
            mailBodyType: [configuration ? configuration.mailBodyType : null],
            isHtmlTemplate: [configuration ? configuration.isHtmlTemplate : null],
            bodyTemplate: [configuration ? configuration.bodyTemplate : null, [Validators.required]],
        });
        this.toEmailConfigForm.get('mailBodyType').valueChanges.pipe(startWith([configuration === null || configuration === void 0 ? void 0 : configuration.subjectTemplate])).subscribe((mailBodyType) => {
            if (mailBodyType === 'dynamic') {
                this.toEmailConfigForm.get('isHtmlTemplate').patchValue('', { emitEvent: false });
                this.toEmailConfigForm.get('isHtmlTemplate').setValidators(Validators.required);
            }
            else {
                this.toEmailConfigForm.get('isHtmlTemplate').clearValidators();
            }
            this.toEmailConfigForm.get('isHtmlTemplate').updateValueAndValidity();
        });
    }
}
ToEmailConfigComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: ToEmailConfigComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
ToEmailConfigComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: ToEmailConfigComponent, selector: "tb-transformation-node-to-email-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"toEmailConfigForm\" fxLayout=\"column\">\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.from-template</mat-label>\n    <textarea required matInput formControlName=\"fromTemplate\" rows=\"2\"></textarea>\n    <mat-error *ngIf=\"toEmailConfigForm.get('fromTemplate').hasError('required')\">\n      {{ 'tb.rulenode.from-template-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.to-template</mat-label>\n    <textarea required matInput formControlName=\"toTemplate\" rows=\"2\"></textarea>\n    <mat-error *ngIf=\"toEmailConfigForm.get('toTemplate').hasError('required')\">\n      {{ 'tb.rulenode.to-template-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.mail-address-list-template-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 24px;\">\n    <mat-label translate>tb.rulenode.cc-template</mat-label>\n    <textarea matInput formControlName=\"ccTemplate\" rows=\"2\"></textarea>\n    <mat-hint [innerHTML]=\"'tb.rulenode.mail-address-list-template-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 24px;\">\n    <mat-label translate>tb.rulenode.bcc-template</mat-label>\n    <textarea matInput formControlName=\"bccTemplate\" rows=\"2\"></textarea>\n    <mat-hint [innerHTML]=\"'tb.rulenode.mail-address-list-template-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 24px;\">\n    <mat-label translate>tb.rulenode.subject-template</mat-label>\n    <textarea required matInput formControlName=\"subjectTemplate\" rows=\"2\"></textarea>\n    <mat-error *ngIf=\"toEmailConfigForm.get('subjectTemplate').hasError('required')\">\n      {{ 'tb.rulenode.subject-template-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\">\n    <mat-label translate>tb.rulenode.mail-body-type</mat-label>\n    <mat-select formControlName=\"mailBodyType\">\n      <mat-option *ngFor=\"let type of mailBodyTypes\" [value]=\"type.value\">\n        {{ type.name | translate }}\n      </mat-option>\n    </mat-select>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\" *ngIf=\"toEmailConfigForm.get('mailBodyType').value === 'dynamic'\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.dynamic-mail-body-type</mat-label>\n    <input required matInput formControlName=\"isHtmlTemplate\">\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n  <mat-form-field class=\"mat-block\" style=\"padding-bottom: 16px;\">\n    <mat-label translate>tb.rulenode.body-template</mat-label>\n    <textarea required matInput formControlName=\"bodyTemplate\" rows=\"6\"></textarea>\n    <mat-error *ngIf=\"toEmailConfigForm.get('bodyTemplate').hasError('required')\">\n      {{ 'tb.rulenode.body-template-required' | translate }}\n    </mat-error>\n    <mat-hint [innerHTML]=\"'tb.rulenode.general-pattern-hint' | translate | safeHtml\"></mat-hint>\n  </mat-form-field>\n</section>\n", components: [{ type: i3.MatFormField, selector: "mat-form-field", inputs: ["color", "floatLabel", "appearance", "hideRequiredMarker", "hintLabel"], exportAs: ["matFormField"] }, { type: i4$1.MatSelect, selector: "mat-select", inputs: ["disabled", "disableRipple", "tabIndex"], exportAs: ["matSelect"] }, { type: i5.MatOption, selector: "mat-option", exportAs: ["matOption"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i3.MatLabel, selector: "mat-label" }, { type: i4.TranslateDirective, selector: "[translate],[ngx-translate]", inputs: ["translate", "translateParams"] }, { type: i11.MatInput, selector: "input[matInput], textarea[matInput], select[matNativeControl],      input[matNativeControl], textarea[matNativeControl]", inputs: ["id", "disabled", "required", "type", "value", "readonly", "placeholder", "errorStateMatcher", "aria-describedby"], exportAs: ["matInput"] }, { type: i2.DefaultValueAccessor, selector: "input:not([type=checkbox])[formControlName],textarea[formControlName],input:not([type=checkbox])[formControl],textarea[formControl],input:not([type=checkbox])[ngModel],textarea[ngModel],[ngDefaultControl]" }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }, { type: i10.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i3.MatError, selector: "mat-error", inputs: ["id"] }, { type: i3.MatHint, selector: "mat-hint", inputs: ["align", "id"] }, { type: i10.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }], pipes: { "translate": i4.TranslatePipe, "safeHtml": SafeHtmlPipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: ToEmailConfigComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-transformation-node-to-email-config',
                    templateUrl: './to-email-config.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class RulenodeCoreConfigTransformModule {
}
RulenodeCoreConfigTransformModule.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RulenodeCoreConfigTransformModule, deps: [], target: i0.ɵɵFactoryTarget.NgModule });
RulenodeCoreConfigTransformModule.ɵmod = i0.ɵɵngDeclareNgModule({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RulenodeCoreConfigTransformModule, declarations: [ChangeOriginatorConfigComponent,
        TransformScriptConfigComponent,
        ToEmailConfigComponent], imports: [CommonModule,
        SharedModule,
        RulenodeCoreConfigCommonModule], exports: [ChangeOriginatorConfigComponent,
        TransformScriptConfigComponent,
        ToEmailConfigComponent] });
RulenodeCoreConfigTransformModule.ɵinj = i0.ɵɵngDeclareInjector({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RulenodeCoreConfigTransformModule, imports: [[
            CommonModule,
            SharedModule,
            RulenodeCoreConfigCommonModule
        ]] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RulenodeCoreConfigTransformModule, decorators: [{
            type: NgModule,
            args: [{
                    declarations: [
                        ChangeOriginatorConfigComponent,
                        TransformScriptConfigComponent,
                        ToEmailConfigComponent
                    ],
                    imports: [
                        CommonModule,
                        SharedModule,
                        RulenodeCoreConfigCommonModule
                    ],
                    exports: [
                        ChangeOriginatorConfigComponent,
                        TransformScriptConfigComponent,
                        ToEmailConfigComponent
                    ]
                }]
        }] });

class RuleChainInputComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
        this.entityType = EntityType;
    }
    configForm() {
        return this.ruleChainInputConfigForm;
    }
    onConfigurationSet(configuration) {
        this.ruleChainInputConfigForm = this.fb.group({
            ruleChainId: [configuration ? configuration.ruleChainId : null, [Validators.required]]
        });
    }
}
RuleChainInputComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleChainInputComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
RuleChainInputComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: RuleChainInputComponent, selector: "tb-flow-node-rule-chain-input-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"ruleChainInputConfigForm\" fxLayout=\"column\">\n  <tb-entity-autocomplete required\n                          [excludeEntityIds]=\"[ruleChainId]\"\n                          [entityType]=\"entityType.RULE_CHAIN\"\n                          [entitySubtype]=\"ruleChainType\"\n                          formControlName=\"ruleChainId\">\n  </tb-entity-autocomplete>\n</section>\n", components: [{ type: i8$3.EntityAutocompleteComponent, selector: "tb-entity-autocomplete", inputs: ["entityType", "entitySubtype", "excludeEntityIds", "labelText", "requiredText", "required", "disabled"], outputs: ["entityChanged"] }], directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }, { type: i2.RequiredValidator, selector: ":not([type=checkbox])[required][formControlName],:not([type=checkbox])[required][formControl],:not([type=checkbox])[required][ngModel]", inputs: ["required"] }, { type: i2.NgControlStatus, selector: "[formControlName],[ngModel],[formControl]" }, { type: i2.FormControlName, selector: "[formControlName]", inputs: ["disabled", "formControlName", "ngModel"], outputs: ["ngModelChange"] }] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleChainInputComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-flow-node-rule-chain-input-config',
                    templateUrl: './rule-chain-input.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class RuleChainOutputComponent extends RuleNodeConfigurationComponent {
    constructor(store, fb) {
        super(store);
        this.store = store;
        this.fb = fb;
    }
    configForm() {
        return this.ruleChainOutputConfigForm;
    }
    onConfigurationSet(configuration) {
        this.ruleChainOutputConfigForm = this.fb.group({});
    }
}
RuleChainOutputComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleChainOutputComponent, deps: [{ token: i1.Store }, { token: i2.FormBuilder }], target: i0.ɵɵFactoryTarget.Component });
RuleChainOutputComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "12.2.14", type: RuleChainOutputComponent, selector: "tb-flow-node-rule-chain-output-config", usesInheritance: true, ngImport: i0, template: "<section [formGroup]=\"ruleChainOutputConfigForm\" fxLayout=\"column\">\n  <div innerHTML=\"{{ 'tb.rulenode.output-node-name-hint' | translate }}\"></div>\n</section>\n", directives: [{ type: i8.DefaultLayoutDirective, selector: "  [fxLayout], [fxLayout.xs], [fxLayout.sm], [fxLayout.md],  [fxLayout.lg], [fxLayout.xl], [fxLayout.lt-sm], [fxLayout.lt-md],  [fxLayout.lt-lg], [fxLayout.lt-xl], [fxLayout.gt-xs], [fxLayout.gt-sm],  [fxLayout.gt-md], [fxLayout.gt-lg]", inputs: ["fxLayout", "fxLayout.xs", "fxLayout.sm", "fxLayout.md", "fxLayout.lg", "fxLayout.xl", "fxLayout.lt-sm", "fxLayout.lt-md", "fxLayout.lt-lg", "fxLayout.lt-xl", "fxLayout.gt-xs", "fxLayout.gt-sm", "fxLayout.gt-md", "fxLayout.gt-lg"] }, { type: i2.NgControlStatusGroup, selector: "[formGroupName],[formArrayName],[ngModelGroup],[formGroup],form:not([ngNoForm]),[ngForm]" }, { type: i2.FormGroupDirective, selector: "[formGroup]", inputs: ["formGroup"], outputs: ["ngSubmit"], exportAs: ["ngForm"] }], pipes: { "translate": i4.TranslatePipe } });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleChainOutputComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'tb-flow-node-rule-chain-output-config',
                    templateUrl: './rule-chain-output.component.html',
                    styleUrls: []
                }]
        }], ctorParameters: function () { return [{ type: i1.Store }, { type: i2.FormBuilder }]; } });

class RuleNodeCoreConfigFlowModule {
}
RuleNodeCoreConfigFlowModule.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigFlowModule, deps: [], target: i0.ɵɵFactoryTarget.NgModule });
RuleNodeCoreConfigFlowModule.ɵmod = i0.ɵɵngDeclareNgModule({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigFlowModule, declarations: [RuleChainInputComponent,
        RuleChainOutputComponent], imports: [CommonModule,
        SharedModule,
        RulenodeCoreConfigCommonModule], exports: [RuleChainInputComponent,
        RuleChainOutputComponent] });
RuleNodeCoreConfigFlowModule.ɵinj = i0.ɵɵngDeclareInjector({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigFlowModule, imports: [[
            CommonModule,
            SharedModule,
            RulenodeCoreConfigCommonModule
        ]] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigFlowModule, decorators: [{
            type: NgModule,
            args: [{
                    declarations: [
                        RuleChainInputComponent,
                        RuleChainOutputComponent
                    ],
                    imports: [
                        CommonModule,
                        SharedModule,
                        RulenodeCoreConfigCommonModule
                    ],
                    exports: [
                        RuleChainInputComponent,
                        RuleChainOutputComponent
                    ]
                }]
        }] });

function addRuleNodeCoreLocaleEnglish(translate) {
    const enUS = {
        tb: {
            rulenode: {
                'create-entity-if-not-exists': 'Create new entity if not exists',
                'create-entity-if-not-exists-hint': 'Create a new entity set above if it does not exist.',
                'entity-name-pattern': 'Name pattern',
                'entity-name-pattern-required': 'Name pattern is required',
                'entity-type-pattern': 'Type pattern',
                'entity-type-pattern-required': 'Type pattern is required',
                'entity-cache-expiration': 'Entities cache expiration time (sec)',
                'entity-cache-expiration-hint': 'Specifies maximum time interval allowed to store found entity records. 0 value means that records will never expire.',
                'entity-cache-expiration-required': 'Entities cache expiration time is required.',
                'entity-cache-expiration-range': 'Entities cache expiration time should be greater than or equal to 0.',
                'customer-name-pattern': 'Customer name pattern',
                'customer-name-pattern-required': 'Customer name pattern is required',
                'create-customer-if-not-exists': 'Create new customer if not exists',
                'customer-cache-expiration': 'Customers cache expiration time (sec)',
                'customer-cache-expiration-hint': 'Specifies maximum time interval allowed to store found customer records. 0 value means that records will never expire.',
                'customer-cache-expiration-required': 'Customers cache expiration time is required.',
                'customer-cache-expiration-range': 'Customers cache expiration time should be greater than or equal to 0.',
                'start-interval': 'Start Interval',
                'end-interval': 'End Interval',
                'start-interval-time-unit': 'Start Interval Time Unit',
                'end-interval-time-unit': 'End Interval Time Unit',
                'fetch-mode': 'Fetch mode',
                'fetch-mode-hint': 'If selected fetch mode \'ALL\'  you able to choose telemetry sampling order.',
                'order-by': 'Order by',
                'order-by-hint': 'Select to choose telemetry sampling order.',
                limit: 'Limit',
                'limit-hint': 'Min limit value is 2, max - 1000. In case you want to fetch a single entry, ' +
                    'select fetch mode \'FIRST\' or \'LAST\'.',
                'time-unit-milliseconds': 'Milliseconds',
                'time-unit-seconds': 'Seconds',
                'time-unit-minutes': 'Minutes',
                'time-unit-hours': 'Hours',
                'time-unit-days': 'Days',
                'time-value-range': 'Time value should be in a range from 1 to 2147483647.',
                'start-interval-value-required': 'Start interval value is required.',
                'end-interval-value-required': 'End interval value is required.',
                filter: 'Filter',
                switch: 'Switch',
                'message-type': 'Message type',
                'message-type-required': 'Message type is required.',
                'message-types-filter': 'Message types filter',
                'no-message-types-found': 'No message types found',
                'no-message-type-matching': '\'{{messageType}}\' not found.',
                'create-new-message-type': 'Create a new one!',
                'message-types-required': 'Message types are required.',
                'client-attributes': 'Client attributes',
                'shared-attributes': 'Shared attributes',
                'server-attributes': 'Server attributes',
                'notify-device': 'Notify Device',
                'notify-device-hint': 'If the message arrives from the device, we will push it back to the device by default.',
                'latest-timeseries': 'Latest timeseries',
                'timeseries-key': 'Timeseries key',
                'data-keys': 'Message data',
                'metadata-keys': 'Message metadata',
                'relations-query': 'Relations query',
                'device-relations-query': 'Device relations query',
                'max-relation-level': 'Max relation level',
                'relation-type-pattern': 'Relation type pattern',
                'relation-type-pattern-required': 'Relation type pattern is required',
                'relation-types-list': 'Relation types to propagate',
                'relation-types-list-hint': 'If Propagate relation types are not selected, ' +
                    'alarms will be propagated without filtering by relation type.',
                'unlimited-level': 'Unlimited level',
                'latest-telemetry': 'Latest telemetry',
                'attr-mapping': 'Attributes mapping',
                'source-attribute': 'Source attribute',
                'source-attribute-required': 'Source attribute is required.',
                'source-telemetry': 'Source telemetry',
                'source-telemetry-required': 'Source telemetry is required.',
                'target-attribute': 'Target attribute',
                'target-attribute-required': 'Target attribute is required.',
                'attr-mapping-required': 'At least one attribute mapping should be specified.',
                'fields-mapping': 'Fields mapping',
                'fields-mapping-required': 'At least one field mapping should be specified.',
                'source-field': 'Source field',
                'source-field-required': 'Source field is required.',
                'originator-source': 'Originator source',
                'originator-customer': 'Customer',
                'originator-tenant': 'Tenant',
                'originator-related': 'Related',
                'originator-alarm-originator': 'Alarm Originator',
                'clone-message': 'Clone message',
                transform: 'Transform',
                'default-ttl': 'Default TTL in seconds',
                'default-ttl-required': 'Default TTL is required.',
                'min-default-ttl-message': 'Only 0 minimum TTL is allowed.',
                'message-count': 'Message count (0 - unlimited)',
                'message-count-required': 'Message count is required.',
                'min-message-count-message': 'Only 0 minimum message count is allowed.',
                'period-seconds': 'Period in seconds',
                'period-seconds-required': 'Period is required.',
                'use-metadata-period-in-seconds-patterns': 'Use period in seconds pattern',
                'use-metadata-period-in-seconds-patterns-hint': 'If selected, rule node use period in seconds interval pattern from message metadata or data ' +
                    'assuming that intervals are in the seconds.',
                'period-in-seconds-pattern': 'Period in seconds pattern',
                'period-in-seconds-pattern-required': 'Period in seconds pattern is required',
                'min-period-seconds-message': 'Only 1 second minimum period is allowed.',
                originator: 'Originator',
                'message-body': 'Message body',
                'message-metadata': 'Message metadata',
                generate: 'Generate',
                'test-generator-function': 'Test generator function',
                generator: 'Generator',
                'test-filter-function': 'Test filter function',
                'test-switch-function': 'Test switch function',
                'test-transformer-function': 'Test transformer function',
                transformer: 'Transformer',
                'alarm-create-condition': 'Alarm create condition',
                'test-condition-function': 'Test condition function',
                'alarm-clear-condition': 'Alarm clear condition',
                'alarm-details-builder': 'Alarm details builder',
                'test-details-function': 'Test details function',
                'alarm-type': 'Alarm type',
                'alarm-type-required': 'Alarm type is required.',
                'alarm-severity': 'Alarm severity',
                'alarm-severity-required': 'Alarm severity is required',
                'alarm-status-filter': 'Alarm status filter',
                'alarm-status-list-empty': 'Alarm status list is empty',
                'no-alarm-status-matching': 'No alarm status matching were found.',
                propagate: 'Propagate',
                condition: 'Condition',
                details: 'Details',
                'to-string': 'To string',
                'test-to-string-function': 'Test to string function',
                'from-template': 'From Template',
                'from-template-required': 'From Template is required',
                'to-template': 'To Template',
                'to-template-required': 'To Template is required',
                'mail-address-list-template-hint': 'Comma separated address list, use <code><span style="color: #000;">$&#123;</span>metadataKey<span style="color: #000;">' +
                    '&#125;</span></code> for value from metadata, <code><span style="color: #000;">$[</span>messageKey' +
                    '<span style="color: #000;">]</span></code> for value from message body',
                'cc-template': 'Cc Template',
                'bcc-template': 'Bcc Template',
                'subject-template': 'Subject Template',
                'subject-template-required': 'Subject Template is required',
                'body-template': 'Body Template',
                'body-template-required': 'Body Template is required',
                'dynamic-mail-body-type': 'Dynamic mail body type',
                'mail-body-type': 'Mail body type',
                'request-id-metadata-attribute': 'Request Id Metadata attribute name',
                'timeout-sec': 'Timeout in seconds',
                'timeout-required': 'Timeout is required',
                'min-timeout-message': 'Only 0 minimum timeout value is allowed.',
                'endpoint-url-pattern': 'Endpoint URL pattern',
                'endpoint-url-pattern-required': 'Endpoint URL pattern is required',
                'request-method': 'Request method',
                'use-simple-client-http-factory': 'Use simple client HTTP factory',
                'ignore-request-body': 'Without request body',
                'read-timeout': 'Read timeout in millis',
                'read-timeout-hint': 'The value of 0 means an infinite timeout',
                'max-parallel-requests-count': 'Max number of parallel requests',
                'max-parallel-requests-count-hint': 'The value of 0 specifies no limit in parallel processing',
                headers: 'Headers',
                'headers-hint': 'Use <code><span style="color: #000;">$&#123;</span>metadataKey<span style="color: #000;">&#125;</span></code> ' +
                    'for value from metadata, <code><span style="color: #000;">$[</span>messageKey<span style="color: #000;">]</span></code> ' +
                    'for value from message body in header/value fields',
                header: 'Header',
                'header-required': 'Header is required',
                value: 'Value',
                'value-required': 'Value is required',
                'topic-pattern': 'Topic pattern',
                'topic-pattern-required': 'Topic pattern is required',
                topic: 'Topic',
                'topic-required': 'Topic is required',
                'bootstrap-servers': 'Bootstrap servers',
                'bootstrap-servers-required': 'Bootstrap servers value is required',
                'other-properties': 'Other properties',
                key: 'Key',
                'key-required': 'Key is required',
                retries: 'Automatically retry times if fails',
                'min-retries-message': 'Only 0 minimum retries is allowed.',
                'batch-size-bytes': 'Produces batch size in bytes',
                'min-batch-size-bytes-message': 'Only 0 minimum batch size is allowed.',
                'linger-ms': 'Time to buffer locally (ms)',
                'min-linger-ms-message': 'Only 0 ms minimum value is allowed.',
                'buffer-memory-bytes': 'Client buffer max size in bytes',
                'min-buffer-memory-message': 'Only 0 minimum buffer size is allowed.',
                acks: 'Number of acknowledgments',
                'key-serializer': 'Key serializer',
                'key-serializer-required': 'Key serializer is required',
                'value-serializer': 'Value serializer',
                'value-serializer-required': 'Value serializer is required',
                'topic-arn-pattern': 'Topic ARN pattern',
                'topic-arn-pattern-required': 'Topic ARN pattern is required',
                'aws-access-key-id': 'AWS Access Key ID',
                'aws-access-key-id-required': 'AWS Access Key ID is required',
                'aws-secret-access-key': 'AWS Secret Access Key',
                'aws-secret-access-key-required': 'AWS Secret Access Key is required',
                'aws-region': 'AWS Region',
                'aws-region-required': 'AWS Region is required',
                'exchange-name-pattern': 'Exchange name pattern',
                'routing-key-pattern': 'Routing key pattern',
                'message-properties': 'Message properties',
                host: 'Host',
                'host-required': 'Host is required',
                port: 'Port',
                'port-required': 'Port is required',
                'port-range': 'Port should be in a range from 1 to 65535.',
                'virtual-host': 'Virtual host',
                username: 'Username',
                password: 'Password',
                'automatic-recovery': 'Automatic recovery',
                'connection-timeout-ms': 'Connection timeout (ms)',
                'min-connection-timeout-ms-message': 'Only 0 ms minimum value is allowed.',
                'handshake-timeout-ms': 'Handshake timeout (ms)',
                'min-handshake-timeout-ms-message': 'Only 0 ms minimum value is allowed.',
                'client-properties': 'Client properties',
                'queue-url-pattern': 'Queue URL pattern',
                'queue-url-pattern-required': 'Queue URL pattern is required',
                'delay-seconds': 'Delay (seconds)',
                'min-delay-seconds-message': 'Only 0 seconds minimum value is allowed.',
                'max-delay-seconds-message': 'Only 900 seconds maximum value is allowed.',
                name: 'Name',
                'name-required': 'Name is required',
                'queue-type': 'Queue type',
                'sqs-queue-standard': 'Standard',
                'sqs-queue-fifo': 'FIFO',
                'gcp-project-id': 'GCP project ID',
                'gcp-project-id-required': 'GCP project ID is required',
                'gcp-service-account-key': 'GCP service account key file',
                'gcp-service-account-key-required': 'GCP service account key file is required',
                'pubsub-topic-name': 'Topic name',
                'pubsub-topic-name-required': 'Topic name is required',
                'message-attributes': 'Message attributes',
                'message-attributes-hint': 'Use <code><span style="color: #000;">$&#123;</span>metadataKey<span style="color: #000;">&#125;</span></code> ' +
                    'for value from metadata, <code><span style="color: #000;">$[</span>messageKey<span style="color: #000;">]</span></code> ' +
                    'for value from message body in name/value fields',
                'connect-timeout': 'Connection timeout (sec)',
                'connect-timeout-required': 'Connection timeout is required.',
                'connect-timeout-range': 'Connection timeout should be in a range from 1 to 200.',
                'client-id': 'Client ID',
                'client-id-hint': 'Hint: Optional. Leave empty for auto-generated client id. Be careful when specifying the Client ID.' +
                    'Majority of the MQTT brokers will not allow multiple connections with the same client id. ' +
                    'To connect to such brokers, your mqtt client id must be unique. ' +
                    'When platform is running in a micro-services mode, the copy of rule nodeis launched in each micro-service. ' +
                    'This will automatically lead to multiple mqtt clients with the same id and may cause failures of the rule node.',
                'device-id': 'Device ID',
                'device-id-required': 'Device ID is required.',
                'clean-session': 'Clean session',
                'enable-ssl': 'Enable SSL',
                credentials: 'Credentials',
                'credentials-type': 'Credentials type',
                'credentials-type-required': 'Credentials type is required.',
                'credentials-anonymous': 'Anonymous',
                'credentials-basic': 'Basic',
                'credentials-pem': 'PEM',
                'credentials-pem-hint': 'At least Server CA certificate file or a pair of Client certificate and Client private key files are required',
                'credentials-sas': 'Shared Access Signature',
                'sas-key': 'SAS Key',
                'sas-key-required': 'SAS Key is required.',
                hostname: 'Hostname',
                'hostname-required': 'Hostname is required.',
                'azure-ca-cert': 'CA certificate file',
                'username-required': 'Username is required.',
                'password-required': 'Password is required.',
                'ca-cert': 'Server CA certificate file *',
                'private-key': 'Client private key file *',
                cert: 'Client certificate file *',
                'no-file': 'No file selected.',
                'drop-file': 'Drop a file or click to select a file to upload.',
                'private-key-password': 'Private key password',
                'use-system-smtp-settings': 'Use system SMTP settings',
                'use-metadata-interval-patterns': 'Use interval patterns',
                'use-metadata-interval-patterns-hint': 'If selected, rule node use start and end interval patterns from message metadata or data ' +
                    'assuming that intervals are in the milliseconds.',
                'use-message-alarm-data': 'Use message alarm data',
                'use-dynamically-change-the-severity-of-alar': 'Use dynamically change the severity of alarm',
                'check-all-keys': 'Check that all selected keys are present',
                'check-all-keys-hint': 'If selected, checks that all specified keys are present in the message data and metadata.',
                'check-relation-to-specific-entity': 'Check relation to specific entity',
                'check-relation-hint': 'Checks existence of relation to specific entity or to any entity based on direction and relation type.',
                'delete-relation-to-specific-entity': 'Delete relation to specific entity',
                'delete-relation-hint': 'Deletes relation from the originator of the incoming message to the specified ' +
                    'entity or list of entities based on direction and type.',
                'remove-current-relations': 'Remove current relations',
                'remove-current-relations-hint': 'Removes current relations from the originator of the incoming message based on direction and type.',
                'change-originator-to-related-entity': 'Change originator to related entity',
                'change-originator-to-related-entity-hint': 'Used to process submitted message as a message from another entity.',
                'start-interval-pattern': 'Start interval pattern',
                'end-interval-pattern': 'End interval pattern',
                'start-interval-pattern-required': 'Start interval pattern is required',
                'end-interval-pattern-required': 'End interval pattern is required',
                'smtp-protocol': 'Protocol',
                'smtp-host': 'SMTP host',
                'smtp-host-required': 'SMTP host is required.',
                'smtp-port': 'SMTP port',
                'smtp-port-required': 'You must supply a smtp port.',
                'smtp-port-range': 'SMTP port should be in a range from 1 to 65535.',
                'timeout-msec': 'Timeout ms',
                'min-timeout-msec-message': 'Only 0 ms minimum value is allowed.',
                'enter-username': 'Enter username',
                'enter-password': 'Enter password',
                'enable-tls': 'Enable TLS',
                'tls-version': 'TLS version',
                'enable-proxy': 'Enable proxy',
                'use-system-proxy-properties': 'Use system proxy properties',
                'proxy-host': 'Proxy host',
                'proxy-host-required': 'Proxy host is required.',
                'proxy-port': 'Proxy port',
                'proxy-port-required': 'Proxy port is required.',
                'proxy-port-range': 'Proxy port should be in a range from 1 to 65535.',
                'proxy-user': 'Proxy user',
                'proxy-password': 'Proxy password',
                'proxy-scheme': 'Proxy scheme',
                'numbers-to-template': 'Phone Numbers To Template',
                'numbers-to-template-required': 'Phone Numbers To Template is required',
                'numbers-to-template-hint': 'Comma separated Phone Numbers, use <code><span style="color: #000;">$&#123;</span>' +
                    'metadataKey<span style="color: #000;">&#125;</span></code> for value from metadata, <code><span style="color: #000;">' +
                    '$[</span>messageKey<span style="color: #000;">]</span></code> for value from message body',
                'sms-message-template': 'SMS message Template',
                'sms-message-template-required': 'SMS message Template is required',
                'use-system-sms-settings': 'Use system SMS provider settings',
                'min-period-0-seconds-message': 'Only 0 second minimum period is allowed.',
                'max-pending-messages': 'Maximum pending messages',
                'max-pending-messages-required': 'Maximum pending messages is required.',
                'max-pending-messages-range': 'Maximum pending messages should be in a range from 1 to 100000.',
                'originator-types-filter': 'Originator types filter',
                'interval-seconds': 'Interval in seconds',
                'interval-seconds-required': 'Interval is required.',
                'min-interval-seconds-message': 'Only 1 second minimum interval is allowed.',
                'output-timeseries-key-prefix': 'Output timeseries key prefix',
                'output-timeseries-key-prefix-required': 'Output timeseries key prefix required.',
                'separator-hint': 'You should press "enter" to complete field input.',
                'entity-details': 'Select entity details:',
                'entity-details-title': 'Title',
                'entity-details-country': 'Country',
                'entity-details-state': 'State',
                'entity-details-zip': 'Zip',
                'entity-details-address': 'Address',
                'entity-details-address2': 'Address2',
                'entity-details-additional_info': 'Additional Info',
                'entity-details-phone': 'Phone',
                'entity-details-email': 'Email',
                'add-to-metadata': 'Add selected details to message metadata',
                'add-to-metadata-hint': 'If selected, adds the selected details keys to the message metadata instead of message data.',
                'entity-details-list-empty': 'No entity details selected.',
                'no-entity-details-matching': 'No entity details matching were found.',
                'custom-table-name': 'Custom table name',
                'custom-table-name-required': 'Table Name is required',
                'custom-table-hint': 'You should enter the table name without prefix \'cs_tb_\'.',
                'message-field': 'Message field',
                'message-field-required': 'Message field is required.',
                'table-col': 'Table column',
                'table-col-required': 'Table column is required.',
                'latitude-key-name': 'Latitude key name',
                'longitude-key-name': 'Longitude key name',
                'latitude-key-name-required': 'Latitude key name is required.',
                'longitude-key-name-required': 'Longitude key name is required.',
                'fetch-perimeter-info-from-message-metadata': 'Fetch perimeter information from message metadata',
                'perimeter-circle': 'Circle',
                'perimeter-polygon': 'Polygon',
                'perimeter-type': 'Perimeter type',
                'circle-center-latitude': 'Center latitude',
                'circle-center-latitude-required': 'Center latitude is required.',
                'circle-center-longitude': 'Center longitude',
                'circle-center-longitude-required': 'Center longitude is required.',
                'range-unit-meter': 'Meter',
                'range-unit-kilometer': 'Kilometer',
                'range-unit-foot': 'Foot',
                'range-unit-mile': 'Mile',
                'range-unit-nautical-mile': 'Nautical mile',
                'range-units': 'Range units',
                range: 'Range',
                'range-required': 'Range is required.',
                'polygon-definition': 'Polygon definition',
                'polygon-definition-required': 'Polygon definition is required.',
                'polygon-definition-hint': 'Please, use the following format for manual definition of polygon: [[lat1,lon1],[lat2,lon2], ... ,[latN,lonN]].',
                'min-inside-duration': 'Minimal inside duration',
                'min-inside-duration-value-required': 'Minimal inside duration is required',
                'min-inside-duration-time-unit': 'Minimal inside duration time unit',
                'min-outside-duration': 'Minimal outside duration',
                'min-outside-duration-value-required': 'Minimal outside duration is required',
                'min-outside-duration-time-unit': 'Minimal outside duration time unit',
                'tell-failure-if-absent': 'Tell Failure',
                'tell-failure-if-absent-hint': 'If at least one selected key doesn\'t exist the outbound message will report "Failure".',
                'get-latest-value-with-ts': 'Fetch Latest telemetry with Timestamp',
                'get-latest-value-with-ts-hint': 'If selected, latest telemetry values will be added to the outbound message metadata with timestamp, ' +
                    'e.g: "temp": "&#123;"ts":1574329385897, "value":42&#125;"',
                'use-redis-queue': 'Use redis queue for message persistence',
                'trim-redis-queue': 'Trim redis queue',
                'redis-queue-max-size': 'Redis queue max size',
                'add-metadata-key-values-as-kafka-headers': 'Add Message metadata key-value pairs to Kafka record headers',
                'add-metadata-key-values-as-kafka-headers-hint': 'If selected, key-value pairs from message metadata will be added to the outgoing records headers as byte arrays with predefined charset encoding.',
                'charset-encoding': 'Charset encoding',
                'charset-encoding-required': 'Charset encoding is required.',
                'charset-us-ascii': 'US-ASCII',
                'charset-iso-8859-1': 'ISO-8859-1',
                'charset-utf-8': 'UTF-8',
                'charset-utf-16be': 'UTF-16BE',
                'charset-utf-16le': 'UTF-16LE',
                'charset-utf-16': 'UTF-16',
                'select-queue-hint': 'The queue name can be selected from a drop-down list or add a custom name.',
                'persist-alarm-rules': 'Persist state of alarm rules',
                'fetch-alarm-rules': 'Fetch state of alarm rules',
                'input-value-key': 'Input value key',
                'input-value-key-required': 'Input value key is required.',
                'output-value-key': 'Output value key',
                'output-value-key-required': 'Output value key is required.',
                round: 'Decimals',
                'round-range': 'Decimals should be in a range from 0 to 15.',
                'use-cache': 'Use cache for latest value',
                'tell-failure-if-delta-is-negative': 'Tell Failure if delta is negative',
                'add-period-between-msgs': 'Add period between messages',
                'period-value-key': 'Period value key',
                'period-key-required': 'Period value key is required.',
                'general-pattern-hint': 'Hint: use <code><span style="color: #000;">$&#123;</span>metadataKey<span style="color: #000;">&#125;</span></code> ' +
                    'for value from metadata, <code><span style="color: #000;">$[</span>messageKey<span style="color: #000;">]</span></code> ' +
                    'for value from message body',
                'alarm-severity-pattern-hint': 'Hint: use <code><span style="color: #000;">$&#123;</span>metadataKey<span style="color: #000;">&#125;</span></code> ' +
                    'for value from metadata, <code><span style="color: #000;">$[</span>messageKey<span style="color: #000;">]</span></code> ' +
                    'for value from message body. Alarm severity should be system (CRITICAL, MAJOR etc.)',
                'output-node-name-hint': 'The <b>rule node name</b> corresponds to the <b>relation type</b> of the output message, and it is used to forward messages to other rule nodes in the caller rule chain.'
            },
            'key-val': {
                key: 'Key',
                value: 'Value',
                'remove-entry': 'Remove entry',
                'add-entry': 'Add entry'
            },
            'mail-body-type': {
                'plain-text': 'Plain Text',
                html: 'HTML',
                dynamic: 'Dynamic'
            }
        }
    };
    translate.setTranslation('en_US', enUS, true);
}

class RuleNodeCoreConfigModule {
    constructor(translate) {
        addRuleNodeCoreLocaleEnglish(translate);
    }
}
RuleNodeCoreConfigModule.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigModule, deps: [{ token: i4.TranslateService }], target: i0.ɵɵFactoryTarget.NgModule });
RuleNodeCoreConfigModule.ɵmod = i0.ɵɵngDeclareNgModule({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigModule, declarations: [EmptyConfigComponent], imports: [CommonModule,
        SharedModule], exports: [RuleNodeCoreConfigActionModule,
        RuleNodeCoreConfigFilterModule,
        RulenodeCoreConfigEnrichmentModule,
        RulenodeCoreConfigTransformModule,
        RuleNodeCoreConfigFlowModule,
        EmptyConfigComponent] });
RuleNodeCoreConfigModule.ɵinj = i0.ɵɵngDeclareInjector({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigModule, imports: [[
            CommonModule,
            SharedModule
        ], RuleNodeCoreConfigActionModule,
        RuleNodeCoreConfigFilterModule,
        RulenodeCoreConfigEnrichmentModule,
        RulenodeCoreConfigTransformModule,
        RuleNodeCoreConfigFlowModule] });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "12.2.14", ngImport: i0, type: RuleNodeCoreConfigModule, decorators: [{
            type: NgModule,
            args: [{
                    declarations: [
                        EmptyConfigComponent
                    ],
                    imports: [
                        CommonModule,
                        SharedModule
                    ],
                    exports: [
                        RuleNodeCoreConfigActionModule,
                        RuleNodeCoreConfigFilterModule,
                        RulenodeCoreConfigEnrichmentModule,
                        RulenodeCoreConfigTransformModule,
                        RuleNodeCoreConfigFlowModule,
                        EmptyConfigComponent
                    ]
                }]
        }], ctorParameters: function () { return [{ type: i4.TranslateService }]; } });

/*
 * Public API Surface of rule-core-config
 */

/**
 * Generated bundle index. Do not edit.
 */

export { AssignCustomerConfigComponent, AttributesConfigComponent, AzureIotHubConfigComponent, CalculateDeltaConfigComponent, ChangeOriginatorConfigComponent, CheckAlarmStatusComponent, CheckMessageConfigComponent, CheckPointConfigComponent, CheckRelationConfigComponent, ClearAlarmConfigComponent, CreateAlarmConfigComponent, CreateRelationConfigComponent, CredentialsConfigComponent, CustomerAttributesConfigComponent, DeleteRelationConfigComponent, DeviceAttributesConfigComponent, DeviceProfileConfigComponent, DeviceRelationsQueryConfigComponent, EmptyConfigComponent, EntityDetailsConfigComponent, GeneratorConfigComponent, GetTelemetryFromDatabaseConfigComponent, GpsGeoActionConfigComponent, GpsGeoFilterConfigComponent, KafkaConfigComponent, KvMapConfigComponent, LogConfigComponent, MessageTypeConfigComponent, MessageTypesConfigComponent, MqttConfigComponent, MsgCountConfigComponent, MsgDelayConfigComponent, OriginatorAttributesConfigComponent, OriginatorFieldsConfigComponent, OriginatorTypeConfigComponent, PubSubConfigComponent, PushToCloudConfigComponent, PushToEdgeConfigComponent, RabbitMqConfigComponent, RelatedAttributesConfigComponent, RelationsQueryConfigComponent, RestApiCallConfigComponent, RpcReplyConfigComponent, RpcRequestConfigComponent, RuleChainInputComponent, RuleChainOutputComponent, RuleNodeCoreConfigActionModule, RuleNodeCoreConfigFilterModule, RuleNodeCoreConfigFlowModule, RuleNodeCoreConfigModule, RulenodeCoreConfigCommonModule, RulenodeCoreConfigEnrichmentModule, RulenodeCoreConfigTransformModule, SafeHtmlPipe, SaveToCustomTableConfigComponent, ScriptConfigComponent, SendEmailConfigComponent, SendSmsConfigComponent, SnsConfigComponent, SqsConfigComponent, SwitchConfigComponent, TenantAttributesConfigComponent, TimeseriesConfigComponent, ToEmailConfigComponent, TransformScriptConfigComponent, UnassignCustomerConfigComponent };
//# sourceMappingURL=rulenode-core-config.js.map
