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
import { MouseEvent } from 'react';
import ThingsboardBaseComponent from './json-form-base-component';
import reactCSS from 'reactcss';
import TextField from '@mui/material/TextField';
import IconButton from '@mui/material/IconButton';
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';
import Clear from '@mui/icons-material/Clear';
import Icon from '@mui/material/Icon';
import Tooltip from '@mui/material/Tooltip';

interface ThingsboardIconState extends JsonFormFieldState {
  icon: string | null;
  focused: boolean;
}

class ThingsboardIcon extends React.Component<JsonFormFieldProps, ThingsboardIconState> {

    containerRef = React.createRef<HTMLDivElement>();

    constructor(props: JsonFormFieldProps) {
        super(props);
        this.onBlur = this.onBlur.bind(this);
        this.onFocus = this.onFocus.bind(this);
        this.onValueChanged = this.onValueChanged.bind(this);
        this.onIconClick = this.onIconClick.bind(this);
        this.onClear = this.onClear.bind(this);
        const icon = props.value ? props.value : '';
        this.state = {
            icon,
            focused: false
        };
    }

    onBlur() {
      this.setState({focused: false});
    }

    onFocus() {
      this.setState({focused: true});
    }

    componentDidMount() {
        const node = this.containerRef.current;
        const iconContainer = $(node).children('#icon-container');
        iconContainer.on('click', (event) => {
          if (!this.props.form.readonly) {
            this.onIconClick(event);
          }
        });
    }

    componentWillUnmount() {
        const node = this.containerRef.current;
        const iconContainer = $(node).children('#icon-container');
        iconContainer.off( 'click' );
    }

    onValueChanged(value: string | null) {
        this.setState({
            icon: value
        });
        this.props.onChange(this.props.form.key, value);
    }

    onIconClick(_event) {
      this.props.onIconClick(this.props.form.key, this.state.icon,
        (color) => {
          this.onValueChanged(color);
        }
      );
    }

    onClear(event: MouseEvent) {
        if (event) {
            event.stopPropagation();
        }
        this.onValueChanged('');
    }

    render() {

        const styles = reactCSS({
            default: {
                container: {
                    display: 'flex',
                    flexDirection: 'row',
                    alignItems: 'center'
                },
                icon: {
                    padding: '12px',
                    marginRight: '10px',
                    marginBottom: 'auto',
                    cursor: 'pointer',
                    border: 'solid 1px rgba(0, 0, 0, .27)',
                    borderRadius: '0'
                },
                iconContainer: {
                    display: 'flex',
                    width: '100%'
                },
                iconText: {
                    width: '100%'
                },
            },
        });

        let fieldClass = 'tb-field';
        if (this.props.form.required) {
            fieldClass += ' tb-required';
        }
        if (this.state.focused) {
            fieldClass += ' tb-focused';
        }

        let pickedIcon = 'more_horiz';
        let icon = '';
        if (this.state.icon !== '') {
            pickedIcon = this.state.icon;
            icon = this.state.icon;
        }

        return (
            <div ref={this.containerRef} style={ styles.container }>
                 <div id='icon-container' style={ styles.iconContainer }>
                    <IconButton style={ styles.icon }>
                      <Icon>{pickedIcon}</Icon>
                    </IconButton>
                    <TextField
                        variant={'standard'}
                        className={fieldClass}
                        label={this.props.form.title}
                        error={!this.props.valid}
                        helperText={this.props.valid ? this.props.form.placeholder : this.props.error}
                        value={icon}
                        disabled={this.props.form.readonly}
                        onFocus={this.onFocus}
                        onBlur={this.onBlur}
                        style={ styles.iconText } />
                 </div>
                 <Tooltip title='Clear' placement='top'><IconButton onClick={this.onClear}><Clear/></IconButton></Tooltip>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardIcon);
