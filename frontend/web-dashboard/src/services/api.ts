import axios from "axios";

const API_URL_ENV_KEY = "VITE_API_URL";

const resolveApiBaseUrl = () => {
  const apiUrl = import.meta.env.VITE_API_URL?.trim();

  if (!apiUrl) {
    throw new Error(`${API_URL_ENV_KEY} is not configured.`);
  }

  if (apiUrl.startsWith(`${API_URL_ENV_KEY}=`)) {
    throw new Error(
      `${API_URL_ENV_KEY} contains its own key name. Set it as ${API_URL_ENV_KEY}=https://api.diasmart.xyz/api/v1`
    );
  }

  try {
    new URL(apiUrl);
  } catch {
    throw new Error(`${API_URL_ENV_KEY} must be an absolute URL. Received: ${apiUrl}`);
  }

  return apiUrl.replace(/\/+$/, "");
};

export const API_BASE_URL = resolveApiBaseUrl();

const api = axios.create({
  baseURL: API_BASE_URL,
});

api.interceptors.request.use(
  (config) => {
    const token =
      sessionStorage.getItem("token");

    if (token) {
      config.headers.Authorization =
        `Bearer ${token}`;
    }

    return config;
  }
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthRequest = error.config?.url?.includes("/auth/");
    if (error.response?.status === 401 && !isAuthRequest) {
      sessionStorage.removeItem("token");
      sessionStorage.removeItem("role");
      sessionStorage.removeItem("userId");
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export default api;
