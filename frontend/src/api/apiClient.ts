export class ApiError extends Error {
  status: number;
  fieldErrors?: Record<string, string>;
  detail?: string;
  retryAfter?: number;

  constructor(status: number, message: string, fieldErrors?: Record<string, string>, detail?: string, retryAfter?: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
    this.detail = detail;
    this.retryAfter = retryAfter;
  }
}

export function getXsrfToken(): string | null {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(/(^|;)\s*XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[2]) : null;
}

export interface RequestOptions extends RequestInit {
  params?: Record<string, any>;
}

const BASE_URL = '/api/v1';

export async function request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { params, headers, ...customConfig } = options;

  let url = `${BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, val]) => {
      if (val !== undefined && val !== null && val !== '') {
        searchParams.append(key, String(val));
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      url += (url.includes('?') ? '&' : '?') + queryString;
    }
  }

  const method = (customConfig.method || 'GET').toUpperCase();
  const reqHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...(headers as Record<string, string>),
  };

  if (method !== 'GET') {
    const xsrf = getXsrfToken();
    if (xsrf && !reqHeaders['X-XSRF-TOKEN']) {
      reqHeaders['X-XSRF-TOKEN'] = xsrf;
    }
  }

  const config: RequestInit = {
    method,
    credentials: customConfig.credentials || 'include',
    ...customConfig,
    headers: reqHeaders,
  };

  let response: Response;
  try {
    response = await fetch(url, config);
  } catch (err: unknown) {
    if (err instanceof Error && err.name === 'AbortError') {
      throw err;
    }
    const message = err instanceof Error ? err.message : 'Network connection failed';
    throw new ApiError(0, message);
  }

  if (!response.ok) {
    let errorData: any = null;
    try {
      errorData = await response.json();
    } catch {
      // Ignore json parse error on non-json body
    }

    const retryAfterHeader = response.headers?.get ? response.headers.get('Retry-After') : undefined;
    const retryAfter = retryAfterHeader ? parseInt(retryAfterHeader, 10) : undefined;

    // 401 Interceptor: redirect to /login?next=... (except /auth/login, /actuator/health, or already on /login)
    if (response.status === 401) {
      const isAuthEndpoint = endpoint.includes('/auth/login') || endpoint.includes('/actuator/health');
      if (typeof window !== 'undefined' && !isAuthEndpoint) {
        const currentPath = window.location.pathname;
        if (currentPath !== '/login') {
          const next = encodeURIComponent(currentPath + window.location.search);
          window.location.href = `/login?next=${next}`;
        }
      }
    }

    // 403 Interceptor: trigger not permitted toast/event
    if (response.status === 403 && typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('gatelms:forbidden', { detail: { message: 'Not permitted' } }));
    }

    const message = errorData?.detail || errorData?.title || errorData?.message || `Request failed with status ${response.status}`;
    const fieldErrors = errorData?.fieldErrors || undefined;
    throw new ApiError(response.status, message, fieldErrors, errorData?.detail, retryAfter);
  }

  if (response.headers && response.headers.get('X-From-Cache') === '1') {
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('gatelms:cached-response'));
    }
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

export const apiClient = {
  get: <T>(url: string, options?: RequestOptions) => request<T>(url, { ...options, method: 'GET' }),
  post: <T>(url: string, body?: unknown, options?: RequestOptions) =>
    request<T>(url, { ...options, method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(url: string, body?: unknown, options?: RequestOptions) =>
    request<T>(url, { ...options, method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  patch: <T>(url: string, body?: unknown, options?: RequestOptions) =>
    request<T>(url, { ...options, method: 'PATCH', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(url: string, options?: RequestOptions) => request<T>(url, { ...options, method: 'DELETE' }),
};
