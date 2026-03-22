// core api fetch wrapper
const api = {
  get baseUrl() {
    return "/api";
  },

  getTokens() {
    return {
      accessToken: localStorage.getItem("accessToken"),
      refreshToken: localStorage.getItem("refreshToken"),
    };
  },

  setTokens(access, refresh) {
    if (access) localStorage.setItem("accessToken", access);
    if (refresh) localStorage.setItem("refreshToken", refresh);
  },

  clearTokens() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
  },

  async fetchWithToken(endpoint, options = {}) {
    let tokens = this.getTokens();

    const headers = {
      ...options.headers,
    };

    if (tokens.accessToken) {
      headers["Authorization"] = `Bearer ${tokens.accessToken}`;
    }

    let config = { ...options, headers };

    // If body is FormData, don't set Content-Type manually so browser sets boundary
    if (!(options.body instanceof FormData) && !headers["Content-Type"]) {
      headers["Content-Type"] = "application/json";
    }

    const targetUrl = endpoint.startsWith(this.baseUrl)
      ? endpoint
      : `${this.baseUrl}${endpoint}`;
    let response = await fetch(targetUrl, config);

    if (response.status === 401 && tokens.refreshToken) {
      // Try reviving token
      const refreshRes = await fetch(`${this.baseUrl}/auth/refresh-token`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: tokens.refreshToken }),
      });

      if (refreshRes.ok) {
        const refreshedData = await refreshRes.json();
        this.setTokens(
          refreshedData.data.accessToken,
          refreshedData.data.refreshToken,
        );
        // Replay original request
        headers["Authorization"] = `Bearer ${refreshedData.data.accessToken}`;
        config.headers = headers;
        response = await fetch(targetUrl, config);
      } else {
        this.clearTokens();
        window.location.href = "/login?expired=true";
        throw new Error("Session expired. Please login again.");
      }
    }

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      if (response.status === 403) {
        this.clearTokens();
        window.location.href = "/login?expired=true&reason=forbidden";
        throw new Error("Quyền truy cập đã thay đổi. Vui lòng đăng nhập lại.");
      }
      throw { status: response.status, data: data };
    }

    return data;
  },

  async get(endpoint) {
    return this.fetchWithToken(endpoint, { method: "GET" });
  },

  async post(endpoint, body) {
    return this.fetchWithToken(endpoint, {
      method: "POST",
      body: JSON.stringify(body),
    });
  },

  async patch(endpoint, body) {
    return this.fetchWithToken(endpoint, {
      method: "PATCH",
      body: JSON.stringify(body),
    });
  },

  async delete(endpoint) {
    return this.fetchWithToken(endpoint, { method: "DELETE" });
  },

  showError(title, message) {
    if (typeof Swal !== "undefined") {
      Swal.fire({ icon: "error", title: title, text: message });
    } else {
      alert(title + ": " + message);
    }
  },
};
