import { createFileRoute } from "@tanstack/react-router";
import { createFromSource } from "fumadocs-core/search/server";
import { loader } from "fumadocs-core/source";
import { docs } from "../../.source/server";
import { homePage, site } from "../lib/site";

const content = docs.toFumadocsSource();
const search = createFromSource(
  loader(
    {
      files: [
        ...content.files,
        {
          type: "page",
          path: "index.mdx",
          data: {
            title: site.title,
            description: homePage.description,
            structuredData: {
              headings: [],
              contents: [{ heading: undefined, content: homePage.description }],
            },
          },
        },
      ],
    },
    { baseUrl: "/" },
  ),
);

export const Route = createFileRoute("/search-index.json")({
  server: { handlers: { GET: () => search.staticGET() } },
});
