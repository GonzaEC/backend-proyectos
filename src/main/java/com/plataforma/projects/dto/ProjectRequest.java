package com.plataforma.projects.dto;

import com.plataforma.projects.model.EnergyType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProjectRequest {
    private String name;
    private String description;
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
}
