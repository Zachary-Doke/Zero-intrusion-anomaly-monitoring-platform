-- Seed defaults required by platform-server startup paths.
-- Safe to re-run: ON CONFLICT DO NOTHING keeps existing configured values.

INSERT INTO rule_settings (
    id,
    package_patterns,
    deep_sampling_enabled,
    depth_limit,
    length_limit,
    collection_limit,
    default_sample_rate,
    queue_capacity,
    flush_interval_ms,
    ai_base_url,
    ai_api_key,
    ai_model,
    ai_prompt_template,
    trace_keys,
    sensitive_fields,
    version
) VALUES (
    'default',
    'com.github.monitor.demo,com.platform',
    TRUE,
    8,
    1024,
    50,
    1.0,
    10000,
    5000,
    'https://api.deepseek.com/v1',
    '',
    'deepseek-chat',
    $$请分析以下 Java 异常，并输出 JSON。
异常类型: {{exception_class}}
服务: {{service_name}}
摘要: {{summary}}
栈顶方法: {{top_stack_frame}}
方法签名: {{method_signature}}
发生次数: {{occurrence_count}}
首次发生: {{first_seen}}
最近发生: {{last_seen}}
请返回 rootCauseAnalysis、impactScope、troubleshootingSteps、fixSuggestion 四个字段。$$,
    'traceId,requestId',
    'password,token,secret,sensitive',
    1
)
ON CONFLICT (id) DO NOTHING;
