package com.react.spring.meta.metapack.service;

import com.react.spring.meta.metapack.entity.MetaPack;
import com.react.spring.meta.metapack.entity.MetaPackRegistration;
import com.react.spring.meta.metapack.repository.MetaPackRepository;
import com.react.spring.meta.metapack.repository.MetaPackRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MetaPackRegistrationService {

    @Autowired
    private MetaPackRegistrationRepository registrationRepository;

    @Autowired
    private MetaPackRepository metaPackRepository;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    public static String generateApiKey() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return "mp_" + base64Encoder.encodeToString(randomBytes).replace("=", "");
    }

    @Transactional(readOnly = true)
    public List<MetaPackRegistration> findByMetaPackId(UUID metaPackId) {
        return registrationRepository.findByMetaPackId(metaPackId);
    }

    @Transactional(readOnly = true)
    public Optional<MetaPackRegistration> findByApiKey(String apiKey) {
        return registrationRepository.findByApiKey(apiKey);
    }

    @Transactional
    public MetaPackRegistration createRegistration(UUID metaPackId, String subscriberName, String requestedFields) {
        MetaPack metaPack = metaPackRepository.findById(metaPackId)
                .orElseThrow(() -> new IllegalArgumentException("MetaPack not found"));

        MetaPackRegistration reg = new MetaPackRegistration();
        reg.setMetaPack(metaPack);
        reg.setSubscriberName(subscriberName);
        reg.setRequestedFields(requestedFields);
        reg.setStatus("PENDING");
        
        return registrationRepository.save(reg);
    }

    @Transactional
    public MetaPackRegistration approveRegistration(UUID registrationId, String apiSettings, Integer customLimitPm, Integer customLimitPd) {
        MetaPackRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        
        reg.setStatus("APPROVED");
        reg.setApiKey(generateApiKey());
        reg.setApiSettings(apiSettings);
        reg.setCustomRateLimitPerMinute(customLimitPm);
        reg.setCustomRateLimitPerDay(customLimitPd);
        
        return registrationRepository.save(reg);
    }

    @Transactional
    public void revokeRegistration(UUID registrationId) {
        MetaPackRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        reg.setStatus("REVOKED");
        registrationRepository.save(reg);
    }
}
