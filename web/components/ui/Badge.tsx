import { cn } from "@/lib/utils";

type BadgeVariant = "success" | "warning" | "error" | "info" | "default";

interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
  className?: string;
}

const variantStyles: Record<BadgeVariant, string> = {
  success: "bg-success/15 text-success border-success/30",
  warning: "bg-secondary/15 text-primary border-secondary/40",
  error: "bg-error/10 text-error border-error/30",
  info: "bg-primary/10 text-primary border-primary/25",
  default: "bg-surface-muted text-text-secondary border-border",
};

export default function Badge({
  variant = "default",
  children,
  className,
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-semibold tracking-wide",
        variantStyles[variant],
        className
      )}
    >
      {children}
    </span>
  );
}
