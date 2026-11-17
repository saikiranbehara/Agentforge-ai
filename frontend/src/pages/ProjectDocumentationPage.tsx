import React, { useState, useEffect } from 'react';
import { Project, ProjectDocument } from '../types';
import { api } from '../services/api';
import {
  BookOpen,
  Download,
  Copy,
  Check,
  FileText,
  Layers,
  Terminal,
  Cpu
} from '../components/Icons';

interface ProjectDocumentationPageProps {
  project: Project;
}

export const ProjectDocumentationPage: React.FC<ProjectDocumentationPageProps> = ({ project }) => {
  const [documents, setDocuments] = useState<ProjectDocument[]>([]);
  const [selectedDoc, setSelectedDoc] = useState<ProjectDocument | null>(null);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    loadDocuments();
  }, [project.id]);

  const loadDocuments = async () => {
    try {
      setLoading(true);
      const data = await api.getProjectDocuments(project.id);
      setDocuments(data);
      if (data.length > 0) setSelectedDoc(data[0]);
    } catch (err: any) {
      console.error('Failed to load project documents', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = () => {
    if (!selectedDoc) return;
    navigator.clipboard.writeText(selectedDoc.documentContent);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownloadDoc = () => {
    if (!selectedDoc) return;
    const blob = new Blob([selectedDoc.documentContent], { type: 'text/markdown;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${selectedDoc.documentType.toLowerCase()}.md`;
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="p-8 space-y-8 max-w-7xl mx-auto h-[calc(100vh-5rem)] flex flex-col">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-6 rounded-2xl glass-panel border border-slate-800 shrink-0">
        <div>
          <div className="flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-sky-400" />
            <h1 className="text-xl font-bold text-white tracking-tight">System Documentation Suite</h1>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Autonomously compiled README.md, Architecture Guides, Installation Manuals, and OpenAPI reference.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={handleDownloadDoc}
            disabled={!selectedDoc}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 transition-colors disabled:opacity-50"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download Doc</span>
          </button>

          <a
            href={api.getDownloadUrl(project.id)}
            download
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white text-xs font-semibold shadow-md shadow-sky-500/20 transition-all"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download All Artifacts</span>
          </a>
        </div>
      </div>

      {/* Split Layout: Document Selector & Markdown Preview */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6 flex-1 min-h-0">
        {/* Left Navigation */}
        <div className="md:col-span-4 rounded-2xl glass-panel border border-slate-800 flex flex-col overflow-hidden">
          <div className="p-4 border-b border-slate-800 bg-slate-850/50">
            <span className="text-xs font-semibold text-white">Documentation Modules</span>
          </div>

          <div className="p-3 space-y-1.5 overflow-y-auto flex-1">
            {documents.map((doc) => {
              const isSelected = selectedDoc?.id === doc.id;
              return (
                <button
                  key={doc.id}
                  onClick={() => setSelectedDoc(doc)}
                  className={`w-full text-left p-3 rounded-xl transition-all flex items-center justify-between ${
                    isSelected
                      ? 'bg-sky-500/15 border border-sky-500/30 text-white shadow-sm shadow-sky-500/10'
                      : 'hover:bg-slate-800/60 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <div className="space-y-0.5">
                    <div className="text-xs font-semibold">{doc.title || doc.documentType}</div>
                    <div className="text-[10px] text-slate-500">{doc.documentType}.md</div>
                  </div>
                  <FileText className={`w-4 h-4 ${isSelected ? 'text-sky-400' : 'text-slate-600'}`} />
                </button>
              );
            })}
            {documents.length === 0 && (
              <div className="text-center py-12 text-slate-500 text-xs">
                No documentation generated yet. The Documentation Agent compiles docs upon pipeline completion.
              </div>
            )}
          </div>
        </div>

        {/* Right Preview */}
        <div className="md:col-span-8 rounded-2xl glass-panel border border-slate-800 flex flex-col overflow-hidden">
          {selectedDoc ? (
            <>
              <div className="p-4 border-b border-slate-800 bg-slate-850/50 flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold text-white font-mono">{selectedDoc.documentType}.md</span>
                  <span className="text-[10px] text-slate-500 ml-2">Generated by Documentation Agent</span>
                </div>
                <button
                  onClick={handleCopy}
                  className="flex items-center gap-1 text-[11px] px-2.5 py-1 rounded-lg bg-slate-800 text-slate-300 hover:text-white transition-colors"
                >
                  {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copied ? 'Copied' : 'Copy Content'}</span>
                </button>
              </div>

              <div className="flex-1 overflow-auto bg-slate-950 p-6 font-mono text-xs text-slate-200 leading-relaxed">
                <pre className="whitespace-pre-wrap">{selectedDoc.documentContent}</pre>
              </div>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-slate-500 text-xs">
              Select a documentation file to preview.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
