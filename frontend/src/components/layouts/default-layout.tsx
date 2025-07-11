import { cn } from "@/lib/utils";
import { Home, MapPin } from "lucide-react";
import { Outlet } from "react-router";
import { Link, useLocation } from "react-router";

const sidebarItems = [
  { icon: Home, label: "Home", path: "/" },
  { icon: MapPin, label: "Locations", path: "/locations" },
];

export function DefaultLayout() {
  const location = useLocation();

  return (
    <div className="min-h-screen bg-background flex">
      {/* Sidebar */}
      <aside className="w-64 border-r bg-card">
        <div className="p-6">
          <h1 className="text-2xl font-bold">CPO Manager</h1>
        </div>
        <nav className="space-y-2 p-4">
          {sidebarItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;

            return (
              <Link
                key={item.path}
                to={item.path}
                className={cn(
                  "flex items-center space-x-3 px-4 py-2 rounded-lg transition-colors",
                  "hover:bg-secondary",
                  isActive && "bg-secondary text-primary font-medium",
                )}
              >
                <Icon className="w-5 h-5" />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>
      </aside>

      {/* Main content */}
      <main className="flex-1 p-8">
        <Outlet />
      </main>
    </div>
  );
}
