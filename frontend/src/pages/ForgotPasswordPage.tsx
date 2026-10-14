import { useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router-dom";
import {
  ApiError,
  requestPasswordReset,
} from "../services/api/apiClient";
import "./AuthPages.css";

function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      await requestPasswordReset({ email: email.trim() });

      // Do not reveal whether the account exists.
      setSubmitted(true);
    } catch (err) {
      if (err instanceof ApiError && err.status < 500) {
        setError(
          "Please check your email address and try again.",
        );
      } else {
        setError(
          "We couldn't process your request right now. Please try again later.",
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
          ✉
        </div>

        {!submitted ? (
          <>
            <p className="auth-eyebrow">ACCOUNT RECOVERY</p>
            <h2>Forgot your password?</h2>
            <p className="auth-description">
              Enter the email address associated with your account.
              If eligible, you'll receive a password-reset link.
            </p>

            <form onSubmit={handleSubmit} className="auth-form">
              <label htmlFor="reset-email">Email address</label>
              <input
                id="reset-email"
                type="email"
                name="email"
                autoComplete="email"
                placeholder="you@example.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                maxLength={254}
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
                disabled={loading}
              >
                {loading ? "Sending request..." : "Send reset link"}
              </button>
            </form>
          </>
        ) : (
          <>
            <p className="auth-eyebrow">CHECK YOUR EMAIL</p>
            <h2>Request received</h2>
            <p className="auth-description" role="status">
              If an eligible account exists for that email address,
              password-reset instructions will be sent.
            </p>
            <p className="auth-help">
              Check your inbox. In local development, you can inspect
              captured emails in Mailpit.
            </p>
            <button
              className="auth-button"
              type="button"
              onClick={() => {
                setSubmitted(false);
                setEmail("");
              }}
            >
              Submit another request
            </button>
          </>
        )}

        <p className="auth-footer">
          <Link to="/">Return to home</Link>
        </p>
      </div>
    </section>
  );
}

export default ForgotPasswordPage;
