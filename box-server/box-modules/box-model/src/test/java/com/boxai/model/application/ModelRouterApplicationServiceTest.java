package com.boxai.model.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ModelRouterApplicationServiceTest {

    private final ModelRouterApplicationService service = new ModelRouterApplicationService();

    @Test
    void costPreferencePicksCheapestModel() {
        List<ModelRouterApplicationService.ModelCandidate> candidates = List.of(
                new ModelRouterApplicationService.ModelCandidate(1L, 0.95, 0.9, 2000L, 0.01),
                new ModelRouterApplicationService.ModelCandidate(2L, 0.7, 0.2, 800L, 0.01));
        Long selected = service.selectBestModelId(candidates, ModelRouterApplicationService.RoutingPreference.COST);
        assertEquals(2L, selected);
    }

    @Test
    void qualityPreferencePicksHighestQualityModel() {
        List<ModelRouterApplicationService.ModelCandidate> candidates = List.of(
                new ModelRouterApplicationService.ModelCandidate(1L, 0.95, 0.9, 2000L, 0.01),
                new ModelRouterApplicationService.ModelCandidate(2L, 0.7, 0.2, 800L, 0.01));
        Long selected = service.selectBestModelId(candidates, ModelRouterApplicationService.RoutingPreference.QUALITY);
        assertEquals(1L, selected);
    }

    @Test
    void emptyCandidatesReturnsNull() {
        assertNull(service.selectBestModelId(List.of(), ModelRouterApplicationService.RoutingPreference.BALANCED));
    }

    @Test
    void parsePreferenceFallsBackToBalanced() {
        assertEquals(ModelRouterApplicationService.RoutingPreference.BALANCED, service.parsePreference(null));
        assertEquals(ModelRouterApplicationService.RoutingPreference.COST, service.parsePreference("cost"));
        assertEquals(ModelRouterApplicationService.RoutingPreference.BALANCED, service.parsePreference("unknown"));
    }
}
