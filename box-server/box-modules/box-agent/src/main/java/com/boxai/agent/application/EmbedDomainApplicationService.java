package com.boxai.agent.application;

import com.boxai.agent.api.AgentEmbedConfigVO;
import com.boxai.agent.support.AgentEmbedConfigSupport;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.EmbedDomainNormalizer;
import com.boxai.common.security.EmbedDomainTxtLookup;
import com.boxai.common.security.SsrfGuard;
import com.boxai.common.security.SsrfSafeHttpClient;
import com.boxai.domain.agent.Agent;
import com.boxai.domain.agent.AgentRepository;
import com.boxai.domain.publish.EmbedCustomDomain;
import com.boxai.domain.publish.EmbedCustomDomainRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmbedDomainApplicationService {

    private final EmbedCustomDomainRepository embedCustomDomainRepository;
    private final AgentRepository agentRepository;
    private final boolean skipDomainVerify;

    public EmbedDomainApplicationService(EmbedCustomDomainRepository embedCustomDomainRepository,
                                         AgentRepository agentRepository,
                                         @Value("${box.embed.skip-domain-verify:true}") boolean skipDomainVerify) {
        this.embedCustomDomainRepository = embedCustomDomainRepository;
        this.agentRepository = agentRepository;
        this.skipDomainVerify = skipDomainVerify;
    }

    public Optional<EmbedCustomDomain> findByAgentId(Long agentId) {
        return embedCustomDomainRepository.findByAgentId(agentId);
    }

    public Optional<EmbedCustomDomain> findVerifiedByHost(String host) {
        String domain = EmbedDomainNormalizer.normalize(host);
        if (domain.isEmpty()) {
            return Optional.empty();
        }
        return embedCustomDomainRepository.findByDomain(domain)
                .filter(item -> item.getVerified() != null && item.getVerified() == 1);
    }

    public Optional<String> findVerifyTokenByHost(String host) {
        String domain = EmbedDomainNormalizer.normalize(host);
        if (domain.isEmpty()) {
            return Optional.empty();
        }
        return embedCustomDomainRepository.findByDomain(domain).map(EmbedCustomDomain::getVerifyToken);
    }

    @Transactional
    public EmbedCustomDomain syncDomain(Agent agent, String rawDomain) {
        String domain = EmbedDomainNormalizer.normalize(rawDomain);
        if (domain.isEmpty()) {
            embedCustomDomainRepository.deleteByAgentId(agent.getId());
            return null;
        }
        Optional<EmbedCustomDomain> occupied = embedCustomDomainRepository.findByDomain(domain);
        if (occupied.isPresent() && !occupied.get().getAgentId().equals(agent.getId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该自定义域名已被其他智能体占用");
        }
        EmbedCustomDomain existing = embedCustomDomainRepository.findByAgentId(agent.getId()).orElse(null);
        if (existing == null) {
            EmbedCustomDomain created = new EmbedCustomDomain();
            created.setWorkspaceId(agent.getWorkspaceId());
            created.setAgentId(agent.getId());
            created.setDomain(domain);
            created.setVerifyToken(newToken());
            created.setVerified(skipDomainVerify ? 1 : 0);
            return embedCustomDomainRepository.save(created);
        }
        boolean sameDomain = domain.equals(existing.getDomain());
        if (!sameDomain) {
            existing.setDomain(domain);
            existing.setVerifyToken(newToken());
            existing.setVerified(skipDomainVerify ? 1 : 0);
        } else if (skipDomainVerify && (existing.getVerified() == null || existing.getVerified() == 0)) {
            existing.setVerified(1);
        }
        embedCustomDomainRepository.update(existing);
        return existing;
    }

    @Transactional
    public EmbedCustomDomain verify(Long agentId) {
        EmbedCustomDomain domain = embedCustomDomainRepository.findByAgentId(agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "尚未配置自定义域名"));
        if (skipDomainVerify) {
            domain.setVerified(1);
            embedCustomDomainRepository.update(domain);
            return domain;
        }
        String expected = domain.getVerifyToken();
        boolean txtOk = EmbedDomainTxtLookup.containsToken(domain.getDomain(), expected);
        boolean fileOk = expected.equals(fetchWellKnownToken(domain.getDomain()).orElse(null));
        if (!txtOk && !fileOk) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "域名验证失败：请将 CNAME 指向当前站点，并添加 TXT 记录 box-verify=" + expected
                            + "，或在 https://" + domain.getDomain() + "/.well-known/box-domain-verify.txt 放置该令牌");
        }
        domain.setVerified(1);
        embedCustomDomainRepository.update(domain);
        return domain;
    }

    public AgentEmbedConfigVO attach(AgentEmbedConfigVO vo, Long agentId, boolean includeToken) {
        EmbedCustomDomain domain = embedCustomDomainRepository.findByAgentId(agentId).orElse(null);
        if (domain == null) {
            return AgentEmbedConfigSupport.withDomain(
                    vo,
                    vo == null ? "" : vo.customDomain(),
                    false,
                    null,
                    skipDomainVerify);
        }
        return AgentEmbedConfigSupport.withDomain(
                vo,
                domain.getDomain(),
                domain.getVerified() != null && domain.getVerified() == 1,
                includeToken ? domain.getVerifyToken() : null,
                skipDomainVerify);
    }

    public Agent requirePublished(Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_NOT_FOUND, "智能体不存在"));
        if (agent.getPublishedVersionId() == null) {
            throw new BusinessException(ErrorCode.AGENT_NOT_PUBLISHED, "智能体尚未发布");
        }
        return agent;
    }

    private Optional<String> fetchWellKnownToken(String domain) {
        try {
            URI uri = SsrfGuard.validateHttpUrl("https://" + domain + "/.well-known/box-domain-verify.txt");
            HttpClient client = SsrfSafeHttpClient.create(Duration.ofSeconds(8), false);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8)).GET().build();
            HttpResponse<String> response = SsrfSafeHttpClient.send(client, request, false, Duration.ofSeconds(8));
            if (response.statusCode() >= 400 || response.body() == null) {
                return Optional.empty();
            }
            return Optional.of(response.body().trim());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
