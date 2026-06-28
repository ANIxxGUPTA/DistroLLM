export const getHealth = () => fetch('https://web-production-82857.up.railway.app/health').then(r => r.json());
export const getMetrics = () => fetch('https://web-production-82857.up.railway.app/metrics/json').then(r => r.json());
export const getMetricsText = () => fetch('https://web-production-82857.up.railway.app/metrics').then(r => r.text());
export const getEndpoints = () => fetch('https://web-production-82857.up.railway.app/endpoints').then(r => r.json());

export const routeQuery = (prompt) => fetch('https://web-production-82857.up.railway.app/route', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ prompt })
}).then(r => {
  if (!r.ok) throw new Error("Request failed");
  return r.json();
});

export const classifyQuery = (prompt) => fetch(`https://web-production-82857.up.railway.app/classify?prompt=${encodeURIComponent(prompt)}`).then(r => {
  if (!r.ok) throw new Error("Request failed");
  return r.json();
});

export const deleteEndpoint = (id) => fetch(`https://web-production-82857.up.railway.app/endpoints/${id}`, { method: 'DELETE' }).then(r=>r.text());
export const registerEndpoint = (data) => fetch('https://web-production-82857.up.railway.app/endpoints', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(data)
}).then(r=>r.text());
