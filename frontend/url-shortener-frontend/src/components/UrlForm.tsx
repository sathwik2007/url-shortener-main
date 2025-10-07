/* eslint-disable @typescript-eslint/no-explicit-any */
import { useCallback, useState } from "react";
import toast from "react-hot-toast";

interface UrlFormProps {
    onSubmit: (url: string) => Promise<void>;
    isLoading?: boolean
}

const UrlForm: React.FC<UrlFormProps> = ({onSubmit, isLoading = false}) => {
    const [url, setUrl] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [isValid, setIsValid] = useState<boolean>(false);

    const validateUrl = useCallback((value: string): string | null => {
        if(!value.trim()) return "URL cannot be empty";
        try {
            const parsed = new URL(value);
            if(!["http:", "https:"].includes(parsed.protocol)) {
                return "URL must start with http:// or https://";
            }
            if(value.length > 2048) {
                return "URL length cannot exceed 2048 characters";
            }
            return null;
        } catch {
            return "Invalid URL format";
        }
    }, []);

    const handleSubmit = async () => {
        const validationError = validateUrl(url);
        if(validationError) {
            setError(validationError);
            setIsValid(false);
            toast.error(validationError);
            return;
        }

        try {
            await onSubmit(url);
            setUrl("");
            setError(null);
            setIsValid(false);
        } catch(err: any) {
            toast.error("Failed to shorten URL. Please try again");
        }
    }

    const handleInputChange = (value: string) => {
        setUrl(value);
        const validationError = validateUrl(value);
        setError(validationError);
        setIsValid(!validationError);
    }

    return(
        <form
            onSubmit={(e) => {
                e.preventDefault();
                handleSubmit();
            }}
            className="flex flex-col sm:flex-row gap-3 items-center justify-center w-full max-w-2xl mx-auto"
            aria-label="URL Shortener Form"
        >
            <div className="w-full flex flex-col">
                <input 
                    type="url"
                    placeholder="Enter your long URL (https://.. or http://)"
                    value={url}
                    onChange={(e) => handleInputChange(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
                    disabled={isLoading}
                    className={`w-full px-4 py-3 border rounded-xl focus:outline-none transition-all duration-200 text-gray-600
                            ${error ? "border-red-500 focus:border-red-600"
                                    : isValid
                                    ? "border-green-500 focus:border-green-600"
                                    : "border-gray-300 focus:border-blue-500"
                            }
                        `}
                    aria-invalid={!!error}
                    aria-describedby="url-error"
                />
                {error && (
                    <span id="url-error" className="text-red-600 text-sm mt-1">{error}</span>
                )}
            </div>
            <button 
                type="submit"
                disabled={isLoading || !isValid}
                className="px-6 py-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700
                            disabled:bg-gray-400 disabled:cursor-not-allowed transition-all duration-200"
            >
                {isLoading ? "Shortening" : "Shorten URL"}
            </button>
        </form>
    );
};

export default UrlForm;