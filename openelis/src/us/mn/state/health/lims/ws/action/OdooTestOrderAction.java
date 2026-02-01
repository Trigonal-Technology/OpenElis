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

package us.mn.state.health.lims.ws.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.bahmni.feed.openelis.ObjectMapperRepository;
import org.bahmni.feed.openelis.feed.contract.odoo.OdooTestOrder;
import org.bahmni.feed.openelis.feed.service.impl.OdooTestOrderService;
import us.mn.state.health.lims.common.exception.LIMSRuntimeException;
import us.mn.state.health.lims.ws.WebServiceAction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

public class OdooTestOrderAction extends WebServiceAction {
    private static final Logger logger = LogManager.getLogger(OdooTestOrderAction.class);
    private static final String APPLICATION_JSON = "application/json";
    private OdooTestOrderService odooTestOrderService;

    public OdooTestOrderAction() {
        this(new OdooTestOrderService());
    }

    public OdooTestOrderAction(OdooTestOrderService odooTestOrderService) {
        this.odooTestOrderService = odooTestOrderService;
    }

    @Override
    protected ActionForward performAction(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                          HttpServletResponse response) throws Exception {
        
        // Only accept POST requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            logger.warn("Invalid HTTP method: " + request.getMethod() + " for Odoo test order endpoint");
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return null;
        }

        try {
            // Read JSON payload from request body
            String jsonPayload = readRequestBody(request);
            logger.debug("Received Odoo test order payload: " + jsonPayload);

            // Parse JSON to OdooTestOrder object
            OdooTestOrder odooTestOrder = ObjectMapperRepository.objectMapper.readValue(jsonPayload, OdooTestOrder.class);

            // Validate required fields
            if (odooTestOrder.getSaleOrderId() == null || odooTestOrder.getSaleOrderId().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Missing required field: sale_order_id");
                return null;
            }

            if (odooTestOrder.getPatient() == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Missing required field: patient");
                return null;
            }

            if (odooTestOrder.getOrderLines() == null || odooTestOrder.getOrderLines().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Missing or empty order_lines");
                return null;
            }

            // Process the test order
            odooTestOrderService.processTestOrder(odooTestOrder);

            // Send success response
            sendSuccessResponse(response, "Test order processed successfully");

        } catch (LIMSRuntimeException e) {
            logger.error("Error processing Odoo test order", e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing Odoo test order", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Internal server error: " + e.getMessage());
        }

        return null;
    }

    private String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder jsonPayload = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonPayload.append(line);
            }
        }
        return jsonPayload.toString();
    }

    private void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType(APPLICATION_JSON);
        response.setStatus(HttpServletResponse.SC_OK);
        String jsonResponse = String.format("{\"status\":\"success\",\"message\":\"%s\"}", message);
        response.getWriter().write(jsonResponse);
    }

    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setContentType(APPLICATION_JSON);
        response.setStatus(statusCode);
        String jsonResponse = String.format("{\"status\":\"error\",\"message\":\"%s\"}", escapeJson(message));
        response.getWriter().write(jsonResponse);
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
