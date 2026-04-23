package com.lanyan.aidrama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanyan.aidrama.entity.AiTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI任务 Mapper (MyBatis Plus)
 */
@Mapper
public interface AiTaskMapper extends BaseMapper<AiTask> {

    /**
     * 查询到期需要轮询的 AI 任务
     * 条件: status=1(处理中) AND next_poll_time <= NOW()
     * 按 next_poll_time 排序，限制 50 条
     */
    List<AiTask> selectPollableTasks();
}
