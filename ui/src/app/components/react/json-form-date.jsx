/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
import DatePicker from 'material-ui/DatePicker/DatePicker';

class ThingsboardDate extends React.Component {

    constructor(props) {
        super(props);
        this.onDatePicked = this.onDatePicked.bind(this);
    }


    onDatePicked(empty, date) {
        this.props.onChangeValidate(date);
    }

    render() {

        var fieldClass = "tb-date-field";
        if (this.props.form.required) {
            fieldClass += " tb-required";
        }
        if (this.props.form.readonly) {
            fieldClass += " tb-readonly";
        }

        return (
            <div style={{width: '100%', display: 'block'}}>
                <DatePicker
                    className={fieldClass}
                    mode={'landscape'}
                    autoOk={true}
                    hintText={this.props.form.title}
                    onChange={this.onDatePicked}
                    onShow={null}
                    onDismiss={null}
                    disabled={this.props.form.readonly}
                    style={this.props.form.style || {width: '100%'}}/>

            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardDate);