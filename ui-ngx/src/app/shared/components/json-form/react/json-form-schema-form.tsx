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

import ThingsboardArray from './json-form-array';
import ThingsboardJavaScript from './json-form-javascript';
import ThingsboardJson from './json-form-json';
import ThingsboardHtml from './json-form-html';
import ThingsboardCss from './json-form-css';
import ThingsboardColor from './json-form-color';
import ThingsboardRcSelect from './json-form-rc-select';
import ThingsboardNumber from './json-form-number';
import ThingsboardText from './json-form-text';
import ThingsboardSelect from './json-form-select';
import ThingsboardRadios from './json-form-radios';
import ThingsboardDate from './json-form-date';
import ThingsboardImage from './json-form-image';
import ThingsboardCheckbox from './json-form-checkbox';
import ThingsboardHelp from './json-form-help';
import ThingsboardFieldSet from './json-form-fieldset';
import ThingsboardIcon from './json-form-icon';
import { JsonFormData, JsonFormProps, onChangeFn, OnColorClickFn, OnIconClickFn } from './json-form.models';

import _ from 'lodash';
import * as tinycolor_ from 'tinycolor2';
import { GroupInfo } from '@shared/models/widget.models';

const tinycolor = tinycolor_;

class ThingsboardSchemaForm extends React.Component<JsonFormProps, any> {

  private hasConditions: boolean;
  private readonly mapper: {[type: string]: any};

  constructor(props) {
    super(props);

    this.mapper = {
      number: ThingsboardNumber,
      text: ThingsboardText,
      password: ThingsboardText,
      textarea: ThingsboardText,
      select: ThingsboardSelect,
      radios: ThingsboardRadios,
      date: ThingsboardDate,
      image: ThingsboardImage,
      checkbox: ThingsboardCheckbox,
      help: ThingsboardHelp,
      array: ThingsboardArray,
      javascript: ThingsboardJavaScript,
      json: ThingsboardJson,
      html: ThingsboardHtml,
      css: ThingsboardCss,
      color: ThingsboardColor,
      'rc-select': ThingsboardRcSelect,
      fieldset: ThingsboardFieldSet,
      icon: ThingsboardIcon
    };

    this.onChange = this.onChange.bind(this);
    this.onColorClick = this.onColorClick.bind(this);
    this.onIconClick = this.onIconClick.bind(this);
    this.onToggleFullscreen = this.onToggleFullscreen.bind(this);
    this.hasConditions = false;
  }

  onChange(key: (string | number)[], val: any, forceUpdate?: boolean) {
    this.props.onModelChange(key, val, forceUpdate);
    if (this.hasConditions) {
      this.forceUpdate();
    }
  }

  onColorClick(key: (string | number)[], val: tinycolor.ColorFormats.RGBA,
               colorSelectedFn: (color: tinycolor.ColorFormats.RGBA) => void) {
    this.props.onColorClick(key, val, colorSelectedFn);
  }

  onIconClick(key: (string | number)[], val: string,
               iconSelectedFn: (icon: string) => void) {
    this.props.onIconClick(key, val, iconSelectedFn);
  }

  onToggleFullscreen(element: HTMLElement, fullscreenFinishFn?: () => void) {
    this.props.onToggleFullscreen(element, fullscreenFinishFn);
  }


  builder(form: JsonFormData,
          model: any,
          index: number,
          onChange: onChangeFn,
          onColorClick: OnColorClickFn,
          onIconClick: OnIconClickFn,
          onToggleFullscreen: () => void,
          mapper: {[type: string]: any}): JSX.Element {
    const type = form.type;
    const Field = this.mapper[type];
    if (!Field) {
      console.log('Invalid field: \"' + form.key[0] + '\"!');
      return null;
    }
    if (form.condition) {
      this.hasConditions = true;
      // tslint:disable-next-line:no-eval
      if (eval(form.condition) === false) {
        return null;
      }
    }
    return <Field model={model} form={form} key={index} onChange={onChange}
                  onColorClick={onColorClick}
                  onIconClick={onIconClick}
                  onToggleFullscreen={onToggleFullscreen}
                  mapper={mapper} builder={this.builder}/>;
  }

  createSchema(theForm: any[]): JSX.Element {
    const merged = JsonFormUtils.merge(this.props.schema, theForm, this.props.ignore, this.props.option);
    let mapper = this.mapper;
    if (this.props.mapper) {
      mapper = _.merge(this.mapper, this.props.mapper);
    }
    const forms = merged.map(function(form, index) {
      return this.builder(form, this.props.model, index, this.onChange, this.onColorClick,
        this.onIconClick, this.onToggleFullscreen, mapper);
    }.bind(this));

    let formClass = 'SchemaForm';
    if (this.props.isFullscreen) {
      formClass += ' SchemaFormFullscreen';
    }

    return (
      <div style={{width: '100%'}} className={formClass}>{forms}</div>
    );
  }

  render() {
    if (this.props.groupInfoes && this.props.groupInfoes.length > 0) {
      const content: JSX.Element[] = [];
      for (const info of this.props.groupInfoes) {
        const forms = this.createSchema(this.props.form[info.formIndex]);
        const item = <ThingsboardSchemaGroup key={content.length} forms={forms} info={info}></ThingsboardSchemaGroup>;
        content.push(item);
      }
      return (<div>{content}</div>);
    } else {
      return this.createSchema(this.props.form);
    }
  }
}
export default ThingsboardSchemaForm;

interface ThingsboardSchemaGroupProps {
  info: GroupInfo;
  forms: JSX.Element;
}

interface ThingsboardSchemaGroupState {
  showGroup: boolean;
}

class ThingsboardSchemaGroup extends React.Component<ThingsboardSchemaGroupProps, ThingsboardSchemaGroupState> {
  constructor(props) {
    super(props);
    this.state = {
      showGroup: true
    };
  }

  toogleGroup(index) {
    this.setState({
      showGroup: !this.state.showGroup
    });
  }

  render() {
    const theCla = 'pull-right fa fa-chevron-down tb-toggle-icon' + (this.state.showGroup ? '' : ' tb-toggled');
    return (<section className='mat-elevation-z1' style={{marginTop: '10px'}}>
      <div className='SchemaGroupname tb-button-toggle'
           onClick={this.toogleGroup.bind(this)}>{this.props.info.GroupTitle}<span className={theCla}></span></div>
      <div style={{padding: '20px'}} className={this.state.showGroup ? '' : 'invisible'}>{this.props.forms}</div>
    </section>);
  }
}
