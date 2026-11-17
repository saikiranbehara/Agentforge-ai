import React, { useState } from 'react';
import { AiConfig } from '../types';
import { api } from '../services/api';
import { Settings, Sparkles, X, Check, AlertTriangle, Lock } from './Icons';

interface AiConfigModalProps {
  config: AiConfig | null;
  isOpen: boolean;
  onClose: () => void;
  onConfigUpdated: (newConfig: AiConfig) => void;
}

export const AiConfigModal: React.FC<AiConfigModalProps> = ({
  config,
  isOpen,
  onClose,
  onConfigUpdated
}) => {
  if (!isOpen) return null;

  const [activeProvider, setActiveProvider] = useState<string>(config?.activeProvider || 'MOCK');
  const [defaultModel, setDefaultModel] = useState<string>(config?.defaultModel || 'gemini-1.5-pro');
  const [geminiApiKey, setGeminiApiKey] = useState<string>('');
  const [openaiApiKey, setOpenaiApiKey] = useState<string>('');
  const [mockModeEnabled, setMockModeEnabled] = useState<boolean>(config?.mockModeEnabled || false);
  const [saving, setSaving] = useState(false);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setStatusMsg(null);
    try {
      const payload: Partial<AiConfig> = {
        activeProvider,
        defaultModel,
        mockModeEnabled
      };
      if (geminiApiKey.trim()) payload.geminiApiKey = geminiApiKey.trim();
      if (openaiApiKey.trim()) payload.openaiApiKey = openaiApiKey.trim();

      const updated = await api.updateAiConfig(payload);
      onConfigUpdated(updated);
      setStatusMsg('Configuration saved successfully!');
      setTimeout(() => {
        onClose();
      }, 1200);
    } catch (err: any) {
      setStatusMsg('Failed to update config: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
      <div className="bg-navy-900 border border-slate-700/80 rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-850/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-lg bg-sky-500/10 text-sky-400">
              <Settings className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-white">AI Engine Configuration</h3>
              <p className="text-xs text-slate-400">Select LLM provider or activate the intelligent mock simulation</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Form */}
        <form onSubmit={handleSave} className="p-6 space-y-6">
          {statusMsg && (
            <div className="p-3 rounded-xl bg-sky-500/10 border border-sky-500/30 text-sky-300 text-xs flex items-center gap-2">
              <Check className="w-4 h-4 shrink-0 text-sky-400" />
              <span>{statusMsg}</span>
            </div>
          )}

          {/* Mode Info Callout */}
          <div className="p-3.5 rounded-xl bg-slate-800/60 border border-slate-700/60 text-xs space-y-1.5">
            <div className="flex items-center gap-2 font-medium text-slate-200">
              <Sparkles className="w-4 h-4 text-sky-400" />
              <span>Current Status: {config?.mockModeEnabled || config?.activeProvider === 'MOCK' ? 'Mock Development Mode Active' : 'Live Cloud LLM Mode Active'}</span>
            </div>
            <p className="text-slate-400 text-[11px] leading-relaxed">
              When an API key is not supplied or Mock mode is toggled, AgentForge uses an intelligent, domain-aware mock provider to generate authentic, compilable full-stack software for UI testing and demos.
            </p>
          </div>

          {/* Provider Toggle */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-2">Primary AI Provider</label>
            <div className="grid grid-cols-3 gap-2.5">
              {[
                { id: 'GEMINI', name: 'Google Gemini', desc: '1.5 Pro / Flash' },
                { id: 'OPENAI', name: 'OpenAI GPT', desc: 'GPT-4o / Mini' },
                { id: 'MOCK', name: 'Mock Provider', desc: 'Offline Simulated' },
              ].map((prov) => (
                <button
                  type="button"
                  key={prov.id}
                  onClick={() => {
                    setActiveProvider(prov.id);
                    if (prov.id === 'MOCK') setMockModeEnabled(true);
                  }}
                  className={`p-3 rounded-xl border text-left transition-all ${
                    activeProvider === prov.id
                      ? 'bg-sky-500/15 border-sky-500 text-white shadow-md shadow-sky-500/10'
                      : 'bg-slate-800/40 border-slate-700/80 text-slate-400 hover:text-slate-200 hover:bg-slate-800/70'
                  }`}
                >
                  <div className="text-xs font-semibold text-slate-200">{prov.name}</div>
                  <div className="text-[10px] text-slate-400">{prov.desc}</div>
                </button>
              ))}
            </div>
          </div>

          {/* Model Selector */}
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">Model Variant</label>
            <select
              value={defaultModel}
              onChange={(e) => setDefaultModel(e.target.value)}
              className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2.5 text-xs text-slate-200 focus:outline-none focus:border-sky-500"
            >
              <option value="gemini-1.5-pro">Gemini 1.5 Pro (Google DeepMind - Recommended)</option>
              <option value="gemini-1.5-flash">Gemini 1.5 Flash (Ultra-fast generation)</option>
              <option value="gpt-4o">OpenAI GPT-4o (Omni Reasoning)</option>
              <option value="gpt-4o-mini">OpenAI GPT-4o-mini (Lightweight)</option>
            </select>
          </div>

          {/* Gemini API Key */}
          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="text-xs font-semibold text-slate-300">Gemini API Key</label>
              <span className="text-[10px] text-slate-400">Current: {config?.geminiApiKeyMasked || 'None'}</span>
            </div>
            <input
              type="password"
              placeholder="AIzaSy... (Leave blank to keep existing key)"
              value={geminiApiKey}
              onChange={(e) => setGeminiApiKey(e.target.value)}
              className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-sky-500"
            />
          </div>

          {/* OpenAI API Key */}
          <div>
            <div className="flex justify-between items-center mb-1">
              <label className="text-xs font-semibold text-slate-300">OpenAI API Key</label>
              <span className="text-[10px] text-slate-400">Current: {config?.openaiApiKeyMasked || 'None'}</span>
            </div>
            <input
              type="password"
              placeholder="sk-... (Leave blank to keep existing key)"
              value={openaiApiKey}
              onChange={(e) => setOpenaiApiKey(e.target.value)}
              className="w-full bg-slate-800 border border-slate-700 rounded-xl p-2.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-sky-500"
            />
          </div>

          {/* Force Mock Mode Switch */}
          <div className="flex items-center justify-between p-3 rounded-xl bg-slate-800/40 border border-slate-700/60">
            <div>
              <span className="text-xs font-semibold text-slate-200 block">Force Mock Simulation Mode</span>
              <span className="text-[11px] text-slate-400 block">Enable zero-cost test runs even if API keys are set</span>
            </div>
            <input
              type="checkbox"
              checked={mockModeEnabled}
              onChange={(e) => setMockModeEnabled(e.target.checked)}
              className="w-4 h-4 rounded text-sky-500 focus:ring-sky-500 focus:ring-offset-slate-900"
            />
          </div>

          {/* Footer Actions */}
          <div className="pt-2 flex justify-end gap-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-medium text-slate-400 hover:text-white rounded-xl hover:bg-slate-800 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2 text-xs font-semibold text-white bg-sky-600 hover:bg-sky-500 rounded-xl transition-all shadow-md shadow-sky-500/20 disabled:opacity-50"
            >
              {saving ? 'Saving...' : 'Save Settings'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
