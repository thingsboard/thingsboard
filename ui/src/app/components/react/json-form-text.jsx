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
import React from 'react';
import ThingsboardBaseComponent from './json-form-base-component.jsx';
import TextField from 'material-ui/TextField';

class ThingsboardText extends React.Component {

    constructor(props) {
        super(props);
        this.onBlur = this.onBlur.bind(this);
        this.onFocus = this.onFocus.bind(this);
        this.state = {
            focused: false
        };
    }

    onBlur() {
        this.setState({ focused: false })
    }

    onFocus() {
        this.setState({ focused: true })
    }

    render() {

        var fieldClass = "tb-field";
        if (this.props.form.required) {
            fieldClass += " tb-required";
        }
        if (this.props.form.readonly) {
            fieldClass += " tb-readonly";
        }
        if (this.state.focused) {
            fieldClass += " tb-focused";
        }

        var multiline = this.props.form.type === 'textarea';
        var rows = multiline ? this.props.form.rows : 1;
        var rowsMax = multiline ? this.props.form.rowsMax : 1;

        return (
            <div>
                <TextField
                    className={fieldClass}
                    type={this.props.form.type}
                    floatingLabelText={this.props.form.title}
                    hintText={this.props.form.placeholder}
                    errorText={this.props.error}
                    onChange={this.props.onChangeValidate}
                    defaultValue={this.props.value}
                    disabled={this.props.form.readonly}
                    multiLine={multiline}
                    rows={rows}
                    rowsMax={rowsMax}
                    onFocus={this.onFocus}
                    onBlur={this.onBlur}
                    style={this.props.form.style || {width: '100%'}} />
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardText);