package us.mn.state.health.lims.labbridge.service.transform;

import java.math.BigDecimal;

import us.mn.state.health.lims.test.valueholder.Test;

/**
 * Default implementation of ResultValueTransformer.
 *
 * - Currently only targets the Platelet Count test as a starting point.
 * - By default, the transformation is effectively a no-op (factor = 1),
 *   so simply introducing this class is non-breaking.
 *
 * To enable actual scaling for Platelet Count, adjust PLATELET_SCALING_FACTOR
 * (and, if needed, the operation) below based on your clinical requirement.
 */
public class ConfigurableResultValueTransformer implements ResultValueTransformer {

    // Canonical test ID for Platelet Count in OpenELIS.
    private static final String PLATELET_TEST_ID = "1482";

    // Scaling factor for Platelet Count: divide incoming result by 100.
    private static final BigDecimal PLATELET_SCALING_FACTOR = new BigDecimal("0.01");

    @Override
    public TransformedResult transform(Test test, String value, String type) {
        // Defensive: if we can't reason about it, don't touch it.
        if (test == null || value == null) {
            return new TransformedResult(value, type);
        }

        String testId = safeTrim(test.getId());

        // For now we only care about Platelet Count; everything else passes through untouched.
        if (!equalsIgnoreCase(testId, PLATELET_TEST_ID)) {
            return new TransformedResult(value, type);
        }

        // Only attempt numeric transformation when resultType is numeric or unspecified.
        String effectiveType = (type == null || type.trim().isEmpty()) ? "N" : type;
        if (!"N".equalsIgnoreCase(effectiveType)) {
            // Dictionary or remark types must not be altered.
            return new TransformedResult(value, type);
        }

        // Try to parse and scale the numeric value; on any error, leave it unchanged.
        try {
            BigDecimal numeric = new BigDecimal(value.trim());

            // Currently a simple multiply; factor is 1 by default (no-op).
            BigDecimal scaled = numeric.multiply(PLATELET_SCALING_FACTOR);

            return new TransformedResult(scaled.toPlainString(), effectiveType.toUpperCase());
        } catch (Exception e) {
            // Parsing or scaling failed: fail safe by returning the original value and type.
            return new TransformedResult(value, type);
        }
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equalsIgnoreCase(b);
    }
}


