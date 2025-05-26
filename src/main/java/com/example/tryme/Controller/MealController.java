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

import com.example.tryme.Model.Meal;
import com.example.tryme.services.MealService;

@RestController
@RequestMapping("/meals")
public class MealController {
    private final MealService mealService;

    @Autowired
    public MealController(MealService mealService) {
        this.mealService = mealService;
    }

    @GetMapping("/by-product")
    public List<Meal> getMealsByProduct(@RequestParam String productName) {
        return mealService.findMealsByProductName(productName);
    }

    @PostMapping("/create")
    public String createMeal(@RequestParam String mealName) {
        return mealService.createMeal(mealName);
    }

    @GetMapping("/{id}")
    public Meal getMeal(@PathVariable Long id) {
        return mealService.getMeal(id);
    }

    @PutMapping("/update/{id}")
    public String updateMeal(@PathVariable Long id, @RequestParam String newName) {
        return mealService.updateMeal(id, newName);
    }

    @DeleteMapping("/delete/{id}")
    public String deleteMeal(@PathVariable Long id) {
        return mealService.deleteMeal(id);
    }

    @GetMapping("/")
    public List<Meal> getAllMeals() {
        return mealService.getAllMeals();
    }
}