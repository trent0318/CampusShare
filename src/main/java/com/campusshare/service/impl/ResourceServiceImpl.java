package com.campusshare.service.impl;

import com.campusshare.common.PageResult;
import com.campusshare.dto.CreateResourceDTO;
import com.campusshare.dto.ResourceQueryDTO;
import com.campusshare.dto.UpdateResourceDTO;
import com.campusshare.entity.Resource;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.CategoryMapper;
import com.campusshare.mapper.ResourceMapper;
import com.campusshare.service.OperationLogService;
import com.campusshare.service.ResourceService;
import com.campusshare.utils.SecurityUtil;
import com.campusshare.vo.ResourceVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    /** 资源状态：0 待审核 / 1 已上架 / 2 已下架 / 3 已驳回 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_ON_SHELF = 1;
    private static final int STATUS_OFF_SHELF = 2;
    private static final int STATUS_REJECTED = 3;

    private static final String TYPE_ITEM = "ITEM";
    private static final String TYPE_VENUE = "VENUE";

    private final ResourceMapper resourceMapper;
    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final OperationLogService operationLogService;

    /** 资源详情缓存 Key 前缀 + TTL（只缓存已上架资源，避免越权泄露） */
    private static final String RESOURCE_DETAIL_KEY_PREFIX = "campusshare:resource:detail:";
    private static final Duration RESOURCE_CACHE_TTL = Duration.ofMinutes(30);

    @Override
    public PageResult<ResourceVO> pageResources(ResourceQueryDTO query) {
        // 普通用户只能看已上架资源（无视其传入的 status）；管理员可看全部
        if (!isAdmin()) {
            query.setStatus(STATUS_ON_SHELF);
        }
        if (query.getPage() < 1) {
            query.setPage(1);
        }
        if (query.getSize() < 1) {
            query.setSize(10);
        }
        query.setOffset((query.getPage() - 1) * query.getSize());

        long total = resourceMapper.count(query);
        List<Resource> list = resourceMapper.selectPage(query);
        return PageResult.of(total, list.stream().map(ResourceVO::from).toList());
    }

    @Override
    public ResourceVO getResource(Long id) {
        String cacheKey = RESOURCE_DETAIL_KEY_PREFIX + id;
        // 1. Cache Aside：先查缓存，命中直接返回
        ResourceVO cached = getCached(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 2. 未命中，查数据库
        Resource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        if (!isAdmin() && resource.getStatus() != STATUS_ON_SHELF) {
            throw new BusinessException("资源不存在或已下架");
        }
        ResourceVO vo = ResourceVO.from(resource);
        // 3. 只有已上架资源才缓存（热门资源）；下架/待审核不缓存，避免普通用户读到
        if (resource.getStatus() != null && resource.getStatus() == STATUS_ON_SHELF) {
            putCache(cacheKey, vo);
        }
        return vo;
    }

    /** 读缓存：损坏或 Redis 异常时降级为未命中，走数据库 */
    private ResourceVO getCached(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, ResourceVO.class);
        } catch (Exception e) {
            log.warn("读资源缓存失败，降级查数据库: {}", key, e);
            return null;
        }
    }

    /** 写缓存：Redis 异常时忽略（缓存只是加速，不阻塞主流程） */
    private void putCache(String key, ResourceVO vo) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vo), RESOURCE_CACHE_TTL);
        } catch (Exception e) {
            log.warn("写资源缓存失败: {}", key, e);
        }
    }

    /** 删除缓存：资源变更后调用，保证缓存与 MySQL 一致（先改库，再删缓存） */
    private void evictResourceCache(Long id) {
        try {
            stringRedisTemplate.delete(RESOURCE_DETAIL_KEY_PREFIX + id);
        } catch (Exception e) {
            log.warn("删除资源缓存失败: {}", id, e);
        }
    }

    @Override
    public Long createResource(CreateResourceDTO dto) {
        validateType(dto.getType());
        if (categoryMapper.selectById(dto.getCategoryId()) == null) {
            throw new BusinessException("分类不存在");
        }
        Resource resource = new Resource();
        resource.setName(dto.getName());
        resource.setCategoryId(dto.getCategoryId());
        resource.setType(dto.getType());
        resource.setDescription(dto.getDescription());
        resource.setImage(dto.getImage());
        resource.setLocation(dto.getLocation());
        resource.setTotalCount(dto.getTotalCount() == null ? 1 : dto.getTotalCount());
        resource.setStatus(STATUS_PENDING);
        resourceMapper.insert(resource);
        operationLogService.record("CREATE_RESOURCE", "RESOURCE", resource.getId(), resource.getName());
        return resource.getId();
    }

    @Override
    public ResourceVO updateResource(Long id, UpdateResourceDTO dto) {
        if (resourceMapper.selectById(id) == null) {
            throw new BusinessException("资源不存在");
        }
        if (dto.getType() != null) {
            validateType(dto.getType());
        }
        if (dto.getCategoryId() != null && categoryMapper.selectById(dto.getCategoryId()) == null) {
            throw new BusinessException("分类不存在");
        }
        Resource resource = new Resource();
        resource.setId(id);
        resource.setName(dto.getName());
        resource.setCategoryId(dto.getCategoryId());
        resource.setType(dto.getType());
        resource.setDescription(dto.getDescription());
        resource.setImage(dto.getImage());
        resource.setLocation(dto.getLocation());
        resource.setTotalCount(dto.getTotalCount());
        resourceMapper.update(resource);
        evictResourceCache(id);
        operationLogService.record("UPDATE_RESOURCE", "RESOURCE", id, dto.getName());
        return ResourceVO.from(resourceMapper.selectById(id));
    }

    @Override
    public void deleteResource(Long id) {
        if (resourceMapper.selectById(id) == null) {
            throw new BusinessException("资源不存在");
        }
        resourceMapper.deleteById(id);
        evictResourceCache(id);
        operationLogService.record("DELETE_RESOURCE", "RESOURCE", id, null);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_ON_SHELF && status != STATUS_OFF_SHELF)) {
            throw new BusinessException("状态值非法，只能上架(1)或下架(2)");
        }
        if (resourceMapper.selectById(id) == null) {
            throw new BusinessException("资源不存在");
        }
        resourceMapper.updateStatus(id, status, null);
        evictResourceCache(id);
        operationLogService.record("CHANGE_RESOURCE_STATUS", "RESOURCE", id, status == STATUS_ON_SHELF ? "上架" : "下架");
    }

    @Override
    public void audit(Long id, boolean approve, String reason) {
        Resource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        if (resource.getStatus() != STATUS_PENDING) {
            throw new BusinessException("只有待审核的资源才能审核");
        }
        if (approve) {
            resourceMapper.updateStatus(id, STATUS_ON_SHELF, null);
        } else {
            if (reason == null || reason.isBlank()) {
                throw new BusinessException("驳回时必须填写原因");
            }
            resourceMapper.updateStatus(id, STATUS_REJECTED, reason);
        }
        evictResourceCache(id);
        operationLogService.record("AUDIT_RESOURCE", "RESOURCE", id, approve ? "审核通过" : "审核驳回");
    }

    private boolean isAdmin() {
        return "ADMIN".equals(SecurityUtil.getCurrentUser().getRole());
    }

    private void validateType(String type) {
        if (!TYPE_ITEM.equals(type) && !TYPE_VENUE.equals(type)) {
            throw new BusinessException("资源类型非法，只能是 ITEM 或 VENUE");
        }
    }
}
