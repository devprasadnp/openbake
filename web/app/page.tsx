"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { ArrowRight, Clock, Truck, Star, ChefHat, Sparkles } from "lucide-react";
import Navbar from "@/components/layout/Navbar";
import Footer from "@/components/layout/Footer";
import ProductCard from "@/components/product/ProductCard";
import api from "@/lib/api";
import type { Product, Category } from "@/types";

const steps = [
  { title: "Browse & Choose", description: "Explore our freshly baked collection of cakes, breads & more", icon: Sparkles, color: "from-primary/20 to-secondary/20" },
  { title: "Customize", description: "Pick your size, flavor, add a personal message on cakes", icon: ChefHat, color: "from-secondary/20 to-accent/20" },
  { title: "Fast Delivery", description: "Get it delivered warm to your doorstep in your chosen time slot", icon: Truck, color: "from-accent/20 to-primary/20" },
];

const testimonials = [
  { name: "Priya S.", text: "The chocolate truffle cake was divine! Perfectly moist and delivered on time.", rating: 5 },
  { name: "Rahul M.", text: "Best bakery in town. The sourdough bread is absolutely fresh every time.", rating: 5 },
  { name: "Ananya K.", text: "Love the eggless options. My birthday cake was beautifully customized!", rating: 4 },
];

export default function HomePage() {
  const [bestsellers, setBestsellers] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const [prodRes, catRes] = await Promise.all([
          api.get<{ items: Product[] }>("/products?page_size=8"),
          api.get<Category[]>("/categories"),
        ]);
        setBestsellers(prodRes.data.items ?? prodRes.data as unknown as Product[]);
        setCategories(catRes.data);
      } catch {
        // API might not be running
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  return (
    <>
      <Navbar />
      <main className="flex-1">
        {/* Hero Section */}
        <section className="relative overflow-hidden bg-gradient-to-br from-cream via-white to-secondary/5">
          <div className="absolute inset-0 opacity-5">
            <div className="absolute top-20 right-20 w-72 h-72 rounded-full bg-primary blur-3xl" />
            <div className="absolute bottom-10 left-10 w-96 h-96 rounded-full bg-secondary blur-3xl" />
          </div>
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 md:py-24 relative">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
              <div>
                <div className="inline-flex items-center gap-2 bg-primary/10 text-primary px-4 py-1.5 rounded-full text-sm font-medium mb-6">
                  <Clock size={14} />
                  Fresh from the oven, every day
                </div>
                <h1 className="font-playfair text-4xl md:text-5xl lg:text-6xl font-bold text-text-primary leading-tight">
                  Artisan Bakes,{" "}
                  <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary to-accent">
                    Delivered Warm
                  </span>
                </h1>
                <p className="mt-5 text-lg text-text-secondary max-w-lg leading-relaxed">
                  Custom cakes, flaky pastries, artisan breads and more &mdash; crafted
                  with love and delivered fresh to your doorstep.
                </p>
                <div className="mt-8 flex flex-wrap gap-4">
                  <Link
                    href="/menu"
                    className="bg-gradient-to-r from-accent to-accent/90 text-white px-8 py-3.5 rounded-full font-semibold hover:shadow-lg hover:shadow-accent/25 transition-all text-lg inline-flex items-center gap-2"
                  >
                    Order Now <ArrowRight size={20} />
                  </Link>
                  <Link
                    href="/menu"
                    className="border-2 border-primary text-primary px-8 py-3.5 rounded-full font-semibold hover:bg-primary hover:text-white transition-all text-lg"
                  >
                    View Menu
                  </Link>
                </div>
                <div className="mt-10 flex items-center gap-6 text-sm text-text-secondary">
                  <div className="flex items-center gap-1.5">
                    <Star size={16} className="fill-secondary text-secondary" />
                    <span className="font-medium">4.8/5 Rating</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Truck size={16} className="text-success" />
                    <span className="font-medium">Free Delivery 500+</span>
                  </div>
                </div>
              </div>
              <div className="hidden lg:block">
                <div className="relative w-full aspect-square max-w-md mx-auto">
                  <div className="absolute inset-4 rounded-3xl bg-gradient-to-br from-primary/10 to-secondary/20 rotate-6" />
                  <div className="relative rounded-3xl overflow-hidden shadow-2xl shadow-primary/10">
                    <Image
                      src="https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=600"
                      alt="Artisan cake"
                      width={600}
                      height={600}
                      className="object-cover w-full h-full"
                      priority
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Categories */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14">
          <h2 className="font-playfair text-2xl font-bold text-text-primary text-center mb-8">
            Shop by Category
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-4">
            {categories.map((cat) => (
              <Link
                key={cat.id}
                href={`/menu?category=${cat.id}`}
                className="group flex flex-col items-center gap-3 bg-white border border-border p-4 rounded-2xl hover:border-primary hover:shadow-md transition-all text-center"
              >
                <div className="w-16 h-16 rounded-full bg-gradient-to-br from-cream to-secondary/10 overflow-hidden">
                  {cat.image_url ? (
                    <Image src={cat.image_url} alt={cat.name} width={64} height={64} className="object-cover w-full h-full" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-2xl">🧁</div>
                  )}
                </div>
                <span className="text-sm font-medium text-text-secondary group-hover:text-primary transition-colors">
                  {cat.name}
                </span>
              </Link>
            ))}
          </div>
        </section>

        {/* Bestsellers */}
        <section className="bg-white py-14">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex items-center justify-between mb-8">
              <h2 className="font-playfair text-2xl md:text-3xl font-bold text-text-primary">
                Our Bestsellers
              </h2>
              <Link href="/menu" className="text-primary font-medium text-sm hover:underline inline-flex items-center gap-1">
                View All <ArrowRight size={16} />
              </Link>
            </div>
            {loading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                {[1, 2, 3, 4].map((i) => (
                  <div key={i} className="bg-cream rounded-2xl overflow-hidden animate-pulse">
                    <div className="aspect-[4/3] bg-border/30" />
                    <div className="p-4 space-y-3">
                      <div className="h-4 bg-border/30 rounded w-3/4" />
                      <div className="h-3 bg-border/30 rounded w-1/2" />
                      <div className="h-5 bg-border/30 rounded w-1/3" />
                    </div>
                  </div>
                ))}
              </div>
            ) : bestsellers.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                {bestsellers.slice(0, 8).map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            ) : (
              <p className="text-center text-text-secondary mt-6">No products available yet.</p>
            )}
          </div>
        </section>

        {/* How It Works */}
        <section className="py-16">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <h2 className="font-playfair text-2xl md:text-3xl font-bold text-text-primary text-center mb-12">
              How It Works
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {steps.map((step, index) => {
                const Icon = step.icon;
                return (
                  <div key={index} className="text-center group">
                    <div className={`w-20 h-20 rounded-2xl bg-gradient-to-br ${step.color} flex items-center justify-center mx-auto mb-5 group-hover:scale-110 transition-transform`}>
                      <Icon size={32} className="text-primary" />
                    </div>
                    <div className="text-sm font-bold text-primary mb-2">Step {index + 1}</div>
                    <h3 className="font-semibold text-lg text-text-primary mb-2">{step.title}</h3>
                    <p className="text-text-secondary text-sm leading-relaxed">{step.description}</p>
                  </div>
                );
              })}
            </div>
          </div>
        </section>

        {/* Testimonials */}
        <section className="bg-white py-16">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <h2 className="font-playfair text-2xl md:text-3xl font-bold text-text-primary text-center mb-12">
              What Our Customers Say
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {testimonials.map((t, i) => (
                <div key={i} className="bg-cream rounded-2xl p-6 border border-border">
                  <div className="flex gap-1 mb-3">
                    {[1, 2, 3, 4, 5].map((s) => (
                      <Star key={s} size={16} className={s <= t.rating ? "fill-secondary text-secondary" : "text-border"} />
                    ))}
                  </div>
                  <p className="text-text-secondary text-sm leading-relaxed mb-4">&ldquo;{t.text}&rdquo;</p>
                  <p className="font-semibold text-text-primary text-sm">{t.name}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* CTA Banner */}
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="bg-gradient-to-r from-primary to-accent rounded-3xl p-8 md:p-12 text-white text-center">
            <h2 className="font-playfair text-2xl md:text-3xl font-bold mb-3">
              Get 10% Off Your First Order
            </h2>
            <p className="text-white/80 mb-6 max-w-md mx-auto">
              Use code <span className="font-bold text-white">WELCOME10</span> at checkout
            </p>
            <Link
              href="/menu"
              className="inline-flex items-center gap-2 bg-white text-primary px-8 py-3 rounded-full font-semibold hover:shadow-lg transition-all"
            >
              Shop Now <ArrowRight size={18} />
            </Link>
          </div>
        </section>
      </main>
      <Footer />
    </>
  );
}
