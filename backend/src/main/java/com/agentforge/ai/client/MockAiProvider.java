package com.agentforge.ai.client;

import com.agentforge.ai.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockAiProvider {

    private final ObjectMapper objectMapper;

    public String generateMockText(String userPrompt) {
        return "Simulated AgentForge Output for: " + userPrompt;
    }

    @SuppressWarnings("unchecked")
    public <T> T generateStructuredMock(String userPrompt, Class<T> clazz) {
        String lower = (userPrompt != null) ? userPrompt.toLowerCase() : "";

        if (clazz.equals(RequirementSpec.class)) {
            return (T) buildContextualRequirementSpec(lower);
        } else if (clazz.equals(SystemArchitectureSpec.class)) {
            return (T) buildContextualArchitectureSpec(lower);
        } else if (clazz.equals(TestExecutionReport.class)) {
            return (T) buildContextualTestReport(lower);
        } else if (clazz.equals(CodeReviewReport.class)) {
            return (T) buildContextualCodeReview(lower);
        } else if (clazz.equals(DocumentationBundle.class)) {
            return (T) buildContextualDocumentation(lower);
        }

        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("Could not instantiate class: {}", clazz.getName(), e);
            return null;
        }
    }

    private RequirementSpec buildContextualRequirementSpec(String prompt) {
        boolean isECommerce = prompt.contains("shop") || prompt.contains("cart") || prompt.contains("store") || prompt.contains("order") || prompt.contains("product");
        boolean isTaskApp = prompt.contains("task") || prompt.contains("todo") || prompt.contains("project");

        String appName = isECommerce ? "E-Commerce Multi-Vendor Hub" : (isTaskApp ? "TaskMaster Workflow Suite" : "Enterprise Management Portal");

        List<String> functional = new ArrayList<>();
        List<RequirementSpec.ModuleSpec> modules = new ArrayList<>();

        if (isECommerce) {
            functional.add("User Registration and JWT-based authentication with role-based access control (CUSTOMER, ADMIN, SELLER)");
            functional.add("Product catalog management with categories, real-time inventory counts, and image links");
            functional.add("Persistent Shopping Cart with live quantity adjustment and pricing calculation");
            functional.add("Order processing workflow with status tracking (PENDING, PAID, SHIPPED, DELIVERED)");
            functional.add("Admin analytics dashboard showing sales volume, active users, and low-inventory alerts");

            modules.add(new RequirementSpec.ModuleSpec("Auth & Identity", "Authentication, registration, password hashing, and token renewal", List.of("Register", "Login", "Profile")));
            modules.add(new RequirementSpec.ModuleSpec("Product Management", "CRUD operations for products, search filters, and inventory", List.of("List Products", "Add Product", "Update Inventory")));
            modules.add(new RequirementSpec.ModuleSpec("Cart & Checkout", "Session/DB backed cart, price tallying, checkout validation", List.of("Add to Cart", "Update Quantity", "Clear Cart")));
            modules.add(new RequirementSpec.ModuleSpec("Order Fulfillment", "Order generation, address association, payment state handling", List.of("Create Order", "Order History", "Order Tracking")));
        } else {
            functional.add("Secure user sign-in and profile management");
            functional.add("Resource entity lifecycle management with full validation");
            functional.add("Interactive dashboard with search, filtering, and pagination");
            functional.add("Export and reporting capabilities with audit logging");

            modules.add(new RequirementSpec.ModuleSpec("Core Management", "Central entities, status transitions, and data storage", List.of("CRUD", "Search", "Filter")));
            modules.add(new RequirementSpec.ModuleSpec("Analytics", "Metrics, charts, and key performance indicators", List.of("KPIs", "Charts", "Exports")));
        }

        List<String> nonFunctional = List.of(
                "Sub-200ms API response time for p95 requests",
                "Strict REST API compliance with HATEOAS principles where suitable",
                "OWASP Top 10 security compliance (SQL injection prevention, XSS prevention)",
                "Stateless backend design capable of horizontal scaling"
        );

        List<String> userStories = List.of(
                "As a customer, I want to browse products and filter by price so I find items quickly.",
                "As a registered user, I want to securely log in and maintain my cart items across devices.",
                "As an admin, I want to view operational metrics to monitor platform health."
        );

        return RequirementSpec.builder()
                .applicationName(appName)
                .summary("A scalable, modern full-stack web application designed with Spring Boot 3 backend and React TypeScript frontend.")
                .functionalRequirements(functional)
                .nonFunctionalRequirements(nonFunctional)
                .modules(modules)
                .userStories(userStories)
                .dependencies(List.of("Spring Data JPA", "Spring Security", "PostgreSQL", "React 19", "Tailwind CSS"))
                .assumptions(List.of("Users have modern web browsers", "PostgreSQL database is available"))
                .build();
    }

    private SystemArchitectureSpec buildContextualArchitectureSpec(String prompt) {
        List<SystemArchitectureSpec.TableSpec> tables = new ArrayList<>();

        tables.add(SystemArchitectureSpec.TableSpec.builder()
                .tableName("users")
                .description("System user accounts and credential hashes")
                .columns(List.of(
                        new SystemArchitectureSpec.ColumnSpec("id", "BIGINT", true, false, "Primary key"),
                        new SystemArchitectureSpec.ColumnSpec("email", "VARCHAR(255)", false, false, "Unique user email"),
                        new SystemArchitectureSpec.ColumnSpec("password_hash", "VARCHAR(255)", false, false, "BCrypt hash"),
                        new SystemArchitectureSpec.ColumnSpec("role", "VARCHAR(50)", false, false, "ROLE_USER, ROLE_ADMIN")
                ))
                .relationships(List.of("One-to-Many with orders"))
                .build());

        tables.add(SystemArchitectureSpec.TableSpec.builder()
                .tableName("products")
                .description("Catalog items, pricing, and stock levels")
                .columns(List.of(
                        new SystemArchitectureSpec.ColumnSpec("id", "BIGINT", true, false, "Primary key"),
                        new SystemArchitectureSpec.ColumnSpec("name", "VARCHAR(255)", false, false, "Product name"),
                        new SystemArchitectureSpec.ColumnSpec("price", "DECIMAL(10,2)", false, false, "Unit price in USD"),
                        new SystemArchitectureSpec.ColumnSpec("stock_quantity", "INTEGER", false, false, "Available stock"),
                        new SystemArchitectureSpec.ColumnSpec("category", "VARCHAR(100)", false, true, "Product category")
                ))
                .relationships(List.of("One-to-Many with order_items"))
                .build());

        tables.add(SystemArchitectureSpec.TableSpec.builder()
                .tableName("orders")
                .description("Customer orders with tracking and total cost")
                .columns(List.of(
                        new SystemArchitectureSpec.ColumnSpec("id", "BIGINT", true, false, "Primary key"),
                        new SystemArchitectureSpec.ColumnSpec("user_id", "BIGINT", false, false, "Foreign key to users"),
                        new SystemArchitectureSpec.ColumnSpec("total_amount", "DECIMAL(10,2)", false, false, "Order total"),
                        new SystemArchitectureSpec.ColumnSpec("status", "VARCHAR(50)", false, false, "PENDING, PAID, SHIPPED")
                ))
                .relationships(List.of("Many-to-One with users", "One-to-Many with order_items"))
                .build());

        List<SystemArchitectureSpec.ApiEndpointSpec> endpoints = List.of(
                new SystemArchitectureSpec.ApiEndpointSpec("POST", "/api/auth/register", "Register new customer", "RegisterRequest", "AuthResponse", "None"),
                new SystemArchitectureSpec.ApiEndpointSpec("POST", "/api/auth/login", "Authenticate customer", "AuthRequest", "AuthResponse", "None"),
                new SystemArchitectureSpec.ApiEndpointSpec("GET", "/api/products", "List active products", "None", "List<ProductDto>", "Optional"),
                new SystemArchitectureSpec.ApiEndpointSpec("POST", "/api/products", "Create catalog item", "ProductCreateDto", "ProductDto", "ROLE_ADMIN"),
                new SystemArchitectureSpec.ApiEndpointSpec("POST", "/api/orders", "Place customer order", "OrderRequestDto", "OrderResponseDto", "ROLE_USER"),
                new SystemArchitectureSpec.ApiEndpointSpec("GET", "/api/orders/my-orders", "List customer orders", "None", "List<OrderResponseDto>", "ROLE_USER")
        );

        List<SystemArchitectureSpec.ModuleDependencySpec> dependencies = List.of(
                new SystemArchitectureSpec.ModuleDependencySpec("OrderModule", List.of("ProductModule", "AuthModule"), "Synchronous service injection with transactional consistency"),
                new SystemArchitectureSpec.ModuleDependencySpec("ProductModule", List.of("AuthModule"), "Role check authorization for inventory mutation")
        );

        List<String> folderStructure = List.of(
                "com.example.app/config",
                "com.example.app/controller",
                "com.example.app/entity",
                "com.example.app/repository",
                "com.example.app/service",
                "com.example.app/dto",
                "frontend/src/components",
                "frontend/src/pages",
                "frontend/src/services"
        );

        return SystemArchitectureSpec.builder()
                .architecturePattern("Layered Clean Hexagonal Architecture (Controller -> Service -> Repository)")
                .overview("Decoupled multi-tier enterprise architecture utilizing Spring Boot 3 REST APIs and a React Single Page Application.")
                .techStack(Map.of(
                        "Backend", "Spring Boot 3.3.5 / Java 21",
                        "Frontend", "React 19 / TypeScript / Vite / Tailwind CSS",
                        "Database", "PostgreSQL with pgvector",
                        "Security", "Spring Security 6 with JWT Stateless Tokens"
                ))
                .databaseSchema(tables)
                .apiEndpoints(endpoints)
                .moduleDependencies(dependencies)
                .folderStructure(folderStructure)
                .designPatterns(List.of("Repository Pattern", "DTO Pattern", "Dependency Injection", "Singleton Services"))
                .securityConsiderations(List.of("BCrypt password hashing (strength 12)", "CORS restricted origin filter", "Parameterized SQL queries against injection"))
                .build();
    }

    private TestExecutionReport buildContextualTestReport(String prompt) {
        List<TestExecutionReport.TestCaseResult> cases = List.of(
                new TestExecutionReport.TestCaseResult("testUserRegistration_Success", "AuthServiceTest", "PASSED", null, null, 142),
                new TestExecutionReport.TestCaseResult("testLogin_InvalidCredentials_ThrowsUnauthorized", "AuthServiceTest", "PASSED", null, null, 88),
                new TestExecutionReport.TestCaseResult("testProductCreation_WithValidPayload", "ProductControllerTest", "PASSED", null, null, 210),
                new TestExecutionReport.TestCaseResult("testOrderPlacement_CalculatesTotalCorrectly", "OrderServiceTest", "PASSED", null, null, 175),
                new TestExecutionReport.TestCaseResult("testInventoryDeduction_OnOrderConfirm", "InventoryServiceTest", "PASSED", null, null, 95)
        );

        return TestExecutionReport.builder()
                .totalTests(cases.size())
                .passedCount(cases.size())
                .failedCount(0)
                .allPassed(true)
                .executionLogs("""
                        [INFO] -------------------------------------------------------
                        [INFO]  T E S T S
                        [INFO] -------------------------------------------------------
                        [INFO] Running com.example.app.service.AuthServiceTest
                        [INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.230 s
                        [INFO] Running com.example.app.controller.ProductControllerTest
                        [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.210 s
                        [INFO] Running com.example.app.service.OrderServiceTest
                        [INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.270 s
                        [INFO] 
                        [INFO] Results:
                        [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
                        [INFO] -------------------------------------------------------
                        [INFO] BUILD SUCCESS
                        """)
                .testCases(cases)
                .build();
    }

    private CodeReviewReport buildContextualCodeReview(String prompt) {
        List<CodeReviewReport.ReviewIssue> issues = List.of(
                new CodeReviewReport.ReviewIssue(
                        "src/main/java/com/example/app/controller/OrderController.java",
                        "WARNING",
                        "PERFORMANCE",
                        45,
                        "Missing pagination parameters on getAllOrders endpoint",
                        "Introduce Pageable parameter with default size 20 to avoid excessive memory usage on large datasets."
                ),
                new CodeReviewReport.ReviewIssue(
                        "src/main/java/com/example/app/service/AuthService.java",
                        "INFO",
                        "STYLE",
                        28,
                        "Hardcoded default token expiration duration",
                        "Extract expiration time to application.properties (@Value) for operational flexibility."
                )
        );

        return CodeReviewReport.builder()
                .qualityScore(94)
                .approved(true)
                .summary("Code passes all security gates. Solid adherence to Spring Boot standards, effective exception handling, clean RESTful patterns.")
                .issues(issues)
                .suggestions(List.of(
                        "Consider enabling Redis caching on the read-heavy /api/products catalog endpoint.",
                        "Add actuator endpoints with Prometheus metric scrapers for production observability."
                ))
                .build();
    }

    private DocumentationBundle buildContextualDocumentation(String prompt) {
        return DocumentationBundle.builder()
                .readme("""
                        # Production Generated Application by AgentForge

                        This enterprise software suite was autonomously architected, coded, tested, and reviewed by the **AgentForge Multi-Agent AI System**.

                        ## Technology Stack
                        - **Backend**: Spring Boot 3.3.5 / Java 21 / Spring Data JPA / Spring Security
                        - **Frontend**: React 19 / TypeScript / Vite / Tailwind CSS
                        - **Database**: PostgreSQL with pgvector support
                        - **Authentication**: Stateless JWT Bearer Tokens

                        ## Quick Start
                        ```bash
                        # 1. Run database & backend
                        cd backend
                        ./mvnw spring-boot:run

                        # 2. Run React frontend
                        cd ../frontend
                        npm install
                        npm run dev
                        ```
                        """)
                .architectureDoc("""
                        # System Architecture Specification

                        The system follows a Hexagonal / Layered Architecture:
                        1. **Web Layer**: RESTful Controllers handling HTTP requests, input validation, and JWT security filter.
                        2. **Service Layer**: Transactional business logic, domain invariants, exception translation.
                        3. **Persistence Layer**: Spring Data JPA repositories with query optimization and index-backed lookups.
                        4. **Presentation Layer**: React Single Page Application utilizing Vite, Tailwind CSS, and typed API service clients.
                        """)
                .installationGuide("""
                        # Installation & Deployment Guide

                        ### Prerequisites
                        - Java 21+ SDK
                        - Node.js 20+
                        - PostgreSQL 15+

                        ### Environment Configuration
                        Create an `.env` file in the root directory:
                        ```env
                        DB_URL=jdbc:postgresql://localhost:5432/app_db
                        DB_USERNAME=postgres
                        DB_PASSWORD=secret
                        JWT_SECRET=super_secret_signing_key_32_characters_minimum
                        ```
                        """)
                .apiDocs("""
                        # REST API Reference Documentation

                        ### Authentication Endpoints
                        - `POST /api/auth/register` - Registers a new user account.
                        - `POST /api/auth/login` - Authenticates user and returns JWT token.

                        ### Business Domain Endpoints
                        - `GET /api/products` - Returns paginated list of catalog products.
                        - `POST /api/products` - Creates a new product (ADMIN role required).
                        - `POST /api/orders` - Places a new customer order.
                        - `GET /api/orders/my-orders` - Returns historical orders for authenticated user.
                        """)
                .deploymentGuide("""
                        # Containerized Docker Deployment

                        Build and launch with Docker Compose:
                        ```bash
                        docker-compose up -d --build
                        ```
                        All services will launch with automated health checks, isolated bridge network, and persistent storage volumes.
                        """)
                .build();
    }
}
