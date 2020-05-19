You can programmatically invoke default OS file promt. In order to do that you'll have to set the ref on your `Dropzone` instance and call the instance `open` method.

```
let dropzoneRef;

<div>
  <Dropzone ref={(node) => { dropzoneRef = node; }} onDrop={(accepted, rejected) => { alert(accepted) }}>
      <p>Drop files here.</p>
  </Dropzone>
  <button type="button" onClick={() => { dropzoneRef.open() }}>
      Open File Dialog
  </button>
</div>
```

The completion handler for the `open` function is also the `onDrop` function.
