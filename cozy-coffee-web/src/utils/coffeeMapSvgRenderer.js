export const cameraToSvgMatrix = camera =>
  `matrix(${camera.zoom} 0 0 ${camera.zoom} ${camera.translateX} ${camera.translateY})`
