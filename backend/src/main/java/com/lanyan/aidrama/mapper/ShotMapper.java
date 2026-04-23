package com.lanyan.aidrama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanyan.aidrama.entity.Shot;
import com.lanyan.aidrama.module.project.dto.ShotVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 分镜 Mapper (MyBatis Plus)
 */
@Mapper
public interface ShotMapper extends BaseMapper<Shot> {

    /**
     * 分页查询分镜列表，LEFT JOIN 关联资产引用和最新 AI 任务
     * 用于 ContentController.listShots
     */
    IPage<ShotVO> selectShotVOPage(
            Page<ShotVO> page,
            @Param("sceneId") Long sceneId,
            @Param("status") Integer status
    );

    /**
     * 项目级分镜聚合查询，跨分场/分集关联
     * 用于 ProjectController.getProjectShots
     */
    IPage<ShotVO> selectProjectShotVOPage(
            Page<ShotVO> page,
            @Param("projectId") Long projectId,
            @Param("sceneId") Long sceneId,
            @Param("status") Integer status
    );
}
