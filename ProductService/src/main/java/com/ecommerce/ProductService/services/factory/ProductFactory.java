package com.ecommerce.ProductService.services.factory;

import com.ecommerce.ProductService.models.Product;
import com.ecommerce.ProductService.models.enums.ProductCategory;
import java.util.Map;

public class ProductFactory {

    public static Product createProduct(ProductCategory category, long merchantId, Map<String, Object> input) {
        try {
            var method = category.getProductClass().getMethod("create", long.class, Map.class);
            return (Product) method.invoke(null, merchantId, input);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create product for category: " + category, e);
        }
    }
}
