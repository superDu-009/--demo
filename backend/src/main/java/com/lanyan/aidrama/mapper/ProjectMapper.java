package com.lanyan.aidrama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanyan.aidrama.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 项目 Mapper (MyBatis Plus)
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 更新项目执行锁状态
     * @param projectId 项目ID
     * @param lockValue 0-未执行 1-执行中
     */
    int updateExecutionLock(@Param("projectId") Long projectId, @Param("lockValue") Integer lockValue);
}
