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
import DateFnsUtils from '@date-io/date-fns';
import { KeyboardDatePicker, MuiPickersUtilsProvider } from '@material-ui/pickers';
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';

interface ThingsboardDateState extends JsonFormFieldState {
  currentValue: Date | null;
}

class ThingsboardDate extends React.Component<JsonFormFieldProps, ThingsboardDateState> {

    constructor(props) {
        super(props);
        this.onDatePicked = this.onDatePicked.bind(this);
        let value: Date | null = null;
        if (this.props.value && typeof this.props.value === 'number') {
          value = new Date(this.props.value);
        }
        this.state = {
          currentValue: value
        };
    }


    onDatePicked(date: Date | null) {
        this.setState({
          currentValue: date
        });
        this.props.onChangeValidate(date ? date.getTime() : null);
    }

    render() {

        let fieldClass = 'tb-date-field';
        if (this.props.form.required) {
            fieldClass += ' tb-required';
        }
        if (this.props.form.readonly) {
            fieldClass += ' tb-readonly';
        }

        return (
          <MuiPickersUtilsProvider utils={DateFnsUtils}>
            <div style={{width: '100%', display: 'block'}}>
                <KeyboardDatePicker
                    disableToolbar
                    variant='inline'
                    format='MM/dd/yyyy'
                    margin='normal'
                    className={fieldClass}
                    label={this.props.form.title}
                    value={this.state.currentValue}
                    onChange={this.onDatePicked}
                    disabled={this.props.form.readonly}
                    style={this.props.form.style || {width: '100%'}}/>

            </div>
          </MuiPickersUtilsProvider>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardDate);
