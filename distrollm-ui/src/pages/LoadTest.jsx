import React, { useState, useEffect, useRef } from 'react';
import './LoadTest.css';
import Spinner from '../components/Spinner';
import RequestChart from '../components/RequestChart';
import { getMetrics, routeQuery } from '../api/client';

const LoadTest = () => {
  const [users, setUsers] = useState(10);
  const [duration, setDuration] = useState(30);
  const [mix, setMix] = useState({ simple: 50, medium: 30, complex: 20 });
  const [error, setError] = useState('');
  
  const [running, setRunning] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [elapsed, setElapsed] = useState(0);
  
  const [liveMetrics, setLiveMetrics] = useState({});
  const [baseline, setBaseline] = useState(0);
  const [history, setHistory] = useState([]);
  
  const [finalStats, setFinalStats] = useState(null);
  const lastTotalRef = useRef(0);
  const testRef = useRef(null);

  const handleMixChange = (key, val) => {
    setMix(prev => ({ ...prev, [key]: parseInt(val) || 0 }));
  };

  const startTest = async () => {
    const totalMix = mix.simple + mix.medium + mix.complex;
    if (totalMix !== 100) {
      setError(`Prompt mix must sum to 100% (currently ${totalMix}%)`);
      return;
    }
    setError('');
    
    try {
      const data = await getMetrics();
      const currentBaseline = data["requests.total"] || 0;
      setBaseline(currentBaseline);
      lastTotalRef.current = currentBaseline;
      setLiveMetrics(data);
    } catch(e) {
       console.error("Baseline fetch failed", e);
    }

    setRunning(true);
    setCompleted(false);
    setElapsed(0);
    setHistory([]);
    setFinalStats(null);
    
    const SIMPLE_PROMPTS = ["Hello", "What is Java?", "Hi", "2+2?"];
    const MEDIUM_PROMPTS = ["Explain TCP vs UDP", "How does JVM work?"];
    const COMPLEX_PROMPTS = ["Design a distributed cache system with consistency guarantees", "Compare architecture of Kubernetes and Docker Swarm"];
    
    const getRandomPrompt = () => {
      const r = Math.random() * 100;
      if (r < mix.simple) return SIMPLE_PROMPTS[Math.floor(Math.random() * SIMPLE_PROMPTS.length)];
      if (r < mix.simple + mix.medium) return MEDIUM_PROMPTS[Math.floor(Math.random() * MEDIUM_PROMPTS.length)];
      return COMPLEX_PROMPTS[Math.floor(Math.random() * COMPLEX_PROMPTS.length)];
    };

    let timePassed = 0;
    
    const pollInterval = setInterval(() => {
      timePassed += 1;
      setElapsed(timePassed);
      
      getMetrics().then(data => {
        setLiveMetrics(data);
        const currentTotal = data["requests.total"] || 0;
        const rps = Math.max(0, currentTotal - lastTotalRef.current);
        lastTotalRef.current = currentTotal;
        
        setHistory(prev => {
          const snapshot = {
            time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
            rps
          };
          const next = [...prev, snapshot];
          return next.length > 60 ? next.slice(-60) : next;
        });
        
        if (timePassed >= duration) {
          clearInterval(pollInterval);
          clearInterval(testRef.current);
          setRunning(false);
          setCompleted(true);
          
          let p95 = 0;
          for (const key in data) {
            if (key.endsWith(".p95")) p95 = Math.max(p95, data[key]);
          }
          const totalSent = (data["requests.total"] || 0) - baseline;
          const successRate = totalSent > 0 ? (((data["requests.success"] || 0) / (data["requests.total"] || 1)) * 100).toFixed(1) : 0;
          
          setFinalStats({
            total: totalSent,
            successRate,
            p95,
            peakRps: Math.max(...history.map(h => h.rps), rps)
          });
        }
      }).catch(console.error);
    }, 1000);
    
    testRef.current = setInterval(() => {
       if (!running && timePassed >= duration) return;
       const batchSize = Math.max(1, Math.floor(users / 5));
       for (let i = 0; i < batchSize; i++) {
          routeQuery(getRandomPrompt()).catch(() => {});
       }
    }, 200);
  };
  
  useEffect(() => {
    return () => {
      if (testRef.current) clearInterval(testRef.current);
    };
  }, []);

  const totalSent = Math.max(0, (liveMetrics["requests.total"] || 0) - baseline);
  const sr = totalSent > 0 ? (((liveMetrics["requests.success"] || 0) / (liveMetrics["requests.total"] || 1)) * 100).toFixed(1) : 0;
  
  let p50 = 0;
  for (const key in liveMetrics) {
    if (key.endsWith(".p50")) p50 = Math.max(p50, liveMetrics[key]);
  }

  const copyResults = () => {
    if(!finalStats) return;
    const txt = `DistroLLM Load Test Results\nTotal Requests: ${finalStats.total}\nSuccess Rate: ${finalStats.successRate}%\nP95 Latency: ${finalStats.p95.toFixed(1)}ms\nPeak Req/s: ${finalStats.peakRps}`;
    navigator.clipboard.writeText(txt);
  };

  return (
    <div className="loadtest-page">
      <div className="panel panel-controls">
        <div className="lt-header">
          <h2>⚡ Load Test</h2>
          <p>Simulate concurrent traffic</p>
        </div>

        <div className="form-group">
          <label>Concurrent Users: {users}</label>
          <input type="range" min="1" max="200" value={users} onChange={e => setUsers(parseInt(e.target.value))} />
          <input type="number" min="1" max="200" value={users} onChange={e => setUsers(parseInt(e.target.value))} className="num-input"/>
        </div>

        <div className="form-group">
          <label>Duration (seconds)</label>
          <input type="number" min="10" max="300" value={duration} onChange={e => setDuration(parseInt(e.target.value))} className="num-input"/>
        </div>

        <div className="form-group">
          <label>Prompt Mix (%)</label>
          <div className="mix-inputs">
            <div className="mix-col">
              <span className="mix-label simple-lbl">SIMPLE</span>
              <input type="number" value={mix.simple} onChange={e => handleMixChange('simple', e.target.value)} className="mix-input" />
            </div>
            <div className="mix-col">
              <span className="mix-label medium-lbl">MEDIUM</span>
              <input type="number" value={mix.medium} onChange={e => handleMixChange('medium', e.target.value)} className="mix-input" />
            </div>
            <div className="mix-col">
              <span className="mix-label complex-lbl">COMPLEX</span>
              <input type="number" value={mix.complex} onChange={e => handleMixChange('complex', e.target.value)} className="mix-input" />
            </div>
          </div>
          {error && <div className="lt-error">{error}</div>}
        </div>

        <button className="btn-start" onClick={startTest} disabled={running}>
          {running ? <><Spinner size={16}/> Running...</> : '▶ Start Load Test'}
        </button>
        <p className="lt-note">Runs directly against localhost:7070 from your browser</p>
      </div>

      <div className="panel panel-results">
        <h2 className="panel-title">Live Results</h2>
        
        {!running && !completed && (
          <div className="lt-placeholder">Configure and start a load test</div>
        )}

        {running && (
          <div className="lt-live">
            <div className="progress-bar-container">
              <div className="progress-bar-fill" style={{ width: `${(elapsed/duration)*100}%` }}></div>
            </div>
            <div className="progress-text">{elapsed} / {duration} seconds</div>

            <div className="lt-stats-grid">
              <div className="lt-stat">
                <span className="lt-lbl">Requests Sent</span>
                <span className="lt-val">{totalSent}</span>
              </div>
              <div className="lt-stat">
                <span className="lt-lbl">Success Rate</span>
                <span className="lt-val">{sr}%</span>
              </div>
              <div className="lt-stat">
                <span className="lt-lbl">Avg Latency</span>
                <span className="lt-val">{p50.toFixed(1)}ms</span>
              </div>
              <div className="lt-stat">
                <span className="lt-lbl">Req/s</span>
                <span className="lt-val">{history.length > 0 ? history[history.length-1].rps : 0}</span>
              </div>
            </div>

            <div className="mini-chart">
              <RequestChart data={history} />
            </div>
          </div>
        )}

        {completed && finalStats && (
          <div className="lt-completed">
            <div className="banner-success">✓ Test Complete</div>
            <div className="final-summary">
              <div className="fs-row"><span>Total Requests</span> <span>{finalStats.total}</span></div>
              <div className="fs-row"><span>Success Rate</span> <span>{finalStats.successRate}%</span></div>
              <div className="fs-row"><span>P95 Latency</span> <span>{finalStats.p95.toFixed(1)}ms</span></div>
              <div className="fs-row"><span>Peak Req/s</span> <span>{finalStats.peakRps}</span></div>
            </div>
            <button className="btn-copy-res" onClick={copyResults}>↓ Copy Results</button>
          </div>
        )}
      </div>
    </div>
  );
};

export default LoadTest;
