export function readmeImage(name: string): string {
  return new URL(`../../../assets/readme/${name}`, import.meta.url).href
}

export function screenshotImage(name: string): string {
  return new URL(`../../../assets/readme/screenshots/${name}`, import.meta.url).href
}
