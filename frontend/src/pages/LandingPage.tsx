import React from 'react';
import {
  Bot,
  Sparkles,
  ArrowRight,
  Cpu,
  Server,
  Layout,
  ShieldAlert,
  CheckCircle2,
  BookOpen,
  Code,
  Layers,
  Database,
  Terminal,
  Activity
} from '../components/Icons';

interface LandingPageProps {
  onGetStarted: () => void;
  onExploreFeatures: () => void;
}

export const LandingPage: React.FC<LandingPageProps> = ({
  onGetStarted,
  onExploreFeatures
}) => {
  const agents = [
    { name: 'Requirement Analyst', role: 'Spec Deconstruction', icon: Bot, color: 'from-sky-500 to-blue-600' },
    { name: 'System Architect', role: 'Relational & API Design', icon: Cpu, color: 'from-indigo-500 to-purple-600' },
    { name: 'Backend Developer', role: 'Spring Boot 3 Synthesis', icon: Server, color: 'from-emerald-500 to-teal-600' },
    { name: 'Frontend Developer', role: 'React & Tailwind UI', icon: Layout, color: 'from-cyan-500 to-sky-600' },
    { name: 'Testing Agent', role: 'Sandboxed Verification', icon: ShieldAlert, color: 'from-amber-500 to-orange-600' },
    { name: 'Code Reviewer', role: 'OWASP & Static Analysis', icon: CheckCircle2, color: 'from-rose-500 to-pink-600' },
    { name: 'Documentation Agent', role: 'Manuals & OpenAPI Spec', icon: BookOpen, color: 'from-violet-500 to-purple-600' },
  ];

  const features = [
    {
      title: 'Autonomous Multi-Agent Collaboration',
      desc: 'Seven specialized agents collaborate in an orchestrated DAG pipeline with strict typed contracts and memory persistence.',
      icon: Layers
    },
    {
      title: 'Self-Healing Code Generation',
      desc: 'Compiler errors and test failures are fed back into developer agents with automated targeted repairs up to configurable retry limits.',
      icon: Sparkles
    },
    {
      title: 'Isolated Execution Sandbox',
      desc: 'Generated code runs only within isolated process watchdog or Docker containers with strict CPU, memory, and network limits.',
      icon: Terminal
    },
    {
      title: 'Human-In-The-Loop Approvals',
      desc: 'Inspect and approve requirements and system architecture before code generation begins, maintaining complete architectural governance.',
      icon: CheckCircle2
    },
    {
      title: 'Real-Time Telemetry & Monitoring',
      desc: 'Stream live agent thoughts, subtasks, durations, and state transitions via native Server-Sent Events (SSE).',
      icon: Activity
    },
    {
      title: 'One-Click Full Stack Export',
      desc: 'Download compilable Spring Boot and React projects as complete ZIP archives ready to deploy with Docker Compose.',
      icon: Code
    }
  ];

  const technologies = [
    { name: 'Spring Boot 3.3', category: 'Backend Engine' },
    { name: 'Java 21 / 25', category: 'Language Runtime' },
    { name: 'React 19 & TypeScript', category: 'Frontend Interface' },
    { name: 'Tailwind CSS', category: 'UI Architecture' },
    { name: 'PostgreSQL & pgvector', category: 'Persistence Layer' },
    { name: 'Gemini 1.5 & GPT-4o', category: 'AI Intelligence' },
    { name: 'Docker Isolation', category: 'Sandbox Security' },
    { name: 'JUnit 5 & Mockito', category: 'Automated QA' },
  ];

  return (
    <div className="min-h-screen bg-navy-950 text-slate-100 flex flex-col justify-between">
      {/* Hero Section */}
      <section className="relative overflow-hidden pt-20 pb-28 px-6">
        {/* Glow ambient background */}
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[400px] bg-gradient-to-tr from-sky-500/15 via-indigo-600/15 to-purple-600/15 blur-[120px] pointer-events-none rounded-full" />

        <div className="max-w-5xl mx-auto text-center relative z-10 space-y-8">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-sky-500/10 border border-sky-500/20 text-sky-300 text-xs font-semibold">
            <Sparkles className="w-3.5 h-3.5 text-sky-400" />
            <span>Next-Generation Autonomous Multi-Agent Platform</span>
          </div>

          <h1 className="text-4xl sm:text-6xl lg:text-7xl font-extrabold tracking-tight text-white leading-tight">
            Your AI Engineering Team. <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-sky-400 via-indigo-300 to-purple-400">
              One Unified Platform.
            </span>
          </h1>

          <p className="text-base sm:text-lg text-slate-400 max-w-3xl mx-auto leading-relaxed">
            Enter your natural language software requirements and let a coordinated squad of seven autonomous AI agents plan, architect, write Spring Boot and React code, execute sandboxed tests, and self-heal defects.
          </p>

          <div className="flex flex-wrap items-center justify-center gap-4 pt-4">
            <button
              onClick={onGetStarted}
              className="flex items-center gap-2 px-6 py-3.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white font-semibold text-sm shadow-xl shadow-sky-500/25 hover:shadow-sky-500/40 hover:-translate-y-0.5 transition-all"
            >
              <span>Get Started Now</span>
              <ArrowRight className="w-4 h-4" />
            </button>
            <button
              onClick={onExploreFeatures}
              className="px-6 py-3.5 rounded-xl bg-slate-900/80 hover:bg-slate-800 text-slate-300 hover:text-white font-semibold text-sm border border-slate-700/80 hover:border-slate-600 transition-all"
            >
              Explore Capabilities
            </button>
          </div>
        </div>

        {/* Multi-Agent Visual Workflow Pipeline */}
        <div className="max-w-6xl mx-auto mt-20 p-6 rounded-2xl glass-panel border border-slate-800 relative shadow-2xl">
          <div className="text-center mb-6">
            <span className="text-[11px] font-bold uppercase tracking-wider text-sky-400 bg-sky-500/10 px-3 py-1 rounded-full border border-sky-500/20">
              Autonomous Agent Orchestration Pipeline
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-3">
            {agents.map((ag, idx) => {
              const Icon = ag.icon;
              return (
                <div
                  key={ag.name}
                  className="bg-navy-900/80 p-4 rounded-xl border border-slate-800 flex flex-col items-center text-center relative group hover:border-sky-500/40 transition-all"
                >
                  <div className="text-[10px] font-mono text-slate-500 absolute top-2 right-2.5">
                    0{idx + 1}
                  </div>
                  <div className={`w-10 h-10 rounded-xl bg-gradient-to-tr ${ag.color} flex items-center justify-center text-white mb-3 shadow-md shadow-sky-500/10`}>
                    <Icon className="w-5 h-5" />
                  </div>
                  <h4 className="text-xs font-semibold text-slate-200 mb-1">{ag.name}</h4>
                  <p className="text-[10px] text-slate-400">{ag.role}</p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="py-20 px-6 border-t border-slate-800/80 bg-navy-900/30">
        <div className="max-w-6xl mx-auto space-y-12">
          <div className="text-center space-y-3">
            <h2 className="text-3xl font-bold text-white tracking-tight">Engineered for Real Software Production</h2>
            <p className="text-sm text-slate-400 max-w-2xl mx-auto">
              AgentForge goes beyond simple code snippet generation to deliver fully architected, compilable, and tested applications.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {features.map((feat) => {
              const Icon = feat.icon;
              return (
                <div
                  key={feat.title}
                  className="p-6 rounded-2xl glass-panel glass-panel-hover transition-all space-y-3"
                >
                  <div className="w-9 h-9 rounded-xl bg-sky-500/10 text-sky-400 border border-sky-500/20 flex items-center justify-center">
                    <Icon className="w-5 h-5" />
                  </div>
                  <h3 className="text-base font-semibold text-slate-100">{feat.title}</h3>
                  <p className="text-xs text-slate-400 leading-relaxed">{feat.desc}</p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Technology Stack Grid */}
      <section className="py-16 px-6 border-t border-slate-800/80 bg-navy-950">
        <div className="max-w-5xl mx-auto space-y-8 text-center">
          <div>
            <h3 className="text-xl font-bold text-white mb-2">Powered by Production-Proven Technologies</h3>
            <p className="text-xs text-slate-400">Strictly adhering to clean architecture and modern developer standards.</p>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {technologies.map((t) => (
              <div key={t.name} className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 text-left">
                <div className="text-xs font-semibold text-slate-200">{t.name}</div>
                <div className="text-[10px] text-sky-400/90">{t.category}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 py-8 px-6 text-center text-xs text-slate-500">
        <div className="max-w-5xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Bot className="w-4 h-4 text-sky-400" />
            <span className="font-semibold text-slate-300">AgentForge</span>
            <span>— Multi-Agent AI Software Development Platform</span>
          </div>
          <div>Production AI Architecture for Software Engineering Placement & Portfolio Demonstrations</div>
        </div>
      </footer>
    </div>
  );
};
