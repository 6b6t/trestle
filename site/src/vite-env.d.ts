/// <reference types="vite/client" />

declare module "virtual:trestle-release" {
  const release: import("./lib/releases").Release | null;
  export default release;
}
