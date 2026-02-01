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
public class OdooTestOrderLine {
    @JsonProperty("product_uuid")
    private String productUuid;
    
    @JsonProperty("product_name")
    private String productName;
    
    @JsonProperty("product_type")
    private String productType; // "Test" or "Panel"
    
    @JsonProperty("quantity")
    private Double quantity;
    
    @JsonProperty("comment")
    private String comment;

    public OdooTestOrderLine() {
    }

    public OdooTestOrderLine(String productUuid, String productName, String productType, Double quantity, String comment) {
        this.productUuid = productUuid;
        this.productName = productName;
        this.productType = productType;
        this.quantity = quantity;
        this.comment = comment;
    }

    public String getProductUuid() {
        return productUuid;
    }

    public void setProductUuid(String productUuid) {
        this.productUuid = productUuid;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
