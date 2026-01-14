/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import api from "@/lib/api";
import { ClickStats } from "@/types/api";
import { ChartBarIcon, EyeIcon } from "@heroicons/react/24/solid";
import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import ClickChart from "./ClickChart";

interface LinkStatsProps {
    shortCode: string;
    isOpen: boolean;
    onClose: () => void;
}

const LinkStats: React.FC<LinkStatsProps> = ({shortCode, isOpen, onClose}) => {
    const [stats, setStats] = useState<ClickStats | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        if(isOpen && shortCode) {
            fetchStats();
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isOpen, shortCode]);

    const fetchStats = async () => {
        try {
            setIsLoading(true);
            const clickStats = await api.getLinkStats(shortCode);
            setStats(clickStats);
        } catch (error: any) {
            console.error('Error fetching stats', error);
            toast.error('Failed to load analytics data');
        } finally {
            setIsLoading(false);
        }
    };

    if(!isOpen) return null;

    return (
      <div className="fixed inset-0 z-50 overflow-y-auto">
        <div className="flex items items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0">

          {/* Background overlay */}
          <div
            className="fixed inset-0 bg-gray-500/40 backdrop-blur-sm"
            onClick={onClose}
          />
            {/* Modal Panel */}
            <div className="relative inline-block w-full max-w-2xl p-6 my-8 overflow-hidden text-left align-middle transition-all transform bg-white shadow-xl rounded-2xl">
              {/* Header */}
              <div className="flex items-center justify-between mb-6">
                <div className="flex items-center space-x-3">
                  <ChartBarIcon className="w-6 h-6 text-blue-600" />
                  <div>
                    <h3 className="text-lg font-medium text-gray-900">
                      Link Analytics
                    </h3>
                    <p className="text-sm text-gray-600">/{shortCode}</p>
                  </div>
                </div>
                <button
                  onClick={onClose}
                  className="text-gray-400 hover:text-gray-600 transition-colors"
                >
                  <svg
                    className="w-6 h-6"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M6 18L18 6M6 6l12 12"
                    />
                  </svg>
                </button>
              </div>

              {/* Stats Summary */}
                {!isLoading && stats && (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
                        <div className="bg-blue-50 rounded-lg p-4">
                            <div className="flex items-center">
                                <EyeIcon className="w-8 h-8 text-blue-600"/>
                                <div className="ml-3">
                                    <p className="text-sm font-medium text-blue-600">Total Clicks</p>
                                    <p className="text-2xl font-bold text-blue-900">{stats.totalClicks}</p>
                                </div>
                            </div>
                        </div>

                        <div className="bg-green-50 rounded-lg p-4">
                            <div className="flex items-center">
                                <ChartBarIcon className="w-8 h-8 text-green-600"/>
                                <div className="ml-3">
                                    <p className="text-sm font-medium text-green-600">Avg. Daily</p>
                                    <p className="text-2xl font-bold text-green-900">
                                        {stats.dailyStats.length > 0
                                            ? Math.round(stats.totalClicks / stats.dailyStats.length)
                                            : 0
                                        }
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* Chart */}
                <ClickChart stats={stats} isLoading={isLoading}/>

                {/* Close Button */}
                <div className="mt-6 flex justify-end">
                    <button
                        onClick={onClose}
                        className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
      </div>
    );
};

export default LinkStats;