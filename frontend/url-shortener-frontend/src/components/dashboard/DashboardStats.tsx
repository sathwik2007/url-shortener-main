/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import api from "@/lib/api";
import { UserUrlSummary } from "@/types/api";
import { CheckCircleIcon, LinkIcon, XCircleIcon } from "@heroicons/react/24/solid";
import { useEffect, useState } from "react";
import toast from "react-hot-toast";

interface DashboardStatsProps {
    refreshTrigger?: number;
}

const DashboardStats: React.FC<DashboardStatsProps> = ({ refreshTrigger = 0 }) => {
    const [stats, setStats] = useState<UserUrlSummary | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    
    useEffect(() => {
        const fetchStats = async () => {
            try {
                setIsLoading(true);
                const summary = await api.getUserUrlSummary();
                setStats(summary);
            } catch(error: any) {
                console.error('Error fetching stats', error);
                toast.error('Failed to load dashboard stats');
            } finally {
                setIsLoading(false);
            }
        };
    fetchStats();
    }, [refreshTrigger]);

    if(isLoading) {
        return (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {[1, 2, 3].map((i) => (
                    <div key={i} className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                        <div className="animate-pulse">
                            <div className="h-4 bg-gray-200 rounded w-1/2 mb-2"></div>
                            <div className="h-8 bg-gray-200 rounded w-1/3"></div>
                        </div>
                    </div>
                ))}
            </div>
        );
    }

    const statCards = [
      {
        title: "Total Links",
        value: stats?.totalUrls || 0,
        icon: LinkIcon,
        color: "text-blue-600",
        bgColor: "bg-blue-50",
      },
      {
        title: "Active Links",
        value: stats?.activeUrls || 0,
        icon: CheckCircleIcon,
        color: "text-green-600",
        bgColor: "bg-green-50",
      },
      {
        title: "Inactive Links",
        value: stats?.inactiveUrls || 0,
        icon: XCircleIcon,
        color: "text-gray-600",
        bgColor: "bg-gray-50",
      },
    ];


    return (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {statCards.map((card) => (
                <div key={card.title} className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                    <div className="flex items-center">
                        <div className={`p-3 rounded-lg ${card.bgColor}`}>
                            <card.icon className={`w-6 h-6 ${card.color}`}/>
                        </div>
                        <div className="ml-4">
                            <p className="text-sm font-medium text-gray-600">{card.title}</p>
                            <p className="text-2xl font-bold text-gray-900">{card.value}</p>
                        </div>
                    </div>
                </div>
            ))}
        </div>
    );
};

export default DashboardStats;