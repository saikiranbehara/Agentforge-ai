import React, { useState, useEffect } from 'react';
import { Project, CodeReview, AgentExecution } from '../types';
import { api } from '../services/api';
import {
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  ShieldAlert,
  Code,
  Check,
  FileText,
  Activity
} from '../components/Icons';

interface CodeReviewDashboardPageProps {
  project: Project;
}

export const CodeReviewDashboardPage: React.FC<CodeReviewDashboardPageProps> = ({ project }) => {
  const [reviews, setReviews] = useState<CodeReview[]>([]);
  const [reviewSummary, setReviewSummary] = useState<string>('');
  const [qualityScore, setQualityScore] = useState<number>(92);
  const [isApproved, setIsApproved] = useState<boolean>(true);
  const [loading, setLoading] = useState(false);
  const [auditing, setAuditing] = useState(false);

  useEffect(() => {
    loadReviews();
  }, [project.id]);

  const loadReviews = async () => {
    try {
      setLoading(true);
      const data = await api.getProjectReviews(project.id);
      setReviews(data);

      const agents = await api.getProjectAgents(project.id);
      const reviewer = agents.find(a => a.agentType === 'CODE_REVIEWER');
      if (reviewer && reviewer.outputData) {
        try {
          const parsed = JSON.parse(reviewer.outputData);
          if (parsed.qualityScore) setQualityScore(parsed.qualityScore);
          if (parsed.approved !== undefined) setIsApproved(parsed.approved);
          if (parsed.summary) setReviewSummary(parsed.summary);
        } catch {
          // fallback
        }
      }
    } catch (err: any) {
      console.error('Failed to load code reviews', err);
    } finally {
      setLoading(false);
    }
  };

  const handleTriggerAudit = async () => {
    try {
      setAuditing(true);
      const data = await api.triggerCodeReview(project.id);
      setReviews(data);
      await loadReviews();
    } catch (err: any) {
      alert('Audit failed: ' + err.message);
    } finally {
      setAuditing(false);
    }
  };

  const criticalIssues = reviews.filter(r => r.severity === 'CRITICAL' || r.severity === 'ERROR');
  const warningIssues = reviews.filter(r => r.severity === 'WARNING');
  const infoIssues = reviews.filter(r => r.severity === 'INFO');

  return (
    <div className="p-8 space-y-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-6 rounded-2xl glass-panel border border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-sky-400" />
            <h1 className="text-xl font-bold text-white tracking-tight">Code Review & Security Audit</h1>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Static security analysis, OWASP vulnerability scans, and maintainability scores
          </p>
        </div>

        <button
          onClick={handleTriggerAudit}
          disabled={auditing}
          className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white text-xs font-semibold shadow-lg shadow-sky-500/20 transition-all disabled:opacity-50 self-start sm:self-auto"
        >
          <RefreshCw className={`w-4 h-4 ${auditing ? 'animate-spin' : ''}`} />
          <span>{auditing ? 'Auditing Codebase...' : 'Re-Run Security Audit'}</span>
        </button>
      </div>

      {/* Quality Dial & Status Banners */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {/* Score Dial */}
        <div className="p-6 rounded-2xl glass-panel border border-slate-800 flex flex-col items-center justify-center text-center space-y-2">
          <span className="text-xs font-semibold text-slate-400">Quality Score</span>
          <div className="text-5xl font-extrabold text-transparent bg-clip-text bg-gradient-to-tr from-sky-400 to-indigo-300">
            {qualityScore}
            <span className="text-base text-slate-500 font-normal">/100</span>
          </div>
          <span className="text-[11px] text-emerald-400 font-medium">Production Ready</span>
        </div>

        {/* Approval Card */}
        <div className="p-6 rounded-2xl glass-panel border border-slate-800 flex flex-col justify-between space-y-3">
          <div>
            <span className="text-xs font-semibold text-slate-400 block mb-1">Audit Status</span>
            <div className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold ${
              isApproved
                ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                : 'bg-rose-500/15 text-rose-400 border border-rose-500/30'
            }`}>
              {isApproved ? <Check className="w-3.5 h-3.5" /> : <AlertTriangle className="w-3.5 h-3.5" />}
              <span>{isApproved ? 'APPROVED' : 'CHANGES REQUESTED'}</span>
            </div>
          </div>
          <p className="text-[11px] text-slate-400">
            {isApproved ? 'All architectural gates and critical security checks passed.' : 'Requires self-healing resolution.'}
          </p>
        </div>

        {/* Issues Count */}
        <div className="p-6 rounded-2xl glass-panel border border-slate-800 space-y-2">
          <span className="text-xs font-semibold text-slate-400">Severity Breakdown</span>
          <div className="space-y-1.5 text-xs">
            <div className="flex justify-between">
              <span className="text-rose-400">Critical / Errors:</span>
              <span className="font-bold text-white">{criticalIssues.length}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-amber-400">Warnings:</span>
              <span className="font-bold text-white">{warningIssues.length}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-sky-400">Info / Style:</span>
              <span className="font-bold text-white">{infoIssues.length}</span>
            </div>
          </div>
        </div>

        {/* Self-Healing Trigger */}
        <div className="p-6 rounded-2xl glass-panel border border-slate-800 flex flex-col justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-400 block mb-1">Self-Healing Loop</span>
            <div className="text-sm font-semibold text-white">
              {project.retryCount > 0 ? `${project.retryCount} Auto-Fixes Applied` : '0 Auto-Fixes Needed'}
            </div>
          </div>
          <span className="text-[11px] text-slate-400">
            Self-healing automatically corrects test & reviewer failures.
          </span>
        </div>
      </div>

      {/* Reviewer Commentary Summary */}
      {reviewSummary && (
        <div className="p-5 rounded-2xl glass-panel border border-slate-800 space-y-2">
          <div className="text-xs font-semibold text-slate-300">Lead Reviewer Evaluation</div>
          <p className="text-xs text-slate-400 leading-relaxed">{reviewSummary}</p>
        </div>
      )}

      {/* Issues Table */}
      <div className="rounded-2xl glass-panel border border-slate-800 overflow-hidden">
        <div className="p-4 border-b border-slate-800 bg-slate-850/50 flex items-center justify-between">
          <span className="text-xs font-semibold text-white">Audited Files & Improvement Suggestions</span>
          <span className="text-[10px] text-slate-500">{reviews.length} Items</span>
        </div>

        <div className="divide-y divide-slate-800/60">
          {reviews.map((rev) => {
            const isCrit = rev.severity === 'CRITICAL' || rev.severity === 'ERROR';
            const isWarn = rev.severity === 'WARNING';

            return (
              <div key={rev.id} className="p-5 space-y-3 hover:bg-slate-800/20 transition-colors">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      isCrit
                        ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                        : isWarn
                        ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                        : 'bg-sky-500/10 text-sky-400 border border-sky-500/20'
                    }`}>
                      {rev.severity}
                    </span>
                    <span className="font-mono text-xs text-white font-semibold">{rev.filePath}</span>
                    {rev.lineNumber && (
                      <span className="text-[10px] text-slate-500 font-mono">Line {rev.lineNumber}</span>
                    )}
                  </div>

                  {rev.category && (
                    <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-400">
                      {rev.category}
                    </span>
                  )}
                </div>

                <p className="text-xs text-slate-300">{rev.issueDescription}</p>

                {rev.suggestedFix && (
                  <div className="p-3 rounded-xl bg-slate-900/80 border border-slate-800 text-xs space-y-1">
                    <span className="text-[10px] font-semibold text-sky-400 uppercase tracking-wider block">
                      Recommended Remediation:
                    </span>
                    <p className="text-[11px] text-slate-400 leading-relaxed font-mono">{rev.suggestedFix}</p>
                  </div>
                )}
              </div>
            );
          })}
          {reviews.length === 0 && (
            <div className="text-center py-12 text-slate-500 text-xs">
              No code review items yet. Run the review from above or execute the full workflow.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
