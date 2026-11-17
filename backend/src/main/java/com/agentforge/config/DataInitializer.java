package com.agentforge.config;

import com.agentforge.entity.Agent;
import com.agentforge.entity.Project;
import com.agentforge.entity.User;
import com.agentforge.repository.AgentRepository;
import com.agentforge.repository.ProjectRepository;
import com.agentforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAgents();
        initDefaultUserAndSampleProject();
    }

    private void initAgents() {
        if (agentRepository.count() == 0) {
            log.info("Initializing AgentForge 7 Specialized AI Agents...");

            List<Agent> agents = List.of(
                    Agent.builder()
                            .agentName("Requirement Analyst")
                            .agentType("REQUIREMENT_ANALYST")
                            .iconName("FileSearch")
                            .orderIndex(1)
                            .description("Deconstructs user requirements into functional and non-functional specifications, modules, and user stories.")
                            .build(),
                    Agent.builder()
                            .agentName("System Architect")
                            .agentType("SYSTEM_ARCHITECT")
                            .iconName("Cpu")
                            .orderIndex(2)
                            .description("Designs database schemas, REST APIs, package structures, and micro-patterns.")
                            .build(),
                    Agent.builder()
                            .agentName("Backend Developer")
                            .agentType("BACKEND_DEVELOPER")
                            .iconName("Server")
                            .orderIndex(3)
                            .description("Generates production-grade Spring Boot 3 Java entities, repositories, services, and REST controllers.")
                            .build(),
                    Agent.builder()
                            .agentName("Frontend Developer")
                            .agentType("FRONTEND_DEVELOPER")
                            .iconName("Layout")
                            .orderIndex(4)
                            .description("Generates React TypeScript pages, Tailwind CSS layouts, and type-safe API client integrations.")
                            .build(),
                    Agent.builder()
                            .agentName("Testing Agent")
                            .agentType("TESTING_AGENT")
                            .iconName("ShieldAlert")
                            .orderIndex(5)
                            .description("Generates JUnit 5 and Mockito test suites, executes in sandbox, and captures execution traces.")
                            .build(),
                    Agent.builder()
                            .agentName("Code Reviewer")
                            .agentType("CODE_REVIEWER")
                            .iconName("CheckCircle2")
                            .orderIndex(6)
                            .description("Audits code for OWASP security vulnerabilities, bugs, and performance benchmarks.")
                            .build(),
                    Agent.builder()
                            .agentName("Documentation Agent")
                            .agentType("DOCUMENTATION_AGENT")
                            .iconName("BookOpen")
                            .orderIndex(7)
                            .description("Synthesizes README.md, Architecture diagrams, Installation manuals, and OpenAPI specifications.")
                            .build()
            );

            agentRepository.saveAll(agents);
            log.info("Initialized 7 AI Agents successfully.");
        }
    }

    private void initDefaultUserAndSampleProject() {
        User defaultUser = userRepository.findByEmail("demo@agentforge.ai").orElseGet(() -> {
            log.info("Creating default demo user: demo@agentforge.ai");
            return userRepository.save(User.builder()
                    .name("Alex Rivers")
                    .email("demo@agentforge.ai")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role("ROLE_USER")
                    .build());
        });

        if (projectRepository.count() == 0) {
            log.info("Creating initial demonstration project...");
            Project sample = Project.builder()
                    .user(defaultUser)
                    .projectName("E-Commerce Cloud Hub")
                    .description("Full-featured digital commerce platform with inventory tracking and secure order processing.")
                    .requirements("Build an online shopping application with user registration, login, product management, shopping cart, order management, and admin dashboard.")
                    .status("CREATED")
                    .backendFramework("Spring Boot 3.x")
                    .frontendFramework("React + TypeScript")
                    .databaseType("PostgreSQL")
                    .aiModel("Gemini 1.5 Pro")
                    .progressPercentage(0)
                    .build();

            projectRepository.save(sample);
            log.info("Sample project seeded.");
        }
    }
}
