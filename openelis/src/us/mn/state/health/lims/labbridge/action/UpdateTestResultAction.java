package us.mn.state.health.lims.labbridge.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.bahmni.feed.openelis.ObjectMapperRepository;
import us.mn.state.health.lims.labbridge.service.ResultUpdateService;
import us.mn.state.health.lims.login.valueholder.UserSessionData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class UpdateTestResultAction extends Action {
    private final String APPLICATION_JSON = "application/json";
    private ResultUpdateService resultUpdateService = new ResultUpdateService();

    private static class UpdatePayload {
        public String analysisId;
        public String testId;
        public String resultValue;
        public String resultType;
    }

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        UpdatePayload payload = parsePayload(request);
        if (payload == null || payload.analysisId == null || payload.testId == null || payload.resultValue == null) {
            return writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters: analysisId, testId, resultValue");
        }

        try {
            // Get system user ID from session
            String sysUserId = "1"; // Default system user
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute("userSessionData");
            if (usd != null) {
                sysUserId = String.valueOf(usd.getSystemUserId());
            }

            // Use the service layer that follows existing UI patterns
            ResultUpdateService.ResultUpdateResponse serviceResponse = resultUpdateService.updateTestResult(
                payload.analysisId, 
                payload.testId, 
                payload.resultValue, 
                payload.resultType,
                sysUserId
            );

            if (!serviceResponse.success) {
                return writeError(response, HttpServletResponse.SC_BAD_REQUEST, serviceResponse.message);
            }

            // Create success response in the same format as before
            Map<String, Object> resp = new LinkedHashMap<String, Object>();
            resp.put("status", "success");
            resp.put("message", serviceResponse.message);
            
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("analysisId", serviceResponse.analysisId);
            data.put("testId", serviceResponse.testId);
            data.put("testName", serviceResponse.testName);
            data.put("accessionNumber", serviceResponse.accessionNumber);
            data.put("resultValue", serviceResponse.resultValue);
            data.put("resultType", serviceResponse.resultType);
            data.put("statusUpdated", serviceResponse.statusUpdated);
            data.put("rowsAffected", serviceResponse.rowsAffected);
            resp.put("data", data);
            
            response.setContentType(APPLICATION_JSON);
            ObjectMapperRepository.objectMapper.writeValue(response.getWriter(), resp);
            return null;
            
        } catch (Exception e) {
            return writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error updating test result: " + e.getMessage());
        }
    }

    private UpdatePayload parsePayload(HttpServletRequest request) throws IOException {
        ObjectMapper mapper = ObjectMapperRepository.objectMapper;
        return mapper.readValue(request.getInputStream(), UpdatePayload.class);
    }

    private ActionForward writeError(HttpServletResponse response, int status, String message) throws IOException {
        Map<String, Object> resp = new LinkedHashMap<String, Object>();
        resp.put("status", "error");
        resp.put("message", message);
        response.setStatus(status);
        response.setContentType(APPLICATION_JSON);
        ObjectMapperRepository.objectMapper.writeValue(response.getWriter(), resp);
        return null;
    }
}


