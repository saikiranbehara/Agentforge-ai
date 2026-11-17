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
        } else if (clazz.equals(GeneratedFileBundle.class)) {
            return (T) buildContextualFileBundle(lower);
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

            modules.add(new RequirementSpec.ModuleSpec("Authentication", "Handles customer and administrator registration, secure login, password resets, and JWT rotation.", List.of("Registration", "Login", "Profile Management")));
            modules.add(new RequirementSpec.ModuleSpec("Catalog & Products", "Manages product listing, search filters, pagination, and inventory tracking.", List.of("Product CRUD", "Category Filter", "Search Index")));
            modules.add(new RequirementSpec.ModuleSpec("Cart & Checkout", "Calculates subtotals, taxes, shipping discounts, and processes order checkout.", List.of("Cart Items", "Discount Codes", "Stripe Checkout")));
            modules.add(new RequirementSpec.ModuleSpec("Order Management", "Stores order history, itemizes line items, and updates delivery tracking.", List.of("Order Status", "Receipt Generation", "Admin Fulfillment")));
        } else {
            functional.add("User authentication with secure password encryption and session management");
            functional.add("Data entity creation, reading, updating, and deletion (CRUD) with validation");
            functional.add("Interactive dashboard with status summaries and recent event tracking");
            functional.add("Search, filtering, and sorting capabilities across all entity records");

            modules.add(new RequirementSpec.ModuleSpec("Core Auth", "User management and access control.", List.of("Login", "Registration", "User Roles")));
            modules.add(new RequirementSpec.ModuleSpec("Domain Engine", "Primary business logic and state management.", List.of("Create Record", "Update State", "Batch Processing")));
            modules.add(new RequirementSpec.ModuleSpec("Reporting", "Analytical summary and metric visualization.", List.of("Status Metrics", "Audit Logs", "Data Export")));
        }

        return RequirementSpec.builder()
                .applicationName(appName)
                .summary("Autonomous software suite engineered to meet enterprise requirements with high availability, security, and scalability.")
                .functionalRequirements(functional)
                .nonFunctionalRequirements(List.of(
                        "99.9% uptime SLA with sub-200ms API response latency on standard read endpoints",
                        "Stateless authentication using industry-standard HMAC-SHA256 JWT tokens",
                        "OWASP Top 10 compliance: SQL injection protection via parameterized queries, XSS sanitization, and CORS enforcement",
                        "Relational database design in third normal form (3NF) with referential integrity constraints",
                        "Responsive UI compatible with desktop, tablet, and mobile displays"
                ))
                .modules(modules)
                .userStories(List.of(
                        "As a user, I want to securely register and log in so that my data is protected.",
                        "As a customer, I want to search and filter products quickly so that I can find items easily.",
                        "As an administrator, I want to manage system entities and inspect platform statistics from a centralized dashboard."
                ))
                .dependencies(List.of(
                        "Spring Boot 3.3.5 (Spring Web, Data JPA, Security, Validation)",
                        "PostgreSQL 16 with pgvector extension",
                        "React 19 + TypeScript + Vite + Tailwind CSS",
                        "Lombok & Jackson Databind",
                        "JUnit 5 & Mockito"
                ))
                .assumptions(List.of(
                        "The application will be deployed as containerized Docker microservices.",
                        "PostgreSQL database will be accessible via standard TCP port 5432.",
                        "Client browser supports ECMAScript 2022 and modern HTML5 APIs."
                ))
                .build();
    }

    private SystemArchitectureSpec buildContextualArchitectureSpec(String prompt) {
        boolean isECommerce = prompt.contains("shop") || prompt.contains("cart") || prompt.contains("store") || prompt.contains("order") || prompt.contains("product");

        List<SystemArchitectureSpec.TableSpec> tables = new ArrayList<>();
        List<SystemArchitectureSpec.ApiEndpointSpec> endpoints = new ArrayList<>();

        // User table
        tables.add(SystemArchitectureSpec.TableSpec.builder()
                .tableName("users")
                .description("Stores registered user credentials, contact details, and role assignments.")
                .columns(List.of(
                        new SystemArchitectureSpec.ColumnSpec("id", "BIGSERIAL", true, false, "Primary Key identifier"),
                        new SystemArchitectureSpec.ColumnSpec("name", "VARCHAR(100)", false, false, "Full user display name"),
                        new SystemArchitectureSpec.ColumnSpec("email", "VARCHAR(150)", false, false, "Unique email address used for login"),
                        new SystemArchitectureSpec.ColumnSpec("password_hash", "VARCHAR(255)", false, false, "BCrypt encrypted password hash"),
                        new SystemArchitectureSpec.ColumnSpec("role", "VARCHAR(50)", false, false, "User role: ROLE_USER, ROLE_ADMIN, ROLE_SELLER"),
                        new SystemArchitectureSpec.ColumnSpec("created_at", "TIMESTAMP", false, false, "Account creation timestamp")
                ))
                .relationships(List.of("1:N with orders", "1:1 with shopping_cart"))
                .build());

        if (isECommerce) {
            // Product table
            tables.add(SystemArchitectureSpec.TableSpec.builder()
                    .tableName("products")
                    .description("Catalog products with pricing, stock counts, categories, and descriptions.")
                    .columns(List.of(
                            new SystemArchitectureSpec.ColumnSpec("id", "BIGSERIAL", true, false, "Primary Key"),
                            new SystemArchitectureSpec.ColumnSpec("name", "VARCHAR(200)", false, false, "Product title"),
                            new SystemArchitectureSpec.ColumnSpec("description", "TEXT", false, true, "Detailed product description"),
                            new SystemArchitectureSpec.ColumnSpec("price", "NUMERIC(10,2)", false, false, "Unit price in USD"),
                            new SystemArchitectureSpec.ColumnSpec("stock_quantity", "INTEGER", false, false, "Available inventory count"),
                            new SystemArchitectureSpec.ColumnSpec("category", "VARCHAR(100)", false, false, "Product categorization tag"),
                            new SystemArchitectureSpec.ColumnSpec("image_url", "VARCHAR(500)", false, true, "Product thumbnail link"),
                            new SystemArchitectureSpec.ColumnSpec("created_at", "TIMESTAMP", false, false, "Creation timestamp")
                    ))
                    .relationships(List.of("1:N with order_items", "1:N with cart_items"))
                    .build());

            // Order table
            tables.add(SystemArchitectureSpec.TableSpec.builder()
                    .tableName("orders")
                    .description("Customer transaction orders and delivery tracking.")
                    .columns(List.of(
                            new SystemArchitectureSpec.ColumnSpec("id", "BIGSERIAL", true, false, "Primary Key"),
                            new SystemArchitectureSpec.ColumnSpec("user_id", "BIGINT", false, false, "Foreign Key to users(id)"),
                            new SystemArchitectureSpec.ColumnSpec("total_amount", "NUMERIC(12,2)", false, false, "Total order amount"),
                            new SystemArchitectureSpec.ColumnSpec("status", "VARCHAR(50)", false, false, "Status: PENDING, PAID, SHIPPED, DELIVERED"),
                            new SystemArchitectureSpec.ColumnSpec("shipping_address", "TEXT", false, false, "Full shipping destination"),
                            new SystemArchitectureSpec.ColumnSpec("created_at", "TIMESTAMP", false, false, "Order placement timestamp")
                    ))
                    .relationships(List.of("N:1 with users", "1:N with order_items"))
                    .build());

            endpoints.add(new SystemArchitectureSpec.ApiEndpointSpec("POST", "/api/auth/register", "Register a new customer account", "RegisterRequest", "AuthResponse", "None"));
            endpoints.add(new SystemArchitectureSpec.ApiEndpointSpec("POST", "/api/auth/login", "Authenticate with email and password", "LoginRequest", "AuthResponse", "None"));
            endpoints.add(new SystemArchitectureSpec.ApiEndpointSpec("GET", "/api/products", "Fetch paginated catalog products", "None", "Page<ProductDto>", "None"));
            endpoints.add(new SystemArchitectureSpec.ApiEndpointSpec("GET", "/api/products/{id}", "Get product details by ID", "None", "ProductDto", "None"));
            endpoints.add(new SystemArchitectureSpec.ApiEndpointSpec("POST", "/api/products", "Create a new product listing", "ProductCreateDto", "ProductDto", "ROLE_ADMIN"));
            endpoints.add(new SystemArchitectureSpec.ApiEndpointSpec("GET", "/api/orders/my-orders", "List order history for authenticated user", "None", "List<OrderDto>", "Bearer Token"));
            endpoints.add(new SystemArchitectureSpec.ApiEndpointSpec("POST", "/api/orders", "Submit a new purchase order", "CreateOrderDto", "OrderDto", "Bearer Token"));
        } else {
            tables.add(SystemArchitectureSpec.TableSpec.builder()
                    .tableName("items")
                    .description("Core domain items managed by the platform.")
                    .columns(List.of(
                            new SystemArchitectureSpec.ColumnSpec("id", "BIGSERIAL", true, false, "Primary Key"),
                            new SystemArchitectureSpec.ColumnSpec("user_id", "BIGINT", false, false, "Foreign Key to users(id)"),
                            new SystemArchitectureSpec.ColumnSpec("title", "VARCHAR(255)", false, false, "Item headline or title"),
                            new SystemArchitectureSpec.ColumnSpec("content", "TEXT", false, true, "Detailed content or notes"),
                            new SystemArchitectureSpec.ColumnSpec("status", "VARCHAR(50)", false, false, "Current status: ACTIVE, PENDING, ARCHIVED"),
                            new SystemArchitectureSpec.ColumnSpec("created_at", "TIMESTAMP", false, false, "Creation timestamp")
                    ))
                    .relationships(List.of("N:1 with users"))
                    .build());

            endpoints.add(new SystemArchitectureSpec.ApiEndpointSpec("POST", "/api/auth/register", "Register a new account", "RegisterRequest", "AuthResponse", "None"));
            endpoints.add(new SystemArchitectureSpec.ApiEndpointSpec("POST", "/api/auth/login", "Authenticate user", "LoginRequest", "AuthResponse", "None"));
            endpoints.add(new SystemArchitectureSpec.ApiEndpointSpec("GET", "/api/items", "List all domain items", "None", "List<ItemDto>", "Bearer Token"));
            endpoints.add(new SystemArchitectureSpec.ApiEndpointSpec("POST", "/api/items", "Create a new domain item", "ItemDto", "ItemDto", "Bearer Token"));
            endpoints.add(new SystemArchitectureSpec.ApiEndpointSpec("DELETE", "/api/items/{id}", "Delete an item by ID", "None", "void", "Bearer Token"));
        }

        return SystemArchitectureSpec.builder()
                .architecturePattern("Hexagonal / Clean Layered Architecture")
                .overview("Decoupled multi-tier enterprise architecture separating REST controllers, transactional business services, and JPA repositories.")
                .techStack(Map.of(
                        "backend", "Java 21 / Spring Boot 3.3.5 / Spring Security / Spring Data JPA",
                        "frontend", "React 19 / TypeScript / Vite / Tailwind CSS / Lucide React",
                        "database", "PostgreSQL 16 with pgvector",
                        "testing", "JUnit 5 / Mockito / MockMvc / React Testing Library",
                        "containerization", "Docker & Docker Compose"
                ))
                .databaseSchema(tables)
                .apiEndpoints(endpoints)
                .moduleDependencies(List.of(
                        new SystemArchitectureSpec.ModuleDependencySpec("Web/API Layer", List.of("Service Layer"), "Synchronous in-process method invocation with DTOs"),
                        new SystemArchitectureSpec.ModuleDependencySpec("Service Layer", List.of("Persistence Layer"), "Spring Data JPA Repository interfaces"),
                        new SystemArchitectureSpec.ModuleDependencySpec("Frontend App", List.of("Web/API Layer"), "HTTP REST APIs with JSON payloads over HTTPS")
                ))
                .folderStructure(List.of(
                        "backend/src/main/java/com/app/controller/",
                        "backend/src/main/java/com/app/service/",
                        "backend/src/main/java/com/app/repository/",
                        "backend/src/main/java/com/app/entity/",
                        "backend/src/main/java/com/app/dto/",
                        "backend/src/main/java/com/app/security/",
                        "frontend/src/components/",
                        "frontend/src/pages/",
                        "frontend/src/services/"
                ))
                .designPatterns(List.of(
                        "Repository Pattern for data access abstraction",
                        "Data Transfer Object (DTO) pattern to encapsulate API payloads",
                        "Dependency Injection for loose coupling across service boundaries",
                        "Strategy Pattern for payment gateways and notification dispatchers",
                        "Global Controller Advice for centralized exception handling"
                ))
                .securityConsiderations(List.of(
                        "BCrypt password hashing with work factor 12",
                        "Stateless JSON Web Tokens (JWT) signed with HMAC-SHA256",
                        "Cross-Origin Resource Sharing (CORS) configured with explicit allowed origins",
                        "Spring Security MethodSecurity (@PreAuthorize) for role enforcement",
                        "Prepared statements via Hibernate to completely eliminate SQL injection risk"
                ))
                .build();
    }

    private GeneratedFileBundle buildContextualFileBundle(String prompt) {
        boolean isFrontend = prompt.contains("react") || prompt.contains("frontend") || prompt.contains("tsx");
        List<GeneratedSourceFile> files = new ArrayList<>();

        if (isFrontend) {
            files.add(GeneratedSourceFile.builder()
                    .filePath("frontend/src/types/domain.ts")
                    .language("typescript")
                    .description("TypeScript interfaces for application domain models")
                    .content("""
                            export interface User {
                              id: number;
                              name: string;
                              email: string;
                              role: string;
                            }
                            
                            export interface Product {
                              id: number;
                              name: string;
                              description: string;
                              price: number;
                              stockQuantity: number;
                              category: string;
                              imageUrl?: string;
                            }
                            
                            export interface Order {
                              id: number;
                              totalAmount: number;
                              status: 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED';
                              shippingAddress: string;
                              createdAt: string;
                            }
                            """)
                    .build());

            files.add(GeneratedSourceFile.builder()
                    .filePath("frontend/src/services/domainApi.ts")
                    .language("typescript")
                    .description("Typed API client with JWT authorization header")
                    .content("""
                            import { Product, Order, User } from '../types/domain';
                            
                            const API_BASE = '/api';
                            
                            function headers(): HeadersInit {
                              const token = localStorage.getItem('token');
                              return {
                                'Content-Type': 'application/json',
                                ...(token ? { Authorization: `Bearer ${token}` } : {})
                              };
                            }
                            
                            export const domainApi = {
                              async getProducts(): Promise<Product[]> {
                                const res = await fetch(`${API_BASE}/products`, { headers: headers() });
                                if (!res.ok) throw new Error('Failed to load products');
                                return res.json();
                              },
                              
                              async createOrder(data: { items: any[]; shippingAddress: string }): Promise<Order> {
                                const res = await fetch(`${API_BASE}/orders`, {
                                  method: 'POST',
                                  headers: headers(),
                                  body: JSON.stringify(data)
                                });
                                if (!res.ok) throw new Error('Failed to place order');
                                return res.json();
                              }
                            };
                            """)
                    .build());

            files.add(GeneratedSourceFile.builder()
                    .filePath("frontend/src/pages/CatalogPage.tsx")
                    .language("typescript")
                    .description("Interactive catalog view with search and cart integration")
                    .content("""
                            import React, { useState, useEffect } from 'react';
                            import { domainApi } from '../services/domainApi';
                            import { Product } from '../types/domain';
                            
                            export function CatalogPage() {
                              const [products, setProducts] = useState<Product[]>([]);
                              const [loading, setLoading] = useState(true);
                              
                              useEffect(() => {
                                domainApi.getProducts()
                                  .then(setProducts)
                                  .catch(console.error)
                                  .finally(() => setLoading(false));
                              }, []);
                              
                              if (loading) return <div className="p-8 text-center text-slate-400">Loading catalog...</div>;
                              
                              return (
                                <div className="p-6 max-w-7xl mx-auto">
                                  <h1 className="text-2xl font-bold text-slate-100 mb-6">Product Catalog</h1>
                                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                                    {products.map(p => (
                                      <div key={p.id} className="bg-navy-900 border border-slate-800 rounded-xl p-5 shadow-lg">
                                        <h3 className="font-semibold text-lg text-slate-100">{p.name}</h3>
                                        <p className="text-sm text-slate-400 mt-2">{p.description}</p>
                                        <div className="flex items-center justify-between mt-4">
                                          <span className="text-emerald-400 font-bold">${p.price.toFixed(2)}</span>
                                          <button className="bg-sky-600 hover:bg-sky-500 text-white px-4 py-1.5 rounded-lg text-sm transition">
                                            Add to Cart
                                          </button>
                                        </div>
                                      </div>
                                    ))}
                                  </div>
                                </div>
                              );
                            }
                            """)
                    .build());
        } else {
            // Backend bundle
            files.add(GeneratedSourceFile.builder()
                    .filePath("backend/src/main/java/com/app/entity/Product.java")
                    .language("java")
                    .description("JPA Entity for catalog products")
                    .content("""
                            package com.app.entity;
                            
                            import jakarta.persistence.*;
                            import jakarta.validation.constraints.*;
                            import lombok.*;
                            import java.math.BigDecimal;
                            import java.time.LocalDateTime;
                            
                            @Entity
                            @Table(name = "products")
                            @Data
                            @NoArgsConstructor
                            @AllArgsConstructor
                            @Builder
                            public class Product {
                                @Id
                                @GeneratedValue(strategy = GenerationType.IDENTITY)
                                private Long id;
                                
                                @NotBlank(message = "Product name is required")
                                @Column(nullable = false)
                                private String name;
                                
                                @Column(columnDefinition = "TEXT")
                                private String description;
                                
                                @NotNull(message = "Price is required")
                                @PositiveOrZero(message = "Price must be positive or zero")
                                @Column(nullable = false, precision = 10, scale = 2)
                                private BigDecimal price;
                                
                                @NotNull
                                @Min(0)
                                @Column(nullable = false)
                                private Integer stockQuantity;
                                
                                @Column(nullable = false)
                                private String category;
                                
                                private String imageUrl;
                                
                                @Builder.Default
                                @Column(nullable = false, updatable = false)
                                private LocalDateTime createdAt = LocalDateTime.now();
                            }
                            """)
                    .build());

            files.add(GeneratedSourceFile.builder()
                    .filePath("backend/src/main/java/com/app/repository/ProductRepository.java")
                    .language("java")
                    .description("Spring Data JPA Repository for Product")
                    .content("""
                            package com.app.repository;
                            
                            import com.app.entity.Product;
                            import org.springframework.data.jpa.repository.JpaRepository;
                            import org.springframework.stereotype.Repository;
                            import java.util.List;
                            
                            @Repository
                            public interface ProductRepository extends JpaRepository<Product, Long> {
                                List<Product> findByCategoryIgnoreCase(String category);
                                List<Product> findByNameContainingIgnoreCase(String keyword);
                            }
                            """)
                    .build());

            files.add(GeneratedSourceFile.builder()
                    .filePath("backend/src/main/java/com/app/service/ProductService.java")
                    .language("java")
                    .description("Transactional business service for catalog management")
                    .content("""
                            package com.app.service;
                            
                            import com.app.entity.Product;
                            import com.app.repository.ProductRepository;
                            import lombok.RequiredArgsConstructor;
                            import org.springframework.stereotype.Service;
                            import org.springframework.transaction.annotation.Transactional;
                            import java.util.List;
                            
                            @Service
                            @RequiredArgsConstructor
                            @Transactional(readOnly = true)
                            public class ProductService {
                                private final ProductRepository productRepository;
                                
                                public List<Product> getAllProducts() {
                                    return productRepository.findAll();
                                }
                                
                                public Product getProductById(Long id) {
                                    return productRepository.findById(id)
                                            .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
                                }
                                
                                @Transactional
                                public Product createProduct(Product product) {
                                    return productRepository.save(product);
                                }
                            }
                            """)
                    .build());

            files.add(GeneratedSourceFile.builder()
                    .filePath("backend/src/main/java/com/app/controller/ProductController.java")
                    .language("java")
                    .description("REST Controller for catalog endpoints")
                    .content("""
                            package com.app.controller;
                            
                            import com.app.entity.Product;
                            import com.app.service.ProductService;
                            import jakarta.validation.Valid;
                            import lombok.RequiredArgsConstructor;
                            import org.springframework.http.HttpStatus;
                            import org.springframework.http.ResponseEntity;
                            import org.springframework.web.bind.annotation.*;
                            import java.util.List;
                            
                            @RestController
                            @RequestMapping("/api/products")
                            @RequiredArgsConstructor
                            @CrossOrigin(origins = "*")
                            public class ProductController {
                                private final ProductService productService;
                                
                                @GetMapping
                                public ResponseEntity<List<Product>> getAllProducts() {
                                    return ResponseEntity.ok(productService.getAllProducts());
                                }
                                
                                @GetMapping("/{id}")
                                public ResponseEntity<Product> getProductById(@PathVariable Long id) {
                                    return ResponseEntity.ok(productService.getProductById(id));
                                }
                                
                                @PostMapping
                                public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
                                    return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(product));
                                }
                            }
                            """)
                    .build());
        }

        return GeneratedFileBundle.builder()
                .files(files)
                .summary("Generated " + files.size() + " production-ready files.")
                .build();
    }

    private TestExecutionReport buildContextualTestReport(String prompt) {
        List<TestExecutionReport.TestCaseResult> cases = List.of(
                new TestExecutionReport.TestCaseResult("registerUser_Success", "com.example.app.service.AuthServiceTest", "PASSED", null, null, 142),
                new TestExecutionReport.TestCaseResult("loginUser_ValidCredentials", "com.example.app.service.AuthServiceTest", "PASSED", null, null, 88),
                new TestExecutionReport.TestCaseResult("getAllProducts_ReturnsList", "com.example.app.controller.ProductControllerTest", "PASSED", null, null, 210),
                new TestExecutionReport.TestCaseResult("createOrder_CalculatesTotalCorrectly", "com.example.app.service.OrderServiceTest", "PASSED", null, null, 165),
                new TestExecutionReport.TestCaseResult("createOrder_DecrementsStock", "com.example.app.service.OrderServiceTest", "PASSED", null, null, 105)
        );

        return TestExecutionReport.builder()
                .totalTests(5)
                .passedCount(5)
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
