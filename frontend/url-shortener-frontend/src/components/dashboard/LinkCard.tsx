/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import api from "@/lib/api";
import { UrlMappingResponse } from "@/types/api";
import { CalendarIcon, ChartBarIcon, CheckIcon, ClipboardIcon, ExclamationCircleIcon, LinkIcon, TrashIcon } from "@heroicons/react/24/solid";
import React, { useState } from "react";
import toast from "react-hot-toast";
import LinkStats from "./LinkStats";

interface LinkCardProps {
    link: UrlMappingResponse;
    onDeleted?: () => void;
}

const LinkCard: React.FC<LinkCardProps> = ({link, onDeleted}) => {
    const [isDeleting, setIsDeleting] = useState(false);
    const [copied, setCopied] = useState(false);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [showStats, setShowStats] = useState(false);

    const copyToClipboard = async () => {
        try {
            await navigator.clipboard.writeText(link.shortUrl);
            setCopied(true);
            toast.success('Link copied to clipboard');
            setTimeout(() => setCopied(false), 2000);
        } catch(error) {
            toast.error('Failed to copy link');
        }
    };

    const handleDelete = async () => {
        try {
            setIsDeleting(true);
            await api.deleteUrl(link.shortCode);
            toast.success('Link deleted successfully');
            onDeleted?.();
        } catch(error: any) {
            toast.error(error.message || 'Failed to delete link');
        } finally {
            setIsDeleting(false);
            setShowDeleteConfirm(false);
        }
    };

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const isExpired = link.expiresAt && new Date(link.expiresAt) < new Date();

    return (
        <div className={`bg-white border rounded-lg p-4 hover:shadow-md transition-shadow ${
            !link.isActive ? 'bg-gray-50 border-gray-300' : 'border-gray-200'
        }`}>
            <div className="flex items-start justify-between">
                <div className="flex-1 min-w-0">
                    {/* Short URL */}
                    <div className="flex items-center space-x-2 mb-2">
                        <LinkIcon className="w-5 h-5 text-blue-600 flex-shrink-0"/>
                        <a
                            href={link.shortUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-blue-600 hover:text-blue-800 font-medium truncate"
                        >
                            {link.shortUrl}
                        </a>
                        <button
                            onClick={copyToClipboard}
                            className={`p-1 rounded transition-colors ${
                                copied ? 'text-green-600 bg-green-50' : 'text-gray-400 hover:text-gray-600 hover:bg-gray-50'
                            }`}
                            title="Copy Link"
                        >
                            {copied ? (
                                <CheckIcon className="w-4 h-4"/>
                            ) : (
                                <ClipboardIcon className="w-4 h-4"/>
                            )}
                        </button>
                    </div>

                    {/* Original URL */}
                    <p className="text-sm text-gray-600 truncate mb-2" title={link.originalUrl}>
                        {link.originalUrl}
                    </p>

                    {/* Metadata */}
                    <div className="flex items-center space-x-4 text-xs text-gray-500">
                        <div className="flex items-center space-x-1">
                            <CalendarIcon className="w-4 h-4"/>
                            <span>Created {formatDate(link.createdAt)}</span>
                        </div>

                        {link.expiresAt && (
                            <div className={`flex items-center space-x-1 ${
                                isExpired ? 'text-red-600' : 'text-gray-500'
                            }`}>
                                <CalendarIcon className="w-4 h-4"/>
                                <span>
                                    {isExpired ? 'Expired' : 'Expires'} {formatDate(link.expiresAt)}
                                </span>
                            </div>
                        )}
                    </div>
                </div>

                {/* Actions */}
                <div className="flex items-center space-x-2 ml-4">
                    {/* Status Badge */}
                    <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                        !link.isActive ? 'bg-gray-100 text-gray-800'
                        : isExpired ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'
                    }`}>
                        {!link.isActive ? 'Inactive' : isExpired ? 'Expired' : 'Active'}
                    </span>

                    <button
                        onClick={() => setShowStats(true)}
                        className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors"
                        title="View Analytics"
                    >
                        <ChartBarIcon className="w-4 h-4"/>
                    </button>

                    {/* Delete Button */}
                    {!showDeleteConfirm ? (
                        <button
                            onClick={() => setShowDeleteConfirm(true)}
                            className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                            title="Delete link"
                        >
                            <TrashIcon className="w-4 h-4"/>
                        </button>
                    ) : (
                        <div className="flex items-center space-x-1">
                            <button
                                onClick={handleDelete}
                                disabled={isDeleting}
                                className="px-2 py-1 bg-red-600 text-white text-xs rounded hover:bg-red-700 disabled:opacity-50"
                            >
                                {isDeleting? 'Deleting...' : 'Confirm'}
                            </button>
                            <button
                                onClick={() => setShowDeleteConfirm(false)}
                                className="px-2 py-1 bg-gray-300 text-gray-700 text-xs rounded hover:bg-gray-400"
                            >
                                Cancel
                            </button>
                        </div>
                    )}
                </div>
            </div>

            {/* Warning for expired /inactive links */}
            {(!link.isActive || isExpired) && (
                <div className="mt-3 flex items-center space-x-2 text-sm text-amber-600 bg-amber-50 p-2 rounded">
                    <ExclamationCircleIcon className="w-4 h-4 flex-shrink-0"/>
                    <span>
                        This link is {!link.isActive ? 'inactive' : 'expired'} and will not redirect visitors.
                    </span>
                </div>
            )}

            {showStats && (
                <LinkStats 
                    shortCode={link.shortCode}
                    isOpen={showStats}
                    onClose={() => setShowStats(false)}
                />
            )}
        </div>
    );
};

export default LinkCard;