package com.lanyan.aidrama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanyan.aidrama.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一任务 Mapper (MyBatis Plus)
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
