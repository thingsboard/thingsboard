/*
 * Copyright © 2016-2021 The Thingsboard Authors
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
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import { FormLabel, Radio, RadioGroup } from '@material-ui/core';
import FormControl from '@material-ui/core/FormControl';
import ThingsboardBaseComponent from '@shared/components/json-form/react/json-form-base-component';

class ThingsboardRadios extends React.Component<JsonFormFieldProps, JsonFormFieldState> {
  render() {
    const items = this.props.form.titleMap.map((item, index) => {
      return (
          <FormControlLabel value={item.value} control={<Radio />} label={item.name} key={index} />
      );
    });

    return (
      <FormControl component='fieldset'
                   className={this.props.form.htmlClass}
                   disabled={this.props.form.readonly}>
        <FormLabel component='legend'>{this.props.form.title}</FormLabel>
        <RadioGroup name={this.props.form.title} value={this.props.value} onChange={(e) => {
          this.props.onChangeValidate(e);
        }}>
          {items}
        </RadioGroup>
      </FormControl>
    );
  }
}

export default ThingsboardBaseComponent(ThingsboardRadios);
