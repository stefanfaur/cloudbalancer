package com.cloudbalancer.dispatcher.persistence;

import jakarta.persistence.*;

import java.util.Map;

@Entity
@Table(name = "scheduling_config")
public class SchedulingConfigRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy_name", nullable = false)
    private String strategyName;

    @Column(columnDefinition = "jsonb", nullable = false)
    @Convert(converter = WeightsConverter.class)
    private Map<String, Integer> weights;

    protected SchedulingConfigRecord() {}

    public SchedulingConfigRecord(String strategyName, Map<String, Integer> weights) {
        this.strategyName = strategyName;
        this.weights = weights;
    }

    public Long getId() { return id; }
    public String getStrategyName() { return strategyName; }
    public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
    public Map<String, Integer> getWeights() { return weights; }
    public void setWeights(Map<String, Integer> weights) { this.weights = weights; }
}
