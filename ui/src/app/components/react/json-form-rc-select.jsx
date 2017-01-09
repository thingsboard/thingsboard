/*
 * Copyright © 2016-2017 The Thingsboard Authors
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
import 'rc-select/assets/index.css';

import React from 'react';
import ThingsboardBaseComponent from './json-form-base-component.jsx';
import Select, {Option} from 'rc-select';

class ThingsboardRcSelect extends React.Component {

    constructor(props) {
        super(props);
        this.onSelect = this.onSelect.bind(this);
        this.onDeselect = this.onDeselect.bind(this);
        this.onBlur = this.onBlur.bind(this);
        this.onFocus = this.onFocus.bind(this);
        let emptyValue = this.props.form.schema.type === 'array'? [] : null;
        this.state = {
            currentValue: this.props.value || emptyValue,
            items: this.props.form.items,
            focused: false
        };
    }

    onSelect(value, option) {
        if(this.props.form.schema.type === 'array') {
            let v = this.state.currentValue;
            v.push(value);
            this.setState({
                currentValue: v
            });
            this.props.onChangeValidate(v);
        } else {
            this.setState({currentValue: value});
            this.props.onChangeValidate({target: {value: value}});
        }
    }

    onDeselect(value, option) {
        if (this.props.form.schema.type === 'array') {
            let v = this.state.currentValue;
            let index = v.indexOf(value);
            if (index > -1) {
                v.splice(index, 1);
            }
            this.setState({
                currentValue: v
            });
            this.props.onChangeValidate(v);
        }
    }

    onBlur() {
        this.setState({ focused: false })
    }

    onFocus() {
        this.setState({ focused: true })
    }

    render() {
        let options = [];
        if(this.state.items && this.state.items.length > 0) {
            options = this.state.items.map((item, idx) => (
                <Option key={idx} value={item.value}>{item.label}</Option>
            ));
        }

        var labelClass = "tb-label";
        if (this.props.form.required) {
            labelClass += " tb-required";
        }
        if (this.props.form.readonly) {
            labelClass += " tb-readonly";
        }
        if (this.state.focused) {
            labelClass += " tb-focused";
        }

        return (
            <div className="tb-container">
                <label className={labelClass}>{this.props.form.title}</label>
                <Select
                    className={this.props.form.className}
                    dropdownClassName={this.props.form.dropdownClassName}
                    dropdownStyle={this.props.form.dropdownStyle}
                    dropdownMenuStyle={this.props.form.dropdownMenuStyle}
                    allowClear={this.props.form.allowClear}
                    tags={this.props.form.tags}
                    maxTagTextLength={this.props.form.maxTagTextLength}
                    multiple={this.props.form.multiple}
                    combobox={this.props.form.combobox}
                    disabled={this.props.form.readonly}
                    value={this.state.currentValue}
                    onSelect={this.onSelect}
                    onDeselect={this.onDeselect}
                    onFocus={this.onFocus}
                    onBlur={this.onBlur}
                    style={this.props.form.style || {width: "100%"}}>
                    {options}
                </Select>
                <div className="json-form-error"
                     style={{opacity: this.props.valid ? '0' : '1',
                             bottom: '-5px'}}>{this.props.error}</div>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardRcSelect);
