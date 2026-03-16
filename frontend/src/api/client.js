const BASE_URL = '/api/v1';

export function getToken() {
  return localStorage.getItem('seo_token');
}

export function setToken(token) {
  localStorage.setItem('seo_token', token);
}

export function removeToken() {
  localStorage.removeItem('seo_token');
}

export async function fetchApi(url, options = {}) {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}${url}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    removeToken();
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }

  const data = await response.json();

  if (!response.ok) {
    throw new Error(data.message || `HTTP ${response.status}`);
  }

  return data;
}

export function get(url) {
  return fetchApi(url);
}

export function post(url, body) {
  return fetchApi(url, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function put(url, body) {
  return fetchApi(url, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export function del(url) {
  return fetchApi(url, { method: 'DELETE' });
}
