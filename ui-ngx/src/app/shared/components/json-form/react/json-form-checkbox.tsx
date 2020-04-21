/*
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import * as React from 'react';
import ThingsboardBaseComponent from './json-form-base-component';
import Checkbox from '@material-ui/core/Checkbox';
import { JsonFormFieldProps, JsonFormFieldState } from './json-form.models.js';
import FormControlLabel from '@material-ui/core/FormControlLabel';

class ThingsboardCheckbox extends React.Component<JsonFormFieldProps, JsonFormFieldState> {
    render() {
        return (
          <div>
          <FormControlLabel
            control={
              <Checkbox
                name={this.props.form.key.slice(-1)[0] + ''}
                value={this.props.form.key.slice(-1)[0]}
                checked={this.props.value || false}
                disabled={this.props.form.readonly}
                onChange={(e, checked) => {
                  this.props.onChangeValidate(e);
                }}
              />
            }
            label={this.props.form.title}
            />
          </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardCheckbox);
