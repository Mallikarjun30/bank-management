const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081/api";

export class ApiError extends Error {
  public readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export async function apiFetch<T>(
  path: string,
  options?: RequestInit,
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options?.headers ?? {}),
    },
  });

  const responseText = await response.text();

  let data: unknown;

  try {
    data = responseText ? JSON.parse(responseText) : null;
  } catch {
    data = null;
  }

  if (!response.ok) {
    const body =
      typeof data === "object" && data !== null
        ? (data as { message?: unknown })
        : null;

    const message =
      typeof body?.message === "string"
        ? body.message
        : `API request failed with status ${response.status}`;

    throw new ApiError(message, response.status);
  }

  return data as T;
}

export type HealthResponse = {
  status: string;
};

export function getHealth(): Promise<HealthResponse> {
  return apiFetch<HealthResponse>("/health");
}

export type PasswordResetRequest = {
  email: string;
};

export type PasswordResetConfirmRequest = {
  token: string;
  newPassword: string;
};

export type MessageResponse = {
  message: string;
};

export function requestPasswordReset(
  request: PasswordResetRequest,
): Promise<MessageResponse> {
  return apiFetch<MessageResponse>("/auth/password-reset/request", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function confirmPasswordReset(
  request: PasswordResetConfirmRequest,
): Promise<MessageResponse> {
  return apiFetch<MessageResponse>("/auth/password-reset/confirm", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
