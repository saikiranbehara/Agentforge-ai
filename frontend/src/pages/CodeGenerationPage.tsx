import React, { useState, useEffect } from 'react';
import { Project, GeneratedFile } from '../types';
import { api } from '../services/api';
import {
  Code,
  Folder,
  File,
  Download,
  RefreshCw,
  Copy,
  Check,
  Clock,
  ChevronRight,
  GitBranch,
  Terminal
} from '../components/Icons';

interface CodeGenerationPageProps {
  project: Project;
}

export const CodeGenerationPage: React.FC<CodeGenerationPageProps> = ({ project }) => {
  const [files, setFiles] = useState<GeneratedFile[]>([]);
  const [selectedFile, setSelectedFile] = useState<GeneratedFile | null>(null);
  const [fileHistory, setFileHistory] = useState<GeneratedFile[]>([]);
  const [loading, setLoading] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [customInstructions, setCustomInstructions] = useState('');
  const [showRegenModal, setShowRegenModal] = useState(false);
  const [copied, setCopied] = useState(false);
  const [showHistory, setShowHistory] = useState(false);

  useEffect(() => {
    loadFiles();
  }, [project.id]);

  const loadFiles = async () => {
    try {
      setLoading(true);
      const data = await api.getProjectFiles(project.id);
      setFiles(data);
      if (data.length > 0 && !selectedFile) {
        setSelectedFile(data[0]);
      } else if (selectedFile) {
        const found = data.find(f => f.id === selectedFile.id) || data[0];
        setSelectedFile(found);
      }
    } catch (err: any) {
      console.error('Failed to load files', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectFile = async (file: GeneratedFile) => {
    setSelectedFile(file);
    setShowHistory(false);
    try {
      const history = await api.getFileHistory(project.id, file.filePath);
      setFileHistory(history);
    } catch {
      setFileHistory([]);
    }
  };

  const handleCopyCode = () => {
    if (!selectedFile) return;
    navigator.clipboard.writeText(selectedFile.fileContent);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleRegenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) return;
    try {
      setRegenerating(true);
      const newVer = await api.regenerateFile(project.id, selectedFile.id, customInstructions);
      setShowRegenModal(false);
      setCustomInstructions('');
      await loadFiles();
      setSelectedFile(newVer);
    } catch (err: any) {
      alert('Regeneration failed: ' + err.message);
    } finally {
      setRegenerating(false);
    }
  };

  const handleRestoreVersion = async (targetFileId: number) => {
    if (!confirm('Restore this previous version as the latest?')) return;
    try {
      setLoading(true);
      const restored = await api.restoreFileVersion(project.id, targetFileId);
      await loadFiles();
      setSelectedFile(restored);
      setShowHistory(false);
    } catch (err: any) {
      alert('Restore failed: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  // Group files into backend, frontend, and root categories
  const backendFiles = files.filter(f => f.filePath.startsWith('backend/'));
  const frontendFiles = files.filter(f => f.filePath.startsWith('frontend/'));
  const rootFiles = files.filter(f => !f.filePath.startsWith('backend/') && !f.filePath.startsWith('frontend/'));

  return (
    <div className="p-8 space-y-6 max-w-7xl mx-auto h-[calc(100vh-5rem)] flex flex-col">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 rounded-2xl glass-panel border border-slate-800 shrink-0">
        <div>
          <div className="flex items-center gap-2">
            <Code className="w-5 h-5 text-sky-400" />
            <h1 className="text-lg font-bold text-white tracking-tight">Source Code Workspace</h1>
            <span className="text-[10px] bg-slate-800 text-slate-400 px-2 py-0.5 rounded-full border border-slate-700">
              {files.length} Generated Files
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Inspect source files, compare revision histories, or trigger targeted agent refactorings.
          </p>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2.5">
          <button
            onClick={() => setShowRegenModal(true)}
            disabled={!selectedFile}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 transition-colors disabled:opacity-50"
          >
            <RefreshCw className="w-3.5 h-3.5 text-sky-400" />
            <span>Refactor / Regenerate File</span>
          </button>

          <a
            href={api.getDownloadUrl(project.id)}
            download
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white text-xs font-semibold shadow-md shadow-sky-500/20 transition-all"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download All (ZIP)</span>
          </a>
        </div>
      </div>

      {/* Main Split Layout: File Tree & Code Viewer */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6 flex-1 min-h-0">
        {/* Left: Folder Tree */}
        <div className="md:col-span-4 rounded-2xl glass-panel border border-slate-800 flex flex-col overflow-hidden">
          <div className="p-3.5 border-b border-slate-800 bg-slate-850/50 flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-300">Project Structure</span>
            <span className="text-[10px] text-slate-500 font-mono">v{selectedFile?.version || 1}</span>
          </div>

          <div className="p-3 overflow-y-auto flex-1 space-y-4 font-mono text-xs">
            {/* Backend Directory */}
            {backendFiles.length > 0 && (
              <div>
                <div className="flex items-center gap-1.5 text-sky-400 font-semibold mb-1 text-[11px]">
                  <Folder className="w-3.5 h-3.5" />
                  <span>backend/</span>
                </div>
                <div className="pl-4 space-y-0.5 border-l border-slate-800">
                  {backendFiles.map((f) => (
                    <button
                      key={f.id}
                      onClick={() => handleSelectFile(f)}
                      className={`w-full text-left px-2 py-1 rounded-lg truncate flex items-center justify-between group transition-colors ${
                        selectedFile?.id === f.id
                          ? 'bg-sky-500/15 text-sky-300 font-semibold'
                          : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                      }`}
                    >
                      <span className="truncate">{f.filePath.replace('backend/', '')}</span>
                      <span className="text-[9px] text-slate-600 group-hover:text-slate-400">v{f.version}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Frontend Directory */}
            {frontendFiles.length > 0 && (
              <div>
                <div className="flex items-center gap-1.5 text-indigo-400 font-semibold mb-1 text-[11px]">
                  <Folder className="w-3.5 h-3.5" />
                  <span>frontend/</span>
                </div>
                <div className="pl-4 space-y-0.5 border-l border-slate-800">
                  {frontendFiles.map((f) => (
                    <button
                      key={f.id}
                      onClick={() => handleSelectFile(f)}
                      className={`w-full text-left px-2 py-1 rounded-lg truncate flex items-center justify-between group transition-colors ${
                        selectedFile?.id === f.id
                          ? 'bg-indigo-500/15 text-indigo-300 font-semibold'
                          : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                      }`}
                    >
                      <span className="truncate">{f.filePath.replace('frontend/', '')}</span>
                      <span className="text-[9px] text-slate-600 group-hover:text-slate-400">v{f.version}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Root Documentation Files */}
            {rootFiles.length > 0 && (
              <div>
                <div className="flex items-center gap-1.5 text-emerald-400 font-semibold mb-1 text-[11px]">
                  <Folder className="w-3.5 h-3.5" />
                  <span>root/ & docs/</span>
                </div>
                <div className="pl-4 space-y-0.5 border-l border-slate-800">
                  {rootFiles.map((f) => (
                    <button
                      key={f.id}
                      onClick={() => handleSelectFile(f)}
                      className={`w-full text-left px-2 py-1 rounded-lg truncate flex items-center justify-between group transition-colors ${
                        selectedFile?.id === f.id
                          ? 'bg-emerald-500/15 text-emerald-300 font-semibold'
                          : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                      }`}
                    >
                      <span className="truncate">{f.filePath}</span>
                      <span className="text-[9px] text-slate-600 group-hover:text-slate-400">v{f.version}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right: Code Viewer */}
        <div className="md:col-span-8 rounded-2xl glass-panel border border-slate-800 flex flex-col overflow-hidden">
          {selectedFile ? (
            <>
              {/* Viewer Header */}
              <div className="p-3.5 border-b border-slate-800 bg-slate-850/50 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <File className="w-4 h-4 text-sky-400" />
                  <span className="font-mono text-xs text-white font-semibold">{selectedFile.filePath}</span>
                  <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-sky-400 border border-slate-700">
                    Version {selectedFile.version}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  {/* Version History Toggle */}
                  <button
                    onClick={() => setShowHistory(!showHistory)}
                    className="flex items-center gap-1 text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 text-slate-300 hover:text-white transition-colors"
                  >
                    <GitBranch className="w-3.5 h-3.5" />
                    <span>Revisions ({fileHistory.length})</span>
                  </button>

                  {/* Copy Button */}
                  <button
                    onClick={handleCopyCode}
                    className="flex items-center gap-1 text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 text-slate-300 hover:text-white transition-colors"
                  >
                    {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copied ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
              </div>

              {/* Version History Drawer */}
              {showHistory && fileHistory.length > 0 && (
                <div className="bg-slate-900 border-b border-slate-800 p-3 space-y-2">
                  <div className="text-[11px] font-semibold text-slate-300">Version History for this file:</div>
                  <div className="flex flex-wrap gap-2">
                    {fileHistory.map((ver) => (
                      <div
                        key={ver.id}
                        className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border text-xs ${
                          ver.id === selectedFile.id
                            ? 'bg-sky-500/20 border-sky-500 text-sky-300'
                            : 'bg-slate-800 border-slate-700 text-slate-400'
                        }`}
                      >
                        <span>v{ver.version}</span>
                        <span className="text-[10px] text-slate-500">
                          {new Date(ver.createdAt).toLocaleTimeString()}
                        </span>
                        {ver.id !== selectedFile.id && (
                          <button
                            onClick={() => handleRestoreVersion(ver.id)}
                            className="text-[10px] text-sky-400 hover:underline font-semibold"
                          >
                            Restore
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Source Code Content */}
              <div className="flex-1 overflow-auto bg-slate-950 p-4 font-mono text-xs leading-relaxed text-slate-200">
                <pre className="whitespace-pre">{selectedFile.fileContent}</pre>
              </div>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-slate-500 text-xs">
              Select a file from the project structure to view and edit.
            </div>
          )}
        </div>
      </div>

      {/* Regeneration Modal */}
      {showRegenModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
          <div className="bg-navy-900 border border-slate-700 rounded-2xl w-full max-w-lg p-6 space-y-4">
            <h3 className="text-base font-semibold text-white">Refactor File: {selectedFile?.filePath}</h3>
            <p className="text-xs text-slate-400">
              Provide custom instructions for the developer agent to refine, optimize, or resolve specific edge cases in this file.
            </p>
            <form onSubmit={handleRegenerate} className="space-y-4">
              <textarea
                rows={4}
                required
                placeholder="e.g. Add input validation annotations, enhance exception handling, or optimize database queries..."
                value={customInstructions}
                onChange={(e) => setCustomInstructions(e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-xl p-3 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-sky-500"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowRegenModal(false)}
                  className="px-4 py-2 text-xs font-medium text-slate-400 hover:text-white rounded-xl hover:bg-slate-800 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={regenerating}
                  className="px-5 py-2 text-xs font-semibold text-white bg-sky-600 hover:bg-sky-500 rounded-xl transition-all shadow-md disabled:opacity-50"
                >
                  {regenerating ? 'Regenerating...' : 'Regenerate File'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
