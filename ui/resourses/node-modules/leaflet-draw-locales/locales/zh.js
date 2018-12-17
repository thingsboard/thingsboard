const drawToolbar = {
  actions: {
    title: '取消标绘',
    text: '取消'
  },
  finish: {
    title: '完成标绘',
    text: '完成'
  },
  undo: {
    title: '删除绘制的最后一个点',
    text: '删除最后一个点'
  },
  buttons: {
    polyline: '线',
    polygon: '多边形',
    rectangle: '矩形',
    circle: '圆',
    marker: '图标标注',
    circlemarker: '圆形标注'
  }
}

const drawHandlers = {
  circle: {
    tooltip: {
      start: '点击并拖到鼠标绘制圆.'
    },
    radius: '半径'
  },
  circlemarker: {
    tooltip: {
      start: '点击地图放置圆形标注.'
    }
  },
  marker: {
    tooltip: {
      start: '点击地图放置图标标注.'
    }
  },
  polygon: {
    error: '<strong>错误:</strong> 出错了!',
    tooltip: {
      start: '点击开始绘制多边形.',
      cont: '点击继续绘制多边形.',
      end: '点击第一个点封闭该多边形完成绘制.'
    }
  },
  polyline: {
    error: '<strong>错误:</strong> 要素不能相交!',
    tooltip: {
      start: '点击开始绘制线条.',
      cont: '点击继续绘制线条.',
      end: '点击最后一个点完成绘制.'
    }
  },
  rectangle: {
    tooltip: {
      start: '点击并拖到绘制矩形.'
    }
  },
  simpleshape: {
    tooltip: {
      end: '松开属性完成绘制.'
    }
  }
}

const editToolbar = {
  actions: {
    save: {
      title: '保存更改.',
      text: '保存'
    },
    cancel: {
      title: '取消编辑, 丢弃所有更改.',
      text: '取消'
    },
    clearAll: {
      title: '清除全部图层.',
      text: '全部清除'
    }
  },
  buttons: {
    edit: '编辑图层.',
    editDisabled: '没有要编辑的图层.',
    remove: '删除图层.',
    removeDisabled: '没有要删除的图层.'
  }
}

const editHandlers = {
  edit: {
    tooltip: {
      text: '拖动角点或标注来编辑要素.',
      subtext: '点击取消，放弃修改.'
    }
  },
  remove: {
    tooltip: {
      text: '点击要素删除它'
    }
  }
}

module.exports = {
  draw: {
    toolbar: drawToolbar,
    handlers: drawHandlers
  },
  edit: {
    toolbar: editToolbar,
    handlers: editHandlers
  }
}
