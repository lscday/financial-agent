package com.aiagent.financial.domain.repository;

/**
 * 审计日志端口。
 *
 * <p>封装关键操作的审计日志上报，领域层仅依赖此接口，
 * 由基础设施层提供控制台或阿里云 SLS 实现。</p>
 */
public interface AuditLogPort {

    /**
     * 记录审计日志。
     *
     * @param module   模块名
     * @param action   动作描述
     * @param detail   详情
     */
    void log(String module, String action, String detail);
}
