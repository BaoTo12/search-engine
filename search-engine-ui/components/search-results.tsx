'use client';

import { SearchResult } from '@/lib/api';
import { ExternalLink } from 'lucide-react';
import { cn } from '@/lib/utils';

interface SearchResultsProps {
    results: SearchResult[];
    query: string;
    totalResults: number;
    executionTime: number;
}

export function SearchResults({ results, query, totalResults, executionTime }: SearchResultsProps) {
    const highlightText = (text: string, query: string) => {
        if (!query) return text;

        const parts = text.split(new RegExp(`(${query})`, 'gi'));
        return parts.map((part, index) =>
            part.toLowerCase() === query.toLowerCase() ? (
                <mark key={index} className="bg-yellow-200 dark:bg-yellow-900">
                    {part}
                </mark>
            ) : (
                part
            )
        );
    };

    return (
        <div className="w-full max-w-3xl">
            {/* Search Stats */}
            <div className="mb-6 text-sm text-gray-600 dark:text-gray-400">
                About {totalResults.toLocaleString()} results ({(executionTime / 1000).toFixed(2)} seconds)
            </div>

            {/* Results */}
            <div className="space-y-8">
                {results.map((result, index) => (
                    <div key={index} className="group">
                        {/* URL */}
                        <div className="mb-1 flex items-center gap-2 text-sm">
                            <span className="truncate text-gray-600 dark:text-gray-400">
                                {result.domain || new URL(result.url).hostname}
                            </span>
                        </div>

                        {/* Title */}
                        <a
                            href={result.url}
                            target="_blank"
                            rel="noopener noreferrer"
                            className={cn(
                                "mb-1 block text-xl font-medium text-blue-700 hover:underline dark:text-blue-400",
                                "flex items-center gap-2"
                            )}
                        >
                            <span>{highlightText(result.title, query)}</span>
                            <ExternalLink className="h-4 w-4 opacity-0 group-hover:opacity-100 transition-opacity" />
                        </a>

                        {/* Snippet */}
                        <p className="text-sm leading-relaxed text-gray-700 dark:text-gray-300">
                            {highlightText(result.snippet, query)}
                        </p>

                        {/* Score (for debugging, can be hidden in production) */}
                        {process.env.NODE_ENV === 'development' && (
                            <div className="mt-1 text-xs text-gray-400">
                                Score: {result.score.toFixed(4)}
                            </div>
                        )}
                    </div>
                ))}
            </div>

            {/* Empty State */}
            {results.length === 0 && (
                <div className="py-12 text-center">
                    <p className="text-lg text-gray-600 dark:text-gray-400">
                        No results found for <span className="font-semibold">&quot;{query}&quot;</span>
                    </p>
                    <p className="mt-2 text-sm text-gray-500 dark:text-gray-500">
                        Try different keywords or check your spelling
                    </p>
                </div>
            )}
        </div>
    );
}
