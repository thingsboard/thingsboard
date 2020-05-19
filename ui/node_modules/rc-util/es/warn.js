export default function warn(msg) {
  if (process.env.NODE_ENV !== 'production') {
    if (typeof console !== 'undefined' && console.warn) {
      console.warn(msg);
    }
  }
}