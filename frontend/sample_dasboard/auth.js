(function () {
    const TOKEN_KEY = "diasmart_jwt_token";
    const USER_KEY = "diasmart_logged_user";

    function getApiBase() {
        return window.DIASMART_API_BASE || "http://localhost:3000";
    }

    function getToken() {
        return localStorage.getItem(TOKEN_KEY);
    }

    function getUser() {
        try {
            const raw = localStorage.getItem(USER_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch (_err) {
            return null;
        }
    }

    function saveSession(token, user) {
        localStorage.setItem(TOKEN_KEY, token);
        localStorage.setItem(USER_KEY, JSON.stringify(user || {}));
    }

    function clearSession() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
    }

    function getCurrentPageName() {
        const parts = window.location.pathname.split("/");
        return parts[parts.length - 1] || "index.html";
    }

    function redirectToLogin() {
        const currentPage = getCurrentPageName();

        if (currentPage === "login.html") {
            return;
        }

        const next = encodeURIComponent(currentPage);
        window.location.href = `login.html?next=${next}`;
    }

    function requireLogin() {
        const token = getToken();

        if (!token) {
            redirectToLogin();
            return false;
        }

        return true;
    }

    async function login(email, password) {
        const response = await fetch(`${getApiBase()}/api/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ email, password })
        });

        let data = {};
        try {
            data = await response.json();
        } catch (_err) {
            data = {};
        }

        if (!response.ok) {
            throw new Error(data.error || "Login failed");
        }

        if (!data.token) {
            throw new Error("Login response did not include a token");
        }

        saveSession(data.token, data.user);
        return data;
    }

    async function authFetch(url, options = {}) {
        const token = getToken();

        const headers = {
            ...(options.headers || {})
        };

        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }

        const response = await fetch(url, {
            ...options,
            headers
        });

        if (response.status === 401) {
            clearSession();
            redirectToLogin();
            throw new Error("Unauthorized. Please log in again.");
        }

        return response;
    }

    function logout() {
        clearSession();
        window.location.href = "login.html";
    }

    window.diasmartAuth = {
        login,
        logout,
        authFetch,
        requireLogin,
        getToken,
        getUser,
        clearSession
    };
})();