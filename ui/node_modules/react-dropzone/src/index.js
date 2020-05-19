/* eslint prefer-template: 0 */

import React from 'react'
import PropTypes from 'prop-types'
import accepts from 'attr-accept'
import getDataTransferItems from './getDataTransferItems'

const supportMultiple = typeof document !== 'undefined' && document && document.createElement
  ? 'multiple' in document.createElement('input')
  : true

function fileAccepted(file, accept) {
  // Firefox versions prior to 53 return a bogus MIME type for every file drag, so dragovers with
  // that MIME type will always be accepted
  return file.type === 'application/x-moz-file' || accepts(file, accept)
}

class Dropzone extends React.Component {
  static onDocumentDragOver(evt) {
    // allow the entire document to be a drag target
    evt.preventDefault()
  }

  constructor(props, context) {
    super(props, context)
    this.onClick = this.onClick.bind(this)
    this.onDocumentDrop = this.onDocumentDrop.bind(this)
    this.onDragStart = this.onDragStart.bind(this)
    this.onDragEnter = this.onDragEnter.bind(this)
    this.onDragLeave = this.onDragLeave.bind(this)
    this.onDragOver = this.onDragOver.bind(this)
    this.onDrop = this.onDrop.bind(this)
    this.onFileDialogCancel = this.onFileDialogCancel.bind(this)
    this.setRef = this.setRef.bind(this)
    this.setRefs = this.setRefs.bind(this)
    this.onInputElementClick = this.onInputElementClick.bind(this)
    this.isFileDialogActive = false
    this.state = {
      draggedFiles: [],
      acceptedFiles: [],
      rejectedFiles: []
    }
  }

  componentDidMount() {
    const { preventDropOnDocument } = this.props
    this.dragTargets = []

    if (preventDropOnDocument) {
      document.addEventListener('dragover', Dropzone.onDocumentDragOver, false)
      document.addEventListener('drop', this.onDocumentDrop, false)
    }
    this.fileInputEl.addEventListener('click', this.onInputElementClick, false)
    // Tried implementing addEventListener, but didn't work out
    document.body.onfocus = this.onFileDialogCancel
  }

  componentWillUnmount() {
    const { preventDropOnDocument } = this.props
    if (preventDropOnDocument) {
      document.removeEventListener('dragover', Dropzone.onDocumentDragOver)
      document.removeEventListener('drop', this.onDocumentDrop)
    }
    this.fileInputEl.removeEventListener('click', this.onInputElementClick, false)
    // Can be replaced with removeEventListener, if addEventListener works
    document.body.onfocus = null
  }

  onDocumentDrop(evt) {
    if (this.node.contains(evt.target)) {
      // if we intercepted an event for our instance, let it propagate down to the instance's onDrop handler
      return
    }
    evt.preventDefault()
    this.dragTargets = []
  }

  onDragStart(evt) {
    if (this.props.onDragStart) {
      this.props.onDragStart.call(this, evt)
    }
  }

  onDragEnter(evt) {
    evt.preventDefault()

    // Count the dropzone and any children that are entered.
    if (this.dragTargets.indexOf(evt.target) === -1) {
      this.dragTargets.push(evt.target)
    }

    this.setState({ draggedFiles: getDataTransferItems(evt) })

    if (this.props.onDragEnter) {
      this.props.onDragEnter.call(this, evt)
    }
  }

  onDragOver(evt) {
    // eslint-disable-line class-methods-use-this
    evt.preventDefault()
    evt.stopPropagation()
    try {
      evt.dataTransfer.dropEffect = 'copy' // eslint-disable-line no-param-reassign
    } catch (err) {
      // continue regardless of error
    }

    if (this.props.onDragOver) {
      this.props.onDragOver.call(this, evt)
    }
    return false
  }

  onDragLeave(evt) {
    evt.preventDefault()

    // Only deactivate once the dropzone and all children have been left.
    this.dragTargets = this.dragTargets.filter(el => el !== evt.target && this.node.contains(el))
    if (this.dragTargets.length > 0) {
      return
    }

    // Clear dragging files state
    this.setState({ draggedFiles: [] })

    if (this.props.onDragLeave) {
      this.props.onDragLeave.call(this, evt)
    }
  }

  onDrop(evt) {
    const { onDrop, onDropAccepted, onDropRejected, multiple, disablePreview, accept } = this.props
    const fileList = getDataTransferItems(evt)
    const acceptedFiles = []
    const rejectedFiles = []

    // Stop default browser behavior
    evt.preventDefault()

    // Reset the counter along with the drag on a drop.
    this.dragTargets = []
    this.isFileDialogActive = false

    fileList.forEach(file => {
      if (!disablePreview) {
        try {
          file.preview = window.URL.createObjectURL(file) // eslint-disable-line no-param-reassign
        } catch (err) {
          if (process.env.NODE_ENV !== 'production') {
            console.error('Failed to generate preview for file', file, err) // eslint-disable-line no-console
          }
        }
      }

      if (fileAccepted(file, accept) && this.fileMatchSize(file)) {
        acceptedFiles.push(file)
      } else {
        rejectedFiles.push(file)
      }
    })

    if (!multiple) {
      // if not in multi mode add any extra accepted files to rejected.
      // This will allow end users to easily ignore a multi file drop in "single" mode.
      rejectedFiles.push(...acceptedFiles.splice(1))
    }

    if (onDrop) {
      onDrop.call(this, acceptedFiles, rejectedFiles, evt)
    }

    if (rejectedFiles.length > 0 && onDropRejected) {
      onDropRejected.call(this, rejectedFiles, evt)
    }

    if (acceptedFiles.length > 0 && onDropAccepted) {
      onDropAccepted.call(this, acceptedFiles, evt)
    }

    // Clear files value
    this.draggedFiles = null

    // Reset drag state
    this.setState({
      draggedFiles: [],
      acceptedFiles,
      rejectedFiles
    })
  }

  onClick(evt) {
    const { onClick, disableClick } = this.props
    if (!disableClick) {
      evt.stopPropagation()

      if (onClick) {
        onClick.call(this, evt)
      }

      // in IE11/Edge the file-browser dialog is blocking, ensure this is behind setTimeout
      // this is so react can handle state changes in the onClick prop above above
      // see: https://github.com/okonet/react-dropzone/issues/450
      setTimeout(this.open.bind(this), 0)
    }
  }

  onInputElementClick(evt) {
    evt.stopPropagation()
    if (this.props.inputProps && this.props.inputProps.onClick) {
      this.props.inputProps.onClick()
    }
  }

  onFileDialogCancel() {
    // timeout will not recognize context of this method
    const { onFileDialogCancel } = this.props
    const { fileInputEl } = this
    let { isFileDialogActive } = this
    // execute the timeout only if the onFileDialogCancel is defined and FileDialog
    // is opened in the browser
    if (onFileDialogCancel && isFileDialogActive) {
      setTimeout(() => {
        // Returns an object as FileList
        const FileList = fileInputEl.files
        if (!FileList.length) {
          isFileDialogActive = false
          onFileDialogCancel()
        }
      }, 300)
    }
  }

  setRef(ref) {
    this.node = ref
  }

  setRefs(ref) {
    this.fileInputEl = ref
  }

  fileMatchSize(file) {
    return file.size <= this.props.maxSize && file.size >= this.props.minSize
  }

  allFilesAccepted(files) {
    return files.every(file => fileAccepted(file, this.props.accept))
  }

  /**
   * Open system file upload dialog.
   *
   * @public
   */
  open() {
    this.isFileDialogActive = true
    this.fileInputEl.value = null
    this.fileInputEl.click()
  }

  renderChildren = (children, isDragActive, isDragReject) => {
    if (typeof children === 'function') {
      return children({ ...this.state, isDragActive, isDragReject })
    }
    return children
  }

  render() {
    const {
      accept,
      activeClassName,
      inputProps,
      multiple,
      name,
      rejectClassName,
      children,
      ...rest
    } = this.props

    let {
      activeStyle,
      className,
      rejectStyle,
      style,
      ...props // eslint-disable-line prefer-const
    } = rest

    const { draggedFiles } = this.state
    const filesCount = draggedFiles.length
    const isMultipleAllowed = multiple || filesCount <= 1
    const isDragActive = filesCount > 0 && this.allFilesAccepted(draggedFiles)
    const isDragReject = filesCount > 0 && (!isDragActive || !isMultipleAllowed)

    className = className || ''

    if (isDragActive && activeClassName) {
      className += ' ' + activeClassName
    }
    if (isDragReject && rejectClassName) {
      className += ' ' + rejectClassName
    }

    if (!className && !style && !activeStyle && !rejectStyle) {
      style = {
        width: 200,
        height: 200,
        borderWidth: 2,
        borderColor: '#666',
        borderStyle: 'dashed',
        borderRadius: 5
      }
      activeStyle = {
        borderStyle: 'solid',
        borderColor: '#6c6',
        backgroundColor: '#eee'
      }
      rejectStyle = {
        borderStyle: 'solid',
        borderColor: '#c66',
        backgroundColor: '#eee'
      }
    }

    let appliedStyle
    if (activeStyle && isDragActive) {
      appliedStyle = {
        ...style,
        ...activeStyle
      }
    } else if (rejectStyle && isDragReject) {
      appliedStyle = {
        ...style,
        ...rejectStyle
      }
    } else {
      appliedStyle = {
        ...style
      }
    }

    const inputAttributes = {
      accept,
      type: 'file',
      style: { display: 'none' },
      multiple: supportMultiple && multiple,
      ref: this.setRefs,
      onChange: this.onDrop
    }

    if (name && name.length) {
      inputAttributes.name = name
    }

    // Remove custom properties before passing them to the wrapper div element
    const customProps = [
      'acceptedFiles',
      'preventDropOnDocument',
      'disablePreview',
      'disableClick',
      'onDropAccepted',
      'onDropRejected',
      'onFileDialogCancel',
      'maxSize',
      'minSize'
    ]
    const divProps = { ...props }
    customProps.forEach(prop => delete divProps[prop])

    return (
      <div
        className={className}
        style={appliedStyle}
        {...divProps /* expand user provided props first so event handlers are never overridden */}
        onClick={this.onClick}
        onDragStart={this.onDragStart}
        onDragEnter={this.onDragEnter}
        onDragOver={this.onDragOver}
        onDragLeave={this.onDragLeave}
        onDrop={this.onDrop}
        ref={this.setRef}
      >
        {this.renderChildren(children, isDragActive, isDragReject)}
        <input
          {...inputProps /* expand user provided inputProps first so inputAttributes override them */}
          {...inputAttributes}
        />
      </div>
    )
  }
}

Dropzone.propTypes = {
  /**
   * Allow specific types of files. See https://github.com/okonet/attr-accept for more information.
   * Keep in mind that mime type determination is not reliable accross platforms. CSV files,
   * for example, are reported as text/plain under macOS but as application/vnd.ms-excel under
   * Windows. In some cases there might not be a mime type set at all.
   * See: https://github.com/okonet/react-dropzone/issues/276
   */
  accept: PropTypes.string,

  /**
   * Contents of the dropzone
   */
  children: PropTypes.oneOfType([PropTypes.node, PropTypes.func]),

  /**
   * Disallow clicking on the dropzone container to open file dialog
   */
  disableClick: PropTypes.bool,

  /**
   * Enable/disable preview generation
   */
  disablePreview: PropTypes.bool,

  /**
   * If false, allow dropped items to take over the current browser window
   */
  preventDropOnDocument: PropTypes.bool,

  /**
   * Pass additional attributes to the `<input type="file"/>` tag
   */
  inputProps: PropTypes.object,

  /**
   * Allow dropping multiple files
   */
  multiple: PropTypes.bool,

  /**
   * `name` attribute for the input tag
   */
  name: PropTypes.string,

  /**
   * Maximum file size
   */
  maxSize: PropTypes.number,

  /**
   * Minimum file size
   */
  minSize: PropTypes.number,

  /**
   * className
   */
  className: PropTypes.string,

  /**
   * className for accepted state
   */
  activeClassName: PropTypes.string,

  /**
   * className for rejected state
   */
  rejectClassName: PropTypes.string,

  /**
   * CSS styles to apply
   */
  style: PropTypes.object,

  /**
   * CSS styles to apply when drop will be accepted
   */
  activeStyle: PropTypes.object,

  /**
   * CSS styles to apply when drop will be rejected
   */
  rejectStyle: PropTypes.object,

  /**
   * onClick callback
   * @param {Event} event
   */
  onClick: PropTypes.func,

  /**
   * onDrop callback
   */
  onDrop: PropTypes.func,

  /**
   * onDropAccepted callback
   */
  onDropAccepted: PropTypes.func,

  /**
   * onDropRejected callback
   */
  onDropRejected: PropTypes.func,

  /**
   * onDragStart callback
   */
  onDragStart: PropTypes.func,

  /**
   * onDragEnter callback
   */
  onDragEnter: PropTypes.func,

  /**
   * onDragOver callback
   */
  onDragOver: PropTypes.func,

  /**
   * onDragLeave callback
   */
  onDragLeave: PropTypes.func,

  /**
   * Provide a callback on clicking the cancel button of the file dialog
   */
  onFileDialogCancel: PropTypes.func
}

Dropzone.defaultProps = {
  preventDropOnDocument: true,
  disablePreview: false,
  disableClick: false,
  multiple: true,
  maxSize: Infinity,
  minSize: 0
}

export default Dropzone
