package com.platform.analyze.aggregate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 聚合后的异常指纹视图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FingerprintAggregateDto {
    private String fingerprint;
    private String exceptionClass;
    private String topStackFrame;
    private String methodSignature;
    private Long occurrenceCount;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
}
