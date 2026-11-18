import { useRef, useState, type KeyboardEvent } from "react";

interface Scene {
  name: string;
  image: string;
  alt: string;
  text: string;
  phone?: boolean;
}

// The recordings live in docs/assets and are copied into the site at build time.
const scenes: Scene[] = [
  {
    name: "Library",
    image: "/assets/showcase/showcase-library.gif",
    alt: "The Trestle library with a Play button, then an instance overview with worlds, servers, and installed content",
    text: "Play from the library, then open an instance to jump into a world, join a server, or manage content.",
  },
  {
    name: "Discover",
    image: "/assets/showcase/showcase-discover.gif",
    alt: "Discover with an install target menu, the new instance dialog with loader choices, and the add account dialog",
    text: "Find mods and shaders for the instance you choose, create new instances, and sign in with the method that suits you.",
  },
  {
    name: "Themes",
    image: "/assets/showcase/showcase-themes.gif",
    alt: "Theme previews in Settings, followed by the library and an instance in the dark and light themes",
    text: "Use the dark or light theme, or follow your system.",
  },
  {
    name: "Phone",
    image: "/assets/showcase/showcase-phone.gif",
    alt: "Trestle on a phone: the library, an instance overview, Discover, and Settings",
    text: "The same layout adapts to phones and tablets.",
    phone: true,
  },
];

/** Recordings of the launcher. Select a tab to play one. */
export function Showcase() {
  const [selected, setSelected] = useState(0);
  const tabs = useRef<(HTMLButtonElement | null)[]>([]);
  const scene = scenes[selected];
  function onKeyDown(event: KeyboardEvent) {
    const step = { ArrowRight: 1, ArrowLeft: -1 }[event.key];
    if (step === undefined) return;
    event.preventDefault();
    const target = (selected + step + scenes.length) % scenes.length;
    setSelected(target);
    tabs.current[target]?.focus();
  }
  return (
    <div className="showcase">
      <div className="showcase-tabs" role="tablist" aria-label="Recordings" onKeyDown={onKeyDown}>
        {scenes.map((item, index) => (
          <button
            key={item.name}
            ref={(node) => {
              tabs.current[index] = node;
            }}
            type="button"
            role="tab"
            id={`showcase-tab-${index}`}
            aria-selected={selected === index}
            aria-controls="showcase-panel"
            tabIndex={selected === index ? 0 : -1}
            onClick={() => setSelected(index)}
          >
            {item.name}
          </button>
        ))}
      </div>
      <figure
        className="showcase-panel"
        id="showcase-panel"
        role="tabpanel"
        aria-labelledby={`showcase-tab-${selected}`}
        data-phone={scene.phone ?? false}
      >
        <div className="showcase-frame">
          <img
            key={scene.image}
            src={scene.image}
            alt={scene.alt}
            width={scene.phone ? 330 : 960}
            height={scene.phone ? 689 : 600}
            loading="lazy"
            decoding="async"
          />
        </div>
        <figcaption>{scene.text}</figcaption>
      </figure>
    </div>
  );
}
