package com.example.tryme.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.tryme.Model.MealProduct;

public interface MealProductRepository extends JpaRepository<MealProduct, Long> {
    @Query("SELECT mp FROM MealProduct mp WHERE mp.meal.id = :mealId")
    List<MealProduct> findByMealId(@Param("mealId") Long mealId);
}