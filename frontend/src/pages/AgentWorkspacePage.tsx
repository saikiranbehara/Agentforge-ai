import React, { useState, useEffect } from 'react';
import { Project, AgentExecution } from '../types';
import { api } from '../services/api';
import {
  Bot,
  Play,
  Download,
  RefreshCw,
  Clock,
  CheckCircle2,
  AlertTriangle,
  FileSearch,
  Cpu,
  Server,
  Layout,
  ShieldAlert,
  BookOpen,
  Terminal,
  Eye,
  Check,
  ChevronDown,
  ChevronRight
} from '../components/Icons';

interface AgentWorkspacePageProps {
  project: Project;
  onProjectUpdated: (p: Project) => void;
  onNavigateToTab: (tab: any) => void;
}

export const AgentWorkspacePage: React.FC<AgentWorkspacePageProps> = ({
  project,
  onProjectUpdated,
  onNavigateToTab
}) => {
  const [agents, setAgents] = useState<AgentExecution[]>([]);
  const [loading, setLoading] = useState(false);
  const [executing, setExecuting] = useState(false);
  const [expandedAgentId, setExpandedAgentId] = useState<number | string | null>(null);

  // Map icon strings to icon components
  const iconMap: Record<string, React.FC<{ className?: string }>> = {
    FileSearch: FileSearch,
    Cpu: Cpu,
    Server: Server,
    Layout: Layout,
    ShieldAlert: ShieldAlert,
    CheckCircle2: CheckCircle2,
    BookOpen: BookOpen,
  };

  useEffect(() => {
    loadAgentExecutions();
  }, [project.id]);

  // Subscribe to real-time SSE stream for this project
  useEffect(() => {
    const unsubscribe = api.subscribeToSse(project.id, (event) => {
      // Refresh executions whenever an event is received
      loadAgentExecutions();
      // Update overall project progress if changed
      if (event.progressPercentage !== undefined) {
        onProjectUpdated({
          ...project,
          progressPercentage: event.progressPercentage,
          currentAgent: event.agentType || project.currentAgent,
          status: (event.status as any) || project.status
        });
      }
    });

    return () => {
      unsubscribe();
    };
  }, [project.id]);

  const loadAgentExecutions = async () => {
    try {
      setLoading(true);
      const data = await api.getProjectAgents(project.id);
      setAgents(data);
    } catch (err: any) {
      console.error('Failed to load agents', err);
    } finally {
      setLoading(false);
    }
  };

  const handleStartWorkflow = async () => {
    try {
      setExecuting(true);
      const updated = await api.executeProject(project.id);
      onProjectUpdated(updated);
      loadAgentExecutions();
    } catch (err: any) {
      alert('Execution failed: ' + err.message);
    } finally {
      setExecuting(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return (
          <span className="flex items-center gap-1 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-2 py-0.5 rounded-full text-[10px] font-semibold">
            <Check className="w-3 h-3" />
            <span>Completed</span>
          </span>
        );
      case 'RUNNING':
        return (
          <span className="flex items-center gap-1 bg-sky-500/15 text-sky-400 border border-sky-500/30 px-2 py-0.5 rounded-full text-[10px] font-semibold animate-pulse">
            <RefreshCw className="w-3 h-3 animate-spin" />
            <span>Running</span>
          </span>
        );
      case 'WAITING_APPROVAL':
        return (
          <span className="flex items-center gap-1 bg-amber-500/15 text-amber-400 border border-amber-500/30 px-2 py-0.5 rounded-full text-[10px] font-semibold">
            <AlertTriangle className="w-3 h-3" />
            <span>Waiting Approval</span>
          </span>
        );
      case 'FAILED':
        return (
          <span className="flex items-center gap-1 bg-rose-500/10 text-rose-400 border border-rose-500/20 px-2 py-0.5 rounded-full text-[10px] font-semibold">
            <span>Failed</span>
          </span>
        );
      default:
        return (
          <span className="bg-slate-800 text-slate-500 px-2 py-0.5 rounded-full text-[10px] font-medium">
            Pending
          </span>
        );
    }
  };

  return (
    <div className="p-8 space-y-8 max-w-7xl mx-auto">
      {/* Header & Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-6 rounded-2xl glass-panel border border-slate-800">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-xl font-bold text-white tracking-tight">{project.projectName}</h1>
            <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-400 border border-slate-700">
              {project.status}
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-1 max-w-2xl line-clamp-1">{project.requirements}</p>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-3 self-start md:self-auto">
          <button
            onClick={loadAgentExecutions}
            disabled={loading}
            className="p-2.5 rounded-xl bg-slate-800/80 hover:bg-slate-800 text-slate-300 hover:text-white border border-slate-700 transition-colors"
            title="Refresh state"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          {project.status === 'COMPLETED' ? (
            <a
              href={api.getDownloadUrl(project.id)}
              download
              className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-lg shadow-emerald-500/20 transition-all"
            >
              <Download className="w-4 h-4" />
              <span>Download Project ZIP</span>
            </a>
          ) : (
            <button
              onClick={handleStartWorkflow}
              disabled={executing || project.status === 'COMPLETED'}
              className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white text-xs font-semibold shadow-lg shadow-sky-500/20 transition-all disabled:opacity-50"
            >
              <Play className="w-4 h-4" />
              <span>{executing ? 'Orchestrating...' : 'Run Autonomous Agents'}</span>
            </button>
          )}
        </div>
      </div>

      {/* 7 Specialized Agent Cards */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-semibold text-slate-300">
            <Bot className="w-4 h-4 text-sky-400" />
            <span>Autonomous Multi-Agent Roster (7 Specialized Agents)</span>
          </div>
          <span className="text-[11px] text-slate-500">Live Telemetry & Output Inspection</span>
        </div>

        <div className="grid grid-cols-1 gap-4">
          {agents.map((ag) => {
            const Icon = (ag.iconName && iconMap[ag.iconName]) || Bot;
            const isExpanded = expandedAgentId === ag.agentId;
            const isRunning = ag.executionStatus === 'RUNNING';

            return (
              <div
                key={ag.agentId}
                className={`p-5 rounded-2xl glass-panel border transition-all ${
                  isRunning
                    ? 'border-sky-500/50 shadow-lg shadow-sky-500/5 ring-1 ring-sky-500/20'
                    : 'border-slate-800/90 hover:border-slate-700/80'
                }`}
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  {/* Agent Identity & Task */}
                  <div className="flex items-center gap-3.5">
                    <div className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 ${
                      ag.executionStatus === 'COMPLETED'
                        ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                        : isRunning
                        ? 'bg-sky-500/20 text-sky-400 border border-sky-500/40 animate-pulse'
                        : 'bg-slate-800 text-slate-400 border border-slate-700/80'
                    }`}>
                      <Icon className="w-5 h-5" />
                    </div>

                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="text-sm font-semibold text-white">{ag.agentName}</h3>
                        <span className="text-[10px] text-slate-500 font-mono">#{ag.orderIndex}</span>
                      </div>
                      <p className="text-xs text-slate-400 mt-0.5">
                        {ag.currentTask || 'Awaiting prior pipeline execution'}
                      </p>
                    </div>
                  </div>

                  {/* Right Metrics & Status */}
                  <div className="flex items-center gap-4 self-end sm:self-auto">
                    {ag.durationMs !== undefined && ag.durationMs !== null && (
                      <div className="flex items-center gap-1 text-[11px] text-slate-400">
                        <Clock className="w-3.5 h-3.5 text-slate-500" />
                        <span>{(ag.durationMs / 1000).toFixed(1)}s</span>
                      </div>
                    )}

                    {getStatusBadge(ag.executionStatus)}

                    {/* Expand Output Toggle */}
                    <button
                      onClick={() => setExpandedAgentId(isExpanded ? null : ag.agentId)}
                      className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                      title={isExpanded ? 'Collapse output' : 'Inspect agent output'}
                    >
                      {isExpanded ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                {/* Progress Bar */}
                <div className="mt-4 pt-3 border-t border-slate-800/80">
                  <div className="flex justify-between items-center text-[10px] text-slate-500 mb-1">
                    <span>Task Completion</span>
                    <span>{ag.progressPercentage}%</span>
                  </div>
                  <div className="h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${
                        ag.executionStatus === 'COMPLETED'
                          ? 'bg-emerald-500'
                          : isRunning
                          ? 'bg-sky-500'
                          : 'bg-slate-700'
                      }`}
                      style={{ width: `${ag.progressPercentage}%` }}
                    />
                  </div>
                </div>

                {/* Expandable Output Terminal Preview */}
                {isExpanded && (
                  <div className="mt-4 p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2 animate-fadeIn">
                    <div className="flex items-center justify-between text-[11px] text-slate-400 border-b border-slate-800 pb-2">
                      <div className="flex items-center gap-1.5 font-mono">
                        <Terminal className="w-3.5 h-3.5 text-sky-400" />
                        <span>{ag.agentName} JSON Contract Output</span>
                      </div>
                      <span className="text-[10px] text-slate-500">Structured Payload</span>
                    </div>
                    <pre className="text-[11px] font-mono text-slate-300 overflow-x-auto p-2 bg-black/40 rounded-lg max-h-64 leading-relaxed whitespace-pre-wrap">
                      {ag.outputData || ag.errorMessage || 'No execution output produced yet.'}
                    </pre>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
