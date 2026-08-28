package com.campusshare.service;

import com.campusshare.dto.CreateCategoryDTO;
import com.campusshare.dto.UpdateCategoryDTO;
import com.campusshare.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    List<CategoryVO> listCategories(String type);

    CategoryVO createCategory(CreateCategoryDTO dto);

    CategoryVO updateCategory(Long id, UpdateCategoryDTO dto);

    void deleteCategory(Long id);
}
