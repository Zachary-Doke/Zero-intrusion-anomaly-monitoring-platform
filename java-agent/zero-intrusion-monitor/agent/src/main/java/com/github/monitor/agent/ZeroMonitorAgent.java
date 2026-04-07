package com.github.monitor.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class ZeroMonitorAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[MonitorAgent] Starting... Args: " + agentArgs);
        
        try {
            AgentConfig.parse(agentArgs);
            AgentConfig.syncRemoteConfigNow();
            
            // Start reporter thread early
            AsyncReporter.getInstance();
            
            new AgentBuilder.Default()
                .type(buildTypeMatcher())
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(
                            DynamicType.Builder<?> builder,
                            TypeDescription typeDescription,
                            ClassLoader classLoader,
                            JavaModule module,
                            ProtectionDomain protectionDomain) {
                        
                        return builder.visit(
                            Advice.to(ExceptionAdvice.class)
                                  .on(isMethod()
                                      .and(not(isAbstract()))
                                      .and(not(isNative()))
                                      .and(not(isConstructor())) // Optional: skip constructors? No, constructors can throw.
                                      // But constructor advice is tricky (super call). Let's keep it for now.
                                  )
                        );
                    }
                })
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                        System.err.println("[MonitorAgent] Error instrumenting " + typeName + ": " + throwable.getMessage());
                    }
                    
                    @Override
                    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
                        // Verbose
                    }
                })
                .ignore(nameStartsWith("net.bytebuddy.")
                        .or(nameStartsWith("com.github.monitor.agent."))
                        .or(nameStartsWith("java."))
                        .or(nameStartsWith("javax."))
                        .or(nameStartsWith("sun."))
                        .or(nameStartsWith("jdk.")))
                .installOn(inst);
                
            System.out.println("[MonitorAgent] Started successfully.");
            
        } catch (Throwable t) {
            System.err.println("[MonitorAgent] Failed to start: " + t.getMessage());
            t.printStackTrace();
        }
    }
    
    private static ElementMatcher.Junction<TypeDescription> buildTypeMatcher() {
        ElementMatcher.Junction<TypeDescription> matcher = ElementMatchers.none();
        
        if (AgentConfig.packages.isEmpty()) {
            System.err.println("[MonitorAgent] No packages configured to monitor. Instrumentation disabled.");
            return matcher;
        }
        
        for (String pkg : AgentConfig.packages) {
            matcher = matcher.or(nameStartsWith(pkg));
        }
        return matcher;
    }
}
