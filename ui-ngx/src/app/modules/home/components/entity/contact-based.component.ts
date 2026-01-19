///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, ValidatorFn, Validators } from '@angular/forms';
import { ContactBased } from '@shared/models/contact-based.model';
import { AfterViewInit, ChangeDetectorRef, DestroyRef, Directive, inject } from '@angular/core';
import { HasId } from '@shared/models/base-data';
import { EntityComponent } from './entity.component';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { CountryData } from '@shared/models/country.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { validateEmail } from '@app/core/utils';

@Directive()
export abstract class ContactBasedComponent<T extends ContactBased<HasId>> extends EntityComponent<T> implements AfterViewInit {

  protected destroyRef = inject(DestroyRef);

  protected constructor(protected store: Store<AppState>,
                        protected fb: UntypedFormBuilder,
                        protected entityValue: T,
                        protected entitiesTableConfigValue: EntityTableConfig<T>,
                        protected cd: ChangeDetectorRef,
                        protected countryData: CountryData) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  buildForm(entity: T): UntypedFormGroup {
    const entityForm = this.buildEntityForm(entity);
    entityForm.addControl('country', this.fb.control(entity ? entity.country : '', [Validators.maxLength(255)]));
    entityForm.addControl('city', this.fb.control(entity ? entity.city : '', [Validators.maxLength(255)]));
    entityForm.addControl('state', this.fb.control(entity ? entity.state : '', [Validators.maxLength(255)]));
    entityForm.addControl('zip', this.fb.control(entity ? entity.zip : '',
      this.zipValidators(entity ? entity.country : '')
    ));
    entityForm.addControl('address', this.fb.control(entity ? entity.address : '', []));
    entityForm.addControl('address2', this.fb.control(entity ? entity.address2 : '', []));
    entityForm.addControl('phone', this.fb.control(entity ? entity.phone : '', [Validators.maxLength(255)]));
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
