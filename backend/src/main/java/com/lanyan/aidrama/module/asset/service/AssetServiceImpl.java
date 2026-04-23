package com.lanyan.aidrama.module.asset.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lanyan.aidrama.common.BusinessException;
import com.lanyan.aidrama.common.ErrorCode;
import com.lanyan.aidrama.common.PageResult;
import com.lanyan.aidrama.common.AssetStatus;
import com.lanyan.aidrama.entity.*;
import com.lanyan.aidrama.mapper.*;
import com.lanyan.aidrama.module.asset.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产服务实现类 (系分 4.3.2)
 * 实现资产 CRUD、确认、引用查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetMapper assetMapper;
    private final ProjectMapper projectMapper;
    private final ShotAssetRefMapper shotAssetRefMapper;
    private final ShotMapper shotMapper;
    private final SceneMapper sceneMapper;
    private final EpisodeMapper episodeMapper;

    @Override
    public List<AssetVO> listAssets(Long projectId, String assetType) {
        // 校验项目存在
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 查询资产
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Asset::getProjectId, projectId)
               .eq(assetType != null, Asset::getAssetType, assetType)
               .orderByAsc(Asset::getAssetType)
               .orderByDesc(Asset::getCreateTime);

        return assetMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long createAsset(Long projectId, AssetCreateRequest req) {
        // 校验项目归属
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!project.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_PROJECT_OWNER);
        }

        // 创建资产
        Asset asset = new Asset();
        asset.setProjectId(projectId);
        asset.setAssetType(req.getAssetType());
        asset.setName(req.getName());
        asset.setDescription(req.getDescription());
        asset.setReferenceImages(req.getReferenceImages());
        asset.setStylePreset(req.getStylePreset());
        asset.setStatus(AssetStatus.DRAFT); // 默认草稿

        assetMapper.insert(asset);
        log.info("创建资产成功, assetId: {}, projectId: {}", asset.getId(), projectId);
        return asset.getId();
    }

    @Override
    public void updateAsset(Long id, AssetUpdateRequest req) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        if (req.getName() != null) {
            asset.setName(req.getName());
        }
        if (req.getDescription() != null) {
            asset.setDescription(req.getDescription());
        }
        if (req.getReferenceImages() != null) {
            asset.setReferenceImages(req.getReferenceImages());
        }
        if (req.getStylePreset() != null) {
            asset.setStylePreset(req.getStylePreset());
        }

        assetMapper.updateById(asset);
        log.info("更新资产成功, assetId: {}", id);
    }

    @Override
    @Transactional
    public void deleteAsset(Long id) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 检查 shot_asset_ref 是否有关联
        LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(ShotAssetRef::getAssetId, id);
        Long refCount = shotAssetRefMapper.selectCount(refWrapper);
        if (refCount > 0) {
            throw new BusinessException(ErrorCode.ASSET_REFERENCED);
        }

        assetMapper.deleteById(id);
        log.info("删除资产成功, assetId: {}", id);
    }

    @Override
    public void confirmAsset(Long id) {
        Asset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        asset.setStatus(AssetStatus.CONFIRMED);
        assetMapper.updateById(asset);
        log.info("确认资产成功, assetId: {}", id);
    }

    @Override
    public PageResult<ShotReferenceVO> getAssetReferences(Long assetId, int page, int size) {
        Asset asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 分页查询被引用的分镜
        Page<ShotAssetRef> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ShotAssetRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(ShotAssetRef::getAssetId, assetId);

        IPage<ShotAssetRef> refPage = shotAssetRefMapper.selectPage(pageParam, refWrapper);

        List<ShotAssetRef> refs = refPage.getRecords();
        if (refs.isEmpty()) {
            PageResult<ShotReferenceVO> result = new PageResult<>();
            result.setList(List.of());
            result.setTotal(refPage.getTotal());
            result.setPage((int) refPage.getCurrent());
            result.setSize((int) refPage.getSize());
            result.setHasNext(false);
            return result;
        }

        // 批量查询分镜，避免 N+1
        List<Long> shotIds = refs.stream()
                .map(ShotAssetRef::getShotId)
                .distinct()
                .toList();
        Map<Long, Shot> shotMap = shotMapper.selectBatchIds(shotIds).stream()
                .collect(Collectors.toMap(Shot::getId, s -> s));

        // 批量查询分场，避免 N+1
        List<Long> sceneIds = shotMap.values().stream()
                .map(Shot::getSceneId)
                .distinct()
                .toList();
        Map<Long, Scene> sceneMap = sceneMapper.selectBatchIds(sceneIds).stream()
                .collect(Collectors.toMap(Scene::getId, s -> s));

        List<ShotReferenceVO> voList = refs.stream()
                .map(ref -> {
                    ShotReferenceVO vo = new ShotReferenceVO();
                    vo.setShotId(ref.getShotId());

                    Shot shot = shotMap.get(ref.getShotId());
                    if (shot != null) {
                        vo.setSceneId(shot.getSceneId());
                        vo.setShotStatus(shot.getStatus());

                        Scene scene = sceneMap.get(shot.getSceneId());
                        if (scene != null) {
                            vo.setEpisodeId(scene.getEpisodeId());
                        }
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        PageResult<ShotReferenceVO> result = new PageResult<>();
        result.setList(voList);
        result.setTotal(refPage.getTotal());
        result.setPage((int) refPage.getCurrent());
        result.setSize((int) refPage.getSize());
        result.setHasNext(refPage.getCurrent() * refPage.getSize() < refPage.getTotal());
        return result;
    }

    /**
     * Asset 转 VO
     */
    private AssetVO toVO(Asset asset) {
        AssetVO vo = new AssetVO();
        vo.setId(asset.getId());
        vo.setProjectId(asset.getProjectId());
        vo.setAssetType(asset.getAssetType());
        vo.setName(asset.getName());
        vo.setDescription(asset.getDescription());
        vo.setReferenceImages(asset.getReferenceImages());
        vo.setStylePreset(asset.getStylePreset());
        vo.setStatus(asset.getStatus());
        vo.setCreateTime(asset.getCreateTime());
        vo.setUpdateTime(asset.getUpdateTime());
        return vo;
    }
}
