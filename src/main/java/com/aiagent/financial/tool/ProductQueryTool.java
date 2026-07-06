package com.aiagent.financial.tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 金融产品查询工具函数。
 * 模拟对接产品管理系统，支持大模型自主调取业务数据。
 */

@Component
public class ProductQueryTool {

    private static final Logger log = LoggerFactory.getLogger(ProductQueryTool.class);

    private static final Map<String, String> PRODUCT_DATABASE = Map.of(
            "理财产品A", """
                    产品名称：稳健增长理财产品A
                    产品代码：LC-2024-001
                    产品类型：固定收益类
                    预期年化收益率：3.8%
                    风险等级：R2（较低风险）
                    投资期限：180天
                    起投金额：10,000元
                    产品状态：在售
                    """,
            "理财产品B", """
                    产品名称：灵活配置混合型理财产品B
                    产品代码：LC-2024-002
                    产品类型：混合类
                    预期年化收益率：4.5%
                    风险等级：R3（中等风险）
                    投资期限：365天
                    起投金额：50,000元
                    产品状态：在售
                    """,
            "养老金产品C", """
                    产品名称：安心养老目标基金C
                    产品代码：YL-2024-001
                    产品类型：养老目标基金
                    预期年化收益率：5.2%
                    风险等级：R3（中等风险）
                    投资期限：长期（建议持有3年以上）
                    起投金额：1,000元
                    产品状态：在售（享受税收优惠）
                    """,
            "现金管理D", """
                    产品名称：天天利现金管理产品D
                    产品代码：HB-2024-001
                    产品类型：货币市场类
                        预期年化收益率：2.5%
                    风险等级：R1（低风险）
                    投资期限：T+0赎回
                    起投金额：1元
                    产品状态：在售
                    """
    );

    /**
     * 查询产品信息。
     *
     * @param query 用户的查询文本
     * @param businessType 业务类型（wealth、pension 等）
     * @return 格式化后的产品信息
     */
    public String query(String query, String businessType) {
        log.info("产品查询：businessType={}, query={}", businessType, query);

        // 尝试查找匹配的产品
        StringBuilder result = new StringBuilder();
        result.append("【产品数据查询结果】\n");
        result.append("查询时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        boolean found = false;
        for (Map.Entry<String, String> entry : PRODUCT_DATABASE.entrySet()) {
            String productName = entry.getKey();
            String productInfo = entry.getValue();

            // 简单的关键词匹配
            if (query != null && (productName.contains(query) || query.contains(productName)
                    || query.contains("全部") || query.contains("所有"))) {
                result.append("--- ").append(productName).append(" ---\n");
                result.append(productInfo).append("\n");
                found = true;
            }
        }

        // 按业务类型匹配
        if (!found && businessType != null) {
            if (businessType.contains("wealth") || businessType.contains("理财")) {
                result.append(PRODUCT_DATABASE.get("理财产品A")).append("\n");
                result.append(PRODUCT_DATABASE.get("理财产品B")).append("\n");
                found = true;
            } else if (businessType.contains("pension") || businessType.contains("养老")) {
                result.append(PRODUCT_DATABASE.get("养老金产品C")).append("\n");
                found = true;
            }
        }

        if (!found) {
            result.append("未找到匹配的产品信息，以下为可选产品：\n");
            PRODUCT_DATABASE.keySet().forEach(name -> result.append("- ").append(name).append("\n"));
        }

        return result.toString();
    }

    /**
     * 按名称获取产品详情。
     */
    public String getProductDetail(String productName) {
        return PRODUCT_DATABASE.getOrDefault(productName, "未找到产品: " + productName);
    }
}
