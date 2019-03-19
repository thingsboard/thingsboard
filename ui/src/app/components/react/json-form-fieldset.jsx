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

class ThingsboardFieldSet extends React.Component {

    render() {
        let forms = this.props.form.items.map(function(form, index){
            return this.props.builder(form, this.props.model, index, this.props.onChange, this.props.onColorClick, this.props.onToggleFullscreen, this.props.mapper, this.props.builder);
        }.bind(this));

        return (
            <div style={{paddingTop: '20px'}}>
                <div className="tb-head-label">
                    {this.props.form.title}
                </div>
                <div>
                    {forms}
                </div>
            </div>
        );
    }
}

export default ThingsboardFieldSet;
