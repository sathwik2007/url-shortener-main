"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/contexts/AuthenticationContext";
import RegisterForm from "@/components/auth/RegisterForm";
import Link from "next/link";

export default function RegisterPage() {
  const { isAuthenticated } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (isAuthenticated) {
      router.push("/");
    }
  }, [isAuthenticated, router]);

  const handleSuccess = () => {
    router.push("/");
  };

  if (isAuthenticated) {
    return null; // Will redirect
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-blue-50 flex items-center justify-center px-4">
      <div className="max-w-md w-full space-y-8">
        {/* Header */}
        <div className="text-center">
          <Link
            href="/"
            className="text-2xl font-bold text-gray-900 hover:text-blue-600"
          >
            URL Shortener
          </Link>
          <h2 className="mt-6 text-3xl font-extrabold text-gray-900">
            Create your account
          </h2>
          <p className="mt-2 text-sm text-gray-600">
            Or{" "}
            <Link
              href="/login"
              className="font-medium text-blue-600 hover:text-blue-500"
            >
              sign in to your existing account
            </Link>
          </p>
        </div>

        {/* Form */}
        <div className="bg-white py-8 px-6 shadow-lg rounded-xl">
          <RegisterForm
            onSuccess={handleSuccess}
            onSwitchToLogin={() => router.push("/login")}
          />
        </div>

        {/* Back to home */}
        <div className="text-center">
          <Link href="/" className="text-sm text-gray-600 hover:text-blue-600">
            ← Back to home
          </Link>
        </div>
      </div>
    </div>
  );
}
