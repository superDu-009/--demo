package com.lanyan.aidrama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanyan.aidrama.entity.WorkflowTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流程任务 Mapper (MyBatis Plus)
 */
@Mapper
public interface WorkflowTaskMapper extends BaseMapper<WorkflowTask> {

    /**
     * 查询项目最后一个执行中的流程任务
     * 用于服务重启后恢复执行
     */
    WorkflowTask selectLatestRunning(@Param("projectId") Long projectId);

    /**
     * 查询项目特定步骤的流程任务
     * 用于判断步骤是否已执行/正在执行
     */
    WorkflowTask selectByProjectAndStep(
            @Param("projectId") Long projectId,
            @Param("stepType") String stepType,
            @Param("episodeId") Long episodeId
    );
}
