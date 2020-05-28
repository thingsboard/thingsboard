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
import $ from 'jquery';
import React from 'react';
import ReactDOM from 'react-dom';
import ThingsboardBaseComponent from './json-form-base-component.jsx';
import reactCSS from 'reactcss';
import TextField from 'material-ui/TextField';
import IconButton from 'material-ui/IconButton';

class ThingsboardIcon extends React.Component {

    constructor(props) {
        super(props);
        this.onValueChanged = this.onValueChanged.bind(this);
        this.onIconClick = this.onIconClick.bind(this);
        this.onClear = this.onClear.bind(this);
        var icon = props.value ? props.value : '';
        this.state = {
            icon: icon
        };
    }

    componentDidMount() {
        var node = ReactDOM.findDOMNode(this);
        var iconContainer = $(node).children('#icon-container');
        iconContainer.click(this, function(event) {
            event.data.onIconClick(event);
        });
    }

    componentWillUnmount () {
        var node = ReactDOM.findDOMNode(this);
        var iconContainer = $(node).children('#icon-container');
        iconContainer.off( "click" );
    }

    onValueChanged(value) {
        var icon = value;

        this.setState({
            icon: value
        })
        this.props.onChange(this.props.form.key, value);
    }

    onIconClick(event) {
        this.props.onIconClick(event);
    }

    onClear(event) {
        if (event) {
            event.stopPropagation();
        }
        this.onValueChanged('');
    }

    render() {

        const styles = reactCSS({
            'default': {
                clear: {
                    marginTop: '15px'
                },
                container: {
                    display: 'flex'
                },
                icon: {
                    display: 'inline-block',
                    marginRight: '10px',
                    marginTop: '16px',
                    marginBottom: 'auto',
                    cursor: 'pointer',
                    border: 'solid 1px rgba(0, 0, 0, .27)'
                },
                iconContainer: {
                    display: 'flex',
                    width: '100%'
                },
                iconText: {
                    display: 'inline-block',
                    width: '100%'
                },
            },
        });

        var fieldClass = "tb-field";
        if (this.props.form.required) {
            fieldClass += " tb-required";
        }
        if (this.state.focused) {
            fieldClass += " tb-focused";
        }

        var pickedIcon = 'more_horiz';
        if (this.state.icon != '') {
            pickedIcon = this.state.icon;
        }

        return (
            <div style={ styles.container }>
                 <div id="icon-container" style={ styles.iconContainer }>
                    <IconButton iconClassName="material-icons" style={ styles.icon }>
                        {pickedIcon}
                    </IconButton>
                    <TextField
                        className={fieldClass}
                        floatingLabelText={this.props.form.title}
                        hintText={this.props.form.placeholder}
                        errorText={this.props.error}
                        value={this.state.icon}
                        disabled={this.props.form.readonly}
                        style={ styles.iconText } />
                 </div>
                <IconButton iconClassName="material-icons" tooltip="Clear" onTouchTap={this.onClear}>clear</IconButton>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardIcon);
