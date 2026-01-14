"use client";

import { useState } from 'react';

export default function SimpleTest() {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  return (
    <div className="min-h-screen bg-gray-100 p-8">
      <div className="max-w-md mx-auto bg-white rounded-lg shadow-md p-6">
        <h1 className="text-2xl font-bold mb-6 text-center">Simple Form Test</h1>
        
        <div className="space-y-4">
          {/* First Name */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              First Name
            </label>
            <input
              type="text"
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Enter first name"
            />
            <p className="text-xs text-gray-500 mt-1">Value: "{firstName}"</p>
          </div>

          {/* Last Name */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Last Name
            </label>
            <input
              type="text"
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Enter last name"
            />
            <p className="text-xs text-gray-500 mt-1">Value: "{lastName}"</p>
          </div>

          {/* Email */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Enter email"
            />
            <p className="text-xs text-gray-500 mt-1">Value: "{email}"</p>
          </div>

          {/* Password */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Enter password"
            />
            <p className="text-xs text-gray-500 mt-1">Value: "{password}"</p>
          </div>

          <button className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700">
            Test Button
          </button>
        </div>

        {/* Debug Info */}
        <div className="mt-6 p-4 bg-gray-50 rounded-md">
          <h3 className="font-semibold mb-2">Debug Info:</h3>
          <div className="text-sm space-y-1">
            <p>First Name: "{firstName}"</p>
            <p>Last Name: "{lastName}"</p>
            <p>Email: "{email}"</p>
            <p>Password: "{password}"</p>
            <p>Window width: {typeof window !== 'undefined' ? window.innerWidth : 'N/A'}</p>
          </div>
        </div>
      </div>
    </div>
  );
}