package com.example.tryme.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.tryme.Model.Meal;
import com.example.tryme.Repository.MealRepository;
import com.example.tryme.exception.ResourceNotFoundException;

@Service
public class MealService {
    private final MealRepository mealRepository;
    private final CacheService cacheService;

    public MealService(MealRepository mealRepository, CacheService cacheService) {
        this.mealRepository = mealRepository;
        this.cacheService = cacheService;
    }

    public List<Meal> findMealsByProductName(String productName) {
        String cacheKey = "productName:" + productName;
        List<Meal> cachedMeals = cacheService.getFromCache("meals", cacheKey);
        if (cachedMeals != null) {
            return cachedMeals;
        }
        List<Meal> meals = mealRepository.findMealsByProductName(productName);
        cacheService.putToCache("meals", cacheKey, meals);
        return meals;
    }

    public String createMeal(String mealName) {
        cacheService.clearCache("meals"); 
        Meal meal = new Meal(mealName); 
        mealRepository.save(meal);
        return "Meal '" + mealName + "' created with ID: " + meal.getId();
    }

    public Meal getMeal(Long id) {
        String cacheKey = "id:" + id;
        List<Meal> cachedMealsList = cacheService.getFromCache("meals", cacheKey);
        if (cachedMealsList != null && !cachedMealsList.isEmpty()) {
            return cachedMealsList.get(0);
        }
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found with id: " + id));
        cacheService.putToCache("meals", cacheKey, List.of(meal));
        return meal;
    }

    public String updateMeal(Long id, String newName) {
        cacheService.clearCache("meals"); 
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found with id: " + id + " for update."));
        meal.setName(newName);
        mealRepository.save(meal);
        return "Meal updated to '" + newName + "'";
    }

    public String deleteMeal(Long id) {
        cacheService.clearCache("meals"); 
        Meal meal = mealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found with id: " + id + " for deletion."));
        mealRepository.delete(meal);
        return "Meal deleted";
    }

    public List<Meal> getAllMeals() {
        List<Meal> cachedMeals = cacheService.getFromCache("meals", "all");
        if (cachedMeals != null) {
            return cachedMeals;
        }
        List<Meal> meals = mealRepository.findAll();
        cacheService.putToCache("meals", "all", meals);
        return meals;
    }
}