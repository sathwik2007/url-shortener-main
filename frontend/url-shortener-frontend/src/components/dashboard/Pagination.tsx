"use client";

import { ChevronLeftIcon, ChevronRightIcon } from "@heroicons/react/24/solid";

interface PaginationProps {
    currentPage: number;
    totalPages: number;
    onPageChange: (page: number) => void;
}

const Pagination: React.FC<PaginationProps> = ({currentPage, totalPages, onPageChange}) => {
    const getVisiblePages = () => {
        const delta = 2;
        const range = [];
        const rangeWithDots = [];

        for(let i = Math.max(0, currentPage - delta); i <= Math.min(totalPages - 1, currentPage + delta); i++) {
            range.push(i);
        }

        if(range[0] > 0 ) {
            if(range[0] > 1) {
                rangeWithDots.push(0, '...');
            } else {
                rangeWithDots.push(0);
            }
        }

        rangeWithDots.push(...range);

        if(range[range.length - 1] < totalPages - 1) {
            if(range[range.length - 1] < totalPages - 2) {
                rangeWithDots.push('...', totalPages - 1);
            } else {
                rangeWithDots.push(totalPages - 1);
            }
        }

        return rangeWithDots;
    };

    if(totalPages <= 1) return null;

    return (
      <div className="flex items-center justify-between">
        <div className="flex-1 flex justify-between sm:hidden">
          <button
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage === 0}
            className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Previous
          </button>
          <button
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage === totalPages - 1}
            className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Next
          </button>
        </div>

        <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-gray-700">
              Showing page{" "}
              <span className="font-medium">{currentPage + 1}</span> of{" "}
              <span className="font-medium">{totalPages}</span>
            </p>
          </div>
          <div>
            <nav
              className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px"
              aria-label="Pagination"
            >
              <button
                onClick={() => onPageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <ChevronLeftIcon className="h-5 w-5" />
              </button>

              {getVisiblePages().map((page, index) => (
                <button
                  key={index}
                  onClick={() =>
                    typeof page === "number" ? onPageChange(page) : undefined
                  }
                  disabled={page === "..."}
                  className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                    page === currentPage
                      ? "z-10 bg-blue-50 border-blue-500 text-blue-600"
                      : page === "..."
                      ? "border-gray-300 bg-white text-gray-500 cursor-default"
                      : "border-gray-300 bg-white text-gray-500 hover:bg-gray-50"
                  }`}
                >
                    {typeof page === 'number' ? page + 1 : page}
                </button>
              ))}

              <button
                onClick={() => onPageChange(currentPage + 1)}
                disabled={currentPage === totalPages - 1}
                className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <ChevronRightIcon className="h-5 w-5"/>
              </button>
            </nav>
          </div>
        </div>
      </div>
    );
}

export default Pagination;