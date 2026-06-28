import React, { useState, useEffect, useRef } from 'react';
import './Metrics.css';
import { getMetrics, getMetricsText } from '../api/client';
import MetricCard from '../components/MetricCard';
import LatencyChart from '../components/LatencyChart';
import RequestChart from '../components/RequestChart';

const Metrics = () => {
  const [metrics, setMetrics] = useState({});
  const [history, setHistory] = useState([]);
  const [rawText, setRawText] = useState('');
  const [copied, setCopied] = useState(false);
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
          return next.length > 60 ? next.slice(-60) : next;
        });
      }).catch(console.error);
      
      getMetricsText().then(text => setRawText(text)).catch(() => setRawText('Failed to load raw metrics'));
    };

    pollMetrics();
    const interval = setInterval(pollMetrics, 2000);
    return () => clearInterval(interval);
  }, []);

  const handleCopy = () => {
    navigator.clipboard.writeText("https://web-production-82857.up.railway.app/metrics").then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  };

  let maxP50 = 0, maxP95 = 0, maxP99 = 0;
  for (const key in metrics) {
    if (key.endsWith(".p50")) maxP50 = Math.max(maxP50, metrics[key]);
    if (key.endsWith(".p95")) maxP95 = Math.max(maxP95, metrics[key]);
    if (key.endsWith(".p99")) maxP99 = Math.max(maxP99, metrics[key]);
  }

  return (
    <div className="metrics-page">
      <div className="page-header">
        <h2 className="title">Metrics</h2>
        <p className="subtitle">Live telemetry — updates every 2s</p>
      </div>

      <div className="metrics-row">
        <MetricCard label="P50 Latency" value={maxP50.toFixed(1)} unit="ms" />
        <MetricCard label="P95 Latency" value={maxP95.toFixed(1)} unit="ms" />
        <MetricCard label="P99 Latency" value={maxP99.toFixed(1)} unit="ms" />
        <MetricCard label="Total Requests" value={metrics["requests.total"] || 0} />
      </div>

      <div className="chart-card full-width">
        <div className="chart-title">Latency Distribution (P50 / P95 / P99)</div>
        <LatencyChart data={history} />
      </div>

      <div className="middle-row">
        <div className="chart-card">
          <div className="chart-title">Request Throughput</div>
          <RequestChart data={history} />
        </div>
        
        <div className="chart-card">
          <div className="chart-title">System Counters</div>
          <div className="counters-table">
            <div className="counter-row">
              <span className="c-label">Total Requests</span>
              <span className="c-val">{metrics["requests.total"] || 0}</span>
            </div>
            <div className="counter-row alt">
              <span className="c-label">Successful</span>
              <span className="c-val">{metrics["requests.success"] || 0}</span>
            </div>
            <div className="counter-row">
              <span className="c-label">Failed</span>
              <span className="c-val">{metrics["requests.failed"] || 0}</span>
            </div>
            <div className="counter-row alt">
              <span className="c-label">Circuit Opens</span>
              <span className="c-val">{metrics["circuit.open.count"] || 0}</span>
            </div>
            <div className="counter-row">
              <span className="c-label">Retries</span>
              <span className="c-val">{metrics["retry.count"] || 0}</span>
            </div>
            <div className="counter-row alt">
              <span className="c-label">Active Threads</span>
              <span className="c-val">{metrics["active.threads"] || 0}</span>
            </div>
            <div className="counter-row">
              <span className="c-label">Queue Depth</span>
              <span className="c-val">{metrics["queue.depth"] || 0}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="chart-card prom-box">
        <div className="prom-header">
          <div className="prom-title">Prometheus Endpoint</div>
          <button className="btn-copy" onClick={handleCopy}>{copied ? "Copied!" : "Copy URL"}</button>
        </div>
        <div className="prom-details">
          <span>Endpoint: <a href="https://web-production-82857.up.railway.app/metrics" target="_blank" rel="noreferrer">https://web-production-82857.up.railway.app/metrics</a></span>
          <span>Format: text/plain (Prometheus 0.0.4)</span>
        </div>
        <div className="prom-raw">
          {rawText.length > 800 ? rawText.substring(0, 800) + '...' : rawText}
        </div>
      </div>
    </div>
  );
};

export default Metrics;
