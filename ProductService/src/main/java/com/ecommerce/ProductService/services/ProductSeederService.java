package com.ecommerce.ProductService.services;

import com.ecommerce.ProductService.models.Accessory;
import com.ecommerce.ProductService.models.Clothing;
import com.ecommerce.ProductService.models.Product;
import com.ecommerce.ProductService.models.ProductReview;
import com.ecommerce.ProductService.repositories.ProductRepository;
import com.ecommerce.ProductService.repositories.ProductReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
public class ProductSeederService {

    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;

    public ProductSeederService(ProductRepository productRepository, ProductReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
    }

    public String seedEverything() {
        if (productRepository.count() > 0) return "❌ products have been seeded";

        List<Long> merchantIds = List.of(4L, 5L, 6L);
        int productCounter = 1;

        for (Long merchantId : merchantIds) {
            for (int i = 1; i <= 5; i++) {
                Product product;
                String productName = "Product-" + productCounter++;

                if (i % 2 == 0) {
                    Clothing clothing = new Clothing();
                    clothing.setCommonAttributes(
                            productName, 150.0, "Zara",
                            "Black", merchantId, 30
                    );
                    clothing.setDetails("M", "Cotton", "Male", "Winter");
                    product = productRepository.save(clothing);
                } else {
                    Accessory accessory = new Accessory();
                    accessory.setCommonAttributes(
                            productName, 250.0, "Gucci",
                            "Gold", merchantId, 15
                    );
                    accessory.setDetails("Ring", "Metal", true);
                    product = productRepository.save(accessory);
                }
                seedReview(product.getUid());
            }
        }

        return "✅ Static products and reviews seeded.";
    }

    private void seedReview(Long productId) {

        if (reviewRepository.count() > 0) return;
        List<String> comments = List.of("Top quality.", "Perfect gift.");
        List<Integer> ratings = List.of(5, 4);

        Random random = new Random();

        int randomIndex = random.nextInt(comments.size());  // 0 or 1
        int randomUserId = 1 + random.nextInt(3);            // 1, 2, or 3

        ProductReview review = new ProductReview();
        review.setProductId(productId);
        review.setUserId((long) randomUserId);
        review.setComment(comments.get(randomIndex));
        review.setRating(ratings.get(randomIndex));

        reviewRepository.save(review);
    }

}
