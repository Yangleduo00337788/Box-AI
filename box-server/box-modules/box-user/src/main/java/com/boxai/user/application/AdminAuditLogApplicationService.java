package com.boxai.user.application;

import com.boxai.common.result.PageResult;
import com.boxai.domain.audit.AuditLog;
import com.boxai.domain.audit.AuditLogQuery;
import com.boxai.domain.audit.AuditLogRepository;
import com.boxai.domain.user.User;
import com.boxai.domain.user.UserRepository;
import com.boxai.user.api.AuditLogVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminAuditLogApplicationService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AdminAuditLogApplicationService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public PageResult<AuditLogVO> page(String action,
                                       String resourceType,
                                       Long userId,
                                       String startTime,
                                       String endTime,
                                       int page,
                                       int pageSize) {
        AuditLogQuery query = new AuditLogQuery();
        query.setAction(trimToNull(action));
        query.setResourceType(trimToNull(resourceType));
        query.setUserId(userId);
        query.setStartTime(parseDateTime(startTime));
        query.setEndTime(parseDateTime(endTime));
        query.setPage(page);
        query.setPageSize(pageSize);
        PageResult<AuditLog> result = auditLogRepository.page(query);
        Set<Long> userIds = result.records().stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> users = userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return new PageResult<>(
                result.records().stream().map(item -> toVO(item, users.get(item.getUserId()))).toList(),
                result.total(),
                result.page(),
                result.pageSize());
    }

    private AuditLogVO toVO(AuditLog auditLog, User user) {
        return new AuditLogVO(
                auditLog.getId(),
                auditLog.getWorkspaceId(),
                auditLog.getUserId(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getNickname(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getResourceName(),
                auditLog.getResult(),
                auditLog.getIpAddress(),
                auditLog.getTraceId(),
                auditLog.getDetail(),
                auditLog.getCreatedAt());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME);
        } catch (DateTimeParseException ex) {
            return LocalDateTime.parse(value.trim());
        }
    }
}
