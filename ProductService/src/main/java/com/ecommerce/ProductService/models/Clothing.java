package com.ecommerce.ProductService.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.Map;

@Entity
@DiscriminatorValue("clothing")
public class Clothing extends Product {
    private String size;
    private String material;
    private String gender;
    private String season;

    public Clothing() {
        super();
    }

    public static Clothing create(long merchantId, Map<String, Object> input) {
        Clothing clothing = new Clothing();

        // Set common attributes
        clothing.setMerchantId(merchantId);
        clothing.setName((String) input.get("name"));
        clothing.setPrice((Double) input.get("price"));
        clothing.setBrand((String) input.get("brand"));
        clothing.setColor((String) input.get("color"));
        clothing.setStockLevel((input.get("stockLevel") != null) ? (Integer) input.get("stockLevel") : 0);

        // Set clothing-specific details
        clothing.setSize((String) input.get("size"));
        clothing.setMaterial((String) input.get("material"));
        clothing.setGender((String) input.get("gender"));
        clothing.setSeason((String) input.get("season"));

        return clothing;
    }
    // Clothing-specific setters
    public void setDetails(String size, String material, String gender, String season) {
        this.size = size;
        this.material = material;
        this.gender = gender;
        this.season = season;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }
}