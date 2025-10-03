/* eslint-disable @typescript-eslint/no-explicit-any */
export interface CreateUrlRequest {
    originalUrl: string;
}
export interface CreateUrlResponse {
    shortCode: string;
    originalUrl: string;
    shortUrl: string;
}

export interface ApiError {
    message: string;
    status?: number;
    details?: any;
}