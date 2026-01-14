"use client";

import { useState } from 'react';
import AuthModal from '@/components/auth/AuthModal';
import RegisterFormDebug from '@/components/auth/RegisterFormDebug';
import LoginForm from '@/components/auth/LoginForm';
import { useAuth } from '@/contexts/AuthenticationContext';

export default function TestAuth() {
  const [showModal, setShowModal] = useState(false);
  const [showDebugForm, setShowDebugForm] = useState(false);
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const { user, isAuthenticated, logout } = useAuth();

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <div className="max-w-4xl mx-auto space-y-8">
        {/* Status Section */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h1 className="text-2xl font-bold mb-4">Authentication Test</h1>
          
          {isAuthenticated ? (
            <div className="space-y-4">
              <div className="p-4 bg-green-100 rounded-lg">
                <h2 className="font-semibold text-green-800">Logged In!</h2>
                <p className="text-green-700">Welcome, {user?.firstName} {user?.lastName}</p>
                <p className="text-sm text-green-600">{user?.email}</p>
              </div>
              <button
                onClick={logout}
                className="w-full bg-red-600 text-white py-2 px-4 rounded-lg hover:bg-red-700"
              >
                Logout
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              <p className="text-gray-600">Not logged in</p>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <h3 className="font-semibold">Modal Tests</h3>
                  <button
                    onClick={() => {
                      setMode('login');
                      setShowModal(true);
                    }}
                    className="w-full bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700"
                  >
                    Test Login Modal
                  </button>
                  <button
                    onClick={() => {
                      setMode('register');
                      setShowModal(true);
                    }}
                    className="w-full bg-green-600 text-white py-2 px-4 rounded-lg hover:bg-green-700"
                  >
                    Test Register Modal
                  </button>
                </div>
                <div className="space-y-2">
                  <h3 className="font-semibold">Debug Forms</h3>
                  <button
                    onClick={() => setShowDebugForm(!showDebugForm)}
                    className="w-full bg-purple-600 text-white py-2 px-4 rounded-lg hover:bg-purple-700"
                  >
                    {showDebugForm ? 'Hide' : 'Show'} Debug Register Form
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Debug Form Section */}
        {showDebugForm && !isAuthenticated && (
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-bold mb-4">Debug Register Form (Inline)</h2>
            <RegisterFormDebug 
              onSuccess={() => {
                setShowDebugForm(false);
              }}
            />
          </div>
        )}

        {/* Inline Login Form for Testing */}
        {!isAuthenticated && (
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-bold mb-4">Inline Login Form</h2>
            <div className="max-w-md">
              <LoginForm onSuccess={() => {}} />
            </div>
          </div>
        )}
      </div>

      <AuthModal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        initialMode={mode}
      />
    </div>
  );
}