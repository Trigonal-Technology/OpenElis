package us.mn.state.health.lims.labbridge.service.transform;

import us.mn.state.health.lims.test.valueholder.Test;

/**
 * Hook for REST-only result transformations.
 *
 * Implementations may adjust the incoming result value and/or type for specific
 * tests before the core ResultUpdateService logic normalizes and persists them.
 *
 * Default behavior should be a no-op (return the original value and type)
 * so that adding this layer is non-breaking.
 */
public interface ResultValueTransformer {

    TransformedResult transform(Test test, String value, String type);
}


