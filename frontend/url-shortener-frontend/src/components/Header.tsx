"use client";

import { useAuth } from "@/contexts/AuthenticationContext";
import { ArrowRightOnRectangleIcon, UserIcon } from "@heroicons/react/24/solid";
import { useState } from "react";
import { useRouter } from "next/navigation";
import AuthModal from "./auth/AuthModal";
import Link from "next/link";

interface HeaderProps {
    onDashboardClick?: () => void;
    usePages?: boolean; // If true, use separate pages instead of modals
}

const Header: React.FC<HeaderProps> = ({onDashboardClick, usePages = false}) => {
    const {user, isAuthenticated, logout} = useAuth();
    const [showAuthModal, setShowAuthModal] = useState(false);
    const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
    const [showUserMenu, setShowUserMenu] = useState(false);
    const router = useRouter();

    const handleAuthClick = (mode: 'login' | 'register') => {
        if (usePages) {
            router.push(`/${mode}`);
        } else {
            setAuthMode(mode);
            setShowAuthModal(true);
        }
    };

    const handleLogOut = () => {
        logout();
        setShowUserMenu(false);
    };

    return (
      <>
        <header className="w-full bg-white shadow-sm border-b border-gray-200">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between items-center h-16">
              {/* Logo */}
              <div className="flex items-center">
                <h1 className="text-xl font-bold text-gray-500">
                  URL Shortener
                </h1>
              </div>
              {/* Navigation */}
              <nav className="flex items-center space-x-4">
                {isAuthenticated ? (
                  <>
                    <Link
                      href="/dashboard"
                      className="text-gray-700 hover:text-blue-600 px-3 py-2 rounded-md text-sm font-medium transition-colors"
                    >
                      Dashboard
                    </Link>
                    {onDashboardClick && (
                      <button
                        onClick={onDashboardClick}
                        className="text-gray-700 hover:text-blue-600 px-3 py-2 rounded-md text-sm font-medium transition-colors"
                      >
                        Dashboard
                      </button>
                    )}
                    {/* User menu */}
                    <div className="relative">
                      <button
                        onClick={() => setShowUserMenu(!showUserMenu)}
                        className="flex items-center space-x-2 text-gray-700 hover:text-blue-600 px-3 py-2 rounded-md text-sm font-medium transition-colors"
                      >
                        <UserIcon className="w-5 h-5" />
                        <span>{user?.firstName}</span>
                      </button>

                      {showUserMenu && (
                        <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg py-1 z-50 border border-gray-200">
                          <div className="px-4 py-2 text-sm text-gray-700 border-b border-gray-100">
                            <p className="font-medium">
                              {user?.firstName} {user?.lastName}
                            </p>
                            <p className="text-gray-500">{user?.email}</p>
                          </div>
                          <button
                            onClick={handleLogOut}
                            className="flex items-center w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition-colors"
                          >
                            <ArrowRightOnRectangleIcon className="w-4 h-4 mr-2" />
                            Sign Out
                          </button>
                        </div>
                      )}
                    </div>
                  </>
                ) : (
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => handleAuthClick("login")}
                      className="text-gray-700 hover:text-blue-600 px-3 py-2 rounded-md text-sm font-medium transition-colors"
                    >
                      Sign In
                    </button>
                    <button
                      onClick={() => handleAuthClick("register")}
                      className="bg-blue-600 text-white hover:bg-blue-700 px-4 py-2 rounded-md text-sm font-medium transition-colors"
                    >
                      Sign Up
                    </button>
                  </div>
                )}
              </nav>
            </div>
          </div>

          {/* Click outside to close user menu */}
          {showUserMenu && (
            <div className="fixed inset-0 z-40" onClick={() => setShowUserMenu(false)}/>
          )}
        </header>

        {/* Auth Modal - only show if not using pages */}
        {!usePages && (
          <AuthModal 
              isOpen={showAuthModal}
              onClose={() => setShowAuthModal(false)}
              initialMode={authMode}
          />
        )}
      </>
    );
}

export default Header;