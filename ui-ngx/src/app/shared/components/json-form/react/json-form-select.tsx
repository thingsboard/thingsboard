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
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Select, { SelectChangeEvent } from '@mui/material/Select';
import ThingsboardBaseComponent from '@shared/components/json-form/react/json-form-base-component';
import { isObject } from '@core/utils';

interface ThingsboardSelectState extends JsonFormFieldState {
  currentValue: any;
}

class ThingsboardSelect extends React.Component<JsonFormFieldProps, ThingsboardSelectState> {

  static getDerivedStateFromProps(props: JsonFormFieldProps) {
    if (props.model && props.form.key) {
      return {
        currentValue: ThingsboardSelect.getModelKey(props.model, props.form.key)
          || (props.form.titleMap != null ? props.form.titleMap[0].value : '')
      }
    }
  }

  static getModelKey(model: any, key: (string | number)[]) {
    if (Array.isArray(key)) {
      const res = key.reduce((cur, nxt) => (cur[nxt] || {}), model);
      if (res && isObject(res)) {
        return undefined;
      } else {
        return res;
      }
    } else {
      return model[key];
    }
  }

  constructor(props: JsonFormFieldProps) {
    super(props);
    this.onSelected = this.onSelected.bind(this);
    const possibleValue = ThingsboardSelect.getModelKey(this.props.model, this.props.form.key);
    this.state = {
      currentValue: this.props.model !== undefined && possibleValue ? possibleValue : this.props.form.titleMap != null ?
        this.props.form.titleMap[0].value : ''
    };
  }

  onSelected(event: SelectChangeEvent<any>) {

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
        <InputLabel variant={'standard'} htmlFor='select-field'>{this.props.form.title}</InputLabel>
        <Select
          variant={'standard'}
          value={this.state.currentValue}
          onChange={this.onSelected}>
          {menuItems}
        </Select>
      </FormControl>
    );
  }
}

export default ThingsboardBaseComponent(ThingsboardSelect);
