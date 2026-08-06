package com.aiagent.financial.infrastructure.persistence.local;

import com.aiagent.financial.domain.model.product.Product;
import com.aiagent.financial.domain.model.product.ProductId;
import com.aiagent.financial.domain.repository.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存产品仓储。
 *
 * <p>模拟产品管理系统，本地运行使用。内置四个演示产品，
 * 后续可替换为对接真实产品系统的实现。</p>
 */
@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<ProductId, Product> products = new ConcurrentHashMap<>();

    /**
     * 构造内存产品仓储，初始化演示数据。
     */
    public InMemoryProductRepository() {
        seedProducts();
    }

    private void seedProducts() {
        products.put(new ProductId("LC-2024-001"), new Product(
                new ProductId("LC-2024-001"), "稳健增长理财产品A", "LC-2024-001",
                "固定收益类", "3.8%", "R2（较低风险）", "180天", "10,000元", true));
        products.put(new ProductId("LC-2024-002"), new Product(
                new ProductId("LC-2024-002"), "灵活配置混合型理财产品B", "LC-2024-002",
                "混合类", "4.5%", "R3（中等风险）", "365天", "50,000元", true));
        products.put(new ProductId("YL-2024-001"), new Product(
                new ProductId("YL-2024-001"), "安心养老目标基金C", "YL-2024-001",
                "养老目标基金", "5.2%", "R3（中等风险）", "长期（建议持有3年以上）", "1,000元", true));
        products.put(new ProductId("HB-2024-001"), new Product(
                new ProductId("HB-2024-001"), "天天利现金管理产品D", "HB-2024-001",
                "货币市场类", "2.5%", "R1（低风险）", "T+0赎回", "1元", true));
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        return Optional.ofNullable(products.get(productId));
    }

    @Override
    public List<Product> findAllOnSale() {
        return products.values().stream().filter(Product::isAvailable).toList();
    }
}
