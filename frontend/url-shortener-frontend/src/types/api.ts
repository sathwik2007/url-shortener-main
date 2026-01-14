/* eslint-disable @typescript-eslint/no-explicit-any */
export interface CreateUrlRequest {
    originalUrl: string;
    expiresAt?: string
}
export interface CreateUrlResponse {
    shortCode: string;
    originalUrl: string;
    shortUrl: string;
    createdAt: string;
}

export interface ApiError {
    message: string;
    status?: number;
    details?: any;
}

export interface RegisterRequest {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface AuthResponse {
    token: string;
    type: string;
    id: number;
    email: string;
    firstName: string;
    lastName: string;
}

export interface User {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
}

export interface UrlMappingResponse {
    shortCode: string;
    shortUrl: string;
    originalUrl: string;
    createdAt: string;
    expiresAt?: string;
    isActive: boolean;
}

export interface UserUrlSummary {
    totalUrls: number;
    activeUrls: number;
    inactiveUrls: number;
}

export interface PaginatedResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
}

export interface ClickStats {
    totalClicks: number;
    dailyStats: DailyClickStat[];
}

export interface DailyClickStat {
    date: string;
    clicks: number;
}

export interface LinkAnalytics {
    shortCode: string;
    totalClicks: number;
    dailyStats: DailyClickStat[];
    createdAt: string;
}