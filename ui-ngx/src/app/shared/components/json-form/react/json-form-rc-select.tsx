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
import Select, { Option } from 'rc-select';
import {
  JsonFormFieldProps,
  JsonFormFieldState,
  KeyLabelItem
} from '@shared/components/json-form/react/json-form.models';
import { Mode } from 'rc-select/lib/interface';

interface ThingsboardRcSelectState extends JsonFormFieldState {
  currentValue: KeyLabelItem | KeyLabelItem[];
  items: Array<KeyLabelItem>;
  focused: boolean;
}

class ThingsboardRcSelect extends React.Component<JsonFormFieldProps, ThingsboardRcSelectState> {

    constructor(props) {
        super(props);
        this.onSelect = this.onSelect.bind(this);
        this.onDeselect = this.onDeselect.bind(this);
        this.onBlur = this.onBlur.bind(this);
        this.onFocus = this.onFocus.bind(this);
        this.state = {
            currentValue: this.keyToCurrentValue(this.props.value, this.props.form.schema.type === 'array'),
            items: this.props.form.items as KeyLabelItem[],
            focused: false
        };
    }

    keyToCurrentValue(key: string | string[], isArray: boolean): KeyLabelItem | KeyLabelItem[] {
        let currentValue: KeyLabelItem | KeyLabelItem[] = isArray ? [] : null;
        if (isArray) {
            const keys = key;
            if (keys) {
              (keys as string[]).forEach((keyVal) => {
                (currentValue as KeyLabelItem[]).push({key: keyVal, label: this.labelFromKey(keyVal)});
              });
            }
        } else {
            currentValue = {key: key as string, label: this.labelFromKey(key as string)};
        }
        return currentValue;
    }

    labelFromKey(key: string): string {
        let label = key || '';
        if (key) {
          for (const item of this.props.form.items) {
            if (item.value === key) {
              label = item.label;
              break;
            }
          }
        }
        return label;
    }

    arrayValues(items: KeyLabelItem[]): string[] {
        const v: string[] = [];
        if (items) {
          items.forEach(item => {
            v.push(item.key);
          });
        }
        return v;
    }

    keyIndex(values: KeyLabelItem[], key: string): number {
        let index = -1;
        if (values) {
            for (let i = 0; i < values.length; i++) {
                if (values[i].key === key) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    onSelect(value: KeyLabelItem, option) {
        if (this.props.form.schema.type === 'array') {
            const v = this.state.currentValue as KeyLabelItem[];
            v.push(this.keyToCurrentValue(value.key, false) as KeyLabelItem);
            this.setState({
                currentValue: v
            });
            this.props.onChangeValidate(this.arrayValues(v));
        } else {
            this.setState({currentValue: this.keyToCurrentValue(value.key, false)});
            this.props.onChangeValidate({target: {value: value.key}});
        }
    }

    onDeselect(value: KeyLabelItem, option) {
        if (this.props.form.schema.type === 'array') {
            const v = this.state.currentValue as KeyLabelItem[];
            const index = this.keyIndex(v, value.key);
            if (index > -1) {
                v.splice(index, 1);
            }
            this.setState({
                currentValue: v
            });
            this.props.onChangeValidate(this.arrayValues(v));
        }
    }

    onBlur() {
        this.setState({ focused: false });
    }

    onFocus() {
        this.setState({ focused: true });
    }

    render() {

        let options: JSX.Element[] = [];
        if (this.state.items && this.state.items.length > 0) {
            options = this.state.items.map((item, idx) => (
              <Option key={idx} value={item.value}>{item.label}</Option>
            ));
        }

        let labelClass = 'tb-label';
        if (this.props.form.required) {
            labelClass += ' tb-required';
        }
        if (this.props.form.readonly) {
            labelClass += ' tb-readonly';
        }
        if (this.state.focused) {
            labelClass += ' tb-focused';
        }
        let mode: Mode;
        if (this.props.form.tags) {
          mode = 'tags';
        } else if (this.props.form.multiple) {
          mode = 'multiple';
        }

        const dropdownStyle = {...this.props.form.dropdownStyle, ...{zIndex: 100001}};
        let dropdownClassName = 'tb-rc-select-dropdown';
        if (this.props.form.dropdownClassName) {
          dropdownClassName += ' ' + this.props.form.dropdownClassName;
        }

        return (
            <div className='tb-container'>
                <label className={labelClass}>{this.props.form.title}</label>
                <Select
                    className={this.props.form.className}
                    dropdownClassName={dropdownClassName}
                    dropdownStyle={dropdownStyle}
                    allowClear={this.props.form.allowClear}
                    showSearch={true}
                    mode={mode}
                    maxTagTextLength={this.props.form.maxTagTextLength}
                    disabled={this.props.form.readonly}
                    optionLabelProp='children'
                    value={this.state.currentValue}
                    labelInValue={true}
                    onSelect={this.onSelect}
                    onDeselect={this.onDeselect}
                    onFocus={this.onFocus}
                    onBlur={this.onBlur}
                    style={this.props.form.style || {width: '100%'}}>
                    {options}
                </Select>
                <div className='json-form-error'
                     style={{opacity: this.props.valid ? '0' : '1',
                             bottom: '-5px'}}>{this.props.error}</div>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardRcSelect);
