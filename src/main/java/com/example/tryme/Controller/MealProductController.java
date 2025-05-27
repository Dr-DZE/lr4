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

import com.example.tryme.Model.MealProduct;
import com.example.tryme.exception.BadRequestException;
import com.example.tryme.services.MealProductService;
import com.example.tryme.services.MealService;
import com.example.tryme.services.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/mealProducts")
@Tag(name = "MealProduct API", description = "API для управления связями 'продукт в блюде'")
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

    @Operation(summary = "Создать новую запись о продукте в блюде")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Запись успешно создана"),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры (ID, граммы)", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", description = "Блюдо или продукт не найдены", ref = "#/components/responses/NotFound")
    })
    @PostMapping("/create")
    public ResponseEntity<String> createMealProduct(
            @Parameter(description = "Вес продукта в граммах", required = true, example = "150") @RequestParam Integer grams,
            @Parameter(description = "ID блюда", required = true, example = "1") @RequestParam Long mealId,
            @Parameter(description = "ID продукта", required = true, example = "1") @RequestParam Long productId) {
        if (grams == null || grams <= 0) { 
            throw new BadRequestException("Параметр 'grams' должен быть указан и быть положительным числом.");
        }

        String message = mealProductService.createMealProduct(grams, mealId, productId, mealService, productService);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @Operation(summary = "Получить запись о продукте в блюде по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запись найдена"),
            @ApiResponse(responseCode = "404", description = "Запись не найдена", ref = "#/components/responses/NotFound")
    })
    @GetMapping("/{id}")
    public ResponseEntity<MealProduct> getMealProduct(
            @Parameter(description = "ID записи о продукте в блюде", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(mealProductService.getMealProduct(id));
    }

    @Operation(summary = "Обновить вес продукта в блюде")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Вес успешно обновлен"),
            @ApiResponse(responseCode = "400", description = "Некорректное значение веса", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", description = "Запись для обновления не найдена", ref = "#/components/responses/NotFound")
    })
    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateMealProduct(
            @Parameter(description = "ID записи для обновления", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "Новый вес продукта в граммах", required = true, example = "200") @RequestParam Integer grams) {
        if (grams == null || grams <= 0) { 
            throw new BadRequestException("Параметр 'grams' должен быть указан и быть положительным числом.");
        }
        return ResponseEntity.ok(mealProductService.updateMealProduct(id, grams));
    }

    @Operation(summary = "Удалить запись о продукте в блюде по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запись успешно удалена"),
            @ApiResponse(responseCode = "404", description = "Запись для удаления не найдена", ref = "#/components/responses/NotFound")
    })
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteMealProduct(
            @Parameter(description = "ID записи для удаления", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(mealProductService.deleteMealProduct(id));
    }

    @Operation(summary = "Получить все записи о продуктах в блюдах")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список всех записей")
    })
    @GetMapping("/")
    public ResponseEntity<List<MealProduct>> getAllMealProducts() {
        return ResponseEntity.ok(mealProductService.getAllMealProducts());
    }
}