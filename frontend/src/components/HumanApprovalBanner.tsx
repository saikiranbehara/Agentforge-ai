import React, { useState } from 'react';
import { Project } from '../types';
import { api } from '../services/api';
import { AlertTriangle, Check, RefreshCw } from './Icons';

interface HumanApprovalBannerProps {
  project: Project;
  onUpdated: (p: Project) => void;
  onViewDetails: () => void;
}

export const HumanApprovalBanner: React.FC<HumanApprovalBannerProps> = ({
  project,
  onUpdated,
  onViewDetails
}) => {
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState('');
  const [showFeedbackInput, setShowFeedbackInput] = useState(false);

  const isRequirementsPending = project.status === 'PENDING_REQUIREMENTS_APPROVAL';
  const isArchitecturePending = project.status === 'PENDING_ARCHITECTURE_APPROVAL';

  if (!isRequirementsPending && !isArchitecturePending) return null;

  const stageName = isRequirementsPending ? 'Requirements Specification' : 'System Architecture & Schema';

  const handleApprove = async () => {
    try {
      setLoading(true);
      let updated: Project;
      if (isRequirementsPending) {
        updated = await api.approveRequirements(project.id, true, feedback);
      } else {
        updated = await api.approveArchitecture(project.id, true, feedback);
      }
      onUpdated(updated);
    } catch (err: any) {
      alert('Approval failed: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleReject = async () => {
    if (!feedback.trim()) {
      alert('Please enter revision instructions or feedback before requesting changes.');
      return;
    }
    try {
      setLoading(true);
      let updated: Project;
      if (isRequirementsPending) {
        updated = await api.approveRequirements(project.id, false, feedback);
      } else {
        updated = await api.approveArchitecture(project.id, false, feedback);
      }
      onUpdated(updated);
    } catch (err: any) {
      alert('Rejection failed: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-amber-500/10 border-b border-amber-500/30 px-6 py-3.5 flex flex-col md:flex-row md:items-center justify-between gap-3 text-xs animate-fadeIn">
      <div className="flex items-center gap-3">
        <div className="p-1.5 rounded-lg bg-amber-500/20 text-amber-400">
          <AlertTriangle className="w-4 h-4 shrink-0" />
        </div>
        <div>
          <span className="font-semibold text-amber-200">Human-In-The-Loop Approval Required: </span>
          <span className="text-amber-300/90">
            Please review the generated {stageName} before the agent team proceeds to code synthesis.
          </span>
        </div>
      </div>

      <div className="flex items-center gap-2.5">
        <button
          onClick={onViewDetails}
          className="text-slate-300 hover:text-white px-3 py-1.5 rounded-lg bg-slate-800/80 hover:bg-slate-800 border border-slate-700 transition-colors font-medium"
        >
          Inspect Specs
        </button>

        {!showFeedbackInput ? (
          <button
            onClick={() => setShowFeedbackInput(true)}
            className="text-amber-300 hover:text-amber-200 px-3 py-1.5 rounded-lg bg-amber-500/20 border border-amber-500/30 transition-colors font-medium"
          >
            Request Revisions
          </button>
        ) : (
          <div className="flex items-center gap-2">
            <input
              type="text"
              placeholder="Enter revision guidance..."
              value={feedback}
              onChange={(e) => setFeedback(e.target.value)}
              className="bg-slate-900 border border-amber-500/50 rounded-lg px-2.5 py-1 text-xs text-white focus:outline-none"
            />
            <button
              onClick={handleReject}
              disabled={loading}
              className="px-2.5 py-1 rounded-lg bg-amber-600 hover:bg-amber-500 text-white font-medium"
            >
              Submit Revision
            </button>
          </div>
        )}

        <button
          onClick={handleApprove}
          disabled={loading}
          className="flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-500 text-white px-3.5 py-1.5 rounded-lg font-semibold shadow-sm transition-all disabled:opacity-50"
        >
          <Check className="w-3.5 h-3.5" />
          <span>Approve & Proceed</span>
        </button>
      </div>
    </div>
  );
};
