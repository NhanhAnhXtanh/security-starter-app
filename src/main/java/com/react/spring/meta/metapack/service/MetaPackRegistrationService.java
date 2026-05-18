package com.react.spring.meta.metapack.service;

import com.react.spring.meta.metapack.entity.MetaPack;
import com.react.spring.meta.metapack.entity.MetaPackRegistration;
import com.vn.security.core.security.data.SecureDataManager;
import com.vn.security.core.security.data.SecureDataManager.EntityMutation;
import com.vn.security.core.security.data.SecuredLoadQuery;
import com.vn.security.core.security.data.UnconstrainedDataManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MetaPackRegistrationService {

    private static final Class<MetaPackRegistration> REG_CLASS = MetaPackRegistration.class;
    private static final String REG_CODE = "metapackregistration";
    private static final List<String> REG_WRITABLE_ATTRS = List.of(
        "metaPack", "subscriberName", "requestedFields", "apiKey",
        "apiSettings", "status", "customRateLimitPerMinute",
        "customRateLimitPerDay", "expiresAt"
    );

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    private final SecureDataManager secureDataManager;
    // §2.3 system bypass — used ONLY by findByApiKey() which is called from
    // MetaPackPublicController (API-key authenticated, no JWT, no user context).
    // The controller enforces its own access control via apiKey + APPROVED status check.
    private final UnconstrainedDataManager unconstrainedDataManager;

    public MetaPackRegistrationService(
        SecureDataManager secureDataManager,
        UnconstrainedDataManager unconstrainedDataManager
    ) {
        this.secureDataManager = secureDataManager;
        this.unconstrainedDataManager = unconstrainedDataManager;
    }

    public static String generateApiKey() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return "mp_" + base64Encoder.encodeToString(randomBytes).replace("=", "");
    }

    @Transactional(readOnly = true)
    public List<MetaPackRegistration> findByMetaPackId(UUID metaPackId) {
        SecuredLoadQuery query = SecuredLoadQuery.builder()
            .entityCode(REG_CODE)
            .jpql("select r from MetaPackRegistration r where r.metaPack.id = :id")
            .parameter("id", metaPackId)
            .pageable(PageRequest.of(0, 10_000))
            .build();
        return secureDataManager.loadByQuery(REG_CLASS, query).getContent();
    }

    /**
     * Public-endpoint lookup — caller (MetaPackPublicController) authenticates via API key
     * (not JWT). No JWT means no user context, so SecureDataManager.checkCrud would fail.
     * Bypass per rules/data-access.md §2.3: caller already enforces access control above.
     */
    @Transactional(readOnly = true)
    public Optional<MetaPackRegistration> findByApiKey(String apiKey) {
        List<MetaPackRegistration> result = unconstrainedDataManager.loadListByJpql(
            REG_CLASS,
            "select r from MetaPackRegistration r where r.apiKey = :key",
            Map.of("key", apiKey),
            null
        );
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Transactional
    public MetaPackRegistration createRegistration(UUID metaPackId, String subscriberName, String requestedFields) {
        MetaPack metaPack = secureDataManager.loadOne(MetaPack.class, metaPackId)
                .orElseThrow(() -> new IllegalArgumentException("MetaPack not found"));

        MetaPackRegistration reg = new MetaPackRegistration();
        reg.setMetaPack(metaPack);
        reg.setSubscriberName(subscriberName);
        reg.setRequestedFields(requestedFields);
        reg.setStatus("PENDING");

        return secureDataManager.save(REG_CLASS, null, new EntityMutation<>(reg, REG_WRITABLE_ATTRS));
    }

    @Transactional
    public MetaPackRegistration approveRegistration(UUID registrationId, String apiSettings, Integer customLimitPm, Integer customLimitPd) {
        MetaPackRegistration reg = secureDataManager.loadOne(REG_CLASS, registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));

        reg.setStatus("APPROVED");
        reg.setApiKey(generateApiKey());
        reg.setApiSettings(apiSettings);
        reg.setCustomRateLimitPerMinute(customLimitPm);
        reg.setCustomRateLimitPerDay(customLimitPd);

        return secureDataManager.save(REG_CLASS, registrationId, new EntityMutation<>(reg, REG_WRITABLE_ATTRS));
    }

    @Transactional
    public void revokeRegistration(UUID registrationId) {
        MetaPackRegistration reg = secureDataManager.loadOne(REG_CLASS, registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        reg.setStatus("REVOKED");
        secureDataManager.save(REG_CLASS, registrationId, new EntityMutation<>(reg, REG_WRITABLE_ATTRS));
    }
}
