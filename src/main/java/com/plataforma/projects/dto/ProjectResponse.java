package com.plataforma.projects.dto;

import com.plataforma.projects.model.EnergyType;
import com.plataforma.projects.model.Project;
import com.plataforma.projects.model.ProjectState;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private ProjectState state;
    private EnergyType energyType;
    private String province;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal installedCapacityMW;
    private BigDecimal totalTokens;
    private BigDecimal tokenPrice;
    private BigDecimal minimumInvestment;
    private BigDecimal expectedAnnualYield;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .ownerId(p.getOwnerId())
                .state(p.getState())
                .energyType(p.getEnergyType())
                .province(p.getProvince())
                .country(p.getCountry())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .installedCapacityMW(p.getInstalledCapacityMW())
                .totalTokens(p.getTotalTokens())
                .tokenPrice(p.getTokenPrice())
                .minimumInvestment(p.getMinimumInvestment())
                .expectedAnnualYield(p.getExpectedAnnualYield())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
