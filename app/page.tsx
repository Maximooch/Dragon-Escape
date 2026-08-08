import type { Metadata } from "next";
import { GameClient } from "./GameClient";

export const metadata: Metadata = {
  title: "Dragon Escape — Public Playtest",
  description: "A first-person multiplayer parkour race against a world-destroying dragon.",
};

export default function Home() {
  return <GameClient />;
}
