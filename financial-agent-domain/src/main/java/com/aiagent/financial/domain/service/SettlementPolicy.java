package com.aiagent.financial.domain.service;

import com.aiagent.financial.domain.model.settlement.Settlement;
import org.springframework.stereotype.Service;

/**
 * 清算判定领域服务。
 *
 * <p>封装"清算是否成功 / 是否应重试"等清算相关业务规则，
 * 替代散落在清算节点中的字符串判断。业务规则只能写在此处。</p>
 */
@Service
public class SettlementPolicy {

    /**
     * 判断清算结果文本是否表示成功。
     *
     * @param result 清算结果文本
     * @return true 表示清算成功
     */
    public boolean isSuccess(String result) {
        return result != null && (result.contains("成功") || result.contains("completed"));
    }

    /**
     * 判断清算是否应继续重试。
     *
     * <p>清算尚未成功且已尝试次数未超过上限时允许重试。</p>
     *
     * @param settlement 清算实体
     * @param retryCount 已尝试次数
     * @param maxRetries 最大重试次数
     * @return true 表示应重试
     */
    public boolean shouldRetry(Settlement settlement, int retryCount, int maxRetries) {
        return !settlement.isCompleted() && retryCount < maxRetries;
    }
}
