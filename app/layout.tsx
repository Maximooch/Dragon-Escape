import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  metadataBase: new URL("https://dragon-escape.openai.site"),
  title: "Dragon Escape",
  description: "Outrun a world-destroying dragon in a first-person parkour race.",
  openGraph: {
    title: "Dragon Escape — Public Playtest",
    description: "Run. Leap. Survive the collapsing Ashen Causeway.",
    type: "website",
    images: [{ url: "/og.png", width: 1200, height: 630, alt: "Dragon Escape — Run. Leap. Survive." }],
  },
  twitter: {
    card: "summary_large_image",
    title: "Dragon Escape — Public Playtest",
    description: "Run. Leap. Survive the collapsing Ashen Causeway.",
    images: ["/og.png"],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        {children}
      </body>
    </html>
  );
}
