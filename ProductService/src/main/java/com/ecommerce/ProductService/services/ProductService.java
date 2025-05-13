package com.ecommerce.ProductService.services;

import com.ecommerce.ProductService.Dto.UserSessionDTO;
import com.ecommerce.ProductService.models.Product;
import com.ecommerce.ProductService.models.ProductReview;
import com.ecommerce.ProductService.models.enums.ProductCategory;
import com.ecommerce.ProductService.repositories.ProductRepository;
import com.ecommerce.ProductService.repositories.ProductReviewRepository;
import com.ecommerce.ProductService.models.Accessory;
import com.ecommerce.ProductService.models.Clothing;
import com.ecommerce.ProductService.services.factory.ProductFactory;
import com.ecommerce.ProductService.services.observer.ProductSubject;
import com.ecommerce.ProductService.services.observer.StockAlertObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductReviewRepository productReviewRepository;

    private final ProductSubject subject = new ProductSubject();

    private final RedisTemplate<String, UserSessionDTO> sessionRedisTemplate;


    @Autowired
    public ProductService(RedisTemplate<String, UserSessionDTO> sessionRedisTemplate) {
        this.sessionRedisTemplate = sessionRedisTemplate;
    }

    public String getToken(String token) {
        UserSessionDTO userSession=sessionRedisTemplate.opsForValue().get(token);
        assert userSession != null;
        return userSession.toString();
    }
    public UserSessionDTO getUserSessionFromToken(String token) {
        // Retrieve the user session from Redis using the provided token
        UserSessionDTO userSession=sessionRedisTemplate.opsForValue().get(token);

        // Check if the session exists and if the user role is "merchant"
        return userSession;
    }

    public Product createProduct(long merchantId,ProductCategory category, Map<String, Object> input) {
        try {

            // Common attributes

            String name = (String) input.get("name");
            // Check if product name already exists for this merchant
            if (productRepository.existsByMerchantIdAndName(merchantId, name)) {
                throw new IllegalArgumentException("Product with name '" + name + "' already exists for this merchant.");
            }

            Product newProduct = ProductFactory.createProduct(category);
            newProduct.setName((String) input.get("name"));
            newProduct.setPrice(Double.parseDouble(input.get("price").toString()));
            newProduct.setBrand((String) input.get("brand"));
            newProduct.setColor((String) input.get("color"));
            newProduct.setMerchantId(merchantId);
            newProduct.setStockLevel(Integer.parseInt(input.get("stockLevel").toString()));

            // Specific attributes
            if (newProduct instanceof Clothing) {
                Clothing clothing = (Clothing) newProduct;
                clothing.setDetails(
                        (String) input.get("size"),
                        (String) input.get("material"),
                        (String) input.get("gender"),
                        (String) input.get("season")
                );
            } else if (newProduct instanceof Accessory) {
                Accessory accessory = (Accessory) newProduct;
                accessory.setDetails(
                        (String) input.get("type"),
                        (String) input.get("material"),
                        Boolean.parseBoolean(input.get("unisex").toString())
                );
            }

            productRepository.save(newProduct);
            return newProduct;
        } catch (Exception e) {
            throw new RuntimeException("Error creating a new product from input: " + input, e);
        }
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product updateProduct(Long userId, String alertEmail, long uid, Product updatedProduct) {
        Product existingProduct = productRepository.findById(uid)
                .orElseThrow(() -> new RuntimeException("Product not found with UID: " + uid));

        if (!existingProduct.getMerchantId().equals(userId)) {
            throw new RuntimeException("Only the owner of Product can update it");
        }

        // Update common attributes if not null
        if (updatedProduct.getName() != null) {
            existingProduct.setName(updatedProduct.getName());
        }
        if (updatedProduct.getPrice() != 0.0) {
            existingProduct.setPrice(updatedProduct.getPrice());
        }
        if (updatedProduct.getBrand() != null) {
            existingProduct.setBrand(updatedProduct.getBrand());
        }
        if (updatedProduct.getColor() != null) {
            existingProduct.setColor(updatedProduct.getColor());
        }
        if (updatedProduct.getMerchantId() != null) {
            existingProduct.setMerchantId(updatedProduct.getMerchantId());
        }
        if (updatedProduct.getStockLevel() != 0) {
            existingProduct.setStockLevel(updatedProduct.getStockLevel());
        }

        // Handle specific attributes for different product types
        if (existingProduct instanceof Clothing && updatedProduct instanceof Clothing) {
            Clothing clothing = (Clothing) existingProduct;
            Clothing updatedClothing = (Clothing) updatedProduct;

            if (updatedClothing.getSize() != null) {
                clothing.setSize(updatedClothing.getSize());
            }
            if (updatedClothing.getMaterial() != null) {
                clothing.setMaterial(updatedClothing.getMaterial());
            }
            if (updatedClothing.getGender() != null) {
                clothing.setGender(updatedClothing.getGender());
            }
            if (updatedClothing.getSeason() != null) {
                clothing.setSeason(updatedClothing.getSeason());
            }

        } else if (existingProduct instanceof Accessory && updatedProduct instanceof Accessory) {
            Accessory accessory = (Accessory) existingProduct;
            Accessory updatedAccessory = (Accessory) updatedProduct;

            if (updatedAccessory.getType() != null) {
                accessory.setType(updatedAccessory.getType());
            }
            if (updatedAccessory.getMaterial() != null) {
                accessory.setMaterial(updatedAccessory.getMaterial());
            }

            // boolean can't be null — so we need a workaround
            // Example: Assume `isUnisexSet()` is a custom method to know if the value is explicitly set
            if (updatedAccessory.isUnisex() != accessory.isUnisex()) {
                accessory.setUnisex(updatedAccessory.isUnisex());
            }
        }

        return productRepository.save(existingProduct);
    }


    public void deleteProduct(Long userId,Long id) {
        Product product=productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
        if(product.getMerchantId()!=userId){
            throw new RuntimeException("Only the owner of Product can delete it");
        }
        productRepository.deleteById(id);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    public Product addStock(String alertEmail,Long id, int stock) {
        // Find the product by ID
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Update the stock level
        product.setStockLevel(product.getStockLevel()+stock);

        // Save the updated product to the database
        Product updatedProduct = productRepository.save(product);

        return updatedProduct; // Return the updated product
    }
    public void removeStock(String alertEmail,Long id, int stock) {
        // Find the product by ID
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Update the stock level

        if(product.getStockLevel()<stock){
            throw new RuntimeException("Not Enoguh in stock");
        }
        product.setStockLevel(product.getStockLevel()-stock);

        // Save the updated product to the database
        Product updatedProduct = productRepository.save(product);


    }


    public ProductReview addReview(Long productId, ProductReview incomingReview) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Product not found with ID: " + productId);
        }

        // Find existing ProductReview for this product (assuming 1 per product)
        ProductReview reviewDoc = productReviewRepository.findByProductId(productId)
                .stream().findFirst()
                .orElseGet(() -> {
                    ProductReview newReview = new ProductReview();
                    newReview.setProductId(productId);
                    return newReview;
                });

        // Append new reviews/ratings
        if (incomingReview.getReviews() != null) {
            reviewDoc.getReviews().addAll(incomingReview.getReviews());
        }
        if (incomingReview.getRatings() != null) {
            reviewDoc.getRatings().addAll(incomingReview.getRatings());
        }

        return productReviewRepository.save(reviewDoc);
    }


    public List<ProductReview> getReviews(Long productId) {
        return productReviewRepository.findByProductId(productId);
    }

    public double getAverageRating(Long productId) {
        List<ProductReview> reviews = getReviews(productId);
        if (reviews.isEmpty()) return 0.0;

        return reviews.stream()
                .flatMapToInt(review -> review.getRatings().stream().mapToInt(Integer::intValue))
                .average()
                .orElse(0.0);
    }


}
