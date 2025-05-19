package com.ecommerce.ProductService.services;

import com.ecommerce.ProductService.Clients.UserServiceFeignClient;
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
import java.util.Set;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductReviewRepository productReviewRepository;

    private final ProductSubject subject = new ProductSubject();

    private final RedisTemplate<String, UserSessionDTO> sessionRedisTemplate;

    @Autowired
    private UserServiceFeignClient userServiceFeignClient;

    @Autowired
    public ProductService(StockAlertObserver observer, RedisTemplate<String, UserSessionDTO> sessionRedisTemplate, UserServiceFeignClient userServiceFeignClient) {
        this.sessionRedisTemplate = sessionRedisTemplate;
        subject.registerObserver(observer);
        this.userServiceFeignClient = userServiceFeignClient;
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

            Product newProduct = ProductFactory.createProduct(category,merchantId,input);
            productRepository.save(newProduct);
            return newProduct;
        } catch (Exception e) {
            throw new RuntimeException("Error creating a new product from input: " + input, e);
        }
    }
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product updateProduct(Long userId, String alertEmail, long uid, Object updatedData) {
        Product existingProduct = productRepository.findById(uid)
                .orElseThrow(() -> new RuntimeException("Product not found with UID: " + uid));

        if (!existingProduct.getMerchantId().equals(userId)) {
            throw new RuntimeException("Only the owner of the product can update it");
        }

        if (!(updatedData instanceof Map)) {
            throw new IllegalArgumentException("Updated data is not a valid map.");
        }

        Map<String, Object> data = (Map<String, Object>) updatedData;

        // Define clothing-specific and accessory-specific fields
        Set<String> clothingFields = Set.of("size", "gender", "season");
        Set<String> accessoryFields = Set.of("type", "unisex");

        boolean hasClothingField = data.keySet().stream().anyMatch(clothingFields::contains);
        boolean hasAccessoryField = data.keySet().stream().anyMatch(accessoryFields::contains);

        // Check type and enforce no cross attributes
        if (existingProduct instanceof Clothing) {
            if (hasAccessoryField) {
                throw new IllegalArgumentException("Accessory attributes provided for a Clothing product.");
            }
        } else if (existingProduct instanceof Accessory) {
            if (hasClothingField) {
                throw new IllegalArgumentException("Clothing attributes provided for an Accessory product.");
            }
        } else {
            throw new IllegalArgumentException("Unknown product type.");
        }

        // Update common fields (same as before)
        if (data.containsKey("name")) {
            existingProduct.setName((String) data.get("name"));
        }
        if (data.containsKey("price")) {
            existingProduct.setPrice(Double.parseDouble(data.get("price").toString()));
        }
        if (data.containsKey("brand")) {
            existingProduct.setBrand((String) data.get("brand"));
        }
        if (data.containsKey("color")) {
            existingProduct.setColor((String) data.get("color"));
        }
        if (data.containsKey("merchantId")) {
            existingProduct.setMerchantId(Long.parseLong(data.get("merchantId").toString()));
        }
        if (data.containsKey("stockLevel")) {
            existingProduct.setStockLevel(Integer.parseInt(data.get("stockLevel").toString()));
        }

        // Update specific fields
        if (existingProduct instanceof Clothing) {
            Clothing clothing = (Clothing) existingProduct;

            if (data.containsKey("size")) {
                clothing.setSize((String) data.get("size"));
            }
            if (data.containsKey("material")) {
                clothing.setMaterial((String) data.get("material"));
            }
            if (data.containsKey("gender")) {
                clothing.setGender((String) data.get("gender"));
            }
            if (data.containsKey("season")) {
                clothing.setSeason((String) data.get("season"));
            }
        } else { // Accessory
            Accessory accessory = (Accessory) existingProduct;

            if (data.containsKey("type")) {
                accessory.setType((String) data.get("type"));
            }
            if (data.containsKey("material")) {
                accessory.setMaterial((String) data.get("material"));
            }
            if (data.containsKey("unisex")) {
                accessory.setUnisex(Boolean.parseBoolean(data.get("unisex").toString()));
            }
        }

        productRepository.save(existingProduct);

        if (subject != null) {
            subject.notifyObservers(alertEmail, existingProduct);
        }

        return existingProduct;
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

    public Product addStock(Long id, int stock) {
        // Find the product by ID
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Update the stock level
        product.setStockLevel(product.getStockLevel()+stock);

        // Save the updated product to the database
        Product updatedProduct = productRepository.save(product);

        String alertEmail= userServiceFeignClient.getUserEmailById(product.getMerchantId());
        // Notify observers (make sure subject is properly initialized)
        if (subject != null) {
            subject.notifyObservers(alertEmail,updatedProduct);
        }

        return updatedProduct; // Return the updated product
    }
    public void removeStock(Long id, int stock) {
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

        String alertEmail= userServiceFeignClient.getUserEmailById(product.getMerchantId());

        // Notify observers (make sure subject is properly initialized)
        if (subject != null) {
            subject.notifyObservers(alertEmail,updatedProduct);
        }

    }


    public ProductReview addReview(Long userId,Long productId, ProductReview incomingReview) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Product not found with ID: " + productId);
        }

        if (incomingReview.getRating() == null ) {
            throw new IllegalArgumentException("Review must have rating");
        }

        boolean alreadyReviewed = productReviewRepository.existsByProductIdAndUserId(productId, userId);
        if (alreadyReviewed) {
            throw new IllegalStateException("User has already reviewed this product");
        }
        incomingReview.setUserId(userId);
        incomingReview.setProductId(productId);

        return productReviewRepository.save(incomingReview);
    }

    public List<ProductReview> getReviews(Long productId) {
        return productReviewRepository.findByProductId(productId);
    }

    public double getAverageRating(Long productId) {
        List<ProductReview> reviews = getReviews(productId);

        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }

        int totalRating = 0;
        int numberOfReviews = 0;

        for (ProductReview review : reviews) {
            if (review.getRating() != null) {
                totalRating += review.getRating();
                numberOfReviews++;
            }
        }

        if (numberOfReviews == 0) {
            return 0.0;
        }

        return (double) totalRating / numberOfReviews;
    }
    public ProductReview editRating(Long userId, Integer newRating,Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Product not found with ID: " + productId);
        }

        if (!productReviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new IllegalArgumentException("You never reviewed this product");
        }
        ProductReview review = productReviewRepository.getProductReviewByProductIdAndUserId(productId, userId);

        review.setRating(newRating);
        return productReviewRepository.save(review);
    }
    public List<Product> filterProductsByPrice(double min, double max) {
        return productRepository.findByPriceBetween(min, max);
    }


}
