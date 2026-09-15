package com.boxai.model.application;

import com.boxai.domain.plan.Plan;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
