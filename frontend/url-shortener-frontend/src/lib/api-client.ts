/* eslint-disable @typescript-eslint/no-explicit-any */
// src/lib/api-client.ts
import axios, { AxiosInstance, AxiosError } from "axios";
import {
  CreateUrlRequest,
  CreateUrlResponse,
  ApiError,
  RegisterRequest,
  LoginRequest,
  AuthResponse,
  UrlMappingResponse,
  UserUrlSummary,
  PaginatedResponse,
  ClickStats,
  LinkAnalytics,
} from "@/types/api";

export class ApiClient {
  private axiosInstance: AxiosInstance;
  private authToken: string | null = null;

  constructor(baseURL: string) {
    this.axiosInstance = axios.create({
      baseURL,
      timeout: 10000, // 10 seconds
      headers: {
        "Content-Type": "application/json",
      },
    });

    // Add interceptors
    this.axiosInstance.interceptors.request.use(
      (config) => {
        if (this.authToken) {
          config.headers.Authorization = `Bearer ${this.authToken}`;
        }
        if (process.env.NODE_ENV === "development") {
          console.log("➡️ [API Request]", config);
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    this.axiosInstance.interceptors.response.use(
      (response) => {
        if (process.env.NODE_ENV === "development") {
          console.log("✅ [API Response]", response);
        }
        return response;
      },
      (error) => {
        if (error.response?.status === 401) {
          this.clearAuthToken();

          if (typeof window !== "undefined") {
            window.dispatchEvent(new CustomEvent("auth:unauthorized"));
          }
        }
        return Promise.reject(this.handleError(error));
      }
    );
  }

  // Token management methods
  public setAuthToken(token: string): void {
    this.authToken = token;
    if (typeof window !== "undefined") {
      localStorage.setItem("auth_token", token);
    }
  }

  public clearAuthToken(): void {
    this.authToken = null;
    if (typeof window !== "undefined") {
      localStorage.removeItem("auth_token");
      localStorage.removeItem("user_data");
    }
  }

  public getAuthToken(): string | null {
    if (!this.authToken && typeof window !== "undefined") {
      this.authToken = localStorage.getItem("auth_token");
    }
    return this.authToken;
  }

  public isAuthenticated(): boolean {
    return !!this.getAuthToken();
  }

  // Centralized error handler
  private handleError(error: AxiosError): ApiError {
    if (error.response) {
      return {
        message: (error.response.data as any)?.message || "Server error",
        status: error.response.status,
        details: error.response.data,
      };
    } else if (error.request) {
      return { message: "No response from server. Please try again later." };
    } else if (error.message.includes("timeout")) {
      return { message: "Request timed out. Please try again." };
    } else {
      return { message: "Unexpected error occurred." };
    }
  }

  // URL validation helper
  private validateUrl(url: string): void {
    if (!url || url.trim().length === 0) {
      throw new Error("URL cannot be empty");
    }

    const trimmedUrl = url.trim();

    // Check URL length (matching backend max length of 2048)
    if (trimmedUrl.length > 2048) {
      throw new Error("URL is too long (maximum 2048 characters)");
    }

    // Basic URL format validation
    try {
      const urlObj = new URL(trimmedUrl);
      if (!["http:", "https:"].includes(urlObj.protocol)) {
        throw new Error("URL must use HTTP or HTTPS protocol");
      }
    } catch {
      throw new Error("Invalid URL format");
    }
  }

  // Response validation helper
  private validateCreateUrlResponse(data: any): CreateUrlResponse {
    if (!data || typeof data !== "object") {
      throw new Error("Invalid response format");
    }

    const { shortCode, originalUrl, shortUrl } = data;

    if (!shortCode || typeof shortCode !== "string") {
      throw new Error("Invalid response: missing or invalid shortCode");
    }

    if (!originalUrl || typeof originalUrl !== "string") {
      throw new Error("Invalid response: missing or invalid originalUrl");
    }

    if (!shortUrl || typeof shortUrl !== "string") {
      throw new Error("Invalid response: missing or invalid shortUrl");
    }

    return { shortCode, originalUrl, shortUrl, createdAt: data.createdAt };
  }
  // Authentication methods

  // Register user method
  public async register(data: RegisterRequest): Promise<AuthResponse> {
    try {
      const response = await this.axiosInstance.post<AuthResponse>(
        "/auth/register",
        data
      );
      const authData = response.data;
      this.setAuthToken(authData.token);

      if (typeof window !== "undefined") {
        localStorage.setItem(
          "user_data",
          JSON.stringify({
            id: authData.id,
            email: authData.email,
            firstName: authData.firstName,
            lastName: authData.lastName,
          })
        );
      }

      return authData;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Login user method
  public async login(data: LoginRequest): Promise<AuthResponse> {
    try {
      const response = await this.axiosInstance.post<AuthResponse>(
        "/auth/login",
        data
      );
      const authData = response.data;
      this.setAuthToken(authData.token);

      if (typeof window !== "undefined") {
        localStorage.setItem(
          "user_data",
          JSON.stringify({
            id: authData.id,
            email: authData.email,
            firstName: authData.firstName,
            lastName: authData.lastName,
          })
        );
      }

      return authData;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Logout user method
  public logout(): void {
    this.clearAuthToken();
  }

  // Fetch Urls of a user with pagination
  public async getUserUrls(
    page = 0,
    size = 10,
    activeOnly = false
  ): Promise<PaginatedResponse<UrlMappingResponse>> {
    try {
      const response = await this.axiosInstance.get<
        PaginatedResponse<UrlMappingResponse>
      >("/api/links", {
        params: { page, size, activeOnly },
      });
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Fetch user Url summary
  public async getUserUrlSummary(): Promise<UserUrlSummary> {
    try {
      const response = await this.axiosInstance.get<UserUrlSummary>(
        "/api/links/summary"
      );
      return response.data;
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Delete a url for a user
  public async deleteUrl(shortCode: string): Promise<void> {
    try {
      await this.axiosInstance.delete(`/api/links/${shortCode}`);
    } catch (error: any) {
      throw this.handleError(error);
    }
  }

  // Create short URL method
  public async createShortUrl(
    originalUrl: string,
    expiresAt?: string
  ): Promise<CreateUrlResponse> {
    try {
      // Client-side validation
      this.validateUrl(originalUrl);

      const payload: CreateUrlRequest = {
        originalUrl: originalUrl.trim(),
        ...(expiresAt && { expiresAt }),
      };
      
      const response = await this.axiosInstance.post<CreateUrlResponse>(
        "/api/links",
        payload
      );

      // Validate response structure
      return this.validateCreateUrlResponse(response.data);
    } catch (error: any) {
      // If it's already our custom error, re-throw it
      if (error.message && !error.response) {
        throw { message: error.message } as ApiError;
      }
      throw this.handleError(error);
    }
  }

  // Get click stats for a specific URL
  public async getLinkStats(shortCode: string): Promise<ClickStats> {
    try {
      const response = await this.axiosInstance.get<ClickStats>(`/api/links/${shortCode}/stats`);
      return response.data;
    } catch(error: any) {
      throw this.handleError(error);
    }
  }

  // Get analytics for all user links
  public async getUserAnalytics(): Promise<LinkAnalytics[]> {
    try {
      const response = await this.axiosInstance.get<LinkAnalytics[]>("/api/analytics/user");
      return response.data;
    } catch(error: any) {
      throw this.handleError(error);
    }
  }
}
