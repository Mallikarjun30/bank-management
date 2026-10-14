import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  ApiError,
  confirmPasswordReset,
} from "../services/api/apiClient";
import "./AuthPages.css";

function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    if (!token.trim()) {
      setError(
        "The reset link is missing its token. Request a new reset link.",
      );
      return;
    }

    if (password.length < 12 || password.length > 128) {
      setError("Your password must be between 12 and 128 characters.");
      return;
    }

    if (password !== confirmPassword) {
      setError("The passwords do not match.");
      return;
    }

    setLoading(true);

    try {
      await confirmPasswordReset({
        token,
        newPassword: password,
      });

      setSuccess(true);
      setPassword("");
      setConfirmPassword("");
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError(
          "This reset link is invalid, expired, or already used. Please request a new one.",
        );
      } else {
        setError(
          "We couldn't reset your password right now. Please try again later.",
        );
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-card">
        <div className="auth-icon" aria-hidden="true">
          🔒
        </div>

        {success ? (
          <>
            <p className="auth-eyebrow">PASSWORD UPDATED</p>
            <h2>Password reset successful</h2>
            <p className="auth-description" role="status">
              Your password has been changed. You can now use your new
              password the next time you sign in.
            </p>
            <p className="auth-help">
              For security, this reset link can no longer be used.
            </p>
            <Link className="auth-button auth-button-link" to="/">
              Return to home
            </Link>
          </>
        ) : (
          <>
            <p className="auth-eyebrow">ACCOUNT RECOVERY</p>
            <h2>Set a new password</h2>
            <p className="auth-description">
              Choose a strong password with at least 12 characters.
            </p>

            <form onSubmit={handleSubmit} className="auth-form">
              <label htmlFor="new-password">New password</label>
              <input
                id="new-password"
                type="password"
                name="newPassword"
                autoComplete="new-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                minLength={12}
                maxLength={128}
                required
                disabled={loading}
              />

              <label htmlFor="confirm-password">
                Confirm new password
              </label>
              <input
                id="confirm-password"
                type="password"
                name="confirmPassword"
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(event) =>
                  setConfirmPassword(event.target.value)
                }
                minLength={12}
                maxLength={128}
                required
                disabled={loading}
              />

              {error && (
                <p className="auth-error" role="alert">
                  {error}
                </p>
              )}

              <button
                className="auth-button"
                type="submit"
                disabled={loading || !token}
              >
                {loading ? "Updating password..." : "Reset password"}
              </button>

              {!token && (
                <p className="auth-error" role="alert">
                  This page has no reset token. Open the link from your
                  password-reset email.
                </p>
              )}
            </form>
          </>
        )}

        <p className="auth-footer">
          <Link to="/forgot-password">Request a new reset link</Link>
        </p>
      </div>
    </section>
  );
}

export default ResetPasswordPage;
