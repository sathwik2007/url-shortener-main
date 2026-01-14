/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import Header from "@/components/Header";
import ResultDisplay from "@/components/ResultDisplay";
import UrlForm from "@/components/UrlForm";
import { useAuth } from "@/contexts/AuthenticationContext";
import api from "@/lib/api";

import { CreateUrlResponse } from "@/types/api";
import { useState } from "react";
import toast from "react-hot-toast";
import Link from "next/link";

export default function Home() {
  const { isAuthenticated, user } = useAuth();
  const [result, setResult] = useState<CreateUrlResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showResult, setShowResult] = useState(false);

  const handleUrlSubmit = async (url:string) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await api.createShortUrl(url);
      setResult(response);
      setShowResult(true);

      if(isAuthenticated) {
        toast.success('Short URL created and saved to your account');
      } else {
        toast.success("Short URL created successfully!");
      }
    } catch (err : any) {
      const message = err?.message || "Something went wrong. Please try again";
      setError(message);
      setShowResult(true);
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  }

  const handleReset = () => {
    setResult(null);
    setError(null);
    setShowResult(false);
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-blue-50">
      <Header />

      <main className="flex flex-col items-center px-6 py-12">
        {/* Welcome message for authenticated users */}
        {isAuthenticated && (
          <div className="mb-8 text-center">
            <p className="text-lg text-gray-700">
              Welcome back,{" "}
              <span className="font-semibold text-blue-600">
                {user?.firstName}
              </span>
            </p>
            <p className="text-sm text-gray-600">
              Your URLs will be saved to your account for easy management
            </p>
            <Link
              href="/dashboard"
              className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              View Dashboard
            </Link>
          </div>
        )}

        {/* Header */}
        <header className="text-center max-w-md mb-8">
          <h1 className="text-3xl sm:text-4xl font-bold text-gray-800 mb-2">
            {isAuthenticated ? "Create Short URL" : "URL Shortener"}
          </h1>
          <p className="text-gray-600 mx-w-md">
            {isAuthenticated
              ? "Create and manage your shortened URLs with analytics"
              : "Paste your long URL below to get a fast, shareable short link."}
          </p>
        </header>

        {/* Main Content */}
        <section className="w-full max-w-2xl">
          <UrlForm onSubmit={handleUrlSubmit} isLoading={isLoading} />

          <ResultDisplay
            result={result}
            error={error}
            isVisible={showResult}
            onReset={handleReset}
          />
        </section>

        {/* Footer */}
        <footer className="mt-auto pt-8 text-center text-sm text-gray-500">
          Built by Sathwik Pillalamarri using Next.js, Typescript and Tailwind
          CSS
        </footer>
      </main>
    </div>
  );
}
