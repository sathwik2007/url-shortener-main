"use client";

import { useAuth } from "@/contexts/AuthenticationContext";
import { EyeIcon, EyeSlashIcon } from "@heroicons/react/24/solid";
import React, { useState } from "react";

interface RegisterFormProps {
    onSuccess?: () => void;
    onSwitchToLogin?: () => void;
}

const RegisterForm: React.FC<RegisterFormProps> = ({onSuccess, onSwitchToLogin}) => {
    const {register, isLoading} = useAuth();
    const [formData, setFormData] = useState({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        confirmPassword: ''
    });
    const [errors, setErrors] = useState<Record<string, string>>({});
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const validateForm = (): boolean => {
        const newErrors: Record<string, string> = {};

        if(!formData.firstName.trim()) {
            newErrors.firstName = 'First Name is required';
        } else if(formData.firstName.trim().length > 100) {
            newErrors.firstName = 'First name must be less than 100 characters';
        }

        if (!formData.lastName.trim()) {
          newErrors.lastName = "Last Name is required";
        } else if (formData.lastName.trim().length > 100) {
          newErrors.lastName = "Last name must be less than 100 characters";
        }

        if (!formData.email.trim()) {
          newErrors.email = "Email is required";
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
          newErrors.email = "Please enter a valid email address";
        }

        if(!formData.password) {
            newErrors.password = 'Password is required';
        } else if(formData.password.length < 8) {
            newErrors.password = 'Password must be at least 8 characters long';
        }

        if(!formData.confirmPassword) {
            newErrors.confirmPassword = 'Please confirm your password';
        } else if(formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = 'Passwords do not match';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if(!validateForm()) {
            return;
        }

        try {
            await register(formData.email, formData.password, formData.firstName, formData.lastName);
            onSuccess?.();
        } catch(error) {
            // Error is handled by auth context
        }
    };

    const handleInputChange = (field: string, value: string) => {
        setFormData(prev => ({...prev, [field]: value}));
        if(errors[field]) {
            setErrors(prev => ({...prev, [field]: ''}));
        }
    };

    return (
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* First Name */}
        <div>
          <label
            htmlFor="firstName"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            First Name
          </label>
          <input
            id="firstName"
            type="text"
            value={formData.firstName}
            onChange={(e) => handleInputChange("firstName", e.target.value)}
            className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 text-gray-800 transition-colors ${
              errors.firstName
                ? "border-red-500 focus:ring-red-500"
                : "border-gray-300 focus:ring-blue-500"
            }`}
            placeholder="First Name"
            disabled={isLoading}
          />
          {errors.firstName && (
            <p className="text-red-500 text-sm mt-1">{errors.firstName}</p>
          )}
        </div>

        {/* Last Name */}
        <div>
          <label
            htmlFor="lastName"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Last Name
          </label>
          <input
            id="lastName"
            type="text"
            value={formData.lastName}
            onChange={(e) => handleInputChange("lastName", e.target.value)}
            className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 text-gray-800 transition-colors ${
              errors.lastName
                ? "border-red-500 focus:ring-red-500"
                : "border-gray-300 focus:ring-blue-500"
            }`}
            placeholder="Last Name"
            disabled={isLoading}
          />
          {errors.lastName && (
            <p className="text-red-500 text-sm mt-1">{errors.lastName}</p>
          )}
        </div>

        <div>
          <label
            htmlFor="email"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Email Address
          </label>
          <input
            id="email"
            type="email"
            value={formData.email}
            onChange={(e) => handleInputChange("email", e.target.value)}
            className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 text-gray-800 transition-colors ${
              errors.email
                ? "border-red-500 focus:ring-red-500"
                : "border-gray-300 focus:ring-blue-500"
            }`}
            placeholder="Enter your email"
            disabled={isLoading}
          />
          {errors.email && (
            <p className="text-red-500 text-sm mt-1">{errors.email}</p>
          )}
        </div>

        <div>
          <label
            htmlFor="password"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Password
          </label>
          <div className="relative">
            <input
              id="password"
              type={showPassword ? "text" : "password"}
              value={formData.password}
              onChange={(e) => handleInputChange("password", e.target.value)}
              className={`w-full px-3 py-2 pr-10 border rounded-lg focus:outline-none focus:ring-2 text-gray-800 transition-colors ${
                errors.password
                  ? "border-red-500 focus:ring-red-500"
                  : "border-gray-300 focus:ring-blue-500"
              }`}
              placeholder="Enter your password"
              disabled={isLoading}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute inset-y-0 right-0 pr-3 flex items-center"
              disabled={isLoading}
            >
              {showPassword ? (
                <EyeSlashIcon className="h-5 w-5 text-gray-400" />
              ) : (
                <EyeIcon className="h-5 w-5 text-gray-400" />
              )}
            </button>
          </div>
          {errors.password && (
            <p className="text-red-500 text-sm mt-1">{errors.password}</p>
          )}
        </div>

        <div>
          <label
            htmlFor="confirmPassword"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Confirm Password
          </label>
          <div className="relative">
            <input
              id="confirmPassword"
              type={showConfirmPassword ? "text" : "password"}
              value={formData.confirmPassword}
              onChange={(e) =>
                handleInputChange("confirmPassword", e.target.value)
              }
              className={`w-full px-3 py-2 pr-10 border rounded-lg focus:outline-none focus:ring-2 text-gray-800 transition-colors ${
                errors.confirmPassword
                  ? "border-red-500 focus:ring-red-500"
                  : "border-gray-300 focus:ring-blue-500"
              }`}
              placeholder="Confirm your password"
              disabled={isLoading}
            />
            <button
              type="button"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              className="absolute inset-y-0 right-0 pr-3 flex items-center"
              disabled={isLoading}
            >
              {showConfirmPassword ? (
                <EyeSlashIcon className="h-5 w-5 text-gray-400" />
              ) : (
                <EyeIcon className="h-5 w-5 text-gray-400" />
              )}
            </button>
          </div>
          {errors.confirmPassword && (
            <p className="text-red-500 text-sm mt-1">
              {errors.confirmPassword}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {isLoading ? "Creating Account..." : "Create Account"}
        </button>

        {onSwitchToLogin && (
          <p className="text-center text-sm text-gray-600">
            Already have an account?{" "}
            <button
              type="button"
              onClick={onSwitchToLogin}
              className="text-blue-600 hover:text-blue-700 font-medium"
            >
              Sign in
            </button>
          </p>
        )}
      </form>
    );
};

export default RegisterForm;