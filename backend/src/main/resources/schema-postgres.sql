-- AgentForge PostgreSQL Database Schema with pgvector
-- Enable pgvector extension if available
CREATE EXTENSION IF NOT EXISTS vector;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'ROLE_USER',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. Projects Table
CREATE TABLE IF NOT EXISTS projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    project_name VARCHAR(255) NOT NULL,
    description TEXT,
    requirements TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'CREATED' NOT NULL,
    backend_framework VARCHAR(100) DEFAULT 'Spring Boot 3.x',
    frontend_framework VARCHAR(100) DEFAULT 'React + TypeScript',
    database_type VARCHAR(100) DEFAULT 'PostgreSQL',
    ai_model VARCHAR(100) DEFAULT 'Gemini 1.5 Pro',
    requirements_approved BOOLEAN DEFAULT FALSE,
    architecture_approved BOOLEAN DEFAULT FALSE,
    current_agent VARCHAR(100),
    progress_percentage INTEGER DEFAULT 0,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_projects_user_id ON projects(user_id);
CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);

-- 3. Agents Table
CREATE TABLE IF NOT EXISTS agents (
    id BIGSERIAL PRIMARY KEY,
    agent_name VARCHAR(255) NOT NULL UNIQUE,
    agent_type VARCHAR(100) NOT NULL UNIQUE,
    icon_name VARCHAR(100),
    order_index INTEGER,
    description TEXT
);

-- 4. Agent Executions Table
CREATE TABLE IF NOT EXISTS agent_executions (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    agent_id BIGINT NOT NULL REFERENCES agents(id),
    execution_status VARCHAR(50) NOT NULL,
    progress_percentage INTEGER DEFAULT 0,
    current_task VARCHAR(255),
    input_data TEXT,
    output_data TEXT,
    error_message TEXT,
    duration_ms BIGINT,
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_agent_executions_project_id ON agent_executions(project_id);

-- 5. Generated Files Table
CREATE TABLE IF NOT EXISTS generated_files (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    file_content TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    version INTEGER DEFAULT 1 NOT NULL,
    generated_by_agent VARCHAR(100),
    is_latest BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_generated_files_proj_path ON generated_files(project_id, file_path);
CREATE INDEX IF NOT EXISTS idx_generated_files_latest ON generated_files(project_id, is_latest);

-- 6. Test Results Table
CREATE TABLE IF NOT EXISTS test_results (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    test_name VARCHAR(255) NOT NULL,
    test_class VARCHAR(255),
    test_status VARCHAR(50) NOT NULL,
    error_message TEXT,
    stack_trace TEXT,
    execution_time BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_test_results_project_id ON test_results(project_id);

-- 7. Code Reviews Table
CREATE TABLE IF NOT EXISTS code_reviews (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    issue_description TEXT NOT NULL,
    suggested_fix TEXT,
    line_number INTEGER,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_code_reviews_project_id ON code_reviews(project_id);

-- 8. Project Documents Table
CREATE TABLE IF NOT EXISTS project_documents (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    document_type VARCHAR(100) NOT NULL,
    title VARCHAR(255),
    document_content TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_project_documents_proj_type ON project_documents(project_id, document_type);
