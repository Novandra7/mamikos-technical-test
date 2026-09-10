<?php

declare(strict_types=1);

namespace App\Enums;

/**
 * The three account types supported by the platform.
 *
 * Role is fixed at registration time and can never be changed through a
 * public API endpoint (see BR-13 in the PRD).
 */
enum UserRole: string
{
    case OWNER = 'owner';
    case REGULAR = 'regular';
    case PREMIUM = 'premium';

    public function label(): string
    {
        return match ($this) {
            self::OWNER => 'Owner',
            self::REGULAR => 'Regular User',
            self::PREMIUM => 'Premium User',
        };
    }

    /**
     * Owners never own a credit wallet (D-02): the absence of a wallet is
     * the enforcement mechanism, not just a validation rule.
     */
    public function hasCreditWallet(): bool
    {
        return $this !== self::OWNER;
    }

    /**
     * @return list<string>
     */
    public static function values(): array
    {
        return array_map(fn (self $role) => $role->value, self::cases());
    }
}
