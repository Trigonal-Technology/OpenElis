package us.mn.state.health.lims.labbridge.action;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.bahmni.feed.openelis.ObjectMapperRepository;
import org.hibernate.Query;
import us.mn.state.health.lims.hibernate.HibernateUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ListTestsAction extends Action {

    private final String APPLICATION_JSON = "application/json";

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean orderableOnly = parseBoolean(request.getParameter("orderableOnly"), false);

        StringBuilder sql = new StringBuilder();
        sql.append("select t.id, t.name from test t where t.is_active = 'Y'");
        if (orderableOnly) {
            sql.append(" and (t.orderable is true or t.orderable is null)");
        }
        sql.append(" order by t.name");

        Query q = HibernateUtil.getSession().createSQLQuery(sql.toString());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.list();
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(rows.size());
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", toStringSafe(r[0]));
            row.put("name", toStringSafe(r[1]));
            data.add(row);
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("status", "success");
        payload.put("count", data.size());
        payload.put("data", data);

        response.setContentType(APPLICATION_JSON);
        ObjectMapperRepository.objectMapper.writeValue(response.getWriter(), payload);
        return null;
    }

    private boolean parseBoolean(String value, boolean defaultVal) {
        if (value == null) return defaultVal;
        String v = value.trim().toLowerCase();
        if ("true".equals(v) || "1".equals(v) || "yes".equals(v)) return true;
        if ("false".equals(v) || "0".equals(v) || "no".equals(v)) return false;
        return defaultVal;
    }

    private String toStringSafe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
