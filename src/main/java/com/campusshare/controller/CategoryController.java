package com.campusshare.controller;

import com.campusshare.common.Result;
import com.campusshare.service.CategoryService;
import com.campusshare.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<List<CategoryVO>> listCategories(@RequestParam(required = false) String type) {
        return Result.success(categoryService.listCategories(type));
    }
}
