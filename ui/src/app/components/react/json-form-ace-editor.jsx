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
import './json-form-ace-editor.scss';

import React from 'react';
import ThingsboardBaseComponent from './json-form-base-component.jsx';
import reactCSS from 'reactcss';
import AceEditor from 'react-ace';
import FlatButton from 'material-ui/FlatButton';
import 'brace/ext/language_tools';
import 'brace/ext/searchbox';
import 'brace/theme/github';

import fixAceEditor from './../ace-editor-fix';

class ThingsboardAceEditor extends React.Component {

    constructor(props) {
        super(props);
        this.onValueChanged = this.onValueChanged.bind(this);
        this.onBlur = this.onBlur.bind(this);
        this.onFocus = this.onFocus.bind(this);
        this.onTidy = this.onTidy.bind(this);
        this.onLoad = this.onLoad.bind(this);
        this.onToggleFull = this.onToggleFull.bind(this);
        var value = props.value ? props.value + '' : '';
        this.state = {
            isFull: false,
            value: value,
            focused: false
        };
    }

    onValueChanged(value) {
        this.setState({
            value: value
        });
        this.props.onChangeValidate({
            target: {
                value: value
            }
        });
    }

    onBlur() {
        this.setState({ focused: false })
    }

    onFocus() {
        this.setState({ focused: true })
    }

    onTidy() {
        if (!this.props.form.readonly) {
            var value = this.state.value;
            value = this.props.onTidy(value);
            this.setState({
                value: value
            })
            this.props.onChangeValidate({
                target: {
                    value: value
                }
            });
        }
    }

    onLoad(editor) {
        this.aceEditor = editor;
        fixAceEditor(editor);
    }

    onToggleFull() {
        this.setState({ isFull: !this.state.isFull });
        this.props.onToggleFullscreen();
        this.updateAceEditorSize = true;
    }

    componentDidUpdate() {
        if (this.updateAceEditorSize) {
            if (this.aceEditor) {
                this.aceEditor.resize();
                this.aceEditor.renderer.updateFull();
            }
            this.updateAceEditorSize = false;
        }
    }

    render() {

        const styles = reactCSS({
            'default': {
                tidyButtonStyle: {
                    color: '#7B7B7B',
                    minWidth: '32px',
                    minHeight: '15px',
                    lineHeight: '15px',
                    fontSize: '0.800rem',
                    margin: '0',
                    padding: '4px',
                    height: '23px',
                    borderRadius: '5px',
                    marginLeft: '5px'
                }
            }
        });

        var labelClass = "tb-label";
        if (this.props.form.required) {
            labelClass += " tb-required";
        }
        if (this.props.form.readonly) {
            labelClass += " tb-readonly";
        }
        if (this.state.focused) {
            labelClass += " tb-focused";
        }
        var containerClass = "tb-container";
        var style = this.props.form.style || {width: '100%'};
        if (this.state.isFull) {
            containerClass += " fullscreen-form-field";
        }
        return (
            <div className={containerClass}>
                <label className={labelClass}>{this.props.form.title}</label>
                <div className="json-form-ace-editor">
                    <div className="title-panel">
                        <label>{this.props.mode}</label>
                        <FlatButton style={ styles.tidyButtonStyle } className="tidy-button" label={'Tidy'} onTouchTap={this.onTidy}/>
                        <FlatButton style={ styles.tidyButtonStyle } className="tidy-button" label={this.state.isFull ? 'Exit fullscreen' : 'Fullscreen'} onTouchTap={this.onToggleFull}/>
                    </div>
                    <AceEditor mode={this.props.mode}
                               height={this.state.isFull ? "100%" : "150px"}
                               width={this.state.isFull ? "100%" : "300px"}
                               theme="github"
                               onChange={this.onValueChanged}
                               onFocus={this.onFocus}
                               onBlur={this.onBlur}
                               onLoad={this.onLoad}
                               name={this.props.form.title}
                               value={this.state.value}
                               readOnly={this.props.form.readonly}
                               editorProps={{$blockScrolling: Infinity}}
                               enableBasicAutocompletion={true}
                               enableSnippets={true}
                               enableLiveAutocompletion={true}
                               style={style}/>
                </div>
                <div className="json-form-error"
                     style={{opacity: this.props.valid ? '0' : '1'}}>{this.props.error}</div>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardAceEditor);
