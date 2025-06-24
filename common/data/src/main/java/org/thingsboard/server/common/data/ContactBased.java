/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@EqualsAndHashCode(callSuper = true)
public abstract class ContactBased<I extends UUIDBased> extends BaseDataWithAdditionalInfo<I> implements HasEmail {

    private static final long serialVersionUID = 5047448057830660988L;

    @Length(fieldName = "country")
    @NoXss
    protected String country;
    @Length(fieldName = "state")
    @NoXss
    protected String state;
    @Length(fieldName = "city")
    @NoXss
    protected String city;
    @NoXss
    protected String address;
    @NoXss
    protected String address2;
    @Length(fieldName = "zip or postal code")
    @NoXss
    protected String zip;
    @Length(fieldName = "phone")
    @NoXss
    protected String phone;
    @Length(fieldName = "email")
    @NoXss
    protected String email;

    public ContactBased() {
        super();
    }

    public ContactBased(I id) {
        super(id);
    }

    public ContactBased(ContactBased<I> contact) {
        super(contact);
        this.country = contact.getCountry();
        this.state = contact.getState();
        this.city = contact.getCity();
        this.address = contact.getAddress();
        this.address2 = contact.getAddress2();
        this.zip = contact.getZip();
        this.phone = contact.getPhone();
        this.email = contact.getEmail();
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
