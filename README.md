# mag-stripe

 Swipe content from mags.

 This utility uses [etaoin](https://github.com/clj-commons/etaoin) to scrape articles from publications and blogs and save them as EDN. It supports many[^1] domains!

 Use the Babashka task `bb scrape` to download posts from the command line.
 
 You will be able to do very little with the resulting data on its own. Imagine this utility as the intermediate processor of a reader mode. I recommend [floki](https://github.com/denisidoro/floki) to quickly explore the output without a REPL.

## Sources

The following platforms are available.

- [`n+1`](#n1)
- [`spike-art-magazine`](#spike-art-magazine)
- [`substack`](#substack)
- [`system`](#system-magazine)

Scraping will return as many posts as possible, each with at least this data:

- `:url`
- `:content` a vector of HTML strings of the main content
- `:source` author with publication name fallback
- `:title`
- `:hero`
- `:byline`

All sources can take these keys as an options map, or as CLI args:

Key | Description | Default | Required?
--- | --- | --- | ---
`:platform` | The platform you're scraping from, see list below. | | Yes
`:outfile` | Where your data will be captured. | `"target/posts.edn"` |
`:append?` | Utility will read from a preexisting outfile and append new entries, otherwise will overwrite. | `true` |
`:path-browser` | Path to a browser executable, in case Chrome isn't found at the default location | etaoin default |
`:driver-log-level` | (Not relevant in babashka) Set how verbose the webdriver process is | `"SEVERE"` |
`:log-level` | (Not relevant in babashka) How verbose the browser runtime is | `:error` |
`:limit` | Don't look for more than this many posts. | unset (unlimited) |
`:headless` | Good for debugging visually, `false` will display web browser. | `true` |
`:timeout` | How many seconds certain `wait` operations should hang. | `15` |
`:retries` | How many times should failed operations retry. | `3` (4 total) |

Each platform takes, and can return, a number of additional keys.

### n+1

Will return public posts (neither paywalled nor print-only).

Key | Description | Default | Required?
--- | --- | --- | ---
`:query` | The method you're using to find content, e.g `tag`, `magazine`, `authors` | | Yes
`:slug` | What to look for, e.g `baer-hannah`, `issue-39` | | Yes

Example:
```sh
bb scrape :platform n+1 :query magazine :slug issue-33
```

Todo:

- Attend to "load more" button on long lists

### Spike Art Magazine

Will return public posts with the default keys, plus `:category`.

Key | Description | Default | Required?
--- | --- | --- | ---
`:query` | `"contributors"`, `"subjects"` etc. | | Yes
`:slug` | The URL slug leading to the content you're looking for. | | Yes

Example:
```sh
bb scrape :platform spike-art-magazine :query contributors :slug dean-kissick-0
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
bb scrape :platform substack :domain 'https://circularrainbowmonasteries.substack.com/'
```

Todo:

- Ignore paywalled posts and/or authenticate.

### System Magazine

Will return all posts published online, with additional keys `:introduction` and `:issue`.

Key | Description | Default | Required?
--- | --- | --- | ---
`:query` | `"issues"`, `"archive"`, `"editorial"` | | Yes
`:slug` | Web location, e.g `"system-portfolio"`, `"issue-18"` | | Yes unless `"archive"` query

example:

```sh
bb scrape :platform system :query issues :slug issue-18 :outfile target/system.edn
```

## Projectwide todos:

- Multimethod hierarchies
- Auto generate README

[^1]: 4
