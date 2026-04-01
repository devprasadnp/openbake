import type { Metadata } from "next";
import Script from "next/script";
import { Playfair_Display, Nunito } from "next/font/google";
import { Toaster } from "react-hot-toast";
import AuthInitializer from "@/components/AuthInitializer";
import "./globals.css";

const playfair = Playfair_Display({
  subsets: ["latin"],
  variable: "--font-playfair",
  display: "swap",
});

const nunito = Nunito({
  subsets: ["latin"],
  variable: "--font-nunito",
  display: "swap",
});

export const metadata: Metadata = {
  title: "OpenBake — Fresh. Warm. Delivered.",
  description:
    "Order freshly baked cakes, pastries, breads, and snacks online. Customize your order and get it delivered to your doorstep.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${playfair.variable} ${nunito.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col font-nunito bg-cream text-primary">
        <Script
          src="https://checkout.razorpay.com/v1/checkout.js"
          strategy="lazyOnload"
        />
        <AuthInitializer>
          <Toaster position="top-right" />
          {children}
        </AuthInitializer>
      </body>
    </html>
  );
}
