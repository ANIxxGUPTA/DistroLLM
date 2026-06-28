import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import './RequestChart.css';

const RequestChart = ({ data }) => {
  return (
    <div className="request-chart-container">
      <ResponsiveContainer width="100%" height={220}>
        <LineChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
          <XAxis 
            dataKey="time" 
            stroke="var(--text-muted)" 
            fontSize={12} 
            tickMargin={10} 
            tickFormatter={(val, i) => {
              if (i === data.length - 1) return "now";
              if (i === Math.max(0, data.length - 6)) return "-10s";
              if (i === Math.max(0, data.length - 11)) return "-20s";
              if (i === Math.max(0, data.length - 16)) return "-30s";
              return "";
            }}
          />
          <YAxis stroke="var(--text-muted)" fontSize={12} width={40} />
          <Tooltip 
            contentStyle={{ backgroundColor: 'var(--surface)', borderColor: 'var(--border)', borderRadius: 'var(--radius)' }} 
            itemStyle={{ color: 'var(--accent)' }} 
          />
          <Line type="monotone" dataKey="rps" stroke="var(--accent)" strokeWidth={2} dot={false} isAnimationActive={false} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default RequestChart;
