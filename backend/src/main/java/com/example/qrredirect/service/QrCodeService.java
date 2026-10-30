package com.example.qrredirect.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ch.qos.logback.classic.Logger;
import jakarta.persistence.criteria.Predicate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    private static final int MAX_PAGE_SIZE = 100;

    /** Newest first. Any filter left null is ignored; filters combine with AND. */
    public Page<QrCode> search(Long companyId, Long id, Boolean active, int page, int size) {
        Specification<QrCode> spec = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            if (companyId != null) filters.add(cb.equal(root.get("company").get("id"), companyId));
            if (id != null)        filters.add(cb.equal(root.get("id"), id));
            if (active != null)    filters.add(cb.equal(root.get("active"), active));
            return cb.and(filters.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));   // id breaks ties so pages are stable

        return repo.findAll(spec, pageable);
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
            String url = policy.validate(targetUrl);
            if (!url.equals(qr.getTargetUrl())) {
                qr.setTargetUrl(url);
                qr.setScanCount(0);
            }
        }
        if (active != null) {
            qr.setActive(active);
        }
        return repo.save(qr);
    }

    @CacheEvict(cacheNames = "targets", key = "#result.shortCode")
    public void delete(Long id) {
        QrCode qr = get(id);        // 404 if it doesn't exist
        repo.delete(qr);
    }

    Logger log;

    public void recordScan(String shortCode) {
        try {
            repo.incrementScans(shortCode);
        } catch (RuntimeException e) {
            log.warn("Could not record scan for {}", shortCode, e);   // never break a redirect over a counter
        }
    }
}