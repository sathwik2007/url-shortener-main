/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import ResultDisplay from "@/components/ResultDisplay";
import UrlForm from "@/components/UrlForm";
import api from "@/lib/api";

import { CreateUrlResponse } from "@/types/api";
import { useState } from "react";
import toast from "react-hot-toast";

export default function Home() {
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
      toast.success("Short URL created successfully!");
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
    <main className="flex flex-col items-center min-h-screen bg-gradient-to-br from-gray-50 to-blue-50 p-6">
      {/* Header */}
      <header className="text-center max-w-md mb-8">
        <h1 className="text-3xl sm:text-4xl font-bold text-gray-800 mb-2">
          URL Shortener
        </h1>
        <p className="text-gray-600 mx-w-md">
          Paste your long URL below to get a fast, shareable short link.
        </p>
      </header>

      {/* Main Content */}
      <section className="w-full max-w-2xl">
        <UrlForm onSubmit={handleUrlSubmit} isLoading={isLoading}/>

        <ResultDisplay 
          result={result}
          error={error}
          isVisible={showResult}
          onReset={handleReset}
        />
      </section>

      {/* Footer */}
      <footer className="mt-auto pt-8 text-center text-sm text-gray-500">
        Built by Sathwik Pillalamarri using Next.js, Typescript and Tailwind CSS
      </footer>
    </main>
  );
}
