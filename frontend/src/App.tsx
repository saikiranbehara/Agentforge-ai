import React, { useState, useEffect } from 'react';
import { Project, DashboardStats, AiConfig, User } from './types';
import { api } from './services/api';

// Components
import { Navbar } from './components/Navbar';
import { Sidebar, NavTab } from './components/Sidebar';
import { HumanApprovalBanner } from './components/HumanApprovalBanner';
import { AiConfigModal } from './components/AiConfigModal';
import { AuthModal } from './components/AuthModal';

// Pages
import { LandingPage } from './pages/LandingPage';
import { DashboardPage } from './pages/DashboardPage';
import { CreateProjectPage } from './pages/CreateProjectPage';
import { AgentWorkspacePage } from './pages/AgentWorkspacePage';
import { ArchitectureViewPage } from './pages/ArchitectureViewPage';
import { CodeGenerationPage } from './pages/CodeGenerationPage';
import { TestingDashboardPage } from './pages/TestingDashboardPage';
import { CodeReviewDashboardPage } from './pages/CodeReviewDashboardPage';
import { ProjectDocumentationPage } from './pages/ProjectDocumentationPage';

export function App() {
  const [activeTab, setActiveTab] = useState<NavTab>('dashboard');
  const [user, setUser] = useState<User | null>({
    id: 1,
    name: 'Alex Rivers',
    email: 'demo@agentforge.ai',
    role: 'ROLE_USER'
  });
  const [projects, setProjects] = useState<Project[]>([]);
  const [currentProject, setCurrentProject] = useState<Project | null>(null);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [aiConfig, setAiConfig] = useState<AiConfig | null>(null);

  // Modals
  const [showAiModal, setShowAiModal] = useState(false);
  const [showAuthModal, setShowAuthModal] = useState(false);

  useEffect(() => {
    // Check local storage auth
    const stored = api.getStoredUser();
    if (stored) {
      setUser({
        id: stored.id,
        name: stored.name,
        email: stored.email,
        role: stored.role
      });
    }

    loadInitialData();
  }, []);

  const loadInitialData = async () => {
    try {
      const [statsData, projectsData, configData] = await Promise.allSettled([
        api.getDashboardStats(),
        api.getProjects(),
        api.getAiConfig()
      ]);

      if (statsData.status === 'fulfilled') setStats(statsData.value);
      if (configData.status === 'fulfilled') setAiConfig(configData.value);

      if (projectsData.status === 'fulfilled') {
        setProjects(projectsData.value);
        if (projectsData.value.length > 0 && !currentProject) {
          setCurrentProject(projectsData.value[0]);
        }
      }
    } catch (err) {
      console.warn('Could not load initial API data', err);
    }
  };

  const handleSelectProject = (project: Project) => {
    setCurrentProject(project);
  };

  const handleProjectCreated = (newProject: Project) => {
    setProjects([newProject, ...projects]);
    setCurrentProject(newProject);
    setActiveTab('workspace');
    loadInitialData();
  };

  const handleProjectUpdated = (updated: Project) => {
    setCurrentProject(updated);
    setProjects(projects.map(p => p.id === updated.id ? updated : p));
  };

  const handleExecuteProject = async (projectId: number) => {
    try {
      const updated = await api.executeProject(projectId);
      handleProjectUpdated(updated);
      const found = projects.find(p => p.id === projectId) || updated;
      setCurrentProject(found);
      setActiveTab('workspace');
    } catch (err: any) {
      alert('Execution failed: ' + err.message);
    }
  };

  const handleDownloadProject = (projectId: number) => {
    window.location.href = api.getDownloadUrl(projectId);
  };

  const handleLogout = () => {
    api.logout();
    setUser(null);
    setShowAuthModal(true);
  };

  return (
    <div className="min-h-screen bg-navy-950 text-slate-100 flex flex-col font-sans">
      {/* Top Navigation */}
      <Navbar
        currentProject={currentProject}
        projects={projects}
        onSelectProject={handleSelectProject}
        aiConfig={aiConfig}
        onOpenAiSettings={() => setShowAiModal(true)}
        onNewProject={() => setActiveTab('create')}
        user={user}
        onLogout={handleLogout}
      />

      {/* Human Approval Alert Banner */}
      {currentProject && (
        <HumanApprovalBanner
          project={currentProject}
          onUpdated={handleProjectUpdated}
          onViewDetails={() => {
            if (currentProject.status === 'PENDING_ARCHITECTURE_APPROVAL') {
              setActiveTab('architecture');
            } else {
              setActiveTab('workspace');
            }
          }}
        />
      )}

      {/* Landing Page View (full width without sidebar) */}
      {activeTab === 'landing' ? (
        <LandingPage
          onGetStarted={() => setActiveTab(projects.length > 0 ? 'workspace' : 'create')}
          onExploreFeatures={() => {
            const el = document.getElementById('features');
            if (el) el.scrollIntoView({ behavior: 'smooth' });
          }}
        />
      ) : (
        /* Workspace Main Body with Sidebar */
        <div className="flex flex-1 overflow-hidden">
          <Sidebar
            activeTab={activeTab}
            onTabChange={(tab) => {
              if (tab === 'settings') {
                setShowAiModal(true);
              } else {
                setActiveTab(tab);
              }
            }}
            hasActiveProject={!!currentProject}
          />

          <main className="flex-1 overflow-y-auto">
            {activeTab === 'dashboard' && (
              <DashboardPage
                stats={stats}
                projects={projects}
                onSelectProject={(p) => {
                  setCurrentProject(p);
                  setActiveTab('workspace');
                }}
                onNewProject={() => setActiveTab('create')}
                onExecuteProject={handleExecuteProject}
                onDownloadProject={handleDownloadProject}
              />
            )}

            {activeTab === 'create' && (
              <CreateProjectPage
                onProjectCreated={handleProjectCreated}
                onCancel={() => setActiveTab(currentProject ? 'workspace' : 'dashboard')}
              />
            )}

            {activeTab === 'workspace' && currentProject && (
              <AgentWorkspacePage
                project={currentProject}
                onProjectUpdated={handleProjectUpdated}
                onNavigateToTab={setActiveTab}
              />
            )}

            {activeTab === 'architecture' && currentProject && (
              <ArchitectureViewPage
                project={currentProject}
                onProjectUpdated={handleProjectUpdated}
              />
            )}

            {activeTab === 'code' && currentProject && (
              <CodeGenerationPage project={currentProject} />
            )}

            {activeTab === 'testing' && currentProject && (
              <TestingDashboardPage project={currentProject} />
            )}

            {activeTab === 'review' && currentProject && (
              <CodeReviewDashboardPage project={currentProject} />
            )}

            {activeTab === 'docs' && currentProject && (
              <ProjectDocumentationPage project={currentProject} />
            )}
          </main>
        </div>
      )}

      {/* AI Config Modal */}
      <AiConfigModal
        config={aiConfig}
        isOpen={showAiModal}
        onClose={() => setShowAiModal(false)}
        onConfigUpdated={(newCfg) => setAiConfig(newCfg)}
      />

      {/* Auth Modal */}
      <AuthModal
        isOpen={showAuthModal}
        onClose={() => setShowAuthModal(false)}
        onLoginSuccess={(u) => {
          setUser(u);
          loadInitialData();
        }}
      />
    </div>
  );
}

export default App;
