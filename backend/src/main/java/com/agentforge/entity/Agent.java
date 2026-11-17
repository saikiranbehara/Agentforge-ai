package com.agentforge.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_name", nullable = false, unique = true)
    private String agentName;

    @Column(name = "agent_type", nullable = false, unique = true)
    private String agentType; // REQUIREMENT_ANALYST, SYSTEM_ARCHITECT, BACKEND_DEVELOPER, FRONTEND_DEVELOPER, TESTING_AGENT, CODE_REVIEWER, DOCUMENTATION_AGENT

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_name")
    private String iconName;

    @Column(name = "order_index")
    private Integer orderIndex;
}
