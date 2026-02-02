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
public class OdooTest {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("code")
    private String code;

    @JsonProperty("description")
    private String description;

    @JsonProperty("category")
    private String category;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("list_price")
    private Double listPrice;

    @JsonProperty("is_panel")
    private Boolean isPanel;

    @JsonProperty("test_uuids")
    private java.util.List<String> testUuids;

    @JsonProperty("elis_department")
    private String department;

    @JsonProperty("elis_sample_type")
    private String sampleType;

    @JsonProperty("elis_result_type")
    private String resultType;

    @JsonProperty("elis_uom")
    private String uom;

    @JsonProperty("elis_reference_range")
    private String referenceRange;

    @JsonProperty("elis_loinc")
    private String loinc;

    @JsonProperty("elis_sort_order")
    private Integer sortOrder;

    public OdooTest() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Double getListPrice() {
        return listPrice;
    }

    public void setListPrice(Double listPrice) {
        this.listPrice = listPrice;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public void setReferenceRange(String referenceRange) {
        this.referenceRange = referenceRange;
    }

    public String getLoinc() {
        return loinc;
    }

    public void setLoinc(String loinc) {
        this.loinc = loinc;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsPanel() {
        return isPanel != null && isPanel;
    }

    public void setIsPanel(Boolean isPanel) {
        this.isPanel = isPanel;
    }

    public java.util.List<String> getTestUuids() {
        return testUuids;
    }

    public void setTestUuids(java.util.List<String> testUuids) {
        this.testUuids = testUuids;
    }
}
