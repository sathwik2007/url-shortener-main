import { ApiClient } from "./api-client";

const baseURL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

const api = new ApiClient(baseURL);

export default api;