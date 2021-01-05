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
import reactCSS from 'reactcss';
import Button from '@material-ui/core/Button';
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';
import { IEditorProps } from 'react-ace/src/types';
import { map, mergeMap } from 'rxjs/operators';
import { loadAceDependencies } from '@shared/models/ace/ace.models';
import { from } from 'rxjs';
import { Observable } from 'rxjs/internal/Observable';

const ReactAce = React.lazy(() => {
  return loadAceDependencies().pipe(
    mergeMap(() => {
      return from(import('react-ace'));
    })
  ).toPromise();
});

interface ThingsboardAceEditorProps extends JsonFormFieldProps {
  mode: string;
  onTidy: (value: string) => Observable<string>;
}

interface ThingsboardAceEditorState extends JsonFormFieldState {
  isFull: boolean;
  focused: boolean;
}

class ThingsboardAceEditor extends React.Component<ThingsboardAceEditorProps, ThingsboardAceEditorState> {

    hostElement: HTMLElement;
    private aceEditor: IEditorProps;

    constructor(props) {
        super(props);
        this.onValueChanged = this.onValueChanged.bind(this);
        this.onBlur = this.onBlur.bind(this);
        this.onFocus = this.onFocus.bind(this);
        this.onTidy = this.onTidy.bind(this);
        this.onLoad = this.onLoad.bind(this);
        this.onToggleFull = this.onToggleFull.bind(this);
        const value = props.value ? props.value + '' : '';
        this.state = {
            isFull: false,
            value,
            focused: false
        };
    }

    onValueChanged(value) {
        this.setState({
            value
        });
        this.props.onChangeValidate({
            target: {
                value
            }
        });
    }

    onBlur() {
        this.setState({ focused: false });
    }

    onFocus() {
        this.setState({ focused: true });
    }

    onTidy() {
        if (!this.props.form.readonly) {
            let value = this.state.value;
            this.props.onTidy(value).subscribe(
              (processedValue) => {
                this.setState({
                  value: processedValue
                });
                this.props.onChangeValidate({
                  target: {
                    value: processedValue
                  }
                });
              }
            );
        }
    }

    onLoad(editor: IEditorProps) {
        this.aceEditor = editor;
    }

    onToggleFull() {
        this.setState({ isFull: !this.state.isFull });
        this.props.onToggleFullscreen(this.hostElement, () => {
          if (this.aceEditor) {
            this.aceEditor.resize();
            this.aceEditor.renderer.updateFull();
          }
        });
    }

    componentDidUpdate() {
    }

    render() {

        const styles = reactCSS({
            default: {
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

        let labelClass = 'tb-label';
        if (this.props.form.required) {
            labelClass += ' tb-required';
        }
        if (this.props.form.readonly) {
            labelClass += ' tb-readonly';
        }
        if (this.state.focused) {
            labelClass += ' tb-focused';
        }
        let containerClass = 'tb-container';
        const style = this.props.form.style || {width: '100%'};
        if (this.state.isFull) {
            containerClass += ' fullscreen-form-field';
        }
        return (
          <div>
            <div className='tb-json-form' ref={c => (this.hostElement = c)}>
              <div className={containerClass}>
                  <label className={labelClass}>{this.props.form.title}</label>
                  <div className='json-form-ace-editor'>
                      <div className='title-panel'>
                          <label>{this.props.mode}</label>
                          <Button style={ styles.tidyButtonStyle }
                                  className='tidy-button' onClick={this.onTidy}>Tidy</Button>
                          <Button style={ styles.tidyButtonStyle }
                                  className='tidy-button' onClick={this.onToggleFull}>
                            {this.state.isFull ?
                              'Exit fullscreen' : 'Fullscreen'}
                          </Button>
                      </div>
                      <React.Suspense fallback={<div>Loading...</div>}>
                        <ReactAce  mode={this.props.mode}
                                   height={this.state.isFull ? '100%' : '150px'}
                                   width={this.state.isFull ? '100%' : '300px'}
                                   theme='github'
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
                      </React.Suspense>
                  </div>
                  <div className='json-form-error'
                       style={{opacity: this.props.valid ? '0' : '1'}}>{this.props.error}</div>
              </div>
            </div>
          </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardAceEditor);
