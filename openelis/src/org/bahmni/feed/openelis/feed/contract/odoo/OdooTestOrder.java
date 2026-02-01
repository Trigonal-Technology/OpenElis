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

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OdooTestOrder {
    @JsonProperty("sale_order_id")
    private String saleOrderId;
    
    @JsonProperty("sale_order_name")
    private String saleOrderName;
    
    @JsonProperty("order_date")
    private String orderDate;
    
    @JsonProperty("patient")
    private OdooPatient patient;
    
    @JsonProperty("order_lines")
    private List<OdooTestOrderLine> orderLines;

    public OdooTestOrder() {
        this.orderLines = new ArrayList<>();
    }

    public OdooTestOrder(String saleOrderId, String saleOrderName, String orderDate, OdooPatient patient, List<OdooTestOrderLine> orderLines) {
        this.saleOrderId = saleOrderId;
        this.saleOrderName = saleOrderName;
        this.orderDate = orderDate;
        this.patient = patient;
        this.orderLines = orderLines != null ? orderLines : new ArrayList<>();
    }

    public String getSaleOrderId() {
        return saleOrderId;
    }

    public void setSaleOrderId(String saleOrderId) {
        this.saleOrderId = saleOrderId;
    }

    public String getSaleOrderName() {
        return saleOrderName;
    }

    public void setSaleOrderName(String saleOrderName) {
        this.saleOrderName = saleOrderName;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public OdooPatient getPatient() {
        return patient;
    }

    public void setPatient(OdooPatient patient) {
        this.patient = patient;
    }

    public List<OdooTestOrderLine> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<OdooTestOrderLine> orderLines) {
        this.orderLines = orderLines;
    }
}
