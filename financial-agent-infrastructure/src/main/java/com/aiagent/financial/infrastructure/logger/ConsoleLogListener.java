package com.aiagent.financial.infrastructure.logger;

import com.aiagent.financial.domain.repository.AuditLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 控制台审计日志实现。
 *
 * <p>将审计日志打印到控制台，用于无阿里云环境的本机调试。
 * 激活条件：非 aliyun profile。</p>
 */
@Component
@Profile("!aliyun")
public class ConsoleLogListener implements AuditLogPort {

    private static final Logger log = LoggerFactory.getLogger(ConsoleLogListener.class);

    @Override
    public void log(String module, String action, String detail) {
        log.info("[AUDIT] 模块={}, 动作={}, 详情={}", module, action, detail);
    }
}
