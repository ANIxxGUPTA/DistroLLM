import React, { useState } from 'react';
import './App.css';
import Sidebar from './components/Sidebar';
import TopBar from './components/TopBar';
import Dashboard from './pages/Dashboard';
import QueryRouter from './pages/QueryRouter';
import Endpoints from './pages/Endpoints';
import Metrics from './pages/Metrics';
import LoadTest from './pages/LoadTest';

function App() {
  const [activePage, setActivePage] = useState('dashboard');

  const pageTitle = activePage === 'dashboard' ? 'Dashboard' : 
                    activePage === 'router' ? 'Query Router' : 
                    activePage === 'endpoints' ? 'Endpoints' :
                    activePage === 'metrics' ? 'Metrics' :
                    activePage === 'loadtest' ? 'Load Test' :
                    activePage.charAt(0).toUpperCase() + activePage.slice(1);

  return (
    <div className="app-container">
      <Sidebar activePage={activePage} setActivePage={setActivePage} />
      <div className="main-content">
        <TopBar title={pageTitle} />
        <div className="page-content">
          {activePage === 'dashboard' && <Dashboard />}
          {activePage === 'router' && <QueryRouter />}
          {activePage === 'endpoints' && <Endpoints />}
          {activePage === 'metrics' && <Metrics />}
          {activePage === 'loadtest' && <LoadTest />}
        </div>
      </div>
    </div>
  );
}

export default App;
