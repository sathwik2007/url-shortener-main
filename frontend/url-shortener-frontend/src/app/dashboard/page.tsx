"use client";

import { useAuth } from "@/contexts/AuthenticationContext";
import { PlusIcon } from "@heroicons/react/24/solid";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import Link from "next/link";
import DashboardStats from "../../components/dashboard/DashboardStats";
import LinksList from "../../components/dashboard/LinksList";
import CreateLinkForm from "../../components/dashboard/CreateLinkForm";

export default function DashboardPage() {
  const { isAuthenticated, user, isLoading } = useAuth();
  const router = useRouter();
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push("/");
    }
  }, [isAuthenticated, isLoading, router]);

  const handleLinkCreated = () => {
    setShowCreateForm(false);
    setRefreshTrigger((prev) => prev + 1); // Triggers refresh of links list
  };

  const handleLinkDeleted = () => {
    setRefreshTrigger((prev) => prev + 1); // Trigger refresh of links list
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading Dashboard...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null; // will redirect
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center justify-between">
            <Link
              href="/"
              className="text-xl font-bold text-blue-600 hover:text-blue-700 transition-colors"
            >
              URL Shortener
            </Link>
            <button
              onClick={() => setShowCreateForm(true)}
              className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 hover:cursor-pointer transition-colors"
            >
              <PlusIcon className="w-5 h-5 mr-2" />
              Create Link
            </button>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="space-y-8">
          {/* Page Title */}
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
            <p className="text-gray-600">Welcome back, {user?.firstName}</p>
          </div>

          {/* Stats Section */}
          <DashboardStats refreshTrigger={refreshTrigger} />

          {/* Links Section */}
          <div className="bg-white rounded-lg shadow-sm border border-gray-200">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-semibold text-gray-900">
                Your Links
              </h2>
              <p className="text-sm text-gray-600">
                Manage and track your shortened URLs
              </p>
            </div>
            <LinksList
              refreshTrigger={refreshTrigger}
              onLinkDeleted={handleLinkDeleted}
            />
          </div>
        </div>
      </div>

      {/* Create Link Modal */}
      {showCreateForm && (
        <CreateLinkForm
          isOpen={showCreateForm}
          onClose={() => setShowCreateForm(false)}
          onSuccess={handleLinkCreated}
        />
      )}
    </div>
  );
}
