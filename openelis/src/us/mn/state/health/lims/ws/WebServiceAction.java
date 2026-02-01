package us.mn.state.health.lims.ws;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.mn.state.health.lims.login.action.LoginValidateAction;
import us.mn.state.health.lims.login.dao.LoginDAO;
import us.mn.state.health.lims.login.dao.UserModuleDAO;
import us.mn.state.health.lims.login.daoimpl.LoginDAOImpl;
import us.mn.state.health.lims.login.daoimpl.UserModuleDAOImpl;
import us.mn.state.health.lims.login.valueholder.Login;
import phl.util.Crypto;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static us.mn.state.health.lims.common.action.IActionConstants.FWD_SUCCESS;

public class WebServiceAction extends Action {

    private final Logger logger = LoggerFactory.getLogger(WebServiceAction.class);
    private UserModuleDAO userModuleDAO = new UserModuleDAOImpl();
    private LoginValidateAction loginValidateAction = new LoginValidateAction();

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        if (!userModuleDAO.isSessionExpired(request) || authorized(mapping, form, request, response)) {
            return performAction(mapping, form, request, response);
        }
        return invalidAccessAttempt(form, request, response);
    }

    private boolean authorized(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        // Support Basic Authentication for stateless REST requests
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String base64Credentials = authHeader.substring("Basic ".length()).trim();
                byte[] credDecoded = java.util.Base64.getDecoder().decode(base64Credentials);
                String credentials = new String(credDecoded, "UTF-8");
                final String[] values = credentials.split(":", 2);
                if (values.length == 2) {
                    String username = values[0];
                    String password = values[1];
                    Login login = new Login();
                    login.setLoginName(username);
                    login.setPassword(password);
                    Login validatedLogin = new us.mn.state.health.lims.login.daoimpl.LoginDAOImpl()
                            .getValidateLogin(login);
                    if (validatedLogin != null) {
                        return true;
                    }
                }
            } catch (Exception e) {
                logger.error("Error during Basic Auth validation", e);
            }
        }

        ActionForward actionForward = loginValidateAction.execute(mapping, form, request, response);
        return actionForward != null && FWD_SUCCESS.equals(actionForward.getName());
    }

    private ActionForward invalidAccessAttempt(ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {
        String loginName = "unknown";
        if (form instanceof DynaActionForm) {
            DynaActionForm dynaActionForm = (DynaActionForm) form;
            loginName = (String) dynaActionForm.get("loginName");
        }
        logger.debug(String.format("Unauthorized web service access attempt from %s using username %s", request
                .getRemoteAddr(), loginName));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return null;
    }

    protected ActionForward performAction(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        return null;
    }
}
