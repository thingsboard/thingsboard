/*
 * Copyright © 2016-2021 The Thingsboard Authors
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
const fs = require('fs');
const fse = require('fs-extra');
const path = require('path');

let _projectRoot = null;


(async() => {
    await fse.move(path.join(projectRoot(), 'target', 'thingsboard-web-ui-linux'),
                   path.join(targetPackageDir('linux'), 'bin', 'tb-web-ui'),
                   {overwrite: true});
    await fse.move(path.join(projectRoot(), 'target', 'thingsboard-web-ui-win.exe'),
                   path.join(targetPackageDir('windows'), 'bin', 'tb-web-ui.exe'),
                   {overwrite: true});
})();


function projectRoot() {
    if (!_projectRoot) {
        _projectRoot = __dirname;
    }
    return _projectRoot;
}

function targetPackageDir(platform) {
    return path.join(projectRoot(), 'target', 'package', platform);
}
