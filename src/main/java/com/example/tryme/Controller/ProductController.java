package com.example.tryme.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tryme.Model.Product;
import com.example.tryme.services.CaloriesService;
import com.example.tryme.services.ProductService;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;
    private final CaloriesService caloriesService;

    @Autowired
    public ProductController(ProductService productService, 
                           CaloriesService caloriesService) {
        this.productService = productService;
        this.caloriesService = caloriesService;
    }

    @GetMapping("/CalculateCalories")
    public List<String> calculateCalories(
            @RequestParam Integer productCount,
            @RequestParam String[] food,
            @RequestParam Integer[] gram) {
        return caloriesService.calculateCalories(productCount, food, gram);
    }

    @PostMapping("/create")
    public String createProduct(@RequestParam String name, @RequestParam Integer caloriesPer100g) {
        return productService.createProduct(name, caloriesPer100g);
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @PutMapping("/update/{id}")
    public String updateProduct(@PathVariable Long id, 
                               @RequestParam String name, 
                               @RequestParam Integer caloriesPer100g) {
        return productService.updateProduct(id, name, caloriesPer100g);
    }

    @DeleteMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        return productService.deleteProduct(id);
    }

    @GetMapping("/")
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }
}