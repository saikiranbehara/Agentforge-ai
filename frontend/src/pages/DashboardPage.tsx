import React from 'react';
import { DashboardStats, Project, AgentExecution } from '../types';
import {
  Activity,
  Layers,
  CheckCircle2,
  AlertTriangle,
  Play,
  ArrowRight,
  Clock,
  Code,
  Download,
  Bot
} from '../components/Icons';

interface DashboardPageProps {
  stats: DashboardStats | null;
  projects: Project[];
  onSelectProject: (p: Project) => void;
  onNewProject: () => void;
  onExecuteProject: (projectId: number) => void;
  onDownloadProject: (projectId: number) => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({
  stats,
  projects,
  onSelectProject,
  onNewProject,
  onExecuteProject,
  onDownloadProject
}) => {
  const statCards = [
    { label: 'Total Projects', value: stats?.totalProjects || projects.length, icon: Layers, color: 'text-sky-400', bg: 'bg-sky-500/10' },
    { label: 'Active Pipeline', value: stats?.activeProjects || 0, icon: Activity, color: 'text-amber-400', bg: 'bg-amber-500/10' },
    { label: 'Completed Systems', value: stats?.completedProjects || 0, icon: CheckCircle2, color: 'text-emerald-400', bg: 'bg-emerald-500/10' },
    { label: 'Failed Executions', value: stats?.failedProjects || 0, icon: AlertTriangle, color: 'text-rose-400', bg: 'bg-rose-500/10' },
  ];

  return (
    <div className="p-8 space-y-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Engineering Dashboard</h1>
          <p className="text-xs text-slate-400">Overview of multi-agent software lifecycles and execution pipelines</p>
        </div>
        <button
          onClick={onNewProject}
          className="flex items-center gap-2 bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white text-xs font-semibold px-4 py-2.5 rounded-xl shadow-lg shadow-sky-500/20 transition-all self-start sm:self-auto"
        >
          <span>Initialize New Software Project</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map((card) => {
          const Icon = card.icon;
          return (
            <div key={card.label} className="p-5 rounded-2xl glass-panel border border-slate-800 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-slate-400">{card.label}</span>
                <div className={`p-2 rounded-xl ${card.bg} ${card.color}`}>
                  <Icon className="w-4 h-4" />
                </div>
              </div>
              <div className="text-2xl font-bold text-white tracking-tight">{card.value}</div>
            </div>
          );
        })}
      </div>

      {/* Recent Projects Table */}
      <div className="p-6 rounded-2xl glass-panel border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-white">Recent Software Engineering Projects</h3>
          <span className="text-xs text-slate-400">{projects.length} Total Projects</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 uppercase tracking-wider text-[10px]">
                <th className="pb-3 pl-2">Project Name</th>
                <th className="pb-3">Status</th>
                <th className="pb-3">Active Agent</th>
                <th className="pb-3">Progress</th>
                <th className="pb-3">Created</th>
                <th className="pb-3 pr-2 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {projects.map((p) => {
                const isCompleted = p.status === 'COMPLETED';
                const isFailed = p.status === 'FAILED';
                const isRunning = !isCompleted && !isFailed && p.status !== 'CREATED';

                return (
                  <tr key={p.id} className="hover:bg-slate-800/30 transition-colors group">
                    <td className="py-3.5 pl-2">
                      <div className="font-semibold text-slate-200 group-hover:text-sky-400 transition-colors cursor-pointer" onClick={() => onSelectProject(p)}>
                        {p.projectName}
                      </div>
                      <div className="text-[11px] text-slate-500 truncate max-w-xs">{p.requirements}</div>
                    </td>
                    <td className="py-3.5">
                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-semibold tracking-wide ${
                        isCompleted
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                          : isFailed
                          ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                          : isRunning
                          ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20 animate-pulse'
                          : 'bg-slate-800 text-slate-400'
                      }`}>
                        {p.status}
                      </span>
                    </td>
                    <td className="py-3.5 text-slate-300">
                      {p.currentAgent ? (
                        <div className="flex items-center gap-1.5">
                          <Bot className="w-3.5 h-3.5 text-sky-400" />
                          <span>{p.currentAgent.replace('_', ' ')}</span>
                        </div>
                      ) : (
                        <span className="text-slate-600">—</span>
                      )}
                    </td>
                    <td className="py-3.5">
                      <div className="w-28 space-y-1">
                        <div className="flex justify-between text-[10px] text-slate-400">
                          <span>{p.progressPercentage}%</span>
                        </div>
                        <div className="h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full transition-all duration-500 ${
                              isCompleted ? 'bg-emerald-500' : isFailed ? 'bg-rose-500' : 'bg-sky-500'
                            }`}
                            style={{ width: `${p.progressPercentage}%` }}
                          />
                        </div>
                      </div>
                    </td>
                    <td className="py-3.5 text-slate-400 text-[11px]">
                      {new Date(p.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-3.5 pr-2 text-right">
                      <div className="flex items-center justify-end gap-2">
                        {p.status === 'CREATED' && (
                          <button
                            onClick={() => onExecuteProject(p.id)}
                            className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-sky-500/10 hover:bg-sky-500/20 text-sky-400 text-xs font-medium transition-colors"
                          >
                            <Play className="w-3 h-3" />
                            <span>Run Agents</span>
                          </button>
                        )}
                        {isCompleted && (
                          <button
                            onClick={() => onDownloadProject(p.id)}
                            className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white text-xs font-medium transition-colors"
                            title="Download complete project ZIP"
                          >
                            <Download className="w-3 h-3" />
                            <span>Export ZIP</span>
                          </button>
                        )}
                        <button
                          onClick={() => onSelectProject(p)}
                          className="px-2.5 py-1 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-slate-300 text-xs font-medium transition-colors"
                        >
                          Workspace
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
              {projects.length === 0 && (
                <tr>
                  <td colSpan={6} className="text-center py-8 text-slate-500">
                    No software projects created yet. Click "Initialize New Software Project" above to start.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
