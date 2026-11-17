import React from 'react';
import {
  Activity,
  Plus,
  Bot,
  Layers,
  Code,
  ShieldAlert,
  CheckCircle2,
  BookOpen,
  Settings,
  Compass
} from './Icons';

export type NavTab =
  | 'landing'
  | 'dashboard'
  | 'create'
  | 'workspace'
  | 'architecture'
  | 'code'
  | 'testing'
  | 'review'
  | 'docs'
  | 'settings';

interface SidebarProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
  hasActiveProject: boolean;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onTabChange,
  hasActiveProject
}) => {
  const items = [
    { id: 'dashboard' as NavTab, label: 'Dashboard', icon: Activity, requireProject: false },
    { id: 'create' as NavTab, label: 'Create Project', icon: Plus, requireProject: false },
    { id: 'workspace' as NavTab, label: 'Agent Workspace', icon: Bot, requireProject: true },
    { id: 'architecture' as NavTab, label: 'Architecture', icon: Layers, requireProject: true },
    { id: 'code' as NavTab, label: 'Code Generation', icon: Code, requireProject: true },
    { id: 'testing' as NavTab, label: 'Testing Suite', icon: ShieldAlert, requireProject: true },
    { id: 'review' as NavTab, label: 'Code Review', icon: CheckCircle2, requireProject: true },
    { id: 'docs' as NavTab, label: 'Documentation', icon: BookOpen, requireProject: true },
  ];

  return (
    <aside className="w-64 border-r border-slate-800 bg-navy-900/60 flex flex-col justify-between shrink-0 h-[calc(100vh-4rem)] sticky top-16 select-none">
      <div className="p-4 space-y-6">
        <div>
          <div className="text-[10px] font-bold uppercase tracking-wider text-slate-500 px-3 mb-2">
            Platform Navigation
          </div>
          <div className="space-y-1">
            <button
              onClick={() => onTabChange('landing')}
              className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition-all ${
                activeTab === 'landing'
                  ? 'bg-sky-500/10 text-sky-400 font-semibold'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
              }`}
            >
              <Compass className="w-4 h-4 shrink-0" />
              <span>Platform Overview</span>
            </button>
          </div>
        </div>

        <div>
          <div className="text-[10px] font-bold uppercase tracking-wider text-slate-500 px-3 mb-2">
            Engineering Workspace
          </div>
          <div className="space-y-1">
            {items.map((item) => {
              const Icon = item.icon;
              const disabled = item.requireProject && !hasActiveProject;
              const isActive = activeTab === item.id;

              return (
                <button
                  key={item.id}
                  disabled={disabled}
                  onClick={() => onTabChange(item.id)}
                  className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-medium transition-all ${
                    isActive
                      ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30 shadow-sm shadow-sky-500/10'
                      : disabled
                      ? 'text-slate-600 opacity-50 cursor-not-allowed'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <Icon className={`w-4 h-4 shrink-0 ${isActive ? 'text-sky-400' : 'text-slate-400'}`} />
                    <span>{item.label}</span>
                  </div>
                  {item.requireProject && !hasActiveProject && (
                    <span className="text-[9px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-500">Lock</span>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Bottom Settings Link */}
      <div className="p-4 border-t border-slate-800/80">
        <button
          onClick={() => onTabChange('settings')}
          className={`w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition-all ${
            activeTab === 'settings'
              ? 'bg-sky-500/15 text-sky-400 border border-sky-500/30'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
          }`}
        >
          <Settings className="w-4 h-4" />
          <span>AI & System Config</span>
        </button>
      </div>
    </aside>
  );
};
