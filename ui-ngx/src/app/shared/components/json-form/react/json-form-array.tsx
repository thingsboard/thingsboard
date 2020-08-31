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
import JsonFormUtils from './json-form-utils';
import ThingsboardBaseComponent from './json-form-base-component';
import Button from '@material-ui/core/Button';
import _ from 'lodash';
import IconButton from '@material-ui/core/IconButton';
import Clear from '@material-ui/icons/Clear';
import Add from '@material-ui/icons/Add';
import Tooltip from '@material-ui/core/Tooltip';
import {
  JsonFormData,
  JsonFormFieldProps,
  JsonFormFieldState
} from '@shared/components/json-form/react/json-form.models';

interface ThingsboardArrayState extends JsonFormFieldState {
  model: any[];
  keys: number[];
}

class ThingsboardArray extends React.Component<JsonFormFieldProps, ThingsboardArrayState> {

    constructor(props) {
        super(props);
        this.onAppend = this.onAppend.bind(this);
        this.onDelete = this.onDelete.bind(this);
        const model = JsonFormUtils.selectOrSet(this.props.form.key, this.props.model) || [];
        const keys: number[] = [];
        for (let i = 0; i < model.length; i++) {
            keys.push(i);
        }
        this.state = {
            model,
            keys
        };
    }

    componentDidMount() {
        if (this.props.form.startEmpty !== true && this.state.model.length === 0) {
            this.onAppend();
        }
    }

    onAppend() {
        let empty;
        if (this.props.form && this.props.form.schema && this.props.form.schema.items) {
            const items = this.props.form.schema.items;
            if (items.type && items.type.indexOf('object') !== -1) {
                empty = {};
                if (!this.props.options || this.props.options.setSchemaDefaults !== false) {
                    empty = typeof items.default !== 'undefined' ? items.default : empty;
                    if (empty) {
                      JsonFormUtils.traverseSchema(items, (prop, path) => {
                            if (typeof prop.default !== 'undefined') {
                              JsonFormUtils.selectOrSet(path, empty, prop.default);
                            }
                        });
                    }
                }
            } else if (items.type && items.type.indexOf('array') !== -1) {
                empty = [];
                if (!this.props.options || this.props.options.setSchemaDefaults !== false) {
                    empty = items.default || empty;
                }
            } else {
                if (!this.props.options || this.props.options.setSchemaDefaults !== false) {
                    empty = items.default || empty;
                }
            }
        }
        const newModel = this.state.model;
        newModel.push(empty);
        const newKeys = this.state.keys;
        let key = 0;
        if (newKeys.length > 0) {
            key = newKeys[newKeys.length - 1] + 1;
        }
        newKeys.push(key);
        this.setState({
                model: newModel,
                keys: newKeys
            }
        );
        this.props.onChangeValidate(this.state.model, true);
    }

    onDelete(index: number) {
        const newModel = this.state.model;
        newModel.splice(index, 1);
        const newKeys = this.state.keys;
        newKeys.splice(index, 1);
        this.setState(
            {
                model: newModel,
                keys: newKeys
            }
        );
        this.props.onChangeValidate(this.state.model, true);
    }

    setIndex(index: number) {
        return (form: JsonFormData) => {
            if (form.key) {
                form.key[form.key.indexOf('')] = index;
            }
        };
    }

    copyWithIndex(form: JsonFormData, index: number): JsonFormData {
        const copy: JsonFormData = _.cloneDeep(form);
        copy.arrayIndex = index;
        JsonFormUtils.traverseForm(copy, this.setIndex(index));
        return copy;
    }

    render() {
        const arrays = [];
        const fields = [];
        const model = this.state.model;
        const keys = this.state.keys;
        const items = this.props.form.items;
        for (let i = 0; i < model.length; i++ ) {
            let removeButton: JSX.Element = null;
            if (!this.props.form.readonly) {
                const boundOnDelete = this.onDelete.bind(this, i);
                removeButton = <Tooltip title='Remove'><IconButton onClick={boundOnDelete}><Clear/></IconButton></Tooltip>;
            }
            const forms = (this.props.form.items as JsonFormData[]).map((form, index) => {
                const copy = this.copyWithIndex(form, i);
                return this.props.builder(copy, this.props.model, index, this.props.onChange,
                  this.props.onColorClick, this.props.onIconClick, this.props.onToggleFullscreen, this.props.mapper);
            });
            arrays.push(
            <li key={keys[i]} className='list-group-item'>
                {removeButton}
                {forms}
                </li>
        );
        }
        let addButton: JSX.Element = null;
        if (!this.props.form.readonly) {
            addButton = <Button variant='contained'
                                color='primary'
                                startIcon={<Add/>}
                                style={{marginBottom: '8px'}}
                                onClick={this.onAppend}>{this.props.form.add || 'New'}</Button>;
        }

        return (
            <div>
                <div className='tb-container'>
                    <div className='tb-head-label'>{this.props.form.title}</div>
                        <ol className='list-group'>
                            {arrays}
                        </ol>
                </div>
                {addButton}
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardArray);
