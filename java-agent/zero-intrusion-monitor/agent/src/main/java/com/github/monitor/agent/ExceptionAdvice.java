package com.github.monitor.agent;

import net.bytebuddy.asm.Advice;
import java.lang.reflect.Method;

public class ExceptionAdvice {

    @Advice.OnMethodEnter
    public static Object[] onEnter(@Advice.AllArguments Object[] args) {
        return args; // Capture arguments array (shallow copy of references)
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, backupArguments = false)
    public static void onExit(
            @Advice.Enter Object[] args,
            @Advice.This(optional = true) Object thisObj,
            @Advice.Origin Method method,
            @Advice.Thrown Throwable throwable) {
            
        if (throwable != null) {
            try {
                // We use reflection/static call to EventCollector to avoid direct dependency if loaded by different classloaders?
                // But since we are in the same agent jar and shaded, direct call is fine *if* EventCollector is visible.
                // EventCollector is in agent jar, loaded by AppClassLoader (if added to classpath) or Agent ClassLoader?
                // If agent is in boot classpath, it's visible.
                // If agent is on system classpath (-javaagent), it is loaded by AppClassLoader.
                // If we use AgentBuilder with specific class loading strategy, we need to be careful.
                // For this "0 to 1" example, simple direct call is best.
                
                EventCollector.onException(
                    method.getDeclaringClass(),
                    method.getName(),
                    method.toString(), // detailed description
                    args,
                    thisObj,
                    throwable
                );
            } catch (Throwable t) {
                // Swallow internal agent errors
            }
        }
    }
}
