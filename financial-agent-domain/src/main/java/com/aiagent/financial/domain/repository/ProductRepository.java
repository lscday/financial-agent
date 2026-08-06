package com.aiagent.financial.domain.repository;

import com.aiagent.financial.domain.model.product.Product;
import com.aiagent.financial.domain.model.product.ProductId;

import java.util.List;
import java.util.Optional;

/**
 * 产品仓储接口。
 *
 * <p>接口定义在领域层，实现在基础设施层（内存 / 真实产品系统）。</p>
 */
public interface ProductRepository {

    /**
     * 按产品代码查询产品。
     *
     * @param productId 产品唯一标识
     * @return 匹配的产品，不存在时返回 {@link Optional#empty()}
     */
    Optional<Product> findById(ProductId productId);

    /**
     * 查询全部在售产品。
     *
     * @return 在售产品列表
     */
    List<Product> findAllOnSale();
}
