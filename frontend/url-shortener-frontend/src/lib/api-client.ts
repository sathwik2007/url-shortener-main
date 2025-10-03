/* eslint-disable @typescript-eslint/no-explicit-any */
// src/lib/api-client.ts
import axios, { AxiosInstance, AxiosError } from "axios";
import { CreateUrlRequest, CreateUrlResponse, ApiError } from "@/types/api";

export class ApiClient {
  private axiosInstance: AxiosInstance;

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
        return Promise.reject(this.handleError(error));
      }
    );
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
      if (!['http:', 'https:'].includes(urlObj.protocol)) {
        throw new Error("URL must use HTTP or HTTPS protocol");
      }
    } catch {
      throw new Error("Invalid URL format");
    }
  }

  // Response validation helper
  private validateCreateUrlResponse(data: any): CreateUrlResponse {
    if (!data || typeof data !== 'object') {
      throw new Error("Invalid response format");
    }

    const { shortCode, originalUrl, shortUrl } = data;

    if (!shortCode || typeof shortCode !== 'string') {
      throw new Error("Invalid response: missing or invalid shortCode");
    }

    if (!originalUrl || typeof originalUrl !== 'string') {
      throw new Error("Invalid response: missing or invalid originalUrl");
    }

    if (!shortUrl || typeof shortUrl !== 'string') {
      throw new Error("Invalid response: missing or invalid shortUrl");
    }

    return { shortCode, originalUrl, shortUrl };
  }

  // Create short URL method
  public async createShortUrl(originalUrl: string): Promise<CreateUrlResponse> {
    try {
      // Client-side validation
      this.validateUrl(originalUrl);

      const payload: CreateUrlRequest = { originalUrl: originalUrl.trim() };
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
}
