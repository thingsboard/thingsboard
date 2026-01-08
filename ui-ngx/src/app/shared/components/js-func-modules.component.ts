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

import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, ValidatorFn } from '@angular/forms';
import { JsFuncModuleRow, moduleValid } from '@shared/components/js-func-module-row.component';

const modulesValidator: ValidatorFn = control => {
  const modulesArray = control.get('modules') as UntypedFormArray;
  const notUniqueControls =
    modulesArray.controls.filter(moduleControl => moduleControl.hasError('moduleAliasNotUnique'));
  if (notUniqueControls.length) {
    return {
      moduleAliasNotUnique: true
    };
  }
  let valid = !modulesArray.controls.some(c => !c.valid);
  valid = valid && control.valid;
  return valid ? null : {
    modules: {
      valid: false,
    },
  };
};

@Component({
  selector: 'tb-js-func-modules',
  templateUrl: './js-func-modules.component.html',
  styleUrls: ['./js-func-modules.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class JsFuncModulesComponent implements OnInit {

  @Input()
  modules: {[alias: string]: string };

  @Input()
  popover: TbPopoverComponent<JsFuncModulesComponent>;

  @Output()
  modulesApplied = new EventEmitter<{[alias: string]: string }>();

  modulesFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    const modulesControls: Array<AbstractControl> = [];
    if (this.modules && Object.keys(this.modules).length) {
      Object.keys(this.modules).forEach((alias) => {
        const moduleRow: JsFuncModuleRow = {
          alias,
          moduleLink: this.modules[alias]
        };
        modulesControls.push(this.fb.control(moduleRow, []));
      });
   }
    this.modulesFormGroup = this.fb.group({
      modules: this.fb.array(modulesControls)
    }, {validators: modulesValidator});
  }

  cancel() {
    this.popover?.hide();
  }

  applyModules() {
    let moduleRows: JsFuncModuleRow[] = this.modulesFormGroup.get('modules').value;
    if (moduleRows) {
      moduleRows = moduleRows.filter(m => moduleValid(m));
    }
    if (moduleRows?.length) {
      const modules: {[alias: string]: string } = {};
      moduleRows.forEach(row => {
        modules[row.alias] = row.moduleLink;
      });
      this.modulesApplied.emit(modules);
    } else {
      this.modulesApplied.emit(null);
    }
  }

  public moduleAliasUnique(alias: string, index: number): boolean {
    const modulesArray = this.modulesFormGroup.get('modules') as UntypedFormArray;
    for (let i = 0; i < modulesArray.controls.length; i++) {
      if (i !== index) {
        const otherControl = modulesArray.controls[i];
        if (alias === otherControl.value.alias) {
          return false;
        }
      }
    }
    return true;
  }

  modulesFormArray(): UntypedFormArray {
    return this.modulesFormGroup.get('modules') as UntypedFormArray;
  }

  trackByModule(_index: number, moduleControl: AbstractControl): any {
    return moduleControl;
  }

  removeModule(index: number, emitEvent = true) {
    (this.modulesFormGroup.get('modules') as UntypedFormArray).removeAt(index, {emitEvent});
    this.modulesFormGroup.get('modules').markAsDirty({emitEvent});
  }

  addModule() {
    const moduleRow: JsFuncModuleRow = {
      alias: '',
      moduleLink: ''
    };
    const modulesArray = this.modulesFormGroup.get('modules') as UntypedFormArray;
    const moduleControl = this.fb.control(moduleRow, []);
    modulesArray.push(moduleControl);
    this.cd.detectChanges();
  }

}
