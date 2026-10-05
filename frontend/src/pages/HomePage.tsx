import { useEffect, useState } from "react";
import { getHealth } from "../services/api/apiClient";

function HomePage() {
  const [backendStatus, setBackendStatus] = useState("Checking...");

  useEffect(() => {
    getHealth()
      .then((data) => setBackendStatus(data.status))
      .catch(() => setBackendStatus("DOWN"));
  }, []);

  return (
    <section>
      <h2>Bank Management System</h2>
      <p>Frontend: UP</p>
      <p>Backend: {backendStatus}</p>
    </section>
  );
}

export default HomePage;
