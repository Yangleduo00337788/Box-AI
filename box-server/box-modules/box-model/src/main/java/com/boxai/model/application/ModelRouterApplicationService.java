package com.boxai.model.application;

import com.boxai.domain.platform.PlatformModel;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ModelRouterApplicationService {

    public Long selectBestModelId(List<ModelCandidate> candidates, RoutingPreference preference) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Comparator<ModelCandidate> comparator = switch (preference == null ? RoutingPreference.BALANCED : preference) {
            case COST -> Comparator.comparing(ModelCandidate::priceScore).thenComparing(ModelCandidate::latencyMs);
            case QUALITY -> Comparator.comparing(ModelCandidate::qualityScore).reversed()
                    .thenComparing(ModelCandidate::errorRate);
            case BALANCED -> Comparator.comparingDouble(this::balancedScore).reversed();
        };
        return candidates.stream().sorted(comparator).findFirst().map(ModelCandidate::modelId).orElse(null);
    }

    public Long selectFromPlatformModels(List<PlatformModel> models, RoutingPreference preference) {
        if (models == null || models.isEmpty()) {
            return null;
        }
        List<ModelCandidate> candidates = models.stream()
                .filter(model -> model.getId() != null)
                .map(this::toCandidate)
                .toList();
        return selectBestModelId(candidates, preference);
    }

    public RoutingPreference parsePreference(String raw) {
        if (raw == null || raw.isBlank()) {
            return RoutingPreference.BALANCED;
        }
        try {
            return RoutingPreference.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return RoutingPreference.BALANCED;
        }
    }

    private ModelCandidate toCandidate(PlatformModel model) {
        String code = model.getModelCode() == null ? "" : model.getModelCode().toLowerCase(Locale.ROOT);
        double quality = scoreQuality(code, model);
        double price = scorePrice(code, model);
        long latency = scoreLatency(code);
        return new ModelCandidate(model.getId(), quality, price, latency, 0.01D);
    }

    private double scoreQuality(String code, PlatformModel model) {
        if (code.contains("gpt-4") || code.contains("claude-3-opus") || code.contains("o1")) {
            return 0.95D;
        }
        if (code.contains("claude-3") || code.contains("gpt-4o")) {
            return 0.88D;
        }
        if (code.contains("mini") || code.contains("haiku") || code.contains("flash")) {
            return 0.72D;
        }
        int context = model.getContextWindow() == null ? 8192 : model.getContextWindow();
        return Math.min(0.85D, 0.55D + context / 200000D);
    }

    private double scorePrice(String code, PlatformModel model) {
        if (code.contains("mini") || code.contains("haiku") || code.contains("flash")) {
            return 0.2D;
        }
        if (code.contains("gpt-4") || code.contains("opus") || code.contains("o1")) {
            return 0.9D;
        }
        int context = model.getContextWindow() == null ? 8192 : model.getContextWindow();
        return Math.min(0.8D, 0.25D + context / 300000D);
    }

    private long scoreLatency(String code) {
        if (code.contains("mini") || code.contains("haiku") || code.contains("flash")) {
            return 600L;
        }
        if (code.contains("gpt-4") || code.contains("opus") || code.contains("o1")) {
            return 2200L;
        }
        return 1200L;
    }

    private double balancedScore(ModelCandidate candidate) {
        return candidate.qualityScore() * 0.5 + (1D / Math.max(candidate.priceScore(), 0.01)) * 0.3
                - candidate.errorRate() * 0.2;
    }

    public enum RoutingPreference {
        COST, QUALITY, BALANCED
    }

    public record ModelCandidate(
            Long modelId,
            double qualityScore,
            double priceScore,
            long latencyMs,
            double errorRate
    ) {
    }
}
