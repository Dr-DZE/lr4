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
            throw new BadRequestException("Ошибка при получении данных о калорийности для запроса: " + query + ". Статус: " + e.getStatusCode(), e);
        } catch (Exception e) {
            logger.error("Unexpected error calling external API for query '{}': {}", query, e.getMessage(), e);
            throw new RuntimeException("Непредвиденная ошибка при обращении к внешнему сервису калорийности для запроса: " + query, e);
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

        Meal meal = new Meal("Meal created on " + new Date().toString());
        mealRepository.save(meal);

        for (int i = 0; i < productCount; i++) {
            
            if (food[i] == null || food[i].trim().isEmpty()) {
                throw new BadRequestException("Название продукта (food) по индексу " + i + " не может быть пустым.");
            }
            if (gram[i] == null || gram[i] <= 0) {
                throw new BadRequestException("Вес продукта (gram) для '" + food[i] + "' должен быть указан и быть положительным числом.");
            }
            String response = getNameFromWebAndSaveProduct(food[i], caloriesIn100, i);
            String temp = gram[i] + "g." + " " + response;
            totalCalories += caloriesIn100[i] * gram[i] / 100;
            listOfProducts.add(temp);

            List<Product> products = productRepository.findByNameContainingIgnoreCase(food[i]);
            if (products.isEmpty()) {
                logger.error("Продукт {} не найден после попытки сохранения из веба.", food[i]);
                throw new ResourceNotFoundException("Продукт " + food[i] + " не удалось обработать и сохранить.");
            }
            Product product = products.get(0);
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
                logger.warn("Для запроса '{}' не найдено результатов от внешнего API. Ответ: {}", query, body);
                throw new ResourceNotFoundException("Информация о продукте не найдена для: " + query);
            }
            JsonNode match = jsonNode.get("results").get(0);

            String productName = match.has("text") ? match.get("text").asText() : query;
            int productCalories = match.has("cal") ? match.get("cal").asInt() : 0;

            if (productName == null || productName.trim().isEmpty()) {
                throw new BadRequestException("Внешний API вернул пустое имя продукта для запроса: " + query);
            }

            responseText += productName;
            responseText += " / cal/100g: ";
            caloriesIn100[numberOfFood] = productCalories;
            responseText += productCalories;

            List<Product> existingProducts = productRepository.findByNameContainingIgnoreCase(productName);
            Product product;
            if (existingProducts.isEmpty()) {
                product = new Product(productName, productCalories);
                productRepository.save(product);
                logger.info("Сохранен новый продукт: {} с {} кал/100г", productName, productCalories);
                cacheService.clearCache("products");
            } else {
                product = existingProducts.get(0);
                if (!product.getCaloriesPer100g().equals(productCalories)) {
                    logger.warn("Несоответствие калорий для продукта '{}'. БД: {}, API: {}. Используется значение из API.",
                                productName, product.getCaloriesPer100g(), productCalories);
                }
            }
            return responseText;
        } catch (JsonProcessingException e) {
            logger.error("Ошибка обработки JSON ответа для запроса '{}': {}", query, e.getMessage(), e);
            throw new BadRequestException("Ошибка обработки данных о калорийности для: " + query, e);
        } 
    }

    public String addProductToMeal(Long mealId, String productName, Integer grams) {
        if (mealId == null) {
            throw new BadRequestException("Параметр 'mealId' не может быть null.");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new BadRequestException("Параметр 'productName' не может быть пустым.");
        }
        if (grams == null || grams <= 0) {
            throw new BadRequestException("Параметр 'grams' должен быть указан и быть положительным числом.");
        }
        
        cacheService.clearCache("meals");
        cacheService.clearCache("mealProducts");

        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResourceNotFoundException("Блюдо с id: " + mealId + " не найдено."));

        List<Product> products = productRepository.findByNameContainingIgnoreCase(productName);
        if (products.isEmpty()) {
            throw new ResourceNotFoundException("Продукт с именем: " + productName + " не найден. " +
                    "Пожалуйста, убедитесь, что он добавлен, возможно, через эндпоинт CalculateCalories.");
        }
        Product product = products.get(0);

        MealProduct mealProduct = new MealProduct(grams, meal, product);
        mealProductRepository.save(mealProduct);

        int calories = product.getCaloriesPer100g() * grams / 100;

        return String.format("Добавлено %dg продукта %s (%d ккал) в блюдо '%s'",
                grams, product.getName(), calories, meal.getName());
    }
}