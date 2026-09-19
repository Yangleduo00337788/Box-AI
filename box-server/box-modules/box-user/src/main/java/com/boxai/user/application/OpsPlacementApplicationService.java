package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.FileSafetyPolicy;
import com.boxai.domain.ops.OpsPlacement;
import com.boxai.domain.ops.OpsPlacementRepository;
import com.boxai.user.api.CreateOpsPlacementRequest;
import com.boxai.user.api.OpsPlacementVO;
import com.boxai.user.api.UpdateOpsPlacementRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OpsPlacementApplicationService {

    private static final Set<String> C_SLOTS = Set.of("CHAT_HOME", "GLOBAL_ALERT", "CHAT_BANNER", "CHAT_AD");
    private static final Set<String> B_SLOTS = Set.of("ADMIN_HEADER", "ADMIN_BANNER");
    private static final Set<String> AUDIENCES = Set.of("C", "B");
    private static final Set<String> KINDS = Set.of("ANNOUNCEMENT", "PROMO", "BANNER", "AD");
    private static final Set<String> THEMES = Set.of("info", "success", "warning", "error");
    private static final Set<String> STATUSES = Set.of("LISTED", "UNLISTED");
    private static final Set<String> ICON_NAMES = Set.of(
            "star", "gift", "notification", "lightbulb", "shop", "tools", "rocket",
            "check-circle", "info-circle", "thumb-up", "heart", "flag");

    private final OpsPlacementRepository opsPlacementRepository;
    private final OpsMetricsApplicationService opsMetricsApplicationService;

    public OpsPlacementApplicationService(OpsPlacementRepository opsPlacementRepository,
                                          OpsMetricsApplicationService opsMetricsApplicationService) {
        this.opsPlacementRepository = opsPlacementRepository;
        this.opsMetricsApplicationService = opsMetricsApplicationService;
    }

    public List<OpsPlacementVO> listForAdmin() {
        Map<Long, int[]> metrics = opsMetricsApplicationService.metricsSummary();
        return opsPlacementRepository.listAllForAdmin().stream()
                .map(placement -> toVo(placement, metrics))
                .toList();
    }

    public List<OpsPlacementVO> listActive(String slot) {
        return listActiveForAudience(slot, "C");
    }

    public List<OpsPlacementVO> listActiveForAudience(String slot, String audience) {
        String normalizedAudience = normalizeAudience(audience);
        String normalized = trimToNull(slot);
        if (normalized != null) {
            normalized = normalized.toUpperCase(Locale.ROOT);
            if (!slotsForAudience(normalizedAudience).contains(normalized)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "投放位置无效");
            }
        }
        return opsPlacementRepository.listActive(normalized, normalizedAudience, LocalDateTime.now()).stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public OpsPlacementVO create(CreateOpsPlacementRequest request) {
        OpsPlacement placement = new OpsPlacement();
        placement.setAudience(normalizeAudience(request.audience()));
        applySlotKind(placement, placement.getAudience(), request.slot(), request.kind());
        placement.setTitle(request.title().trim());
        placement.setBody(trimToNull(request.body()));
        placement.setLinkUrl(normalizeLink(request.linkUrl()));
        placement.setLinkLabel(trimToNull(request.linkLabel()));
        placement.setIconName(normalizeIconName(request.iconName()));
        placement.setIconUrl(normalizeIconUrl(request.iconUrl()));
        placement.setIconSvg(normalizeIconSvg(request.iconSvg()));
        placement.setImageUrl(normalizeImageUrl(request.imageUrl()));
        placement.setTheme(normalizeTheme(request.theme()));
        placement.setDismissible(Boolean.FALSE.equals(request.dismissible()) ? 0 : 1);
        placement.setStatus("LISTED");
        placement.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        placement.setStartsAt(request.startsAt());
        placement.setEndsAt(request.endsAt());
        validateWindow(placement.getStartsAt(), placement.getEndsAt());
        requireCreativeImage(placement);
        opsPlacementRepository.save(placement);
        return toVo(placement);
    }

    @Transactional
    public OpsPlacementVO update(Long id, UpdateOpsPlacementRequest request) {
        OpsPlacement placement = opsPlacementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "运营位不存在"));
        if (request.audience() != null) {
            placement.setAudience(normalizeAudience(request.audience()));
        }
        String slot = request.slot() == null ? placement.getSlot() : request.slot();
        String kind = request.kind() == null ? placement.getKind() : request.kind();
        applySlotKind(placement, placement.getAudience(), slot, kind);
        if (request.title() != null) {
            placement.setTitle(request.title().trim());
        }
        if (request.body() != null) {
            placement.setBody(trimToNull(request.body()));
        }
        if (request.linkUrl() != null) {
            placement.setLinkUrl(normalizeLink(request.linkUrl()));
        }
        if (request.linkLabel() != null) {
            placement.setLinkLabel(trimToNull(request.linkLabel()));
        }
        if (request.iconName() != null) {
            placement.setIconName(normalizeIconName(request.iconName()));
        }
        if (request.iconUrl() != null) {
            placement.setIconUrl(normalizeIconUrl(request.iconUrl()));
        }
        if (request.iconSvg() != null) {
            placement.setIconSvg(normalizeIconSvg(request.iconSvg()));
        }
        if (request.imageUrl() != null) {
            placement.setImageUrl(normalizeImageUrl(request.imageUrl()));
        }
        if (request.theme() != null) {
            placement.setTheme(normalizeTheme(request.theme()));
        }
        if (request.dismissible() != null) {
            placement.setDismissible(request.dismissible() ? 1 : 0);
        }
        if (request.status() != null) {
            String status = request.status().trim().toUpperCase(Locale.ROOT);
            if (!STATUSES.contains(status)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "状态无效");
            }
            placement.setStatus(status);
        }
        if (request.sortOrder() != null) {
            placement.setSortOrder(request.sortOrder());
        }
        if (request.startsAt() != null) {
            placement.setStartsAt(request.startsAt());
        }
        if (request.endsAt() != null) {
            placement.setEndsAt(request.endsAt());
        }
        validateWindow(placement.getStartsAt(), placement.getEndsAt());
        requireCreativeImage(placement);
        opsPlacementRepository.update(placement);
        return toVo(placement);
    }

    @Transactional
    public void delete(Long id) {
        opsPlacementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "运营位不存在"));
        opsPlacementRepository.delete(id);
    }

    private void applySlotKind(OpsPlacement placement, String audience, String slotRaw, String kindRaw) {
        String normalizedAudience = normalizeAudience(audience);
        String slot = slotRaw == null ? "" : slotRaw.trim().toUpperCase(Locale.ROOT);
        String kind = kindRaw == null ? "" : kindRaw.trim().toUpperCase(Locale.ROOT);
        if (!slotsForAudience(normalizedAudience).contains(slot)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "投放位置无效");
        }
        if ("CHAT_BANNER".equals(slot) || "ADMIN_BANNER".equals(slot)) {
            kind = "BANNER";
        } else if ("CHAT_AD".equals(slot)) {
            kind = "AD";
        } else if ("BANNER".equals(kind) || "AD".equals(kind)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该位置不支持图片投放");
        }
        if (!KINDS.contains(kind)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内容类型无效");
        }
        placement.setSlot(slot);
        placement.setKind(kind);
    }

    private String normalizeTheme(String theme) {
        String value = theme == null || theme.isBlank() ? "info" : theme.trim().toLowerCase(Locale.ROOT);
        if (!THEMES.contains(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "提示样式无效");
        }
        return value;
    }

    private String normalizeLink(String linkUrl) {
        String value = trimToNull(linkUrl);
        if (value == null) {
            return null;
        }
        if (value.startsWith("/") || value.startsWith("https://") || value.startsWith("http://")) {
            return value;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "链接需为站内路径或 http(s) 地址");
    }

    private String normalizeIconName(String iconName) {
        String value = trimToNull(iconName);
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!ICON_NAMES.contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持该图标");
        }
        return normalized;
    }

    private String normalizeIconUrl(String iconUrl) {
        String value = trimToNull(iconUrl);
        if (value == null) {
            return null;
        }
        if (value.startsWith("/api/v1/public-assets/")
                && value.matches("/api/v1/public-assets/[0-9a-fA-F-]{36}\\.(png|jpg|jpeg|svg)")) {
            return value;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传 PNG / JPG / SVG 图标");
    }

    private String normalizeImageUrl(String imageUrl) {
        String value = trimToNull(imageUrl);
        if (value == null) {
            return null;
        }
        if (value.startsWith("/api/v1/public-assets/")
                && value.matches("/api/v1/public-assets/[0-9a-fA-F-]{36}\\.(png|jpg|jpeg)")) {
            return value;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传 PNG / JPG 图片");
    }

    private void requireCreativeImage(OpsPlacement placement) {
        if (("CHAT_BANNER".equals(placement.getSlot())
                || "CHAT_AD".equals(placement.getSlot())
                || "ADMIN_BANNER".equals(placement.getSlot()))
                && trimToNull(placement.getImageUrl()) == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传投放图片");
        }
    }

    private String normalizeIconSvg(String iconSvg) {
        return FileSafetyPolicy.sanitizeInlineSvg(iconSvg);
    }

    private void validateWindow(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt != null && endsAt != null && endsAt.isBefore(startsAt)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "结束时间不能早于开始时间");
        }
    }

    private OpsPlacementVO toVo(OpsPlacement placement, Map<Long, int[]> metrics) {
        int[] values = metrics.getOrDefault(placement.getId(), new int[] {0, 0});
        return new OpsPlacementVO(
                placement.getId(),
                placement.getAudience() == null ? "C" : placement.getAudience(),
                placement.getSlot(),
                placement.getKind(),
                placement.getTitle(),
                placement.getBody(),
                placement.getLinkUrl(),
                placement.getLinkLabel(),
                placement.getIconName(),
                placement.getIconUrl(),
                placement.getIconSvg(),
                placement.getImageUrl(),
                placement.getTheme(),
                placement.getDismissible() == null || placement.getDismissible() == 1,
                placement.getStatus(),
                placement.getSortOrder(),
                placement.getStartsAt(),
                placement.getEndsAt(),
                placement.getCreatedAt(),
                values[0],
                values[1]);
    }

    private OpsPlacementVO toVo(OpsPlacement placement) {
        return toVo(placement, Map.of());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeAudience(String audience) {
        String value = audience == null || audience.isBlank() ? "C" : audience.trim().toUpperCase(Locale.ROOT);
        if (!AUDIENCES.contains(value)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "投放端无效");
        }
        return value;
    }

    private Set<String> slotsForAudience(String audience) {
        return "B".equals(audience) ? B_SLOTS : C_SLOTS;
    }
}
