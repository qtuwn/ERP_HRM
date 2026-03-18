// core api fetch wrapper
const api = {
    get baseUrl() {
        return '/api';
    },
    
    getTokens() {
        return {
            accessToken: localStorage.getItem('accessToken'),
            refreshToken: localStorage.getItem('refreshToken'),
        };
    },

    setTokens(access, refresh) {
        if(access) localStorage.setItem('accessToken', access);
        if(refresh) localStorage.setItem('refreshToken', refresh);
    },

    clearTokens() {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
    },

    async fetchWithToken(endpoint, options = {}) {
        let tokens = this.getTokens();
        
        const headers = {
            ...options.headers,
        };

        if (tokens.accessToken) {
            headers['Authorization'] = `Bearer ${tokens.accessToken}`;
        }

        let config = { ...options, headers };
        
        // If body is FormData, don't set Content-Type manually so browser sets boundary
        if (!(options.body instanceof FormData) && !headers['Content-Type']) {
            headers['Content-Type'] = 'application/json';
        }

        let response = await fetch(`${this.baseUrl}${endpoint}`, config);

        if (response.status === 401 && tokens.refreshToken) {
            // Try reviving token
            const refreshRes = await fetch(`${this.baseUrl}/auth/refresh-token`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken: tokens.refreshToken })
            });

            if (refreshRes.ok) {
                const refreshedData = await refreshRes.json();
                this.setTokens(refreshedData.data.accessToken, refreshedData.data.refreshToken);
                // Replay original request
                headers['Authorization'] = `Bearer ${refreshedData.data.accessToken}`;
                config.headers = headers;
                response = await fetch(`${this.baseUrl}${endpoint}`, config);
            } else {
                this.clearTokens();
                window.location.href = '/login?expired=true';
                throw new Error("Session expired. Please login again.");
            }
        }

        const data = await response.json().catch(() => null);
        
        if (!response.ok) {
            throw { status: response.status, data: data };
        }
        
        return data;
    }
};
