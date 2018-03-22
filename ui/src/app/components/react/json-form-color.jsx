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
import './json-form-color.scss';

import $ from 'jquery';
import React from 'react';
import ReactDOM from 'react-dom';
import ThingsboardBaseComponent from './json-form-base-component.jsx';
import reactCSS from 'reactcss';
import tinycolor from 'tinycolor2';
import TextField from 'material-ui/TextField';
import IconButton from 'material-ui/IconButton';

class ThingsboardColor extends React.Component {

    constructor(props) {
        super(props);
        this.onValueChanged = this.onValueChanged.bind(this);
        this.onSwatchClick = this.onSwatchClick.bind(this);
        this.onClear = this.onClear.bind(this);
        var value = props.value ? props.value + '' : null;
        var color = value != null ? tinycolor(value).toRgb() : null;
        this.state = {
            color: color
        };
    }

    componentDidMount() {
        var node = ReactDOM.findDOMNode(this);
        var colContainer = $(node).children('#color-container');
        colContainer.click(this, function(event) {
            if (!event.data.props.form.readonly) {
                event.data.onSwatchClick(event);
            }
        });
    }

    componentWillUnmount () {
        var node = ReactDOM.findDOMNode(this);
        var colContainer = $(node).children('#color-container');
        colContainer.off( "click" );
    }

    onValueChanged(value) {
        var color = null;
        if (value != null) {
            color = tinycolor(value);
        }
        this.setState({
            color: value
        })
        var colorValue = '';
        if (color != null && color.getAlpha() != 1) {
            colorValue = color.toRgbString();
        } else if (color != null) {
            colorValue = color.toHexString();
        }
        this.props.onChangeValidate({
            target: {
                value: colorValue
            }
        });
    }

    onSwatchClick(event) {
        this.props.onColorClick(event, this.props.form.key, this.state.color);
    }

    onClear(event) {
        if (event) {
            event.stopPropagation();
        }
        this.onValueChanged(null);
    }

    render() {

        var background = 'rgba(0,0,0,0)';
        if (this.state.color != null) {
            background = `rgba(${ this.state.color.r }, ${ this.state.color.g }, ${ this.state.color.b }, ${ this.state.color.a })`;
        }

        const styles = reactCSS({
            'default': {
                color: {
                    background: `${ background }`
                },
                swatch: {
                    display: 'inline-block',
                    marginRight: '10px',
                    marginTop: 'auto',
                    marginBottom: 'auto',
                    cursor: 'pointer',
                    opacity: `${ this.props.form.readonly ? '0.6' : '1' }`
                },
                swatchText: {
                    display: 'inline-block',
                    width: '100%'
                },
                container: {
                    display: 'flex'
                },
                colorContainer: {
                    display: 'flex',
                    width: '100%'
                }
            },
        });

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

        var stringColor = '';
        if (this.state.color != null) {
            var color = tinycolor(this.state.color);
            stringColor = color.toRgbString();
        }

        return (
            <div style={ styles.container }>
                 <div id="color-container" style={ styles.colorContainer }>
                    <div className="tb-color-preview" style={ styles.swatch }>
                        <div className="tb-color-result" style={ styles.color }/>
                    </div>
                    <TextField
                        className={fieldClass}
                        floatingLabelText={this.props.form.title}
                        hintText={this.props.form.placeholder}
                        errorText={this.props.error}
                        value={stringColor}
                        disabled={this.props.form.readonly}
                        style={ styles.swatchText } />
                 </div>
                <IconButton iconClassName="material-icons" tooltip="Clear" onTouchTap={this.onClear}>clear</IconButton>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardColor);
