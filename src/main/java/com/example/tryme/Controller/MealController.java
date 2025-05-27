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

import com.example.tryme.Model.Meal;
import com.example.tryme.exception.BadRequestException;
import com.example.tryme.services.MealService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/meals")
@Tag(name = "Meal API", description = "API для управления блюдами")
public class MealController {
    private final MealService mealService;

    @Autowired
    public MealController(MealService mealService) {
        this.mealService = mealService;
    }

    @Operation(summary = "Получить блюда по названию продукта", description = "Возвращает список блюд, содержащих указанный продукт.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный ответ",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class, subTypes = {Meal.class}))),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос (например, пустое имя продукта)", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", ref = "#/components/responses/InternalServerError")
    })
    @GetMapping("/by-product")
    public ResponseEntity<List<Meal>> getMealsByProduct(
            @Parameter(description = "Название продукта для поиска", required = true, example = "Chicken") @RequestParam String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new BadRequestException("Product name cannot be empty."); 
        }
        return ResponseEntity.ok(mealService.findMealsByProductName(productName));
    }

    @Operation(summary = "Создать новое блюдо")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Блюдо успешно создано",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Некорректное имя блюда", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", ref = "#/components/responses/InternalServerError")
    })
    @PostMapping("/create")
    public ResponseEntity<String> createMeal(
            @Parameter(description = "Название нового блюда", required = true, example = "Pasta Carbonara") @RequestParam String mealName) {
        if (mealName == null || mealName.trim().isEmpty()) {
            throw new BadRequestException("Meal name cannot be empty.");
        }
        String message = mealService.createMeal(mealName);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @Operation(summary = "Получить блюдо по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Блюдо найдено",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Meal.class))),
            @ApiResponse(responseCode = "404", description = "Блюдо не найдено", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", ref = "#/components/responses/InternalServerError")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Meal> getMeal(
            @Parameter(description = "ID блюда", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(mealService.getMeal(id));
    }

    @Operation(summary = "Обновить существующее блюдо")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Блюдо успешно обновлено",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Некорректное новое имя блюда", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", description = "Блюдо для обновления не найдено", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", ref = "#/components/responses/InternalServerError")
    })
    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateMeal(
            @Parameter(description = "ID блюда для обновления", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "Новое название блюда", required = true, example = "Spaghetti Bolognese") @RequestParam String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new BadRequestException("New meal name cannot be empty."); 
        }
        return ResponseEntity.ok(mealService.updateMeal(id, newName));
    }

    @Operation(summary = "Удалить блюдо по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Блюдо успешно удалено",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "404", description = "Блюдо для удаления не найдено", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", ref = "#/components/responses/InternalServerError")
    })
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteMeal(
            @Parameter(description = "ID блюда для удаления", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(mealService.deleteMeal(id));
    }

    @Operation(summary = "Получить все блюда")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список всех блюд",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class, subTypes = {Meal.class}))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", ref = "#/components/responses/InternalServerError")
    })
    @GetMapping("/")
    public ResponseEntity<List<Meal>> getAllMeals() {
        return ResponseEntity.ok(mealService.getAllMeals());
    }
}