/*
* The contents of this file are subject to the Mozilla Public License
* Version 1.1 (the "License"); you may not use this file except in
* compliance with the License. You may obtain a copy of the License at
* http://www.mozilla.org/MPL/ 
* 
* Software distributed under the License is distributed on an "AS IS"
* basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
* License for the specific language governing rights and limitations under
* the License.
* 
* The Original Code is OpenELIS code.
* 
* Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
*/

package org.bahmni.feed.openelis.feed.contract.odoo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OdooPatient {
    @JsonProperty("ref")
    private String ref; // Patient identifier

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("email")
    private String email;

    @JsonProperty("birthdate")
    private String birthdate; // ISO format date string (YYYY-MM-DD)

    @JsonProperty("gender")
    private String gender; // Male, Female, Other

    @JsonProperty("primary_relative")
    private String primaryRelative;

    @JsonProperty("occupation")
    private String occupation;

    @JsonProperty("address")
    private java.util.Map<String, String> address;

    public OdooPatient() {
    }

    public OdooPatient(String ref, String uuid, String name, String phone, String email) {
        this.ref = ref;
        this.uuid = uuid;
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    public OdooPatient(String ref, String uuid, String name, String phone, String email, String birthdate) {
        this.ref = ref;
        this.uuid = uuid;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.birthdate = birthdate;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPrimaryRelative() {
        return primaryRelative;
    }

    public void setPrimaryRelative(String primaryRelative) {
        this.primaryRelative = primaryRelative;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public java.util.Map<String, String> getAddress() {
        return address;
    }

    public void setAddress(java.util.Map<String, String> address) {
        this.address = address;
    }
}
