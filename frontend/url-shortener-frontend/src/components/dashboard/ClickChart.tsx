"use client";

import { ClickStats } from "@/types/api";
import { ChartBarIcon } from "@heroicons/react/24/solid";
import React from "react";

interface ClickChartProps {
    stats: ClickStats | null;
    isLoading?: boolean;
    className?: string;
}

const ClickChart: React.FC<ClickChartProps> = ({
    stats,
    isLoading = false,
    className = ""
}) => {
    if(isLoading) {
        return (
            <div className={`bg-white rounded-lg border border-gray-200 p-6 ${className}`}>
                <div className="animate-pulse">
                    <div className="h-4 bg-gray-200 rounded w-1/3 mb-4"></div>
                    <div className="h-32 bg-gray-200 rounded"></div>
                </div>
            </div>
        );
    }

    if(!stats || stats.dailyStats.length === 0) {
        return (
          <div className={`bg-white rounded-lg border border-gray-200 p-6 ${className}`}>
            <div className="text-center py-8">
                <ChartBarIcon className="w-12 h-12 text-gray-400 mx-auto mb-4"/>
                <h3 className="text-lg font-medium text-gray-900 mb-2">No Data Available</h3>
                <p className="text-gray-600">No click data to display yet.</p>
            </div>
          </div>
        );
    }

    const maxClicks = Math.max(...stats.dailyStats.map(stat => stat.clicks));
    const chartHeight = 120;

    const formatDate = (dateString: string) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric'
        });
    };

    return (
      <div className={`bg-white rounded-lg border border-gray-200 p-6 ${className}`}>
        <div className="mb-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
                7-Day Click History
            </h3>
            <p className="text-sm text-gray-600">
                Total Clicks: <span className="font-medium">{stats.totalClicks}</span>
            </p>
        </div>

        <div className="relative">
            {/* Chart */}
            <div className="flex items-end justify-between space-x-2" style={{height: chartHeight}}>
                {stats.dailyStats.map((stat, index) => {
                    const barHeight = maxClicks > 0 ? (stat.clicks / maxClicks) * chartHeight : 0;

                    return (
                        <div key={stat.date} className="flex-1 flex flex-col items-center">
                            {/* Bar */}
                            <div className="w-full flex flex-col justify-end" style={{height: chartHeight}}>
                                <div className="bg-blue-500 rounded-t transition-all duration-300 hover:bg-blue-600 relative group"
                                style={{height: `${barHeight}px`, minHeight: stat.clicks > 0 ? '4px' : '0px'}}>
                                    {/* Tooltip */}
                                    <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-2 py-1 bg-gray-800 text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
                                        {stat.clicks} clicks
                                    </div>
                                </div>
                            </div>

                            {/* Date Label */}
                            <div className="mt-2 text-xs text-gray-600 text-center">
                                {formatDate(stat.date)}
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* Y-axis labels */}
            <div className="absolute left-0 top-0 h-full flex flex-col justify-between text-xs text-gray-500 -ml-8">
                <span>{maxClicks}</span>
                <span>{Math.floor(maxClicks / 2)}</span>
                <span>0</span>
            </div>
        </div>
      </div>
    );
};

export default ClickChart;