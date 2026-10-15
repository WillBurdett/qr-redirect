package com.example.qrredirect.service;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.qrredirect.model.Company;
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
    private final CompanyService companies;
    private final ShortCodeGenerator generator;
    private final TargetUrlPolicy policy;

    public QrCodeService(QrCodeRepository repo, CompanyService companies,
                         ShortCodeGenerator generator, TargetUrlPolicy policy) {
        this.repo = repo;
        this.companies = companies;
        this.generator = generator;
        this.policy = policy;
    }

    public QrCode create(Long companyId, String targetUrl) {
        Company company = companies.get(companyId);   // 404 if it doesn't exist
        String url = policy.validate(targetUrl);

        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String shortCode = generator.next();
            if (repo.existsByShortCode(shortCode)) {
                continue;
            }
            QrCode qr = new QrCode();
            qr.setShortCode(shortCode);
            qr.setCompany(company);
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

    /** All codes, newest first, or just one company's when companyId is given. */
    public List<QrCode> list(Long companyId) {
        return companyId == null
                ? repo.findAllByOrderByCreatedAtDesc()
                : repo.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    public QrCode get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QR code not found"));
    }

    /** Reassigns and/or (de)activates a code. Null arguments are left unchanged. */
    @CacheEvict(cacheNames = "targets", key = "#result.shortCode")
    public QrCode update(Long id, String targetUrl, Boolean active) {
        QrCode qr = get(id);
        if (targetUrl != null) {
            qr.setTargetUrl(policy.validate(targetUrl));
        }
        if (active != null) {
            qr.setActive(active);
        }
        return repo.save(qr);
    }

    @CacheEvict(cacheNames = "targets", key = "#result.shortCode")
    public QrCode delete(Long id) {
        QrCode qr = get(id);        // 404 if it doesn't exist
        repo.delete(qr);
        return qr;                  // returned so the cache key can use its shortCode
    }
}