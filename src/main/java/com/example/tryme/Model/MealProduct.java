package com.example.tryme.Model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class MealProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer grams;

    @ManyToOne
    @JoinColumn(name = "meal_id")
    @JsonBackReference 
    private Meal meal;

    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonBackReference 
    private Product product;

   
    public MealProduct() {}

  
    public MealProduct(Integer grams, Meal meal, Product product) {
        this.grams = grams;
        this.meal = meal;
        this.product = product;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getGrams() { return grams; }
    public void setGrams(Integer grams) { this.grams = grams; }
    public Meal getMeal() { return meal; }
    public void setMeal(Meal meal) { this.meal = meal; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}