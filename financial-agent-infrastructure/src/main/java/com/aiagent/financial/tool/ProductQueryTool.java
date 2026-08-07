package com.aiagent.financial.tool;
import com.aiagent.financial.domain.model.product.Product;
import com.aiagent.financial.domain.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 金融产品查询工具函数。
 * 模拟对接产品管理系统，支持大模型自主调取业务数据。
 *
 * <p>数据源来自 {@link ProductRepository}（领域仓储接口），
 * 当前由内存实现提供模拟产品，后续可替换为真实产品系统。</p>
 */
@Component
public class ProductQueryTool {

    private static final Logger log = LoggerFactory.getLogger(ProductQueryTool.class);

    private final ProductRepository productRepository;

    /**
     * 构造产品查询工具。
     *
     * @param productRepository 产品仓储（领域接口）
     */
    public ProductQueryTool(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 查询产品信息。
     *
     * @param query        用户的查询文本
     * @param businessType 业务类型（wealth、pension 等）
     * @return 格式化后的产品信息
     */
    public String query(String query, String businessType) {
        log.info("产品查询：businessType={}, query={}", businessType, query);

        StringBuilder result = new StringBuilder();
        result.append("【产品数据查询结果】\n");
        result.append("查询时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        List<Product> products = productRepository.findAllOnSale();
        boolean found = false;

        // 关键词匹配产品名
        for (Product product : products) {
            if (matches(query, product.getName())) {
                result.append("--- ").append(product.getName()).append(" ---\n");
                result.append(formatProduct(product)).append("\n");
                found = true;
            }
        }

        // 按业务类型匹配
        if (!found && businessType != null) {
            for (Product product : products) {
                if (matchesBusinessType(product, businessType)) {
                    result.append("--- ").append(product.getName()).append(" ---\n");
                    result.append(formatProduct(product)).append("\n");
                    found = true;
                }
            }
        }

        if (!found) {
            result.append("未找到匹配的产品信息，以下为可选产品：\n");
            products.forEach(p -> result.append("- ").append(p.getName()).append("\n"));
        }

        return result.toString();
    }

    /**
     * 按名称获取产品详情。
     *
     * @param productName 产品名称
     * @return 格式化后的产品信息
     */
    public String getProductDetail(String productName) {
        return productRepository.findAllOnSale().stream()
                .filter(p -> p.getName().contains(productName))
                .findFirst()
                .map(this::formatProduct)
                .orElse("未找到产品: " + productName);
    }

    private boolean matches(String query, String productName) {
        return query != null && (productName.contains(query) || query.contains(productName)
                || query.contains("全部") || query.contains("所有"));
    }

    private boolean matchesBusinessType(Product product, String businessType) {
        if (businessType.contains("wealth") || businessType.contains("理财")) {
            return product.getType().contains("固定收益") || product.getType().contains("混合");
        }
        if (businessType.contains("pension") || businessType.contains("养老")) {
            return product.getName().contains("养老");
        }
        return false;
    }

    private String formatProduct(Product product) {
        return """
                产品名称：%s
                产品代码：%s
                产品类型：%s
                预期年化收益率：%s
                风险等级：%s
                投资期限：%s
                起投金额：%s
                产品状态：%s
                """.formatted(
                product.getName(),
                product.getCode(),
                product.getType(),
                product.getExpectedRate(),
                product.getRiskLevel(),
                product.getTerm(),
                product.getMinInvestment(),
                product.isAvailable() ? "在售" : "停售");
    }
}
