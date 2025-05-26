package com.example.tryme.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.example.tryme.Model.Meal;
import com.example.tryme.Model.MealProduct;
import com.example.tryme.Model.Product;
import com.example.tryme.Repository.MealProductRepository;
import com.example.tryme.Repository.MealRepository;
import com.example.tryme.Repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CaloriesService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ProductRepository productRepository;
    private final MealRepository mealRepository;
    private final MealProductRepository mealProductRepository;
    private final CacheService cacheService;

    public CaloriesService(ProductRepository productRepository, 
                         MealRepository mealRepository,
                         MealProductRepository mealProductRepository,
                         CacheService cacheService) {
        this.productRepository = productRepository;
        this.mealRepository = mealRepository;
        this.mealProductRepository = mealProductRepository;
        this.cacheService = cacheService;
    }

    private String sendPostRequest(String query) {
        String url = "https://calculat.ru/wp-content/themes/EmptyCanvas/db123.php";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("term", query);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
        
        return response.getBody();
    }

    public List<String> calculateCalories(Integer productCount, String[] food, Integer[] gram) {
        String cacheKey = String.join(":", food) + ":" + String.join(":", Arrays.stream(gram).map(String::valueOf).toArray(String[]::new));
        List<String> cachedResult = cacheService.getFromCache("calories", cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        List<String> listOfProducts = new ArrayList<>();
        Integer[] caloriesIn100 = new Integer[productCount];
        Integer totalCalories = 0;

        Meal meal = new Meal("Meal " + new Date());
        mealRepository.save(meal);

        for (int i = 0; i < productCount; i++) {
            String response = getNameFromWeb(food[i], caloriesIn100, i);
            String temp = gram[i] + "g." + " " + response;
            totalCalories += caloriesIn100[i] * gram[i] / 100;
            listOfProducts.add(temp);

            Product product = productRepository.findByNameContainingIgnoreCase(food[i]).get(0);
            MealProduct mealProduct = new MealProduct(gram[i], meal, product);
            mealProductRepository.save(mealProduct);
        }

        listOfProducts.add("Total calories: " + totalCalories);
        cacheService.putToCache("calories", cacheKey, listOfProducts);
        return listOfProducts;
    }

    private String getNameFromWeb(String query, Integer[] caloriesIn100, Integer numberOfFood) {    
        try {
            String body = this.sendPostRequest(query);
            ObjectMapper objectMapper = new ObjectMapper();
            String response = "";
        
            JsonNode jsonNode = objectMapper.readTree(body);
            JsonNode match = jsonNode.get("results").get(0);

            response += match.get("text").asText();
            response += " / cal/100g: ";
            caloriesIn100[numberOfFood] = match.get("cal").asInt();
            response += match.get("cal").asText();
            
            Product product = new Product(match.get("text").asText(), match.get("cal").asInt());
            productRepository.save(product);
            
            return response;
        } catch (JsonProcessingException e) {
            return "Error processing response";
        }
    }

    public String addProductToMeal(Long mealId, String productName, Integer grams) {
        cacheService.clearCache("meals");
        cacheService.clearCache("mealProducts");
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new RuntimeException("Meal not found"));
        
        List<Product> products = productRepository.findByNameContainingIgnoreCase(productName);
        if (products.isEmpty()) {
            return "Product not found. Please add it first using the calculate endpoint.";
        }
        Product product = products.get(0);
        
        MealProduct mealProduct = new MealProduct(grams, meal, product);
        mealProductRepository.save(mealProduct);
        
        int calories = product.getCaloriesPer100g() * grams / 100;
        
        return String.format("Added %dg of %s (%d kcal) to meal '%s'",
                grams, product.getName(), calories, meal.getName());
    }
}