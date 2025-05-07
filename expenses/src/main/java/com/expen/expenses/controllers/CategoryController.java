package com.expen.expenses.controllers;

import com.expen.expenses.dtos.CategoryDTO;
import com.expen.expenses.dtos.CategoryRequest;
import com.expen.expenses.services.CategoryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public CategoryDTO createCategory(@RequestBody CategoryRequest request) {
        return categoryService.createCategory(request.getName());
    }

    @GetMapping
    public List<CategoryDTO> getUserCategories() {
        return categoryService.getUserCategories();
    }

    @PutMapping("/{id}")
    public CategoryDTO updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        return categoryService.updateCategory(id, request.getName());
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
    }
}
