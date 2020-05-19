You can wrap the whole app into the dropzone. This will make the whole app a Dropzone target.

```
class FullScreen extends React.Component {
  constructor() {
    super()
    this.state = {
      accept: '',
      files: [],
      dropzoneActive: false
    }
  }

  onDragEnter() {
    this.setState({
      dropzoneActive: true
    });
  }

  onDragLeave() {
    this.setState({
      dropzoneActive: false
    });
  }

  onDrop(files) {
    this.setState({
      files,
      dropzoneActive: false
    });
  }

  applyMimeTypes(event) {
    this.setState({
      accept: event.target.value
    });
  }

  render() {
    const { accept, files, dropzoneActive } = this.state;
    const overlayStyle = {
      position: 'absolute',
      top: 0,
      right: 0,
      bottom: 0,
      left: 0,
      padding: '2.5em 0',
      background: 'rgba(0,0,0,0.5)',
      textAlign: 'center',
      color: '#fff'
    };
    return (
      <Dropzone
        disableClick
        style={{}}
        accept={accept}
        onDrop={this.onDrop.bind(this)}
        onDragEnter={this.onDragEnter.bind(this)}
        onDragLeave={this.onDragLeave.bind(this)}
      >
        { dropzoneActive && <div style={overlayStyle}>Drop files...</div> }
        <div>
          <h1>My awesome app</h1>
          <label htmlFor="mimetypes">Enter mime types you want to accept: </label>
          <input
            type="text"
            id="mimetypes"
            onChange={this.applyMimeTypes.bind(this)}
          />

          <h2>Dropped files</h2>
          <ul>
            {
              files.map(f => <li>{f.name} - {f.size} bytes</li>)
            }
          </ul>

        </div>
      </Dropzone>
    );
  }
}

<FullScreen />
```
