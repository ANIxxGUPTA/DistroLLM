import React, { useState } from 'react';
import './QueryRouter.css';
import Spinner from '../components/Spinner';
import { routeQuery, classifyQuery } from '../api/client';

const QueryRouter = () => {
  const [prompt, setPrompt] = useState('');
  const [history, setHistory] = useState([]);
  const [loadingType, setLoadingType] = useState(null); // 'route' | 'classify' | null
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleAction = async (type) => {
    if (!prompt.trim()) return;
    
    setLoadingType(type);
    setResult(null);
    setError(null);
    
    // Add to history list, removing duplicates
    setHistory(prev => {
      const newHist = [prompt, ...prev.filter(p => p !== prompt)].slice(0, 5);
      return newHist;
    });

    try {
      if (type === 'route') {
        const routeData = await routeQuery(prompt);
        
        // Also fetch classification data so we can display score and reasons cleanly
        let classifyData = null;
        try {
           classifyData = await classifyQuery(prompt);
        } catch (e) {
           console.error("Failed to fetch classify data for route details", e);
        }
        
        let modelStr = 'unknown';
        if (routeData.modelEndpoint === 'ep-1') modelStr = 'llama3.2:1b';
        else if (routeData.modelEndpoint === 'ep-2') modelStr = 'llama3.2:3b';
        else if (routeData.modelEndpoint === 'ep-3') modelStr = 'llama3.1:8b';

        setResult({
          type: 'route',
          complexity: routeData.complexity || (classifyData ? classifyData.complexity : 'UNKNOWN'),
          endpoint: routeData.modelEndpoint || 'unknown',
          model: modelStr,
          latency: routeData.latencyMs,
          response: routeData.response,
          score: classifyData ? classifyData.score : null,
          reasons: classifyData ? classifyData.reasons.join(', ') : 'N/A'
        });
      } else {
        const data = await classifyQuery(prompt);
        setResult({
          type: 'classify',
          complexity: data.complexity,
          score: data.score,
          reasons: data.reasons.join(', ')
        });
      }
    } catch (err) {
      setError("Backend is unreachable or returned an error.");
    } finally {
      setLoadingType(null);
    }
  };

  const badgeClass = result?.complexity === 'SIMPLE' ? 'badge-simple' : 
                     result?.complexity === 'MEDIUM' ? 'badge-medium' : 
                     result?.complexity === 'COMPLEX' ? 'badge-complex' : '';

  return (
    <div className="query-router-container">
      <div className="panel panel-left">
        <h2 className="panel-title">Send a Query</h2>
        <textarea
          className="prompt-input"
          placeholder="Type your prompt here — try short for SIMPLE, or detailed with multiple questions for COMPLEX..."
          value={prompt}
          onChange={e => setPrompt(e.target.value)}
        />
        
        <div className="button-group">
          <button 
            className="btn btn-outline"
            disabled={loadingType !== null}
            onClick={() => handleAction('classify')}
          >
            {loadingType === 'classify' && <Spinner size={16} />}
            Classify only
          </button>
          
          <button 
            className="btn btn-filled"
            disabled={loadingType !== null}
            onClick={() => handleAction('route')}
          >
            {loadingType === 'route' && <Spinner size={16} />}
            Route Query
          </button>
        </div>

        {history.length > 0 && (
          <div className="history-section">
            <div className="history-label">Recent queries:</div>
            <div className="history-chips">
              {history.map((h, i) => (
                <div key={i} className="chip" onClick={() => setPrompt(h)}>
                  {h.length > 30 ? h.substring(0, 30) + '...' : h}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="panel panel-right">
        <h2 className="panel-title">Routing Result</h2>
        
        {!result && !error && (
          <div className="placeholder-text">Send a query to see routing details</div>
        )}
        
        {error && (
          <div className="error-text">{error}</div>
        )}

        {result && (
          <div className="result-content">
            <div className="badge-container">
              <span className={`complexity-badge ${badgeClass}`}>
                {result.complexity}
              </span>
            </div>

            <div className="details-card">
              {result.type === 'route' && (
                <>
                  <div className="details-row">
                    <span className="details-label">Endpoint</span>
                    <span className="details-value">{result.endpoint}</span>
                  </div>
                  <div className="details-divider"></div>
                  <div className="details-row">
                    <span className="details-label">Model</span>
                    <span className="details-value">{result.model}</span>
                  </div>
                  <div className="details-divider"></div>
                  <div className="details-row">
                    <span className="details-label">Latency</span>
                    <span className="details-value">{result.latency}ms</span>
                  </div>
                  <div className="details-divider"></div>
                </>
              )}
              
              {result.score !== null && result.score !== undefined && (
                <>
                  <div className="details-row">
                    <span className="details-label">Score</span>
                    <span className="details-value">{result.score} pts</span>
                  </div>
                  <div className="details-divider"></div>
                </>
              )}
              
              <div className="details-row">
                <span className="details-label">Reasons</span>
                <span className="details-value">{result.reasons}</span>
              </div>
            </div>

            {result.type === 'route' && (
              <div className="response-container">
                <div className="response-label">Model Response</div>
                <div className="response-box">
                  {result.response}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default QueryRouter;
