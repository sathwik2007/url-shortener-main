"use client";

import api from "@/lib/api";
import { AuthResponse, User } from "@/types/api";
import React, { createContext, ReactNode, useContext, useEffect, useState } from "react";
import toast from "react-hot-toast";

interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (email: string, password: string) => Promise<void>;
    register: (email: string, password: string, firstName: string, lastName: string) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if(context === undefined) {
        throw new Error('userAuth must be used within an AuthProvider');
    }
    return context;
}

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({children}) => {
    const [user, setUser] = useState<User | null>(null);
    const[isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        // Initialize auth state from local storage
        const initializeAuth = () => {
            try {
                const token = api.getAuthToken();
                const userData = localStorage.getItem('user_data');

                if(token && userData) {
                    const parsedUser = JSON.parse(userData);
                    setUser(parsedUser);
                }
            } catch(error) {
                console.error('Error initializing auth', error);
                api.clearAuthToken();
            } finally {
                setIsLoading(false);
            }
        };

        initializeAuth();

        // Listens for unauthorized events and handle them
        const handleUnauthorized = () => {
            setUser(null);
            toast.error('Session expired. Please login again');
        };

        window.addEventListener('auth:unauthorized', handleUnauthorized);
        return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
    }, []);

    const login = async (email: string, password: string): Promise<void> => {
        try {
            setIsLoading(true);
            const authData: AuthResponse = await api.login({email, password});

            const userData: User = {
                id: authData.id,
                email: authData.email,
                firstName: authData.firstName,
                lastName: authData.lastName
            };

            setUser(userData);
            toast.success(`Welcome back, ${userData.firstName}!`);
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } catch(error: any) {
            toast.error(error.message || 'Login failed');
            throw error;
        } finally {
            setIsLoading(false);
        }
    };

    const register = async(email: string, password: string, firstName: string, lastName: string): Promise<void> => {
        try {
            setIsLoading(true);
            const authData: AuthResponse = await api.register({email, password, firstName, lastName});

            const userData: User = {
                id: authData.id,
                email: authData.email,
                firstName: authData.firstName,
                lastName: authData.lastName
            };

            setUser(userData);
            toast.success(`Welcome ${userData.firstName}! Your account has been created`);
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } catch(error: any) {
            toast.error(error.message || 'Registration failed');
            throw error;
        } finally {
            setIsLoading(false);
        }
    }

    const logout = (): void => {
        api.logout();
        setUser(null);
        toast.success('Logged out successfully');
    };

    const value: AuthContextType = {
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout
    };

    return(
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    )
}

