// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, ValidatorFn, Validators } from '@angular/forms';
import { ContactBased } from '@shared/models/contact-based.model';
import { AfterViewInit, ChangeDetectorRef, DestroyRef, Directive, inject } from '@angular/core';
import { HasId } from '@shared/models/base-data';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { CountryData } from '@shared/models/country.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { validateEmail } from '@app/core/utils';

@Directive()
export abstract class GroupContactBasedComponent<T extends ContactBased<HasId>> extends GroupEntityComponent<T> implements AfterViewInit {

  protected destroyRef = inject(DestroyRef);

  protected constructor(protected store: Store<AppState>,
                        protected fb: UntypedFormBuilder,
                        protected entityValue: T,
                        protected entitiesTableConfigValue: EntityTableConfig<T> | GroupEntityTableConfig<T>,
                        protected cd: ChangeDetectorRef,
                        protected countryData: CountryData,
                        protected userPermissionsService: UserPermissionsService) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd, userPermissionsService);
  }

  buildForm(entity: T): UntypedFormGroup {
    const entityForm = this.buildEntityForm(entity);
    entityForm.addControl('country', this.fb.control(entity ? entity.country : '', []));
    entityForm.addControl('city', this.fb.control(entity ? entity.city : '', []));
    entityForm.addControl('state', this.fb.control(entity ? entity.state : '', []));
    entityForm.addControl('zip', this.fb.control(entity ? entity.zip : '',
      this.zipValidators(entity ? entity.country : '')
    ));
    entityForm.addControl('address', this.fb.control(entity ? entity.address : '', []));
    entityForm.addControl('address2', this.fb.control(entity ? entity.address2 : '', []));
    entityForm.addControl('phone', this.fb.control(entity ? entity.phone : '', []));
    entityForm.addControl('email', this.fb.control(entity ? entity.email : '', [validateEmail]));
    return entityForm;
  }

  updateForm(entity: T) {
    this.updateEntityForm(entity);
    this.entityForm.patchValue({country: entity.country});
    this.entityForm.patchValue({city: entity.city});
    this.entityForm.patchValue({state: entity.state});
    this.entityForm.get('zip').setValidators(this.zipValidators(entity.country));
    this.entityForm.patchValue({zip: entity.zip});
    this.entityForm.patchValue({address: entity.address});
    this.entityForm.patchValue({address2: entity.address2});
    this.entityForm.patchValue({phone: entity.phone});
    this.entityForm.patchValue({email: entity.email});
  }

  ngAfterViewInit() {
    this.entityForm.get('country').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      (country) => {
        this.entityForm.get('zip').setValidators(this.zipValidators(country));
        this.entityForm.get('zip').updateValueAndValidity({onlySelf: true});
        this.entityForm.get('zip').markAsTouched({onlySelf: true});
      }
    );
  }

  zipValidators(country: string): ValidatorFn[] {
    const zipValidators = [];
    if (country) {
      const postCodePattern = this.countryData.allCountries.find(item => item.name === country)?.postCodePattern;
      if (postCodePattern) {
        zipValidators.push(Validators.pattern(postCodePattern));
      }
    }
    return zipValidators;
  }

  abstract buildEntityForm(entity: T): UntypedFormGroup;

  abstract updateEntityForm(entity: T);

}
