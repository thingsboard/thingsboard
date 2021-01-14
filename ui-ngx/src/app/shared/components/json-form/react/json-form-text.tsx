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
import ThingsboardBaseComponent from './json-form-base-component';
import TextField from '@material-ui/core/TextField';
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';

interface ThingsboardTextState extends JsonFormFieldState {
  focused: boolean;
}

class ThingsboardText extends React.Component<JsonFormFieldProps, ThingsboardTextState> {

  constructor(props) {
    super(props);
    this.onBlur = this.onBlur.bind(this);
    this.onFocus = this.onFocus.bind(this);
    this.state = {
      focused: false
    };
  }

  onBlur() {
    this.setState({focused: false});
  }

  onFocus() {
    this.setState({focused: true});
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

    const multiline = this.props.form.type === 'textarea';
    let rows = 1;
    let rowsMax = 1;
    let minHeight = 48;
    if (multiline) {
      rows = this.props.form.rows || 2;
      rowsMax = this.props.form.rowsMax;
      minHeight = 19 * rows + 48;
    }

    return (
      <div>
        <TextField
          className={fieldClass}
          type={this.props.form.type}
          label={this.props.form.title}
          multiline={multiline}
          error={!this.props.valid}
          helperText={this.props.valid ? this.props.form.placeholder : this.props.error}
          onChange={(e) => {
            this.props.onChangeValidate(e);
          }}
          defaultValue={this.props.value}
          disabled={this.props.form.readonly}
          rows={rows}
          rowsMax={rowsMax}
          onFocus={this.onFocus}
          onBlur={this.onBlur}
          style={this.props.form.style || {width: '100%', minHeight: minHeight + 'px'}}/>
      </div>
    );
  }
}

export default ThingsboardBaseComponent(ThingsboardText);
