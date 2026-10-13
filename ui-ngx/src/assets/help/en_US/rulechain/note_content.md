#### Markdown/HTML note content

<div class="divider"></div>
<br/>

Notes support **Markdown** and **HTML** markup for rich text formatting directly on the rule chain canvas.

<div class="divider"></div>
<br/>

#### Headings

Use `#` to create headings. The number of `#` characters defines the level:

```markdown
# Heading 1
## Heading 2
### Heading 3
#### Heading 4
{:copy-code}
```

<div class="divider"></div>
<br/>

#### Text formatting

```markdown
**Bold text**
*Italic text*
~~Strikethrough~~
**_Bold and italic_**
{:copy-code}
```

**Bold text**
*Italic text*
~~Strikethrough~~
**_Bold and italic_**

<div class="divider"></div>
<br/>

#### Lists

Unordered list using `-` or `*`:

```markdown
- Step 1: Receive telemetry
- Step 2: Filter by threshold
  - Value > 100 → alarm branch
  - Value ≤ 100 → success branch
- Step 3: Save to time series
{:copy-code}
```

- Step 1: Receive telemetry
- Step 2: Filter by threshold
  - Value > 100 → alarm branch
  - Value ≤ 100 → success branch
- Step 3: Save to time series

Ordered list using numbers:

```markdown
1. Validate message type
2. Enrich with device attributes
3. Route to appropriate handler
{:copy-code}
```

1. Validate message type
2. Enrich with device attributes
3. Route to appropriate handler

<div class="divider"></div>
<br/>

#### Blockquotes

Use `>` for notes, warnings, or highlighted information:

```markdown
> **Note:** This node requires a valid device alias to be configured upstream.

> **Warning:** Debug mode increases load — disable in production.
{:copy-code}
```

> **Note:** This node requires a valid device alias to be configured upstream.

> **Warning:** Debug mode increases load — disable in production.

<div class="divider"></div>
<br/>

#### Inline code and code blocks

Use backticks for inline code and triple backticks for blocks:

```markdown
The message type must be `POST_TELEMETRY_REQUEST`.
{:copy-code}
```

The message type must be `POST_TELEMETRY_REQUEST`.

````markdown
```json
{
  "temperature": 42.5,
  "humidity": 68
}
```
{:copy-code}
````

```json
{
  "temperature": 42.5,
  "humidity": 68
}
```

<div class="divider"></div>
<br/>

#### Tables

```markdown
| Branch   | Condition        | Next node          |
|----------|------------------|--------------------|
| True     | temperature > 80 | Create Alarm       |
| False    | temperature ≤ 80 | Save Timeseries    |
{:copy-code}
```

| Branch   | Condition        | Next node          |
|----------|------------------|--------------------|
| True     | temperature > 80 | Create Alarm       |
| False    | temperature ≤ 80 | Save Timeseries    |

<div class="divider"></div>
<br/>

#### Horizontal rule

Use `---` to visually separate sections:

```markdown
### Input

Device telemetry message.

---

### Output

Enriched message with customer attributes.
{:copy-code}
```

<div class="divider"></div>
<br/>

#### HTML elements

When **Apply default markdown style** is enabled, you can also use HTML tags:

```html
<span style="color: #e53935;">🔴 Critical path</span><br/>
<span style="color: #43a047;">🟢 Happy path</span>
{:copy-code}
```

<div class="divider"></div>
<br/>

#### Tips for rule chain notes

Use notes to annotate the canvas with context that helps your team:

```markdown
## 🌡️ Temperature monitoring flow

Processes telemetry from HVAC sensors.

| Threshold | Severity |
|-----------|----------|
| > 85 °C   | CRITICAL |
| > 70 °C   | WARNING  |

> **Owner:** Platform team · **Updated:** 2026-02
{:copy-code}
```
