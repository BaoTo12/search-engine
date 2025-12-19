import { SearchBox } from '@/components/search-box';
import { Search } from 'lucide-react';

export default function HomePage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-8 bg-white dark:bg-gray-900">
      {/* Logo */}
      <div className="mb-12">
        <div className="flex items-center gap-3">
          <div className="rounded-full bg-gradient-to-br from-blue-600 to-purple-600 p-4">
            <Search className="h-12 w-12 text-white" />
          </div>
          <h1 className="text-5xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
            SearchEngine
          </h1>
        </div>
        <p className="mt-4 text-center text-gray-600 dark:text-gray-400">
          Enterprise-grade distributed search engine
        </p>
      </div>

      {/* Search Box */}
      <SearchBox autoFocus />

      {/* Quick Stats */}
      <div className="mt-16 grid grid-cols-3 gap-8 text-center">
        <div>
          <div className="text-3xl font-bold text-blue-600 dark:text-blue-400">Fast</div>
          <div className="mt-1 text-sm text-gray-600 dark:text-gray-400">~500ms response time</div>
        </div>
        <div>
          <div className="text-3xl font-bold text-purple-600 dark:text-purple-400">Smart</div>
          <div className="mt-1 text-sm text-gray-600 dark:text-gray-400">PageRank + TF-IDF</div>
        </div>
        <div>
          <div className="text-3xl font-bold text-pink-600 dark:text-pink-400">Scalable</div>
          <div className="mt-1 text-sm text-gray-600 dark:text-gray-400">100+ pages/sec</div>
        </div>
      </div>

      {/* Footer */}
      <footer className="absolute bottom-8 text-sm text-gray-500 dark:text-gray-600">
        Built with Spring Boot, Kafka, Elasticsearch & Next.js
      </footer>
    </main>
  );
}
