# mag-stripe

 Swipe content from mags.

 This utility uses [Babashka](https://book.babashka.org/#libraries) and [etaoin](https://github.com/clj-commons/etaoin) to scrape articles from publications and blogs and save them as EDN. It supports many[^1] domains!

 Use the babashka task `scrape` to download posts from the command line.

## Sources

All sources can take these keys as an options map, or as CLI args:

Key | Description | Default | Required?
--- | --- | --- | ---
`:platform` | The platform you're scraping from (see list). | | Yes
`:outfile` | Where your data will be captured. | | Yes
`:append?` | Utility will read from a preexisting outfile and append new entries, otherwise will overwrite. | `false` |
`:path-browser` | Path to a browser executable, in case Chrome isn't found at the default location | etaoin default |
`:driver-log-level` | (Not relevant in babashka) Set how verbose the webdriver process is | `"SEVERE"` |
`:log-level` | (Not relevant in babashka) How verbose the browser runtime is | `:error` |
`:limit` | Don't look for more than this many posts. | unset (unlimited) |
`:headless` | Good for debugging visually, whether to display web browser. | `true` |
`:timeout` | How many seconds certain `wait` operations should hang. | `15` |
`:retries` | How many times should failed operations retry. | `3` (4 total) |

The following platforms are available.

- `:nplusone`
- `:substack`
- `:spike-art-magazine`

Scraping any platform will return as many posts as possible, each with at least this data:

- `:url`
- `:content`
- `:source`
- `:title`
- `:hero`
- `:byline`

Each platform takes, and can return, a number of additional keys.

### n+1

Will return public posts (not paywalled or print-only).

Key | Description | Default | Required?
--- | --- | --- | ---
`:query` | The method you're using to find content (e.g, `tag`, `magazine`, `authors`) | | Yes
`:slug` | What to look for (e.g, `baer-hannah`, `issue-39`) | | Yes

Example:
```sh
bb scrape :platform :nplusone :query :magazine :slug :issue-33
```

Todo:

- Attend to "load more" button on long lists
- Skip paywalled articles

### Spike Art Magazine

Will return public posts with the default keys, plus `:category`.

Key | Description | Default | Required?
--- | --- | --- | ---
`:query` | `"contributors"`, `"subjects"` etc. | | Yes
`:slug` | The URL slug leading to the content you're looking for. | | Yes

Example:
```sh
bb scrape :platform :spike-art-magazine :query contributors :slug dean-kissick-0
```

### Substack

Will return all public posts.

**Caveat:** Substack seems to occasionally throttle/block/drop headless requests; If the utility keeps throwing exceptions while parsing posts, try setting `:headless true`.

Key | Description | Default | Required?
--- | --- | --- | ---
`:domain` | Homepage URL of the blog you want | | Yes
`:sort-method` | How the archive should be sorted | `"new"`

Example:
```sh
bb scrape :platform :substack :domain 'https://circularrainbowmonasteries.substack.com/'
```

TODO, ignore paywalled posts and/or authenticate.

[^1]: 3
