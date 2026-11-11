#### Advanced CSS

<div class="divider"></div>
<br/>

CSS injected into the platform UI — applied across the whole platform when configured under White Labeling, or just the login page when configured under Login White Labeling. Target existing elements, including vendor-prefixed selectors such as scrollbars. See [White Labeling{:target="_blank"}](${siteBaseUrl}/docs${docPlatformPrefix}/user-guide/white-labeling/) for details.

**Example:**

```css
::-webkit-scrollbar {
  width: 10px;
}

::-webkit-scrollbar-thumb {
  background-color: #263238;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background-color: #263238;
}
{:copy-code}
```
