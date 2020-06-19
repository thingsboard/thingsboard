///
/// Copyright © 2016-2020 The Thingsboard Authors
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
import { FormBuilder, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { ContactBased } from '@shared/models/contact-based.model';
import { AfterViewInit } from '@angular/core';
import { POSTAL_CODE_PATTERNS } from '@home/models/contact.models';
import { HasId } from '@shared/models/base-data';
import { EntityComponent } from './entity.component';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

export abstract class ContactBasedComponent<T extends ContactBased<HasId>> extends EntityComponent<T> implements AfterViewInit {

  protected constructor(protected store: Store<AppState>,
                        protected fb: FormBuilder,
                        protected entityValue: T,
                        protected entitiesTableConfigValue: EntityTableConfig<T>) {
    super(store, fb, entityValue, entitiesTableConfigValue);
  }

  buildForm(entity: T): FormGroup {
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
    entityForm.addControl('email', this.fb.control(entity ? entity.email : '', [Validators.email]));
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
    this.entityForm.get('country').valueChanges.subscribe(
      (country) => {
        this.entityForm.get('zip').setValidators(this.zipValidators(country));
        this.entityForm.get('zip').updateValueAndValidity({onlySelf: true});
        this.entityForm.get('zip').markAsTouched({onlySelf: true});
      }
    );
  }

  zipValidators(country: string): ValidatorFn[] {
    const zipValidators = [];
    if (country && POSTAL_CODE_PATTERNS[country]) {
      const postalCodePattern = POSTAL_CODE_PATTERNS[country];
      zipValidators.push(Validators.pattern(postalCodePattern));
    }
    return zipValidators;
  }

  abstract buildEntityForm(entity: T): FormGroup;

  abstract updateEntityForm(entity: T);

}
