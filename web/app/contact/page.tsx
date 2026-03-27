"use client";

import { useState } from "react";
import Navbar from "@/components/layout/Navbar";
import Footer from "@/components/layout/Footer";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import toast from "react-hot-toast";

export default function ContactPage() {
  const [form, setForm] = useState({ name: "", email: "", subject: "", message: "" });
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    // Simulate submission (replace with real API call if needed)
    await new Promise((r) => setTimeout(r, 800));
    toast.success("Message sent! We'll get back to you soon.");
    setForm({ name: "", email: "", subject: "", message: "" });
    setSubmitting(false);
  };

  return (
    <>
      <Navbar />
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="text-center mb-12">
          <h1 className="font-playfair text-4xl font-bold mb-4">Contact Us</h1>
          <p className="text-text-secondary text-lg max-w-xl mx-auto">
            Have a question, special order, or feedback? We&apos;d love to hear from you.
            Reach out and our team will respond within 24 hours.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
          {/* Contact info */}
          <div className="space-y-8">
            <div>
              <h2 className="font-playfair text-2xl font-semibold mb-6">Get in Touch</h2>
              <div className="space-y-5">
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-lg shrink-0">
                    📍
                  </div>
                  <div>
                    <p className="font-semibold">Our Bakery</p>
                    <p className="text-text-secondary text-sm">
                      123 Baker Street, Bengaluru,<br />Karnataka 560001, India
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-lg shrink-0">
                    📞
                  </div>
                  <div>
                    <p className="font-semibold">Phone</p>
                    <a href="tel:+918001234567" className="text-primary text-sm hover:underline">
                      +91 800 123 4567
                    </a>
                  </div>
                </div>

                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-lg shrink-0">
                    ✉️
                  </div>
                  <div>
                    <p className="font-semibold">Email</p>
                    <a href="mailto:hello@openbake.in" className="text-primary text-sm hover:underline">
                      hello@openbake.in
                    </a>
                  </div>
                </div>

                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-lg shrink-0">
                    🕐
                  </div>
                  <div>
                    <p className="font-semibold">Business Hours</p>
                    <p className="text-text-secondary text-sm">
                      Monday – Saturday: 7:00 AM – 9:00 PM<br />
                      Sunday: 8:00 AM – 6:00 PM
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Map placeholder */}
            <div className="rounded-2xl overflow-hidden bg-cream border border-border h-48 flex items-center justify-center">
              <p className="text-text-secondary text-sm">🗺️ Map coming soon</p>
            </div>
          </div>

          {/* Contact form */}
          <div className="bg-white rounded-2xl p-8 shadow-sm">
            <h2 className="font-playfair text-2xl font-semibold mb-6">Send a Message</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <Input
                id="name"
                label="Full Name"
                placeholder="Jane Doe"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
              <Input
                id="email"
                label="Email Address"
                type="email"
                placeholder="jane@example.com"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
              <Input
                id="subject"
                label="Subject"
                placeholder="Custom cake order, delivery query…"
                value={form.subject}
                onChange={(e) => setForm({ ...form, subject: e.target.value })}
                required
              />
              <div className="flex flex-col gap-1">
                <label htmlFor="message" className="text-sm font-medium text-text-primary">
                  Message
                </label>
                <textarea
                  id="message"
                  rows={5}
                  placeholder="Tell us more about your request…"
                  value={form.message}
                  onChange={(e) => setForm({ ...form, message: e.target.value })}
                  required
                  className="w-full rounded-xl border border-border px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 resize-none bg-cream/30"
                />
              </div>
              <Button type="submit" disabled={submitting} className="w-full">
                {submitting ? "Sending…" : "Send Message"}
              </Button>
            </form>
          </div>
        </div>
      </main>
      <Footer />
    </>
  );
}
