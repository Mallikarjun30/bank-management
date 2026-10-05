import { useEffect, useState } from "react";

type HealthResponse = {
  status: string;
};

function App() {
  const [backendStatus, setBackendStatus] = useState("Checking...");

  useEffect(() => {
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;

    fetch(`${apiBaseUrl}/health`)
      .then((response) => {
        if (!response.ok) {
          throw new Error("Backend request failed");
        }

        return response.json() as Promise<HealthResponse>;
      })
      .then((data) => {
        setBackendStatus(data.status);
      })
      .catch((error) => {
        console.error("Backend health check failed:", error);
        setBackendStatus("DOWN");
      });
  }, []);

  return (
    <main>
      <h1>Bank Management System</h1>

      <p>Frontend: UP</p>
      <p>Backend: {backendStatus}</p>
    </main>
  );
}

export default App;
