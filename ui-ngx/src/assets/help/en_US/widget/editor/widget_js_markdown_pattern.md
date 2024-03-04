#### Markdown pattern

<div class="divider"></div>
<br/>

The Markdown template displays the value of the first found key in the entities in the entity alias.

<div class="divider"></div>
<br/>

#### Examples

Use # to create a Markdown header. The number of characters # specifies the type of header: # - h1, ## - h2, ### - h3, etc.

```markdown
    ###### Markdown/HTML card
{:copy-code}
```
 ###### Markdown/HTML card
 
<div class="divider"></div>
<br/>

Use - character to create list item. You can create nested lists separating them with tabs in the pattern:

 ```markdown
    - Element 1
    - Element 2
        - Element 2.1
        - Element 2.2
    -Element 3 
{:copy-code}
 ```
- Element 1
- Element 2
    - Element 2.1
    - Element 2.2
- Element 3

<div class="divider"></div>
<br/>

Use * character to choose style:
 
 ```markdown
   - *Element 1*
   - **Element 2**
   - ***Element 3***
{:copy-code}
 ```
- *Element 1*
- **Element 2**
- ***Element 3***

<div class="divider"></div>
<br/>

Use ${} to add some value from your key:
 ```markdown
    - **Element 1**: ${key1Name}
    - **Element 1**: ${key2Name}
    - **Element 1**: ${key3Name}
{:copy-code}
 ```
 - **Element 1**: key1Value
 - **Element 2**: key2Value
 - **Element 3**: key3Value
 
