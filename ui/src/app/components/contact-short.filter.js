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
export default angular.module('thingsboard.filters.contactShort', [])
    .filter('contactShort', ContactShort)
    .name;

/*@ngInject*/
function ContactShort($filter) {
    return function (contact) {
        var contactShort = '';
        if (contact) {
            if (contact.address) {
                contactShort += contact.address;
                contactShort += ' ';
            }
            if (contact.address2) {
                contactShort += contact.address2;
                contactShort += ' ';
            }
            if (contact.city) {
                contactShort += contact.city;
                contactShort += ' ';
            }
            if (contact.state) {
                contactShort += contact.state;
                contactShort += ' ';
            }
            if (contact.zip) {
                contactShort += contact.zip;
                contactShort += ' ';
            }
            if (contact.country) {
                contactShort += contact.country;
            }
        }
        if (contactShort === '') {
            contactShort = $filter('translate')('contact.no-address');
        }
        return contactShort;
    };
}
