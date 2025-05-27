package com.example.tryme.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.tryme.Model.Meal;
import com.example.tryme.Model.MealProduct;
import com.example.tryme.Model.Product;
import com.example.tryme.Repository.MealProductRepository;
import com.example.tryme.Repository.MealRepository;
import com.example.tryme.Repository.ProductRepository;
import com.example.tryme.exception.BadRequestException;
import com.example.tryme.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CaloriesService {
    private static final Logger logger = LoggerFactory.getLogger(CaloriesService.class);
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
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Error calling external API for query '{}': {} - {}", query, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BadRequestException("Error fetching data from external calorie service for query: " + query + ". Status: " + e.getStatusCode(), e);
        } catch (Exception e) { 
            logger.error("Unexpected error calling external API for query '{}': {}", query, e.getMessage(), e);
            throw new RuntimeException("Unexpected error communicating with external calorie service for query: " + query, e);
        }
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

        // Используем конструктор Meal(String name)
        Meal meal = new Meal("Meal created on " + new Date().toString());
        mealRepository.save(meal);

        for (int i = 0; i < productCount; i++) {
            if (food[i] == null || food[i].trim().isEmpty()) {
                throw new BadRequestException("Food name at index " + i + " cannot be empty.");
            }
            if (gram[i] == null || gram[i] <= 0) {
                throw new BadRequestException("Grams for food '" + food[i] + "' must be a positive integer.");
            }
            String response = getNameFromWebAndSaveProduct(food[i], caloriesIn100, i);
            String temp = gram[i] + "g." + " " + response;
            totalCalories += caloriesIn100[i] * gram[i] / 100;
            listOfProducts.add(temp);

            List<Product> products = productRepository.findByNameContainingIgnoreCase(food[i]);
            if (products.isEmpty()) {
                logger.error("Product {} not found after attempting to save from web.", food[i]);
                throw new ResourceNotFoundException("Product " + food[i] + " could not be processed and saved.");
            }
            Product product = products.get(0);
            // Используем конструктор MealProduct(Integer grams, Meal meal, Product product)
            MealProduct mealProduct = new MealProduct(gram[i], meal, product);
            mealProductRepository.save(mealProduct);
        }

        listOfProducts.add("Total calories: " + totalCalories);
        cacheService.putToCache("calories", cacheKey, listOfProducts);
        return listOfProducts;
    }

    private String getNameFromWebAndSaveProduct(String query, Integer[] caloriesIn100, Integer numberOfFood) {
        try {
            String body = this.sendPostRequest(query);
            ObjectMapper objectMapper = new ObjectMapper();
            String responseText = "";

            JsonNode jsonNode = objectMapper.readTree(body);
            if (jsonNode == null || !jsonNode.has("results") || !jsonNode.get("results").isArray() || jsonNode.get("results").isEmpty()) {
                logger.warn("No results found for query '{}' from external API. Response: {}", query, body);
                throw new ResourceNotFoundException("Product information not found for: " + query);
            }
            JsonNode match = jsonNode.get("results").get(0);

            String productName = match.has("text") ? match.get("text").asText() : query;
            int productCalories = match.has("cal") ? match.get("cal").asInt() : 0;

            responseText += productName;
            responseText += " / cal/100g: ";
            caloriesIn100[numberOfFood] = productCalories;
            responseText += productCalories;

            List<Product> existingProducts = productRepository.findByNameContainingIgnoreCase(productName);
            Product product; 
            if (existingProducts.isEmpty()) {
                product = new Product(productName, productCalories);
                productRepository.save(product);
                logger.info("Saved new product: {} with {} cal/100g", productName, productCalories);
                cacheService.clearCache("products");
            } else {
                product = existingProducts.get(0); 
                if (!product.getCaloriesPer100g().equals(productCalories)) {
                    logger.warn("Calorie mismatch for product '{}'. DB: {}, API: {}. Using API value for current calculation.",
                                productName, product.getCaloriesPer100g(), productCalories);
                }
            }
            return responseText;
        } catch (JsonProcessingException e) {
            logger.error("Error processing JSON response for query '{}': {}", query, e.getMessage(), e);
            throw new BadRequestException("Error processing calorie data for: " + query, e);
        } catch (ResourceNotFoundException e) { 
            throw e;
        } catch (RuntimeException e) { 
             logger.error("Runtime error in getNameFromWebAndSaveProduct for query '{}': {}", query, e.getMessage(), e);
             if (e instanceof BadRequestException) throw e; 
             throw new RuntimeException("Failed to retrieve or save product data for: " + query + " due to an internal error.", e);
        }
    }

    public String addProductToMeal(Long mealId, String productName, Integer grams) {
        if (grams <= 0) {
            throw new BadRequestException("Grams must be a positive value.");
        }
        cacheService.clearCache("meals");
        cacheService.clearCache("mealProducts");

        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException("Meal not found with id: " + mealId));

        List<Product> products = productRepository.findByNameContainingIgnoreCase(productName);
        if (products.isEmpty()) {
            throw new ResourceNotFoundException("Product not found: " + productName +
                    ". Please ensure it's added, possibly via CalculateCalories endpoint first.");
        }
        Product product = products.get(0);

        // Используем конструктор MealProduct(Integer grams, Meal meal, Product product)
        MealProduct mealProduct = new MealProduct(grams, meal, product);
        mealProductRepository.save(mealProduct);

        int calories = product.getCaloriesPer100g() * grams / 100;

        return String.format("Added %dg of %s (%d kcal) to meal '%s'",
                grams, product.getName(), calories, meal.getName());
    }
}