import React, { useState, useEffect, useRef } from 'react';
import './Dashboard.css';
import { getMetrics } from '../api/client';
import MetricCard from '../components/MetricCard';
import RequestChart from '../components/RequestChart';
import LatencyChart from '../components/LatencyChart';
import ComplexityBreakdown from '../components/ComplexityBreakdown';

const Dashboard = () => {
  const [metrics, setMetrics] = useState({});
  const [history, setHistory] = useState([]);
  const lastReqRef = useRef(0);

  useEffect(() => {
    const pollMetrics = () => {
      getMetrics().then(data => {
        setMetrics(data);
        
        const currentTotal = data["requests.total"] || 0;
        const reqPerSec = Math.max(0, (currentTotal - lastReqRef.current) / 2);
        lastReqRef.current = currentTotal;

        let p50 = 0, p95 = 0, p99 = 0;
        for (const key in data) {
          if (key.endsWith(".p50")) p50 = Math.max(p50, data[key]);
          if (key.endsWith(".p95")) p95 = Math.max(p95, data[key]);
          if (key.endsWith(".p99")) p99 = Math.max(p99, data[key]);
        }

        setHistory(prev => {
          const snapshot = {
            time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
            rps: reqPerSec,
            p50, p95, p99
          };
          const next = [...prev, snapshot];
          return next.length > 30 ? next.slice(-30) : next;
        });
      }).catch(console.error);
    };

    pollMetrics();
    const interval = setInterval(pollMetrics, 2000);
    return () => clearInterval(interval);
  }, []);

  const total = metrics["requests.total"] || 0;
  const failed = metrics["requests.failed"] || 0;
  const successRate = total > 0 ? ((total - failed) / total * 100).toFixed(1) + "%" : "0.0%";

  return (
    <div className="dashboard-grid">
      <div className="metrics-row">
        <MetricCard label="Total Requests" value={total} />
        <MetricCard label="Success Rate" value={successRate} />
        <MetricCard label="Active Threads" value={metrics["active.threads"] || 0} />
        <MetricCard label="Queue Depth" value={metrics["queue.depth"] || 0} />
      </div>

      <div className="middle-row">
        <div className="chart-card">
          <div className="chart-title">Requests per Second</div>
          <RequestChart data={history} />
        </div>
        <div className="chart-card">
          <ComplexityBreakdown metrics={metrics} />
        </div>
      </div>

      <div className="chart-card">
        <div className="chart-title">System Latency</div>
        <LatencyChart data={history} />
      </div>
    </div>
  );
};

export default Dashboard;
