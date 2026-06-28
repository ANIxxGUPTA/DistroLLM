import React from 'react';
import './ComplexityBreakdown.css';

const ComplexityBreakdown = ({ metrics }) => {
  const simple = metrics["routing.simple"] || metrics["SIMPLE"] || 0;
  const medium = metrics["routing.medium"] || metrics["MEDIUM"] || 0;
  const complex = metrics["routing.complex"] || metrics["COMPLEX"] || 0;
  
  const total = simple + medium + complex;

  const getPct = (val) => total > 0 ? (val / total * 100) : 0;

  return (
    <div className="complexity-container">
      <div className="chart-title">Query Distribution</div>
      <div className="bars-wrapper">
        
        <div className="bar-row">
          <div className="bar-label">SIMPLE</div>
          <div className="bar-track">
            <div className="bar-fill fill-simple" style={{ width: `${getPct(simple)}%` }}></div>
          </div>
          <div className="bar-value">{simple}</div>
        </div>

        <div className="bar-row">
          <div className="bar-label">MEDIUM</div>
          <div className="bar-track">
            <div className="bar-fill fill-medium" style={{ width: `${getPct(medium)}%` }}></div>
          </div>
          <div className="bar-value">{medium}</div>
        </div>

        <div className="bar-row">
          <div className="bar-label">COMPLEX</div>
          <div className="bar-track">
            <div className="bar-fill fill-complex" style={{ width: `${getPct(complex)}%` }}></div>
          </div>
          <div className="bar-value">{complex}</div>
        </div>

      </div>
    </div>
  );
};

export default ComplexityBreakdown;
