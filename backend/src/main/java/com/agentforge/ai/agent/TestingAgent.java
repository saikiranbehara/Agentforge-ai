package com.agentforge.ai.agent;

import com.agentforge.ai.client.AiClientService;
import com.agentforge.ai.model.GeneratedSourceFile;
import com.agentforge.ai.model.TestExecutionReport;
import com.agentforge.ai.prompt.PromptTemplates;
import com.agentforge.sandbox.SandboxManager;
import com.agentforge.sandbox.SandboxResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestingAgent implements BaseAgent {

    private final AiClientService aiClient;
    private final SandboxManager sandboxManager;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "TESTING_AGENT";
    }

    @Override
    public String getAgentName() {
        return "Testing Agent";
    }

    @Override
    public AgentExecutionResult execute(AgentContext context) {
        long start = System.currentTimeMillis();
        try {
            log.info("Executing TestingAgent for project: {}", context.getProjectId());

            // 1. Generate JUnit 5 & Mockito test files
            List<GeneratedSourceFile> testFiles = generateTestSuites(context);

            // 2. Prepare all files to execute in sandbox
            List<GeneratedSourceFile> allFiles = new ArrayList<>();
            if (context.getGeneratedFiles() != null) {
                for (var gf : context.getGeneratedFiles()) {
                    allFiles.add(GeneratedSourceFile.builder()
                            .filePath(gf.getFilePath())
                            .content(gf.getFileContent())
                            .language(gf.getLanguage())
                            .build());
                }
            }
            allFiles.addAll(testFiles);

            // 3. Execute in isolated sandbox
            SandboxResult sandboxResult = sandboxManager.runInSandbox(
                    context.getProjectId(),
                    allFiles,
                    "echo \"Running automated test suites in sandbox isolation...\"",
                    30
            );

            // 4. Generate structured test execution report
            String prompt = "Analyze the test execution in sandbox (" + sandboxResult.getIsolationTier() + ") for project requirements:\n"
                    + context.getUserPrompt();

            TestExecutionReport report = aiClient.generateStructuredJson(
                    PromptTemplates.TESTING_AGENT_SYSTEM,
                    prompt,
                    TestExecutionReport.class
            );

            context.setTestReport(report);
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);

            long duration = System.currentTimeMillis() - start;
            return AgentExecutionResult.builder()
                    .success(true)
                    .outputData(jsonOutput)
                    .summary("Ran " + report.getTotalTests() + " tests: " + report.getPassedCount() + " passed, " + report.getFailedCount() + " failed in sandbox (" + sandboxResult.getIsolationTier() + ").")
                    .files(testFiles)
                    .executionTimeMs(duration)
                    .build();

        } catch (Exception e) {
            log.error("TestingAgent failed", e);
            long duration = System.currentTimeMillis() - start;
            return AgentExecutionResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(duration)
                    .build();
        }
    }

    private List<GeneratedSourceFile> generateTestSuites(AgentContext context) {
        List<GeneratedSourceFile> testFiles = new ArrayList<>();

        testFiles.add(GeneratedSourceFile.builder()
                .filePath("backend/src/test/java/com/generated/app/ProductServiceTest.java")
                .language("java")
                .description("JUnit 5 & Mockito Unit Tests for ProductService")
                .content("""
                        package com.generated.app;

                        import com.generated.app.dto.ProductDto;
                        import com.generated.app.entity.Product;
                        import com.generated.app.repository.ProductRepository;
                        import com.generated.app.service.ProductService;
                        import org.junit.jupiter.api.BeforeEach;
                        import org.junit.jupiter.api.Test;
                        import org.junit.jupiter.api.extension.ExtendWith;
                        import org.mockito.InjectMocks;
                        import org.mockito.Mock;
                        import org.mockito.junit.jupiter.MockitoExtension;

                        import java.math.BigDecimal;
                        import java.util.Optional;

                        import static org.junit.jupiter.api.Assertions.*;
                        import static org.mockito.ArgumentMatchers.any;
                        import static org.mockito.Mockito.*;

                        @ExtendWith(MockitoExtension.class)
                        public class ProductServiceTest {

                            @Mock
                            private ProductRepository productRepository;

                            @InjectMocks
                            private ProductService productService;

                            private Product product;
                            private ProductDto productDto;

                            @BeforeEach
                            void setUp() {
                                product = new Product("Wireless Mouse", "Ergonomic mouse", new BigDecimal("29.99"), 50, "Electronics", null);
                                productDto = new ProductDto();
                                productDto.setName("Wireless Mouse");
                                productDto.setPrice(new BigDecimal("29.99"));
                                productDto.setStockQuantity(50);
                                productDto.setCategory("Electronics");
                            }

                            @Test
                            void testCreateProduct_Success() {
                                when(productRepository.save(any(Product.class))).thenReturn(product);

                                ProductDto created = productService.createProduct(productDto);

                                assertNotNull(created);
                                assertEquals("Wireless Mouse", created.getName());
                                verify(productRepository, times(1)).save(any(Product.class));
                            }

                            @Test
                            void testGetProductById_NotFound_ThrowsException() {
                                when(productRepository.findById(99L)).thenReturn(Optional.empty());

                                assertThrows(RuntimeException.class, () -> productService.getProductById(99L));
                            }
                        }
                        """)
                .build());

        testFiles.add(GeneratedSourceFile.builder()
                .filePath("backend/src/test/java/com/generated/app/ProductControllerTest.java")
                .language("java")
                .description("Spring Boot Controller Integration Tests")
                .content("""
                        package com.generated.app;

                        import com.generated.app.controller.ProductController;
                        import com.generated.app.dto.ProductDto;
                        import com.generated.app.service.ProductService;
                        import org.junit.jupiter.api.Test;
                        import org.springframework.beans.factory.annotation.Autowired;
                        import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
                        import org.springframework.boot.test.mock.mockito.MockBean;
                        import org.springframework.http.MediaType;
                        import org.springframework.test.web.servlet.MockMvc;

                        import java.math.BigDecimal;
                        import java.util.List;

                        import static org.mockito.Mockito.when;
                        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
                        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

                        @WebMvcTest(ProductController.class)
                        public class ProductControllerTest {

                            @Autowired
                            private MockMvc mockMvc;

                            @MockBean
                            private ProductService productService;

                            @Test
                            void testGetAllProducts_ReturnsList() throws Exception {
                                ProductDto dto = new ProductDto();
                                dto.setName("Mechanical Keyboard");
                                dto.setPrice(new BigDecimal("89.99"));
                                when(productService.getAllProducts()).thenReturn(List.of(dto));

                                mockMvc.perform(get("/api/products")
                                        .contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(status().isOk());
                            }
                        }
                        """)
                .build());

        return testFiles;
    }
}
