package com.boxai.infrastructure.persistence.repository;

import com.boxai.domain.ops.OpsPlacement;
import com.boxai.domain.ops.OpsPlacementRepository;
import com.boxai.infrastructure.persistence.entity.OpsPlacementDO;
import com.boxai.infrastructure.persistence.mapper.OpsPlacementMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OpsPlacementRepositoryImpl implements OpsPlacementRepository {

    private final OpsPlacementMapper mapper;

    public OpsPlacementRepositoryImpl(OpsPlacementMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<OpsPlacement> findById(Long id) {
        return Optional.ofNullable(mapper.selectOneById(id)).map(this::toDomain);
    }

    @Override
    public List<OpsPlacement> listAllForAdmin() {
        return mapper.selectListByQuery(QueryWrapper.create()
                        .orderBy("sort_order", false)
                        .orderBy("id", true))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<OpsPlacement> listActive(String slot, String audience, LocalDateTime now) {
        String normalizedAudience = audience == null || audience.isBlank() ? "C" : audience.trim().toUpperCase();
        QueryWrapper query = QueryWrapper.create()
                .eq("status", "LISTED")
                .eq("audience", normalizedAudience)
                .orderBy("sort_order", false)
                .orderBy("id", true);
        if (slot != null && !slot.isBlank()) {
            query.eq("slot", slot.trim());
        }
        return mapper.selectListByQuery(query).stream()
                .map(this::toDomain)
                .filter(item -> isEffective(item, now))
                .toList();
    }

    private boolean isEffective(OpsPlacement item, LocalDateTime now) {
        if (item.getStartsAt() != null && item.getStartsAt().isAfter(now)) {
            return false;
        }
        return item.getEndsAt() == null || !item.getEndsAt().isBefore(now);
    }

    @Override
    public OpsPlacement save(OpsPlacement placement) {
        OpsPlacementDO row = toDo(placement);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        row.setDeleted(0);
        mapper.insert(row);
        placement.setId(row.getId());
        placement.setCreatedAt(row.getCreatedAt());
        placement.setUpdatedAt(row.getUpdatedAt());
        return placement;
    }

    @Override
    public void update(OpsPlacement placement) {
        LocalDateTime now = LocalDateTime.now();
        UpdateChain<OpsPlacementDO> chain = UpdateChain.of(OpsPlacementDO.class)
                .set("audience", placement.getAudience())
                .set("slot", placement.getSlot())
                .set("kind", placement.getKind())
                .set("title", placement.getTitle())
                .set("theme", placement.getTheme())
                .set("dismissible", placement.getDismissible())
                .set("status", placement.getStatus())
                .set("sort_order", placement.getSortOrder())
                .set("updated_at", now);
        setNullable(chain, "body", placement.getBody());
        setNullable(chain, "link_url", placement.getLinkUrl());
        setNullable(chain, "link_label", placement.getLinkLabel());
        setNullable(chain, "icon_name", placement.getIconName());
        setNullable(chain, "icon_url", placement.getIconUrl());
        setNullable(chain, "icon_svg", placement.getIconSvg());
        setNullable(chain, "image_url", placement.getImageUrl());
        setNullable(chain, "starts_at", placement.getStartsAt());
        setNullable(chain, "ends_at", placement.getEndsAt());
        chain.where("id = ?", placement.getId()).update();
        placement.setUpdatedAt(now);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private OpsPlacementDO toDo(OpsPlacement placement) {
        OpsPlacementDO row = new OpsPlacementDO();
        row.setAudience(placement.getAudience() == null ? "C" : placement.getAudience());
        row.setSlot(placement.getSlot());
        row.setKind(placement.getKind());
        row.setTitle(placement.getTitle());
        row.setBody(placement.getBody());
        row.setLinkUrl(placement.getLinkUrl());
        row.setLinkLabel(placement.getLinkLabel());
        row.setIconName(placement.getIconName());
        row.setIconUrl(placement.getIconUrl());
        row.setIconSvg(placement.getIconSvg());
        row.setImageUrl(placement.getImageUrl());
        row.setTheme(placement.getTheme());
        row.setDismissible(placement.getDismissible());
        row.setStatus(placement.getStatus());
        row.setSortOrder(placement.getSortOrder());
        row.setStartsAt(placement.getStartsAt());
        row.setEndsAt(placement.getEndsAt());
        return row;
    }

    private OpsPlacement toDomain(OpsPlacementDO row) {
        OpsPlacement placement = new OpsPlacement();
        placement.setId(row.getId());
        placement.setAudience(row.getAudience() == null ? "C" : row.getAudience());
        placement.setSlot(row.getSlot());
        placement.setKind(row.getKind());
        placement.setTitle(row.getTitle());
        placement.setBody(row.getBody());
        placement.setLinkUrl(row.getLinkUrl());
        placement.setLinkLabel(row.getLinkLabel());
        placement.setIconName(row.getIconName());
        placement.setIconUrl(row.getIconUrl());
        placement.setIconSvg(row.getIconSvg());
        placement.setImageUrl(row.getImageUrl());
        placement.setTheme(row.getTheme());
        placement.setDismissible(row.getDismissible());
        placement.setStatus(row.getStatus());
        placement.setSortOrder(row.getSortOrder());
        placement.setStartsAt(row.getStartsAt());
        placement.setEndsAt(row.getEndsAt());
        placement.setCreatedAt(row.getCreatedAt());
        placement.setUpdatedAt(row.getUpdatedAt());
        return placement;
    }

    private static void setNullable(UpdateChain<OpsPlacementDO> chain, String column, Object value) {
        if (value == null) {
            chain.setRaw(column, "NULL");
            return;
        }
        chain.set(column, value);
    }
}
