package com.platform.analyze.service;

import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;

/**
 * 异常指纹生成器
 */
@Service
public class FingerprintGenerator {

    /**
     * 生成异常指纹
     * 算法：fingerprint = hash(exceptionClass + normalizedTopStackFrame + methodSignature)
     */
    public String generate(String exceptionClass, String topStackFrame, String methodSignature) {
        String normalizedTopStackFrame = normalizeStackFrame(topStackFrame);
        String raw = exceptionClass + "|" + normalizedTopStackFrame + "|" + methodSignature;
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 归一化栈顶帧，例如去除具体的行号信息
     */
    private String normalizeStackFrame(String stackFrame) {
        if (stackFrame == null) {
            return "";
        }
        // 假设栈帧格式为 com.example.MyClass.myMethod(MyClass.java:123)
        // 去除行号 :123 变为 com.example.MyClass.myMethod(MyClass.java)
        return stackFrame.replaceAll(":\\d+\\)", ")");
    }
}
