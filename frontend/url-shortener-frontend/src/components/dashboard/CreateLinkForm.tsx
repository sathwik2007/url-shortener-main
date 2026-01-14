/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import { useAuth } from "@/contexts/AuthenticationContext";
import api from "@/lib/api";
import { CalendarIcon, XMarkIcon } from "@heroicons/react/24/solid";
import React, { useState } from "react";
import toast from "react-hot-toast";

interface CreateLinkFormProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

const CreateLinkForm: React.FC<CreateLinkFormProps> = ({isOpen, onClose, onSuccess}) => {
    const {isAuthenticated} = useAuth();
    const [formData, setFormData] = useState({
        originalUrl: '',
        expiresAt: ''
    });
    const [errors, setErrors] = useState<Record<string, string>>({});
    const [isLoading, setIsLoading] = useState(false);

    const validateForm = (): boolean => {
        const newErrors: Record<string, string> = {}

        if(!formData.originalUrl.trim()) {
            newErrors.originalUrl = 'URL is required';
        } else {
            try {
                const url = new URL(formData.originalUrl);
                if(!['http:', 'https:'].includes(url.protocol)) {
                    newErrors.originalUrl = 'URL must use HTTP or HTTPS protocol';
                }
            } catch {
                newErrors.originalUrl = 'Please enter a valid URL';
            }
        }

        if(formData.expiresAt) {
            const expirationDate = new Date(formData.expiresAt);
            if(expirationDate <= new Date()) {
                newErrors.expiresAt = 'Expiration date must be in the future';
            }
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if(!validateForm()) return;

        try {
            setIsLoading(true);
            await api.createShortUrl(
                formData.originalUrl,
                formData.expiresAt || undefined
            );

            toast.success('Link created successfully');
            setFormData({originalUrl: '', expiresAt: ''});
            setErrors({});
            onSuccess();
        } catch(error: any) {
            toast.error(error.message || 'Failed to create link');
        } finally {
            setIsLoading(false);
        }
    };

    const handleInputChange = (field: string, value: string) => {
        setFormData(prev => ({...prev, [field]: value}));
        if(errors[field]) {
            setErrors(prev => ({...prev, [field]: ''}));
        }
    };

    if(!isOpen) return null;
    return (
      <div className="fixed inset-0 z-50 overflow-y-auto">
        <div className="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0">
          {/* Background Overlay */}
          <div
            className="fixed inset-0 bg-gray-500/40 backdrop-blur-sm z-40"
            onClick={onClose}
          />

          {/* Modal Panel */}
          <div className="relative z-50 inline-block w-full max-w-md p-6 my-8 overflow-hidden text-left align-middle transition-all transform bg-white shadow-xl rounded-2xl">
            {/* Header */}
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-medium text-gray-900">
                Create New Link
              </h3>
              <button
                onClick={onClose}
                className="text-gray-400 hover:text-gray-600 transition-colors"
              >
                <XMarkIcon className="w-6 h-6" />
              </button>
            </div>

            {/* Form */}
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Original URL */}
              <div>
                <label
                  htmlFor="originalUrl"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Original URL
                </label>
                <input
                  id="originalUrl"
                  type="url"
                  value={formData.originalUrl}
                  onChange={(e) =>
                    handleInputChange("originalUrl", e.target.value)
                  }
                  className={`w-full px-3 py-2 text-gray-700 border rounded-lg focus:outline-none focus:ring-2 transition-colors ${
                    errors.originalUrl
                      ? "border-red-500 focus:ring-red-500"
                      : "border-gray-300 focus:ring-blue-500"
                  }`}
                  placeholder="https://example.com/very-long-url"
                  disabled={isLoading}
                />
                {errors.originalUrl && (
                  <p className="text-red-500 text-sm mt-1">
                    {errors.originalUrl}
                  </p>
                )}
              </div>

              {/* Expiration Date */}
              <div>
                <label
                  htmlFor="expiresAt"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Expiration Date (Optional)
                </label>
                <div className="relative">
                  <input
                    id="expiresAt"
                    type="datetime-local"
                    value={formData.expiresAt}
                    onChange={(e) =>
                      handleInputChange("expiresAt", e.target.value)
                    }
                    min={new Date().toISOString().slice(0, 16)}
                    className={`w-full px-3 py-2 pr-2 text-gray-500 border rounded-lg focus:outline-none focus:ring-2 transition-colors ${
                      errors.expiresAt
                        ? "border-red-500 focus:ring-red-500"
                        : "border-gray-300 focus:ring-blue-500"
                    }`}
                    disabled={isLoading}
                  />
                  {/* <CalendarIcon className="absolute right-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none" /> */}
                </div>
                {errors.expiresAt && (
                  <p className="text-red-500 text-sm mt-1">
                    {errors.expiresAt}
                  </p>
                )}
                <p className="text-xs text-gray-500 mt-1">
                  Leave empty for permanent links
                </p>
              </div>

              {/* Submit Button */}
              <div className="flex space-x-3 pt-4">
                <button
                  type="button"
                  onClick={onClose}
                  className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
                  disabled={isLoading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {isLoading ? "Creating..." : "Create Link"}
                </button>
              </div>
            </form>

            {/* Info */}
            {isAuthenticated && (
              <div className="mt-4 p-3 bg-blue-50 rounded-lg">
                <p className="text-sm text-blue-700">
                  This link will be saved to your account and you can manage it
                  from your dashboard.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    );
};

export default CreateLinkForm;