import axios from "axios";

console.log("api.ts loaded");

console.log(
  "API URL:",
  import.meta.env.VITE_API_URL
);

const api = axios.create({
  baseURL:
    import.meta.env.VITE_API_URL,
});

api.interceptors.request.use(
  (config) => {
    const token =
      localStorage.getItem("token");

    if (token) {
      config.headers.Authorization =
        `Bearer ${token}`;
    }

    return config;
  }
);

export default api;