package com.github.monitor.agent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class AgentIdentityTest {

    private String originalAppName;
    private String originalServiceName;

    @Before
    public void setUp() {
        originalAppName = AgentConfig.appName;
        originalServiceName = AgentConfig.serviceName;
        AgentConfig.appName = "unknown-app";
        AgentConfig.serviceName = "unknown-service";
    }

    @After
    public void tearDown() {
        AgentConfig.appName = originalAppName;
        AgentConfig.serviceName = originalServiceName;
    }

    @Test
    public void shouldDefaultServiceNameToAppNameWhenMissing() {
        AgentConfig.parse("appName=VerifyDemo");

        assertEquals("VerifyDemo", AgentConfig.appName);
        assertEquals("VerifyDemo", AgentConfig.serviceName);
    }

    @Test
    public void shouldDefaultServiceNameToAppNameWhenUnknownServiceNameProvided() {
        AgentConfig.parse("appName=VerifyDemo;serviceName=unknown-service");

        assertEquals("VerifyDemo", AgentConfig.appName);
        assertEquals("VerifyDemo", AgentConfig.serviceName);
    }

    @Test
    public void shouldKeepExplicitServiceName() {
        AgentConfig.parse("appName=VerifyDemo;serviceName=demo");

        assertEquals("VerifyDemo", AgentConfig.appName);
        assertEquals("demo", AgentConfig.serviceName);
    }

    @Test
    public void shouldResolveServiceNameFromConfigInsteadOfPackageName() throws Exception {
        AgentConfig.appName = "VerifyDemo";
        AgentConfig.serviceName = "";
        AgentConfig.normalizeIdentity();

        Method method = EventCollector.class.getDeclaredMethod("resolveServiceName", Class.class);
        method.setAccessible(true);
        String serviceName = (String) method.invoke(null, EventCollector.class);

        assertEquals("VerifyDemo", serviceName);
    }
}
