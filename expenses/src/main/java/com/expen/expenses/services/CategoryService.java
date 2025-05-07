package com.expen.expenses.services;

import com.expen.expenses.dtos.CategoryDTO;
import com.expen.expenses.models.Category;
import com.expen.expenses.repositories.CategoryRepository;
import com.expen.expenses.jwt.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private static final Logger log = Logger.getLogger(CategoryService.class.getName());

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    public CategoryDTO createCategory(String name) {
        Long userId = extractUserIdFromToken();
        Category category = Category.builder().name(name).userId(userId).build();
        Category saved = categoryRepository.save(category);
        return mapToDTO(saved);
    }

    public List<CategoryDTO> getUserCategories() {
        Long userId = extractUserIdFromToken();
        return categoryRepository.findByUserId(userId)
                .stream().map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public CategoryDTO updateCategory(Long id, String name) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada"));
        category.setName(name);
        Category updated = categoryRepository.save(category);
        return mapToDTO(updated);
    }

    private CategoryDTO mapToDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    private Long extractUserIdFromToken() {
        final String authorizationHeader = request.getHeader("Authorization");
        String jwt = null;
        Long userId = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            userId = jwtUtil.extractUserId(jwt);
        }

        if (userId == null) {
            throw new RuntimeException("No se pudo extraer el userId del token JWT");
        }

        return userId;
    }
}
