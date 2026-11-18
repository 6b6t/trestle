import { readdirSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import tailwindcss from "@tailwindcss/vite";
import { tanstackStart } from "@tanstack/react-start/plugin/vite";
import react from "@vitejs/plugin-react";
import { fumadocsMdx } from "fumadocs-mdx/vite";
import { defineConfig } from "vite";

const docs = readdirSync(new URL("./content/docs", import.meta.url))
  .filter((file) => file.endsWith(".mdx") && file !== "download.mdx")
  .map((file) => `/${file.slice(0, -4)}/`);

export default defineConfig({
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  plugins: [
    {
      // The latest release, fetched by scripts/prepare.ts before each build.
      name: "trestle-release",
      resolveId(id) {
        if (id === "virtual:trestle-release") return `\0${id}`;
      },
      load(id) {
        if (id !== "\0virtual:trestle-release") return;
        try {
          return `export default ${readFileSync(new URL("./.generated/release.json", import.meta.url), "utf8")};`;
        } catch (error) {
          if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
          return "export default null;";
        }
      },
    },
    fumadocsMdx(),
    tailwindcss(),
    tanstackStart({
      router: { quoteStyle: "double", semicolons: true },
      prerender: {
        enabled: true,
        autoSubfolderIndex: true,
        crawlLinks: false,
        autoStaticPathsDiscovery: false,
        failOnError: true,
      },
      pages: [
        { path: "/" },
        { path: "/download/" },
        ...docs.map((path) => ({ path })),
        { path: "/search-index.json", prerender: { outputPath: "/search-index.json" } },
        { path: "/404", prerender: { outputPath: "/404.html" } },
      ],
    }),
    react(),
  ],
});
