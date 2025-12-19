'use client';

import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';

interface PaginationProps {
    currentPage: number;
    totalPages: number;
    onPageChange: (page: number) => void;
}

export function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
    const getPageNumbers = () => {
        const pages: (number | string)[] = [];
        const maxVisible = 7;

        if (totalPages <= maxVisible) {
            for (let i = 0; i < totalPages; i++) {
                pages.push(i);
            }
        } else {
            pages.push(0); // First page

            if (currentPage > 3) {
                pages.push('...');
            }

            const start = Math.max(1, currentPage - 1);
            const end = Math.min(totalPages - 2, currentPage + 1);

            for (let i = start; i <= end; i++) {
                pages.push(i);
            }

            if (currentPage < totalPages - 4) {
                pages.push('...');
            }

            pages.push(totalPages - 1); // Last page
        }

        return pages;
    };

    if (totalPages <= 1) return null;

    return (
        <div className="flex items-center justify-center gap-2 py-8">
            {/* Previous Button */}
            <button
                onClick={() => onPageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className={cn(
                    "flex items-center gap-1 rounded-lg px-3 py-2 text-sm font-medium",
                    currentPage === 0
                        ? "cursor-not-allowed text-gray-400 dark:text-gray-600"
                        : "text-blue-700 hover:bg-blue-50 dark:text-blue-400 dark:hover:bg-gray-800"
                )}
            >
                <ChevronLeft className="h-4 w-4" />
                <span>Previous</span>
            </button>

            {/* Page Numbers */}
            <div className="flex items-center gap-1">
                {getPageNumbers().map((page, index) =>
                    page === '...' ? (
                        <span key={`ellipsis-${index}`} className="px-3 py-2 text-gray-500">
                            ...
                        </span>
                    ) : (
                        <button
                            key={page}
                            onClick={() => onPageChange(page as number)}
                            className={cn(
                                "h-10 w-10 rounded-lg text-sm font-medium transition-colors",
                                currentPage === page
                                    ? "bg-blue-600 text-white dark:bg-blue-500"
                                    : "text-blue-700 hover:bg-blue-50 dark:text-blue-400 dark:hover:bg-gray-800"
                            )}
                        >
                            {(page as number) + 1}
                        </button>
                    )
                )}
            </div>

            {/* Next Button */}
            <button
                onClick={() => onPageChange(currentPage + 1)}
                disabled={currentPage === totalPages - 1}
                className={cn(
                    "flex items-center gap-1 rounded-lg px-3 py-2 text-sm font-medium",
                    currentPage === totalPages - 1
                        ? "cursor-not-allowed text-gray-400 dark:text-gray-600"
                        : "text-blue-700 hover:bg-blue-50 dark:text-blue-400 dark:hover:bg-gray-800"
                )}
            >
                <span>Next</span>
                <ChevronRight className="h-4 w-4" />
            </button>
        </div>
    );
}
