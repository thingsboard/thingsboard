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
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';
import MenuItem from '@material-ui/core/MenuItem';
import FormControl from '@material-ui/core/FormControl';
import InputLabel from '@material-ui/core/InputLabel';
import Select from '@material-ui/core/Select';
import ThingsboardBaseComponent from '@shared/components/json-form/react/json-form-base-component';

interface ThingsboardSelectState extends JsonFormFieldState {
  currentValue: any;
}

class ThingsboardSelect extends React.Component<JsonFormFieldProps, ThingsboardSelectState> {

  constructor(props) {
    super(props);
    this.onSelected = this.onSelected.bind(this);
    const possibleValue = this.getModelKey(this.props.model, this.props.form.key);
    this.state = {
      currentValue: this.props.model !== undefined && possibleValue ? possibleValue : this.props.form.titleMap != null ?
        this.props.form.titleMap[0].value : ''
    };
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.model && nextProps.form.key) {
      this.setState({
        currentValue: this.getModelKey(nextProps.model, nextProps.form.key)
          || (nextProps.form.titleMap != null ? nextProps.form.titleMap[0].value : '')
      });
    }
  }

  getModelKey(model, key) {
    if (Array.isArray(key)) {
      return key.reduce((cur, nxt) => (cur[nxt] || {}), model);
    } else {
      return model[key];
    }
  }

  onSelected(event: React.ChangeEvent<{ name?: string; value: any }>) {

    this.setState({
      currentValue: event.target.value
    });
    this.props.onChangeValidate(event);
  }

  render() {
    const menuItems = this.props.form.titleMap.map((item, idx) => (
      <MenuItem key={idx}
                value={item.value}>{item.name}</MenuItem>
    ));

    return (
      <FormControl className={this.props.form.htmlClass}
                   disabled={this.props.form.readonly}
                   fullWidth={true}>
        <InputLabel htmlFor='select-field'>{this.props.form.title}</InputLabel>
        <Select
          value={this.state.currentValue}
          onChange={this.onSelected}>
          {menuItems}
        </Select>
      </FormControl>
    );
  }
}

export default ThingsboardBaseComponent(ThingsboardSelect);
