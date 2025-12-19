'use client';

import { useState, useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { SearchBox } from '@/components/search-box';
import { SearchResults } from '@/components/search-results';
import { Pagination } from '@/components/pagination';
import { searchApi } from '@/lib/api';
import { Search, Loader2 } from 'lucide-react';

function SearchPageContent() {
    const searchParams = useSearchParams();
    const queryParam = searchParams.get('q') || '';
    const [query, setQuery] = useState(queryParam);
    const [page, setPage] = useState(0);

    // Update query when URL changes
    useEffect(() => {
        if (queryParam) {
            setQuery(queryParam);
            setPage(0);
        }
    }, [queryParam]);

    // Fetch search results
    const { data, isLoading, error } = useQuery({
        queryKey: ['search', query, page],
        queryFn: () => searchApi.search(query, page, 10),
        enabled: !!query,
    });

    const handleSearch = (newQuery: string) => {
        setQuery(newQuery);
        setPage(0);
        window.history.pushState({}, '', `/search?q=${encodeURIComponent(newQuery)}`);
    };

    return (
        <div className="min-h-screen bg-white dark:bg-gray-900">
            {/* Header */}
            <header className="border-b border-gray-200 dark:border-gray-800">
                <div className="container mx-auto flex items-center gap-8 px-6 py-4">
                    {/* Logo */}
                    <a href="/" className="flex items-center gap-2">
                        <div className="rounded-full bg-gradient-to-br from-blue-600 to-purple-600 p-2">
                            <Search className="h-6 w-6 text-white" />
                        </div>
                        <span className="text-xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
                            Search
                        </span>
                    </a>

                    {/* Search Box */}
                    <div className="flex-1">
                        <SearchBox initialQuery={query} onSearch={handleSearch} />
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="container mx-auto px-6 py-8">
                {isLoading && (
                    <div className="flex flex-col items-center justify-center py-20">
                        <Loader2 className="h-12 w-12 animate-spin text-blue-600" />
                        <p className="mt-4 text-gray-600 dark:text-gray-400">Searching...</p>
                    </div>
                )}

                {error && (
                    <div className="py-12 text-center">
                        <p className="text-lg text-red-600 dark:text-red-400">
                            Error: {(error as Error).message}
                        </p>
                        <p className="mt-2 text-sm text-gray-500">
                            Please check if the backend server is running
                        </p>
                    </div>
                )}

                {data && !isLoading && (
                    <>
                        <SearchResults
                            results={data.results}
                            query={query}
                            totalResults={data.totalResults}
                            executionTime={data.executionTimeMs}
                        />

                        {data.totalPages > 1 && (
                            <Pagination
                                currentPage={page}
                                totalPages={data.totalPages}
                                onPageChange={setPage}
                            />
                        )}
                    </>
                )}

                {!query && !isLoading && (
                    <div className="py-20 text-center">
                        <Search className="mx-auto h-16 w-16 text-gray-300 dark:text-gray-700" />
                        <p className="mt-4 text-lg text-gray-600 dark:text-gray-400">
                            Enter a search query to get started
                        </p>
                    </div>
                )}
            </main>
        </div>
    );
}

export default function SearchPage() {
    return (
        <Suspense fallback={
            <div className="flex min-h-screen items-center justify-center">
                <Loader2 className="h-12 w-12 animate-spin text-blue-600" />
            </div>
        }>
            <SearchPageContent />
        </Suspense>
    );
}
