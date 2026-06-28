export const getHealth = () => fetch('http://localhost:7070/health').then(r => r.json());
export const getMetrics = () => fetch('http://localhost:7070/metrics/json').then(r => r.json());
export const getMetricsText = () => fetch('http://localhost:7070/metrics').then(r => r.text());
export const getEndpoints = () => fetch('http://localhost:7070/endpoints').then(r => r.json());

export const routeQuery = (prompt) => fetch('http://localhost:7070/route', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ prompt })
}).then(r => {
  if (!r.ok) throw new Error("Request failed");
  return r.json();
});

export const classifyQuery = (prompt) => fetch(`http://localhost:7070/classify?prompt=${encodeURIComponent(prompt)}`).then(r => {
  if (!r.ok) throw new Error("Request failed");
  return r.json();
});

export const deleteEndpoint = (id) => fetch(`http://localhost:7070/endpoints/${id}`, { method: 'DELETE' }).then(r=>r.text());
export const registerEndpoint = (data) => fetch('http://localhost:7070/endpoints', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(data)
}).then(r=>r.text());
