import React, { useState, useEffect } from 'react';
import './Endpoints.css';
import { getEndpoints, deleteEndpoint, registerEndpoint } from '../api/client';
import Toast from '../components/Toast';

const Endpoints = () => {
  const [endpoints, setEndpoints] = useState([]);
  const [lastCheck, setLastCheck] = useState(Date.now());
  const [secondsAgo, setSecondsAgo] = useState(0);
  const [toast, setToast] = useState({ message: '', type: 'success', visible: false });

  const showToast = (message, type) => {
    setToast({ message, type, visible: true });
  };

  useEffect(() => {
    const fetchEndpoints = () => {
      getEndpoints().then(data => {
        const arr = Array.isArray(data) ? data : Object.values(data || {});
        
        // Sort for consistent rendering order (ep-1, ep-2, ep-3)
        arr.sort((a, b) => (a.id || '').localeCompare(b.id || ''));
        
        setEndpoints(arr);
        setLastCheck(Date.now());
        setSecondsAgo(0);
      }).catch(err => console.error("Endpoints poll error:", err));
    };

    fetchEndpoints();
    const interval = setInterval(fetchEndpoints, 3000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const timer = setInterval(() => {
      setSecondsAgo(Math.floor((Date.now() - lastCheck) / 1000));
    }, 1000);
    return () => clearInterval(timer);
  }, [lastCheck]);

  const healthyCount = endpoints.filter(ep => ep.isHealthy).length;
  const totalCount = endpoints.length;
  let healthColor = 'red';
  if (healthyCount === totalCount && totalCount > 0) healthColor = 'green';
  else if (healthyCount > 0) healthColor = 'amber';

  const totalRequests = endpoints.reduce((sum, ep) => sum + (ep.totalRequestsServed || 0), 0);

  const handleTrip = async (id) => {
    try {
      await deleteEndpoint(id);
      showToast(`${id} circuit tripped`, 'warning');
    } catch (e) {
      showToast(`Failed to trip ${id}`, 'error');
    }
  };

  const handleRestoreAll = async () => {
    try {
      const endpointsToRegister = [
        { id: "ep-1", url: "http://localhost:11434", modelName: "llama3.2:1b" },
        { id: "ep-2", url: "http://localhost:11434", modelName: "llama3.2:3b" },
        { id: "ep-3", url: "http://localhost:11434", modelName: "llama3.1:8b" }
      ];
      for (const ep of endpointsToRegister) {
        await registerEndpoint(ep);
      }
      showToast("Restoring all endpoints...", 'success');
    } catch (e) {
      showToast("Failed to restore all", 'error');
    }
  };

  return (
    <div className="endpoints-page">
      <div className="page-header">
        <h2 className="title">Live circuit breaker states</h2>
        
        <div className="summary-bar">
          <div className="stat-chip" style={{ color: `var(--${healthColor === 'green' ? 'success' : healthColor === 'amber' ? 'warning' : 'danger'})` }}>
            {healthyCount} / {Math.max(totalCount, 3)} healthy
          </div>
          <div className="stat-chip">
            Total requests served: <span className="stat-value">{totalRequests}</span>
          </div>
          <div className="stat-chip">
            Last checked: <span className="stat-value">{secondsAgo}s ago</span>
          </div>
        </div>
      </div>

      <div className="cards-grid">
        {endpoints.map(ep => {
          let cbState = ep.circuitState || (ep.isHealthy ? 'CLOSED' : 'OPEN');
          
          let cbClass = '';
          let cbText = '';
          if (cbState === 'CLOSED') {
             cbClass = 'cb-closed';
             cbText = '● CLOSED';
          } else if (cbState === 'OPEN') {
             cbClass = 'cb-open';
             cbText = '◉ OPEN';
          } else {
             cbClass = 'cb-half';
             cbText = '◑ HALF_OPEN';
          }

          let barClass = ep.id === 'ep-1' ? 'bar-simple' : 
                         ep.id === 'ep-2' ? 'bar-medium' : 'bar-complex';
          
          const pct = totalRequests > 0 ? ((ep.totalRequestsServed || 0) / totalRequests * 100) : 0;

          return (
            <div key={ep.id} className="endpoint-card">
              <div className="card-top">
                <div className="ep-info">
                  <div className="ep-id">{ep.id}</div>
                  <div className="ep-model">{ep.modelName}</div>
                </div>
                <div className={`ep-health ${ep.isHealthy ? 'health-ok' : 'health-bad'}`}>
                  {ep.isHealthy ? '● Healthy' : '● Unhealthy'}
                </div>
              </div>

              <div className={`cb-state-block ${cbClass}`}>
                {cbText}
              </div>

              <div className="stats-grid">
                <div className="stat-cell">
                  <div className="cell-label">Requests served</div>
                  <div className="cell-value">{ep.totalRequestsServed || 0}</div>
                </div>
                <div className="stat-cell">
                  <div className="cell-label">Failure count</div>
                  <div className="cell-value">{ep.failureCount || 0}</div>
                </div>
                <div className="stat-cell">
                  <div className="cell-label">URL</div>
                  <div className="cell-value">{ep.url}</div>
                </div>
                <div className="stat-cell">
                  <div className="cell-label">Model</div>
                  <div className="cell-value">{ep.modelName}</div>
                </div>
              </div>

              <div className="progress-track">
                <div className={`progress-fill ${barClass}`} style={{ width: `${pct}%` }}></div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="fault-panel">
        <div className="fault-header">
          <span className="fault-icon">⚠</span>
          <div className="fault-titles">
            <h3>Fault Injection</h3>
            <p>Trigger circuit breaker states to test fault tolerance</p>
          </div>
        </div>
        <div className="fault-actions">
          <button className="btn-fault" onClick={() => handleTrip('ep-1')}>Trip ep-1 circuit</button>
          <button className="btn-fault" onClick={() => handleTrip('ep-2')}>Trip ep-2 circuit</button>
          <button className="btn-fault" onClick={handleRestoreAll}>Restore all</button>
        </div>
      </div>

      <Toast 
        message={toast.message} 
        type={toast.type} 
        visible={toast.visible} 
        onHide={() => setToast({ ...toast, visible: false })} 
      />
    </div>
  );
};

export default Endpoints;
