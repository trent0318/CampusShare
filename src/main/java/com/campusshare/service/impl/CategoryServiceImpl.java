package com.campusshare.service.impl;

import com.campusshare.dto.CreateCategoryDTO;
import com.campusshare.dto.UpdateCategoryDTO;
import com.campusshare.entity.Category;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.CategoryMapper;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.service.CategoryService;
import com.campusshare.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String TYPE_ITEM = "ITEM";
    private static final String TYPE_VENUE = "VENUE";

    private final CategoryMapper categoryMapper;
    private final ResourceMapper resourceMapper;

    @Override
    public List<CategoryVO> listCategories(String type) {
        return categoryMapper.selectList(type).stream().map(CategoryVO::from).toList();
    }

    @Override
    public CategoryVO createCategory(CreateCategoryDTO dto) {
        validateType(dto.getType());
        Category category = new Category();
        category.setName(dto.getName());
        category.setType(dto.getType());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        category.setStatus(1);
        categoryMapper.insert(category);
        return CategoryVO.from(categoryMapper.selectById(category.getId()));
    }

    @Override
    public CategoryVO updateCategory(Long id, UpdateCategoryDTO dto) {
        if (categoryMapper.selectById(id) == null) {
            throw new BusinessException("分类不存在");
        }
        if (dto.getType() != null) {
            validateType(dto.getType());
        }
        Category category = new Category();
        category.setId(id);
        category.setName(dto.getName());
        category.setType(dto.getType());
        category.setSort(dto.getSort());
        category.setStatus(dto.getStatus());
        categoryMapper.update(category);
        return CategoryVO.from(categoryMapper.selectById(id));
    }

    @Override
    public void deleteCategory(Long id) {
        if (categoryMapper.selectById(id) == null) {
            throw new BusinessException("分类不存在");
        }
        long count = resourceMapper.countByCategoryId(id);
        if (count > 0) {
            throw new BusinessException("该分类下存在资源，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    private void validateType(String type) {
        if (!TYPE_ITEM.equals(type) && !TYPE_VENUE.equals(type)) {
            throw new BusinessException("分类类型非法，只能是 ITEM 或 VENUE");
        }
    }
}
