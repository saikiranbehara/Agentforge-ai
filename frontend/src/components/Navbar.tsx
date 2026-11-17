import React from 'react';
import { Project, AiConfig, User } from '../types';
import { Bot, Sparkles, Settings, Plus, LogOut, ChevronDown } from './Icons';

interface NavbarProps {
  currentProject: Project | null;
  projects: Project[];
  onSelectProject: (p: Project) => void;
  aiConfig: AiConfig | null;
  onOpenAiSettings: () => void;
  onNewProject: () => void;
  user: User | null;
  onLogout: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentProject,
  projects,
  onSelectProject,
  aiConfig,
  onOpenAiSettings,
  onNewProject,
  user,
  onLogout
}) => {
  const isMock = aiConfig?.mockModeEnabled || aiConfig?.activeProvider === 'MOCK';

  return (
    <nav className="h-16 border-b border-slate-800 bg-navy-900/90 backdrop-blur-md px-6 flex items-center justify-between sticky top-0 z-40">
      {/* Brand */}
      <div className="flex items-center gap-6">
        <a href="#landing" className="flex items-center gap-3 group">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-sky-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-sky-500/20 group-hover:scale-105 transition-transform">
            <Bot className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="font-bold text-lg tracking-tight text-white">Agent<span className="text-sky-400">Forge</span></span>
              <span className="text-[10px] font-semibold uppercase px-1.5 py-0.5 rounded bg-sky-500/10 text-sky-400 border border-sky-500/20">v1.0</span>
            </div>
          </div>
        </a>

        {/* Project Selector */}
        {projects.length > 0 && (
          <div className="relative group">
            <select
              value={currentProject?.id || ''}
              onChange={(e) => {
                const found = projects.find(p => p.id === Number(e.target.value));
                if (found) onSelectProject(found);
              }}
              className="appearance-none bg-slate-800/80 hover:bg-slate-800 text-slate-200 text-xs font-medium pl-3 pr-8 py-2 rounded-lg border border-slate-700/80 focus:outline-none focus:border-sky-500 transition-colors cursor-pointer max-w-[200px] truncate"
            >
              {projects.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.projectName}
                </option>
              ))}
            </select>
            <ChevronDown className="w-3.5 h-3.5 text-slate-400 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none" />
          </div>
        )}
      </div>

      {/* Right Controls */}
      <div className="flex items-center gap-3">
        {/* AI Provider Status Pill */}
        <button
          onClick={onOpenAiSettings}
          className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium border transition-all ${
            isMock
              ? 'bg-amber-500/10 text-amber-300 border-amber-500/30 hover:bg-amber-500/20'
              : 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30 hover:bg-emerald-500/20'
          }`}
          title="Click to configure AI providers and API keys"
        >
          <span className={`w-2 h-2 rounded-full ${isMock ? 'bg-amber-400 animate-pulse' : 'bg-emerald-400'}`} />
          <span>{isMock ? 'Mock Provider (Simulated)' : `${aiConfig?.activeProvider || 'AI'} (${aiConfig?.defaultModel || 'Online'})`}</span>
          <Settings className="w-3.5 h-3.5 ml-1 text-slate-400 hover:text-white" />
        </button>

        {/* New Project Button */}
        <button
          onClick={onNewProject}
          className="flex items-center gap-1.5 bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white text-xs font-semibold px-3 py-2 rounded-lg shadow-md shadow-sky-500/20 transition-all"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>New Project</span>
        </button>

        {/* User profile / Logout */}
        <div className="flex items-center gap-2 pl-3 border-l border-slate-800">
          <div className="w-7 h-7 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-xs font-semibold text-sky-400">
            {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
          </div>
          <span className="text-xs text-slate-300 hidden md:inline">{user?.name || 'Developer'}</span>
          <button
            onClick={onLogout}
            className="p-1.5 text-slate-400 hover:text-rose-400 rounded-lg hover:bg-slate-800 transition-colors"
            title="Log out"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </nav>
  );
};
