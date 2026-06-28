import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import './LatencyChart.css';

const LatencyChart = ({ data }) => {
  return (
    <div className="latency-chart-container">
      <ResponsiveContainer width="100%" height={200}>
        <LineChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
          <XAxis dataKey="time" stroke="var(--text-muted)" fontSize={12} tick={false} />
          <YAxis stroke="var(--text-muted)" fontSize={12} width={40} label={{ value: 'ms', angle: -90, position: 'insideLeft', fill: 'var(--text-muted)' }} />
          <Tooltip 
            contentStyle={{ backgroundColor: 'var(--surface)', borderColor: 'var(--border)', borderRadius: 'var(--radius)' }} 
          />
          <Legend wrapperStyle={{ fontSize: '12px', color: 'var(--text-muted)' }} />
          <Line type="monotone" dataKey="p50" name="P50" stroke="#22c55e" strokeWidth={2} dot={false} isAnimationActive={false} />
          <Line type="monotone" dataKey="p95" name="P95" stroke="#f59e0b" strokeWidth={2} dot={false} isAnimationActive={false} />
          <Line type="monotone" dataKey="p99" name="P99" stroke="#ef4444" strokeWidth={2} dot={false} isAnimationActive={false} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default LatencyChart;
