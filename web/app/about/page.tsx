"use client";

import Link from "next/link";
import { Heart, MapPin, Clock, Truck, Star, ChefHat, Leaf, Users } from "lucide-react";
import Navbar from "@/components/layout/Navbar";
import Footer from "@/components/layout/Footer";

const values = [
  {
    icon: Leaf,
    title: "Fresh Ingredients",
    desc: "We source only the finest local and seasonal ingredients — no preservatives, no shortcuts.",
  },
  {
    icon: Heart,
    title: "Made with Love",
    desc: "Every bake is crafted by hand by our passionate bakers who treat each order as a work of art.",
  },
  {
    icon: Truck,
    title: "Warm Delivery",
    desc: "We package and dispatch so your bakes arrive fresh, warm, and perfectly presented.",
  },
  {
    icon: Star,
    title: "Quality First",
    desc: "We never compromise. If it's not perfect, it doesn't leave our kitchen.",
  },
];

const team = [
  {
    name: "Arjun Mehta",
    role: "Head Baker & Founder",
    emoji: "👨‍🍳",
    bio: "With 15 years of experience in artisan baking across Mumbai and Paris, Arjun started Sri Vinayaka Bakery to bring world-class bakes to your doorstep.",
  },
  {
    name: "Priya Nair",
    role: "Pastry Chef",
    emoji: "👩‍🍳",
    bio: "Priya specialises in French pastry and is the creative mind behind our seasonal specials and wedding cake designs.",
  },
  {
    name: "Rahul Sharma",
    role: "Bread Artisan",
    emoji: "🥖",
    bio: "Rahul's sourdoughs and focaccias have a cult following. He cold-ferments every loaf for the perfect crumb and crust.",
  },
];

export default function AboutPage() {
  return (
    <>
      <Navbar />
      <main className="flex-1">
        {/* Hero */}
        <section className="relative bg-gradient-to-br from-cream via-white to-secondary/10 py-20 overflow-hidden">
          <div className="absolute inset-0 opacity-5 pointer-events-none">
            <div className="absolute top-10 right-20 w-72 h-72 rounded-full bg-primary blur-3xl" />
            <div className="absolute bottom-10 left-10 w-96 h-96 rounded-full bg-secondary blur-3xl" />
          </div>
          <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center relative">
            <div className="w-16 h-16 rounded-full bg-gradient-to-br from-primary to-secondary flex items-center justify-center mx-auto mb-6 shadow-lg">
              <ChefHat size={28} className="text-white" />
            </div>
            <h1 className="font-playfair text-4xl md:text-5xl font-bold text-text-primary mb-6">
              Our Story
            </h1>
            <p className="text-lg md:text-xl text-text-secondary max-w-2xl mx-auto leading-relaxed">
              Sri Vinayaka Bakery was born out of a simple belief — everyone deserves access to truly great,
              freshly baked food without having to wait for a special occasion or travel far.
            </p>
            <p className="mt-4 text-text-secondary max-w-2xl mx-auto leading-relaxed">
              Founded in 2022 in Hyderabad, we started as a small home bakery with a sourdough
              loaf, a borrowed oven, and an Instagram page. Today we bake over 500 orders a week,
              but our commitment to handcrafted quality has never changed.
            </p>
          </div>
        </section>

        {/* Values */}
        <section className="py-16 bg-white">
          <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
            <h2 className="font-playfair text-3xl font-bold text-center text-text-primary mb-12">
              What We Stand For
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
              {values.map((v) => {
                const Icon = v.icon;
                return (
                  <div
                    key={v.title}
                    className="bg-cream rounded-2xl p-6 text-center hover:shadow-md transition-shadow"
                  >
                    <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-4">
                      <Icon size={22} className="text-primary" />
                    </div>
                    <h3 className="font-semibold text-text-primary mb-2">{v.title}</h3>
                    <p className="text-sm text-text-secondary leading-relaxed">{v.desc}</p>
                  </div>
                );
              })}
            </div>
          </div>
        </section>

        {/* Stats */}
        <section className="py-14 bg-gradient-to-r from-primary to-primary/90 text-white">
          <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
              {[
                { label: "Orders Delivered", value: "25,000+" },
                { label: "Recipes Mastered", value: "120+" },
                { label: "Happy Customers", value: "8,500+" },
                { label: "Years Baking", value: "3+" },
              ].map((stat) => (
                <div key={stat.label}>
                  <p className="font-playfair text-4xl font-bold">{stat.value}</p>
                  <p className="text-white/80 text-sm mt-1">{stat.label}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Team */}
        <section className="py-16 bg-white">
          <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
            <h2 className="font-playfair text-3xl font-bold text-center text-text-primary mb-12">
              Meet the Bakers
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {team.map((member) => (
                <div
                  key={member.name}
                  className="bg-cream rounded-2xl p-6 text-center hover:shadow-md transition-shadow"
                >
                  <div className="text-5xl mb-4">{member.emoji}</div>
                  <h3 className="font-playfair text-lg font-bold text-text-primary">
                    {member.name}
                  </h3>
                  <p className="text-primary text-sm font-medium mb-3">{member.role}</p>
                  <p className="text-sm text-text-secondary leading-relaxed">{member.bio}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Location */}
        <section className="py-14 bg-cream">
          <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
            <h2 className="font-playfair text-3xl font-bold text-center text-text-primary mb-10">
              Find Us
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="bg-white rounded-2xl p-6 text-center shadow-sm">
                <MapPin size={24} className="text-primary mx-auto mb-3" />
                <h3 className="font-semibold mb-1">Address</h3>
                <p className="text-sm text-text-secondary">
                  123 Bakery Street,<br />Banjara Hills,<br />Hyderabad, 500034
                </p>
              </div>
              <div className="bg-white rounded-2xl p-6 text-center shadow-sm">
                <Clock size={24} className="text-primary mx-auto mb-3" />
                <h3 className="font-semibold mb-1">Baking Hours</h3>
                <p className="text-sm text-text-secondary">
                  Mon – Sat: 6 AM – 9 PM<br />
                  Sunday: 7 AM – 7 PM<br />
                  Online orders: 24 / 7
                </p>
              </div>
              <div className="bg-white rounded-2xl p-6 text-center shadow-sm">
                <Users size={24} className="text-primary mx-auto mb-3" />
                <h3 className="font-semibold mb-1">Get in Touch</h3>
                <p className="text-sm text-text-secondary">
                  hello@srivinayakabakery.in<br />
                  +91 98765 43210<br />
                  @srivinayakabakery on Instagram
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* CTA */}
        <section className="py-16 bg-white text-center">
          <div className="max-w-lg mx-auto px-4">
            <h2 className="font-playfair text-3xl font-bold text-text-primary mb-4">
              Ready to taste the difference?
            </h2>
            <p className="text-text-secondary mb-8">
              Browse our freshly baked selection — from sourdough loaves to custom celebration
              cakes — and get it delivered warm to your door.
            </p>
            <Link
              href="/menu"
              className="bg-gradient-to-r from-accent to-accent/90 text-white px-8 py-3.5 rounded-full font-semibold hover:shadow-lg hover:shadow-accent/25 transition-all inline-flex items-center gap-2"
            >
              Explore the Menu
            </Link>
          </div>
        </section>
      </main>
      <Footer />
    </>
  );
}
