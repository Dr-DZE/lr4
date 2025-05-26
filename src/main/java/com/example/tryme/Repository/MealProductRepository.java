package com.example.tryme.Repository;

import com.example.tryme.Model.MealProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealProductRepository extends JpaRepository<MealProduct, Long> {
}