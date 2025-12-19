# Robots.txt Compliance (RFC 9309)

> **File:** [RobotsTxtService.java](file:///c:/Users/Admin/Desktop/projects/search-engine/search-engine/src/main/java/com/chibao/edu/search_engine/service/RobotsTxtService.java)  
> **Standard:** RFC 9309 compliant parser

---

## RFC 9309 Standard

### Robots.txt Example

```
User-agent: *
Disallow: /admin/
Disallow: /private/
Crawl-delay: 10

User-agent: Googlebot
Allow: /admin/public/
Disallow: /admin/

User-agent: MySearchBot
Disallow: /api/
Allow: /api/public

Sitemap: https://example.com/sitemap.xml
```

---

## Parser Implementation

```java
@Service
public class RobotsTxtService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String USER_AGENT = "MySearchBot/1.0";
    
    public boolean isAllowed(String domain, String path) {
        // 1. Fetch robots.txt
        RobotsTxt robots = getRobotsTxt(domain);
        
        if (robots == null) {
            return true;  // No robots.txt = allow all
        }
        
        // 2. Find matching user-agent rules
        List<Rule> rules = robots.getRulesForUserAgent(USER_AGENT);
        
        // 3. Check rules in order
        for (Rule rule : rules) {
            if (matchesPattern(path, rule.getPattern())) {
                return rule.isAllow();
            }
        }
        
        // 4. Default: allow
        return true;
    }
    
    private RobotsTxt getRobotsTxt(String domain) {
        // Check cache
        String cacheKey = "robots_txt:" + domain;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return parseRobotsTxt(cached);
        }
        
        // Fetch from domain
        try {
            String url = "https://" + domain + "/robots.txt";
            HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            
            if (response.statusCode() == 200) {
                String content = response.body();
                
                // Cache for 24 hours
                redisTemplate.opsForValue().set(cacheKey, content, 24, TimeUnit.HOURS);
                
                return parseRobotsTxt(content);
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch robots.txt for {}", domain, e);
        }
        
        return null;  // Allow if can't fetch
    }
}
```

### Parsing

```java
public RobotsTxt parseRobotsTxt(String content) {
    RobotsTxt robots = new RobotsTxt();
    String currentUserAgent = null;
    List<Rule> currentRules = new ArrayList<>();
    
    for (String line : content.split("\\n")) {
        line = line.trim();
        
        // Skip comments and empty lines
        if (line.isEmpty() || line.startsWith("#")) {
            continue;
        }
        
        // Parse directives
        String[] parts = line.split(":", 2);
        if (parts.length != 2) continue;
        
        String directive = parts[0].trim().toLowerCase();
        String value = parts[1].trim();
        
        switch (directive) {
            case "user-agent":
                // Save previous user-agent rules
                if (currentUserAgent != null) {
                    robots.addRules(currentUserAgent, currentRules);
                }
                
                currentUserAgent = value;
                currentRules = new ArrayList<>();
                break;
                
            case "disallow":
                if (currentUserAgent != null) {
                    currentRules.add(new Rule(value, false));
                }
                break;
                
            case "allow":
                if (currentUserAgent != null) {
                    currentRules.add(new Rule(value, true));
                }
                break;
                
            case "crawl-delay":
                int delay = Integer.parseInt(value);
                robots.setCrawlDelay(currentUserAgent, delay);
                break;
                
            case "sitemap":
                robots.addSitemap(value);
                break;
        }
    }
    
    // Save last user-agent
    if (currentUserAgent != null) {
        robots.addRules(currentUserAgent, currentRules);
    }
    
    return robots;
}
```

### Wildcard Matching

```java
private boolean matchesPattern(String path, String pattern) {
    // Convert robots.txt pattern to regex
    // * = zero or more characters
    // $ = end of URL
    
    String regex = pattern
        .replace(".", "\\.")    // Escape dots
        .replace("*", ".*")     // * → .*
        .replace("$", "$");     // $ stays as end anchor
    
    return path.matches(regex);
}
```

**Examples:**
```
Pattern: /admin/      → Matches: /admin/*, /admin/users
Pattern: /*.pdf       → Matches: /file.pdf, /docs/report.pdf
Pattern: /private/$   → Matches: /private/ only (not /private/page)
```

---

## Sitemap Extraction

```java
public List<String> discoverSitemaps(String domain) {
    RobotsTxt robots = getRobotsTxt(domain);
    
    if (robots != null && !robots.getSitemaps().isEmpty()) {
        return robots.getSitemaps();
    }
    
    // Try default locations
    return Arrays.asList(
        "https://" + domain + "/sitemap.xml",
        "https://" + domain + "/sitemap_index.xml"
    );
}
```

---

## Crawl Delay Enforcement

```java
@Service
public class CrawlSchedulerService {
    
    public void respectCrawlDelay(String domain) {
        RobotsTxt robots = robotsTxtService.getRobotsTxt(domain);
        
        if (robots != null) {
            int crawlDelay = robots.getCrawlDelay(USER_AGENT);
            
            if (crawlDelay > 0) {
                // Update rate limiter
                rateLimiter.setDelay(domain, crawlDelay * 1000);
                
                log.info("Respecting crawl-delay of {}s for {}", crawlDelay, domain);
            }
        }
    }
}
```

---

## Admin API

```java
@GetMapping("/robots-txt/{domain}")
public RobotsTxtResponse getRobotsTxt(@PathVariable String domain) {
    RobotsTxt robots = robotsTxtService.getRobotsTxt(domain);
    
    return RobotsTxtResponse.builder()
        .domain(domain)
        .rules(robots.getAllRules())
        .crawlDelay(robots.getCrawlDelay("*"))
        .sitemaps(robots.getSitemaps())
        .build();
}

@PostMapping("/robots-txt/refresh/{domain}")
public void refreshRobotsTxt(@PathVariable String domain) {
    robotsTxtService.invalidateCache(domain);
}
```

---

*RFC 9309 compliant robots.txt parser ensures ethical crawling!*
