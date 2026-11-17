import {
  AuthResponse,
  Project,
  AgentExecution,
  GeneratedFile,
  TestResult,
  CodeReview,
  ProjectDocument,
  DashboardStats,
  AiConfig,
  OrchestrationEvent
} from '../types';

const API_BASE = '/api';

function getHeaders(customHeaders: HeadersInit = {}): HeadersInit {
  const token = localStorage.getItem('agentforge_token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...customHeaders
  };
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let msg = `HTTP error ${res.status}`;
    try {
      const errJson = await res.json();
      msg = errJson.message || errJson.error || msg;
    } catch {
      // fallback to statusText
      msg = res.statusText || msg;
    }
    throw new Error(msg);
  }
  if (res.status === 204) {
    return {} as T;
  }
  return res.json();
}

export const api = {
  // Auth
  async register(name: string, email: string, password: string): Promise<AuthResponse> {
    const res = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, password })
    });
    const data = await handleResponse<AuthResponse>(res);
    localStorage.setItem('agentforge_token', data.token);
    localStorage.setItem('agentforge_user', JSON.stringify(data));
    return data;
  },

  async login(email: string, password: string): Promise<AuthResponse> {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    const data = await handleResponse<AuthResponse>(res);
    localStorage.setItem('agentforge_token', data.token);
    localStorage.setItem('agentforge_user', JSON.stringify(data));
    return data;
  },

  logout(): void {
    localStorage.removeItem('agentforge_token');
    localStorage.removeItem('agentforge_user');
  },

  getStoredUser(): AuthResponse | null {
    const str = localStorage.getItem('agentforge_user');
    if (!str) return null;
    try {
      return JSON.parse(str);
    } catch {
      return null;
    }
  },

  // Projects
  async getDashboardStats(): Promise<DashboardStats> {
    const res = await fetch(`${API_BASE}/projects/dashboard/stats`, { headers: getHeaders() });
    return handleResponse<DashboardStats>(res);
  },

  async getProjects(): Promise<Project[]> {
    const res = await fetch(`${API_BASE}/projects`, { headers: getHeaders() });
    return handleResponse<Project[]>(res);
  },

  async getProject(id: number): Promise<Project> {
    const res = await fetch(`${API_BASE}/projects/${id}`, { headers: getHeaders() });
    return handleResponse<Project>(res);
  },

  async createProject(payload: {
    projectName: string;
    description?: string;
    requirements: string;
    backendFramework?: string;
    frontendFramework?: string;
    databaseType?: string;
    aiModel?: string;
  }): Promise<Project> {
    const res = await fetch(`${API_BASE}/projects`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(payload)
    });
    return handleResponse<Project>(res);
  },

  async deleteProject(id: number): Promise<void> {
    const res = await fetch(`${API_BASE}/projects/${id}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    return handleResponse<void>(res);
  },

  // Executions
  async executeProject(id: number): Promise<Project> {
    const res = await fetch(`${API_BASE}/projects/${id}/execute`, {
      method: 'POST',
      headers: getHeaders()
    });
    return handleResponse<Project>(res);
  },

  async getProjectAgents(id: number): Promise<AgentExecution[]> {
    const res = await fetch(`${API_BASE}/projects/${id}/agents`, { headers: getHeaders() });
    return handleResponse<AgentExecution[]>(res);
  },

  // Approvals
  async approveRequirements(id: number, approved: boolean = true, feedback?: string): Promise<Project> {
    const res = await fetch(`${API_BASE}/projects/${id}/approve-requirements`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ approved, feedback })
    });
    return handleResponse<Project>(res);
  },

  async approveArchitecture(id: number, approved: boolean = true, feedback?: string): Promise<Project> {
    const res = await fetch(`${API_BASE}/projects/${id}/approve-architecture`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ approved, feedback })
    });
    return handleResponse<Project>(res);
  },

  // Code files
  async getProjectFiles(id: number): Promise<GeneratedFile[]> {
    const res = await fetch(`${API_BASE}/projects/${id}/files`, { headers: getHeaders() });
    return handleResponse<GeneratedFile[]>(res);
  },

  async getFile(projectId: number, fileId: number): Promise<GeneratedFile> {
    const res = await fetch(`${API_BASE}/projects/${projectId}/files/${fileId}`, { headers: getHeaders() });
    return handleResponse<GeneratedFile>(res);
  },

  async regenerateFile(projectId: number, fileId: number, customInstructions?: string): Promise<GeneratedFile> {
    const res = await fetch(`${API_BASE}/projects/${projectId}/files/${fileId}/regenerate`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ customInstructions })
    });
    return handleResponse<GeneratedFile>(res);
  },

  async getFileHistory(projectId: number, path: string): Promise<GeneratedFile[]> {
    const res = await fetch(`${API_BASE}/projects/${projectId}/files/history?path=${encodeURIComponent(path)}`, {
      headers: getHeaders()
    });
    return handleResponse<GeneratedFile[]>(res);
  },

  async restoreFileVersion(projectId: number, fileId: number): Promise<GeneratedFile> {
    const res = await fetch(`${API_BASE}/projects/${projectId}/files/${fileId}/restore`, {
      method: 'POST',
      headers: getHeaders()
    });
    return handleResponse<GeneratedFile>(res);
  },

  // Testing
  async getProjectTests(id: number): Promise<TestResult[]> {
    const res = await fetch(`${API_BASE}/projects/${id}/tests`, { headers: getHeaders() });
    return handleResponse<TestResult[]>(res);
  },

  async runProjectTests(id: number): Promise<TestResult[]> {
    const res = await fetch(`${API_BASE}/projects/${id}/tests/run`, {
      method: 'POST',
      headers: getHeaders()
    });
    return handleResponse<TestResult[]>(res);
  },

  // Code Review
  async getProjectReviews(id: number): Promise<CodeReview[]> {
    const res = await fetch(`${API_BASE}/projects/${id}/review`, { headers: getHeaders() });
    return handleResponse<CodeReview[]>(res);
  },

  async triggerCodeReview(id: number): Promise<CodeReview[]> {
    const res = await fetch(`${API_BASE}/projects/${id}/review`, {
      method: 'POST',
      headers: getHeaders()
    });
    return handleResponse<CodeReview[]>(res);
  },

  // Documents & Download
  getDownloadUrl(projectId: number): string {
    return `${API_BASE}/projects/${projectId}/download`;
  },

  async getProjectDocuments(id: number): Promise<ProjectDocument[]> {
    const res = await fetch(`${API_BASE}/projects/${id}/documents`, { headers: getHeaders() });
    return handleResponse<ProjectDocument[]>(res);
  },

  // AI Configuration
  async getAiConfig(): Promise<AiConfig> {
    const res = await fetch(`${API_BASE}/ai-config`, { headers: getHeaders() });
    return handleResponse<AiConfig>(res);
  },

  async updateAiConfig(config: Partial<AiConfig>): Promise<AiConfig> {
    const res = await fetch(`${API_BASE}/ai-config`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(config)
    });
    return handleResponse<AiConfig>(res);
  },

  // SSE Real-Time Stream Subscription
  subscribeToSse(
    projectId: number,
    onEvent: (event: OrchestrationEvent) => void,
    onError?: (err: any) => void
  ): () => void {
    const eventSource = new EventSource(`${API_BASE}/sse/projects/${projectId}`);

    eventSource.addEventListener('AGENT_STATUS', (e: MessageEvent) => {
      try {
        const parsed = JSON.parse(e.data);
        onEvent(parsed);
      } catch (err) {
        console.error('SSE JSON parse error', err);
      }
    });

    eventSource.addEventListener('WORKFLOW_STATUS', (e: MessageEvent) => {
      try {
        const parsed = JSON.parse(e.data);
        onEvent(parsed);
      } catch (err) {
        console.error('SSE JSON parse error', err);
      }
    });

    eventSource.onerror = (err) => {
      if (onError) onError(err);
    };

    return () => {
      eventSource.close();
    };
  }
};
