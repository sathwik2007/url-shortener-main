import { CreateUrlResponse } from "@/types/api";
import { CheckIcon, ClipboardIcon, ExclamationTriangleIcon } from "@heroicons/react/24/solid";
import { useCallback, useEffect, useState } from "react";
import toast from "react-hot-toast";

interface ResultDisplayProps {
    result?: CreateUrlResponse | null;
    error?: string | null;
    isVisible?: boolean;
    onReset?: () => void;
}

const ResultDisplay: React.FC<ResultDisplayProps> = ({
    result,
    error,
    isVisible = false,
    onReset
}) => {
    const [copied, setCopied] = useState(false);
    const [showDetails, setShowDetails] = useState(false);

    const copyToClipboard = useCallback(
      async (text: string) => {
        try {
          if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
          } else {
            // fallback method (no hook calls here)
            const textarea = document.createElement("textarea");
            textarea.value = text;
            textarea.style.position = "fixed";
            textarea.style.top = "0";
            textarea.style.left = "0";
            textarea.style.opacity = "0";
            document.body.appendChild(textarea);
            textarea.focus();
            textarea.select();
            document.execCommand?.("copy"); // harmless in modern browsers
            document.body.removeChild(textarea);
          }

          setCopied(true);
          toast.success("Copied!");
          setTimeout(() => setCopied(false), 2000);
        } catch (err) {
          console.error("Clipboard copy failed:", err);
          toast.error("Failed to copy.");
        }
      },
      [setCopied]
    );

    useEffect(() => {
        if(isVisible && result) {
            setCopied(false);
        }
    }, [isVisible, result]);

    if(!isVisible) return null;

    return (
      <div
        className={`w-full max-w-2xl mx-auto mt-6 p-5 rounded-xl shadow-md transition-all duration-300
                ${
                  error
                    ? "bg-red-50 border border-red-200"
                    : result
                    ? "bg-green-50 border border-green-200"
                    : "bg-gray-50 border border-gray-200"
                }
                `}
      >
        {/* state-specific rendering here */}
        {result && !error && (
          <div className="flex flex-col items-center gap-3 text-center animate-fadeIn">
            <div className="flex items-center gap-2 text-green-700">
              <CheckIcon className="w-6 h-6" />
              <span className="font-semibold">
                Success! Your short link is ready
              </span>
            </div>

            <div className="flex flex-wrap items-center justify-center gap-2 mt-2">
              <a
                href={result.shortUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-lg font-medium text-blue-700 hover:underline break-all"
              >
                {result.shortUrl}
              </a>
              <button
                onClick={() => copyToClipboard(result.shortUrl)}
                className={`flex items-center gap-1 px-3 py-1 border rounded-md transition-all duration-200
          ${
            copied
              ? "bg-green-600 text-white"
              : "border-gray-300 text-gray-700 hover:bg-gray-100"
          }`}
                aria-label="Copy shortened URL"
              >
                {copied ? (
                  <CheckIcon className="w-5 h-5" />
                ) : (
                  <ClipboardIcon className="w-5 h-5" />
                )}
                {copied ? "Copied!" : "Copy"}
              </button>
            </div>

            {/* original URL section */}
            <div className="mt-3 text-sm text-gray-600">
              <button
                className="text-blue-600 underline"
                onClick={() => setShowDetails((p) => !p)}
              >
                {showDetails ? "Hide original URL" : "Show original URL"}
              </button>
              {showDetails && (
                <p className="mt-2 break-all">
                  {result.originalUrl.length > 80
                    ? `${result.originalUrl.slice(0, 80)}...`
                    : result.originalUrl}
                </p>
              )}
            </div>

            <button
              onClick={onReset}
              className="mt-4 px-5 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-all duration-200"
            >
              Try Another
            </button>
          </div>
        )}

        {/* Error State Rendering */}
        {error && (
          <div className="flex flex-col items-center text-center gap-3 text-red-700 animate-fadeIn">
            <div className="flex items-center gap-2">
              <ExclamationTriangleIcon className="w-6 h-6" />
              <span className="font-semibold">Something went wrong</span>
            </div>
            <p className="text-sm">{error}</p>
            <button
              onClick={onReset}
              className="mt-2 px-5 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-all duration-200"
            >
              Try Again
            </button>
          </div>
        )}

        {copied && (
          <span role="status" className="sr-only">
            Copied to clipboard
          </span>
        )}
      </div>
    );
};

export default ResultDisplay;