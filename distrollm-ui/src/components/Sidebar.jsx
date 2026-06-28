import React from 'react';
import './Sidebar.css';

const Sidebar = ({ activePage, setActivePage }) => {
  return (
    <aside className="sidebar">
      <div className="logo">
        <span className="icon">⚡</span> DistroLLM
      </div>
      <nav className="nav-menu">
        <div 
          className={`nav-item ${activePage === 'dashboard' ? 'active' : ''}`}
          onClick={() => setActivePage('dashboard')}
        >
          Dashboard
        </div>
        <div 
          className={`nav-item ${activePage === 'router' ? 'active' : ''}`}
          onClick={() => setActivePage('router')}
        >
          Query Router
        </div>
        <div 
          className={`nav-item ${activePage === 'endpoints' ? 'active' : ''}`}
          onClick={() => setActivePage('endpoints')}
        >
          Endpoints
        </div>
        <div 
          className={`nav-item ${activePage === 'metrics' ? 'active' : ''}`}
          onClick={() => setActivePage('metrics')}
        >
          Metrics
        </div>
        <div 
          className={`nav-item ${activePage === 'loadtest' ? 'active' : ''}`}
          onClick={() => setActivePage('loadtest')}
        >
          Load Test
        </div>
      </nav>
      <div className="version-badge">v1.0</div>
    </aside>
  );
};

export default Sidebar;
