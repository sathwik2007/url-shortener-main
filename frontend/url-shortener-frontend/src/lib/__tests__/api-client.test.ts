import axios from 'axios';
import { ApiClient } from '../api-client';
import { CreateUrlResponse, ApiError } from '@/types/api';

// Mock axios
jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('ApiClient', () => {
  let apiClient: ApiClient;
  const mockAxiosInstance = {
    create: jest.fn(),
    post: jest.fn(),
    interceptors: {
      request: { use: jest.fn() },
      response: { use: jest.fn() }
    }
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockedAxios.create.mockReturnValue(mockAxiosInstance as any);
    apiClient = new ApiClient('http://localhost:8080');
  });

  describe('createShortUrl', () => {
    const validUrl = 'https://www.example.com';
    const mockResponse: CreateUrlResponse = {
      shortCode: 'abc123',
      originalUrl: validUrl,
      shortUrl: 'http://localhost:8080/abc123'
    };

    it('should create short URL successfully', async () => {
      mockAxiosInstance.post.mockResolvedValue({ data: mockResponse });

      const result = await apiClient.createShortUrl(validUrl);

      expect(result).toEqual(mockResponse);
      expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/links', {
        originalUrl: validUrl
      });
    });

    it('should trim whitespace from URL', async () => {
      const urlWithSpaces = '  https://www.example.com  ';
      mockAxiosInstance.post.mockResolvedValue({ data: mockResponse });

      await apiClient.createShortUrl(urlWithSpaces);

      expect(mockAxiosInstance.post).toHaveBeenCalledWith('/api/links', {
        originalUrl: validUrl
      });
    });

    it('should throw error for empty URL', async () => {
      await expect(apiClient.createShortUrl('')).rejects.toEqual({
        message: 'URL cannot be empty'
      });

      await expect(apiClient.createShortUrl('   ')).rejects.toEqual({
        message: 'URL cannot be empty'
      });
    });

    it('should throw error for invalid URL format', async () => {
      await expect(apiClient.createShortUrl('not-a-url')).rejects.toEqual({
        message: 'Invalid URL format'
      });

      await expect(apiClient.createShortUrl('ftp://example.com')).rejects.toEqual({
        message: 'URL must use HTTP or HTTPS protocol'
      });
    });

    it('should throw error for URL too long', async () => {
      const longUrl = 'https://example.com/' + 'a'.repeat(2050);

      await expect(apiClient.createShortUrl(longUrl)).rejects.toEqual({
        message: 'URL is too long (maximum 2048 characters)'
      });
    });

    it('should validate response structure', async () => {
      const invalidResponse = { shortCode: 'abc123' }; // missing fields
      mockAxiosInstance.post.mockResolvedValue({ data: invalidResponse });

      await expect(apiClient.createShortUrl(validUrl)).rejects.toEqual({
        message: 'Invalid response: missing or invalid originalUrl'
      });
    });

    it('should handle network errors', async () => {
      const networkError = {
        request: {},
        message: 'Network Error'
      };
      mockAxiosInstance.post.mockRejectedValue(networkError);

      await expect(apiClient.createShortUrl(validUrl)).rejects.toEqual({
        message: 'No response from server. Please try again later.'
      });
    });

    it('should handle timeout errors', async () => {
      const timeoutError = {
        message: 'timeout of 10000ms exceeded'
      };
      mockAxiosInstance.post.mockRejectedValue(timeoutError);

      await expect(apiClient.createShortUrl(validUrl)).rejects.toEqual({
        message: 'Request timed out. Please try again.'
      });
    });

    it('should handle HTTP errors', async () => {
      const httpError = {
        response: {
          status: 400,
          data: { message: 'Invalid URL provided' }
        }
      };
      mockAxiosInstance.post.mockRejectedValue(httpError);

      await expect(apiClient.createShortUrl(validUrl)).rejects.toEqual({
        message: 'Invalid URL provided',
        status: 400,
        details: { message: 'Invalid URL provided' }
      });
    });

    it('should handle server errors without message', async () => {
      const serverError = {
        response: {
          status: 500,
          data: {}
        }
      };
      mockAxiosInstance.post.mockRejectedValue(serverError);

      await expect(apiClient.createShortUrl(validUrl)).rejects.toEqual({
        message: 'Server error',
        status: 500,
        details: {}
      });
    });
  });

  describe('constructor', () => {
    it('should create axios instance with correct config', () => {
      expect(mockedAxios.create).toHaveBeenCalledWith({
        baseURL: 'http://localhost:8080',
        timeout: 10000,
        headers: {
          'Content-Type': 'application/json'
        }
      });
    });

    it('should set up request and response interceptors', () => {
      expect(mockAxiosInstance.interceptors.request.use).toHaveBeenCalled();
      expect(mockAxiosInstance.interceptors.response.use).toHaveBeenCalled();
    });
  });
});