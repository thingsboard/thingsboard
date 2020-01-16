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
import { utils } from 'react-schema-form';
import ThingsboardBaseComponent from './json-form-base-component.jsx';
import RaisedButton from 'material-ui/RaisedButton';
import FontIcon from 'material-ui/FontIcon';
import _ from 'lodash';
import IconButton from 'material-ui/IconButton';

class ThingsboardArray extends React.Component {

    constructor(props) {
        super(props);
        this.onAppend = this.onAppend.bind(this);
        this.onDelete = this.onDelete.bind(this);
        var model = utils.selectOrSet(this.props.form.key, this.props.model) || [];
        var keys = [];
        for (var i=0;i<model.length;i++) {
            keys.push(i);
        }
        this.state = {
            model:  model,
            keys: keys
        };
    }

    componentDidMount() {
        if(this.props.form.startEmpty !== true && this.state.model.length === 0) {
            this.onAppend();
        }
    }

    onAppend() {
        var empty;
        if(this.props.form && this.props.form.schema && this.props.form.schema.items) {
            var items = this.props.form.schema.items;
            if (items.type && items.type.indexOf('object') !== -1) {
                empty = {};
                if (!this.props.options || this.props.options.setSchemaDefaults !== false) {
                    empty = typeof items['default'] !== 'undefined' ? items['default'] : empty;
                    if (empty) {
                        utils.traverseSchema(items, function(prop, path) {
                            if (typeof prop['default'] !== 'undefined') {
                                utils.selectOrSet(path, empty, prop['default']);
                            }
                        });
                    }
                }

            } else if (items.type && items.type.indexOf('array') !== -1) {
                empty = [];
                if (!this.props.options || this.props.options.setSchemaDefaults !== false) {
                    empty = items['default'] || empty;
                }
            } else {
                if (!this.props.options || this.props.options.setSchemaDefaults !== false) {
                    empty = items['default'] || empty;
                }
            }
        }
        var newModel = this.state.model;
        newModel.push(empty);
        var newKeys = this.state.keys;
        var key = 0;
        if (newKeys.length > 0) {
            key = newKeys[newKeys.length-1]+1;
        }
        newKeys.push(key);
        this.setState({
                model: newModel,
                keys: newKeys
            }
        );
        this.props.onChangeValidate(this.state.model);
    }

    onDelete(index) {
        var newModel = this.state.model;
        newModel.splice(index, 1);
        var newKeys = this.state.keys;
        newKeys.splice(index, 1);
        this.setState(
            {
                model: newModel,
                keys: newKeys
            }
        );
        this.props.onChangeValidate(this.state.model);
    }

    setIndex(index) {
        return function(form) {
            if (form.key) {
                form.key[form.key.indexOf('')] = index;
            }
        };
    };

    copyWithIndex(form, index) {
        var copy = _.cloneDeep(form);
        copy.arrayIndex = index;
        utils.traverseForm(copy, this.setIndex(index));
        return copy;
    };

    render() {
        var arrays = [];
        var fields = [];
        var model = this.state.model;
        var keys = this.state.keys;
        var items = this.props.form.items;
        for(var i = 0; i < model.length; i++ ) {
            let removeButton = '';
            if (!this.props.form.readonly) {
                let boundOnDelete = this.onDelete.bind(this, i)
                removeButton = <IconButton iconClassName="material-icons" tooltip="Remove" onTouchTap={boundOnDelete}>clear</IconButton>
            }
            let forms = this.props.form.items.map(function(form, index){
                var copy = this.copyWithIndex(form, i);
                return this.props.builder(copy, this.props.model, index, this.props.onChange, this.props.onColorClick, this.props.onIconClick, this.props.onToggleFullscreen, this.props.mapper, this.props.builder);
            }.bind(this));
            arrays.push(
            <li key={keys[i]} className="list-group-item">
                {removeButton}
                {forms}
                </li>
        );
        }
        let addButton = '';
        if (!this.props.form.readonly) {
            addButton = <RaisedButton label={this.props.form.add || 'New'}
                                    primary={true}
                                    icon={<FontIcon className="material-icons">add</FontIcon>}
                                    onTouchTap={this.onAppend}></RaisedButton>;
        }

        return (
            <div>
                <div className="tb-container">
                    <div className="tb-head-label">{this.props.form.title}</div>
                        <ol className="list-group">
                            {arrays}
                        </ol>
                </div>
                {addButton}
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardArray);
