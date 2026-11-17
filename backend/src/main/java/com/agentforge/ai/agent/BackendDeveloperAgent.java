package com.agentforge.ai.agent;

import com.agentforge.ai.client.AiClientService;
import com.agentforge.ai.model.GeneratedSourceFile;
import com.agentforge.ai.model.SystemArchitectureSpec;
import com.agentforge.ai.prompt.PromptTemplates;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackendDeveloperAgent implements BaseAgent {

    private final AiClientService aiClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "BACKEND_DEVELOPER";
    }

    @Override
    public String getAgentName() {
        return "Backend Developer";
    }

    @Override
    public AgentExecutionResult execute(AgentContext context) {
        long start = System.currentTimeMillis();
        try {
            log.info("Executing BackendDeveloperAgent for project: {}", context.getProjectId());
            List<GeneratedSourceFile> files = new ArrayList<>();

            // Check if this is a targeted file regeneration or self-healing fix
            if (context.getTargetFilePath() != null && context.getTargetFilePath().endsWith(".java")) {
                files.add(regenerateSingleBackendFile(context));
            } else {
                files.addAll(generateCompleteBackendSuite(context));
            }

            long duration = System.currentTimeMillis() - start;
            return AgentExecutionResult.builder()
                    .success(true)
                    .summary("Successfully generated " + files.size() + " Spring Boot backend source files.")
                    .outputData("Generated " + files.size() + " Java source files (Entities, Repositories, Services, Controllers, DTOs).")
                    .files(files)
                    .executionTimeMs(duration)
                    .build();

        } catch (Exception e) {
            log.error("BackendDeveloperAgent failed", e);
            long duration = System.currentTimeMillis() - start;
            return AgentExecutionResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(duration)
                    .build();
        }
    }

    private List<GeneratedSourceFile> generateCompleteBackendSuite(AgentContext context) {
        List<GeneratedSourceFile> files = new ArrayList<>();
        SystemArchitectureSpec arch = context.getArchitectureSpec();
        String userPrompt = context.getUserPrompt();
        boolean isECommerce = userPrompt != null && (userPrompt.toLowerCase().contains("shop") || userPrompt.toLowerCase().contains("cart") || userPrompt.toLowerCase().contains("product"));

        String basePkg = "com.generated.app";

        // 1. Maven POM
        files.add(GeneratedSourceFile.builder()
                .filePath("backend/pom.xml")
                .language("xml")
                .description("Spring Boot Maven POM with JPA, Security, and PostgreSQL")
                .content("""
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                            <modelVersion>4.0.0</modelVersion>
                            <parent>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-starter-parent</artifactId>
                                <version>3.3.5</version>
                                <relativePath/>
                            </parent>
                            <groupId>com.generated</groupId>
                            <artifactId>generated-app</artifactId>
                            <version>1.0.0</version>
                            <name>Generated Application</name>
                            <properties>
                                <java.version>21</java.version>
                            </properties>
                            <dependencies>
                                <dependency>
                                    <groupId>org.springframework.boot</groupId>
                                    <artifactId>spring-boot-starter-web</artifactId>
                                </dependency>
                                <dependency>
                                    <groupId>org.springframework.boot</groupId>
                                    <artifactId>spring-boot-starter-data-jpa</artifactId>
                                </dependency>
                                <dependency>
                                    <groupId>org.springframework.boot</groupId>
                                    <artifactId>spring-boot-starter-validation</artifactId>
                                </dependency>
                                <dependency>
                                    <groupId>org.springframework.boot</groupId>
                                    <artifactId>spring-boot-starter-security</artifactId>
                                </dependency>
                                <dependency>
                                    <groupId>org.postgresql</groupId>
                                    <artifactId>postgresql</artifactId>
                                    <scope>runtime</scope>
                                </dependency>
                                <dependency>
                                    <groupId>com.h2database</groupId>
                                    <artifactId>h2</artifactId>
                                    <scope>runtime</scope>
                                </dependency>
                                <dependency>
                                    <groupId>org.springframework.boot</groupId>
                                    <artifactId>spring-boot-starter-test</artifactId>
                                    <scope>test</scope>
                                </dependency>
                            </dependencies>
                            <build>
                                <plugins>
                                    <plugin>
                                        <groupId>org.springframework.boot</groupId>
                                        <artifactId>spring-boot-maven-plugin</artifactId>
                                    </plugin>
                                </plugins>
                            </build>
                        </project>
                        """)
                .build());

        // 2. Application entry point
        files.add(GeneratedSourceFile.builder()
                .filePath("backend/src/main/java/com/generated/app/Application.java")
                .language("java")
                .description("Spring Boot Application Main Entry Point")
                .content("""
                        package com.generated.app;

                        import org.springframework.boot.SpringApplication;
                        import org.springframework.boot.autoconfigure.SpringBootApplication;

                        @SpringBootApplication
                        public class Application {
                            public static void main(String[] args) {
                                SpringApplication.run(Application.class, args);
                            }
                        }
                        """)
                .build());

        // 3. User Entity & Product Entity
        files.add(GeneratedSourceFile.builder()
                .filePath("backend/src/main/java/com/generated/app/entity/UserAccount.java")
                .language("java")
                .description("User Account JPA Entity")
                .content("""
                        package com.generated.app.entity;

                        import jakarta.persistence.*;
                        import java.time.LocalDateTime;

                        @Entity
                        @Table(name = "users")
                        public class UserAccount {
                            @Id
                            @GeneratedValue(strategy = GenerationType.IDENTITY)
                            private Long id;

                            @Column(nullable = false, unique = true)
                            private String email;

                            @Column(nullable = false)
                            private String name;

                            @Column(nullable = false)
                            private String passwordHash;

                            @Column(nullable = false)
                            private String role;

                            private LocalDateTime createdAt = LocalDateTime.now();

                            public UserAccount() {}

                            public UserAccount(String email, String name, String passwordHash, String role) {
                                this.email = email;
                                this.name = name;
                                this.passwordHash = passwordHash;
                                this.role = role;
                            }

                            public Long getId() { return id; }
                            public String getEmail() { return email; }
                            public void setEmail(String email) { this.email = email; }
                            public String getName() { return name; }
                            public void setName(String name) { this.name = name; }
                            public String getPasswordHash() { return passwordHash; }
                            public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
                            public String getRole() { return role; }
                            public void setRole(String role) { this.role = role; }
                            public LocalDateTime getCreatedAt() { return createdAt; }
                        }
                        """)
                .build());

        files.add(GeneratedSourceFile.builder()
                .filePath("backend/src/main/java/com/generated/app/entity/Product.java")
                .language("java")
                .description("Product JPA Entity")
                .content("""
                        package com.generated.app.entity;

                        import jakarta.persistence.*;
                        import java.math.BigDecimal;
                        import java.time.LocalDateTime;

                        @Entity
                        @Table(name = "products")
                        public class Product {
                            @Id
                            @GeneratedValue(strategy = GenerationType.IDENTITY)
                            private Long id;

                            @Column(nullable = false)
                            private String name;

                            @Column(length = 1000)
                            private String description;

                            @Column(nullable = false, precision = 10, scale = 2)
                            private BigDecimal price;

                            @Column(nullable = false)
                            private Integer stockQuantity;

                            private String category;
                            private String imageUrl;
                            private LocalDateTime createdAt = LocalDateTime.now();

                            public Product() {}

                            public Product(String name, String description, BigDecimal price, Integer stockQuantity, String category, String imageUrl) {
                                this.name = name;
                                this.description = description;
                                this.price = price;
                                this.stockQuantity = stockQuantity;
                                this.category = category;
                                this.imageUrl = imageUrl;
                            }

                            public Long getId() { return id; }
                            public String getName() { return name; }
                            public void setName(String name) { this.name = name; }
                            public String getDescription() { return description; }
                            public void setDescription(String description) { this.description = description; }
                            public BigDecimal getPrice() { return price; }
                            public void setPrice(BigDecimal price) { this.price = price; }
                            public Integer getStockQuantity() { return stockQuantity; }
                            public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
                            public String getCategory() { return category; }
                            public void setCategory(String category) { this.category = category; }
                            public String getImageUrl() { return imageUrl; }
                            public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
                            public LocalDateTime getCreatedAt() { return createdAt; }
                        }
                        """)
                .build());

        // 4. Product Repository
        files.add(GeneratedSourceFile.builder()
                .filePath("backend/src/main/java/com/generated/app/repository/ProductRepository.java")
                .language("java")
                .description("Spring Data JPA Product Repository")
                .content("""
                        package com.generated.app.repository;

                        import com.generated.app.entity.Product;
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

        // 5. Product DTO
        files.add(GeneratedSourceFile.builder()
                .filePath("backend/src/main/java/com/generated/app/dto/ProductDto.java")
                .language("java")
                .description("Product Data Transfer Object")
                .content("""
                        package com.generated.app.dto;

                        import jakarta.validation.constraints.DecimalMin;
                        import jakarta.validation.constraints.Min;
                        import jakarta.validation.constraints.NotBlank;
                        import jakarta.validation.constraints.NotNull;
                        import java.math.BigDecimal;

                        public class ProductDto {
                            private Long id;

                            @NotBlank(message = "Product name is required")
                            private String name;

                            private String description;

                            @NotNull(message = "Price is required")
                            @DecimalMin(value = "0.01", message = "Price must be greater than zero")
                            private BigDecimal price;

                            @NotNull(message = "Stock quantity is required")
                            @Min(value = 0, message = "Stock cannot be negative")
                            private Integer stockQuantity;

                            private String category;
                            private String imageUrl;

                            public ProductDto() {}

                            public Long getId() { return id; }
                            public void setId(Long id) { this.id = id; }
                            public String getName() { return name; }
                            public void setName(String name) { this.name = name; }
                            public String getDescription() { return description; }
                            public void setDescription(String description) { this.description = description; }
                            public BigDecimal getPrice() { return price; }
                            public void setPrice(BigDecimal price) { this.price = price; }
                            public Integer getStockQuantity() { return stockQuantity; }
                            public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
                            public String getCategory() { return category; }
                            public void setCategory(String category) { this.category = category; }
                            public String getImageUrl() { return imageUrl; }
                            public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
                        }
                        """)
                .build());

        // 6. Product Service Interface & Implementation
        files.add(GeneratedSourceFile.builder()
                .filePath("backend/src/main/java/com/generated/app/service/ProductService.java")
                .language("java")
                .description("Product Business Service")
                .content("""
                        package com.generated.app.service;

                        import com.generated.app.dto.ProductDto;
                        import com.generated.app.entity.Product;
                        import com.generated.app.repository.ProductRepository;
                        import org.springframework.stereotype.Service;
                        import org.springframework.transaction.annotation.Transactional;

                        import java.util.List;
                        import java.util.stream.Collectors;

                        @Service
                        @Transactional
                        public class ProductService {

                            private final ProductRepository productRepository;

                            public ProductService(ProductRepository productRepository) {
                                this.productRepository = productRepository;
                            }

                            @Transactional(readOnly = true)
                            public List<ProductDto> getAllProducts() {
                                return productRepository.findAll().stream()
                                        .map(this::toDto)
                                        .collect(Collectors.toList());
                            }

                            @Transactional(readOnly = true)
                            public ProductDto getProductById(Long id) {
                                Product product = productRepository.findById(id)
                                        .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
                                return toDto(product);
                            }

                            public ProductDto createProduct(ProductDto dto) {
                                Product product = new Product(
                                        dto.getName(),
                                        dto.getDescription(),
                                        dto.getPrice(),
                                        dto.getStockQuantity(),
                                        dto.getCategory(),
                                        dto.getImageUrl()
                                );
                                Product saved = productRepository.save(product);
                                return toDto(saved);
                            }

                            public ProductDto updateProduct(Long id, ProductDto dto) {
                                Product product = productRepository.findById(id)
                                        .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
                                product.setName(dto.getName());
                                product.setDescription(dto.getDescription());
                                product.setPrice(dto.getPrice());
                                product.setStockQuantity(dto.getStockQuantity());
                                product.setCategory(dto.getCategory());
                                product.setImageUrl(dto.getImageUrl());
                                return toDto(productRepository.save(product));
                            }

                            public void deleteProduct(Long id) {
                                if (!productRepository.existsById(id)) {
                                    throw new RuntimeException("Product not found with id: " + id);
                                }
                                productRepository.deleteById(id);
                            }

                            private ProductDto toDto(Product product) {
                                ProductDto dto = new ProductDto();
                                dto.setId(product.getId());
                                dto.setName(product.getName());
                                dto.setDescription(product.getDescription());
                                dto.setPrice(product.getPrice());
                                dto.setStockQuantity(product.getStockQuantity());
                                dto.setCategory(product.getCategory());
                                dto.setImageUrl(product.getImageUrl());
                                return dto;
                            }
                        }
                        """)
                .build());

        // 7. Product REST Controller
        files.add(GeneratedSourceFile.builder()
                .filePath("backend/src/main/java/com/generated/app/controller/ProductController.java")
                .language("java")
                .description("Product RESTful API Controller")
                .content("""
                        package com.generated.app.controller;

                        import com.generated.app.dto.ProductDto;
                        import com.generated.app.service.ProductService;
                        import jakarta.validation.Valid;
                        import org.springframework.http.HttpStatus;
                        import org.springframework.http.ResponseEntity;
                        import org.springframework.web.bind.annotation.*;

                        import java.util.List;

                        @RestController
                        @RequestMapping("/api/products")
                        @CrossOrigin(origins = "*")
                        public class ProductController {

                            private final ProductService productService;

                            public ProductController(ProductService productService) {
                                this.productService = productService;
                            }

                            @GetMapping
                            public ResponseEntity<List<ProductDto>> getAllProducts() {
                                return ResponseEntity.ok(productService.getAllProducts());
                            }

                            @GetMapping("/{id}")
                            public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
                                return ResponseEntity.ok(productService.getProductById(id));
                            }

                            @PostMapping
                            public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
                                return new ResponseEntity<>(productService.createProduct(productDto), HttpStatus.CREATED);
                            }

                            @PutMapping("/{id}")
                            public ResponseEntity<ProductDto> updateProduct(
                                    @PathVariable Long id,
                                    @Valid @RequestBody ProductDto productDto) {
                                return ResponseEntity.ok(productService.updateProduct(id, productDto));
                            }

                            @DeleteMapping("/{id}")
                            public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
                                productService.deleteProduct(id);
                                return ResponseEntity.noContent().build();
                            }
                        }
                        """)
                .build());

        // 8. Application Properties
        files.add(GeneratedSourceFile.builder()
                .filePath("backend/src/main/resources/application.properties")
                .language("properties")
                .description("Spring Boot Application Configuration")
                .content("""
                        spring.application.name=generated-app
                        server.port=8080

                        # Database Configuration (PostgreSQL / H2 fallback)
                        spring.datasource.url=${DB_URL:jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1}
                        spring.datasource.driverClassName=${DB_DRIVER:org.h2.Driver}
                        spring.datasource.username=${DB_USER:sa}
                        spring.datasource.password=${DB_PASS:}

                        spring.jpa.hibernate.ddl-auto=update
                        spring.jpa.show-sql=false
                        spring.jpa.properties.hibernate.format_sql=true
                        """)
                .build());

        return files;
    }

    private GeneratedSourceFile regenerateSingleBackendFile(AgentContext context) {
        String path = context.getTargetFilePath();
        String instructions = context.getCustomInstructions();
        log.info("Regenerating targeted backend file: {} with instructions: {}", path, instructions);

        String prompt = "You are an expert Spring Boot developer. Regenerate or correct this source file: " + path
                + "\nInstructions/Errors to resolve:\n" + (instructions != null ? instructions : "Improve code quality, add missing checks, and ensure clean compilation.");

        String code = aiClient.generateText(PromptTemplates.BACKEND_DEVELOPER_SYSTEM, prompt);
        return GeneratedSourceFile.builder()
                .filePath(path)
                .language("java")
                .description("Regenerated Java file: " + path)
                .content(code)
                .build();
    }
}
