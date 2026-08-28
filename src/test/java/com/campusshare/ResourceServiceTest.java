package com.campusshare;

import com.campusshare.dto.CreateResourceDTO;
import com.campusshare.entity.Category;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.CategoryMapper;
import com.campusshare.security.LoginUser;
import com.campusshare.service.ResourceService;
import com.campusshare.vo.ResourceVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Phase 7 单元/集成测试：资源缓存（Cache Aside）与可见性权限。
 * 通过新建待审核资源隔离测试数据，避免污染已上架的真实资源。
 */
@SpringBootTest
class ResourceServiceTest {

    private static final String DETAIL_KEY_PREFIX = "campusshare:resource:detail:";

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Long userId, String role) {
        LoginUser loginUser = new LoginUser(userId, "tuser" + userId, role);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /** 新建一个待审核(0)资源并上架(1)，返回其 id；测试用数据，与真实资源隔离 */
    private Long createOnShelfResource(String name) {
        loginAs(9201L, "USER");
        List<Category> categories = categoryMapper.selectList(null);
        assertFalse(categories.isEmpty(), "测试需要一个分类，请先初始化分类数据");

        CreateResourceDTO dto = new CreateResourceDTO();
        dto.setName(name);
        dto.setCategoryId(categories.get(0).getId());
        dto.setType("ITEM");
        dto.setTotalCount(1);
        Long id = resourceService.createResource(dto);
        resourceService.changeStatus(id, 1);
        return id;
    }

    @Test
    void getResource_notFound_rejected() {
        loginAs(9202L, "USER");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> resourceService.getResource(999999L));
        assertEquals("资源不存在", ex.getMessage());
    }

    @Test
    void getResource_onShelf_cacheWritten() {
        Long id = createOnShelfResource("缓存测试资源");
        String cacheKey = DETAIL_KEY_PREFIX + id;
        stringRedisTemplate.delete(cacheKey);

        loginAs(9203L, "USER");
        ResourceVO vo = resourceService.getResource(id);

        assertNotNull(vo);
        assertEquals(id, vo.getId());
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        assertNotNull(cached, "读取已上架资源后应写入缓存");
    }

    @Test
    void getResource_offShelf_userForbidden() {
        Long id = createOnShelfResource("下架可见性测试资源");
        // 下架该资源
        resourceService.changeStatus(id, 2);

        loginAs(9204L, "USER");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> resourceService.getResource(id));
        assertEquals("资源不存在或已下架", ex.getMessage());
    }

    @Test
    void getResource_offShelf_adminVisible() {
        Long id = createOnShelfResource("管理员可见性测试资源");
        resourceService.changeStatus(id, 2);

        loginAs(9205L, "ADMIN");
        ResourceVO vo = resourceService.getResource(id);
        assertNotNull(vo);
        assertEquals(id, vo.getId());
    }

    @Test
    void changeStatus_evictsCache() {
        Long id = createOnShelfResource("缓存失效测试资源");
        String cacheKey = DETAIL_KEY_PREFIX + id;

        loginAs(9206L, "USER");
        resourceService.getResource(id);
        assertNotNull(stringRedisTemplate.opsForValue().get(cacheKey), "上架资源读取后应已有缓存");

        resourceService.changeStatus(id, 2);
        assertNull(stringRedisTemplate.opsForValue().get(cacheKey), "资源变更后应删除缓存");
    }
}
