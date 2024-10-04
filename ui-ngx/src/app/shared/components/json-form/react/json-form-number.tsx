/*
 * Copyright © 2016-2024 The Thingsboard Authors
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
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';
import { TextField } from '@mui/material';
import { ChangeEvent } from 'react';

interface ThingsboardNumberState extends JsonFormFieldState {
  focused: boolean;
  lastSuccessfulValue: number;
}

class ThingsboardNumber extends React.Component<JsonFormFieldProps, ThingsboardNumberState> {

  constructor(props: JsonFormFieldProps) {
    super(props);
    this.preValidationCheck = this.preValidationCheck.bind(this);
    this.onBlur = this.onBlur.bind(this);
    this.onFocus = this.onFocus.bind(this);
    this.state = {
      lastSuccessfulValue: this.props.value,
      focused: false
    };
  }

  isNumeric(n: any) {
    return n === null || n === '' || !isNaN(n) && isFinite(n);
  }

  onBlur() {
    this.setState({focused: false});
  }

  onFocus() {
    this.setState({focused: true});
  }

  preValidationCheck(e: ChangeEvent<HTMLTextAreaElement | HTMLInputElement>) {
    if (this.isNumeric(e.target.value)) {
      this.setState({
        lastSuccessfulValue: Number(e.target.value)
      });
      this.props.onChangeValidate(e);
    }
  }

  render() {

    let fieldClass = 'tb-field';
    if (this.props.form.required) {
      fieldClass += ' tb-required';
    }
    if (this.props.form.readonly) {
      fieldClass += ' tb-readonly';
    }
    if (this.state.focused) {
      fieldClass += ' tb-focused';
    }
    let value = this.state.lastSuccessfulValue;
    if (typeof value !== 'undefined') {
      value = Number(value);
    } else {
      value = null;
    }
    return (
      <div>
        <TextField
          variant={'standard'}
          className={fieldClass}
          label={this.props.form.title}
          type='number'
          error={!this.props.valid}
          helperText={this.props.valid ? this.props.form.placeholder : this.props.error}
          onChange={this.preValidationCheck}
          defaultValue={value}
          disabled={this.props.form.readonly}
          onFocus={this.onFocus}
          onBlur={this.onBlur}
          style={this.props.form.style || {width: '100%'}}/>
      </div>
    );
  }
}

export default ThingsboardBaseComponent(ThingsboardNumber);
