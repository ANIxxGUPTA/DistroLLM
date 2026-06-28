import React, { useEffect, useState } from 'react';
import './TopBar.css';
import { getHealth } from '../api/client';

const TopBar = ({ title }) => {
  const [isOnline, setIsOnline] = useState(false);
  const [uptime, setUptime] = useState(0);

  useEffect(() => {
    // Initial health check
    getHealth()
      .then(data => {
        if (data && data.status === 'UP') {
          setIsOnline(true);
        } else {
          setIsOnline(false);
        }
      })
      .catch(() => setIsOnline(false));

    // Uptime counter
    const timer = setInterval(() => {
      setUptime(prev => prev + 1);
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const formatUptime = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}m ${s}s`;
  };

  return (
    <header className="topbar">
      <h1 className="page-title">{title}</h1>
      <div className="status-area">
        <div className="status-badge">
          <span className={`status-dot ${isOnline ? 'online' : 'offline'}`}></span>
          {isOnline ? 'Server online' : 'Server offline'}
        </div>
        <div className="uptime">
          Uptime: {formatUptime(uptime)}
        </div>
      </div>
    </header>
  );
};

export default TopBar;
