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
import { AdapterMoment } from '@mui/x-date-pickers/AdapterMoment'
import { LocalizationProvider, DatePicker } from '@mui/x-date-pickers';
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';
import moment from 'moment';

interface ThingsboardDateState extends JsonFormFieldState {
  currentValue: Date | null;
}

class ThingsboardDate extends React.Component<JsonFormFieldProps, ThingsboardDateState> {

    constructor(props: JsonFormFieldProps) {
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


    onDatePicked(date: moment.Moment | null) {
        this.setState({
          currentValue: date?.toDate()
        });
        this.props.onChangeValidate(date ? date.valueOf() : null);
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
          <LocalizationProvider dateAdapter={AdapterMoment}>
            <div style={{width: '100%', display: 'block'}}>
                <DatePicker
                    slotProps={{
                      textField: {
                        variant: 'standard'
                      }
                    }}
                    format='MM/DD/YYYY'
                    className={fieldClass}
                    label={this.props.form.title}
                    value={moment(this.state.currentValue?.valueOf())}
                    onChange={this.onDatePicked}
                    disabled={this.props.form.readonly}
                    sx={this.props.form.style || {width: '100%'}}
                />

            </div>
          </LocalizationProvider>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardDate);
