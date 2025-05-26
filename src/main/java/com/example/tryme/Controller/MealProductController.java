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

import com.example.tryme.Model.MealProduct;
import com.example.tryme.services.MealProductService;
import com.example.tryme.services.MealService;
import com.example.tryme.services.ProductService;

@RestController
@RequestMapping("/mealProducts")
public class MealProductController {
    private final MealProductService mealProductService;
    private final MealService mealService;
    private final ProductService productService;

    @Autowired
    public MealProductController(MealProductService mealProductService,
                               MealService mealService,
                               ProductService productService) {
        this.mealProductService = mealProductService;
        this.mealService = mealService;
        this.productService = productService;
    }

    @PostMapping("/create")
    public String createMealProduct(
            @RequestParam Integer grams,
            @RequestParam Long mealId,
            @RequestParam Long productId) {
        return mealProductService.createMealProduct(grams, mealId, productId, mealService, productService);
    }

    @GetMapping("/{id}")
    public MealProduct getMealProduct(@PathVariable Long id) {
        return mealProductService.getMealProduct(id);
    }

    @PutMapping("/update/{id}")
    public String updateMealProduct(@PathVariable Long id, @RequestParam Integer grams) {
        return mealProductService.updateMealProduct(id, grams);
    }

    @DeleteMapping("/delete/{id}")
    public String deleteMealProduct(@PathVariable Long id) {
        return mealProductService.deleteMealProduct(id);
    }

    @GetMapping("/")
    public List<MealProduct> getAllMealProducts() {
        return mealProductService.getAllMealProducts();
    }
}