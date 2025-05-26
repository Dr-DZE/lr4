package com.example.tryme.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.tryme.Model.Meal;

public interface MealRepository extends JpaRepository<Meal, Long> {
    @Query("SELECT m FROM Meal m JOIN m.products mp WHERE mp.product.name LIKE %:productName%")
    List<Meal> findMealsByProductName(@Param("productName") String productName);
}