package com.campusshare.controller.admin;

import com.campusshare.common.Result;
import com.campusshare.dto.CreateCategoryDTO;
import com.campusshare.dto.UpdateCategoryDTO;
import com.campusshare.service.CategoryService;
import com.campusshare.vo.CategoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public Result<CategoryVO> createCategory(@Valid @RequestBody CreateCategoryDTO dto) {
        return Result.success(categoryService.createCategory(dto));
    }

    @PutMapping("/{id}")
    public Result<CategoryVO> updateCategory(@PathVariable Long id, @Valid @RequestBody UpdateCategoryDTO dto) {
        return Result.success(categoryService.updateCategory(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
