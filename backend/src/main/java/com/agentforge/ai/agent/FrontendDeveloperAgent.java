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
public class FrontendDeveloperAgent implements BaseAgent {

    private final AiClientService aiClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getAgentType() {
        return "FRONTEND_DEVELOPER";
    }

    @Override
    public String getAgentName() {
        return "Frontend Developer";
    }

    @Override
    public AgentExecutionResult execute(AgentContext context) {
        long start = System.currentTimeMillis();
        try {
            log.info("Executing FrontendDeveloperAgent for project: {}", context.getProjectId());
            List<GeneratedSourceFile> files = new ArrayList<>();

            if (context.getTargetFilePath() != null && (context.getTargetFilePath().endsWith(".tsx") || context.getTargetFilePath().endsWith(".ts"))) {
                files.add(regenerateSingleFrontendFile(context));
            } else {
                files.addAll(generateCompleteFrontendSuite(context));
            }

            long duration = System.currentTimeMillis() - start;
            return AgentExecutionResult.builder()
                    .success(true)
                    .summary("Successfully generated " + files.size() + " React + TypeScript frontend files.")
                    .outputData("Generated " + files.size() + " frontend files (Vite config, React components, Tailwind styling, API services).")
                    .files(files)
                    .executionTimeMs(duration)
                    .build();

        } catch (Exception e) {
            log.error("FrontendDeveloperAgent failed", e);
            long duration = System.currentTimeMillis() - start;
            return AgentExecutionResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .executionTimeMs(duration)
                    .build();
        }
    }

    private List<GeneratedSourceFile> generateCompleteFrontendSuite(AgentContext context) {
        List<GeneratedSourceFile> files = new ArrayList<>();

        // 1. package.json
        files.add(GeneratedSourceFile.builder()
                .filePath("frontend/package.json")
                .language("json")
                .description("Vite React TypeScript Dependencies")
                .content("""
                        {
                          "name": "generated-client",
                          "private": true,
                          "version": "1.0.0",
                          "type": "module",
                          "scripts": {
                            "dev": "vite",
                            "build": "tsc && vite build",
                            "preview": "vite preview"
                          },
                          "dependencies": {
                            "react": "^18.3.1",
                            "react-dom": "^18.3.1"
                          },
                          "devDependencies": {
                            "@types/react": "^18.3.3",
                            "@types/react-dom": "^18.3.0",
                            "@vitejs/plugin-react": "^4.3.1",
                            "typescript": "^5.5.3",
                            "vite": "^5.4.1"
                          }
                        }
                        """)
                .build());

        // 2. index.html
        files.add(GeneratedSourceFile.builder()
                .filePath("frontend/index.html")
                .language("html")
                .description("HTML5 Application Shell")
                .content("""
                        <!doctype html>
                        <html lang="en">
                          <head>
                            <meta charset="UTF-8" />
                            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                            <title>Generated Application</title>
                            <script src="https://cdn.tailwindcss.com"></script>
                          </head>
                          <body class="bg-slate-900 text-slate-100 min-h-screen">
                            <div id="root"></div>
                            <script type="module" src="/src/main.tsx"></script>
                          </body>
                        </html>
                        """)
                .build());

        // 3. API Client Service
        files.add(GeneratedSourceFile.builder()
                .filePath("frontend/src/services/apiService.ts")
                .language("typescript")
                .description("Type-Safe REST API Client with JWT Bearer Token Support")
                .content("""
                        export interface Product {
                          id?: number;
                          name: string;
                          description: string;
                          price: number;
                          stockQuantity: number;
                          category: string;
                          imageUrl?: string;
                        }

                        const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

                        function getAuthHeaders(): HeadersInit {
                          const token = localStorage.getItem('auth_token');
                          return {
                            'Content-Type': 'application/json',
                            ...(token ? { Authorization: `Bearer ${token}` } : {})
                          };
                        }

                        export const apiService = {
                          async getProducts(): Promise<Product[]> {
                            const res = await fetch(`${BASE_URL}/products`, {
                              headers: getAuthHeaders()
                            });
                            if (!res.ok) throw new Error('Failed to fetch products');
                            return res.json();
                          },

                          async createProduct(product: Product): Promise<Product> {
                            const res = await fetch(`${BASE_URL}/products`, {
                              method: 'POST',
                              headers: getAuthHeaders(),
                              body: JSON.stringify(product)
                            });
                            if (!res.ok) throw new Error('Failed to create product');
                            return res.json();
                          },

                          async deleteProduct(id: number): Promise<void> {
                            const res = await fetch(`${BASE_URL}/products/${id}`, {
                              method: 'DELETE',
                              headers: getAuthHeaders()
                            });
                            if (!res.ok) throw new Error('Failed to delete product');
                          }
                        };
                        """)
                .build());

        // 4. Main React Component App.tsx
        files.add(GeneratedSourceFile.builder()
                .filePath("frontend/src/App.tsx")
                .language("typescript")
                .description("Primary React Application Component with Product Management UI")
                .content("""
                        import React, { useState, useEffect } from 'react';
                        import { apiService, Product } from './services/apiService';

                        export function App() {
                          const [products, setProducts] = useState<Product[]>([]);
                          const [loading, setLoading] = useState<boolean>(true);
                          const [error, setError] = useState<string | null>(null);

                          // Form state
                          const [name, setName] = useState('');
                          const [price, setPrice] = useState('');
                          const [stock, setStock] = useState('');
                          const [category, setCategory] = useState('General');

                          useEffect(() => {
                            loadProducts();
                          }, []);

                          const loadProducts = async () => {
                            try {
                              setLoading(true);
                              const data = await apiService.getProducts();
                              setProducts(data);
                            } catch (err: any) {
                              setError(err.message || 'Error loading products');
                            } finally {
                              setLoading(false);
                            }
                          };

                          const handleCreate = async (e: React.FormEvent) => {
                            e.preventDefault();
                            try {
                              await apiService.createProduct({
                                name,
                                description: 'Standard item',
                                price: parseFloat(price),
                                stockQuantity: parseInt(stock, 10),
                                category
                              });
                              setName('');
                              setPrice('');
                              setStock('');
                              loadProducts();
                            } catch (err: any) {
                              alert(err.message);
                            }
                          };

                          const handleDelete = async (id: number) => {
                            if (!confirm('Are you sure you want to delete this product?')) return;
                            try {
                              await apiService.deleteProduct(id);
                              loadProducts();
                            } catch (err: any) {
                              alert(err.message);
                            }
                          };

                          return (
                            <div className="max-w-6xl mx-auto p-6">
                              <header className="mb-8 flex justify-between items-center border-b border-slate-700 pb-4">
                                <div>
                                  <h1 className="text-3xl font-bold text-sky-400">Application Dashboard</h1>
                                  <p className="text-slate-400">Autonomously generated full-stack architecture</p>
                                </div>
                                <span className="bg-emerald-500/20 text-emerald-400 px-3 py-1 rounded-full text-xs font-semibold">
                                  System Online
                                </span>
                              </header>

                              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                                {/* Creation Form */}
                                <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 h-fit">
                                  <h2 className="text-lg font-semibold mb-4 text-slate-200">Add New Product</h2>
                                  <form onSubmit={handleCreate} className="space-y-4">
                                    <div>
                                      <label className="block text-xs font-medium text-slate-400 mb-1">Product Name</label>
                                      <input
                                        type="text"
                                        required
                                        value={name}
                                        onChange={e => setName(e.target.value)}
                                        className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-sm text-slate-100 focus:outline-none focus:border-sky-500"
                                      />
                                    </div>
                                    <div>
                                      <label className="block text-xs font-medium text-slate-400 mb-1">Price ($)</label>
                                      <input
                                        type="number"
                                        step="0.01"
                                        required
                                        value={price}
                                        onChange={e => setPrice(e.target.value)}
                                        className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-sm text-slate-100 focus:outline-none focus:border-sky-500"
                                      />
                                    </div>
                                    <div>
                                      <label className="block text-xs font-medium text-slate-400 mb-1">Stock Units</label>
                                      <input
                                        type="number"
                                        required
                                        value={stock}
                                        onChange={e => setStock(e.target.value)}
                                        className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-sm text-slate-100 focus:outline-none focus:border-sky-500"
                                      />
                                    </div>
                                    <button
                                      type="submit"
                                      className="w-full bg-sky-600 hover:bg-sky-500 text-white font-medium py-2 rounded-lg transition-colors text-sm"
                                    >
                                      Save Product
                                    </button>
                                  </form>
                                </div>

                                {/* Product List */}
                                <div className="md:col-span-2 space-y-4">
                                  <h2 className="text-lg font-semibold text-slate-200">Inventory Items ({products.length})</h2>
                                  {loading && <p className="text-slate-400">Loading catalog...</p>}
                                  {error && <p className="text-rose-400">{error}</p>}
                                  
                                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                    {products.map(p => (
                                      <div key={p.id} className="bg-slate-800 p-4 rounded-xl border border-slate-700 flex flex-col justify-between">
                                        <div>
                                          <div className="flex justify-between items-start mb-2">
                                            <h3 className="font-semibold text-slate-100">{p.name}</h3>
                                            <span className="text-xs bg-slate-700 text-slate-300 px-2 py-0.5 rounded">
                                              {p.category}
                                            </span>
                                          </div>
                                          <p className="text-sm text-slate-400 mb-2">{p.description}</p>
                                          <p className="text-lg font-bold text-sky-400">${p.price?.toFixed(2)}</p>
                                          <p className="text-xs text-slate-500">Stock: {p.stockQuantity} available</p>
                                        </div>
                                        <div className="mt-4 pt-3 border-t border-slate-700 flex justify-end">
                                          <button
                                            onClick={() => p.id && handleDelete(p.id)}
                                            className="text-xs text-rose-400 hover:text-rose-300 font-medium"
                                          >
                                            Delete
                                          </button>
                                        </div>
                                      </div>
                                    ))}
                                    {products.length === 0 && !loading && (
                                      <div className="col-span-2 text-center p-8 bg-slate-800/50 rounded-xl border border-dashed border-slate-700 text-slate-400">
                                        No items in catalog yet. Use the form to add one.
                                      </div>
                                    )}
                                  </div>
                                </div>
                              </div>
                            </div>
                          );
                        }
                        export default App;
                        """)
                .build());

        // 5. main.tsx
        files.add(GeneratedSourceFile.builder()
                .filePath("frontend/src/main.tsx")
                .language("typescript")
                .description("React DOM entry point")
                .content("""
                        import React from 'react';
                        import ReactDOM from 'react-dom/client';
                        import App from './App';

                        ReactDOM.createRoot(document.getElementById('root')!).render(
                          <React.StrictMode>
                            <App />
                          </React.StrictMode>
                        );
                        """)
                .build());

        return files;
    }

    private GeneratedSourceFile regenerateSingleFrontendFile(AgentContext context) {
        String path = context.getTargetFilePath();
        String instructions = context.getCustomInstructions();
        log.info("Regenerating targeted frontend file: {} with instructions: {}", path, instructions);

        String prompt = "You are an expert React TypeScript developer. Regenerate or fix this UI file: " + path
                + "\nInstructions to resolve:\n" + (instructions != null ? instructions : "Enhance UI aesthetics, add error handling, and ensure type safety.");

        String code = aiClient.generateText(PromptTemplates.FRONTEND_DEVELOPER_SYSTEM, prompt);
        return GeneratedSourceFile.builder()
                .filePath(path)
                .language("typescript")
                .description("Regenerated React file: " + path)
                .content(code)
                .build();
    }
}
