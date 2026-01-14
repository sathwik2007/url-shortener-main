/* eslint-disable @typescript-eslint/no-explicit-any */
"use client";

import api from "@/lib/api";
import { PaginatedResponse, UrlMappingResponse } from "@/types/api";
import { LinkIcon, MagnifyingGlassIcon } from "@heroicons/react/24/outline";
import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import LinkCard from "./LinkCard";
import Pagination from "./Pagination";

interface LinksListProps {
  refreshTrigger?: number;
  onLinkDeleted?: () => void;
}

const LinksList: React.FC<LinksListProps> = ({
  refreshTrigger = 0,
  onLinkDeleted,
}) => {
  const [links, setLinks] =
    useState<PaginatedResponse<UrlMappingResponse> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);
  const [activeOnly, setActiveOnly] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");

  const fetchLinks = async (page: number = currentPage) => {
    try {
      setIsLoading(true);
      const response = await api.getUserUrls(page, pageSize, activeOnly);
      setLinks(response);
    } catch (error: any) {
      console.error("Error fetching links: ", error);
      toast.error("Failed to load links");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLinks(0);
    setCurrentPage(0);
  }, [refreshTrigger, activeOnly]);

  useEffect(() => {
    fetchLinks(currentPage);
  }, [currentPage]);

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  const handleLinkDeleted = (shortCode: string) => {
    // Remove the deleted link from the current list
    if (links) {
      const updatedContent = links.content.filter(
        (link) => link.shortCode != shortCode
      );
      setLinks({
        ...links,
        content: updatedContent,
        totalElements: links.totalElements - 1,
      });
    }
    onLinkDeleted?.();
  };

  // Filter links based on search term
  const filteredLinks =
    links?.content.filter(
      (link) =>
        link.originalUrl.toLowerCase().includes(searchTerm.toLowerCase()) ||
        link.shortCode.toLowerCase().includes(searchTerm.toLowerCase())
    ) || [];

  if (isLoading && !links) {
    return (
      <div className="p-6">
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="animate-pulse">
              <div className="h-20 bg-gray-200 rounded-lg"></div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Filters and Search */}
      <div className="flex flex-col sm:flex-row gap-4 mb-6">
        {/* Search */}
        <div className="relative flex-1">
          <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
          <input
            type="text"
            placeholder="Search links..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {/* Active Filter */}
        <div className="flex items-center space-x-4">
          <label className="flex items-center">
            <input
              type="checkbox"
              checked={activeOnly}
              onChange={(e) => setActiveOnly(e.target.checked)}
              className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            <span className="ml-2 text-sm text-gray-700">Active only</span>
          </label>
        </div>
      </div>

      {/* Links List */}
      {filteredLinks.length === 0 ? (
        <div className="text-center py-12">
          <LinkIcon className="w-12 h-12 text-gray-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">
            {searchTerm ? "No links found" : "No links yet"}
          </h3>
          <p className="text-gray-600">
            {searchTerm
              ? "Try adjusting your search terms"
              : "Create your first shortened link to get shortened"}
          </p>
        </div>
      ) : (
        <>
          <div className="space-y-4">
            {filteredLinks.map((link) => (
              <LinkCard
                key={link.shortCode}
                link={link}
                onDeleted={() => handleLinkDeleted(link.shortCode)}
              />
            ))}
          </div>

          {/* Pagination */}
          {links && links.totalPages > 1 && (
            <div className="mt-6">
              <Pagination
                currentPage={currentPage}
                totalPages={links.totalPages}
                onPageChange={handlePageChange}
              />
            </div>
          )}
        </>
      )}

      {/* Loading Overlay */}
      {isLoading && links && (
        <div className="absolute inset-0 bg-white bg-opacity-50 flex items-center justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      )}
    </div>
  );
};

export default LinksList;
