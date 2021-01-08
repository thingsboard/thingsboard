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
import ThingsboardAceEditor from './json-form-ace-editor';
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';
import { Observable } from 'rxjs/internal/Observable';
import { beautifyJs } from '@shared/models/beautify.models';

class ThingsboardJavaScript extends React.Component<JsonFormFieldProps, JsonFormFieldState> {

    constructor(props) {
        super(props);
        this.onTidyJavascript = this.onTidyJavascript.bind(this);
    }

    onTidyJavascript(javascript: string): Observable<string> {
        return beautifyJs(javascript, {indent_size: 4, wrap_line_length: 60});
    }

    render() {
        return (
           <ThingsboardAceEditor {...this.props} mode='javascript' onTidy={this.onTidyJavascript} {...this.state}></ThingsboardAceEditor>
        );
    }
}

export default ThingsboardJavaScript;
