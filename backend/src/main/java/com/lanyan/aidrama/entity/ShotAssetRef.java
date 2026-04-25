package com.lanyan.aidrama.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分镜-资产关联实体类 (对应 shot_asset_ref 表，系分 3. DDL 第7张表)
 */
@Data
public class ShotAssetRef {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分镜ID */
    private Long shotId;

    /** 资产ID */
    private Long assetId;

    /** 资产类型: character/scene/prop/voice */
    private String assetType;

    /** 创建时间 */
    private LocalDateTime createTime;
}
