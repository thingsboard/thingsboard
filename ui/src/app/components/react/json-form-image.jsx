/*
 * Copyright © 2016-2017 The Thingsboard Authors
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
import './json-form-image.scss';

import React from 'react';
import ThingsboardBaseComponent from './json-form-base-component.jsx';
import Dropzone from 'react-dropzone';
import IconButton from 'material-ui/IconButton';

class ThingsboardImage extends React.Component {

    constructor(props) {
        super(props);
        this.onValueChanged = this.onValueChanged.bind(this);
        this.onDrop = this.onDrop.bind(this);
        this.onClear = this.onClear.bind(this);
        var value = props.value ? props.value + '' : null;
        this.state = {
            imageUrl: value
        };
    }

    onValueChanged(value) {
        this.setState({
            imageUrl: value
        });
        this.props.onChangeValidate({
            target: {
                value: value
            }
        });
    }

    onDrop(files) {
        var reader = new FileReader();
        reader.onload = (function(tImg) {
            return function(event) {
                tImg.onValueChanged(event.target.result);
            };
        })(this);
        reader.readAsDataURL(files[0]);
    }

    onClear(event) {
        if (event) {
            event.stopPropagation();
        }
        this.onValueChanged("");
    }

    render() {

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

        var previewComponent;
        if (this.state.imageUrl) {
            previewComponent = <img className="tb-image-preview" src={this.state.imageUrl} />;
        } else {
            previewComponent = <div>No image selected</div>;
        }

        return (
            <div className="tb-container">
                <label className={labelClass}>{this.props.form.title}</label>
                <div className="tb-image-select-container">
                    <div className="tb-image-preview-container">{previewComponent}</div>
                    <div className="tb-image-clear-container">
                        <IconButton className="tb-image-clear-btn" iconClassName="material-icons" tooltip="Clear" onTouchTap={this.onClear}>clear</IconButton>
                    </div>
                    <Dropzone className="tb-dropzone"
                              onDrop={this.onDrop}
                              multiple={false}
                              accept="image/*">
                        <div>Drop an image or click to select a file to upload.</div>
                    </Dropzone>
                </div>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardImage);