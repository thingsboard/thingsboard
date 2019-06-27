/*
 * Copyright © 2016-2019 The Thingsboard Authors
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

import ThingsboardArray from './json-form-array.jsx';
import ThingsboardJavaScript from './json-form-javascript.jsx';
import ThingsboardJson from './json-form-json.jsx';
import ThingsboardHtml from './json-form-html.jsx';
import ThingsboardCss from './json-form-css.jsx';
import ThingsboardColor from './json-form-color.jsx'
import ThingsboardRcSelect from './json-form-rc-select.jsx';
import ThingsboardNumber from './json-form-number.jsx';
import ThingsboardText from './json-form-text.jsx';
import Select from 'react-schema-form/lib/Select';
import Radios from 'react-schema-form/lib/Radios';
import ThingsboardDate from './json-form-date.jsx';
import ThingsboardImage from './json-form-image.jsx';
import ThingsboardCheckbox from './json-form-checkbox.jsx';
import Help from 'react-schema-form/lib/Help';
import ThingsboardFieldSet from './json-form-fieldset.jsx';

import _ from 'lodash';

class ThingsboardSchemaForm extends React.Component {

    constructor(props) {
        super(props);

        this.mapper = {
            'number': ThingsboardNumber,
            'text': ThingsboardText,
            'password': ThingsboardText,
            'textarea': ThingsboardText,
            'select': Select,
            'radios': Radios,
            'date': ThingsboardDate,
            'image': ThingsboardImage,
            'checkbox': ThingsboardCheckbox,
            'help': Help,
            'array': ThingsboardArray,
            'javascript': ThingsboardJavaScript,
            'json': ThingsboardJson,
            'html': ThingsboardHtml,
            'css': ThingsboardCss,
            'color': ThingsboardColor,
            'rc-select': ThingsboardRcSelect,
            'fieldset': ThingsboardFieldSet
        };

        this.onChange = this.onChange.bind(this);
        this.onColorClick = this.onColorClick.bind(this);
        this.onToggleFullscreen = this.onToggleFullscreen.bind(this);
        this.hasConditions = false;
    }

    onChange(key, val) {
        //console.log('SchemaForm.onChange', key, val);
        this.props.onModelChange(key, val);
        if (this.hasConditions) {
            this.forceUpdate();
        }
    }

    onColorClick(event, key, val) {
        this.props.onColorClick(event, key, val);
    }

    onToggleFullscreen() {
        this.props.onToggleFullscreen();
    }

    
    builder(form, model, index, onChange, onColorClick, onToggleFullscreen, mapper) {
        var type = form.type;
        let Field = this.mapper[type];
        if(!Field) {
            console.log('Invalid field: \"' + form.key[0] + '\"!');
            return null;
        }
        if(form.condition) {
            this.hasConditions = true;
            if (eval(form.condition) === false) {
                return null;
            }
        }
        return <Field model={model} form={form} key={index} onChange={onChange} onColorClick={onColorClick} onToggleFullscreen={onToggleFullscreen} mapper={mapper} builder={this.builder}/>
    }

    createSchema(theForm) {
        let merged = utils.merge(this.props.schema, theForm, this.props.ignore, this.props.option);
        let mapper = this.mapper;
        if(this.props.mapper) {
            mapper = _.merge(this.mapper, this.props.mapper);
        }
        let forms = merged.map(function(form, index) {
            return this.builder(form, this.props.model, index, this.onChange, this.onColorClick, this.onToggleFullscreen, mapper);
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
        if(this.props.groupInfoes&&this.props.groupInfoes.length>0){
            let content=[];
            for(let info of this.props.groupInfoes){
                let forms = this.createSchema(this.props.form[info.formIndex]);
                let item = <ThingsboardSchemaGroup key={content.length} forms={forms} info={info}></ThingsboardSchemaGroup>;
                content.push(item);
            }
            return (<div>{content}</div>);
        }
        else
            return this.createSchema(this.props.form);
    }
}
export default ThingsboardSchemaForm;


class ThingsboardSchemaGroup extends React.Component{
    constructor(props) {
        super(props);
        this.state={
            showGroup:true
        }
    }

    toogleGroup(index) {
        this.setState({
            showGroup:!this.state.showGroup
        });
    }

    render() {
        let theCla = "pull-right fa fa-chevron-down md-toggle-icon"+(this.state.showGroup?"":" tb-toggled")
        return (<section className="md-whiteframe-z1" style={{marginTop: '10px'}}>
                    <div className='SchemaGroupname md-button-toggle' onClick={this.toogleGroup.bind(this)}>{this.props.info.GroupTitle}<span className={theCla}></span></div>
                    <div style={{padding: '20px'}} className={this.state.showGroup?"":"invisible"}>{this.props.forms}</div>
                </section>);
    }
}