package com.plataforma.projects.service.impl;

import com.plataforma.projects.dto.UserHoldingResponse;
import com.plataforma.projects.exception.ProjectNotFoundException;
import com.plataforma.projects.model.Project;
import com.plataforma.projects.model.UserHolding;
import com.plataforma.projects.repository.ProjectRepository;
import com.plataforma.projects.repository.UserHoldingRepository;
import com.plataforma.projects.service.UserHoldingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserHoldingServiceImpl implements UserHoldingService {

    private final UserHoldingRepository holdingRepository;
    private final ProjectRepository projectRepository;

    @Override
    public Page<UserHoldingResponse> listHolders(Long projectId, Pageable pageable) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }
        return holdingRepository.findByProjectId(projectId, pageable).map(UserHoldingResponse::from);
    }

    @Override
    @Transactional
    public void updateHolding(Long userId, Long projectId, BigDecimal tokensDelta) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        UserHolding holding = holdingRepository
                .findByUserIdAndProjectId(userId, projectId)
                .orElseGet(() -> UserHolding.builder()
                        .userId(userId)
                        .project(project)
                        .tokensAmount(BigDecimal.ZERO)
                        .build());

        BigDecimal newAmount = holding.getTokensAmount().add(tokensDelta);
        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Holding negativo ignorado: userId={} projectId={} delta={}", userId, projectId, tokensDelta);
            return;
        }

        holding.setTokensAmount(newAmount);
        holding.setLastUpdatedAt(LocalDateTime.now());
        holdingRepository.save(holding);
    }
}
