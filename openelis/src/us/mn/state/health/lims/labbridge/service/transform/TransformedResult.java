package us.mn.state.health.lims.labbridge.service.transform;

/**
 * Simple value holder for transformed result data.
 */
public class TransformedResult {

    private final String value;
    private final String type;

    public TransformedResult(String value, String type) {
        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}


