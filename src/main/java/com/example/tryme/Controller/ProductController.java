package com.example.tryme.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tryme.Model.Product;
import com.example.tryme.exception.BadRequestException;
import com.example.tryme.services.CaloriesService;
import com.example.tryme.services.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/products")
@Tag(name = "Product API", description = "API для управления продуктами и расчета калорий")
public class ProductController {
    private final ProductService productService;
    private final CaloriesService caloriesService;

    @Autowired
    public ProductController(ProductService productService,
                             CaloriesService caloriesService) {
        this.productService = productService;
        this.caloriesService = caloriesService;
    }

    @Operation(summary = "Рассчитать калории для набора продуктов",
            description = "Рассчитывает общую калорийность на основе списка продуктов и их веса. Также добавляет продукты в базу данных, если их там нет.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Расчет калорий выполнен"),
            @ApiResponse(responseCode = "400", description = "Некорректные входные данные", ref = "#/components/responses/BadRequest")
    })
    @GetMapping("/CalculateCalories")
    public ResponseEntity<List<String>> calculateCalories(
            @Parameter(description = "Количество продуктов для расчета.", example = "2")
            @RequestParam(required = false) Integer productCount,
            
            @Parameter(description = "Массив названий продуктов.", example = "[\"Chicken Breast\", \"Broccoli\"]")
            @RequestParam(required = false) String[] food,
            
            @Parameter(description = "Массив веса продуктов в граммах.", example = "[150, 100]")
            @RequestParam(required = false) Integer[] gram) {

        if (productCount == null || food == null || gram == null) {
            StringBuilder missingParams = new StringBuilder("Не указаны обязательные параметры: ");
            if (productCount == null) missingParams.append("productCount; ");
            if (food == null) missingParams.append("food; ");
            if (gram == null) missingParams.append("gram; ");
            throw new BadRequestException(missingParams.toString().trim());
        }

        if (productCount <= 0) {
            throw new BadRequestException("Параметр 'productCount' должен быть положительным числом.");
        }
        if (food.length != productCount || gram.length != productCount) {
             throw new BadRequestException("Длины массивов 'food' и 'gram' должны соответствовать значению 'productCount'.");
        }
        if (productCount > 0 && (food.length == 0 || gram.length == 0)) { 
             throw new BadRequestException("Массивы 'food' и 'gram' не могут быть пустыми, если 'productCount' > 0.");
        }
        for (int i = 0; i < productCount; i++) {
            if (food[i] == null || food[i].trim().isEmpty()) {
                throw new BadRequestException("Название продукта (food) по индексу " + i + " не может быть пустым.");
            }
            if (gram[i] == null || gram[i] <= 0) {
                throw new BadRequestException("Вес продукта (gram) для '" + food[i] + "' должен быть указан и быть положительным числом.");
            }
        }
        return ResponseEntity.ok(caloriesService.calculateCalories(productCount, food, gram));
    }

    @Operation(summary = "Создать новый продукт")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Продукт успешно создан"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные продукта (имя, калории)", ref = "#/components/responses/BadRequest")
    })
    @PostMapping("/create")
    public ResponseEntity<String> createProduct(
            @Parameter(description = "Название продукта", required = true, example = "Apple") @RequestParam String name,
            @Parameter(description = "Калорийность на 100г", required = true, example = "52") @RequestParam Integer caloriesPer100g) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Параметр 'name' не может быть пустым.");
        }
        if (caloriesPer100g == null || caloriesPer100g < 0) {
            throw new BadRequestException("Параметр 'caloriesPer100g' должен быть указан и не может быть отрицательным.");
        }
        String message = productService.createProduct(name, caloriesPer100g);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @Operation(summary = "Получить продукт по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Продукт найден"),
            @ApiResponse(responseCode = "404", description = "Продукт не найден", ref = "#/components/responses/NotFound")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(
            @Parameter(description = "ID продукта", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @Operation(summary = "Обновить существующий продукт")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Продукт успешно обновлен"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные для обновления", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", description = "Продукт для обновления не найден", ref = "#/components/responses/NotFound")
    })
    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateProduct(
            @Parameter(description = "ID продукта для обновления", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "Новое название продукта", required = true, example = "Green Apple") @RequestParam String name,
            @Parameter(description = "Новая калорийность на 100г", required = true, example = "55") @RequestParam Integer caloriesPer100g) {
        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Параметр 'name' не может быть пустым.");
        }
        if (caloriesPer100g == null || caloriesPer100g < 0) {
            throw new BadRequestException("Параметр 'caloriesPer100g' должен быть указан и не может быть отрицательным.");
        }
        return ResponseEntity.ok(productService.updateProduct(id, name, caloriesPer100g));
    }

    @Operation(summary = "Удалить продукт по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Продукт успешно удален"),
            @ApiResponse(responseCode = "404", description = "Продукт для удаления не найден", ref = "#/components/responses/NotFound")
    })
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteProduct(
            @Parameter(description = "ID продукта для удаления", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(productService.deleteProduct(id));
    }

    @Operation(summary = "Получить все продукты")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список всех продуктов")
    })
    @GetMapping("/")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }
}