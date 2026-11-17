import React, { useState } from 'react';
import { Project } from '../types';
import { api } from '../services/api';
import { Sparkles, ArrowRight, Layers, Database, Code, Cpu } from '../components/Icons';

interface CreateProjectPageProps {
  onProjectCreated: (p: Project) => void;
  onCancel: () => void;
}

export const CreateProjectPage: React.FC<CreateProjectPageProps> = ({
  onProjectCreated,
  onCancel
}) => {
  const [projectName, setProjectName] = useState('');
  const [description, setDescription] = useState('');
  const [requirements, setRequirements] = useState('');
  const [backendFramework, setBackendFramework] = useState('Spring Boot 3.x');
  const [frontendFramework, setFrontendFramework] = useState('React + TypeScript');
  const [databaseType, setDatabaseType] = useState('PostgreSQL');
  const [aiModel, setAiModel] = useState('Gemini 1.5 Pro');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const sampleTemplates = [
    {
      title: 'E-Commerce Platform',
      desc: 'User auth, product catalog, cart, orders, and admin dashboard',
      req: 'Build an online shopping application with user registration, login, product management, shopping cart, order management, and admin dashboard.'
    },
    {
      title: 'Task & Workflow Manager',
      desc: 'Kanban boards, team assignments, priorities, and audit logging',
      req: 'Build a project management platform with workspace creation, task board with status lanes (TODO, IN_PROGRESS, DONE), user assignment, comments, and activity audit trails.'
    },
    {
      title: 'Healthcare Patient Portal',
      desc: 'Appointments, medical history, doctor schedules, and prescriptions',
      req: 'Build a healthcare management system with doctor profiles, patient appointments, prescription records, and secure medical history tracking.'
    }
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectName.trim() || !requirements.trim()) {
      setError('Please provide both a project name and software requirements.');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const created = await api.createProject({
        projectName,
        description,
        requirements,
        backendFramework,
        frontendFramework,
        databaseType,
        aiModel
      });
      onProjectCreated(created);
    } catch (err: any) {
      setError(err.message || 'Failed to create project');
    } finally {
      setLoading(false);
    }
  };

  const applyTemplate = (template: typeof sampleTemplates[0]) => {
    setProjectName(template.title);
    setDescription(template.desc);
    setRequirements(template.req);
  };

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white tracking-tight">Create Software Project</h1>
        <p className="text-xs text-slate-400">Describe your software in natural language for the multi-agent AI engineering team</p>
      </div>

      {/* Quick Templates Banner */}
      <div className="p-5 rounded-2xl glass-panel border border-slate-800 space-y-3">
        <div className="flex items-center gap-2 text-xs font-semibold text-sky-400">
          <Sparkles className="w-4 h-4" />
          <span>Quick Requirement Templates</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {sampleTemplates.map((t) => (
            <button
              key={t.title}
              type="button"
              onClick={() => applyTemplate(t)}
              className="p-3 text-left rounded-xl bg-slate-900/80 hover:bg-slate-800 border border-slate-800 hover:border-sky-500/40 transition-all group"
            >
              <div className="text-xs font-semibold text-slate-200 group-hover:text-sky-300">{t.title}</div>
              <div className="text-[11px] text-slate-500 line-clamp-2 mt-0.5">{t.desc}</div>
            </button>
          ))}
        </div>
      </div>

      {/* Main Creation Form */}
      <form onSubmit={handleSubmit} className="p-6 rounded-2xl glass-panel border border-slate-800 space-y-6">
        {error && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs">
            {error}
          </div>
        )}

        {/* Project Name & Description */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">Project Name *</label>
            <input
              type="text"
              required
              placeholder="e.g. ShopSphere Cloud Hub"
              value={projectName}
              onChange={(e) => setProjectName(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700/80 rounded-xl p-2.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-sky-500 transition-colors"
            />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">Short Summary / Description</label>
            <input
              type="text"
              placeholder="e.g. Scalable commerce system with inventory tracking"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700/80 rounded-xl p-2.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-sky-500 transition-colors"
            />
          </div>
        </div>

        {/* Natural Language Requirements Textarea */}
        <div>
          <label className="block text-xs font-semibold text-slate-300 mb-1.5">
            Software Requirements (Natural Language) *
          </label>
          <textarea
            required
            rows={5}
            placeholder="Describe the application features, user roles, business entities, workflows, and integrations in plain English..."
            value={requirements}
            onChange={(e) => setRequirements(e.target.value)}
            className="w-full bg-slate-900 border border-slate-700/80 rounded-xl p-3 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-sky-500 transition-colors leading-relaxed font-mono"
          />
          <span className="text-[11px] text-slate-500 mt-1 block">
            The Requirement Analyst Agent will extract functional specs, modules, and user stories from this prompt.
          </span>
        </div>

        {/* Technology Preference Selectors */}
        <div className="pt-2 border-t border-slate-800/80">
          <div className="text-xs font-semibold text-slate-300 mb-3 flex items-center gap-1.5">
            <Layers className="w-3.5 h-3.5 text-sky-400" />
            <span>Architecture & Technology Stack Preferences</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-[11px] text-slate-400 mb-1">Backend Framework</label>
              <select
                value={backendFramework}
                onChange={(e) => setBackendFramework(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700/80 rounded-xl p-2 text-xs text-slate-200 focus:outline-none focus:border-sky-500"
              >
                <option value="Spring Boot 3.x">Spring Boot 3.3.x (Java 21)</option>
                <option value="Spring Boot 3.x Microservice">Spring Boot Modular Hexagonal</option>
              </select>
            </div>

            <div>
              <label className="block text-[11px] text-slate-400 mb-1">Frontend Framework</label>
              <select
                value={frontendFramework}
                onChange={(e) => setFrontendFramework(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700/80 rounded-xl p-2 text-xs text-slate-200 focus:outline-none focus:border-sky-500"
              >
                <option value="React + TypeScript">React 19 + TypeScript + Vite</option>
                <option value="React + Tailwind">React + Tailwind SPA</option>
              </select>
            </div>

            <div>
              <label className="block text-[11px] text-slate-400 mb-1">Database Engine</label>
              <select
                value={databaseType}
                onChange={(e) => setDatabaseType(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700/80 rounded-xl p-2 text-xs text-slate-200 focus:outline-none focus:border-sky-500"
              >
                <option value="PostgreSQL">PostgreSQL (with pgvector)</option>
                <option value="PostgreSQL + H2">PostgreSQL + H2 Dev Fallback</option>
              </select>
            </div>

            <div>
              <label className="block text-[11px] text-slate-400 mb-1">AI Reasoning Engine</label>
              <select
                value={aiModel}
                onChange={(e) => setAiModel(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700/80 rounded-xl p-2 text-xs text-slate-200 focus:outline-none focus:border-sky-500"
              >
                <option value="Gemini 1.5 Pro">Gemini 1.5 Pro (DeepMind)</option>
                <option value="Gemini 1.5 Flash">Gemini 1.5 Flash (Fast)</option>
                <option value="GPT-4o">OpenAI GPT-4o</option>
              </select>
            </div>
          </div>
        </div>

        {/* Buttons */}
        <div className="pt-4 flex items-center justify-end gap-3 border-t border-slate-800">
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2.5 text-xs font-medium text-slate-400 hover:text-white rounded-xl hover:bg-slate-800 transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading}
            className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white font-semibold text-xs shadow-lg shadow-sky-500/20 transition-all disabled:opacity-50"
          >
            <span>{loading ? 'Initializing Project...' : 'Initialize & Open Workspace'}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </form>
    </div>
  );
};
