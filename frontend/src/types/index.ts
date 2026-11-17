export interface User {
  id: number;
  name: string;
  email: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  name: string;
  email: string;
  role: string;
}

export interface Project {
  id: number;
  userId?: number;
  projectName: string;
  description?: string;
  requirements: string;
  status: 'CREATED' | 'ANALYZING' | 'PENDING_REQUIREMENTS_APPROVAL' | 'REQUIREMENTS_APPROVED' | 'ARCHITECTING' | 'PENDING_ARCHITECTURE_APPROVAL' | 'ARCHITECTURE_APPROVED' | 'GENERATING' | 'TESTING' | 'REVIEWING' | 'SELF_HEALING' | 'DOCUMENTING' | 'COMPLETED' | 'FAILED';
  backendFramework?: string;
  frontendFramework?: string;
  databaseType?: string;
  aiModel?: string;
  requirementsApproved?: boolean;
  architectureApproved?: boolean;
  currentAgent?: string;
  progressPercentage: number;
  retryCount: number;
  totalFiles?: number;
  totalTests?: number;
  passedTests?: number;
  failedTests?: number;
  codeReviewIssues?: number;
  createdAt: string;
  updatedAt?: string;
}

export interface AgentExecution {
  id?: number;
  projectId: number;
  agentId: number;
  agentName: string;
  agentType: string;
  iconName?: string;
  orderIndex?: number;
  executionStatus: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'WAITING_APPROVAL';
  progressPercentage: number;
  currentTask?: string;
  inputData?: string;
  outputData?: string;
  errorMessage?: string;
  durationMs?: number;
  startedAt?: string;
  completedAt?: string;
}

export interface GeneratedFile {
  id: number;
  projectId: number;
  filePath: string;
  fileContent: string;
  language: string;
  version: number;
  generatedByAgent?: string;
  isLatest: boolean;
  createdAt: string;
}

export interface TestResult {
  id: number;
  projectId: number;
  testName: string;
  testClass?: string;
  testStatus: 'PASSED' | 'FAILED' | 'ERROR' | 'SKIPPED';
  errorMessage?: string;
  stackTrace?: string;
  executionTime?: number;
  createdAt: string;
}

export interface CodeReview {
  id: number;
  projectId: number;
  filePath: string;
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';
  category?: 'SECURITY' | 'BUG' | 'PERFORMANCE' | 'STYLE' | 'ARCHITECTURE';
  issueDescription: string;
  suggestedFix?: string;
  lineNumber?: number;
  createdAt: string;
}

export interface ProjectDocument {
  id: number;
  projectId: number;
  documentType: 'README' | 'ARCHITECTURE' | 'INSTALLATION' | 'API_SPEC' | 'DEPLOYMENT';
  title?: string;
  documentContent: string;
  createdAt: string;
}

export interface DashboardStats {
  totalProjects: number;
  activeProjects: number;
  completedProjects: number;
  failedProjects: number;
  recentProjects: Project[];
  recentExecutions?: AgentExecution[];
}

export interface AiConfig {
  activeProvider: 'GEMINI' | 'OPENAI' | 'MOCK' | string;
  geminiApiKeyMasked?: string;
  openaiApiKeyMasked?: string;
  geminiApiKey?: string;
  openaiApiKey?: string;
  defaultModel: string;
  mockModeEnabled: boolean;
  statusMessage?: string;
}

export interface RequirementSpec {
  applicationName: string;
  summary: string;
  functionalRequirements: string[];
  nonFunctionalRequirements: string[];
  modules: Array<{
    name: string;
    description: string;
    keyFeatures: string[];
  }>;
  userStories: string[];
  dependencies: string[];
  assumptions: string[];
}

export interface SystemArchitectureSpec {
  architecturePattern: string;
  overview: string;
  techStack: Record<string, string>;
  databaseSchema: Array<{
    tableName: string;
    description: string;
    columns: Array<{
      name: string;
      type: string;
      primaryKey: boolean;
      nullable: boolean;
      description: string;
    }>;
    relationships: string[];
  }>;
  apiEndpoints: Array<{
    method: string;
    path: string;
    summary: string;
    requestBody?: string;
    responseBody?: string;
    authRequired?: string;
  }>;
  moduleDependencies: Array<{
    module: string;
    dependsOn: string[];
    communication: string;
  }>;
  folderStructure: string[];
  designPatterns: string[];
  securityConsiderations: string[];
}

export interface OrchestrationEvent {
  projectId: number;
  agentType?: string;
  agentName?: string;
  status?: string;
  progressPercentage?: number;
  currentTask?: string;
  message?: string;
  durationMs?: number;
  timestamp?: string;
}
