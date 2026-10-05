import { Link, Outlet } from "react-router-dom";

function AppShell() {
  return (
    <div>
      <header>
        <h1>Bank Management System</h1>
        <nav>
          <Link to="/">Home</Link>
        </nav>
      </header>

      <main>
        <Outlet />
      </main>
    </div>
  );
}

export default AppShell;
