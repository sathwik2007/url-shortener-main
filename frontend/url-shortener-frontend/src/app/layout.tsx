import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Toaster } from "react-hot-toast";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "URL Shortener",
  description:
    "Shorten long URLs quickly and easily using this free URL shortener.",
  icons: { icon: "/favicon.ico" },
  openGraph: {
    title: "URL Shortener",
    description: "Create short, shareable links in seconds!",
    url: "https://yourapp.vercel.app",
    siteName: "URL Shortener",
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
        <Toaster position="top-center" reverseOrder={false}/>
      </body>
    </html>
  );
}
