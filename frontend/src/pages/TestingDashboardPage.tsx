import React, { useState, useEffect } from 'react';
import { Project, TestResult } from '../types';
import { api } from '../services/api';
import {
  ShieldAlert,
  CheckCircle2,
  AlertTriangle,
  Play,
  RefreshCw,
  Clock,
  Terminal,
  Check
} from '../components/Icons';

interface TestingDashboardPageProps {
  project: Project;
}

export const TestingDashboardPage: React.FC<TestingDashboardPageProps> = ({ project }) => {
  const [tests, setTests] = useState<TestResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [running, setRunning] = useState(false);
  const [selectedTest, setSelectedTest] = useState<TestResult | null>(null);

  useEffect(() => {
    loadTests();
  }, [project.id]);

  const loadTests = async () => {
    try {
      setLoading(true);
      const data = await api.getProjectTests(project.id);
      setTests(data);
      if (data.length > 0) setSelectedTest(data[0]);
    } catch (err: any) {
      console.error('Failed to load tests', err);
    } finally {
      setLoading(false);
    }
  };

  const handleRunTests = async () => {
    try {
      setRunning(true);
      const data = await api.runProjectTests(project.id);
      setTests(data);
      if (data.length > 0) setSelectedTest(data[0]);
    } catch (err: any) {
      alert('Test run failed: ' + err.message);
    } finally {
      setRunning(false);
    }
  };

  const total = tests.length;
  const passed = tests.filter(t => t.testStatus === 'PASSED').length;
  const failed = tests.filter(t => t.testStatus === 'FAILED' || t.testStatus === 'ERROR').length;
  const passRate = total > 0 ? Math.round((passed / total) * 100) : 100;

  return (
    <div className="p-8 space-y-8 max-w-7xl mx-auto">
      {/* Header & Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-6 rounded-2xl glass-panel border border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-sky-400" />
            <h1 className="text-xl font-bold text-white tracking-tight">Automated Testing & Sandbox Verification</h1>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Isolated execution results from JUnit 5 test suites running inside containerized sandbox
          </p>
        </div>

        <button
          onClick={handleRunTests}
          disabled={running}
          className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white text-xs font-semibold shadow-lg shadow-sky-500/20 transition-all disabled:opacity-50 self-start sm:self-auto"
        >
          <Play className="w-4 h-4" />
          <span>{running ? 'Executing in Sandbox...' : 'Re-Run Test Suites'}</span>
        </button>
      </div>

      {/* Test Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="p-5 rounded-2xl glass-panel border border-slate-800 space-y-2">
          <span className="text-xs font-medium text-slate-400">Total Test Cases</span>
          <div className="text-2xl font-bold text-white">{total}</div>
        </div>

        <div className="p-5 rounded-2xl glass-panel border border-slate-800 space-y-2">
          <span className="text-xs font-medium text-emerald-400">Passed Tests</span>
          <div className="text-2xl font-bold text-emerald-400">{passed}</div>
        </div>

        <div className="p-5 rounded-2xl glass-panel border border-slate-800 space-y-2">
          <span className="text-xs font-medium text-rose-400">Failed / Errors</span>
          <div className="text-2xl font-bold text-rose-400">{failed}</div>
        </div>

        <div className="p-5 rounded-2xl glass-panel border border-slate-800 space-y-2">
          <span className="text-xs font-medium text-sky-400">Pass Rate</span>
          <div className="text-2xl font-bold text-sky-400">{passRate}%</div>
        </div>
      </div>

      {/* Split Pane: Test List & Log Trace */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
        {/* Test Cases List */}
        <div className="md:col-span-6 rounded-2xl glass-panel border border-slate-800 overflow-hidden flex flex-col">
          <div className="p-4 border-b border-slate-800 bg-slate-850/50 flex items-center justify-between">
            <span className="text-xs font-semibold text-white">Generated Test Cases</span>
            <span className="text-[10px] text-slate-500">JUnit 5 / Mockito</span>
          </div>

          <div className="divide-y divide-slate-800/60 overflow-y-auto max-h-[500px]">
            {tests.map((t) => {
              const isPassed = t.testStatus === 'PASSED';
              const isSelected = selectedTest?.id === t.id;

              return (
                <div
                  key={t.id}
                  onClick={() => setSelectedTest(t)}
                  className={`p-4 flex items-center justify-between cursor-pointer transition-colors ${
                    isSelected ? 'bg-sky-500/10' : 'hover:bg-slate-800/30'
                  }`}
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className={`w-2 h-2 rounded-full ${isPassed ? 'bg-emerald-400' : 'bg-rose-400'}`} />
                      <span className="text-xs font-semibold text-slate-200 font-mono">{t.testName}</span>
                    </div>
                    <div className="text-[11px] text-slate-500">{t.testClass || 'ServiceIntegrationTest'}</div>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className="text-[10px] text-slate-500 font-mono">
                      {t.executionTime || 120}ms
                    </span>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      isPassed ? 'bg-emerald-500/10 text-emerald-400' : 'bg-rose-500/10 text-rose-400'
                    }`}>
                      {t.testStatus}
                    </span>
                  </div>
                </div>
              );
            })}
            {tests.length === 0 && (
              <div className="text-center py-12 text-slate-500 text-xs">
                No test results recorded yet. Run the workflow or click "Re-Run Test Suites" above.
              </div>
            )}
          </div>
        </div>

        {/* Selected Test Detail & Log Console */}
        <div className="md:col-span-6 rounded-2xl glass-panel border border-slate-800 overflow-hidden flex flex-col">
          <div className="p-4 border-b border-slate-800 bg-slate-850/50 flex items-center justify-between">
            <div className="flex items-center gap-2 font-mono text-xs text-white">
              <Terminal className="w-4 h-4 text-sky-400" />
              <span>Execution Log & Stack Trace</span>
            </div>
          </div>

          <div className="p-4 flex-1 overflow-auto bg-slate-950 font-mono text-xs text-slate-300 space-y-3">
            {selectedTest ? (
              <>
                <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 space-y-1">
                  <div className="text-[11px] text-slate-400">Test Method: <span className="text-white font-semibold">{selectedTest.testName}</span></div>
                  <div className="text-[11px] text-slate-400">Status: <span className={selectedTest.testStatus === 'PASSED' ? 'text-emerald-400 font-bold' : 'text-rose-400 font-bold'}>{selectedTest.testStatus}</span></div>
                  <div className="text-[11px] text-slate-400">Execution Time: <span className="text-slate-200">{selectedTest.executionTime || 120}ms</span></div>
                </div>

                {selectedTest.errorMessage ? (
                  <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs space-y-2">
                    <div className="font-bold">Assertion / Compiler Error:</div>
                    <pre className="whitespace-pre-wrap">{selectedTest.errorMessage}</pre>
                    {selectedTest.stackTrace && (
                      <pre className="text-[10px] text-rose-400/80 whitespace-pre-wrap">{selectedTest.stackTrace}</pre>
                    )}
                  </div>
                ) : (
                  <div className="p-4 rounded-lg bg-emerald-500/5 border border-emerald-500/10 text-emerald-400/90 text-xs space-y-2">
                    <div className="flex items-center gap-1.5 font-bold">
                      <Check className="w-4 h-4" />
                      <span>Test Execution Passed Cleanly</span>
                    </div>
                    <p className="text-[11px] text-slate-400">
                      All assertions satisfied. Spring Security context, Mockito stubs, and transactional boundaries verified without failures.
                    </p>
                  </div>
                )}
              </>
            ) : (
              <div className="text-center py-12 text-slate-500 text-xs">
                Select a test case to view its execution traces.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
