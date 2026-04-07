package com.github.monitor.agent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SensitiveDataSanitizerTest {

    private Set<String> originalSensitiveFields;

    @Before
    public void setUp() {
        originalSensitiveFields = new LinkedHashSet<String>(AgentConfig.sensitiveFields);
        AgentConfig.sensitiveFields = new LinkedHashSet<String>();
    }

    @After
    public void tearDown() {
        AgentConfig.sensitiveFields = originalSensitiveFields;
    }

    @Test
    public void shouldMaskSensitiveKeyEvenWhenRuntimeFieldsAreEmpty() {
        String sanitized = SensitiveDataSanitizer.sanitizeText(
                "Invalid argument with data: {name=test-user, id=123, sensitive=password123}"
        );

        assertFalse(sanitized.contains("password123"));
        assertTrue(sanitized.contains("sensitive=***"));
    }
}
