package com.boxai.analytics.application;

import com.boxai.analytics.api.AnalyticsTrendPointVO;
import com.boxai.analytics.api.AnalyticsTrendsVO;
import com.boxai.analytics.api.ModelErrorStatVO;
import com.boxai.analytics.api.PlatformAnalyticsOverviewVO;
import com.boxai.analytics.api.PlatformTopTenantVO;
import com.boxai.analytics.api.TenantAnalyticsDetailVO;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.domain.workspace.Workspace;
import com.boxai.domain.workspace.WorkspaceRepository;
import com.boxai.domain.plan.TenantUsage;
import com.boxai.domain.plan.TenantUsageRepository;
import com.boxai.domain.tenant.Tenant;
import com.boxai.domain.tenant.TenantRepository;
import com.boxai.domain.trace.Execution;
import com.boxai.domain.trace.ExecutionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminPlatformAnalyticsApplicationService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ExecutionRepository executionRepository;
    private final TenantUsageRepository tenantUsageRepository;
    private final TenantRepository tenantRepository;
    private final WorkspaceRepository workspaceRepository;

    public AdminPlatformAnalyticsApplicationService(ExecutionRepository executionRepository,
                                                    TenantUsageRepository tenantUsageRepository,
                                                    TenantRepository tenantRepository,
                                                    WorkspaceRepository workspaceRepository) {
        this.executionRepository = executionRepository;
        this.tenantUsageRepository = tenantUsageRepository;
        this.tenantRepository = tenantRepository;
        this.workspaceRepository = workspaceRepository;
    }

    public PlatformAnalyticsOverviewVO overview(int days) {
        int periodDays = days <= 0 ? 7 : Math.min(days, 90);
        LocalDateTime since = LocalDateTime.now().minusDays(periodDays);
        List<Execution> periodExecutions = executionRepository.listRecent(5000).stream()
                .filter(item -> item.getStartedAt() != null && !item.getStartedAt().isBefore(since))
                .toList();
        int succeeded = (int) periodExecutions.stream().filter(this::succeeded).count();
        double successRate = periodExecutions.isEmpty() ? 0D : succeeded * 100D / periodExecutions.size();
        long avgLatencyMs = (long) periodExecutions.stream()
                .map(Execution::getDurationMs)
                .filter(value -> value != null && value > 0)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0D);
        long periodTokens = periodExecutions.stream()
                .map(Execution::getTotalTokens)
                .filter(value -> value != null && value > 0)
                .mapToLong(Integer::longValue)
                .sum();
        String usagePeriod = LocalDate.now().format(MONTH);
        List<TenantUsage> usages = tenantUsageRepository.listByPeriod(usagePeriod);
        Map<Long, String> tenantNames = tenantRepository.listAll().stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (left, right) -> left));
        int monthAiCalls = usages.stream().mapToInt(item -> item.getAiCalls() == null ? 0 : item.getAiCalls()).sum();
        long monthTokens = usages.stream().mapToLong(item -> item.getTokens() == null ? 0L : item.getTokens()).sum();
        List<PlatformTopTenantVO> topTenants = usages.stream()
                .sorted(Comparator.comparingLong((TenantUsage item) -> item.getTokens() == null ? 0L : item.getTokens())
                        .reversed())
                .limit(8)
                .map(item -> new PlatformTopTenantVO(
                        item.getTenantId(),
                        tenantNames.getOrDefault(item.getTenantId(), "租户 #" + item.getTenantId()),
                        item.getAiCalls() == null ? 0 : item.getAiCalls(),
                        item.getTokens() == null ? 0L : item.getTokens()))
                .toList();
        return new PlatformAnalyticsOverviewVO(
                periodDays,
                usagePeriod,
                tenantRepository.listAll().size(),
                periodExecutions.size(),
                executionRepository.countAll(),
                successRate,
                avgLatencyMs,
                periodTokens,
                periodExecutions.size(),
                monthTokens,
                monthAiCalls,
                topTenants);
    }

    public AnalyticsTrendsVO trends(int days) {
        int periodDays = days <= 0 ? 7 : Math.min(days, 90);
        LocalDate today = LocalDate.now();
        LocalDateTime since = today.minusDays(periodDays - 1).atStartOfDay();
        List<Execution> periodExecutions = executionRepository.listRecent(5000).stream()
                .filter(item -> item.getStartedAt() != null && !item.getStartedAt().isBefore(since))
                .toList();
        Map<LocalDate, List<Execution>> grouped = new LinkedHashMap<>();
        for (int offset = periodDays - 1; offset >= 0; offset--) {
            grouped.put(today.minusDays(offset), new ArrayList<>());
        }
        for (Execution execution : periodExecutions) {
            LocalDate date = execution.getStartedAt().toLocalDate();
            grouped.computeIfAbsent(date, key -> new ArrayList<>()).add(execution);
        }
        List<AnalyticsTrendPointVO> points = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<Execution> items = entry.getValue();
                    int total = items.size();
                    int ok = (int) items.stream().filter(this::succeeded).count();
                    double successRate = total == 0 ? 0D : ok * 100D / total;
                    long avgLatencyMs = (long) items.stream()
                            .map(Execution::getDurationMs)
                            .filter(value -> value != null && value > 0)
                            .mapToLong(Long::longValue)
                            .average()
                            .orElse(0D);
                    return new AnalyticsTrendPointVO(entry.getKey().toString(), total, successRate, avgLatencyMs);
                })
                .toList();
        return new AnalyticsTrendsVO(periodDays, points);
    }

    public TenantAnalyticsDetailVO tenantDetail(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND, "租户不存在"));
        List<Long> workspaceIds = workspaceRepository.listByTenantId(tenantId).stream()
                .map(Workspace::getId)
                .toList();
        List<Execution> executions = executionRepository.listRecent(5000).stream()
                .filter(item -> item.getWorkspaceId() != null && workspaceIds.contains(item.getWorkspaceId()))
                .toList();
        int succeededCount = (int) executions.stream().filter(this::succeeded).count();
        double successRate = executions.isEmpty() ? 0D : succeededCount * 100D / executions.size();
        long totalTokens = executions.stream()
                .map(Execution::getTotalTokens)
                .filter(value -> value != null && value > 0)
                .mapToLong(Integer::longValue)
                .sum();
        List<ModelErrorStatVO> modelErrors = executions.stream()
                .filter(item -> !succeeded(item))
                .collect(Collectors.groupingBy(item -> item.getErrorCode() == null ? "UNKNOWN" : item.getErrorCode(), Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new ModelErrorStatVO(entry.getKey(), entry.getValue().intValue(), executions.size()))
                .toList();
        return new TenantAnalyticsDetailVO(
                tenant.getId(),
                tenant.getName(),
                executions.size(),
                successRate,
                totalTokens,
                modelErrors);
    }

    private boolean succeeded(Execution execution) {
        return "SUCCEEDED".equalsIgnoreCase(execution.getStatus())
                || "SUCCESS".equalsIgnoreCase(execution.getStatus());
    }
}
