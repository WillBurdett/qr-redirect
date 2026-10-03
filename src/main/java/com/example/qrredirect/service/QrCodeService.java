package com.example.qrredirect.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.qrredirect.model.QrCode;
import com.example.qrredirect.repo.QrCodeRepository;

/**
 * Note: methods here are deliberately not @Transactional. Each repository call commits on its
 * own, which lets create() retry cleanly after a rare short code collision and means the cache
 * eviction in update() happens after the new target has been committed.
 */
@Service
public class QrCodeService {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final QrCodeRepository repo;
    private final ShortCodeGenerator generator;
    private final TargetUrlPolicy policy;

    public QrCodeService(QrCodeRepository repo, ShortCodeGenerator generator, TargetUrlPolicy policy) {
        this.repo = repo;
        this.generator = generator;
        this.policy = policy;
    }

    public QrCode create(String companyId, String targetUrl) {
        String url = policy.validate(targetUrl);

        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String shortCode = generator.next();
            if (repo.existsByShortCode(shortCode)) {
                continue;
            }
            QrCode qr = new QrCode();
            qr.setShortCode(shortCode);
            qr.setCompanyId(companyId);
            qr.setTargetUrl(url);
            try {
                return repo.saveAndFlush(qr);
            } catch (DataIntegrityViolationException collision) {
                // Another request grabbed the same code between the check and the insert. Try again.
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not allocate a short code");
    }

    /**
     * The hot path: short code -> target URL. Only active codes resolve. Misses are not cached.
     * (Spring unwraps the Optional, so "#result == null" means "empty".)
     */
    @Cacheable(cacheNames = "targets", key = "#shortCode", unless = "#result == null")
    public Optional<String> resolveTarget(String shortCode) {
        return repo.findByShortCode(shortCode)
                .filter(QrCode::isActive)
                .map(QrCode::getTargetUrl);
    }

    public List<QrCode> list(String companyId) {
        return repo.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    /** Looks up a code owned by this company. Other companies' codes look like they don't exist. */
    public QrCode get(String companyId, Long id) {
        return repo.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QR code not found"));
    }

    /** Reassigns and/or (de)activates a code. Null arguments are left unchanged. */
    @CacheEvict(cacheNames = "targets", key = "#result.shortCode")
    public QrCode update(String companyId, Long id, String targetUrl, Boolean active) {
        QrCode qr = get(companyId, id);
        if (targetUrl != null) {
            qr.setTargetUrl(policy.validate(targetUrl));
        }
        if (active != null) {
            qr.setActive(active);
        }
        return repo.save(qr);
    }
}
