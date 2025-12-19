'use client';

import { useState, useEffect, useRef } from 'react';
import { Search, X } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { searchApi } from '@/lib/api';
import { cn } from '@/lib/utils';

interface SearchBoxProps {
    initialQuery?: string;
    autoFocus?: boolean;
    onSearch?: (query: string) => void;
}

export function SearchBox({ initialQuery = '', autoFocus = false, onSearch }: SearchBoxProps) {
    const [query, setQuery] = useState(initialQuery);
    const [suggestions, setSuggestions] = useState<string[]>([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [selectedIndex, setSelectedIndex] = useState(-1);
    const inputRef = useRef<HTMLInputElement>(null);
    const router = useRouter();

    useEffect(() => {
        if (autoFocus && inputRef.current) {
            inputRef.current.focus();
        }
    }, [autoFocus]);

    useEffect(() => {
        const fetchSuggestions = async () => {
            if (query.length < 2) {
                setSuggestions([]);
                return;
            }

            try {
                const results = await searchApi.getSuggestions(query);
                setSuggestions(results.map(s => s.text).slice(0, 8));
            } catch (error) {
                console.error('Error fetching suggestions:', error);
                setSuggestions([]);
            }
        };

        const debounce = setTimeout(fetchSuggestions, 300);
        return () => clearTimeout(debounce);
    }, [query]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (query.trim()) {
            setShowSuggestions(false);
            if (onSearch) {
                onSearch(query);
            } else {
                router.push(`/search?q=${encodeURIComponent(query)}`);
            }
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (!showSuggestions || suggestions.length === 0) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            setSelectedIndex(prev => (prev < suggestions.length - 1 ? prev + 1 : prev));
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setSelectedIndex(prev => (prev > 0 ? prev - 1 : -1));
        } else if (e.key === 'Enter' && selectedIndex >= 0) {
            e.preventDefault();
            setQuery(suggestions[selectedIndex]);
            setShowSuggestions(false);
            if (onSearch) {
                onSearch(suggestions[selectedIndex]);
            } else {
                router.push(`/search?q=${encodeURIComponent(suggestions[selectedIndex])}`);
            }
        } else if (e.key === 'Escape') {
            setShowSuggestions(false);
        }
    };

    return (
        <div className="relative w-full max-w-2xl">
            <form onSubmit={handleSubmit} className="relative">
                <div className="relative flex items-center">
                    <Search className="absolute left-4 h-5 w-5 text-gray-400" />
                    <input
                        ref={inputRef}
                        type="text"
                        value={query}
                        onChange={(e) => {
                            setQuery(e.target.value);
                            setShowSuggestions(true);
                            setSelectedIndex(-1);
                        }}
                        onFocus={() => setShowSuggestions(true)}
                        onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                        onKeyDown={handleKeyDown}
                        placeholder="Search the web..."
                        className={cn(
                            "w-full rounded-full border border-gray-300 py-3 pl-12 pr-12",
                            "text-base focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200",
                            "dark:border-gray-600 dark:bg-gray-800 dark:text-white dark:focus:border-blue-400"
                        )}
                    />
                    {query && (
                        <button
                            type="button"
                            onClick={() => {
                                setQuery('');
                                inputRef.current?.focus();
                            }}
                            className="absolute right-12 p-1 hover:bg-gray-100 rounded-full dark:hover:bg-gray-700"
                        >
                            <X className="h-5 w-5 text-gray-400" />
                        </button>
                    )}
                    <button
                        type="submit"
                        className="absolute right-2 rounded-full bg-blue-600 p-2 hover:bg-blue-700 dark:bg-blue-500 dark:hover:bg-blue-600"
                    >
                        <Search className="h-5 w-5 text-white" />
                    </button>
                </div>
            </form>

            {/* Suggestions Dropdown */}
            {showSuggestions && suggestions.length > 0 && (
                <div className="absolute z-10 mt-2 w-full rounded-lg border border-gray-200 bg-white shadow-lg dark:border-gray-700 dark:bg-gray-800">
                    {suggestions.map((suggestion, index) => (
                        <button
                            key={index}
                            type="button"
                            onClick={() => {
                                setQuery(suggestion);
                                setShowSuggestions(false);
                                if (onSearch) {
                                    onSearch(suggestion);
                                } else {
                                    router.push(`/search?q=${encodeURIComponent(suggestion)}`);
                                }
                            }}
                            className={cn(
                                "w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700",
                                "flex items-center gap-3",
                                index === selectedIndex && "bg-gray-100 dark:bg-gray-700",
                                index === 0 && "rounded-t-lg",
                                index === suggestions.length - 1 && "rounded-b-lg"
                            )}
                        >
                            <Search className="h-4 w-4 text-gray-400" />
                            <span className="text-sm dark:text-white">{suggestion}</span>
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}
