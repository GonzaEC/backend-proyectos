package com.plataforma.projects.service.impl;

import com.plataforma.projects.dto.ProjectRequest;
import com.plataforma.projects.dto.ProjectResponse;
import com.plataforma.projects.event.ProjectEventPublisher;
import com.plataforma.projects.exception.ProjectNotFoundException;
import com.plataforma.projects.exception.ProjectStateException;
import com.plataforma.projects.exception.UnauthorizedProjectAccessException;
import com.plataforma.projects.model.EnergyType;
import com.plataforma.projects.model.Project;
import com.plataforma.projects.model.ProjectState;
import com.plataforma.projects.repository.ProjectRepository;
import com.plataforma.projects.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectEventPublisher eventPublisher;

    @Override
    public Page<ProjectResponse> listProjects(ProjectState state, EnergyType energyType, Pageable pageable) {
        Page<Project> page;
        if (state != null && energyType != null) {
            page = projectRepository.findByActiveTrueAndStateAndEnergyType(state, energyType, pageable);
        } else if (state != null) {
            page = projectRepository.findByActiveTrueAndState(state, pageable);
        } else if (energyType != null) {
            page = projectRepository.findByActiveTrueAndEnergyType(energyType, pageable);
        } else {
            page = projectRepository.findByActiveTrue(pageable);
        }
        return page.map(ProjectResponse::from);
    }

    @Override
    public ProjectResponse getProject(Long id) {
        return ProjectResponse.from(findActiveOrThrow(id));
    }

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectRequest request, Long ownerId) {
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(ownerId)
                .energyType(request.getEnergyType())
                .province(request.getProvince())
                .country(request.getCountry() != null ? request.getCountry() : "Argentina")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .installedCapacityMW(request.getInstalledCapacityMW())
                .totalTokens(request.getTotalTokens())
                .tokenPrice(request.getTokenPrice())
                .minimumInvestment(request.getMinimumInvestment())
                .expectedAnnualYield(request.getExpectedAnnualYield())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        Project saved = projectRepository.save(project);
        eventPublisher.publishProjectCreated(saved);
        return ProjectResponse.from(saved);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request, Long requesterId, boolean isAdmin) {
        Project project = findActiveOrThrow(id);
        checkOwnership(project, requesterId, isAdmin);

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setEnergyType(request.getEnergyType());
        project.setProvince(request.getProvince());
        if (request.getCountry() != null) project.setCountry(request.getCountry());
        project.setLatitude(request.getLatitude());
        project.setLongitude(request.getLongitude());
        project.setInstalledCapacityMW(request.getInstalledCapacityMW());
        project.setTotalTokens(request.getTotalTokens());
        project.setTokenPrice(request.getTokenPrice());
        project.setMinimumInvestment(request.getMinimumInvestment());
        project.setExpectedAnnualYield(request.getExpectedAnnualYield());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());

        return ProjectResponse.from(projectRepository.save(project));
    }

    @Override
    @Transactional
    public void deleteProject(Long id, Long requesterId, boolean isAdmin) {
        Project project = findActiveOrThrow(id);
        checkOwnership(project, requesterId, isAdmin);

        if (!project.canBeDeleted()) {
            throw new ProjectStateException("Solo se pueden eliminar proyectos en estado DRAFT");
        }

        project.setActive(false);
        projectRepository.save(project);
    }

    @Override
    @Transactional
    public ProjectResponse changeState(Long id, ProjectState newState, Long requesterId, boolean isAdmin) {
        Project project = findActiveOrThrow(id);
        checkOwnership(project, requesterId, isAdmin);

        ProjectState oldState = project.getState();
        project.advanceState(newState);
        Project saved = projectRepository.save(project);
        eventPublisher.publishStateChanged(saved, oldState, newState);
        return ProjectResponse.from(saved);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Project findActiveOrThrow(Long id) {
        return projectRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }

    private void checkOwnership(Project project, Long requesterId, boolean isAdmin) {
        if (!isAdmin && !project.getOwnerId().equals(requesterId)) {
            throw new UnauthorizedProjectAccessException();
        }
    }
}
