import React, { useState, useEffect } from 'react';
import { Project, AgentExecution, SystemArchitectureSpec } from '../types';
import { api } from '../services/api';
import {
  Layers,
  Database,
  Terminal,
  Check,
  CheckCircle2,
  AlertTriangle,
  Code,
  Boxes,
  Lock
} from '../components/Icons';

interface ArchitectureViewPageProps {
  project: Project;
  onProjectUpdated: (p: Project) => void;
}

export const ArchitectureViewPage: React.FC<ArchitectureViewPageProps> = ({
  project,
  onProjectUpdated
}) => {
  const [archSpec, setArchSpec] = useState<SystemArchitectureSpec | null>(null);
  const [loading, setLoading] = useState(false);
  const [approving, setApproving] = useState(false);
  const [activeTab, setActiveTab] = useState<'erd' | 'api' | 'modules'>('erd');

  useEffect(() => {
    loadArchitectureSpec();
  }, [project.id]);

  const loadArchitectureSpec = async () => {
    try {
      setLoading(true);
      const agents = await api.getProjectAgents(project.id);
      const architect = agents.find(a => a.agentType === 'SYSTEM_ARCHITECT');
      if (architect && architect.outputData) {
        try {
          const parsed = JSON.parse(architect.outputData);
          setArchSpec(parsed);
        } catch {
          // not ready
        }
      }
    } catch (err: any) {
      console.error('Failed to load architecture spec', err);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async () => {
    try {
      setApproving(true);
      const updated = await api.approveArchitecture(project.id, true);
      onProjectUpdated(updated);
      alert('Architecture successfully approved! Developer agents will begin code generation.');
    } catch (err: any) {
      alert('Approval failed: ' + err.message);
    } finally {
      setApproving(false);
    }
  };

  return (
    <div className="p-8 space-y-8 max-w-7xl mx-auto">
      {/* Header & Approval Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 p-6 rounded-2xl glass-panel border border-slate-800">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-xl font-bold text-white tracking-tight">System Architecture Specification</h1>
            {project.architectureApproved ? (
              <span className="flex items-center gap-1 text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-2.5 py-0.5 rounded-full">
                <Check className="w-3 h-3" />
                <span>Architecture Approved</span>
              </span>
            ) : (
              <span className="flex items-center gap-1 text-[10px] font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20 px-2.5 py-0.5 rounded-full">
                <AlertTriangle className="w-3 h-3" />
                <span>Awaiting Review</span>
              </span>
            )}
          </div>
          <p className="text-xs text-slate-400 mt-1 max-w-2xl">
            {archSpec?.overview || 'Relational schemas, REST endpoint specifications, and module boundaries architected by AgentForge.'}
          </p>
        </div>

        {/* Approval Button */}
        {!project.architectureApproved && (
          <button
            onClick={handleApprove}
            disabled={approving || !archSpec}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold shadow-lg shadow-emerald-500/20 transition-all disabled:opacity-50 self-start md:self-auto"
          >
            <Check className="w-4 h-4" />
            <span>{approving ? 'Approving...' : 'Approve Architecture'}</span>
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-slate-800 pb-2">
        <button
          onClick={() => setActiveTab('erd')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold transition-all ${
            activeTab === 'erd'
              ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <Database className="w-4 h-4" />
          <span>Database Schema ({archSpec?.databaseSchema?.length || 0} Tables)</span>
        </button>

        <button
          onClick={() => setActiveTab('api')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold transition-all ${
            activeTab === 'api'
              ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <Terminal className="w-4 h-4" />
          <span>REST API Endpoints ({archSpec?.apiEndpoints?.length || 0})</span>
        </button>

        <button
          onClick={() => setActiveTab('modules')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold transition-all ${
            activeTab === 'modules'
              ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <Boxes className="w-4 h-4" />
          <span>Module Dependencies & Patterns</span>
        </button>
      </div>

      {/* Content Panes */}
      {activeTab === 'erd' && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {archSpec?.databaseSchema?.map((table) => (
            <div key={table.tableName} className="rounded-2xl glass-panel border border-slate-800 overflow-hidden flex flex-col justify-between">
              <div>
                <div className="p-4 bg-slate-850/60 border-b border-slate-800 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Database className="w-4 h-4 text-sky-400" />
                    <span className="font-mono text-xs font-bold text-white">{table.tableName}</span>
                  </div>
                  <span className="text-[10px] text-slate-500">{table.columns?.length || 0} columns</span>
                </div>
                <div className="p-4 space-y-2">
                  <p className="text-[11px] text-slate-400 mb-3">{table.description}</p>
                  <div className="divide-y divide-slate-800/60">
                    {table.columns?.map((col) => (
                      <div key={col.name} className="py-1.5 flex items-center justify-between text-xs font-mono">
                        <div className="flex items-center gap-1.5">
                          {col.primaryKey && (
                            <span className="text-[9px] bg-amber-500/20 text-amber-400 px-1 py-0.2 rounded">PK</span>
                          )}
                          <span className={col.primaryKey ? 'font-bold text-white' : 'text-slate-300'}>{col.name}</span>
                        </div>
                        <span className="text-[10px] text-sky-400/80">{col.type}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {table.relationships && table.relationships.length > 0 && (
                <div className="p-3 bg-slate-900/40 border-t border-slate-800/80 text-[10px] text-slate-400 space-y-1">
                  <span className="font-semibold text-slate-300 block">Relationships:</span>
                  {table.relationships.map((rel, idx) => (
                    <div key={idx} className="flex items-center gap-1">
                      <span className="text-sky-400">•</span>
                      <span>{rel}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
          {(!archSpec?.databaseSchema || archSpec.databaseSchema.length === 0) && (
            <div className="col-span-3 text-center py-12 text-slate-500 text-xs">
              System Architect Agent has not generated database schemas yet. Run the workflow from the Workspace tab.
            </div>
          )}
        </div>
      )}

      {activeTab === 'api' && (
        <div className="rounded-2xl glass-panel border border-slate-800 overflow-hidden">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 uppercase tracking-wider text-[10px] bg-slate-850/50">
                <th className="py-3 pl-4">Method</th>
                <th className="py-3">Endpoint Path</th>
                <th className="py-3">Summary</th>
                <th className="py-3">Request Payload</th>
                <th className="py-3">Response Payload</th>
                <th className="py-3 pr-4">Authorization</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono">
              {archSpec?.apiEndpoints?.map((ep, idx) => {
                const isGet = ep.method === 'GET';
                const isPost = ep.method === 'POST';
                const isPut = ep.method === 'PUT';
                const isDelete = ep.method === 'DELETE';

                return (
                  <tr key={idx} className="hover:bg-slate-800/30 transition-colors">
                    <td className="py-3 pl-4">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        isGet
                          ? 'bg-sky-500/10 text-sky-400'
                          : isPost
                          ? 'bg-emerald-500/10 text-emerald-400'
                          : isPut
                          ? 'bg-amber-500/10 text-amber-400'
                          : 'bg-rose-500/10 text-rose-400'
                      }`}>
                        {ep.method}
                      </span>
                    </td>
                    <td className="py-3 font-semibold text-white">{ep.path}</td>
                    <td className="py-3 font-sans text-slate-300">{ep.summary}</td>
                    <td className="py-3 text-slate-400 text-[11px]">{ep.requestBody || 'None'}</td>
                    <td className="py-3 text-slate-400 text-[11px]">{ep.responseBody || '200 OK'}</td>
                    <td className="py-3 pr-4">
                      {ep.authRequired && ep.authRequired !== 'None' ? (
                        <div className="flex items-center gap-1 text-[11px] text-amber-300 font-sans">
                          <Lock className="w-3 h-3 text-amber-400" />
                          <span>{ep.authRequired}</span>
                        </div>
                      ) : (
                        <span className="text-slate-500 text-[11px] font-sans">Public</span>
                      )}
                    </td>
                  </tr>
                );
              })}
              {(!archSpec?.apiEndpoints || archSpec.apiEndpoints.length === 0) && (
                <tr>
                  <td colSpan={6} className="text-center py-8 text-slate-500 text-xs font-sans">
                    No REST endpoints architected yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'modules' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Module Dependencies */}
          <div className="p-6 rounded-2xl glass-panel border border-slate-800 space-y-4">
            <h3 className="text-sm font-semibold text-white flex items-center gap-2">
              <Boxes className="w-4 h-4 text-sky-400" />
              <span>Inter-Module Dependency Graph</span>
            </h3>
            <div className="space-y-3">
              {archSpec?.moduleDependencies?.map((mod, idx) => (
                <div key={idx} className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 space-y-2">
                  <div className="font-semibold text-slate-200 text-xs">{mod.module}</div>
                  <div className="flex flex-wrap items-center gap-1.5 text-[11px]">
                    <span className="text-slate-500">Depends on:</span>
                    {mod.dependsOn?.map((d) => (
                      <span key={d} className="px-2 py-0.5 rounded bg-sky-500/10 text-sky-300 border border-sky-500/20">
                        {d}
                      </span>
                    ))}
                  </div>
                  <p className="text-[11px] text-slate-400">{mod.communication}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Design Patterns & Security */}
          <div className="p-6 rounded-2xl glass-panel border border-slate-800 space-y-6">
            <div>
              <h3 className="text-sm font-semibold text-white mb-3">Architectural Design Patterns</h3>
              <div className="flex flex-wrap gap-2">
                {archSpec?.designPatterns?.map((pat) => (
                  <span key={pat} className="px-3 py-1.5 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-medium">
                    {pat}
                  </span>
                ))}
              </div>
            </div>

            <div>
              <h3 className="text-sm font-semibold text-white mb-3">Security & OWASP Hardening</h3>
              <div className="space-y-2">
                {archSpec?.securityConsiderations?.map((sec, idx) => (
                  <div key={idx} className="flex items-start gap-2 text-xs text-slate-300">
                    <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                    <span>{sec}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
